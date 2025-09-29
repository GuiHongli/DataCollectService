#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试逻辑环境管理页面的逻辑组网数据与网络类型管理页面的数据统一性
"""

import requests
import json
import sys

# 配置
BASE_URL = "http://localhost:8080"
HEADERS = {
    "Content-Type": "application/json"
}

def test_data_source_unification():
    """测试数据源统一性"""
    print("=== 测试数据源统一性 ===")
    
    # 1. 创建测试网络类型
    print("1. 创建测试网络类型...")
    test_network = {
        "name": "数据统一测试网络",
        "description": "用于测试数据统一性的网络类型",
        "status": 1
    }
    
    try:
        response = requests.post(f"{BASE_URL}/network-type", json=test_network, headers=HEADERS)
        if response.status_code == 200:
            network = response.json()["data"]
            network_id = network["id"]
            print(f"✓ 网络类型创建成功，ID: {network_id}")
        else:
            print(f"✗ 网络类型创建失败: {response.text}")
            return False, None
    except Exception as e:
        print(f"✗ 网络类型创建失败: {e}")
        return False, None
    
    # 2. 通过网络类型管理接口查询
    print("2. 通过网络类型管理接口查询...")
    try:
        response = requests.get(f"{BASE_URL}/network-type/list", headers=HEADERS)
        if response.status_code == 200:
            network_types = response.json()["data"]
            found = False
            for nt in network_types:
                if nt["id"] == network_id:
                    found = True
                    print(f"✓ 在网络类型接口中找到数据: {nt['name']}")
                    break
            if not found:
                print("✗ 在网络类型接口中未找到数据")
                return False, network_id
        else:
            print(f"✗ 网络类型接口查询失败: {response.text}")
            return False, network_id
    except Exception as e:
        print(f"✗ 网络类型接口查询失败: {e}")
        return False, network_id
    
    # 3. 通过逻辑组网接口查询（应该返回相同数据）
    print("3. 通过逻辑组网接口查询...")
    try:
        response = requests.get(f"{BASE_URL}/logic-network/list", headers=HEADERS)
        if response.status_code == 200:
            logic_networks = response.json()["data"]
            found = False
            for ln in logic_networks:
                if ln["id"] == network_id:
                    found = True
                    print(f"✓ 在逻辑组网接口中找到相同数据: {ln['name']}")
                    break
            if not found:
                print("✗ 在逻辑组网接口中未找到相同数据")
                return False, network_id
        else:
            print(f"✗ 逻辑组网接口查询失败: {response.text}")
            return False, network_id
    except Exception as e:
        print(f"✗ 逻辑组网接口查询失败: {e}")
        return False, network_id
    
    return True, network_id

def test_data_consistency():
    """测试数据一致性"""
    print("\n=== 测试数据一致性 ===")
    
    # 1. 通过网络类型接口更新数据
    print("1. 通过网络类型接口更新数据...")
    update_data = {
        "name": "数据统一测试网络（已更新）",
        "description": "用于测试数据统一性的网络类型（已更新）",
        "status": 1
    }
    
    # 先获取一个网络类型ID
    try:
        response = requests.get(f"{BASE_URL}/network-type/list", headers=HEADERS)
        if response.status_code == 200:
            network_types = response.json()["data"]
            if network_types:
                test_network = network_types[0]
                network_id = test_network["id"]
                
                # 更新网络类型
                response = requests.put(f"{BASE_URL}/network-type/{network_id}", json=update_data, headers=HEADERS)
                if response.status_code == 200:
                    print("✓ 网络类型更新成功")
                else:
                    print(f"✗ 网络类型更新失败: {response.text}")
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
    
    # 2. 验证逻辑组网接口中的数据也已更新
    print("2. 验证逻辑组网接口中的数据也已更新...")
    try:
        response = requests.get(f"{BASE_URL}/logic-network/list", headers=HEADERS)
        if response.status_code == 200:
            logic_networks = response.json()["data"]
            for ln in logic_networks:
                if ln["id"] == network_id:
                    if "已更新" in ln["name"]:
                        print("✓ 逻辑组网接口中的数据已同步更新")
                        return True
                    else:
                        print("✗ 逻辑组网接口中的数据未同步更新")
                        return False
        else:
            print(f"✗ 逻辑组网接口查询失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 逻辑组网接口查询失败: {e}")
        return False

def test_interface_compatibility():
    """测试接口兼容性"""
    print("\n=== 测试接口兼容性 ===")
    
    # 1. 测试逻辑组网接口的CRUD操作
    print("1. 测试逻辑组网接口的CRUD操作...")
    
    # 创建
    create_data = {
        "name": "逻辑组网接口测试",
        "description": "通过逻辑组网接口创建的测试数据",
        "status": 1
    }
    
    try:
        response = requests.post(f"{BASE_URL}/logic-network", json=create_data, headers=HEADERS)
        if response.status_code == 200:
            network = response.json()["data"]
            network_id = network["id"]
            print(f"✓ 通过逻辑组网接口创建成功，ID: {network_id}")
        else:
            print(f"✗ 通过逻辑组网接口创建失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 通过逻辑组网接口创建失败: {e}")
        return False
    
    # 2. 验证数据在网络类型接口中可见
    print("2. 验证数据在网络类型接口中可见...")
    try:
        response = requests.get(f"{BASE_URL}/network-type/list", headers=HEADERS)
        if response.status_code == 200:
            network_types = response.json()["data"]
            found = False
            for nt in network_types:
                if nt["id"] == network_id:
                    found = True
                    print(f"✓ 在网络类型接口中找到通过逻辑组网接口创建的数据: {nt['name']}")
                    break
            if not found:
                print("✗ 在网络类型接口中未找到通过逻辑组网接口创建的数据")
                return False
        else:
            print(f"✗ 网络类型接口查询失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 网络类型接口查询失败: {e}")
        return False
    
    return network_id

def cleanup_test_data(network_ids):
    """清理测试数据"""
    print("\n=== 清理测试数据 ===")
    for network_id in network_ids:
        if network_id:
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
    print("开始测试逻辑环境管理页面的逻辑组网数据与网络类型管理页面的数据统一性...")
    
    test_network_ids = []
    
    try:
        # 测试数据源统一性
        success, network_id = test_data_source_unification()
        if success and network_id:
            test_network_ids.append(network_id)
        
        # 测试数据一致性
        consistency_ok = test_data_consistency()
        
        # 测试接口兼容性
        compatibility_network_id = test_interface_compatibility()
        if compatibility_network_id:
            test_network_ids.append(compatibility_network_id)
        
        # 总结
        print("\n=== 测试总结 ===")
        if success and consistency_ok and compatibility_network_id:
            print("✓ 所有测试通过！")
            print("✓ 逻辑环境管理页面的逻辑组网数据与网络类型管理页面的数据已成功统一。")
            print("✓ 两个页面使用同一份后台数据源。")
        else:
            print("✗ 部分测试失败，请检查实现。")
            
    except Exception as e:
        print(f"测试过程中发生错误: {e}")
        sys.exit(1)
    
    finally:
        # 清理测试数据
        if test_network_ids:
            cleanup_test_data(test_network_ids)

if __name__ == "__main__":
    main()
