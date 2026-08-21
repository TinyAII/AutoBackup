package com.tinyaii.autobackup.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    // 定时备份间隔（分钟）
    private int interval;
    // 停服前备份
    private boolean shutdownBackupEnabled;
    // 压缩格式: zip 或 tar.gz
    private String compressFormat;
    // 命名格式
    private String namingFormat;

    // 备份内容开关
    private boolean backupWorlds;
    private boolean backupPlugins;
    private boolean backupConfig;
    private boolean backupWhitelist;

    // 本地存储配置
    private boolean localEnabled;
    private String localPath;
    private int localKeepCount;

    // 阿里云 OSS
    private boolean aliyunEnabled;
    private String aliyunEndpoint;
    private String aliyunBucket;
    private String aliyunAccessKey;
    private String aliyunSecretKey;
    private String aliyunPathPrefix;
    private int aliyunKeepCount;

    // S3 兼容
    private boolean s3Enabled;
    private String s3Endpoint;
    private String s3Bucket;
    private String s3AccessKey;
    private String s3SecretKey;
    private String s3PathPrefix;
    private int s3KeepCount;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 首次加载：确保默认配置存在 + 读取并解析
     */
    public void load() {
        saveDefaultConfig();
        config = plugin.getConfig(); // Bukkit 首次加载时已缓存 config.yml
        parse();
    }

    /**
     * 重载：让 Bukkit 重新读取 config.yml 磁盘文件，再解析
     */
    public void reloadConfig() {
        plugin.reloadConfig(); // 让 Bukkit 重新读磁盘上的 config.yml
        config = plugin.getConfig();
        parse();
    }

    /**
     * 从 config 中解析所有字段到内存变量
     */
    private void parse() {
        interval = config.getInt("间隔", 360);
        shutdownBackupEnabled = config.getBoolean("停服前备份.启用", false);
        compressFormat = config.getString("压缩格式", "tar.gz");
        namingFormat = config.getString("命名格式", "backup-{date}-{time}-{count}");

        ConfigurationSection backupContent = config.getConfigurationSection("备份内容");
        if (backupContent != null) {
            backupWorlds = backupContent.getBoolean("世界", true);
            backupPlugins = backupContent.getBoolean("插件", true);
            backupConfig = backupContent.getBoolean("配置", true);
            backupWhitelist = backupContent.getBoolean("白名单", true);
        } else {
            backupWorlds = true;
            backupPlugins = true;
            backupConfig = true;
            backupWhitelist = true;
        }

        ConfigurationSection local = config.getConfigurationSection("本地");
        if (local != null) {
            localEnabled = local.getBoolean("启用", true);
            localPath = local.getString("路径", "./backups");
            localKeepCount = local.getInt("保留数量", 10);
        } else {
            localEnabled = true;
            localPath = "./backups";
            localKeepCount = 10;
        }

        ConfigurationSection aliyun = config.getConfigurationSection("阿里云OSS");
        if (aliyun != null) {
            aliyunEnabled = aliyun.getBoolean("启用", false);
            aliyunEndpoint = aliyun.getString("节点", "oss-cn-hangzhou.aliyuncs.com");
            aliyunBucket = aliyun.getString("桶名", "");
            aliyunAccessKey = aliyun.getString("访问密钥", "");
            aliyunSecretKey = aliyun.getString("秘密密钥", "");
            aliyunPathPrefix = aliyun.getString("路径前缀", "mc-backups/");
            aliyunKeepCount = aliyun.getInt("保留数量", 30);
        } else {
            aliyunEnabled = false;
        }

        ConfigurationSection s3 = config.getConfigurationSection("S3兼容");
        if (s3 != null) {
            s3Enabled = s3.getBoolean("启用", false);
            s3Endpoint = s3.getString("节点", "s3.amazonaws.com");
            s3Bucket = s3.getString("桶名", "");
            s3AccessKey = s3.getString("访问密钥", "");
            s3SecretKey = s3.getString("秘密密钥", "");
            s3PathPrefix = s3.getString("路径前缀", "mc-backups/");
            s3KeepCount = s3.getInt("保留数量", 30);
        } else {
            s3Enabled = false;
        }
    }

    /**
     * 确保默认配置文件已复制到插件数据目录
     */
    private void saveDefaultConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    // ===== Getters =====
    public int getInterval() { return Math.max(interval, 0); }
    public boolean isShutdownBackupEnabled() { return shutdownBackupEnabled; }
    public String getCompressFormat() { return compressFormat; }
    public String getNamingFormat() { return namingFormat; }
    public boolean isBackupWorlds() { return backupWorlds; }
    public boolean isBackupPlugins() { return backupPlugins; }
    public boolean isBackupConfig() { return backupConfig; }
    public boolean isBackupWhitelist() { return backupWhitelist; }
    public boolean isLocalEnabled() { return localEnabled; }
    public String getLocalPath() { return localPath; }
    public int getLocalKeepCount() { return localKeepCount; }
    public boolean isAliyunEnabled() { return aliyunEnabled; }
    public String getAliyunEndpoint() { return aliyunEndpoint; }
    public String getAliyunBucket() { return aliyunBucket; }
    public String getAliyunAccessKey() { return aliyunAccessKey; }
    public String getAliyunSecretKey() { return aliyunSecretKey; }
    public String getAliyunPathPrefix() { return aliyunPathPrefix; }
    public int getAliyunKeepCount() { return aliyunKeepCount; }
    public boolean isS3Enabled() { return s3Enabled; }
    public String getS3Endpoint() { return s3Endpoint; }
    public String getS3Bucket() { return s3Bucket; }
    public String getS3AccessKey() { return s3AccessKey; }
    public String getS3SecretKey() { return s3SecretKey; }
    public String getS3PathPrefix() { return s3PathPrefix; }
    public int getS3KeepCount() { return s3KeepCount; }
}
