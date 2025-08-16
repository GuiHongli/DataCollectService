# 采集任务详情功能实现

## 功能概述

为DataCollectFront的采集任务管理页面增加了详情功能，支持查看任务执行进度、执行结果统计和用例例次执行信息。

## 功能特性

### 1. 任务名称点击查看详情
- 任务名称显示为可点击的链接样式
- 点击后打开详情对话框，显示完整的任务信息

### 2. 详情对话框内容
- **基本信息**: 任务名称、状态、关联策略、执行计划、创建时间等
- **执行进度**: 总用例数、成功用例数、失败用例数、执行中用例数，以及进度条
- **执行结果统计**: 成功、失败、执行中的统计卡片
- **用例例次执行信息**: 详细的执行例次列表表格

## 技术实现

### 1. 前端实现 (DataCollectFront)

#### 文件修改
- `DataCollectFront/src/views/collect-task/index.vue`

#### 主要功能
1. **任务名称点击事件**
   ```vue
   <el-button 
     type="text" 
     @click="handleViewDetail(scope.row)"
     style="color: #409eff; text-decoration: none;"
   >
     {{ scope.row.name }}
   </el-button>
   ```

2. **详情对话框结构**
   - 基本信息卡片
   - 执行进度卡片（带刷新按钮）
   - 执行结果统计卡片
   - 用例例次执行信息表格（带刷新按钮）

3. **数据获取方法**
   ```javascript
   const loadTaskProgress = async (taskId) => {
     const res = await request({
       url: `/collect-task/${taskId}/progress`,
       method: 'get',
     })
     taskProgress.value = res.data
   }

   const loadExecutionInstances = async (taskId) => {
     const res = await request({
       url: `/collect-task/${taskId}/execution-instances`,
       method: 'get',
     })
     executionInstances.value = res.data
   }
   ```

4. **进度计算**
   ```javascript
   const getProgressPercentage = () => {
     const total = taskProgress.value.totalCount || 0
     const completed = (taskProgress.value.successCount || 0) + (taskProgress.value.failedCount || 0)
     return total > 0 ? Math.round((completed / total) * 100) : 0
   }
   ```

### 2. 后端实现 (DataCollectService)

#### 文件修改
- `DataCollectService/src/main/java/com/datacollect/controller/CollectTaskController.java`

#### 新增API接口

1. **获取任务执行进度**
   ```java
   @GetMapping("/{id}/progress")
   public Result<Map<String, Object>> getTaskProgress(@PathVariable @NotNull Long id)
   ```
   - 返回总用例数、成功用例数、失败用例数、执行中用例数

2. **获取任务执行例次列表**
   ```java
   @GetMapping("/{id}/execution-instances")
   public Result<List<Map<String, Object>>> getExecutionInstances(@PathVariable @NotNull Long id)
   ```
   - 返回执行例次的详细信息，包括用例信息、逻辑环境信息等

#### 数据关联查询
- 通过`TestCaseExecutionInstanceService.getByCollectTaskId()`获取执行例次
- 通过`TestCaseService.getById()`获取用例详细信息
- 通过`LogicEnvironmentService.getById()`获取逻辑环境信息

## 界面设计

### 1. 视觉设计
- **卡片式布局**: 使用Element Plus的Card组件，信息分类清晰
- **进度可视化**: 使用进度条和数字统计展示执行进度
- **状态标识**: 使用不同颜色的Tag组件标识不同状态
- **响应式设计**: 支持移动端适配

### 2. 交互设计
- **实时刷新**: 进度和例次信息支持手动刷新
- **状态反馈**: 加载状态、错误状态都有相应的视觉反馈
- **操作便捷**: 一键查看详情，操作简单直观

### 3. 数据展示
- **执行进度**: 数字统计 + 进度条
- **结果统计**: 图标 + 数字 + 标签的卡片式展示
- **例次列表**: 表格形式，支持排序和筛选

## 数据结构

### 1. 任务进度数据
```json
{
  "totalCount": 10,
  "successCount": 5,
  "failedCount": 2,
  "runningCount": 3
}
```

### 2. 执行例次数据
```json
[
  {
    "id": 1,
    "testCaseId": 41,
    "testCaseNumber": "TC001",
    "testCaseName": "4G网络连接测试",
    "round": 1,
    "logicEnvironmentId": 1,
    "logicEnvironmentName": "北京4G环境",
    "executorIp": "192.168.1.100",
    "status": "COMPLETED",
    "executionTaskId": "TASK_1234567890",
    "createTime": "2025-08-16T10:00:00",
    "updateTime": "2025-08-16T10:05:00"
  }
]
```

## 使用场景

1. **任务监控**: 实时查看任务执行进度和状态
2. **问题排查**: 通过执行例次信息定位问题
3. **结果分析**: 统计成功率和失败原因
4. **执行追踪**: 追踪每个用例的执行情况

## 扩展功能

### 1. 可扩展的功能
- **实时更新**: 支持WebSocket实时推送进度更新
- **结果详情**: 点击查看具体的执行结果和日志
- **导出功能**: 支持导出执行报告
- **筛选功能**: 支持按状态、时间等条件筛选例次

### 2. 性能优化
- **分页加载**: 大量例次数据支持分页显示
- **缓存机制**: 缓存任务进度数据，减少API调用
- **懒加载**: 详情对话框按需加载数据

## 注意事项

1. **数据一致性**: 确保前端显示的数据与后端数据库一致
2. **错误处理**: 网络异常、数据异常等情况的处理
3. **权限控制**: 确保用户只能查看有权限的任务详情
4. **性能考虑**: 大量数据时的加载性能优化

## 测试建议

1. **功能测试**: 测试各种状态下的数据显示
2. **性能测试**: 测试大量数据时的加载性能
3. **兼容性测试**: 测试不同浏览器的兼容性
4. **用户体验测试**: 测试界面的易用性和直观性
