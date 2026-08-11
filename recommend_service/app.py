"""
TF-IDF 推荐服务 (FastAPI)
启动: uvicorn app:app --host 0.0.0.0 --port 8000
"""

from datetime import datetime
from typing import Any, Dict, List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from model import _model

app = FastAPI(title="TF-IDF 推荐相似度服务", version="2.0")


class TrainJob(BaseModel):
    job_id: Optional[int] = None
    job_name: str = ""
    company_name: str = ""
    responsibility: Optional[str] = ""
    skills: Optional[str] = ""
    city: Optional[str] = ""
    salary: Optional[str] = ""
    education: Optional[str] = ""
    major_keyword: Optional[str] = ""
    industry: Optional[str] = ""
    source: Optional[str] = ""


class TrainRequest(BaseModel):
    jobs: List[TrainJob]


class SimilarityRequest(BaseModel):
    resume_text: str
    job_texts: List[Dict[str, Any]]


class LabeledSample(BaseModel):
    resume_text: str
    job_text: str
    label: int


class EvaluateRequest(BaseModel):
    samples: List[LabeledSample]
    test_ratio: float = 0.2


@app.get("/")
def root() -> Dict[str, Any]:
    status = _model.status()
    return {
        "service": "TF-IDF 推荐相似度服务",
        "version": "2.0",
        "model_status": "已训练" if status["model_loaded"] else "未训练",
        "status": status,
        "endpoints": ["/train", "/similarity", "/status", "/evaluate", "/health"],
    }


@app.post("/train")
def train(req: TrainRequest) -> Dict[str, Any]:
    if not req.jobs:
        raise HTTPException(status_code=400, detail="训练数据为空，请先准备职位数据")

    jobs = [job.model_dump() for job in req.jobs]
    result = _model.train_jobs(jobs)
    return {
        "success": True,
        **result,
    }


@app.post("/similarity")
def compute_similarity(req: SimilarityRequest) -> Dict[str, Any]:
    if not req.resume_text or not req.resume_text.strip():
        raise HTTPException(status_code=400, detail="简历文本为空")

    scores = _model.compute_similarity(req.resume_text, req.job_texts)
    return {
        "success": True,
        "body": scores,
        "count": len(scores),
        "model_loaded": _model.is_ready(),
    }


@app.get("/status")
def status() -> Dict[str, Any]:
    return _model.status()


@app.post("/evaluate")
def evaluate_model(req: EvaluateRequest) -> Dict[str, Any]:
    labeled_data = [sample.model_dump() for sample in req.samples]
    return _model.evaluate(labeled_data, req.test_ratio)


@app.get("/health")
def health_check() -> Dict[str, Any]:
    return {
        "status": "ok",
        "model_loaded": _model.is_ready(),
        "checked_at": datetime.now().isoformat(),
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
