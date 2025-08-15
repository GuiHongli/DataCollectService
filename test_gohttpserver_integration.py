#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试gohttpserver集成的脚本
"""

import requests
import json
import os

# 配置
BASE_URL = "http://localhost:8080/api"
TEST_FILE = "网络测试用例集_v1.0.zip"

def test_gohttpserver_config():
    """测试获取gohttpserver配置"""
    print("=== 测试获取gohttpserver配置 ===")
    try:
        response = requests.get(f"{BASE_URL}/test-case-set/gohttpserver/config")
        if response.status_code == 200:
            result = response.json()
            print(f"✅ 配置信息: {result['data']}")
        else:
            print(f"❌ 请求失败: {response.status_code}")
    except Exception as e:
        print(f"❌ 请求异常: {e}")

def test_upload_to_gohttpserver():
    """测试上传文件到gohttpserver"""
    print("\n=== 测试上传文件到gohttpserver ===")
    if not os.path.exists(TEST_FILE):
        print(f"❌ 测试文件不存在: {TEST_FILE}")
        return
    
    try:
        with open(TEST_FILE, 'rb') as f:
            files = {'file': (TEST_FILE, f, 'application/zip')}
            data = {'targetFileName': 'test_upload.zip'}
            
            response = requests.post(f"{BASE_URL}/test-case-set/gohttpserver/upload", 
                                   files=files, data=data)
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ 上传成功: {result['data']}")
            else:
                print(f"❌ 上传失败: {response.status_code}")
                print(f"响应内容: {response.text}")
    except Exception as e:
        print(f"❌ 上传异常: {e}")

def test_upload_test_case_set():
    """测试上传用例集（包含gohttpserver集成）"""
    print("\n=== 测试上传用例集 ===")
    if not os.path.exists(TEST_FILE):
        print(f"❌ 测试文件不存在: {TEST_FILE}")
        return
    
    try:
        with open(TEST_FILE, 'rb') as f:
            files = {'file': (TEST_FILE, f, 'application/zip')}
            data = {'description': '测试用例集，包含gohttpserver集成'}
            
            response = requests.post(f"{BASE_URL}/test-case-set/upload", 
                                   files=files, data=data)
            
            if response.status_code == 200:
                result = response.json()
                print(f"✅ 用例集上传成功")
                print(f"用例集信息: {json.dumps(result['data'], indent=2, ensure_ascii=False)}")
            else:
                print(f"❌ 用例集上传失败: {response.status_code}")
                print(f"响应内容: {response.text}")
    except Exception as e:
        print(f"❌ 用例集上传异常: {e}")

def test_list_test_case_sets():
    """测试获取用例集列表"""
    print("\n=== 测试获取用例集列表 ===")
    try:
        response = requests.get(f"{BASE_URL}/test-case-set/page?current=1&size=10")
        if response.status_code == 200:
            result = response.json()
            print(f"✅ 获取用例集列表成功")
            for item in result['data']['records']:
                print(f"用例集: {item['name']} v{item['version']}")
                if item.get('gohttpserverUrl'):
                    print(f"  GoHttpServer URL: {item['gohttpserverUrl']}")
        else:
            print(f"❌ 获取用例集列表失败: {response.status_code}")
    except Exception as e:
        print(f"❌ 获取用例集列表异常: {e}")

def main():
    """主函数"""
    print("开始测试gohttpserver集成...")
    
    # 测试gohttpserver配置
    test_gohttpserver_config()
    
    # 测试直接上传到gohttpserver
    test_upload_to_gohttpserver()
    
    # 测试上传用例集（包含gohttpserver集成）
    test_upload_test_case_set()
    
    # 测试获取用例集列表
    test_list_test_case_sets()
    
    print("\n测试完成！")

if __name__ == "__main__":
    main()
