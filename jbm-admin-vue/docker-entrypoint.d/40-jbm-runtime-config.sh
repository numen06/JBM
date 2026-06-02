#!/bin/sh
set -eu

api_prefix="${JBM_API_PREFIX:-/v3/api/}"
gateway_upstream="${JBM_GATEWAY_UPSTREAM:-jbm-cluster-platform-gateway:6060}"

api_prefix="/$(printf '%s' "$api_prefix" | sed 's#^/*##; s#/*$##')/"
api_prefix_no_slash="$(printf '%s' "$api_prefix" | sed 's#/$##')"
api_prefix_regex="$(printf '%s' "$api_prefix" | sed 's#[.[\*^$()+?{}|]#\\&#g')"

case "$gateway_upstream" in
  http://*|https://*) gateway_url="$gateway_upstream" ;;
  *) gateway_url="http://$gateway_upstream" ;;
esac

js_api_prefix="$(printf '%s' "$api_prefix" | sed 's#\\#\\\\#g; s#"#\\"#g')"
cat > /usr/share/nginx/html/env.js <<EOF
window.JBM_ADMIN_CONFIG = {
  apiBaseUrl: "$js_api_prefix"
}
EOF

cat > /etc/nginx/conf.d/default.conf <<EOF
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    resolver 127.0.0.11 valid=10s ipv6=off;
    set \$gateway_url $gateway_url;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location = $api_prefix_no_slash {
        return 308 $api_prefix;
    }

    location $api_prefix {
        rewrite ^$api_prefix_regex(.*)\$ /\$1 break;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_pass \$gateway_url;
    }
}
EOF
