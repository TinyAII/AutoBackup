# AutoBackup 自动备份插件

**TinyAII 出品** | MIT 开源 | Paper 1.21.8 | 零依赖

自动备份你的 Minecraft 服务器，支持定时备份、手动备份、多存储后端（本地已实现，OSS/S3 接口预留），以及停服前自动备份开关。

## 功能

| 功能 | 说明 |
|------|------|
| 定时自动备份 | 可配置间隔（分钟），后台自动执行 |
| 手动备份 | `/备份 立即` 随时触发一次完整备份 |
| 备份内容可选 | 世界、插件、配置、白名单（可单独开关） |
| 压缩格式 | ZIP / tar.gz（默认 ZIP） |
| 本地存储 | 备份到服务器磁盘指定目录，自动保留最近 N 个 |
| 远端存储预留 | 阿里云 OSS / S3 兼容接口已预留，后续接入 SDK 即可启用 |
| 停服前备份 | 可配置停服时是否自动执行最后一次备份 |
| 备份列表 | `/备份 列表` 查看所有备份记录（文件名、大小、时间） |
| 恢复指引 | `/备份 恢复 <文件名>` 给出恢复步骤（需停服手动操作） |
| 中文指令 | 所有命令支持中文，Tab 补全友好 |

## 安装

1. 下载 `autobackup-1.0.0.jar`
2. 放入服务器的 `plugins/` 目录
3. 重启服务器或执行 `/reload`
4. 插件会自动生成 `plugins/AutoBackup/config.yml`，按需修改

## 配置

插件安装后会在 `plugins/AutoBackup/` 下生成 `config.yml`，主要配置项：

```yaml
# 定时备份间隔（分钟），0 = 关闭
interval: 360

# 停服前是否自动备份
shutdown-backup: false

# 备份内容开关
backup:
  worlds: true      # 世界数据
  plugins: true     # 插件目录
  config: true      # 配置文件
  whitelist: true   # 白名单/封禁列表

# 压缩格式
compress-format: zip

# 本地存储配置
local:
  enabled: true
  path: ./backups
  keep-count: 10
```

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/备份` | 查看帮助菜单 | 全员 |
| `/备份 立即` | 立即执行一次备份 | 全员 |
| `/备份 列表` | 查看所有备份记录 | 全员 |
| `/备份 状态` | 查看当前配置和存储状态 | 全员 |
| `/备份 重载` | 重载配置文件 | OP |
| `/备份 测试oss` | 测试存储连接 | OP |
| `/备份 清理本地` | 清理超出保留数量的旧备份 | OP |
| `/备份 清理远端` | 清理所有远端存储的旧备份 | OP |

## 兼容性

- **Paper** 1.21.8
- **Java** 21+
- 零依赖，无需其他插件

## 已知限制

- 恢复备份需**停服后手动操作**（插件给出指引，不自动恢复）
- 阿里云 OSS / S3 接口已预留，暂未接入 SDK
- 大世界（>10GB）备份可能需要较长时间，期间服务器可能轻微卡顿

## 更新日志

### v1.0.0
- 初始版本发布
- 定时备份 + 手动备份
- 本地存储 + OSS/S3 预留
- 中文指令 + 进度广播

## License

MIT License · Copyright (c) 2026 TinyAII

---

# AutoBackup (English)

**by TinyAII** | MIT License | Paper 1.21.8 | Zero dependencies

Auto-backup plugin for Minecraft servers. Supports scheduled backups, manual backups, multiple storage backends (local implemented, OSS/S3 interfaces reserved), and pre-shutdown backup toggle.

## Features

| Feature | Description |
|---------|-------------|
| Scheduled backups | Configurable interval (minutes), runs in background |
| Manual backup | `/backup now` to trigger a full backup anytime |
| Selective content | Worlds, plugins, config, whitelist (toggle each) |
| Compression | ZIP / tar.gz (default ZIP) |
| Local storage | Saves to server disk, auto-keeps last N backups |
| Remote storage reserved | Alibaba Cloud OSS / S3 compatible interfaces ready for SDK integration |
| Pre-shutdown backup | Optional auto-backup before server stops |
| Backup list | `/backup list` shows all backups (name, size, time) |
| Restore guide | `/backup restore <filename>` gives restore steps (manual, requires stop) |
| Chinese commands | All commands support Chinese, Tab completion included |

## Installation

1. Download `autobackup-1.0.0.jar`
2. Put it in your server's `plugins/` folder
3. Restart server or run `/reload`
4. Plugin auto-generates `plugins/AutoBackup/config.yml`, edit as needed

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/backup` | Show help menu | Everyone |
| `/backup now` | Trigger an immediate backup | Everyone |
| `/backup list` | List all backups | Everyone |
| `/backup status` | Show config and storage status | Everyone |
| `/backup reload` | Reload config file | OP |
| `/backup testoss` | Test storage connections | OP |
| `/backup cleanlocal` | Clean old local backups exceeding keep count | OP |
| `/backup cleanremote` | Clean all remote storage old backups | OP |

## Compatibility

- **Paper** 1.21.8
- **Java** 21+
- Zero dependencies

## Known Limitations

- Restore requires manual operation after stopping the server (plugin provides guide)
- Alibaba Cloud OSS / S3 interfaces are reserved, SDK not yet integrated
- Large worlds (>10GB) may cause brief lag during backup

## Changelog

### v1.0.0
- Initial release
- Scheduled + manual backups
- Local storage + OSS/S3 reserved
- Chinese commands + progress broadcast

## License

MIT License · Copyright (c) 2026 TinyAII
