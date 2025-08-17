#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试BLOCKED状态处理
"""

import requests
import json
from datetime import datetime

def test_blocked_status_reporting():
    """测试BLOCKED状态上报"""
    
    print("🔍 测试BLOCKED状态上报...")
    
    # 测试结果上报接口
    report_url = "http://localhost:8080/api/test-result/report"
    
    # 测试BLOCKED状态上报
    blocked_report = {
        "taskId": "TEST_TASK_BLOCKED_001",
        "testCaseId": 115,
        "round": 1,
        "status": "BLOCKED",
        "result": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中",
        "executionTime": 0,
        "startTime": "2024-01-01T12:00:00",
        "endTime": "2024-01-01T12:00:00",
        "executorIp": "127.0.0.1",
        "testCaseSetId": 31,
        "errorMessage": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中"
    }
    
    try:
        print(f"📤 发送BLOCKED状态上报请求到 {report_url}")
        print(f"📋 请求数据: {json.dumps(blocked_report, indent=2, ensure_ascii=False)}")
        
        response = requests.post(report_url, json=blocked_report, timeout=10)
        
        print(f"📥 响应状态码: {response.status_code}")
        print(f"📥 响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ BLOCKED状态上报成功")
        else:
            print("❌ BLOCKED状态上报失败")
            
    except requests.exceptions.ConnectionError:
        print("❌ 连接被拒绝，DataCollectService可能未启动")
    except Exception as e:
        print(f"❌ 请求异常: {e}")

def test_different_blocked_scenarios():
    """测试不同的BLOCKED场景"""
    
    print("\n🔍 测试不同的BLOCKED场景...")
    
    scenarios = [
        {
            "name": "Python执行器不可用",
            "status": "BLOCKED",
            "result": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中",
            "errorMessage": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中"
        },
        {
            "name": "脚本文件不存在",
            "status": "BLOCKED", 
            "result": "Python脚本文件不存在: scripts/test_script.py (用例编号: test_script)",
            "errorMessage": "Python脚本文件不存在: scripts/test_script.py (用例编号: test_script)"
        },
        {
            "name": "用例编号为空",
            "status": "BLOCKED",
            "result": "用例编号为空，无法查找脚本文件",
            "errorMessage": "用例编号为空，无法查找脚本文件"
        }
    ]
    
    for i, scenario in enumerate(scenarios, 1):
        print(f"\n📋 场景 {i}: {scenario['name']}")
        print(f"   状态: {scenario['status']}")
        print(f"   结果: {scenario['result']}")
        print(f"   错误信息: {scenario['errorMessage']}")

def main():
    print("=" * 60)
    print("BLOCKED状态处理测试")
    print("=" * 60)
    
    # 测试BLOCKED状态上报
    test_blocked_status_reporting()
    
    # 测试不同的BLOCKED场景
    test_different_blocked_scenarios()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 BLOCKED状态说明:")
    print("1. BLOCKED状态表示执行被阻塞，属于执行完成状态")
    print("2. 常见BLOCKED原因:")
    print("   - Python执行器不可用")
    print("   - 脚本文件不存在")
    print("   - 用例编号为空")
    print("3. BLOCKED状态会在页面上显示错误原因")
    print("4. BLOCKED状态不会影响其他用例的执行")

if __name__ == "__main__":
    main()
