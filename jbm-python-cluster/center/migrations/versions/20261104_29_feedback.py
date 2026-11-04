"""Add deployment-local private feedback and retain the standard platform role."""
import sqlalchemy as sa
from alembic import op
from jbm_cluster_py.platform.center.feedback import metadata

revision = "20261104_29"
down_revision = "20261103_28"
branch_labels = None
depends_on = None


def upgrade():
    conn = op.get_bind()
    metadata.create_all(conn)
    # The standard role is part of installation data, never an ad-hoc account grant.
    # Existing role IDs, names, disabled states and installation grants are retained.
    if not conn.scalar(sa.text("SELECT role_id FROM base_role WHERE role_code='platform_operator'")):
        role_id = 3
        if conn.scalar(sa.text("SELECT role_id FROM base_role WHERE role_id=3")):
            raise ValueError("平台运营标准角色编号 3 已被占用，请先核对历史种子数据")
        conn.execute(sa.text("INSERT INTO base_role (role_id,role_code,role_name,role_desc,status,is_persist,create_time,update_time) "
            "VALUES (:id,'platform_operator','平台运营','平台侧跨租户运营角色',1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"), {"id": role_id})


def downgrade():
    # Retain feedback and the standard role when rolling back application code.
    pass
