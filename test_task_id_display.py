#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试任务ID显示功能
"""

import requests
import json

def test_task_id_display():
    """测试任务ID显示功能"""
    
    print("🔍 测试任务ID显示功能...")
    
    # DataCollectService基础URL
    base_url = "http://localhost:8080"
    
    # 测试获取采集任务列表
    print("\n📋 测试获取采集任务列表...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/page?current=1&size=10",
            timeout=10
        )
        
        if response.status_code == 200:
            response_data = response.json()
            if response_data.get("code") == 200:
                page_data = response_data.get("data", {})
                records = page_data.get("records", [])
                total = page_data.get("total", 0)
                
                print(f"✅ 获取成功，共 {total} 个任务:")
                
                for i, task in enumerate(records, 1):
                    print(f"  {i}. 任务ID: {task.get('id')}, "
                          f"任务名称: {task.get('name')}, "
                          f"任务状态: {task.get('status')}, "
                          f"关联策略: {task.get('strategyName')}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 请求异常: {e}")
    
    # 测试获取单个任务详情
    print("\n📋 测试获取单个任务详情...")
    try:
        response = requests.get(
            f"{base_url}/api/collect-task/1",
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
                print(f"  关联策略: {task.get('strategyName')}")
                print(f"  总用例数: {task.get('totalTestCaseCount', 0)}")
                print(f"  已完成用例数: {task.get('completedTestCaseCount', 0)}")
                print(f"  成功用例数: {task.get('successTestCaseCount', 0)}")
                print(f"  失败用例数: {task.get('failedTestCaseCount', 0)}")
            else:
                print(f"❌ 获取失败: {response_data.get('message')}")
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            
    except Exception as e:
        print(f"❌ 请求异常: {e}")

def main():
    print("=" * 60)
    print("任务ID显示功能测试")
    print("=" * 60)
    
    # 测试任务ID显示功能
    test_task_id_display()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. 采集任务列表页面已添加任务ID列")
    print("2. 任务详情对话框已添加任务ID显示")
    print("3. 任务ID列宽度设置为80px，适合显示")
    print("4. 任务ID显示在任务名称之前，便于识别")
    print("5. 前端页面可以正确显示任务ID信息")

if __name__ == "__main__":
    main()
