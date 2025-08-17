#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
执行机ID为7的专用调试脚本
"""

import requests
import json
import socket
from datetime import datetime

def test_executor_7_connectivity():
    """测试执行机ID为7的网络连通性"""
    print("🔍 测试执行机ID为7的网络连通性...")
    
    executor_ip = "127.0.0.1"
    port = 8081
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((executor_ip, port))
        sock.close()
        
        if result == 0:
            print(f"✅ {executor_ip}:{port} - 端口开放")
            return True
        else:
            print(f"❌ {executor_ip}:{port} - 端口关闭")
            return False
            
    except Exception as e:
        print(f"❌ {executor_ip}:{port} - 连接异常: {e}")
        return False

def test_case_execute_service_for_executor_7():
    """测试CaseExecuteService接口（执行机ID为7）"""
    print("\n🔍 测试CaseExecuteService接口（执行机ID为7）...")
    
    executor_ip = "127.0.0.1"
    url = f"http://{executor_ip}:8081/api/test-case-execution/receive"
    
    # 构建测试请求
    test_request = {
        "taskId": "TEST_TASK_EXECUTOR_7_" + str(int(datetime.now().timestamp())),
        "executorIp": executor_ip,
        "testCaseSetId": 1,
        "testCaseSetPath": "http://localhost:8000/upload/test.zip",
        "testCaseList": [
            {
                "testCaseId": 1,
                "testCaseNumber": "TC001",
                "round": 1
            }
        ],
        "resultReportUrl": "http://localhost:8080/api/test-result/report",
        "logReportUrl": "http://localhost:8000/upload/log"
    }
    
    try:
        print(f"📤 发送请求到 {url}")
        print(f"📋 请求数据: {json.dumps(test_request, indent=2, ensure_ascii=False)}")
        
        response = requests.post(url, json=test_request, timeout=10)
        
        print(f"📥 响应状态码: {response.status_code}")
        print(f"📥 响应内容: {response.text}")
        
        if response.status_code == 200:
            print(f"✅ 执行机ID为7的接口调用成功")
            return True
        else:
            print(f"❌ 执行机ID为7的接口调用失败")
            return False
            
    except requests.exceptions.ConnectionError:
        print(f"❌ 连接被拒绝，CaseExecuteService可能未启动")
        return False
    except requests.exceptions.Timeout:
        print(f"❌ 请求超时")
        return False
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        return False

def test_collect_task_creation_with_executor_7():
    """测试使用执行机ID为7的采集任务创建"""
    print("\n🔍 测试使用执行机ID为7的采集任务创建...")
    
    create_url = "http://localhost:8080/api/collect-task/create"
    
    # 使用执行机ID为7对应的逻辑环境ID
    test_request = {
        "name": "执行机7调试任务",
        "description": "专门用于调试执行机ID为7的任务",
        "collectStrategyId": 6,  # 使用采集策略ID为6，对应用例集ID为31
        "collectCount": 1,
        "regionId": 1,
        "countryId": None,
        "provinceId": None,
        "cityId": None,
        "logicEnvironmentIds": [10]  # 逻辑环境ID为10，对应执行机ID为7
    }
    
    try:
        print(f"📤 发送采集任务创建请求到 {create_url}")
        print(f"📋 请求数据: {json.dumps(test_request, indent=2, ensure_ascii=False)}")
        
        response = requests.post(create_url, json=test_request, timeout=30)
        
        print(f"📥 响应状态码: {response.status_code}")
        print(f"📥 响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ 采集任务创建成功")
            # 解析响应获取任务ID
            try:
                result = response.json()
                if result.get('code') == 200:
                    task_data = result.get('data')
                    if isinstance(task_data, dict) and 'collectTaskId' in task_data:
                        task_id = task_data.get('collectTaskId')
                    else:
                        task_id = task_data
                    print(f"📋 创建的任务ID: {task_id}")
                    
                    # 等待一段时间后检查任务状态
                    import time
                    time.sleep(5)
                    check_task_status(task_id)
                    return True
                else:
                    print(f"❌ 任务创建失败: {result.get('message')}")
                    return False
            except:
                print("❌ 无法解析响应数据")
                return False
        else:
            print("❌ 采集任务创建失败")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ 连接被拒绝，DataCollectService可能未启动")
        return False
    except Exception as e:
        print(f"❌ 请求异常: {e}")
        return False

def check_task_status(task_id):
    """检查任务状态"""
    print(f"\n🔍 检查任务状态 - 任务ID: {task_id}")
    
    status_url = f"http://localhost:8080/api/collect-task/{task_id}/progress"
    
    try:
        response = requests.get(status_url, timeout=10)
        
        print(f"📥 响应状态码: {response.status_code}")
        print(f"📥 响应内容: {response.text}")
        
        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                data = result.get('data', {})
                print(f"📊 任务状态: {data}")
            else:
                print(f"❌ 获取任务状态失败: {result.get('message')}")
        else:
            print("❌ 获取任务状态失败")
            
    except Exception as e:
        print(f"❌ 检查任务状态异常: {e}")

def check_database_data():
    """检查数据库相关数据"""
    print("\n🔍 检查数据库相关数据...")
    
    # 这里可以添加数据库查询逻辑
    print("📋 执行机ID为7的信息:")
    print("   - IP地址: 127.0.0.1")
    print("   - 状态: 1 (在线)")
    print("   - 逻辑环境ID: 10")
    print("   - 逻辑环境名称: 逻辑环境--4G标准网络-new")

def main():
    print("=" * 60)
    print("执行机ID为7专用调试工具")
    print("=" * 60)
    
    # 1. 检查数据库数据
    check_database_data()
    
    # 2. 测试网络连通性
    connectivity_ok = test_executor_7_connectivity()
    
    # 3. 测试CaseExecuteService接口
    if connectivity_ok:
        case_execute_ok = test_case_execute_service_for_executor_7()
    else:
        case_execute_ok = False
    
    # 4. 测试采集任务创建
    if case_execute_ok:
        task_creation_ok = test_collect_task_creation_with_executor_7()
    else:
        task_creation_ok = False
    
    print("\n" + "=" * 60)
    print("调试结果总结")
    print("=" * 60)
    print(f"网络连通性: {'✅ 正常' if connectivity_ok else '❌ 异常'}")
    print(f"CaseExecuteService接口: {'✅ 正常' if case_execute_ok else '❌ 异常'}")
    print(f"采集任务创建: {'✅ 正常' if task_creation_ok else '❌ 异常'}")
    
    if not task_creation_ok:
        print("\n📋 可能的问题:")
        print("1. 采集策略ID为1不存在或无效")
        print("2. 用例集数据不完整")
        print("3. 逻辑环境配置问题")
        print("4. 数据库连接问题")

if __name__ == "__main__":
    main()
