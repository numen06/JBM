"""Fetch pinned public ip2region data; never send login IPs to a third party."""

import hashlib
from pathlib import Path
from urllib.request import urlopen

COMMIT = "f8b0792b0cd729f5b2593f690aeed225e49c1967"
FILES = {
    "ip2region_v4.xdb": "27ecba8c9a6d5e3b280aedbe7ee15c65372b179fbd0dad3845d8c608f2fcace1",
    "ip2region_v6.xdb": "939f6b46bd2b8bec3cf7c5ceb8ba782266ae9b1f35b5ba7916700dec0b7506ed",
}
MIRROR = {
    'ip2region_v4.xdb': 'https://numen-share.oss-cn-shanghai.aliyuncs.com/jbm-build/ip2region/f8b0792b0cd729f5b2593f690aeed225e49c1967/ip2region_v4-20261025-140910.xdb',
    'ip2region_v6.xdb': 'https://numen-share.oss-cn-shanghai.aliyuncs.com/jbm-build/ip2region/f8b0792b0cd729f5b2593f690aeed225e49c1967/ip2region_v6-20261025-140912.xdb',
}

if __name__ == "__main__":
    directory = Path(__file__).parent / "data"
    directory.mkdir(exist_ok=True)
    for name, expected in FILES.items():
        target = directory / name
        if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == expected:
            continue
        # A controlled mirror avoids unbounded upstream DNS resolution in BuildKit.
        # These bytes are still validated against the pinned upstream commit hashes.
        data = urlopen(MIRROR[name], timeout=60).read()
        if hashlib.sha256(data).hexdigest() != expected:
            raise RuntimeError("IP region data checksum mismatch: " + name)
        temporary = target.with_suffix(".download")
        temporary.write_bytes(data)
        temporary.replace(target)
