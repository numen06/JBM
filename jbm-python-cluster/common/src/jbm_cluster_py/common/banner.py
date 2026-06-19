from __future__ import annotations

import os


_printed = False


JBM_BANNER = r"""  .    _      __        __           ____         _          __ _ _
 /\  | | /| / /__ ___ / /__ __ __  / __/__  ____(_)__  ___  \ \ \ \
( ( ) | |/ |/ / -_|_-</ / -_) // / _\ \/ _ \/ __/ / _ \/ _ `/ > > > >
 \/  |__/|__/\__/___/_/\__/\_, / /___/ .__/_/ /_/_//_/\_, / /_/_/_/
  '                        /___/     /_/              /___/
 :: Prower By Wesley & Python / FastAPI :: ===== (Python)
"""


def print_jbm_banner() -> None:
    global _printed
    if _printed:
        return
    if os.getenv("JBM_BANNER", "true").lower() in {"0", "false", "off", "no"}:
        _printed = True
        return
    print(JBM_BANNER, flush=True)
    _printed = True
