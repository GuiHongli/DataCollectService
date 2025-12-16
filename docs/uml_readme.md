# UML 图说明文档

本文档包含端侧数据采集系统的 UML 类图和时序图。

## 文件说明

### 1. uml_class_diagram.puml
**类图** - 展示了系统中主要类之间的关系

包含以下内容：
- **实体类（Entity）**: TaskInfo, SpeedData, VmosData
- **DTO类**: TaskInfoDTO
- **Mapper接口**: TaskInfoMapper, SpeedDataMapper, VmosDataMapper
- **Service接口**: TaskInfoService, SpeedDataService, VmosDataService
- **Service实现类**: TaskInfoServiceImpl, SpeedDataServiceImpl, VmosDataServiceImpl
- **工具类**: ClientFileProcessor
- **服务类**: FtpFileProcessService

### 2. uml_sequence_diagram.puml
**时序图** - 展示了处理端侧FTP文件的完整流程

流程包括：
1. 从FTP服务器下载压缩包
2. MD5校验（可选）
3. 解析taskinfo.json并保存
4. 解析speed-10s.csv并保存
5. 解析vmos-10s.xlsx并保存
6. 上传文件到gohttpserver

## 如何查看UML图

### 方法1：使用PlantUML在线编辑器
1. 访问 https://www.plantuml.com/plantuml/uml/
2. 复制 `.puml` 文件内容
3. 粘贴到编辑器中查看

### 方法2：使用VS Code插件
1. 安装 PlantUML 插件
2. 打开 `.puml` 文件
3. 使用 `Alt+D` 预览图表

### 方法3：使用IntelliJ IDEA
1. 安装 PlantUML integration 插件
2. 打开 `.puml` 文件
3. 右键选择 "Preview PlantUML Diagram"

### 方法4：使用命令行工具
```bash
# 安装PlantUML
# Windows: choco install plantuml
# Mac: brew install plantuml
# Linux: apt-get install plantuml

# 生成PNG图片
plantuml uml_class_diagram.puml
plantuml uml_sequence_diagram.puml

# 生成SVG图片
plantuml -tsvg uml_class_diagram.puml
plantuml -tsvg uml_sequence_diagram.puml
```

## 类关系说明

### 继承关系
- `TaskInfoServiceImpl`, `SpeedDataServiceImpl`, `VmosDataServiceImpl` 继承 `ServiceImpl<M, T>`
- `TaskInfoMapper`, `SpeedDataMapper`, `VmosDataMapper` 继承 `BaseMapper<T>`

### 实现关系
- Service实现类实现对应的Service接口

### 依赖关系
- `FtpFileProcessService` 依赖 `TaskInfoService`, `SpeedDataService`, `VmosDataService`
- `FtpFileProcessService` 使用 `ClientFileProcessor` 工具类
- Service实现类依赖对应的Mapper接口

### 关联关系
- `SpeedData` 和 `VmosData` 通过 `taskId` 关联到 `TaskInfo`
- `TaskInfoDTO` 转换为 `TaskInfo` 实体

## 数据库表关系

```
client_task_info (1) ----< (N) speed_data
client_task_info (1) ----< (N) vmos_data
```

- `speed_data.task_id` 关联 `client_task_info.task_id`
- `vmos_data.task_id` 关联 `client_task_info.task_id`

## 数据流向

1. **文件解析**: 压缩包 → ClientFileProcessor → DTO/Entity
2. **数据转换**: TaskInfoDTO → TaskInfo (通过TaskInfoServiceImpl)
3. **数据持久化**: Entity → Mapper → Database
4. **批量处理**: List<Entity> → Service → Database


