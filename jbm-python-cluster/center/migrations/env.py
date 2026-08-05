from __future__ import annotations

import asyncio
from logging.config import fileConfig

from alembic import context
from sqlalchemy.ext.asyncio import async_engine_from_config

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.integrations.database import configured_database_url


config = context.config
if config.config_file_name:
    fileConfig(config.config_file_name)

database_url = configured_database_url(AppConfig.load(app="center").database)
if not database_url:
    raise RuntimeError("Center database is not configured")
config.set_main_option("sqlalchemy.url", database_url.replace("%", "%%"))


def run_migrations_offline() -> None:
    context.configure(url=database_url, literal_binds=True, dialect_opts={"paramstyle": "named"})
    with context.begin_transaction():
        context.run_migrations()


def do_run_migrations(connection) -> None:
    context.configure(connection=connection, target_metadata=None)
    with context.begin_transaction():
        context.run_migrations()


async def run_async_migrations() -> None:
    connectable = async_engine_from_config(
        config.get_section(config.config_ini_section, {}), prefix="sqlalchemy.", pool_pre_ping=True
    )
    async with connectable.connect() as connection:
        await connection.run_sync(do_run_migrations)
    await connectable.dispose()


if context.is_offline_mode():
    run_migrations_offline()
else:
    asyncio.run(run_async_migrations())
