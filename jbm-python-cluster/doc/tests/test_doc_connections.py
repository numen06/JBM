from unittest.mock import Mock

import pytest
from sqlalchemy import event
from sqlalchemy.pool import QueuePool

from jbm_cluster_py.platform.doc.repository import _ping_doc_connection


def test_closed_transport_is_replaced_before_checkout():
    stale, fresh = Mock(), Mock()
    creator = Mock(side_effect=[stale, fresh])
    pool = QueuePool(creator)
    dialect = Mock()
    dialect.do_ping.side_effect = [
        RuntimeError("unable to perform operation on <TCPTransport closed=True reading=False>; the handler is closed"),
        True,
    ]
    event.listen(pool, "checkout", lambda connection, record, proxy: _ping_doc_connection(dialect, connection))
    connection = pool.connect()
    assert connection.dbapi_connection is fresh
    assert creator.call_count == 2
    stale.close.assert_called_once()
    connection.close()
    pool.dispose()


def test_unrelated_runtime_error_is_not_retried():
    dialect = Mock()
    dialect.do_ping.side_effect = RuntimeError("unrelated programming error")
    dialect.is_disconnect.return_value = False
    with pytest.raises(RuntimeError, match="unrelated programming error"):
        _ping_doc_connection(dialect, Mock())
