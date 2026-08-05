import sqlite3

import httpx
import pytest

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.center.bootstrap.app import create_app
from jbm_cluster_py.platform.center.modules.governance.api.compatibility_router import route_set


@pytest.mark.asyncio
async def test_center_core_compatibility(tmp_path) -> None:
    database = tmp_path / "center.db"
    _seed(database)
    config = AppConfig(
        {
            "server": {"port": 17777},
            "spring": {
                "application": {"name": "jbm-cluster-platform-center-py"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
            },
            "integrations": {"database": {"url": f"sqlite+aiosqlite:///{database.as_posix()}"}},
            "jbm": {
                "center": {"security": {"enabled": False}},
                "python": {"openapi": {"title": "Center Test"}},
            },
        },
        "test",
        None,
        app="center",
    )

    app = create_app(config)
    async with app.router.lifespan_context(app):
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            assert (await client.get("/actuator/health")).json()["status"] == "UP"

            current = (await client.get("/current/user")).json()["result"]
            assert current["userName"] == "admin"
            assert current["roles"][0]["roleCode"] == "super_admin"
            assert current["authorities"][0]["authority"] == "MENU_dashboard"

            menus = (await client.get("/current/user/menus")).json()["result"]
            assert menus[0]["menuCode"] == "dashboard"

            users = (await client.get("/user", params={"pageForm.currPage": 1, "pageForm.pageSize": 10})).json()["result"]
            assert users["total"] == 1
            assert users["contents"][0]["companyId"] == 1

            orgs = (await client.post("/baseOrg/root", json={})).json()["result"]
            assert orgs == [{"id": 1, "parentId": None, "orgName": "默认组织", "orgCode": "default", "status": 1, "level": 0}]

            dictionaries = (
                await client.post(
                    "/baseDic/root/pageList",
                    json={"pageForm": {"currPage": 1, "pageSize": 10}, "baseDic": {}},
                )
            ).json()["result"]
            assert dictionaries["contents"][0]["code"] == "sys_status"

            assert (await client.get("/app", params={"pageForm.currPage": 1})).json()["result"]["total"] == 1
            assert (await client.get("/role/all")).json()["result"][0]["roleCode"] == "super_admin"
            assert (await client.get("/user/1/roles")).json()["result"][0]["roleCode"] == "super_admin"
            assert (await client.get("/user/1/orgs")).json()["result"][0]["orgId"] == 1
            assert (await client.get("/user/1/accounts")).json()["result"][0].get("password") is None
            assert (await client.get("/baseDic/getDicMap")).json()["result"]["sys_status"][0]["code"] == "1"
            assert (
                await client.post("/user/sessions", json={"username": "admin", "password": "wrong"})
            ).json()["success"] is False
            assert (await client.get("/gateway/routes")).json()["result"]["total"] == 1
            assert (await client.get("/not-migrated")).status_code == 404

            routes = list(app.routes)
            for route in list(routes):
                nested = getattr(route, "original_router", None)
                if nested is not None:
                    routes.extend(nested.routes)
            implemented = {
                (method, route.path)
                for route in routes
                for method in getattr(route, "methods", set())
            }
            assert route_set() <= implemented
            assert len(route_set()) == 191


def _seed(path) -> None:
    connection = sqlite3.connect(path)
    connection.executescript(
        """
        CREATE TABLE base_user (user_id INTEGER PRIMARY KEY, user_name TEXT, user_type TEXT, company_id INTEGER, department_id INTEGER, nick_name TEXT, real_name TEXT, avatar TEXT, email TEXT, mobile TEXT, user_desc TEXT, status INTEGER);
        CREATE TABLE base_role (role_id INTEGER PRIMARY KEY, role_code TEXT, role_name TEXT, role_desc TEXT, status INTEGER);
        CREATE TABLE base_role_user (user_id INTEGER, role_id INTEGER);
        CREATE TABLE base_user_org (id INTEGER PRIMARY KEY, user_id INTEGER, org_id INTEGER, expire_time TEXT);
        CREATE TABLE base_account (account_id INTEGER PRIMARY KEY, user_id INTEGER, account TEXT, password TEXT, account_type TEXT, status INTEGER, domain TEXT);
        CREATE TABLE base_authority (authority_id INTEGER PRIMARY KEY, authority TEXT, menu_id INTEGER, status INTEGER);
        CREATE TABLE base_authority_user (authority_id INTEGER, user_id INTEGER, expire_time TEXT);
        CREATE TABLE base_authority_role (authority_id INTEGER, role_id INTEGER, expire_time TEXT);
        CREATE TABLE base_menu (menu_id INTEGER PRIMARY KEY, app_id INTEGER, parent_id INTEGER, menu_code TEXT, menu_name TEXT, icon TEXT, path TEXT, priority INTEGER, status INTEGER, is_persist INTEGER, hidden INTEGER);
        CREATE TABLE base_org (id INTEGER PRIMARY KEY, parent_id INTEGER, org_name TEXT, org_code TEXT, status INTEGER, level INTEGER);
        CREATE TABLE base_dic (id INTEGER PRIMARY KEY, parent_id INTEGER, name TEXT, code TEXT, remark TEXT);
        CREATE TABLE base_app (app_id INTEGER PRIMARY KEY, code TEXT, api_key TEXT, app_name TEXT, app_type TEXT, status INTEGER, org_id INTEGER);
        CREATE TABLE gateway_route (route_id INTEGER PRIMARY KEY, route_name TEXT, path TEXT, service_id TEXT, url TEXT, strip_prefix INTEGER, status INTEGER);
        CREATE TABLE base_api (api_id INTEGER PRIMARY KEY);
        CREATE TABLE base_api_key (key_id INTEGER PRIMARY KEY);

        INSERT INTO base_user VALUES (1, 'admin', 'super', 1, NULL, '超级管理员', '超级管理员', NULL, NULL, NULL, NULL, 1);
        INSERT INTO base_role VALUES (1, 'super_admin', '超级管理员', NULL, 1);
        INSERT INTO base_role_user VALUES (1, 1);
        INSERT INTO base_user_org VALUES (1, 1, 1, NULL);
        INSERT INTO base_account VALUES (1, 1, 'admin', 'secret', 'username', 1, '@admin.com');
        INSERT INTO base_authority VALUES (1, 'MENU_dashboard', 1, 1);
        INSERT INTO base_menu VALUES (1, 1000, NULL, 'dashboard', '仪表盘', NULL, '/dashboard', 1, 1, 1, 0);
        INSERT INTO base_org VALUES (1, NULL, '默认组织', 'default', 1, 0);
        INSERT INTO base_dic VALUES (1, NULL, '启用状态', 'sys_status', NULL);
        INSERT INTO base_dic VALUES (2, 1, '启用', '1', NULL);
        INSERT INTO base_app VALUES (1000, NULL, 'jbmSeedDevAppKey00000001', 'JBM基础应用', 'pc', 1, 1);
        INSERT INTO gateway_route VALUES (1000, 'center-default', '/**', 'jbm-cluster-platform-center-jbm7', NULL, 0, 1);
        """
    )
    connection.commit()
    connection.close()
