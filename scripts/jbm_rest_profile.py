# -*- coding: utf-8 -*-
"""REST test runners: unified Spring profile."""
from pathlib import Path

REST_PROFILE = "jaja7"


def apply_rest_profile(cfg, cli_profile=None):
    """Force profile jaja7 on config dict; return active profile."""
    requested = (cfg.get("profile") or cli_profile or REST_PROFILE)
    if isinstance(requested, str):
        requested = requested.strip()
    if requested and requested != REST_PROFILE:
        print(f"[profile] use {REST_PROFILE} (ignore {requested!r})")
    cfg["profile"] = REST_PROFILE
    return REST_PROFILE


def docs_dir(root, suite_slug, profile=None):
    profile = profile or REST_PROFILE
    path = Path(root) / "docs/testing" / f"{suite_slug}-{profile}"
    path.mkdir(parents=True, exist_ok=True)
    (path / "modules").mkdir(exist_ok=True)
    return path


def spring_boot_profile_arg(profile=None):
    return f"-Dspring-boot.run.profiles={profile or REST_PROFILE}"