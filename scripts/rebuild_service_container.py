#!/usr/bin/env python3
import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]
DEFAULT_COMPOSE_FILE = "/home/opt/portainer/compose/7/docker-compose.yml"
DEFAULT_COMPOSE_PROJECT = "jbm73"
DEFAULT_IMAGE_PREFIX = "registry.cn-hangzhou.aliyuncs.com/51jbm"
DEFAULT_IMAGE_TAG = "7.3"

MODULES = {
    "jbm-cluster-platform-center": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center",
    "jbm-cluster-platform-auth": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth",
    "jbm-cluster-platform-doc": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-doc",
    "jbm-cluster-platform-gateway": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway",
    "jbm-cluster-platform-logs": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-logs",
    "jbm-cluster-platform-push": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-push",
    "jbm-cluster-platform-bigscreen": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-bigscreen",
    "jbm-cluster-platform-job": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job",
    "jbm-cluster-platform-weixin": "jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-weixin",
}

ALIASES = {
    "center": "jbm-cluster-platform-center",
    "auth": "jbm-cluster-platform-auth",
    "doc": "jbm-cluster-platform-doc",
    "gateway": "jbm-cluster-platform-gateway",
    "logs": "jbm-cluster-platform-logs",
    "push": "jbm-cluster-platform-push",
    "bigscreen": "jbm-cluster-platform-bigscreen",
    "job": "jbm-cluster-platform-job",
    "weixin": "jbm-cluster-platform-weixin",
    "admin": "jbm-admin",
}

KNOWN_SERVICES = sorted(set(MODULES) | {"jbm-admin"})


def run(cmd, cwd=ROOT_DIR, input_text=None, capture=False):
    print("+ " + " ".join(str(part) for part in cmd), flush=True)
    stdout = subprocess.PIPE if capture else None
    stderr = subprocess.PIPE if capture else None
    return subprocess.run(
        [str(part) for part in cmd],
        cwd=str(cwd),
        input=input_text,
        check=True,
        stdout=stdout,
        stderr=stderr,
        universal_newlines=True,
    )


def env_value(name, default):
    return os.environ.get(name, default)


def image_for(service, image_prefix, image_tag):
    return f"{image_prefix}/{service}:{image_tag}"


def normalize_service(value):
    if value == "all":
        return value
    return ALIASES.get(value, value)


def compose_cmd(args, service=None):
    cmd = ["docker", "compose", "-p", args.compose_project, "-f", args.compose_file]
    if service is None:
        return cmd
    return cmd + [service]


def compose_services(args):
    result = run(compose_cmd(args) + ["config", "--services"], capture=True)
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def compose_has_service(args, service):
    return service in set(compose_services(args))


def package_backend(service):
    module = MODULES[service]
    print(f"==> Maven package: {service} ({module})", flush=True)
    run(["mvn", "-pl", module, "-am", "-DskipTests", "package"])


def build_backend_overlay_image(args, service):
    image = image_for(service, args.image_prefix, args.image_tag)
    jar = ROOT_DIR / "dist" / f"{service}.jar"
    if not jar.is_file():
        raise SystemExit(f"Missing jar: {jar}")

    print(f"==> Docker overlay image: {image}", flush=True)
    with tempfile.TemporaryDirectory(prefix=f"jbm-{service}-") as tmp:
        tmp_path = Path(tmp)
        shutil.copy2(jar, tmp_path / "app.jar")
        dockerfile = f"FROM {image}\nCOPY app.jar /app/app.jar\n"
        run(["docker", "build", "-t", image, "-f", "-", tmp_path], input_text=dockerfile)


def build_backend_full_image(args, service):
    image = image_for(service, args.image_prefix, args.image_tag)
    module = MODULES[service]
    build_args = f"-pl {module} -am -DskipTests package"
    print(f"==> Docker full image: {image}", flush=True)
    run(
        [
            "docker",
            "build",
            "--network=host",
            "-f",
            "allinone.Dockerfile",
            "--target",
            service,
            "--build-arg",
            f"MAVEN_BUILD_ARGS={build_args}",
            "-t",
            image,
            ".",
        ]
    )


def build_admin_overlay_image(args):
    image = image_for("jbm-admin", args.image_prefix, args.image_tag)
    dist = ROOT_DIR / "jbm-admin-vue" / "dist"
    print("==> npm build: jbm-admin-vue", flush=True)
    run(["npm", "run", "build"], cwd=ROOT_DIR / "jbm-admin-vue")
    if not dist.is_dir():
        raise SystemExit(f"Missing dist: {dist}")
    for path in dist.rglob("*"):
        path.chmod(0o755 if path.is_dir() else 0o644)

    print(f"==> Docker overlay image: {image}", flush=True)
    with tempfile.TemporaryDirectory(prefix="jbm-admin-") as tmp:
        tmp_path = Path(tmp)
        shutil.copytree(dist, tmp_path / "dist")
        dockerfile = f"FROM {image}\nCOPY dist /usr/share/nginx/html\n"
        run(["docker", "build", "-t", image, "-f", "-", tmp_path], input_text=dockerfile)


def build_admin_image(args):
    image = image_for("jbm-admin", args.image_prefix, args.image_tag)
    print(f"==> Docker image: {image}", flush=True)
    run(
        [
            "docker",
            "build",
            "--network=host",
            "-f",
            "allinone.Dockerfile",
            "--target",
            "jbm-admin",
            "-t",
            image,
            ".",
        ]
    )


def recreate_container(args, service):
    if not compose_has_service(args, service):
        print(f"==> Skip compose recreate: {service} is not in {args.compose_file}", flush=True)
        return
    print(f"==> Compose recreate: {service}", flush=True)
    run(compose_cmd(args) + ["up", "-d", "--no-deps", "--force-recreate", service], cwd=Path(args.compose_file).parent)


def show_status(args, service):
    print(f"==> Status: {service}", flush=True)
    try:
        run(compose_cmd(args) + ["ps", service], cwd=Path(args.compose_file).parent)
    except subprocess.CalledProcessError:
        pass


def show_logs(args, service):
    if not compose_has_service(args, service):
        return
    print(f"==> Logs: {service}", flush=True)
    try:
        run(compose_cmd(args) + ["logs", "--tail", str(args.tail_lines), service], cwd=Path(args.compose_file).parent)
    except subprocess.CalledProcessError:
        pass


def rebuild_one(args, service):
    service = normalize_service(service)
    if service not in KNOWN_SERVICES:
        known = ", ".join(["all"] + sorted(ALIASES) + KNOWN_SERVICES)
        raise SystemExit(f"Unknown service: {service}\nKnown: {known}")

    if service == "jbm-admin":
        if args.mode == "full":
            build_admin_image(args)
        else:
            build_admin_overlay_image(args)
    elif args.mode == "full":
        build_backend_full_image(args, service)
    else:
        package_backend(service)
        build_backend_overlay_image(args, service)

    if not args.no_up:
        recreate_container(args, service)
        if args.wait_seconds > 0:
            time.sleep(args.wait_seconds)
    if args.status:
        show_status(args, service)
    if args.logs:
        show_logs(args, service)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Build a JBM service image and recreate its Docker Compose container."
    )
    parser.add_argument("target", help="service alias/name or all")
    parser.add_argument("--full", dest="mode", action="store_const", const="full", help="full Docker build")
    parser.add_argument("--overlay", dest="mode", action="store_const", const="overlay", help="package jar and overlay existing image")
    parser.add_argument("--no-up", action="store_true", help="build image only, do not recreate container")
    parser.add_argument("--logs", action="store_true", help="print recent compose logs after recreate")
    parser.add_argument("--no-status", dest="status", action="store_false", help="skip compose ps")
    parser.set_defaults(status=True)
    parser.add_argument("--compose-file", default=env_value("COMPOSE_FILE", DEFAULT_COMPOSE_FILE))
    parser.add_argument("--compose-project", default=env_value("COMPOSE_PROJECT", DEFAULT_COMPOSE_PROJECT))
    parser.add_argument("--image-prefix", default=env_value("IMAGE_PREFIX", DEFAULT_IMAGE_PREFIX))
    parser.add_argument("--image-tag", default=env_value("IMAGE_TAG", DEFAULT_IMAGE_TAG))
    parser.add_argument("--wait-seconds", type=int, default=int(env_value("WAIT_SECONDS", "45")))
    parser.add_argument("--tail-lines", type=int, default=int(env_value("TAIL_LINES", "160")))
    parser.set_defaults(mode=env_value("MODE", "overlay"))
    return parser.parse_args()


def main():
    args = parse_args()
    if args.mode not in {"overlay", "full"}:
        raise SystemExit("MODE must be overlay or full")

    target = normalize_service(args.target)
    if target == "all":
        services = [service for service in compose_services(args) if service in KNOWN_SERVICES]
        for service in services:
            rebuild_one(args, service)
        return
    rebuild_one(args, target)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        sys.exit(exc.returncode)
