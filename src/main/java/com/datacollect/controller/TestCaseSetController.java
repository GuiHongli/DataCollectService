package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.service.TestCaseSetService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/test-case-set")
@Validated
public class TestCaseSetController {

    @Autowired
    private TestCaseSetService testCaseSetService;

    @PostMapping
    public Result<TestCaseSet> create(@Valid @RequestBody TestCaseSet testCaseSet) {
        testCaseSetService.save(testCaseSet);
        return Result.success(testCaseSet);
    }

    @PostMapping("/upload")
    public Result<TestCaseSet> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("description") String description) {
        try {
            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".zip")) {
                return Result.error("只支持上传zip文件");
            }

            // 解析文件名获取用例集名称和版本
            String[] parts = originalFilename.replace(".zip", "").split("_");
            if (parts.length < 2) {
                return Result.error("文件名格式错误，应为：用例集名称_版本.zip");
            }

            String name = parts[0];
            String version = parts[1];

            // 检查是否已存在相同名称和版本
            QueryWrapper<TestCaseSet> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", name);
            queryWrapper.eq("version", version);
            if (testCaseSetService.count(queryWrapper) > 0) {
                return Result.error("已存在相同名称和版本的用例集");
            }

            // 创建上传目录
            String uploadDir = "uploads/testcase";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + ".zip";
            String filePath = uploadDir + "/" + fileName;

            // 保存文件
            File dest = new File(filePath);
            file.transferTo(dest);

            // 创建用例集记录
            TestCaseSet testCaseSet = new TestCaseSet();
            testCaseSet.setName(name);
            testCaseSet.setVersion(version);
            testCaseSet.setFilePath(filePath);
            testCaseSet.setFileSize(file.getSize());
            testCaseSet.setDescription(description);
            testCaseSet.setStatus(1);
            testCaseSet.setCreateBy("admin"); // 这里应该从登录用户获取

            testCaseSetService.save(testCaseSet);
            return Result.success(testCaseSet);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<TestCaseSet> update(@PathVariable @NotNull Long id, @Valid @RequestBody TestCaseSet testCaseSet) {
        testCaseSet.setId(id);
        testCaseSetService.updateById(testCaseSet);
        return Result.success(testCaseSet);
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
            queryWrapper.eq("version", version);
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
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("version");
        List<TestCaseSet> list = testCaseSetService.list(queryWrapper);
        return Result.success(list);
    }
}
