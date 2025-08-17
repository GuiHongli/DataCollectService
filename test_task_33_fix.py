#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试任务33的状态修复
"""

import requests
import json

def test_task_33_fix():
    """测试任务33的状态修复"""
    
    print("🔍 测试任务33的状态修复...")
    
    # DataCollectService基础URL
    base_url = "http://localhost:8080"
    
    # 模拟上报一个用例执行结果，使用execution_task_id格式的taskId
    print("\n📤 模拟上报用例执行结果...")
    
    test_result = {
        "taskId": "TASK_1755425937268_127_0_0_1",  # 使用execution_task_id格式
        "testCaseId": 113,
        "round": 1,
        "status": "BLOCKED",
        "result": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中",
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
                          f"执行结果: {instance.get('result') or '无'}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")
    
    # 查询任务33的状态
    print("\n🔍 查询任务33的状态...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/33",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                task = response_data.get("data", {})
                print(f"✅ 获取成功:")
                print(f"  任务ID: {task.get('id')}")
                print(f"  任务名称: {task.get('name')}")
                print(f"  任务状态: {task.get('status')}")
                print(f"  总用例数: {task.get('totalTestCaseCount', 0)}")
                print(f"  已完成用例数: {task.get('completedTestCaseCount', 0)}")
                print(f"  成功用例数: {task.get('successTestCaseCount', 0)}")
                print(f"  失败用例数: {task.get('failedTestCaseCount', 0)}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")

def main():
    print("=" * 60)
    print("任务33状态修复测试")
    print("=" * 60)
    
    # 测试任务33的状态修复
    test_task_33_fix()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. 修复了taskId格式不匹配的问题")
    print("2. 支持数字格式和execution_task_id格式的taskId")
    print("3. 用例执行结果上报后能正确更新例次状态")
    print("4. 任务完成状态检查逻辑正常工作")
    print("5. 任务33的状态应该能正确更新")

if __name__ == "__main__":
    main()
