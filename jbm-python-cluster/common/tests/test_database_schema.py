import pytest
from jbm_cluster_py.integrations.database import require_tables
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine


@pytest.mark.asyncio
async def test_require_tables_fails_fast_until_migration_is_applied() -> None:
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    try:
        with pytest.raises(RuntimeError, match="missing tables: owned_table"):
            await require_tables(engine, ["owned_table"])
        async with engine.begin() as connection:
            await connection.execute(text("CREATE TABLE owned_table (id INTEGER)"))
        await require_tables(engine, ["owned_table"])
    finally:
        await engine.dispose()
