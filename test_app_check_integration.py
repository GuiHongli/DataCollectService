#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试应用检查集成功能
"""

import requests
import json

def test_external_api():
    """测试外部接口调用"""
    url = "http://localhost:8080/api/external/check-app-is-new"
    
    # 测试数据
    test_data = [
        {
            "app_name": "微信",
            "is_ios": False
        }
    ]
    
    headers = {
        "Content-Type": "application/json"
    }
    
    try:
        print("测试外部接口调用...")
        print(f"请求URL: {url}")
        print(f"请求数据: {json.dumps(test_data, ensure_ascii=False, indent=2)}")
        
        response = requests.post(url, json=test_data, headers=headers)
        
        print(f"响应状态码: {response.status_code}")
        print(f"响应内容: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
        
        if response.status_code == 200:
            print("✅ 外部接口调用成功")
        else:
            print("❌ 外部接口调用失败")
            
    except Exception as e:
        print(f"❌ 测试异常: {e}")

def test_collect_task_creation():
    """测试采集任务创建（需要先启动服务）"""
    print("\n注意：此测试需要先启动DataCollectService服务")
    print("并且需要配置正确的外部接口地址")

if __name__ == "__main__":
    print("=== 应用检查集成功能测试 ===")
    test_external_api()
    test_collect_task_creation()
