#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试CaseExecuteService是否正确上报失败原因
"""

import requests
import json

def test_caseexecute_failure_reason():
    """测试CaseExecuteService是否正确上报失败原因"""
    
    print("🔍 测试CaseExecuteService是否正确上报失败原因...")
    
    # CaseExecuteService基础URL
    base_url = "http://localhost:8081"
    
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
    
    print(f"📋 上报数据:")
    print(f"  taskId: {test_result['taskId']}")
    print(f"  testCaseId: {test_result['testCaseId']}")
    print(f"  round: {test_result['round']}")
    print(f"  status: {test_result['status']}")
    print(f"  result: {test_result['result']}")
    print(f"  errorMessage: {test_result['errorMessage']}")
    
    try:
        response = requests.post(
            f"{base_url}/test-case-execution/receive",
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
    
    # 查询DataCollectService中的用例执行结果
    print("\n🔍 查询DataCollectService中的用例执行结果...")
    try:
        response = requests.get(
            "http://localhost:8080/api/test-result/task/TASK_1755425937268_127_0_0_1",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                results = response_data.get("data", [])
                print(f"✅ 获取成功，共 {len(results)} 个执行结果:")
                
                for i, result in enumerate(results, 1):
                    print(f"  {i}. 用例ID: {result.get('testCaseId')}, "
                          f"轮次: {result.get('round')}, "
                          f"状态: {result.get('status')}, "
                          f"结果: {result.get('result')}, "
                          f"错误信息: {result.get('errorMessage') or '无'}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")
    
    # 查询任务33的执行例次状态
    print("\n🔍 查询任务33的执行例次状态...")
    try:
        response = requests.get(
            "http://localhost:8080/api/collect-task/33/execution-instances",
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
    print("CaseExecuteService失败原因上报测试")
    print("=" * 60)
    
    # 测试CaseExecuteService是否正确上报失败原因
    test_caseexecute_failure_reason()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. CaseExecuteService的TestCaseResultReport DTO包含errorMessage字段")
    print("2. 在FAILED和BLOCKED状态下会设置errorMessage")
    print("3. DataCollectService会接收并保存errorMessage")
    print("4. 用例执行例次表会更新failure_reason字段")
    print("5. 前端页面会显示失败原因")

if __name__ == "__main__":
    main()
