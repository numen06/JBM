from __future__ import annotations

import time
from datetime import datetime, timezone
from typing import Any, Dict, Iterable, List, Optional

from jbm_cluster_py.common.result import page_result


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class JobService:
    def __init__(self) -> None:
        self.jobs: List[Dict[str, Any]] = []
        self.logs: List[Dict[str, Any]] = []
        self._next_job_id = 1
        self._next_log_id = 1

    def page_jobs(self, body: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        payload = body or {}
        query = payload.get("sysJob") or payload.get("entity") or payload
        return self._page(self._filter(self.jobs, query, ("jobName", "jobGroup", "invokeTarget", "status")), payload)

    def page_logs(self, body: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        payload = body or {}
        query = payload.get("sysJobLog") or payload.get("entity") or payload
        return self._page(self._filter(self.logs, query, ("jobName", "jobGroup", "invokeTarget", "status")), payload)

    def get_job(self, job_id: int) -> Optional[Dict[str, Any]]:
        return next((row for row in self.jobs if int(row.get("jobId") or 0) == job_id), None)

    def create_job(self, data: Dict[str, Any]) -> int:
        row = dict(data or {})
        row["jobId"] = self._next_job_id
        row["id"] = row["jobId"]
        self._next_job_id += 1
        row.setdefault("jobGroup", "DEFAULT")
        row.setdefault("status", "NORMAL")
        row.setdefault("misfirePolicy", "DEFAULT")
        row.setdefault("concurrent", False)
        row.setdefault("recordLog", True)
        row.setdefault("createTime", now_iso())
        row["updateTime"] = now_iso()
        self.jobs.append(row)
        return int(row["jobId"])

    def update_job(self, job_id: int, data: Dict[str, Any]) -> bool:
        row = self.get_job(job_id)
        if row is None:
            return False
        row.update(dict(data or {}))
        row["jobId"] = job_id
        row["id"] = job_id
        row["updateTime"] = now_iso()
        return True

    def delete_job(self, job_id: int) -> int:
        before = len(self.jobs)
        self.jobs = [row for row in self.jobs if int(row.get("jobId") or 0) != job_id]
        return 1 if len(self.jobs) != before else 0

    def change_status(self, data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        job_id = int(data.get("jobId") or data.get("id") or 0)
        row = self.get_job(job_id)
        if row is None:
            return None
        row["status"] = data.get("status") or row.get("status")
        row["updateTime"] = now_iso()
        return dict(row)

    def run_job(self, data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        job_id = int(data.get("jobId") or data.get("id") or 0)
        row = self.get_job(job_id)
        if row is None:
            return None
        start = time.time()
        log = {
            "jobLogId": self._next_log_id,
            "id": self._next_log_id,
            "jobName": row.get("jobName"),
            "jobGroup": row.get("jobGroup") or "DEFAULT",
            "invokeTarget": row.get("invokeTarget"),
            "jobMessage": "Python job compatibility runner accepted the task.",
            "status": "SUCCESS",
            "exceptionInfo": "",
            "startTime": now_iso(),
            "stopTime": None,
            "createTime": now_iso(),
        }
        self._next_log_id += 1
        log["runTime"] = int((time.time() - start) * 1000)
        log["stopTime"] = now_iso()
        self.logs.insert(0, log)
        return dict(row)

    def clean_logs(self) -> None:
        self.logs.clear()

    def save_master(self, rows: List[Dict[str, Any]], data: Dict[str, Any], key: str) -> Dict[str, Any]:
        row = dict(data or {})
        if not row.get(key):
            row[key] = len(rows) + 1
            row["id"] = row[key]
            row["createTime"] = now_iso()
            rows.append(row)
        else:
            for index, existing in enumerate(rows):
                if existing.get(key) == row.get(key):
                    row = {**existing, **row}
                    rows[index] = row
                    break
            else:
                rows.append(row)
        row["updateTime"] = now_iso()
        return row

    def delete_ids(self, rows: List[Dict[str, Any]], ids: Iterable[Any], key: str = "id") -> bool:
        id_set = {int(item) for item in ids if str(item).isdigit()}
        before = len(rows)
        rows[:] = [row for row in rows if int(row.get(key) or row.get("id") or 0) not in id_set]
        return len(rows) != before

    def _filter(self, rows: List[Dict[str, Any]], query: Dict[str, Any], keys: tuple[str, ...]) -> List[Dict[str, Any]]:
        result = list(rows)
        keyword = str(query.get("keyword") or "").strip().lower()
        for key in keys:
            value = query.get(key)
            if value in (None, ""):
                continue
            if key == "status":
                result = [row for row in result if str(row.get(key) or "") == str(value)]
            else:
                result = [row for row in result if str(value).lower() in str(row.get(key) or "").lower()]
        if keyword:
            result = [row for row in result if keyword in str(row).lower()]
        result.sort(key=lambda row: str(row.get("updateTime") or row.get("createTime") or ""), reverse=True)
        return result

    def _page(self, rows: List[Dict[str, Any]], payload: Dict[str, Any]) -> Dict[str, Any]:
        page_form = payload.get("pageForm") or {}
        curr_page = max(int(page_form.get("currPage") or 1), 1)
        page_size = max(int(page_form.get("pageSize") or 20), 1)
        start = (curr_page - 1) * page_size
        return page_result(rows[start : start + page_size], len(rows), curr_page, page_size)
