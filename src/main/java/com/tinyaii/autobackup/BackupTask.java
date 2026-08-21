package com.tinyaii.autobackup;

import com.tinyaii.autobackup.config.ConfigManager;
import com.tinyaii.autobackup.storage.StorageBackend;
import com.tinyaii.autobackup.storage.StorageFactory;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

public class BackupTask {

    private final AutoBackupPlugin plugin;
    private final ConfigManager config;
    private final StorageFactory storageFactory;
    private BukkitTask task;
    private boolean running = false;

    public BackupTask(AutoBackupPlugin plugin, ConfigManager config, StorageFactory storageFactory) {
        this.plugin = plugin;
        this.config = config;
        this.storageFactory = storageFactory;
    }

    public void start() {
        int intervalMinutes = config.getInterval();
        if (intervalMinutes <= 0) {
            plugin.getLogger().info("[AutoBackup] §7定时备份已关闭（间隔设置为 0）");
            return;
        }

        long intervalTicks = intervalMinutes * 60L * 20L; // 分钟 → 秒 → tick
        if (intervalTicks < 20) intervalTicks = 20; // 至少 1 秒

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::executeBackup, intervalTicks, intervalTicks);
        running = true;

        plugin.getLogger().info("[AutoBackup] §a定时备份已启动 §7(每 " + intervalMinutes + " 分钟)");
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        running = false;
    }

    /**
     * 执行一次完整备份流程（手动触发或定时触发）
     */
    public void executeBackup() {
        // 注意：手动执行时 running 可能为 false（新实例），不做 guard
        // running 仅用于定时器 stop() 后阻止继续调度，不阻止单次执行

        Bukkit.getScheduler().runTask(plugin, () -> {
            // 在同步线程中创建备份文件（需要访问世界容器）
            BackupCreator creator = new BackupCreator(plugin, config);
            File backupFile = creator.createBackup();

            if (backupFile == null || !backupFile.exists()) {
                Bukkit.broadcastMessage("§c[AutoBackup] §c备份失败，请查看控制台日志");
                return;
            }

            // 异步上传到所有存储后端
            final File finalBackupFile = backupFile;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String remoteName = finalBackupFile.getName();
                storageFactory.uploadAll(finalBackupFile, remoteName);

                // 清理旧备份
                storageFactory.cleanupAll();

                // 广播通知
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcastMessage("§a[AutoBackup] §f自动备份完成 §7| §f" + remoteName + " §7(" + formatSize(finalBackupFile.length()) + ")");
                });
            });
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
