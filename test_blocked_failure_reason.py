#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试BLOCKED状态的失败原因上报功能
"""

import requests
import json
from datetime import datetime

def test_blocked_failure_reason_reporting():
    """测试BLOCKED状态的失败原因上报"""
    
    print("🔍 测试BLOCKED状态的失败原因上报...")
    
    base_url = "http://localhost:8080"
    
    # 测试不同的BLOCKED场景
    blocked_scenarios = [
        {
            "name": "Python模块导入失败",
            "taskId": "TEST_BLOCKED_IMPORT_001",
            "testCaseId": 113,
            "round": 1,
            "status": "BLOCKED",
            "result": "Python模块导入失败",
            "failureReason": "Python模块导入失败: 缺少必要的依赖包，请检查Python环境和依赖安装",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "name": "文件权限问题",
            "taskId": "TEST_BLOCKED_PERMISSION_001",
            "testCaseId": 114,
            "round": 1,
            "status": "BLOCKED",
            "result": "权限不足",
            "failureReason": "权限不足: 无法访问文件或目录，请检查文件权限设置",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "name": "网络连接失败",
            "taskId": "TEST_BLOCKED_NETWORK_001",
            "testCaseId": 115,
            "round": 1,
            "status": "BLOCKED",
            "result": "网络连接失败",
            "failureReason": "网络连接失败: 无法连接到目标服务器，请检查网络配置和服务器状态",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "name": "内存不足",
            "taskId": "TEST_BLOCKED_MEMORY_001",
            "testCaseId": 116,
            "round": 1,
            "status": "BLOCKED",
            "result": "内存不足",
            "failureReason": "内存不足: 系统内存不足，无法执行用例，请检查系统资源",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "name": "Python执行器不可用",
            "taskId": "TEST_BLOCKED_PYTHON_001",
            "testCaseId": 113,
            "round": 2,
            "status": "BLOCKED",
            "result": "Python执行器不可用",
            "failureReason": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中，请检查Python安装",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        }
    ]
    
    for scenario in blocked_scenarios:
        print(f"\n📤 测试场景: {scenario['name']}")
        
        # 构建上报数据
        report_data = {
            "taskId": scenario["taskId"],
            "testCaseId": scenario["testCaseId"],
            "round": scenario["round"],
            "status": scenario["status"],
            "result": scenario["result"],
            "executionTime": 0,
            "startTime": None,
            "endTime": None,
            "executorIp": scenario["executorIp"],
            "testCaseSetId": scenario["testCaseSetId"],
            "failureReason": scenario["failureReason"]
        }
        
        try:
            # 上报BLOCKED状态
            response = requests.post(
                f"{base_url}/api/test-result/report",
                json=report_data,
                timeout=10
            )
            
            print(f"📥 响应状态码: {response.status_code}")
            
            if response.status_code == 200:
                response_data = response.json()
                if response_data.get("code") == 200:
                    print(f"✅ {scenario['name']} 上报成功")
                    print(f"   失败原因: {scenario['failureReason']}")
                else:
                    print(f"❌ {scenario['name']} 上报失败: {response_data.get('message')}")
            else:
                print(f"❌ {scenario['name']} HTTP错误: {response.status_code}")
                
        except requests.exceptions.ConnectionError:
            print(f"❌ {scenario['name']} 连接被拒绝，DataCollectService可能未启动")
        except Exception as e:
            print(f"❌ {scenario['name']} 请求异常: {e}")

def test_existing_blocked_data():
    """测试现有BLOCKED数据的失败原因显示"""
    
    print("\n🔍 查询现有BLOCKED数据的失败原因...")
    
    base_url = "http://localhost:8080"
    
    # 查询任务48的执行例次数据
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/48/execution-instances",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                instances = response_data.get("data", [])
                print(f"✅ 获取成功，共 {len(instances)} 个执行例次:")
                
                blocked_count = 0
                for i, instance in enumerate(instances, 1):
                    result = instance.get('result')
                    failure_reason = instance.get('failureReason')
                    
                    print(f"  {i}. 用例ID: {instance.get('testCaseId')}, "
                          f"轮次: {instance.get('round')}, "
                          f"执行状态: {instance.get('status')}, "
                          f"执行结果: {result or '无'}")
                    
                    if result == 'BLOCKED':
                        blocked_count += 1
                        print(f"     失败原因: {failure_reason or '无'}")
                
                print(f"\n📊 BLOCKED状态统计: {blocked_count} 个")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")

def main():
    print("=" * 60)
    print("BLOCKED状态失败原因上报测试")
    print("=" * 60)
    
    # 测试BLOCKED状态的失败原因上报
    test_blocked_failure_reason_reporting()
    
    # 测试现有BLOCKED数据的失败原因显示
    test_existing_blocked_data()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 BLOCKED状态失败原因说明:")
    print("1. BLOCKED状态表示执行被阻塞，属于执行完成状态")
    print("2. 常见BLOCKED原因及具体描述:")
    print("   - Python模块导入失败: 缺少必要的依赖包")
    print("   - 权限不足: 无法访问文件或目录")
    print("   - 文件不存在: 无法找到所需的文件或目录")
    print("   - 网络连接失败: 无法连接到目标服务器")
    print("   - 内存不足: 系统内存不足，无法执行用例")
    print("   - Python执行器不可用: 系统中未安装Python")
    print("3. 失败原因会在前端页面中显示")
    print("4. 失败原因包含具体的解决建议")

if __name__ == "__main__":
    main()
