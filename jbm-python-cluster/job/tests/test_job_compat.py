from fastapi.testclient import TestClient

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.job.main import create_app


def job_config() -> AppConfig:
    return AppConfig(
        {
            "server": {"host": "127.0.0.1", "port": 4444},
            "spring": {
                "application": {"name": "jbm-cluster-platform-job"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
            },
            "integrations": {"telemetry": {"enabled": False}},
        },
        profile="test",
        config_dir=None,
        app="job",
    )


def test_job_crud_run_and_logs() -> None:
    with TestClient(create_app(job_config())) as client:
        created = client.post(
            "/sysJob",
            json={
                "jobName": "demo",
                "jobGroup": "DEFAULT",
                "invokeTarget": "demo.run",
                "cronExpression": "0/5 * * * * ?",
            },
        )
        job_id = created.json()["result"]

        page = client.post("/sysJob/pageList", json={"sysJob": {"jobName": "demo"}, "pageForm": {"currPage": 1, "pageSize": 10}})
        assert page.json()["result"]["total"] == 1

        changed = client.put("/sysJob/changeStatus", json={"jobId": job_id, "status": "PAUSE"})
        assert changed.json()["result"]["status"] == "PAUSE"

        ran = client.put("/sysJob/run", json={"jobId": job_id})
        assert ran.json()["success"] is True

        logs = client.post("/sysJobLog/pageList", json={"pageForm": {"currPage": 1, "pageSize": 10}})
        assert logs.json()["result"]["total"] == 1

        deleted = client.delete("/sysJob/%s" % job_id)
        assert deleted.json()["result"] == 1
