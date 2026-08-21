package com.tinyaii.autobackup.storage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 备份文件信息
 */
public class BackupFile implements Comparable<BackupFile> {
    private final String name;
    private final long size;
    private final LocalDateTime createdAt;
    private final String storageType;

    public BackupFile(String name, long size, LocalDateTime createdAt, String storageType) {
        this.name = name;
        this.size = size;
        this.createdAt = createdAt;
        this.storageType = storageType;
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStorageType() { return storageType; }

    public String getSizeFormatted() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    public String getTimeFormatted() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public int compareTo(BackupFile other) {
        return other.createdAt.compareTo(this.createdAt); // 降序，最新的在前
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s",
                name, getSizeFormatted(), getTimeFormatted(), storageType);
    }
}
