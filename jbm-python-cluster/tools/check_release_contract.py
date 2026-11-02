"""Verify an authenticated release contract without running business mutations."""
import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path


def check(origin, contract, token):
    results = []
    for item in contract['checks']:
        if item['method'] != 'GET' or not item['path'].startswith('/'):
            raise ValueError('Release contracts may only read GET endpoints')
        request = urllib.request.Request(origin.rstrip('/') + item['path'],
                                         headers={'Authorization': 'Bearer ' + token})
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                body = json.load(response)
                passed = response.status == 200 and isinstance(body, dict) and body.get('success') is True
                results.append({'path': item['path'], 'status': response.status, 'passed': passed})
        except urllib.error.HTTPError as error:
            results.append({'path': item['path'], 'status': error.code, 'passed': False})
        except (urllib.error.URLError, ValueError, TimeoutError):
            results.append({'path': item['path'], 'status': 'invalid_response', 'passed': False})
    return {'name': contract['name'], 'passed': bool(results) and all(row['passed'] for row in results),
            'checks': results, 'businessWrites': 0}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--origin', required=True)
    parser.add_argument('--contract', required=True)
    parser.add_argument('--token-file', required=True, help='Local JSON session; never uploaded')
    args = parser.parse_args()
    session = json.loads(Path(args.token_file).read_text(encoding='utf-8-sig'))
    token = session.get('access_token') or session.get('token')
    if not token:
        raise ValueError('Missing authenticated session')
    result = check(args.origin, json.loads(Path(args.contract).read_text(encoding='utf-8-sig')), token)
    print(json.dumps(result, ensure_ascii=False))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
