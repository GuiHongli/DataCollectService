package com.datacollect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    private String baseDir = "./uploads";
    private String testcaseDir = "testcase";
    private String tempDir = "temp";

    private Path basePath;
    private Path testcasePath;
    private Path tempPath;

    @PostConstruct
    public void init() {
        try {
            // 初始化路径
            this.basePath = Paths.get(baseDir).toAbsolutePath();
            this.testcasePath = basePath.resolve(testcaseDir);
            this.tempPath = basePath.resolve(tempDir);

            // 创建必要的目录
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            if (!Files.exists(testcasePath)) {
                Files.createDirectories(testcasePath);
            }
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化文件上传目录失败", e);
        }
    }

    /**
     * 获取测试用例上传目录
     */
    public Path getTestcaseUploadPath() {
        return testcasePath;
    }

    /**
     * 获取临时目录
     */
    public Path getTempPath() {
        return tempPath;
    }

    /**
     * 创建临时目录
     */
    public Path createTempDir(String prefix) {
        try {
            Path tempDir = tempPath.resolve(prefix + "_" + System.currentTimeMillis());
            Files.createDirectories(tempDir);
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("创建临时目录失败", e);
        }
    }
}
