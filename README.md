# 数据采集任务配置管理系统

## 项目简介

这是一个完整的数据采集任务配置管理系统，包含前端Vue项目和后端SpringBoot服务。系统提供地域管理、网络类型管理、执行机管理、UE管理、逻辑环境管理、用例集管理、采集策略管理和采集任务管理等完整功能。

## 技术栈

### 后端技术栈
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- MySQL 5.7
- Java 8
- Maven

### 前端技术栈
- Vue 3.3.4
- Vue Router 4.2.4
- Pinia 2.1.6
- Element Plus 2.3.8
- Vite 4.4.5
- Axios 1.4.0

## 功能模块

### 1. 地域管理模块
- 片区、国家、省份、城市子模块
- 各模块之间有联动关系
- 示例：中国China-中国China-辽宁省LN-沈阳SY

### 2. 网络类型管理模块
- 网络类型分为四种：normal、弱网、拥塞、弱网+拥塞

### 3. 执行机管理模块
- 基础信息：执行机IP、执行机所属地域（片区、国家、省份、城市）、描述

### 4. UE管理模块
- UE基础信息：ueid、用途、网络类型信息

### 5. 逻辑环境管理模块
- 对执行机和UE的使用组合
- 逻辑环境基础信息：逻辑环境名称
- 一个逻辑环境包含一个执行机、多个UE

### 6. 用例集管理模块
- 用例集关键信息：用例集名称、用例集版本、用例集zip、创建人、创建时间
- 用例集名称和版本从上传的用例集zip命名中提取
- zip命名规则：用例集名称_用例集版本.zip
- 示例：短视频采集_v0.1.zip

### 7. 采集策略管理模块
- 基本信息：策略名称、采集次数、逻辑组网（页面选择）
- 基于采集策略，可创建采集任务

### 8. 采集任务管理模块
- 采集任务的增删改查
- 支持任务状态控制（启动、停止、暂停）

## 项目结构

```
├── DataCollectService/          # 后端SpringBoot项目
│   ├── src/main/java/com/datacollect/
│   │   ├── common/              # 通用类
│   │   ├── config/              # 配置类
│   │   ├── controller/          # 控制器层
│   │   ├── entity/              # 实体类
│   │   ├── mapper/              # 数据访问层
│   │   └── service/             # 服务层
│   ├── src/main/resources/
│   │   ├── db/                  # 数据库脚本
│   │   └── application.yml      # 配置文件
│   └── pom.xml                  # Maven配置
│
└── DataCollectFront/            # 前端Vue项目
    ├── src/
    │   ├── api/                 # API接口
    │   ├── components/          # 组件
    │   ├── layout/              # 布局组件
    │   ├── router/              # 路由配置
    │   ├── utils/               # 工具类
    │   ├── views/               # 页面
    │   └── style.css            # 全局样式
    ├── index.html               # HTML入口
    ├── package.json             # 依赖配置
    └── vite.config.js           # Vite配置
```

## 快速开始

### 环境要求
- JDK 8+
- MySQL 5.7+
- Node.js 16+
- Maven 3.6+

### 后端启动
1. 创建数据库：
```sql
CREATE DATABASE data_collect DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```sql
-- 执行 DataCollectService/src/main/resources/db/init.sql 文件
```

3. 修改数据库连接配置：
```yaml
# DataCollectService/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/data_collect?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
```

4. 启动后端服务：
```bash
cd DataCollectService
mvn spring-boot:run
```

### 前端启动
1. 安装依赖：
```bash
cd DataCollectFront
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

3. 访问地址：http://localhost:3000

## API接口

### 地域管理
- `POST /api/region` - 创建地域
- `PUT /api/region/{id}` - 更新地域
- `DELETE /api/region/{id}` - 删除地域
- `GET /api/region/{id}` - 获取地域详情
- `GET /api/region/page` - 分页查询地域
- `GET /api/region/list` - 获取地域列表
- `GET /api/region/level/{level}` - 根据层级获取地域
- `GET /api/region/parent/{parentId}` - 根据父级ID获取地域
- `GET /api/region/tree` - 获取地域树形结构

### 网络类型管理
- `POST /api/network-type` - 创建网络类型
- `PUT /api/network-type/{id}` - 更新网络类型
- `DELETE /api/network-type/{id}` - 删除网络类型
- `GET /api/network-type/{id}` - 获取网络类型详情
- `GET /api/network-type/page` - 分页查询网络类型
- `GET /api/network-type/list` - 获取网络类型列表

### 执行机管理
- `POST /api/executor` - 创建执行机
- `PUT /api/executor/{id}` - 更新执行机
- `DELETE /api/executor/{id}` - 删除执行机
- `GET /api/executor/{id}` - 获取执行机详情
- `GET /api/executor/page` - 分页查询执行机
- `GET /api/executor/list` - 获取执行机列表
- `GET /api/executor/region/{regionId}` - 根据地域ID获取执行机

### UE管理
- `POST /api/ue` - 创建UE
- `PUT /api/ue/{id}` - 更新UE
- `DELETE /api/ue/{id}` - 删除UE
- `GET /api/ue/{id}` - 获取UE详情
- `GET /api/ue/page` - 分页查询UE
- `GET /api/ue/list` - 获取UE列表
- `GET /api/ue/network-type/{networkTypeId}` - 根据网络类型ID获取UE

### 逻辑环境管理
- `POST /api/logic-environment` - 创建逻辑环境
- `PUT /api/logic-environment/{id}` - 更新逻辑环境
- `DELETE /api/logic-environment/{id}` - 删除逻辑环境
- `GET /api/logic-environment/{id}` - 获取逻辑环境详情
- `GET /api/logic-environment/page` - 分页查询逻辑环境
- `GET /api/logic-environment/list` - 获取逻辑环境列表
- `GET /api/logic-environment/executor/{executorId}` - 根据执行机ID获取逻辑环境
- `POST /api/logic-environment/{logicEnvironmentId}/ue` - 添加UE到逻辑环境
- `DELETE /api/logic-environment/{logicEnvironmentId}/ue/{ueId}` - 从逻辑环境移除UE
- `GET /api/logic-environment/{logicEnvironmentId}/ue` - 获取逻辑环境的UE列表

### 用例集管理
- `POST /api/test-case-set` - 创建用例集
- `POST /api/test-case-set/upload` - 上传用例集文件
- `PUT /api/test-case-set/{id}` - 更新用例集
- `DELETE /api/test-case-set/{id}` - 删除用例集
- `GET /api/test-case-set/{id}` - 获取用例集详情
- `GET /api/test-case-set/page` - 分页查询用例集
- `GET /api/test-case-set/list` - 获取用例集列表
- `GET /api/test-case-set/name/{name}` - 根据名称获取用例集

### 采集策略管理
- `POST /api/collect-strategy` - 创建采集策略
- `PUT /api/collect-strategy/{id}` - 更新采集策略
- `DELETE /api/collect-strategy/{id}` - 删除采集策略
- `GET /api/collect-strategy/{id}` - 获取采集策略详情
- `GET /api/collect-strategy/page` - 分页查询采集策略
- `GET /api/collect-strategy/list` - 获取采集策略列表
- `GET /api/collect-strategy/logic-environment/{logicEnvironmentId}` - 根据逻辑环境ID获取采集策略

### 采集任务管理
- `POST /api/collect-task` - 创建采集任务
- `PUT /api/collect-task/{id}` - 更新采集任务
- `DELETE /api/collect-task/{id}` - 删除采集任务
- `GET /api/collect-task/{id}` - 获取采集任务详情
- `GET /api/collect-task/page` - 分页查询采集任务
- `GET /api/collect-task/list` - 获取采集任务列表
- `GET /api/collect-task/strategy/{strategyId}` - 根据策略ID获取采集任务
- `POST /api/collect-task/{id}/start` - 启动任务
- `POST /api/collect-task/{id}/stop` - 停止任务
- `POST /api/collect-task/{id}/pause` - 暂停任务
- `GET /api/collect-task/status/{status}` - 根据状态获取采集任务

## 开发说明

### 代码规范
- 后端使用Lombok简化代码
- 遵循RESTful API设计规范
- 使用MyBatis Plus简化数据库操作
- 统一异常处理和返回结果格式
- 前端使用Vue 3 Composition API
- 遵循Element Plus设计规范
- 所有管理模块均记录创建人、修改人、创建时间、修改时间等变更内容

### 数据库设计
- 使用逻辑删除，避免数据丢失
- 添加创建时间和更新时间字段
- 合理设置索引提高查询性能
- 使用外键关联保证数据一致性

### 特殊功能
- 文件上传：支持用例集zip文件上传，自动解析文件名获取名称和版本
- 地域联动：支持片区、国家、省份、城市的层级查询
- 逻辑环境：支持执行机和UE的多对多关联管理
- 任务控制：支持采集任务的启动、停止、暂停操作

## 许可证

MIT License

# 测试脚本说明

本目录包含两个Python unittest测试脚本，用于执行网络相关的自动化测试。

## 脚本列表

### 1. 4G_Network_Connection_Test.py
**对应测试用例：TC001 - 4G网络连接测试**

**功能：**
- 网络连通性测试
- HTTP请求测试
- 网络延迟测试
- 网络失败模拟测试
- 带宽和并发连接测试

**运行方法：**
```bash
python3 4G_Network_Connection_Test.py
```

**测试内容：**
- DNS解析测试
- TCP连接测试
- HTTP请求测试
- 网络延迟测量
- 网络失败场景模拟

### 2. WiFi_Connection_Stability_Test.py
**对应测试用例：TC003 - WiFi连接稳定性测试**

**功能：**
- WiFi连接状态检测
- 数据传输测试
- 连接稳定性监控
- 网络切换测试
- 信号强度监控
- 连接恢复测试

**运行方法：**
```bash
python3 WiFi_Connection_Stability_Test.py
```

**测试内容：**
- WiFi信息获取（支持macOS、Linux、Windows）
- 数据传输模拟
- Ping稳定性测试
- 网络切换模拟
- 信号强度监控
- 连接中断恢复测试

## 脚本命名规则

脚本文件名与Excel中的用例名称对应关系：
- `4G_Network_Connection_Test.py` ↔ `4G网络连接测试`
- `WiFi_Connection_Stability_Test.py` ↔ `WiFi连接稳定性测试`

## 依赖要求

### 基础依赖
- Python 3.6+
- unittest (Python标准库)
- time (Python标准库)
- socket (Python标准库)
- subprocess (Python标准库)
- platform (Python标准库)
- json (Python标准库)

### 可选依赖
- requests (用于HTTP请求测试)
  ```bash
  pip install requests
  ```

## 运行环境

### 支持的操作系统
- macOS
- Linux
- Windows

### 权限要求
- 网络访问权限
- 系统命令执行权限（ping、iwconfig等）

## 测试报告

每个脚本运行完成后会生成详细的测试报告，包括：
- 测试执行结果
- 成功/失败统计
- 错误详情
- 性能指标

## 注意事项

1. **网络依赖**：测试需要网络连接，请确保网络环境正常
2. **权限要求**：某些测试需要系统权限，可能需要sudo运行
3. **超时设置**：测试包含超时机制，避免长时间等待
4. **模拟测试**：部分测试使用mock模拟，不会影响实际网络

## 自定义配置

可以通过修改脚本中的以下参数来自定义测试：

```python
# 测试目标主机
self.test_host = "www.baidu.com"

# 测试超时时间
self.timeout = 10

# 测试持续时间
self.test_duration = 30

# Ping测试次数
self.ping_count = 10
```

## 故障排除

### 常见问题

1. **权限错误**
   ```bash
   chmod +x 4G_Network_Connection_Test.py
   chmod +x WiFi_Connection_Stability_Test.py
   ```

2. **网络连接失败**
   - 检查网络连接
   - 确认防火墙设置
   - 验证DNS配置

3. **依赖缺失**
   ```bash
   pip install requests
   ```

4. **系统命令不可用**
   - macOS: 确保airport命令可用
   - Linux: 安装wireless-tools
   - Windows: 确保netsh命令可用

## 扩展开发

如需添加新的测试用例，可以参考现有脚本的结构：

1. 继承unittest.TestCase
2. 实现setUp和tearDown方法
3. 编写具体的测试方法
4. 添加适当的断言和错误处理
5. 集成到测试套件中

## 文件结构

```
scripts/
├── README.md                           # 说明文档
├── 4G_Network_Connection_Test.py       # 4G网络连接测试
└── WiFi_Connection_Stability_Test.py   # WiFi连接稳定性测试
```


