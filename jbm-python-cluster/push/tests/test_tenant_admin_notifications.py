import json

import pytest
from sqlalchemy import text

from jbm_cluster_py.platform.push.push_message_repository import PushMessageRepository
from jbm_cluster_py.platform.push.service import PushService


CLIENT = 'iot_platform-98dc2cbb2a528b0e'


async def directory(tmp_path):
    repo = PushMessageRepository({'url': f"sqlite+aiosqlite:///{tmp_path / 'notifications.db'}"})
    await repo.start()
    async with repo.engine.begin() as db:
        for ddl in [
            'base_app (app_id INTEGER, api_key TEXT, status INTEGER)',
            'base_user (user_id INTEGER, status INTEGER)',
            'base_role (role_id INTEGER, role_code TEXT, status INTEGER)',
            'base_role_user (user_id INTEGER, role_id INTEGER, tenant_id TEXT, app_id INTEGER)',
            'base_user_org (user_id INTEGER, org_id TEXT, expire_time TEXT)',
        ]:
            await db.execute(text('CREATE TABLE ' + ddl))
        await db.execute(text('INSERT INTO base_app VALUES (33,:client,1)'), {'client': CLIENT})
        await db.execute(text('INSERT INTO base_user VALUES (0,1),(101,1),(102,1),(103,1),(104,0),(105,1),(201,1)'))
        await db.execute(text("INSERT INTO base_role VALUES (1,'super_admin',1),(2,'tenant_admin',1),(3,'tenant_demo_operator',1),(4,'iot_admin',1),(5,'tenant_admin',0)"))
        await db.execute(text("INSERT INTO base_role_user VALUES (0,1,NULL,NULL),(101,2,'tenant-a',33),(101,2,'tenant-a',33),(102,4,'tenant-a',33),(103,3,'tenant-a',33),(104,2,'tenant-a',33),(105,2,'tenant-a',33),(201,2,'tenant-b',33),(201,2,'tenant-a',33)"))
        await db.execute(text("INSERT INTO base_user_org VALUES (101,'tenant-a',NULL),(102,'tenant-a',NULL),(103,'tenant-a',NULL),(104,'tenant-a',NULL),(105,'tenant-a','2000-01-01'),(201,'tenant-b',NULL)"))
    return repo


def event(tenant='tenant-a', channel='internal'):
    return {'eventType': 'PUSH_MESSAGE', 'recipientType': 'tenant_admin', 'recipientAppClient': CLIENT,
            'pushWay': channel, 'sysMsg': True, 'title': '设备离线通知', 'showInMessageCenter': True,
            'extend': {'tenantId': tenant, 'deviceId': 'lamp', 'presenceEventId': 'presence-1'}}


@pytest.mark.asyncio
async def test_directory_uses_active_app_roles_and_tenant_membership(tmp_path):
    repo = await directory(tmp_path)
    try:
        assert await repo.tenant_admin_recipients('tenant-a', CLIENT) == [101, 102]
        assert await repo.tenant_admin_recipients('tenant-b', CLIENT) == [201]
        async with repo.engine.begin() as db:
            await db.execute(text('UPDATE base_role_user SET app_id=99 WHERE user_id=102'))
            await db.execute(text('UPDATE base_role_user SET role_id=5 WHERE user_id=101'))
        assert await repo.tenant_admin_recipients('tenant-a', CLIENT) == []
        assert await repo.tenant_admin_recipients('tenant-a', 'other-app') == []
        with pytest.raises(ValueError):
            await repo.tenant_admin_recipients('0', CLIENT)
    finally:
        await repo.stop()


@pytest.mark.asyncio
async def test_device_notifications_enter_only_the_owning_admin_inboxes(tmp_path, monkeypatch):
    repo = await directory(tmp_path)
    service = PushService(message_repository=repo)
    try:
        assert (await service.handle_push_event(event()))['recipientCount'] == 2
        await service.handle_push_event(event('tenant-b'))
        for user, tenant in [(101, 'tenant-a'), (102, 'tenant-a'), (201, 'tenant-b')]:
            page = await service.page_messages({}, user)
            assert page['total'] == 1
            assert page['contents'][0]['extend']['tenantId'] == tenant
            assert await service.unread_count(user) == 1
        assert await service.unread_count(0) == await service.unread_count(103) == 0
        ntfy_users = []

        async def ntfy(payload):
            ntfy_users.append(payload['recUserId'])
            return {'deliveryStatus': 'sent'}

        monkeypatch.setattr(service, '_deliver_ntfy', ntfy)
        await service.handle_push_event(event(channel='ntfy'))
        assert ntfy_users == [101, 102]
        # User-facing send requests cannot set a broker-only audience.
        built = service._build_event({**event('tenant-b'), 'recUserId': 101}, 101, 101)
        assert 'recipientType' not in built and built['recUserId'] == 101
    finally:
        await repo.stop()


@pytest.mark.asyncio
async def test_unresolved_audience_uses_durable_delayed_retry(tmp_path):
    repo = await directory(tmp_path)

    class Channel:
        default_exchange = None

        def __init__(self):
            self.default_exchange = self
            self.messages = []
            self.arguments = None

        async def declare_queue(self, queue, **kwargs):
            self.arguments = kwargs

        async def publish(self, message, **kwargs):
            self.messages.append(json.loads(message.body))

    class Rabbit:
        channel = Channel()

    service = PushService(message_repository=repo, rabbitmq=Rabbit())
    try:
        result = await service.handle_push_event(event('tenant-c'))
        assert result['deliveryStatus'] == 'pending'
        channel = service.rabbitmq.channel
        assert channel.arguments['durable'] is True
        assert channel.arguments['arguments']['x-message-ttl'] == 300_000
        assert channel.arguments['arguments']['x-dead-letter-routing-key'] == service.push_queue_name()
        assert 'recUserId' not in channel.messages[0]
        assert await repo.count_unread(0) == 0
        async with repo.engine.begin() as db:
            await db.execute(text("INSERT INTO base_role_user VALUES (101,2,'tenant-c',33)"))
            await db.execute(text("INSERT INTO base_user_org VALUES (101,'tenant-c',NULL)"))
        assert (await service.handle_push_event(channel.messages[0]))['deliveryStatus'] == 'sent'
        assert await repo.count_unread(101) == 1
    finally:
        await repo.stop()


@pytest.mark.asyncio
async def test_partial_delivery_retry_skips_completed_recipients(tmp_path, monkeypatch):
    repo = await directory(tmp_path)
    service = PushService(message_repository=repo)
    original = service._deliver_websocket
    pending = []

    async def fail_second(payload):
        if payload['recUserId'] == 102:
            raise ConnectionError('temporary persistence failure')
        await original(payload)

    async def defer(payload):
        pending.append(payload)

    monkeypatch.setattr(service, '_deliver_websocket', fail_second)
    monkeypatch.setattr(service, '_defer_tenant_message', defer)
    try:
        assert (await service.handle_push_event(event()))['deliveryStatus'] == 'pending'
        assert pending[0]['deliveredRecipientIds'] == [101]
        monkeypatch.setattr(service, '_deliver_websocket', original)
        await service.handle_push_event(pending[0])
        assert await repo.count_unread(101) == await repo.count_unread(102) == 1
    finally:
        await repo.stop()
