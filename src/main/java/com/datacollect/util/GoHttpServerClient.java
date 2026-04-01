package com.datacollect.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class GoHttpServerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoHttpServerClient.class);

    @Value("${gohttpserver.url:http://localhost:8081}")
    private String goHttpServerUrl;

    private final CloseableHttpClient httpClient;

    public GoHttpServerClient() {
        // 配置超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000) // 30秒连接超时
                .setSocketTimeout(60000)  // 60秒读取超时
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * 上传文件到gohttpserver
     * @param file 要上传的文件
     * @param targetFileName 目标文件名
     * @return 上传后的文件URL
     */
    public String uploadFile(MultipartFile file, String targetFileName) throws IOException {
        LOGGER.info("Starting file upload to gohttpserver: {}", targetFileName);
        
        try {
            // 构建上传URL，使用gohttpserver的标准上传接口
            String uploadUrl = goHttpServerUrl + "/upload";
            
            // 创建multipart请求
            HttpPost httpPost = new HttpPost(uploadUrl);
            
            // 构建multipart entity
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("file", file.getBytes(), 
                    ContentType.create("application/zip"), targetFileName);
            
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                
                if (statusCode == 200 || statusCode == 201) {
                    String fileUrl = goHttpServerUrl + "/upload/" + targetFileName;
                    LOGGER.info("File upload successful: {}", fileUrl);
                    return fileUrl;
                } else {
                    throw new IOException("Upload failed, HTTP status code: " + statusCode + ", Response: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to upload file to gohttpserver: {}", e.getMessage());
            throw new IOException("File upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到gohttpserver
     * @param localFilePath 本地文件路径
     * @param targetFileName 目标文件名
     * @return 上传后的文件URL
     */
    public String uploadLocalFile(String localFilePath, String targetFileName) throws IOException {
        LOGGER.info("Starting local file upload to gohttpserver: {} -> {}", localFilePath, targetFileName);
        
        try {
            Path sourcePath = Paths.get(localFilePath);
            if (!Files.exists(sourcePath)) {
                throw new IOException("Source file does not exist: " + localFilePath);
            }
            
            // 构建上传URL，使用gohttpserver的标准上传接口
            String uploadUrl = goHttpServerUrl + "/upload";
            
            // 创建multipart请求
            HttpPost httpPost = new HttpPost(uploadUrl);
            
            // 构建multipart entity
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("file", Files.readAllBytes(sourcePath), 
                    ContentType.create("application/zip"), targetFileName);
            
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                
                if (statusCode == 200 || statusCode == 201) {
                    String fileUrl = goHttpServerUrl + "/upload/" + targetFileName;
                    LOGGER.info("Local file upload successful: {}", fileUrl);
                    return fileUrl;
                } else {
                    throw new IOException("Upload failed, HTTP status code: " + statusCode + ", Response: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to upload local file to gohttpserver: {}", e.getMessage());
            throw new IOException("Local file upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除gohttpserver中的文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileName) {
        try {
            String deleteUrl = goHttpServerUrl + "/test_case_set/" + fileName;
            
            HttpDelete httpDelete = new HttpDelete(deleteUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200 || statusCode == 204) {
                    LOGGER.info("File deleted successfully: {}", fileName);
                    return true;
                } else {
                    LOGGER.warn("File deletion failed, HTTP status code: {}", statusCode);
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * checkgohttpserver是否available
     * @return 是否available
     */
    public boolean isAvailable() {
        try {
            String healthUrl = goHttpServerUrl + "/";
            
            HttpGet httpGet = new HttpGet(healthUrl);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode == 200;
            }
        } catch (Exception e) {
            LOGGER.error("gohttpserver is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * getgohttpserver配置信息
     * @return 配置信息
     */
    public String getConfigInfo() {
        return String.format("GoHttpServer Configuration - URL: %s", goHttpServerUrl);
    }
}
