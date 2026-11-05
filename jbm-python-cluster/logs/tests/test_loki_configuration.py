from pathlib import Path
import yaml


def test_log_storage_survives_recreation_and_can_verify_old_backfill():
    root = Path(__file__).resolve().parents[3]
    config = yaml.safe_load((root / "docker/local-cluster/loki/config.yml").read_text(encoding="utf8"))
    prefix = config["common"]["path_prefix"]
    assert prefix.startswith("/tmp/loki/")
    for path in config["common"]["storage"]["filesystem"].values():
        assert path.startswith(prefix + "/")
    # Loki 3.7's single-binary filesystem mode overrides a zero querier lookback;
    # its exact infinite sentinel is -1 nanosecond, not -1 second.
    assert config["querier"]["query_ingesters_within"] == "0s"
    assert config["ingester"]["query_store_max_look_back_period"] == "-1ns"
    assert config["limits_config"]["retention_period"] == "8760h"
    assert config["limits_config"]["deletion_mode"] == "disabled"
