package com.tinyaii.autobackup;

import com.tinyaii.autobackup.BackupTask;
import com.tinyaii.autobackup.command.BackupCommand;
import com.tinyaii.autobackup.config.ConfigManager;
import com.tinyaii.autobackup.storage.StorageFactory;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoBackupPlugin extends JavaPlugin {

    private static AutoBackupPlugin instance;
    private ConfigManager configManager;
    private BackupTask backupTask;
    private StorageFactory storageFactory;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info(" _____ _                _    ___ ___");
        getLogger().info("|_   _(_)_ __  _   _   / \\  |_ _|_ _|");
        getLogger().info("  | | | | '_ \\| | | | / _ \\  | | | |");
        getLogger().info("  | | | | | | | |_| |/ ___ \\ | | | |");
        getLogger().info("  |_| |_|_| |_|\\__, /_/   \\_\\___|___|");
        getLogger().info("               |___/");
        getLogger().info("自动备份插件 v" + getDescription().getVersion() + " - TinyAII 出品");

        configManager = new ConfigManager(this);
        configManager.load();

        storageFactory = new StorageFactory(this, configManager);

        BackupCommand commandHandler = new BackupCommand(this, configManager, storageFactory);
        getCommand("备份").setExecutor(commandHandler);
        getCommand("备份").setTabCompleter(commandHandler);

        backupTask = new BackupTask(this, configManager, storageFactory);
        backupTask.start();

        getLogger().info("[AutoBackup] 定时备份间隔: " + configManager.getInterval() + " 分钟");
        getLogger().info("[AutoBackup] 停服前备份: " + (configManager.isShutdownBackupEnabled() ? "开启" : "关闭"));
    }

    @Override
    public void onDisable() {
        if (backupTask != null) {
            backupTask.stop();
        }
        getLogger().info("[AutoBackup] 自动备份插件已禁用");
    }

    public static AutoBackupPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageFactory getStorageFactory() {
        return storageFactory;
    }
}
