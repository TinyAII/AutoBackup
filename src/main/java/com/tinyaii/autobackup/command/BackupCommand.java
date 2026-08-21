package com.tinyaii.autobackup.command;

import com.tinyaii.autobackup.AutoBackupPlugin;
import com.tinyaii.autobackup.BackupTask;
import com.tinyaii.autobackup.config.ConfigManager;
import com.tinyaii.autobackup.storage.BackupFile;
import com.tinyaii.autobackup.storage.StorageBackend;
import com.tinyaii.autobackup.storage.StorageFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BackupCommand implements CommandExecutor, TabCompleter {

    private final AutoBackupPlugin plugin;
    private final ConfigManager config;
    private final StorageFactory storageFactory;

    public BackupCommand(AutoBackupPlugin plugin, ConfigManager config, StorageFactory storageFactory) {
        this.plugin = plugin;
        this.config = config;
        this.storageFactory = storageFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "立即":
            case "now":
                return cmdNow(sender);
            case "列表":
            case "list":
                return cmdList(sender);
            case "恢复":
            case "restore":
                return cmdRestore(sender, args);
            case "状态":
            case "status":
            case "config":
                return cmdStatus(sender);
            case "重载":
            case "reload":
                return cmdReload(sender);
            case "测试oss":
            case "testoss":
                return cmdTestOSS(sender);
            case "清理本地":
            case "cleanlocal":
                return cmdCleanLocal(sender);
            case "清理远端":
            case "cleanremote":
                return cmdCleanRemote(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    // ===== 立即备份 =====
    private boolean cmdNow(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.use")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        sender.sendMessage("§e[AutoBackup] §f正在执行备份...");
        plugin.getLogger().info("[AutoBackup] §f玩家 " + sender.getName() + " 触发了手动备份");

        BackupTask task = new BackupTask(plugin, config, storageFactory);
        task.executeBackup();
        return true;
    }

    // ===== 列出备份 =====
    private boolean cmdList(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.use")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        List<BackupFile> backups = storageFactory.listAllBackups();

        if (backups.isEmpty()) {
            sender.sendMessage("§e[AutoBackup] §f暂无备份记录");
            return true;
        }

        sender.sendMessage("§b[AutoBackup] §f备份列表 §7(共 " + backups.size() + " 个)");
        sender.sendMessage("§7" + "─".repeat(60));

        int showCount = Math.min(backups.size(), 20);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < showCount; i++) {
            BackupFile bf = backups.get(i);
            String line = String.format("§7[%d] §f%s §7| §f%s §7| §e%s §7| §f%s",
                    i + 1,
                    bf.getName(),
                    bf.getSizeFormatted(),
                    bf.getTimeFormatted(),
                    bf.getStorageType());
            sender.sendMessage(line);
        }

        if (backups.size() > 20) {
            sender.sendMessage("§7... 还有 " + (backups.size() - 20) + " 个备份未显示");
        }

        return true;
    }

    // ===== 恢复备份 =====
    private boolean cmdRestore(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "autobackup.admin")) {
            sender.sendMessage("§c你没有权限恢复备份（需要 op 权限）");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /备份 恢复 <文件名>");
            return true;
        }

        String fileName = args[1];

        sender.sendMessage("§c[AutoBackup] §f恢复备份需要 §c停止服务器 §f后才能执行");
        sender.sendMessage("§7请在服务器停止后，将备份文件手动解压到服务器目录");
        sender.sendMessage("§7备份文件位置: ./backups/" + fileName);

        // 查找本地备份
        List<BackupFile> backups = storageFactory.listAllBackups();
        boolean found = backups.stream().anyMatch(bf -> bf.getName().equals(fileName));

        if (!found) {
            sender.sendMessage("§c未找到备份文件: " + fileName);
            sender.sendMessage("§7请使用 /备份 列表 查看可用备份");
        } else {
            sender.sendMessage("§a找到备份文件: " + fileName);
            sender.sendMessage("§7恢复步骤: ① 停止服务器 ② 删除或重命名当前 world/ 等目录 ③ 将备份解压到服务器根目录 ④ 启动服务器");
        }

        return true;
    }

    // ===== 查看状态 =====
    private boolean cmdStatus(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.use")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        sender.sendMessage("§b[AutoBackup] §f当前配置:");
        sender.sendMessage("§7" + "─".repeat(50));

        // 定时备份
        int interval = config.getInterval();
        if (interval > 0) {
            sender.sendMessage(String.format("§7定时备份: §a开启 §7(每 %d 分钟)", interval));
        } else {
            sender.sendMessage("§7定时备份: §c关闭");
        }

        // 停服前备份
        sender.sendMessage("§7停服前备份: §" + (config.isShutdownBackupEnabled() ? "a开启" : "c关闭"));

        // 备份内容
        sender.sendMessage(String.format("§7备份内容: §a世界(%s) §f插件(%s) §f配置(%s) §f白名单(%s)",
                config.isBackupWorlds() ? "§a✔" : "§c✘",
                config.isBackupPlugins() ? "§a✔" : "§c✘",
                config.isBackupConfig() ? "§a✔" : "§c✘",
                config.isBackupWhitelist() ? "§a✔" : "§c✘"));

        sender.sendMessage("§7压缩格式: §f" + config.getCompressFormat());

        // 存储后端
        sender.sendMessage("§7存储后端:");
        List<StorageBackend> backends = storageFactory.getBackends();
        if (backends.isEmpty()) {
            sender.sendMessage("  §c无启用的存储后端");
        } else {
            for (StorageBackend backend : backends) {
                sender.sendMessage("  §f- §a" + backend.getName());
            }
        }

        // 备份数量
        List<BackupFile> backups = storageFactory.listAllBackups();
        sender.sendMessage("§7备份总数: §f" + backups.size() + " §7个");
        if (!backups.isEmpty()) {
            sender.sendMessage("§7最新备份: §f" + backups.get(0).getName() + " §7(" + backups.get(0).getSizeFormatted() + ")");
        }

        return true;
    }

    // ===== 重载配置 =====
    private boolean cmdReload(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.admin")) {
            sender.sendMessage("§c你没有权限重载配置（需要 op 权限）");
            return true;
        }

        config.reloadConfig();
        plugin.getLogger().info("[AutoBackup] §f玩家 " + sender.getName() + " 重载了配置");
        sender.sendMessage("§a[AutoBackup] §f配置已重载");

        return true;
    }

    // ===== 测试 OSS 连接 =====
    private boolean cmdTestOSS(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        sender.sendMessage("§b[AutoBackup] §f测试存储连接...");
        List<String> results = storageFactory.testAllConnections();
        for (String result : results) {
            sender.sendMessage(result);
        }

        return true;
    }

    // ===== 清理本地备份 =====
    private boolean cmdCleanLocal(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        List<BackupFile> backups = storageFactory.listAllBackups();
        int keepCount = config.getLocalKeepCount();

        if (backups.size() <= keepCount) {
            sender.sendMessage("§e[AutoBackup] §f备份数量未超过保留上限，无需清理");
            return true;
        }

        int toDelete = backups.size() - keepCount;
        sender.sendMessage("§e[AutoBackup] §f正在清理 " + toDelete + " 个旧备份...");

        int deleted = 0;
        for (int i = keepCount; i < backups.size(); i++) {
            BackupFile bf = backups.get(i);
            for (StorageBackend backend : storageFactory.getBackends()) {
                if (backend.delete(bf.getName())) {
                    deleted++;
                    break;
                }
            }
        }

        sender.sendMessage("§a[AutoBackup] §f清理完成，删除了 " + deleted + " 个旧备份");
        return true;
    }

    // ===== 清理远端备份 =====
    private boolean cmdCleanRemote(CommandSender sender) {
        if (!hasPermission(sender, "autobackup.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        sender.sendMessage("§e[AutoBackup] §f已触发所有存储后端的自动清理");
        storageFactory.cleanupAll();
        sender.sendMessage("§a[AutoBackup] §f清理完成");
        return true;
    }

    // ===== 发送帮助 =====
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b═══════════════════════════════════════");
        sender.sendMessage("§b  AutoBackup §f自动备份插件 §7v1.0.0 §f- §fTinyAII 出品");
        sender.sendMessage("§b═══════════════════════════════════════");
        sender.sendMessage("§f/备份 立即 §7- 立即执行一次备份");
        sender.sendMessage("§f/备份 列表 §7- 查看所有备份记录");
        sender.sendMessage("§f/备份 恢复 <文件名> §7- 查看恢复方法（需停服）");
        sender.sendMessage("§f/备份 状态 §7- 查看配置和存储状态");
        sender.sendMessage("§f/备份 重载 §7- 重载配置文件");
        sender.sendMessage("§f/备份 测试oss §7- 测试存储连接");
        if (sender.hasPermission("autobackup.admin")) {
            sender.sendMessage("§7────────── §c管理员指令 §7──────────");
            sender.sendMessage("§f/备份 清理本地 §7- 清理超出保留数量的本地备份");
            sender.sendMessage("§f/备份 清理远端 §7- 清理所有远端存储的旧备份");
        }
        sender.sendMessage("§b═══════════════════════════════════════");
    }

    // ===== Tab 补全 =====
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("立即");
            subCommands.add("列表");
            subCommands.add("恢复");
            subCommands.add("状态");
            subCommands.add("重载");
            subCommands.add("测试oss");
            if (sender.hasPermission("autobackup.admin")) {
                subCommands.add("清理本地");
                subCommands.add("清理远端");
            }
            return filterStartsWith(subCommands, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("恢复") || args[0].equalsIgnoreCase("restore")) {
                // 列出备份文件名
                List<BackupFile> backups = storageFactory.listAllBackups();
                return backups.stream()
                        .map(BackupFile::getName)
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.isOp()) return true;
        if (sender instanceof Player) {
            return ((Player) sender).hasPermission(permission);
        }
        return sender.hasPermission(permission);
    }
}
