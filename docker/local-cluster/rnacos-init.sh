#!/bin/sh
set -eu

base_url="http://rnacos:8848/nacos/v1"
namespace="jbm-py"
username=${RNACOS_USERNAME:?RNACOS_USERNAME is required}
password=${RNACOS_PASSWORD:?RNACOS_PASSWORD is required}
token=""
until [ -n "$token" ]; do
  login=$(curl -fsS -X POST "$base_url/auth/login" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$password" 2>/dev/null || true)
  token=$(printf '%s' "$login" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
  [ -n "$token" ] && break
  sleep 2
done

curl -fsS -X POST "$base_url/console/namespaces" \
  --data-urlencode "accessToken=$token" \
  --data-urlencode "customNamespaceId=$namespace" \
  --data-urlencode "namespaceName=$namespace" \
  --data-urlencode "namespaceDesc=JBM 7.3 Python local Docker" >/dev/null || true

for file in /init/nacos-configs/*-dev.yml; do
  data_id=${file##*/}
  curl -fsS -X POST "$base_url/cs/configs" \
    --data-urlencode "accessToken=$token" \
    --data-urlencode "tenant=$namespace" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file" >/dev/null
done
