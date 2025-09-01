package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.service.TestCaseService;
import com.datacollect.service.TestCaseSetService;
import com.datacollect.util.ExcelParser;
import com.datacollect.util.ZipProcessor;
import com.datacollect.util.GoHttpServerClient;
import com.datacollect.config.FileUploadConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/test-case-set")
@Validated
public class TestCaseSetController {

    @Autowired
    private TestCaseSetService testCaseSetService;
    
    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private ZipProcessor zipProcessor;
    
    @Autowired
    private ExcelParser excelParser;
    
    @Autowired
    private GoHttpServerClient goHttpServerClient;
    
    @Autowired
    private FileUploadConfig fileUploadConfig;

    @PostMapping
    public Result<TestCaseSet> create(@Valid @RequestBody TestCaseSet testCaseSet) {
        testCaseSetService.save(testCaseSet);
        return Result.success(testCaseSet);
    }

    @PostMapping("/upload")
    public Result<TestCaseSet> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("description") String description) {
        String extractDir = null;
        try {
            // 验证文件
            String originalFilename = file.getOriginalFilename();
            if (!isValidFile(file, originalFilename)) {
                return Result.error("文件验证失败");
            }

            // 解析文件名获取用例集名称和版本
            String[] nameAndVersion = parseFileName(originalFilename);
            if (nameAndVersion == null) {
                return Result.error("文件名格式错误，应为：用例集名称_版本.zip");
            }

            String name = nameAndVersion[0];
            String version = nameAndVersion[1];

            // 检查是否已存在相同名称和版本
            if (isDuplicateTestCaseSet(name, version)) {
                return Result.error("已存在相同名称和版本的用例集");
            }

            // 保存文件到本地
            Path filePath = saveFileToLocal(file, originalFilename);

            // 上传到gohttpserver
            String goHttpServerUrl = uploadToGoHttpServer(filePath, originalFilename);

            // 创建用例集记录
            TestCaseSet testCaseSet = createTestCaseSet(name, version, filePath, goHttpServerUrl, file.getSize(), description);
            testCaseSetService.save(testCaseSet);

            // 处理ZIP文件并解析Excel
            List<TestCase> testCases = processZipFileAndParseExcel(filePath, testCaseSet.getId());
            if (testCases.isEmpty()) {
                return Result.error("Excel文件中未解析到有效的测试用例数据");
            }

            // 批量保存测试用例
            testCaseService.saveBatch(testCases);
            
            log.info("成功上传用例集: {}, 版本: {}, 包含 {} 个测试用例", name, version, testCases.size());

            return Result.success(testCaseSet);

        } catch (IOException e) {
            log.error("文件上传或解析失败", e);
            return Result.error("文件上传或解析失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("处理用例集上传时发生错误", e);
            return Result.error("处理用例集上传时发生错误：" + e.getMessage());
        } finally {
            // 清理临时文件
            if (extractDir != null) {
                zipProcessor.cleanupExtractedFiles(extractDir);
            }
        }
    }

    /**
     * 验证文件
     */
    private boolean isValidFile(MultipartFile file, String originalFilename) {
        if (originalFilename == null || !originalFilename.endsWith(".zip")) {
            return false;
        }

        long maxSize = 100 * 1024 * 1024; // 100MB
        if (file.getSize() > maxSize) {
            return false;
        }

        return true;
    }

    /**
     * 解析文件名获取用例集名称和版本
     */
    private String[] parseFileName(String originalFilename) {
        String[] parts = originalFilename.replace(".zip", "").split("_");
        if (parts.length < 2) {
            return null;
        }
        return parts;
    }

    /**
     * 检查是否已存在相同名称和版本的用例集
     */
    private boolean isDuplicateTestCaseSet(String name, String version) {
        QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.eq("version", version);
        queryWrapper.eq("deleted", 0); // 只查未被软删除的记录
        return testCaseSetService.count(queryWrapper) > 0;
    }

    /**
     * 保存文件到本地
     */
    private Path saveFileToLocal(MultipartFile file, String originalFilename) throws IOException {
        Path uploadPath = fileUploadConfig.getTestcaseUploadPath();
        String originalFileName = originalFilename;
        Path filePath = uploadPath.resolve(originalFileName);
        
        // 如果文件已存在，添加时间戳后缀
        if (Files.exists(filePath)) {
            String nameWithoutExt = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
            String timestamp = String.valueOf(System.currentTimeMillis());
            originalFileName = nameWithoutExt + "_" + timestamp + extension;
            filePath = uploadPath.resolve(originalFileName);
        }

        File dest = filePath.toFile();
        file.transferTo(dest);
        return filePath;
    }

    /**
     * 上传到gohttpserver
     */
    private String uploadToGoHttpServer(Path filePath, String originalFilename) {
        String goHttpServerUrl = null;
        try {
            if (goHttpServerClient.isAvailable()) {
                goHttpServerUrl = goHttpServerClient.uploadLocalFile(filePath.toString(), originalFilename);
                log.info("文件已上传到gohttpserver: {}", goHttpServerUrl);
            } else {
                log.warn("gohttpserver不可用，跳过上传");
            }
        } catch (Exception e) {
            log.error("上传到gohttpserver失败: {}", e.getMessage());
            // 不影响主要流程，继续执行
        }
        return goHttpServerUrl;
    }

    /**
     * 创建用例集记录
     */
    private TestCaseSet createTestCaseSet(String name, String version, Path filePath, 
                                        String goHttpServerUrl, long fileSize, String description) {
        TestCaseSet testCaseSet = new TestCaseSet();
        testCaseSet.setName(name);
        testCaseSet.setVersion(version);
        testCaseSet.setFilePath(filePath.toString());
        testCaseSet.setGohttpserverUrl(goHttpServerUrl);
        testCaseSet.setFileSize(fileSize);
        testCaseSet.setDescription(description);
        testCaseSet.setStatus(1);
        testCaseSet.setCreateBy("admin"); // 这里应该从登录用户获取
        return testCaseSet;
    }

    /**
     * 处理ZIP文件并解析Excel
     */
    private List<TestCase> processZipFileAndParseExcel(Path filePath, Long testCaseSetId) throws IOException {
        Path extractPath = fileUploadConfig.createTempDir("extract");
        String excelFilePath = zipProcessor.extractAndFindExcel(filePath.toString(), extractPath.toString(), "cases.xlsx");
        
        if (excelFilePath == null) {
            throw new IOException("ZIP文件中未找到case.xlsx文件");
        }

        return excelParser.parseTestCaseExcel(excelFilePath, testCaseSetId);
    }



    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        TestCaseSet testCaseSet = testCaseSetService.getById(id);
        if (testCaseSet != null && testCaseSet.getFilePath() != null) {
            // 删除物理文件
            try {
                Files.deleteIfExists(Paths.get(testCaseSet.getFilePath()));
            } catch (IOException e) {
                log.error("删除文件失败", e);
            }
        }
        
        // 删除关联的测试用例
        QueryWrapper<TestCase> testCaseQuery = new QueryWrapper<>();
        testCaseQuery.eq("test_case_set_id", id);
        testCaseService.remove(testCaseQuery);
        
        boolean result = testCaseSetService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<TestCaseSet> getById(@PathVariable @NotNull Long id) {
        TestCaseSet testCaseSet = testCaseSetService.getById(id);
        return Result.success(testCaseSet);
    }

    @GetMapping("/page")
    public Result<Page<TestCaseSet>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String version) {
        
        Page<TestCaseSet> page = new Page<>(current, size);
        QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (version != null && !version.isEmpty()) {
            queryWrapper.like("version", version);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<TestCaseSet> result = testCaseSetService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<TestCaseSet>> list() {
        QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        List<TestCaseSet> list = testCaseSetService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/name/{name}")
    public Result<List<TestCaseSet>> getByName(@PathVariable @NotNull String name) {
        QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.orderByDesc("create_time");
        List<TestCaseSet> list = testCaseSetService.list(queryWrapper);
        return Result.success(list);
    }
    
    /**
     * 获取用例集的测试用例列表
     */
    @GetMapping("/{id}/test-cases")
    public Result<List<TestCase>> getTestCases(@PathVariable @NotNull Long id) {
        List<TestCase> testCases = testCaseService.getByTestCaseSetId(id);
        return Result.success(testCases);
    }
    
    /**
     * 获取gohttpserver配置信息
     */
    @GetMapping("/gohttpserver/config")
    public Result<String> getGoHttpServerConfig() {
        String configInfo = goHttpServerClient.getConfigInfo();
        boolean isAvailable = goHttpServerClient.isAvailable();
        return Result.success(configInfo + ", 状态: " + (isAvailable ? "可用" : "不可用"));
    }
    
    /**
     * 手动上传文件到gohttpserver
     */
    @PostMapping("/gohttpserver/upload")
    public Result<String> uploadToGoHttpServer(@RequestParam("file") MultipartFile file,
                                              @RequestParam("targetFileName") String targetFileName) {
        try {
            String fileUrl = goHttpServerClient.uploadFile(file, targetFileName);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("上传到gohttpserver失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 清理软删除的记录
     */
    @DeleteMapping("/cleanup-soft-deleted")
    public Result<String> cleanupSoftDeleted() {
        try {
            // 查询软删除的记录
            QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("deleted", 1);
            List<TestCaseSet> softDeletedRecords = testCaseSetService.list(queryWrapper);
            
            if (softDeletedRecords.isEmpty()) {
                return Result.success("没有软删除的记录需要清理");
            }
            
            // 删除相关的测试用例记录
            for (TestCaseSet record : softDeletedRecords) {
                QueryWrapper<TestCase> testCaseQuery = new QueryWrapper<>();
                testCaseQuery.eq("test_case_set_id", record.getId());
                testCaseService.remove(testCaseQuery);
            }
            
            // 删除软删除的用例集记录
            testCaseSetService.remove(queryWrapper);
            
            return Result.success("成功清理 " + softDeletedRecords.size() + " 条软删除记录");
        } catch (Exception e) {
            log.error("清理软删除记录失败", e);
            return Result.error("清理失败：" + e.getMessage());
        }
    }
}
