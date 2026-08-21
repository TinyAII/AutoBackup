package com.tinyaii.autobackup.storage;

import com.tinyaii.autobackup.AutoBackupPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储后端工厂
 * 根据配置创建和管理所有存储后端
 */
public class StorageFactory {

    private final AutoBackupPlugin plugin;
    private final List<StorageBackend> backends = new ArrayList<>();

    public StorageFactory(AutoBackupPlugin plugin, com.tinyaii.autobackup.config.ConfigManager config) {
        this.plugin = plugin;

        // 本地存储（始终可用）
        if (config.isLocalEnabled()) {
            LocalStorage local = new LocalStorage(plugin, config.getLocalPath(), config.getLocalKeepCount());
            local.loadData();
            backends.add(local);
            plugin.getLogger().info("[AutoBackup] §a本地存储已启用 §7(路径: " + config.getLocalPath() + ")");
        }

        // 阿里云 OSS（预留接口，暂未启用）
        if (config.isAliyunEnabled()) {
            plugin.getLogger().warning("[AutoBackup] §e阿里云 OSS 接口已预留，暂未实现。待接入 SDK 后可用。");
            // 以后接入: backends.add(new AliyunOSSBackend(plugin, config));
        }

        // S3 兼容（预留接口，暂未启用）
        if (config.isS3Enabled()) {
            plugin.getLogger().warning("[AutoBackup] §eS3 兼容接口已预留，暂未实现。待接入 SDK 后可用。");
            // 以后接入: backends.add(new S3StorageBackend(plugin, config));
        }

        if (backends.isEmpty()) {
            plugin.getLogger().warning("[AutoBackup] §c没有启用的存储后端！请检查配置文件。");
        }
    }

    /**
     * 上传到所有启用的存储后端
     */
    public boolean uploadAll(java.io.File sourceFile, String remoteName) {
        boolean allSuccess = true;
        for (StorageBackend backend : backends) {
            boolean success = backend.upload(sourceFile, remoteName);
            if (!success) {
                plugin.getLogger().warning("[AutoBackup] §c" + backend.getName() + " 上传失败: " + remoteName);
                allSuccess = false;
            } else {
                plugin.getLogger().info("[AutoBackup] §a" + backend.getName() + " 上传成功: " + remoteName);
            }
        }
        return allSuccess;
    }

    /**
     * 清理所有存储后端的旧备份
     */
    public void cleanupAll() {
        for (StorageBackend backend : backends) {
            int deleted = backend.cleanup(getKeepCount(backend));
            if (deleted > 0) {
                plugin.getLogger().info("[AutoBackup] §7" + backend.getName() + " 清理了 " + deleted + " 个旧备份");
            }
        }
    }

    /**
     * 获取所有备份列表
     */
    public List<BackupFile> listAllBackups() {
        List<BackupFile> all = new ArrayList<>();
        for (StorageBackend backend : backends) {
            all.addAll(backend.listBackups());
        }
        all.sort(java.util.Comparator.reverseOrder());
        return all;
    }

    /**
     * 测试所有后端连接
     */
    public List<String> testAllConnections() {
        List<String> results = new ArrayList<>();
        for (StorageBackend backend : backends) {
            results.add(backend.testConnection());
        }
        return results;
    }

    /**
     * 获取指定后端的保留数量
     */
    private int getKeepCount(StorageBackend backend) {
        com.tinyaii.autobackup.config.ConfigManager config = plugin.getConfigManager();
        if (backend instanceof LocalStorage) {
            return config.getLocalKeepCount();
        }
        // 预留：OSS/S3 的保留数量
        return 10;
    }

    public List<StorageBackend> getBackends() {
        return backends;
    }
}
