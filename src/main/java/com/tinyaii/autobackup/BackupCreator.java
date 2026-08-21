package com.tinyaii.autobackup;

import com.tinyaii.autobackup.config.ConfigManager;
import com.tinyaii.autobackup.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupCreator {

    private final AutoBackupPlugin plugin;
    private final ConfigManager config;
    private BukkitTask progressTask;
    private long startTime;

    public BackupCreator(AutoBackupPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * 创建备份压缩包
     * @return 备份文件路径，失败返回 null
     */
    public File createBackup() {
        String baseDir = Bukkit.getWorldContainer().getAbsolutePath();
        File backupDir = new File(baseDir, "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // 生成备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName;
        String format = config.getCompressFormat();

        if ("zip".equalsIgnoreCase(format)) {
            fileName = "backup-" + timestamp + ".zip";
        } else {
            fileName = "backup-" + timestamp + ".tar.gz";
        }

        File backupFile = new File(backupDir, fileName);

        plugin.getLogger().info("[AutoBackup] §e正在创建备份: " + fileName);
        Bukkit.broadcastMessage("§e[AutoBackup] §f正在创建备份压缩包...");

        startTime = System.currentTimeMillis();
        startProgressReporter();

        try {
            if ("zip".equalsIgnoreCase(format)) {
                createZipBackup(backupFile, baseDir);
            } else {
                createTarGzBackup(backupFile, baseDir);
            }

            long size = backupFile.length();
            String sizeStr = formatSize(size);
            plugin.getLogger().info("[AutoBackup] §a备份创建成功: §f" + fileName + " §7(" + sizeStr + ")");

            stopProgressReporter();
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            Bukkit.broadcastMessage("§a[AutoBackup] §f备份创建完成 §7| §f" + fileName + " §7(" + sizeStr + ") §7| §f耗时 " + elapsed + " 秒");
            return backupFile;
        } catch (Exception e) {
            stopProgressReporter();
            plugin.getLogger().warning("[AutoBackup] §c备份创建失败: " + e.getMessage());
            e.printStackTrace();
            Bukkit.broadcastMessage("§c[AutoBackup] §c备份创建失败，请查看控制台日志");
            return null;
        }
    }

    /** 每 10 秒广播一次进度 */
    private void startProgressReporter() {
        progressTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage("§e[AutoBackup] §f正在备份中... §7(已用 " + elapsed + " 秒)");
            });
        }, 200L, 200L); // 200 ticks = 10 秒
    }

    private void stopProgressReporter() {
        if (progressTask != null && !progressTask.isCancelled()) {
            progressTask.cancel();
        }
    }

    /**
     * 创建 ZIP 格式备份
     */
    private void createZipBackup(File backupFile, String baseDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            zos.setLevel(9);

            // 备份世界
            if (config.isBackupWorlds()) {
                addWorldsToZip(zos, baseDir);
            }
            // 备份插件目录
            if (config.isBackupPlugins()) {
                addDirectoryToZip(zos, baseDir, "plugins", "plugins");
            }
            // 备份配置
            if (config.isBackupConfig()) {
                addConfigFilesToZip(zos, baseDir);
            }
            // 备份白名单
            if (config.isBackupWhitelist()) {
                addWhitelistFilesToZip(zos, baseDir);
            }
        }
    }

    /**
     * 创建 tar.gz 格式备份（简化版：用 zip 模拟，保持兼容）
     * 注：纯 Java 实现 tar.gz 较复杂，这里用 zip 替代但扩展名 .tar.gz
     * 如需真 tar.gz，可后续引入 Apache Commons Compress
     */
    private void createTarGzBackup(File backupFile, String baseDir) throws IOException {
        // 先用 zip 创建，后续可替换为真 tar.gz
        createZipBackup(backupFile, baseDir);
    }

    private void addWorldsToZip(ZipOutputStream zos, String baseDir) throws IOException {
        // 备份主世界和维度世界
        String[] worldNames = {"world", "world_nether", "world_the_end", "DIM-1", "DIM1"};
        for (String worldName : worldNames) {
            File worldDir = new File(baseDir, worldName);
            if (worldDir.exists() && worldDir.isDirectory()) {
                plugin.getLogger().info("[AutoBackup] §7  备份世界: " + worldName);
                addDirectoryToZip(zos, baseDir, worldName, worldName);
            }
        }
    }

    private void addConfigFilesToZip(ZipOutputStream zos, String baseDir) throws IOException {
        String[] configFiles = {
                "server.properties", "bukkit.yml", "spigot.yml", "paper.yml",
                "paper-world-defaults.yml", "permissions.yml", "commands.yml"
        };
        for (String fileName : configFiles) {
            File f = new File(baseDir, fileName);
            if (f.exists() && f.isFile()) {
                addFileToZip(zos, baseDir, fileName);
            }
        }
    }

    private void addWhitelistFilesToZip(ZipOutputStream zos, String baseDir) throws IOException {
        String[] whitelistFiles = {
                "whitelist.json", "ops.json", "banned-players.json",
                "banned-ips.json", "usercache.json"
        };
        for (String fileName : whitelistFiles) {
            File f = new File(baseDir, fileName);
            if (f.exists() && f.isFile()) {
                addFileToZip(zos, baseDir, fileName);
            }
        }
    }

    private void addDirectoryToZip(ZipOutputStream zos, String baseDir, String dirName, String zipPrefix) throws IOException {
        File dir = new File(baseDir, dirName);
        if (!dir.exists()) return;

        Path basePath = Paths.get(baseDir);
        Files.walk(dir.toPath())
                .filter(p -> !Files.isDirectory(p))
                .forEach(path -> {
                    String relativePath = basePath.relativize(path).toString().replace("\\", "/");
                    // 排除不需要的文件
                    if (relativePath.contains("session.lock")) return;
                    if (relativePath.contains("uid.dat")) return;
                    if (relativePath.contains("playerdata") && !isRecent(path)) return;

                    try {
                        ZipEntry entry = new ZipEntry(relativePath);
                        entry.setTime(path.toFile().lastModified());
                        zos.putNextEntry(entry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        plugin.getLogger().warning("[AutoBackup] §c写入文件失败: " + relativePath);
                    }
                });
    }

    private void addFileToZip(ZipOutputStream zos, String baseDir, String fileName) throws IOException {
        File file = new File(baseDir, fileName);
        if (!file.exists()) return;
        ZipEntry entry = new ZipEntry(fileName);
        entry.setTime(file.lastModified());
        zos.putNextEntry(entry);
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    private boolean isRecent(Path path) {
        // 只备份最近 7 天有活跃玩家的数据（简化处理：全部备份）
        return true;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
