#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试gohttpserver上传功能的脚本
"""
import requests
import json
import os
import time

# 配置
BACKEND_URL = "http://localhost:8080/api"
GOHTTPSERVER_URL = "http://localhost:8000"

def test_gohttpserver_health():
    """测试gohttpserver健康状态"""
    print("1. 测试gohttpserver健康状态...")
    try:
        response = requests.get(f"{GOHTTPSERVER_URL}/", timeout=10)
        if response.status_code == 200:
            print("✅ gohttpserver运行正常")
            return True
        else:
            print(f"❌ gohttpserver响应异常: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ gohttpserver连接失败: {e}")
        return False

def test_backend_gohttpserver_config():
    """测试后端gohttpserver配置"""
    print("\n2. 测试后端gohttpserver配置...")
    try:
        response = requests.get(f"{BACKEND_URL}/test-case-set/gohttpserver/config")
        if response.status_code == 200:
            data = response.json()
            print(f"✅ 后端配置正常: {data.get('data', '')}")
            return True
        else:
            print(f"❌ 后端配置异常: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 后端配置请求失败: {e}")
        return False

def test_upload_test_case_set():
    """测试上传用例集"""
    print("\n3. 测试上传用例集...")
    
    # 检查测试文件是否存在
    test_file = "网络测试用例集_v1.0.zip"
    if not os.path.exists(test_file):
        print(f"❌ 测试文件不存在: {test_file}")
        return False
    
    try:
        # 准备上传数据
        files = {
            'file': (test_file, open(test_file, 'rb'), 'application/zip')
        }
        data = {
            'description': '测试gohttpserver上传功能'
        }
        
        # 上传文件
        response = requests.post(
            f"{BACKEND_URL}/test-case-set/upload",
            files=files,
            data=data,
            timeout=60
        )
        
        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                test_case_set = result.get('data', {})
                gohttpserver_url = test_case_set.get('gohttpserverUrl')
                if gohttpserver_url:
                    print(f"✅ 上传成功，gohttpserver地址: {gohttpserver_url}")
                    
                    # 验证文件是否可以通过gohttpserver访问
                    time.sleep(2)  # 等待文件处理
                    file_response = requests.get(gohttpserver_url, timeout=10)
                    if file_response.status_code == 200:
                        print("✅ 文件可通过gohttpserver正常访问")
                    else:
                        print(f"⚠️ 文件访问异常: {file_response.status_code}")
                    
                    return True
                else:
                    print("❌ 上传成功但未返回gohttpserver地址")
                    return False
            else:
                print(f"❌ 上传失败: {result.get('message')}")
                return False
        else:
            print(f"❌ 上传请求失败: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ 上传过程出错: {e}")
        return False

def test_list_test_case_sets():
    """测试获取用例集列表"""
    print("\n4. 测试获取用例集列表...")
    try:
        response = requests.get(f"{BACKEND_URL}/test-case-set/page?current=1&size=10")
        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                records = result.get('data', {}).get('records', [])
                print(f"✅ 获取到 {len(records)} 个用例集")
                
                # 显示gohttpserver地址
                for record in records:
                    name = record.get('name', '')
                    version = record.get('version', '')
                    gohttpserver_url = record.get('gohttpserverUrl', '')
                    if gohttpserver_url:
                        print(f"  - {name} {version}: {gohttpserver_url}")
                    else:
                        print(f"  - {name} {version}: 未上传到gohttpserver")
                
                return True
            else:
                print(f"❌ 获取列表失败: {result.get('message')}")
                return False
        else:
            print(f"❌ 获取列表请求失败: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 获取列表过程出错: {e}")
        return False

def test_gohttpserver_directory():
    """测试gohttpserver目录访问"""
    print("\n5. 测试gohttpserver目录访问...")
    try:
        # 访问test_case_set目录
        dir_url = f"{GOHTTPSERVER_URL}/test_case_set/"
        response = requests.get(dir_url, timeout=10)
        
        if response.status_code == 200:
            print("✅ test_case_set目录可正常访问")
            print(f"   目录URL: {dir_url}")
            return True
        else:
            print(f"❌ test_case_set目录访问失败: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 目录访问出错: {e}")
        return False

def main():
    """主函数"""
    print("开始测试gohttpserver上传功能...")
    print("=" * 50)
    
    # 检查gohttpserver是否运行
    if not test_gohttpserver_health():
        print("\n❌ gohttpserver未运行，请先启动gohttpserver")
        return
    
    # 测试后端配置
    if not test_backend_gohttpserver_config():
        print("\n❌ 后端gohttpserver配置异常")
        return
    
    # 测试目录访问
    test_gohttpserver_directory()
    
    # 测试上传功能
    if test_upload_test_case_set():
        print("\n✅ 上传功能测试成功")
    else:
        print("\n❌ 上传功能测试失败")
    
    # 测试列表获取
    test_list_test_case_sets()
    
    print("\n" + "=" * 50)
    print("测试完成！")

if __name__ == "__main__":
    main()
