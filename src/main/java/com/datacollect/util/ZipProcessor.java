package com.datacollect.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class ZipProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipProcessor.class);

    /**
     * 解压ZIP文件并查找指定的Excel文件
     * @param zipFilePath ZIP文件路径
     * @param extractDir 解压目录
     * @param excelFileName 要查找的Excel文件名
     * @return Excel文件的完整路径，如果未找到返回null
     */
    public String extractAndFindExcel(String zipFilePath, String extractDir, String excelFileName) throws IOException {
        // 创建解压目录
        Path extractPath = Paths.get(extractDir);
        if (!Files.exists(extractPath)) {
            Files.createDirectories(extractPath);
        }
        
        String foundExcelPath = null;
        
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipArchiveInputStream zis = new ZipArchiveInputStream(fis)) {
            
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    LOGGER.info("Extracting file: {}", entryName);
                    
                    // check是否是目标Excel文件
                    if (entryName.equals(excelFileName)) {
                        foundExcelPath = extractDir + File.separator + entryName;
                    }
                    
                    // Extracting  file
                    File outputFile = new File(extractDir, entryName);
                    // 确保父目录存在
                    outputFile.getParentFile().mkdirs();
                    
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
        
        if (foundExcelPath != null) {
            LOGGER.info("Found Excel file: {}", foundExcelPath);
        } else {
            LOGGER.warn("Specified Excel file not found: {}", excelFileName);
        }
        
        return foundExcelPath;
    }
    
    /**
     * 清理Extracting 的临时 file
     * @param extractDir 解压目录
     */
    public void cleanupExtractedFiles(String extractDir) {
        try {
            Path extractPath = Paths.get(extractDir);
            if (Files.exists(extractPath)) {
                deleteDirectory(extractPath.toFile());
                LOGGER.info("Cleaned up temporary file directory: {}", extractDir);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to clean up temporary files: {}", e.getMessage());
        }
    }
    
    /**
     * 递归删除目录
     * @param directory 要删除的目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
