#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试逻辑环境管理页面查询表格数据时，逻辑组网列的数据是否来自网络类型管理
"""

import requests
import json
import sys

# 配置
BASE_URL = "http://localhost:8080"
HEADERS = {
    "Content-Type": "application/json"
}

def test_table_data_network_source():
    """测试表格数据中逻辑组网列的数据来源"""
    print("=== 测试表格数据中逻辑组网列的数据来源 ===")
    
    # 1. 创建测试网络类型
    print("1. 创建测试网络类型...")
    test_networks = [
        {
            "name": "表格测试网络1",
            "description": "用于测试表格数据关联的网络类型1",
            "status": 1
        },
        {
            "name": "表格测试网络2",
            "description": "用于测试表格数据关联的网络类型2",
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
        "name": "表格数据测试环境",
        "description": "用于测试表格数据关联的逻辑环境",
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
    
    # 4. 查询逻辑环境表格数据
    print("4. 查询逻辑环境表格数据...")
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/page", headers=HEADERS)
        if response.status_code == 200:
            page_data = response.json()["data"]
            logic_environments = page_data["records"]
            print(f"✓ 获取逻辑环境表格数据成功，共 {len(logic_environments)} 个环境")
            
            # 查找我们创建的测试环境
            test_env = None
            for env in logic_environments:
                if env["id"] == logic_env_id:
                    test_env = env
                    break
            
            if test_env:
                print(f"✓ 找到测试环境: {test_env['name']}")
                
                # 检查逻辑组网列的数据
                if "networkList" in test_env and test_env["networkList"]:
                    print(f"✓ 逻辑组网列有数据，共 {len(test_env['networkList'])} 个网络")
                    
                    # 验证网络数据是否来自网络类型管理
                    for network in test_env["networkList"]:
                        print(f"  - 网络ID: {network['id']}, 名称: {network['name']}, 描述: {network['description']}")
                        
                        # 验证这个网络ID是否在我们创建的网络类型中
                        if network['id'] in created_network_ids:
                            print(f"    ✓ 确认网络 {network['name']} 来自网络类型管理数据")
                        else:
                            print(f"    ✗ 网络 {network['name']} 不在网络类型管理数据中")
                            return False, created_network_ids
                else:
                    print("✗ 逻辑组网列没有数据")
                    return False, created_network_ids
            else:
                print("✗ 未找到测试环境")
                return False, created_network_ids
        else:
            print(f"✗ 获取逻辑环境表格数据失败: {response.text}")
            return False, created_network_ids
    except Exception as e:
        print(f"✗ 获取逻辑环境表格数据失败: {e}")
        return False, created_network_ids
    
    return logic_env_id, created_network_ids

def test_network_data_sync_in_table():
    """测试表格数据中网络数据的同步性"""
    print("\n=== 测试表格数据中网络数据的同步性 ===")
    
    # 1. 通过网络类型管理修改数据
    print("1. 通过网络类型管理修改数据...")
    
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
                    "name": f"{test_network['name']}（表格同步测试）",
                    "description": f"{test_network['description']}（表格同步测试）",
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
    
    # 2. 验证表格数据中的网络信息是否同步更新
    print("2. 验证表格数据中的网络信息是否同步更新...")
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/page", headers=HEADERS)
        if response.status_code == 200:
            page_data = response.json()["data"]
            logic_environments = page_data["records"]
            
            # 查找包含该网络的环境
            found_sync = False
            for env in logic_environments:
                if "networkList" in env and env["networkList"]:
                    for network in env["networkList"]:
                        if network['id'] == network_id:
                            if "表格同步测试" in network['name']:
                                print(f"✓ 逻辑环境 {env['name']} 中的网络信息已同步更新")
                                found_sync = True
                                break
                            else:
                                print(f"✗ 逻辑环境 {env['name']} 中的网络信息未同步更新")
                                return False
                if found_sync:
                    break
            
            if not found_sync:
                print("✗ 未找到包含该网络的环境或数据未同步")
                return False
        else:
            print(f"✗ 获取逻辑环境表格数据失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 逻辑环境表格数据操作失败: {e}")
        return False
    
    return True

def test_network_status_filter_in_table():
    """测试表格数据中网络状态过滤"""
    print("\n=== 测试表格数据中网络状态过滤 ===")
    
    # 1. 创建一个网络类型并关联到环境
    print("1. 创建网络类型并关联到环境...")
    
    # 创建网络类型
    network_data = {
        "name": "状态过滤测试网络",
        "description": "用于测试状态过滤的网络类型",
        "status": 1
    }
    
    try:
        response = requests.post(f"{BASE_URL}/network-type", json=network_data, headers=HEADERS)
        if response.status_code == 200:
            network = response.json()["data"]
            network_id = network["id"]
            print(f"✓ 创建网络类型成功: {network['name']} (ID: {network_id})")
        else:
            print(f"✗ 创建网络类型失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 创建网络类型失败: {e}")
        return False
    
    # 获取一个逻辑环境并关联网络
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/page", headers=HEADERS)
        if response.status_code == 200:
            page_data = response.json()["data"]
            logic_environments = page_data["records"]
            if logic_environments:
                test_env = logic_environments[0]
                env_id = test_env["id"]
                
                # 关联网络到环境
                response = requests.post(f"{BASE_URL}/logic-environment/{env_id}/network", 
                                       json=[network_id], headers=HEADERS)
                if response.status_code == 200:
                    print(f"✓ 网络关联到环境成功")
                else:
                    print(f"✗ 网络关联失败: {response.text}")
                    return False
            else:
                print("✗ 没有可用的逻辑环境")
                return False
        else:
            print(f"✗ 获取逻辑环境列表失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 逻辑环境操作失败: {e}")
        return False
    
    # 2. 禁用网络类型
    print("2. 禁用网络类型...")
    try:
        update_data = {
            "name": "状态过滤测试网络",
            "description": "用于测试状态过滤的网络类型",
            "status": 0  # 禁用状态
        }
        
        response = requests.put(f"{BASE_URL}/network-type/{network_id}", json=update_data, headers=HEADERS)
        if response.status_code == 200:
            print("✓ 网络类型禁用成功")
        else:
            print(f"✗ 网络类型禁用失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 网络类型禁用失败: {e}")
        return False
    
    # 3. 验证表格数据中是否还显示该网络
    print("3. 验证表格数据中是否还显示该网络...")
    try:
        response = requests.get(f"{BASE_URL}/logic-environment/page", headers=HEADERS)
        if response.status_code == 200:
            page_data = response.json()["data"]
            logic_environments = page_data["records"]
            
            # 查找包含该网络的环境
            for env in logic_environments:
                if "networkList" in env and env["networkList"]:
                    for network in env["networkList"]:
                        if network['id'] == network_id:
                            print(f"✗ 禁用的网络类型仍然显示在表格数据中: {network['name']}")
                            return False
            
            print("✓ 禁用的网络类型已从表格数据中过滤")
        else:
            print(f"✗ 获取逻辑环境表格数据失败: {response.text}")
            return False
    except Exception as e:
        print(f"✗ 逻辑环境表格数据操作失败: {e}")
        return False
    
    return network_id

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
    print("开始测试逻辑环境管理页面查询表格数据时，逻辑组网列的数据是否来自网络类型管理...")
    
    try:
        # 测试表格数据中逻辑组网列的数据来源
        result = test_table_data_network_source()
        if result:
            logic_env_id, network_ids = result
        else:
            print("✗ 表格数据网络来源测试失败")
            return
        
        # 测试网络数据同步性
        sync_ok = test_network_data_sync_in_table()
        
        # 测试网络状态过滤
        status_network_id = test_network_status_filter_in_table()
        if status_network_id:
            network_ids.append(status_network_id)
        
        # 总结
        print("\n=== 测试总结 ===")
        if result and sync_ok and status_network_id:
            print("✓ 所有测试通过！")
            print("✓ 逻辑环境管理页面查询表格数据时，逻辑组网列的数据确实来自网络类型管理。")
            print("✓ 数据修改会实时同步到表格数据中。")
            print("✓ 网络状态过滤功能正常工作。")
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

