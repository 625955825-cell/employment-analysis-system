"""
TF-IDF 推荐服务 (FastAPI)
启动: uvicorn main:app --port 8000
"""

from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from model import _model

app = FastAPI(title="TF-IDF 推荐相似度服务", version="1.0")


class SimilarityRequest(BaseModel):
    resume_text: str
    job_texts: List[Dict[str, Any]]  # [{"id": 1, "text": "...", "name": "...", "salary": "...", ...}, ...]


class SimilarityJobResult(BaseModel):
    id: Any
    score: float
    name: Optional[str] = None
    salary: Optional[str] = None
    city: Optional[str] = None
    education: Optional[str] = None
    companyName: Optional[str] = None
    industry: Optional[str] = None
    detailUrl: Optional[str] = None
    responsibility: Optional[str] = None
    source: Optional[str] = None
    positionSource: Optional[str] = None


class LabeledSample(BaseModel):
    resume_text: str
    job_text: str
    label: int  # 1=正样本(合适), 0=负样本(不合适)


class EvaluateRequest(BaseModel):
    samples: List[LabeledSample]
    test_ratio: float = 0.2


@app.post("/similarity")
def compute_similarity(req: SimilarityRequest):
    """
    计算简历与职位列表的文本相似度，返回排序后的结果
    """
    job_texts = [item.get("text", "") for item in req.job_texts]
    scores = _model.compute_similarity(req.resume_text, job_texts)

    results = []
    for item, score in zip(req.job_texts, scores):
        result = {
            "id": item.get("id"),
            "score": score,
            "name": item.get("name"),
            "salary": item.get("salary"),
            "city": item.get("city"),
            "education": item.get("education"),
            "companyName": item.get("companyName"),
            "industry": item.get("industry"),
            "detailUrl": item.get("detailUrl"),
            "responsibility": item.get("responsibility"),
            "source": item.get("source"),
            "positionSource": item.get("positionSource")
        }
        results.append(result)

    results.sort(key=lambda x: x["score"], reverse=True)
    return {"body": results, "count": len(results)}


@app.post("/evaluate")
def evaluate_model(req: EvaluateRequest) -> Dict[str, Any]:
    """
    评估 TF-IDF 模型性能
    """
    labeled_data = [
        {"resume_text": s.resume_text, "job_text": s.job_text, "label": s.label}
        for s in req.samples
    ]
    return _model.evaluate(labeled_data, req.test_ratio)


@app.get("/health")
def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
