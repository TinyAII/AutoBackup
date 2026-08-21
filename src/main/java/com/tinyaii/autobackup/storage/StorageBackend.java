package com.tinyaii.autobackup.storage;

import java.io.File;

/**
 * 存储后端抽象接口
 * 实现此接口即可添加新的存储方式（OSS、S3、FTP 等）
 */
public interface StorageBackend {

    /**
     * 上传备份文件
     * @param sourceFile 本地备份文件
     * @param remoteName 远端存储时的名称
     * @return 是否上传成功
     */
    boolean upload(File sourceFile, String remoteName);

    /**
     * 下载备份文件
     * @param remoteName 远端文件名
     * @param destFile 本地目标文件
     * @return 是否下载成功
     */
    boolean download(String remoteName, File destFile);

    /**
     * 列出所有备份文件
     * @return 备份文件列表（按时间排序，最新的在前）
     */
    java.util.List<BackupFile> listBackups();

    /**
     * 删除备份文件
     * @param remoteName 远端文件名
     * @return 是否删除成功
     */
    boolean delete(String remoteName);

    /**
     * 清理超出保留数量的旧备份
     * @param keepCount 保留数量
     * @return 清理的数量
     */
    int cleanup(int keepCount);

    /**
     * 测试连接是否正常
     * @return 连接状态信息
     */
    String testConnection();

    /**
     * 获取存储后端名称
     */
    String getName();
}
