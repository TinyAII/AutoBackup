package com.tinyaii.autobackup.storage;

import com.tinyaii.autobackup.AutoBackupPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地存储后端
 * 将备份文件保存到服务器磁盘的指定目录
 */
public class LocalStorage implements StorageBackend {

    private final AutoBackupPlugin plugin;
    private final String basePath;
    private final int keepCount;
    private final File dataFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 本地备份记录（用于列表和清理）
    private final Map<String, LocalDateTime> backupRecords = new LinkedHashMap<>();

    public LocalStorage(AutoBackupPlugin plugin, String basePath, int keepCount) {
        this.plugin = plugin;
        this.basePath = basePath;
        this.keepCount = keepCount;
        this.dataFile = new File(plugin.getDataFolder(), "data" + File.separator + "backups.yml");
    }

    @Override
    public boolean upload(File sourceFile, String remoteName) {
        try {
            File backupDir = new File(basePath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            File dest = new File(backupDir, remoteName);
            java.nio.file.Files.copy(sourceFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            backupRecords.put(remoteName, LocalDateTime.now());
            saveData();
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[AutoBackup] §c本地备份失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean download(String remoteName, File destFile) {
        File source = new File(basePath, remoteName);
        if (!source.exists()) {
            return false;
        }
        try {
            java.nio.file.Files.copy(source.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[AutoBackup] §c恢复失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<BackupFile> listBackups() {
        File backupDir = new File(basePath);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return new ArrayList<>();
        }
        File[] files = backupDir.listFiles();
        if (files == null) return new ArrayList<>();

        List<BackupFile> result = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && (f.getName().endsWith(".zip") || f.getName().endsWith(".tar.gz"))) {
                LocalDateTime created = backupRecords.containsKey(f.getName())
                        ? backupRecords.get(f.getName())
                        : LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
                result.add(new BackupFile(f.getName(), f.length(), created, "本地"));
            }
        }
        result.sort(Comparator.reverseOrder());
        return result;
    }

    @Override
    public boolean delete(String remoteName) {
        File file = new File(basePath, remoteName);
        if (file.exists() && file.delete()) {
            backupRecords.remove(remoteName);
            saveData();
            return true;
        }
        return false;
    }

    @Override
    public int cleanup(int keepCount) {
        List<BackupFile> backups = listBackups();
        if (backups.size() <= keepCount) {
            return 0;
        }
        int deleted = 0;
        // 保留最新的 keepCount 个，删除其余
        for (int i = keepCount; i < backups.size(); i++) {
            BackupFile bf = backups.get(i);
            if (delete(bf.getName())) {
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public String testConnection() {
        File backupDir = new File(basePath);
        if (!backupDir.exists()) {
            boolean created = backupDir.mkdirs();
            if (!created) {
                return "§c无法创建备份目录: " + basePath;
            }
        }
        if (!backupDir.canWrite()) {
            return "§c备份目录不可写: " + basePath;
        }
        return "§a本地存储正常 §7(路径: " + basePath + ")";
    }

    @Override
    public String getName() {
        return "本地存储";
    }

    public void loadData() {
        if (!dataFile.exists()) {
            // 没有数据文件，尝试从目录扫描
            File backupDir = new File(basePath);
            if (backupDir.exists()) {
                File[] files = backupDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && (f.getName().endsWith(".zip") || f.getName().endsWith(".tar.gz"))) {
                            backupRecords.put(f.getName(), LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault()));
                        }
                    }
                }
            }
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            String timeStr = data.getString(key + ".time");
            if (timeStr != null) {
                try {
                    backupRecords.put(key, LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME));
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private void saveData() {
        try {
            File parent = dataFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileConfiguration data = new YamlConfiguration();
            int idx = 0;
            for (Map.Entry<String, LocalDateTime> entry : backupRecords.entrySet()) {
                String path = "backups." + idx;
                data.set(path + ".name", entry.getKey());
                data.set(path + ".time", entry.getValue().format(DateTimeFormatter.ISO_DATE_TIME));
                idx++;
            }
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[AutoBackup] §c保存备份记录失败: " + e.getMessage());
        }
    }
}
