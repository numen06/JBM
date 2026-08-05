#!/bin/sh
set -eu

base_url="http://rnacos:8848/nacos/v1"
until curl -fsS "$base_url/ns/operator/metrics" >/dev/null; do
  sleep 2
done

curl -fsS -X POST "$base_url/console/namespaces" \
  --data-urlencode "customNamespaceId=jbm7" \
  --data-urlencode "namespaceName=jbm7" \
  --data-urlencode "namespaceDesc=JBM 7.3 Python local Docker" >/dev/null || true
