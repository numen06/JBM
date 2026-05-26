# 集群非核心应用 jaja7 冒烟报告

时间：2026-05-26T17:00:46
Profile：jaja7

## 服务探活

- logs: DOWN
- push: DOWN
- doc: DOWN
- job: DOWN
- bigscreen: DOWN

## 用例结果

- **health-logs**: skipped — service logs not reachable
- **gateway-logs-access**: skipped — service logs not reachable
- **gateway-logs-find**: skipped — service logs not reachable
- **gateway-push-pageList**: skipped — service push not reachable
- **gateway-job-test**: skipped — service job not reachable
- **gateway-bigscreen-isUpload**: skipped — service bigscreen not reachable
- **gateway-doc-pageList**: skipped — service doc not reachable
- **auth-no-token-logs**: skipped — service logs not reachable
- **auth-fake-internal-header**: skipped — service logs not reachable

汇总：passed=0 failed=0 skipped=9

机器可读结果：`.cursor/cluster-apps-smoke-result.json`
