package com.datacollect.util;

import com.datacollect.dto.TaskInfoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 端侧文件处理工具类
 * 负责解压端侧压缩包并解析taskinfo.json
 */
@Slf4j
public class ClientFileProcessor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解压端侧压缩包并解析taskinfo.json
     *
     * @param zipFilePath 压缩包文件路径
     * @return TaskInfoDTO对象，如果解析失败返回null
     * @throws IOException IO异常
     */
    public static TaskInfoDTO extractAndParseTaskInfo(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析taskinfo.json: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        TaskInfoDTO taskInfo = null;

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析taskinfo.json
            Path taskInfoPath = extractDir.resolve("taskinfo.json");
            if (!Files.exists(taskInfoPath)) {
                // 尝试在子目录中查找
                taskInfoPath = findTaskInfoFile(extractDir);
            }

            if (taskInfoPath != null && Files.exists(taskInfoPath)) {
                log.info("找到taskinfo.json文件: {}", taskInfoPath);
                taskInfo = parseTaskInfoJson(taskInfoPath);
                log.info("taskinfo.json解析成功: taskId={}", taskInfo != null ? taskInfo.getTaskId() : "null");
            } else {
                log.warn("未找到taskinfo.json文件");
            }

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return taskInfo;
    }

    /**
     * 解压ZIP文件到指定目录
     *
     * @param zipPath ZIP文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractZipFile(Path zipPath, Path extractDir) throws IOException {
        log.info("解压ZIP文件: {} -> {}", zipPath, extractDir);

        try (FileInputStream fis = new FileInputStream(zipPath.toFile());
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = extractDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);
                }

                zis.closeEntry();
            }

            log.info("ZIP文件解压完成");
        }
    }

    /**
     * 递归查找taskinfo.json文件
     *
     * @param directory 搜索目录
     * @return taskinfo.json文件路径，如果未找到返回null
     */
    private static Path findTaskInfoFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("taskinfo.json"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析taskinfo.json文件
     *
     * @param taskInfoPath taskinfo.json文件路径
     * @return TaskInfoDTO对象
     * @throws IOException IO异常
     */
    private static TaskInfoDTO parseTaskInfoJson(Path taskInfoPath) throws IOException {
        try {
            String jsonContent = new String(Files.readAllBytes(taskInfoPath), "UTF-8");
            log.debug("taskinfo.json内容: {}", jsonContent);

            TaskInfoDTO taskInfo = objectMapper.readValue(jsonContent, TaskInfoDTO.class);
            return taskInfo;
        } catch (Exception e) {
            log.error("解析taskinfo.json失败: {}", e.getMessage(), e);
            throw new IOException("解析taskinfo.json失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归删除目录
     *
     * @param directory 要删除的目录
     * @throws IOException IO异常
     */
    private static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件/目录失败: {}", path, e);
                        }
                    });
        }
    }
}

