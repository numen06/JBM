FROM m.daocloud.io/docker.io/python:3.12 AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/ \
    PIP_TRUSTED_HOST=mirrors.aliyun.com \
    JBM_APP=center \
    JBM_PROFILE=dev \
    PYTHONPATH=/app/common/src:/app/integrations/src:/app/auth/src:/app/doc/src:/app/push/src:/app/job/src:/app/gateway/src:/app/center/src:/app/logs/src:/app/bigscreen/src

WORKDIR /app

RUN set -eux; \
    . /etc/os-release; \
    printf '%s\n' \
      'Types: deb' \
      'URIs: https://mirrors.aliyun.com/debian' \
      "Suites: ${VERSION_CODENAME} ${VERSION_CODENAME}-updates" \
      'Components: main' \
      'Signed-By: /usr/share/keyrings/debian-archive-keyring.gpg' \
      '' \
      'Types: deb' \
      'URIs: https://mirrors.aliyun.com/debian-security' \
      "Suites: ${VERSION_CODENAME}-security" \
      'Components: main' \
      'Signed-By: /usr/share/keyrings/debian-archive-keyring.gpg' \
      > /etc/apt/sources.list.d/debian.sources; \
    apt-get update; \
    rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /usr/sbin/nologin appuser
RUN mkdir -p /app/nacos-data/snapshot /app/nacos-data/failover /app/data && chown -R appuser:appuser /app/nacos-data /app/data

COPY --chown=appuser:appuser pyproject.toml requirements.txt README.md ./

RUN pip install -r requirements.txt

COPY --chown=appuser:appuser common ./common
COPY --chown=appuser:appuser integrations ./integrations
COPY --chown=appuser:appuser auth ./auth
COPY --chown=appuser:appuser doc ./doc
COPY --chown=appuser:appuser push ./push
COPY --chown=appuser:appuser job ./job
COPY --chown=appuser:appuser gateway ./gateway
COPY --chown=appuser:appuser center ./center
COPY --chown=appuser:appuser logs ./logs
COPY --chown=appuser:appuser bigscreen ./bigscreen

USER appuser

EXPOSE 3312 3313 3314 4444 5555 6060 7777 9999

CMD ["sh", "-c", "python -m jbm_cluster_py.platform.${JBM_APP}.main"]
