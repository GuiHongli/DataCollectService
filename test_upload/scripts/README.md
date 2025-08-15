# 测试脚本说明

本目录包含两个Python unittest测试脚本，用于执行网络相关的自动化测试。

## 脚本列表

### 1. test_network_connection.py
**对应测试用例：TC001 - 4G网络连接测试**

**功能：**
- 网络连通性测试
- HTTP请求测试
- 网络延迟测试
- 网络失败模拟测试
- 带宽和并发连接测试

**运行方法：**
```bash
python3 test_network_connection.py
```

**测试内容：**
- DNS解析测试
- TCP连接测试
- HTTP请求测试
- 网络延迟测量
- 网络失败场景模拟

### 2. test_wifi_stability.py
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
python3 test_wifi_stability.py
```

**测试内容：**
- WiFi信息获取（支持macOS、Linux、Windows）
- 数据传输模拟
- Ping稳定性测试
- 网络切换模拟
- 信号强度监控
- 连接中断恢复测试

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
   chmod +x test_network_connection.py
   chmod +x test_wifi_stability.py
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
