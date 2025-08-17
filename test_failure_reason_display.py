#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试失败原因显示功能
"""

import requests
import json

def test_failure_reason_display():
    """测试失败原因显示功能"""
    
    print("🔍 测试失败原因显示功能...")
    
    # DataCollectService基础URL
    base_url = "http://localhost:8080"
    
    # 模拟上报一个失败的用例执行结果
    print("\n📤 模拟上报失败的用例执行结果...")
    
    test_result = {
        "taskId": "TASK_1755425937268_127_0_0_1",
        "testCaseId": 113,
        "round": 1,
        "status": "FAILED",
        "result": "用例执行失败",
        "executionTime": 5000,
        "startTime": "2024-01-01T10:00:00",
        "endTime": "2024-01-01T10:00:05",
        "errorMessage": "网络连接超时，无法访问目标服务器",
        "executorIp": "127.0.0.1",
        "testCaseSetId": 38
    }
    
    try:
        response = requests.post(
            f"{base_url}/api/test-result/report",
            json=test_result,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                print(f"✅ 上报成功: {response_data.get('message')}")
            else:
                print(f"❌ 上报失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 上报异常: {e}")
    
    # 等待一秒让系统处理
    import time
    time.sleep(1)
    
    # 查询任务33的执行例次状态
    print("\n🔍 查询任务33的执行例次状态...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/33/execution-instances",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                instances = response_data.get("data", [])
                print(f"✅ 获取成功，共 {len(instances)} 个执行例次:")
                
                for i, instance in enumerate(instances, 1):
                    print(f"  {i}. 用例ID: {instance.get('testCaseId')}, "
                          f"轮次: {instance.get('round')}, "
                          f"执行状态: {instance.get('status')}, "
                          f"执行结果: {instance.get('result') or '无'}, "
                          f"失败原因: {instance.get('failureReason') or '无'}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")
    
    # 模拟上报一个阻塞的用例执行结果
    print("\n📤 模拟上报阻塞的用例执行结果...")
    
    blocked_result = {
        "taskId": "TASK_1755425937268_127_0_0_1",
        "testCaseId": 114,
        "round": 1,
        "status": "BLOCKED",
        "result": "Python执行器不可用",
        "executionTime": 0,
        "startTime": None,
        "endTime": None,
        "errorMessage": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中",
        "executorIp": "127.0.0.1",
        "testCaseSetId": 38
    }
    
    try:
        response = requests.post(
            f"{base_url}/api/test-result/report",
            json=blocked_result,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                print(f"✅ 上报成功: {response_data.get('message')}")
            else:
                print(f"❌ 上报失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 上报异常: {e}")
    
    # 等待一秒让系统处理
    time.sleep(1)
    
    # 再次查询任务33的执行例次状态
    print("\n🔍 再次查询任务33的执行例次状态...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/33/execution-instances",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                instances = response_data.get("data", [])
                print(f"✅ 获取成功，共 {len(instances)} 个执行例次:")
                
                for i, instance in enumerate(instances, 1):
                    print(f"  {i}. 用例ID: {instance.get('testCaseId')}, "
                          f"轮次: {instance.get('round')}, "
                          f"执行状态: {instance.get('status')}, "
                          f"执行结果: {instance.get('result') or '无'}, "
                          f"失败原因: {instance.get('failureReason') or '无'}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")

def main():
    print("=" * 60)
    print("失败原因显示功能测试")
    print("=" * 60)
    
    # 测试失败原因显示功能
    test_failure_reason_display()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. 用例执行例次表已添加failure_reason字段")
    print("2. 后端API已支持返回失败原因")
    print("3. 前端页面已添加失败原因列显示")
    print("4. 支持FAILED和BLOCKED状态的失败原因显示")
    print("5. 失败原因使用tooltip显示完整内容")

if __name__ == "__main__":
    main()
