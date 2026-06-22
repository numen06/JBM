#!/bin/sh
set -eu

api_prefix="${JBM_API_PREFIX:-/v3/api/}"
gateway_port="${JBM_GATEWAY_PORT:-6060}"
gateway_upstream="${JBM_GATEWAY_UPSTREAM:-jbm-cluster-platform-gateway:${gateway_port}}"
auth_upstream="${JBM_AUTH_UPSTREAM:-jbm-cluster-platform-auth:5555}"
push_ws_upstream="${JBM_PUSH_WS_UPSTREAM:-jbm-cluster-platform-push:3313}"
admin_debug="${JBM_ADMIN_DEBUG:-false}"
local_dev_login="${JBM_LOCAL_DEV_LOGIN:-$admin_debug}"
local_dev_password="${JBM_LOCAL_DEV_PASSWORD:-Admin@123}"
local_dev_users="${JBM_LOCAL_DEV_USERS:-}"
oauth_client_id="${JBM_OAUTH_CLIENT_ID:-}"
oauth_client_secret="${JBM_OAUTH_CLIENT_SECRET:-}"
oauth_authorize_base_url="${JBM_OAUTH_AUTHORIZE_BASE_URL:-/auth-center}"
login_password="${JBM_LOGIN_PASSWORD:-$local_dev_password}"

api_prefix="/$(printf '%s' "$api_prefix" | sed 's#^/*##; s#/*$##')/"
api_prefix_no_slash="$(printf '%s' "$api_prefix" | sed 's#/$##')"
api_prefix_regex="$(printf '%s' "$api_prefix" | sed 's#[.[\*^$()+?{}|]#\\&#g')"

case "$gateway_upstream" in
  http://*|https://*) gateway_url="$gateway_upstream" ;;
  *) gateway_url="http://$gateway_upstream" ;;
esac
case "$auth_upstream" in
  http://*|https://*) auth_url="$auth_upstream" ;;
  *) auth_url="http://$auth_upstream" ;;
esac
case "$push_ws_upstream" in
  http://*|https://*) push_ws_url="$push_ws_upstream" ;;
  *) push_ws_url="http://$push_ws_upstream" ;;
esac

json_escape() {
  printf '%s' "$1" | sed 's#\\#\\\\#g; s#"#\\"#g'
}

js_api_prefix="$(json_escape "$api_prefix")"
js_admin_debug="$(json_escape "$admin_debug")"
js_local_dev_login="$(json_escape "$local_dev_login")"
js_local_dev_password="$(json_escape "$local_dev_password")"
js_local_dev_users="$(json_escape "$local_dev_users")"
js_oauth_client_id="$(json_escape "$oauth_client_id")"
js_oauth_client_secret="$(json_escape "$oauth_client_secret")"
js_oauth_authorize_base_url="$(json_escape "$oauth_authorize_base_url")"
js_login_password="$(json_escape "$login_password")"
cat > /usr/share/nginx/html/env.js <<EOF
window.JBM_ADMIN_CONFIG = {
  apiBaseUrl: "$js_api_prefix",
  debug: "$js_admin_debug",
  localDevLogin: "$js_local_dev_login",
  localDevPassword: "$js_local_dev_password",
  localDevUsers: "$js_local_dev_users",
  oauthClientId: "$js_oauth_client_id",
  oauthClientSecret: "$js_oauth_client_secret",
  oauthAuthorizeBaseUrl: "$js_oauth_authorize_base_url",
  loginPassword: "$js_login_password"
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
    set \$auth_url $auth_url;
    set \$push_ws_url $push_ws_url;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location = /env.js {
        add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0" always;
        add_header Pragma "no-cache" always;
        add_header Expires "0" always;
        try_files /env.js =404;
    }

    location = /auth-center {
        return 308 /auth-center/;
    }

    location /auth-center/ {
        rewrite ^/auth-center/(.*)\$ /\$1 break;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 3600s;
        proxy_pass \$auth_url;
    }

    location = $api_prefix_no_slash {
        return 308 $api_prefix;
    }

    location = ${api_prefix}push/ws {
        rewrite ^${api_prefix_regex}push/ws\$ /ws break;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 3600s;
        proxy_pass \$push_ws_url;
    }

    location $api_prefix {
        rewrite ^$api_prefix_regex(.*)\$ /\$1 break;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 3600s;
        proxy_pass \$gateway_url;
    }
}
EOF
