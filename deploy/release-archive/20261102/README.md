# 历史发布文件归档

此目录保存 2026 年 11 月 2 日整理前的一次性发布配置和未被当前入口使用的构建文件，文件内容未改。它们用于审计和复现历史，不是正式发布入口。

原路径、归档路径、来源提交和 SHA-256 见 `inventory.json`。当前发布规则见 `docs/operations/multi-platform-version-release-sop.md`，当前保留入口见 `deploy/release-catalog.json`。历史验收记录仍使用当时的路径，须结合其源码提交查阅。不得直接执行归档脚本或从历史源码分支构建正式版本。

校验说明：`sha256` 留存整理前本地原始字节，备份包保存该字节；`sha256Lf` 用于 Git 的 CRLF/LF 换行归一化后校验，跨 Windows/Linux 检出仍可验证文本内容一致。
