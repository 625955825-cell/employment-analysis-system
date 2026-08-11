"""
TF-IDF + Cosine Similarity 核心模型
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


class TfidfSimilarityModel:
    def __init__(self):
        self.vectorizer = TfidfVectorizer(
            max_features=5000,
            ngram_range=(1, 2),
            min_df=1,
            max_df=0.95,
        )
        self._fitted = False
        self._job_vectors = None
        self._jobs: List[Dict[str, Any]] = []
        self._trained_at: str | None = None

    def is_ready(self) -> bool:
        return self._fitted and self._job_vectors is not None and len(self._jobs) > 0

    def status(self) -> Dict[str, Any]:
        vocabulary_size = len(self.vectorizer.vocabulary_) if self._fitted else 0
        feature_dim = int(self._job_vectors.shape[1]) if self._job_vectors is not None else 0
        return {
            "model_loaded": self.is_ready(),
            "ready": self.is_ready(),
            "total_jobs": len(self._jobs),
            "vocabulary_size": vocabulary_size,
            "feature_dim": feature_dim,
            "last_trained": self._trained_at,
        }

    def train_jobs(self, jobs: List[Dict[str, Any]]) -> Dict[str, Any]:
        valid_jobs = []
        texts = []
        for job in jobs:
            text = self._build_job_text(job)
            if not text:
                continue
            valid_jobs.append(job)
            texts.append(text)

        if not texts:
            raise ValueError("没有可用于训练的有效职位文本")

        self._job_vectors = self.vectorizer.fit_transform(texts)
        self._jobs = valid_jobs
        self._fitted = True
        self._trained_at = datetime.now().isoformat()

        return {
            "total_jobs": len(valid_jobs),
            "feature_dim": int(self._job_vectors.shape[1]),
            "vocabulary_size": len(self.vectorizer.vocabulary_),
            "trained_at": self._trained_at,
            "message": f"TF-IDF 模型训练完成，共 {len(valid_jobs)} 条职位数据",
        }

    def compute_similarity(self, resume_text: str, job_items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if not self.is_ready():
            raise ValueError("模型尚未训练")
        if not resume_text or not resume_text.strip():
            return []

        resume_vec = self.vectorizer.transform([resume_text])
        results: List[Dict[str, Any]] = []

        for item in job_items:
            text = item.get("text") or self._build_job_text(item)
            if not text:
                score = 0.0
            else:
                job_vec = self.vectorizer.transform([text])
                score = float(cosine_similarity(resume_vec, job_vec)[0][0])

            result = dict(item)
            result["score"] = round(score, 4)
            results.append(result)

        results.sort(key=lambda x: x["score"], reverse=True)
        return results

    def evaluate(self, labeled_data: List[Dict[str, Any]], test_ratio: float = 0.2) -> Dict[str, Any]:
        if len(labeled_data) < 5:
            return {
                "error": "数据量不足，至少需要5条标注数据",
                "total_samples": len(labeled_data),
                "train_samples": 0,
                "test_samples": 0,
            }

        np.random.shuffle(labeled_data)
        split_idx = max(1, int(len(labeled_data) * (1 - test_ratio)))
        train_data = labeled_data[:split_idx]
        test_data = labeled_data[split_idx:]

        train_texts = []
        for item in train_data:
            merged = f"{item.get('resume_text', '')} {item.get('job_text', '')}".strip()
            if merged:
                train_texts.append(merged)

        if not train_texts:
            return {"error": "标注数据格式有误，无法提取有效文本"}

        self.vectorizer.fit(train_texts)
        self._fitted = True

        y_true: List[int] = []
        y_pred: List[int] = []
        for item in test_data:
            resume_text = item.get("resume_text", "")
            job_text = item.get("job_text", "")
            if not resume_text or not job_text:
                continue
            score = self.compute_pair_similarity(resume_text, job_text)
            pred_label = 1 if score >= 0.1 else 0
            y_true.append(int(item.get("label", 0)))
            y_pred.append(pred_label)

        if not y_true:
            return {"error": "测试集中无有效数据"}

        tp = sum(1 for t, p in zip(y_true, y_pred) if t == 1 and p == 1)
        fp = sum(1 for t, p in zip(y_true, y_pred) if t == 0 and p == 1)
        fn = sum(1 for t, p in zip(y_true, y_pred) if t == 1 and p == 0)
        tn = sum(1 for t, p in zip(y_true, y_pred) if t == 0 and p == 0)

        accuracy = (tp + tn) / len(y_true) if y_true else 0
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0
        f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

        return {
            "accuracy": round(accuracy, 4),
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1": round(f1, 4),
            "total_samples": len(labeled_data),
            "train_samples": len(train_data),
            "test_samples": len(test_data),
            "tp": tp,
            "fp": fp,
            "fn": fn,
            "tn": tn,
        }

    def compute_pair_similarity(self, resume_text: str, job_text: str) -> float:
        if not resume_text or not job_text:
            return 0.0
        resume_vec = self.vectorizer.transform([resume_text])
        job_vec = self.vectorizer.transform([job_text])
        return float(cosine_similarity(resume_vec, job_vec)[0][0])

    def _build_job_text(self, job: Dict[str, Any]) -> str:
        return " ".join(
            part.strip()
            for part in [
                str(job.get("job_name") or job.get("name") or ""),
                str(job.get("company_name") or job.get("companyName") or ""),
                str(job.get("responsibility") or ""),
                str(job.get("skills") or ""),
                str(job.get("city") or ""),
                str(job.get("education") or ""),
                str(job.get("major_keyword") or ""),
                str(job.get("industry") or ""),
                str(job.get("text") or ""),
            ]
            if part and part.strip()
        )


_model = TfidfSimilarityModel()
