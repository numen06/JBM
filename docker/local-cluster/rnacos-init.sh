#!/bin/sh
set -eu

base_url="http://rnacos:8848/nacos/v1"
namespace="jbm-py"
until curl -fsS "$base_url/ns/operator/metrics" >/dev/null; do
  sleep 2
done

curl -fsS -X POST "$base_url/console/namespaces" \
  --data-urlencode "customNamespaceId=$namespace" \
  --data-urlencode "namespaceName=$namespace" \
  --data-urlencode "namespaceDesc=JBM 7.3 Python local Docker" >/dev/null || true

for file in /init/nacos-configs/*-dev.yml; do
  data_id=${file##*/}
  curl -fsS -X POST "$base_url/cs/configs" \
    --data-urlencode "tenant=$namespace" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file" >/dev/null
done
