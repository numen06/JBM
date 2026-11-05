import pytest
import base64
import json
from starlette.responses import Response
from jbm_cluster_py.common.admin_security import attach_audit_actor
from fastapi import HTTPException
from jbm_cluster_py.common.admin_security import AdminAccessPolicy, source_ip
from jbm_cluster_py.common.api_access import authorize_api


def test_forwarding_is_trusted_only_up_to_untrusted_hop():
    proxies = ["172.16.0.0/12", "127.0.0.0/8"]
    assert source_ip("198.51.100.1", {"x-forwarded-for": "127.0.0.1"}, proxies) == "198.51.100.1"
    assert source_ip("172.18.0.2", {"x-forwarded-for": "127.0.0.1, 198.51.100.1"}, proxies) == "198.51.100.1"
    assert source_ip("172.18.0.2", {"x-forwarded-for": "bad, 198.51.100.1"}, proxies) == "198.51.100.1"
    assert source_ip("172.18.0.2", {"x-forwarded-for": "bad"}, proxies) == "172.18.0.2"


def test_admin_source_does_not_change_normal_user_permissions():
    policy = AdminAccessPolicy({"mode": "enforce", "allowed-networks": ["198.51.100.0/24", "2001:db8::/32"]})
    for roles in (["tenant_admin"], ["super_admin"], ["platform_operator"], ["security_auditor"]):
        actor = {"userId": 2, "roles": roles}
        assert policy.require(actor, "198.51.100.7")
        assert policy.require(actor, "2001:db8::2")
        with pytest.raises(HTTPException):
            policy.require(actor, "203.0.113.3")
    assert policy.require({"userId": 3, "roles": ["operations"]}, "203.0.113.3")
    assert AdminAccessPolicy().require({"username": "admin"}, "203.0.113.3") is False
    with pytest.raises(ValueError):
        AdminAccessPolicy({"mode": "enforce"})


def test_auditor_can_read_only_audit_and_cannot_administer_tenants():
    auditor = {"userId": 4, "roles": ["security_auditor"], "permissions": ["ACTION_security.audit.read"]}
    authorize_api("logs", "POST", "/securityAudit/query", auditor)
    for service, method, path in (("logs", "DELETE", "/securityAudit/1"), ("center", "POST", "/role"), ("center", "PUT", "/org/999"), ("logs", "DELETE", "/businessLog/1")):
        with pytest.raises(HTTPException):
            authorize_api(service, method, path, auditor)
    tenant = {"userId": 3, "tenantId": 1, "roles": ["tenant_admin"]}
    authorize_api("logs", "POST", "/securityAudit/query", tenant)
    authorize_api("logs", "GET", "/securityAudit/status", tenant)
    for method, path in (("DELETE", "/securityAudit/1"), ("POST", "/loginLogs/pageList"), ("GET", "/gatewayLogs/all")):
        with pytest.raises(HTTPException):
            authorize_api("logs", method, path, tenant)


def test_expired_password_cannot_bypass_frontend_gate():
    # Existing change-password reminders keep their previous behavior during the
    # rollout; only a verified expiry in the new lifecycle blocks operations.
    authorize_api("center", "GET", "/role/all", {"userId": 4, "admin": True, "mustChangePassword": True})
    actor = {"userId": 4, "admin": True, "mustChangePassword": True, "passwordExpired": True}
    with pytest.raises(HTTPException):
        authorize_api("center", "GET", "/role/all", actor)
    authorize_api("center", "PUT", "/current/user/password", actor)


def test_platform_technical_tenant_is_not_a_resource_scope():
    def metadata(identity, *scope):
        return json.loads(base64.urlsafe_b64decode(attach_audit_actor(Response(), identity, *scope).headers['X-JBM-Audit-Actor']))
    root = {"admin": True, "userId": 1, "companyId": 1}
    assert metadata(root)["tenantId"] is None
    assert metadata(root)["tenantScopeVerified"] is False
    assert metadata(root, 2)["tenantId"] == "2"
    assert metadata(root, 2)["tenantScopeVerified"] is True
    assert metadata({"userId": 2100, "tenantId": 1, "roles": ["tenant_admin"]})["tenantId"] == "1"
