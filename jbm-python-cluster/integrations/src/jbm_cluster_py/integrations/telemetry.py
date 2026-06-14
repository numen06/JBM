import logging
from typing import Any, Mapping

logger = logging.getLogger(__name__)


def init_telemetry(config: Mapping[str, Any]) -> None:
    if not config.get("enabled"):
        return
    try:
        from openobserve import openobserve_init
    except ImportError:
        logger.warning("openobserve-telemetry-sdk is not installed; skip telemetry init")
        return
    openobserve_init()
