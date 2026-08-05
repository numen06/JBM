from __future__ import annotations

import uvicorn

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.center.bootstrap.app import create_app


app = create_app()


def run() -> None:
    config = AppConfig.load(app="center")
    uvicorn.run(
        "jbm_cluster_py.platform.center.main:app",
        host=config.host,
        port=config.port,
        reload=False,
    )


if __name__ == "__main__":
    run()
