#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试逻辑环境管理页面中逻辑组网列的数据是否来自网络类型数据表
"""

import requests
import json
import sys

# 配置
BASE_URL = "http://localhost:8080"
HEADERS = {
    "Content-Type": "application/json"
}

def test_network_type_data_in_logic_environment():
    """测试逻辑环境中的网络数据是否来自网络类型数据表"""
    print("=== 测试逻辑环境中的网络数据是否来自网络类型数据表 ===")
    
    # 1. 创建测试网络类型
    print("1. 创建测试网络类型...")
    test_networks = [
        {
            "name": "逻辑环境测试网络1",
            "description": "用于逻辑环境测试的网络类型1",
            "status": 1
        },
        {
            "name": "逻辑环境测试网络2",
            "description": "用于逻辑环境测试的网络类型2", 
            "status": 1
        }
    ]
    
    created_network_ids = []
    for network_data in test_networks:
        try:
            response = requests.post(f"{BASE_URL}/network-type", json=network_data, headers=HEADERS)
            if response.status_code == 200:
                network = response.json()["data"]
                created_network_ids.append(network["id"])
                print(f"✓ 创建网络类型成功: {network['name']} (ID: {network['id']})")
            else:
                print(f"✗ 创建网络类型失败: {response.text}")
                return False, []
        except Exception as e:
            print(f"✗ 创建网络类型失败: {e}")
            return False, []
    
    # 2. 创建测试逻辑环境
    print("2. 创建测试逻辑环境...")
    logic_environment_data = {
        "name": "网络数据测试环境",
        "description": "用于测试网络数据关联的逻辑环境",
        "status": 1
    }
    
    try:
        response = requests.post(f"{BASE_URL}/logic-environment", json=logic_environment_data, headers=HEADERS)
        if response.status_code == 200:
            logic_env = response.json()["data"]
            logic_env_id = logic_env["id"]
            print(f"✓ 创建逻辑环境成功，ID: {logic_env_id}")
        else:
            print(f"✗ 创建逻辑环境失败: {response.text}")
            return False, created_network_ids
    except Exception as e:
        print(f"✗ 创建逻辑环境失败: {e}")
        return False, created_network_ids
    
    # 3. 将网络类型关联到逻辑环境
    print("3. 将网络类型关联到逻辑环境...")
    try:
        response = requests.post(f"{BASE_URL}/logic-environment/{logic_env_id}/network", 
                               json=created_network_ids, headers=HEADERS)
        if response.status_code == 200:
            print("✓ 网络类型关联到逻辑环境成功")
        else:
            print(f"✗ 网络类型关联失败: {response.text}")
            return False, created_network_ids
    except Exception as e:
        print(f"✗ 网络类型关联失败: {e}")
        return False, created_network_ids
    
    # 4. 获取逻辑环境的网络信息
    print("4. 获取逻辑环境的网络信息...")
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/{logic_env_id}/network", headers=HEADERS)
        if response.status_code == 200:
            network_list = response.json()["data"]
            print(f"✓ 获取逻辑环境网络信息成功，共 {len(network_list)} 个网络")
            
            # 验证网络信息是否来自网络类型数据表
            for network in network_list:
                print(f"  - 网络ID: {network['id']}, 名称: {network['name']}, 描述: {network['description']}")
                
                # 验证这个网络ID是否在我们创建的网络类型中
                if network['id'] in created_network_ids:
                    print(f"    ✓ 确认网络 {network['name']} 来自网络类型数据表")
                else:
                    print(f"    ✗ 网络 {network['name']} 不在网络类型数据表中")
                    return False, created_network_ids
        else:
            print(f"✗ 获取逻辑环境网络信息失败: {response.text}")
            return False, created_network_ids
    except Exception as e:
        print(f"✗ 获取逻辑环境网络信息失败: {e}")
        return False, created_network_ids
    
    return logic_env_id, created_network_ids

def test_network_data_consistency():
    """测试网络数据的一致性"""
    print("\n=== 测试网络数据的一致性 ===")
    
    # 1. 通过网络类型接口修改数据
    print("1. 通过网络类型接口修改数据...")
    
    # 先获取一个网络类型
    try:
        response = requests.get(f"{BASE_URL}/network-type/list", headers=HEADERS)
        if response.status_code == 200:
            network_types = response.json()["data"]
            if network_types:
                test_network = network_types[0]
                network_id = test_network["id"]
                
                # 修改网络类型
                update_data = {
                    "name": f"{test_network['name']}（已修改）",
                    "description": f"{test_network['description']}（已修改）",
                    "status": 1
                }
                
                response = requests.put(f"{BASE_URL}/network-type/{network_id}", json=update_data, headers=HEADERS)
                if response.status_code == 200:
                    print(f"✓ 网络类型修改成功: {update_data['name']}")
                else:
                    print(f"✗ 网络类型修改失败: {response.text}")
                    return False
            else:
                print("✗ 没有可用的网络类型进行测试")
                return False
        else:
            print(f"✗ 获取网络类型列表失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 网络类型操作失败: {e}")
        return False
    
    # 2. 验证逻辑环境中的网络信息是否同步更新
    print("2. 验证逻辑环境中的网络信息是否同步更新...")
    
    # 获取所有逻辑环境
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/page", headers=HEADERS)
        if response.status_code == 200:
            logic_environments = response.json()["data"]["records"]
            
            for logic_env in logic_environments:
                # 获取每个逻辑环境的网络信息
                response = requests.get(f"{BASE_URL}/logic-environment/{logic_env['id']}/network", headers=HEADERS)
                if response.status_code == 200:
                    network_list = response.json()["data"]
                    
                    for network in network_list:
                        if network['id'] == network_id:
                            if "已修改" in network['name']:
                                print(f"✓ 逻辑环境 {logic_env['name']} 中的网络信息已同步更新")
                                return True
                            else:
                                print(f"✗ 逻辑环境 {logic_env['name']} 中的网络信息未同步更新")
                                return False
        else:
            print(f"✗ 获取逻辑环境列表失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 逻辑环境操作失败: {e}")
        return False

def cleanup_test_data(logic_env_id, network_ids):
    """清理测试数据"""
    print("\n=== 清理测试数据 ===")
    
    # 删除逻辑环境
    if logic_env_id:
        try:
            response = requests.delete(f"{BASE_URL}/logic-environment/{logic_env_id}", headers=HEADERS)
            if response.status_code == 200:
                print(f"✓ 删除逻辑环境成功: {logic_env_id}")
            else:
                print(f"✗ 删除逻辑环境失败: {logic_env_id}")
        except Exception as e:
            print(f"✗ 删除逻辑环境失败: {logic_env_id}, 错误: {e}")
    
    # 删除网络类型
    for network_id in network_ids:
        try:
            response = requests.delete(f"{BASE_URL}/network-type/{network_id}", headers=HEADERS)
            if response.status_code == 200:
                print(f"✓ 删除网络类型成功: {network_id}")
            else:
                print(f"✗ 删除网络类型失败: {network_id}")
        except Exception as e:
            print(f"✗ 删除网络类型失败: {network_id}, 错误: {e}")

def main():
    """主测试函数"""
    print("开始测试逻辑环境管理页面中逻辑组网列的数据是否来自网络类型数据表...")
    
    try:
        # 测试网络类型数据在逻辑环境中的关联
        result = test_network_type_data_in_logic_environment()
        if result:
            logic_env_id, network_ids = result
        else:
            print("✗ 网络类型数据关联测试失败")
            return
        
        # 测试数据一致性
        consistency_ok = test_network_data_consistency()
        
        # 总结
        print("\n=== 测试总结 ===")
        if result and consistency_ok:
            print("✓ 所有测试通过！")
            print("✓ 逻辑环境管理页面中逻辑组网列的数据确实来自网络类型数据表。")
            print("✓ 数据修改会实时同步到逻辑环境中的网络信息。")
        else:
            print("✗ 部分测试失败，请检查实现。")
            
    except Exception as e:
        print(f"测试过程中发生错误: {e}")
        sys.exit(1)
    
    finally:
        # 清理测试数据
        if 'logic_env_id' in locals() and 'network_ids' in locals():
            cleanup_test_data(logic_env_id, network_ids)

if __name__ == "__main__":
    main()

