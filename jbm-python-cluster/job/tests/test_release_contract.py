import importlib.util
import io
from pathlib import Path
from unittest.mock import patch
from urllib.error import HTTPError


spec = importlib.util.spec_from_file_location('release_contract', Path(__file__).resolve().parents[2] / 'tools/check_release_contract.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def test_release_gate_rejects_missing_unauthorized_and_business_failed_endpoints():
    contract = {'name': 'Database operations', 'checks': [{'method': 'GET', 'path': '/job/sysJob/database-backups/sources'}]}
    for body, expected in [(b'{"success":true,"result":[]}', True), (b'{"success":false}', False), (b'<!doctype html>', False)]:
        response = io.BytesIO(body)
        response.status = 200
        with patch.object(module.urllib.request, 'urlopen', return_value=response):
            result = module.check('https://example.test/jbm-api', contract, 'private-fixture-token')
        assert result['passed'] is expected
        assert result['businessWrites'] == 0
        assert 'private-fixture-token' not in str(result)
    for status in [403, 404]:
        with patch.object(module.urllib.request, 'urlopen', side_effect=HTTPError('https://example.test', status, '', {}, None)):
            assert module.check('https://example.test/jbm-api', contract, 'private-fixture-token')['passed'] is False
