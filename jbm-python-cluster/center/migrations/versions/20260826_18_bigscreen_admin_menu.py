"""Register bigscreen management as a JBM administration function.

Revision ID: 20260826_18
Revises: 20260826_17
"""

from collections.abc import Sequence

from alembic import op
from sqlalchemy import inspect

revision: str = "20260826_18"
down_revision: str | None = "20260826_17"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

MENU_ID = 2196140000000000180
AUTHORITY_ID = 2196140000000000181
ROLE_GRANT_ID = 2196140000000000200


def upgrade() -> None:
    op.execute(
        f"""
        INSERT INTO base_menu
          (menu_id, parent_id, menu_code, menu_name, icon, scheme, path, target,
           priority, status, is_persist, service_id, hidden, create_time, update_time)
        SELECT {MENU_ID}, 100, 'bigscreen_views', '大屏管理', 'monitor-up', '/',
               '/bigscreen/views', '_self', 8, 1, 1,
               'jbm-cluster-platform-bigscreen', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (SELECT 1 FROM base_menu WHERE menu_code='bigscreen_views')
        """
    )
    op.execute(
        f"""
        INSERT INTO base_authority
          (authority_id, authority, resource_type, menu_id, status, create_time, update_time)
        SELECT {AUTHORITY_ID}, 'MENU_bigscreen_views', 'menu', menu_id, 1,
               CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        FROM base_menu
        WHERE menu_code='bigscreen_views'
          AND NOT EXISTS (SELECT 1 FROM base_authority WHERE authority='MENU_bigscreen_views')
        LIMIT 1
        """
    )
    role_grant_columns = {
        column["name"]
        for column in inspect(op.get_bind()).get_columns("base_authority_role")
    }
    if "id" in role_grant_columns:
        insert_columns = "id, authority_id, role_id, create_time, update_time"
        select_columns = (
            f"{ROLE_GRANT_ID} + ROW_NUMBER() OVER (ORDER BY role.role_id), "
            "authority.authority_id, role.role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
        )
    else:
        insert_columns = "authority_id, role_id, create_time, update_time"
        select_columns = (
            "authority.authority_id, role.role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
        )
    op.execute(
        f"""
        INSERT INTO base_authority_role ({insert_columns})
        SELECT {select_columns}
        FROM base_role role
        JOIN base_authority authority ON authority.authority='MENU_bigscreen_views'
        WHERE role.role_code IN ('super_admin', 'platform_operator', 'tenant_admin')
          AND NOT EXISTS (
            SELECT 1 FROM base_authority_role grant_row
            WHERE grant_row.authority_id=authority.authority_id
              AND grant_row.role_id=role.role_id
          )
        """
    )


def downgrade() -> None:
    pass
