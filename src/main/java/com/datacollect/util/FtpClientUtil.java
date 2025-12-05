package com.datacollect.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * FTP客户端工具类
 * 用于连接FTP服务器、下载文件等操作
 */
@Slf4j
public class FtpClientUtil {

    /**
     * 连接FTP服务器并下载文件
     *
     * @param serverAddress FTP服务器地址（格式：host:port 或 host）
     * @param account 账户
     * @param password 密码
     * @param remoteDirectory 远程目录
     * @param remoteFileName 远程文件名
     * @param localFilePath 本地保存路径
     * @return 是否下载成功
     * @throws IOException IO异常
     */
    public static boolean downloadFile(String serverAddress, String account, String password,
                                      String remoteDirectory, String remoteFileName, String localFilePath) throws IOException {
        FTPClient ftpClient = new FTPClient();
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // 解析服务器地址和端口
            String host;
            int port = 21; // 默认FTP端口
            if (serverAddress.contains(":")) {
                String[] parts = serverAddress.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                host = serverAddress;
            }

            log.info("Connecting to FTP server: {}:{}", host, port);

            // 连接FTP服务器
            ftpClient.connect(host, port);
            int replyCode = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();
                throw new IOException("FTP server refused connection. Reply code: " + replyCode);
            }

            // 登录
            boolean loginSuccess = ftpClient.login(account, password);
            if (!loginSuccess) {
                ftpClient.disconnect();
                throw new IOException("FTP login failed. Check username and password.");
            }

            log.info("FTP login successful");

            // 设置文件传输模式为二进制
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // 设置被动模式
            ftpClient.enterLocalPassiveMode();

            // 切换到指定目录
            if (remoteDirectory != null && !remoteDirectory.trim().isEmpty()) {
                boolean changeDirSuccess = ftpClient.changeWorkingDirectory(remoteDirectory);
                if (!changeDirSuccess) {
                    log.warn("Failed to change directory to: {}. Current directory: {}", remoteDirectory, ftpClient.printWorkingDirectory());
                } else {
                    log.info("Changed to directory: {}", remoteDirectory);
                }
            }

            // 检查文件是否存在
            FTPFile[] files = ftpClient.listFiles(remoteFileName);
            if (files.length == 0) {
                throw new IOException("File not found on FTP server: " + remoteFileName);
            }

            log.info("File found on FTP server: {} (size: {} bytes)", remoteFileName, files[0].getSize());

            // 创建本地目录
            Path localPath = Paths.get(localFilePath);
            Files.createDirectories(localPath.getParent());

            // 下载文件
            inputStream = ftpClient.retrieveFileStream(remoteFileName);
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream: " + remoteFileName);
            }

            outputStream = new FileOutputStream(localFilePath);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            log.info("File downloaded successfully: {} ({} bytes)", localFilePath, totalBytesRead);

            // 完成文件传输
            boolean completed = ftpClient.completePendingCommand();
            if (!completed) {
                log.warn("File transfer may not be completed properly");
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to download file from FTP server: {}", e.getMessage(), e);
            throw new IOException("FTP download failed: " + e.getMessage(), e);
        } finally {
            // 关闭流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close input stream: {}", e.getMessage());
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close output stream: {}", e.getMessage());
                }
            }

            // 断开连接
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    log.warn("Failed to disconnect FTP client: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 从FTP服务器读取文件内容（用于读取MD5文件等文本文件）
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param remoteDirectory 远程目录
     * @param remoteFileName 远程文件名
     * @return 文件内容
     * @throws IOException IO异常
     */
    public static String readFileContent(String serverAddress, String account, String password,
                                        String remoteDirectory, String remoteFileName) throws IOException {
        FTPClient ftpClient = new FTPClient();
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            // 解析服务器地址和端口
            String host;
            int port = 21;
            if (serverAddress.contains(":")) {
                String[] parts = serverAddress.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                host = serverAddress;
            }

            log.info("Connecting to FTP server to read file: {}:{}", host, port);

            // 连接FTP服务器
            ftpClient.connect(host, port);
            int replyCode = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();
                throw new IOException("FTP server refused connection. Reply code: " + replyCode);
            }

            // 登录
            boolean loginSuccess = ftpClient.login(account, password);
            if (!loginSuccess) {
                ftpClient.disconnect();
                throw new IOException("FTP login failed. Check username and password.");
            }

            // 设置文件传输模式为ASCII（文本文件）
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);

            // 设置被动模式
            ftpClient.enterLocalPassiveMode();

            // 切换到指定目录
            if (remoteDirectory != null && !remoteDirectory.trim().isEmpty()) {
                boolean changeDirSuccess = ftpClient.changeWorkingDirectory(remoteDirectory);
                if (!changeDirSuccess) {
                    log.warn("Failed to change directory to: {}", remoteDirectory);
                }
            }

            // 读取文件内容
            inputStream = ftpClient.retrieveFileStream(remoteFileName);
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream: " + remoteFileName);
            }

            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            // 完成文件传输
            ftpClient.completePendingCommand();

            return content.toString().trim();

        } catch (Exception e) {
            log.error("Failed to read file content from FTP server: {}", e.getMessage(), e);
            throw new IOException("FTP read file failed: " + e.getMessage(), e);
        } finally {
            // 关闭流
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Failed to close reader: {}", e.getMessage());
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close input stream: {}", e.getMessage());
                }
            }

            // 断开连接
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    log.warn("Failed to disconnect FTP client: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 列出FTP服务器指定目录下的所有文件
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param remoteDirectory 远程目录
     * @return 文件名列表
     * @throws IOException IO异常
     */
    public static List<String> listFiles(String serverAddress, String account, String password,
                                         String remoteDirectory) throws IOException {
        FTPClient ftpClient = new FTPClient();
        List<String> fileNames = new ArrayList<>();

        try {
            // 解析服务器地址和端口
            String host;
            int port = 21;
            if (serverAddress.contains(":")) {
                String[] parts = serverAddress.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                host = serverAddress;
            }

            log.info("Connecting to FTP server to list files: {}:{}", host, port);

            // 连接FTP服务器
            ftpClient.connect(host, port);
            int replyCode = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect();
                throw new IOException("FTP server refused connection. Reply code: " + replyCode);
            }

            // 登录
            boolean loginSuccess = ftpClient.login(account, password);
            if (!loginSuccess) {
                ftpClient.disconnect();
                throw new IOException("FTP login failed. Check username and password.");
            }

            log.info("FTP login successful");

            // 设置被动模式
            ftpClient.enterLocalPassiveMode();

            // 切换到指定目录
            if (remoteDirectory != null && !remoteDirectory.trim().isEmpty()) {
                boolean changeDirSuccess = ftpClient.changeWorkingDirectory(remoteDirectory);
                if (!changeDirSuccess) {
                    log.warn("Failed to change directory to: {}. Current directory: {}", 
                            remoteDirectory, ftpClient.printWorkingDirectory());
                    throw new IOException("Failed to change directory to: " + remoteDirectory);
                } else {
                    log.info("Changed to directory: {}", remoteDirectory);
                }
            }

            // 列出目录下的所有文件
            FTPFile[] files = ftpClient.listFiles();
            log.info("Found {} items in directory", files.length);

            for (FTPFile file : files) {
                if (file.isFile()) {
                    // 排除.md5文件
                    if (!file.getName().endsWith(".md5")) {
                        fileNames.add(file.getName());
                        log.debug("Found file: {} (size: {} bytes)", file.getName(), file.getSize());
                    }
                }
            }

            log.info("Listed {} files from FTP directory: {}", fileNames.size(), remoteDirectory);
            return fileNames;

        } catch (Exception e) {
            log.error("Failed to list files from FTP server: {}", e.getMessage(), e);
            throw new IOException("FTP list files failed: " + e.getMessage(), e);
        } finally {
            // 断开连接
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    log.warn("Failed to disconnect FTP client: {}", e.getMessage());
                }
            }
        }
    }
}

