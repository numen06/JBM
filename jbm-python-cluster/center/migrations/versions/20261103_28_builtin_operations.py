"""Install the tenant business operations role without assigning any users."""

import json
import secrets
from datetime import datetime
from pathlib import Path

import sqlalchemy as sa
from alembic import op

revision = "20261103_28"
down_revision = "20261029_27"
branch_labels = None
depends_on = None

ROLE_ID = 2196140000000000280


def upgrade():
    conn = op.get_bind()
    metadata = sa.MetaData()
    roles, authorities, grants, menus, actions = [
        sa.Table(name, metadata, autoload_with=conn)
        for name in ("base_role", "base_authority", "base_authority_role", "base_menu", "base_action")
    ]
    definition = json.loads((Path(__file__).parents[1] / "sql/builtin_operations.json").read_text(encoding="utf-8"))
    now = datetime.now()
    role_id = conn.execute(sa.select(roles.c.role_id).where(roles.c.role_code == definition["roleCode"])).scalar()
    if role_id is not None:
        # An existing role may have installation-specific grants; preserve it.
        return
    conn.execute(roles.insert().values(role_id=ROLE_ID, role_code=definition["roleCode"],
        role_name=definition["roleName"], role_desc=definition["roleDesc"], status=1,
        is_persist=1, create_time=now, update_time=now))

    # The existing CRUD API registers an action together with its authority.
    # Reproduce that registration only for installed building applications.
    for menu in conn.execute(sa.select(menus).where(menus.c.menu_code == "building_group_energy", menus.c.status == 1)).mappings():
        code = "building.platform.operate"
        existing = conn.execute(sa.select(authorities.c.authority_id).where(
            authorities.c.authority == "ACTION_" + code, authorities.c.app_id == menu["app_id"])).scalar()
        if existing is None:
            action_id, authority_id = secrets.randbits(62), secrets.randbits(62)
            conn.execute(actions.insert().values(action_id=action_id, action_code=code,
                action_name="建筑业务运维", menu_id=menu["menu_id"], app_id=menu["app_id"],
                status=1, is_persist=1, create_time=now, update_time=now))
            conn.execute(authorities.insert().values(authority_id=authority_id, authority="ACTION_" + code,
                resource_type="action", action_id=action_id, menu_id=menu["menu_id"], app_id=menu["app_id"],
                status=1, create_time=now, update_time=now))

    codes = ["MENU_" + code for code in definition["menuCodes"]]
    codes += ["ACTION_" + code for code in definition["operationCodes"]]
    ids = conn.execute(sa.select(authorities.c.authority_id).where(authorities.c.authority.in_(codes), authorities.c.status == 1)).scalars()
    for authority_id in ids:
        values = dict(role_id=ROLE_ID, authority_id=authority_id, create_time=now, update_time=now)
        if "id" in grants.c:
            values["id"] = secrets.randbits(62)
        conn.execute(grants.insert().values(**values))


def downgrade():
    # Accounts may already use the role. Preserve authorization on code rollback.
    pass
