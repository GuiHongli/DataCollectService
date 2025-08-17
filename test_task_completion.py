#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试任务完成状态检查功能
"""

import requests
import json
import time
from datetime import datetime

def test_task_completion():
    """测试任务完成状态检查功能"""
    
    print("🔍 测试任务完成状态检查功能...")
    
    # DataCollectService基础URL
    base_url = "http://localhost:8080"
    
    # 测试用例执行结果数据
    test_results = [
        {
            "taskId": "1",  # 假设这是采集任务ID 1
            "testCaseId": 1,
            "round": 1,
            "status": "SUCCESS",
            "result": "用例执行成功",
            "executionTime": 15000,
            "startTime": "2024-01-01T10:00:00",
            "endTime": "2024-01-01T10:00:15",
            "errorMessage": None,
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "taskId": "1",
            "testCaseId": 2,
            "round": 1,
            "status": "SUCCESS",
            "result": "用例执行成功",
            "executionTime": 12000,
            "startTime": "2024-01-01T10:00:15",
            "endTime": "2024-01-01T10:00:27",
            "errorMessage": None,
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "taskId": "1",
            "testCaseId": 1,
            "round": 2,
            "status": "FAILED",
            "result": "用例执行失败",
            "executionTime": 8000,
            "startTime": "2024-01-01T10:00:30",
            "endTime": "2024-01-01T10:00:38",
            "errorMessage": "网络连接超时",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        },
        {
            "taskId": "1",
            "testCaseId": 2,
            "round": 2,
            "status": "BLOCKED",
            "result": "Python执行器不可用",
            "executionTime": 0,
            "startTime": None,
            "endTime": None,
            "errorMessage": "Python执行器不可用: 系统中未安装Python或Python不在PATH环境变量中",
            "executorIp": "127.0.0.1",
            "testCaseSetId": 31
        }
    ]
    
    print(f"📋 准备上报 {len(test_results)} 个用例执行结果...")
    
    # 逐个上报用例执行结果
    for i, result in enumerate(test_results, 1):
        print(f"\n📤 上报第 {i} 个结果 - 用例ID: {result['testCaseId']}, 轮次: {result['round']}, 状态: {result['status']}")
        
        try:
            response = requests.post(
                f"{base_url}/api/test-result/report",
                json=result,
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
        
        # 等待一秒再上报下一个
        time.sleep(1)
    
    print(f"\n⏳ 等待3秒让系统处理...")
    time.sleep(3)
    
    # 查询任务状态
    print(f"\n🔍 查询任务状态...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/1",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                task = response_data.get("data")
                if task:
                    print(f"📋 任务状态: {task.get('status')}")
                    print(f"📋 总用例数: {task.get('totalTestCaseCount')}")
                    print(f"📋 已完成用例数: {task.get('completedTestCaseCount')}")
                    print(f"📋 成功用例数: {task.get('successTestCaseCount')}")
                    print(f"📋 失败用例数: {task.get('failedTestCaseCount')}")
                    print(f"📋 开始时间: {task.get('startTime')}")
                    print(f"📋 结束时间: {task.get('endTime')}")
                    
                    # 检查任务是否完成
                    if task.get('status') in ['COMPLETED', 'FAILED']:
                        print(f"✅ 任务已完成，状态: {task.get('status')}")
                    else:
                        print(f"⏳ 任务尚未完成，状态: {task.get('status')}")
                else:
                    print("❌ 未找到任务数据")
            else:
                print(f"❌ 查询失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")
    
    # 查询用例执行例次状态
    print(f"\n🔍 查询用例执行例次状态...")
    try:
        response = requests.get(
            f"{base_url}/api/test-case-execution-instance/task/1",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                instances = response_data.get("data", [])
                print(f"📋 找到 {len(instances)} 个用例执行例次:")
                
                for instance in instances:
                    print(f"  - 用例ID: {instance.get('testCaseId')}, 轮次: {instance.get('round')}, 执行状态: {instance.get('status')}, 执行结果: {instance.get('result')}")
            else:
                print(f"❌ 查询失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 查询异常: {e}")

def main():
    print("=" * 60)
    print("任务完成状态检查功能测试")
    print("=" * 60)
    
    # 测试任务完成状态检查
    test_task_completion()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. DataCollectService接收到用例执行结果后")
    print("2. 自动更新对应的用例执行例次状态")
    print("3. 检查所有用例例次是否都已完成")
    print("4. 如果全部完成，将采集任务状态置为完成")
    print("5. 更新任务的结束时间和进度统计")

if __name__ == "__main__":
    main()
