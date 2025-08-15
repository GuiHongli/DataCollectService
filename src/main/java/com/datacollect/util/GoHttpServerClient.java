package com.datacollect.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Component
public class GoHttpServerClient {

    @Value("${gohttpserver.url:http://localhost:8081}")
    private String goHttpServerUrl;

    private final HttpClient httpClient;

    public GoHttpServerClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 上传文件到gohttpserver
     * @param file 要上传的文件
     * @param targetFileName 目标文件名
     * @return 上传后的文件URL
     */
    public String uploadFile(MultipartFile file, String targetFileName) throws IOException {
        log.info("开始上传文件到gohttpserver: {}", targetFileName);
        
        try {
            // 构建上传URL，使用gohttpserver的标准上传接口
            String uploadUrl = goHttpServerUrl + "/upload";
            
            // 构建multipart请求
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            // 构建multipart body，直接上传到根目录
            byte[] fileBytes = file.getBytes();
            byte[] multipartBody = buildMultipartBody(fileBytes, targetFileName, boundary);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String fileUrl = goHttpServerUrl + "/upload/" + targetFileName;
                log.info("文件上传成功: {}", fileUrl);
                return fileUrl;
            } else {
                throw new IOException("上传失败，HTTP状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
            
        } catch (Exception e) {
            log.error("上传文件到gohttpserver失败: {}", e.getMessage());
            throw new IOException("上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到gohttpserver
     * @param localFilePath 本地文件路径
     * @param targetFileName 目标文件名
     * @return 上传后的文件URL
     */
    public String uploadLocalFile(String localFilePath, String targetFileName) throws IOException {
        log.info("开始上传本地文件到gohttpserver: {} -> {}", localFilePath, targetFileName);
        
        try {
            Path sourcePath = Paths.get(localFilePath);
            if (!Files.exists(sourcePath)) {
                throw new IOException("源文件不存在: " + localFilePath);
            }
            
            // 构建上传URL，使用gohttpserver的标准上传接口
            String uploadUrl = goHttpServerUrl + "/upload";
            
            // 读取文件内容
            byte[] fileBytes = Files.readAllBytes(sourcePath);
            
            // 构建multipart请求，直接上传到根目录
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            byte[] multipartBody = buildMultipartBody(fileBytes, targetFileName, boundary);
            
            // 发送HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String fileUrl = goHttpServerUrl + "/upload/" + targetFileName;
                log.info("本地文件上传成功: {}", fileUrl);
                return fileUrl;
            } else {
                throw new IOException("上传失败，HTTP状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
            
        } catch (Exception e) {
            log.error("上传本地文件到gohttpserver失败: {}", e.getMessage());
            throw new IOException("上传本地文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建multipart body
     */
    private byte[] buildMultipartBody(byte[] fileBytes, String fileName, String boundary) throws IOException {
        StringBuilder body = new StringBuilder();
        
        // 添加文件部分
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        body.append("Content-Type: application/zip\r\n");
        body.append("\r\n");
        
        // 转换为字节数组
        byte[] headerBytes = body.toString().getBytes("UTF-8");
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
        
        // 组合完整的multipart body
        byte[] multipartBody = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, multipartBody, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + fileBytes.length, footerBytes.length);
        
        return multipartBody;
    }

    /**
     * 删除gohttpserver中的文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileName) {
        try {
            String deleteUrl = goHttpServerUrl + "/test_case_set/" + fileName;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .DELETE()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("文件删除成功: {}", fileName);
                return true;
            } else {
                log.warn("文件删除失败，HTTP状态码: {}", response.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查gohttpserver是否可用
     * @return 是否可用
     */
    public boolean isAvailable() {
        try {
            String healthUrl = goHttpServerUrl + "/";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.error("gohttpserver不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取gohttpserver配置信息
     * @return 配置信息
     */
    public String getConfigInfo() {
        return String.format("GoHttpServer配置 - URL: %s", goHttpServerUrl);
    }
}
