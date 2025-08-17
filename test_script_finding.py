#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试脚本查找逻辑
"""

import os
import zipfile

def test_script_finding():
    """测试脚本查找逻辑"""
    
    # 测试用例集ZIP文件
    zip_file = "网络测试用例集_v1.0_1755424247922.zip"
    
    if not os.path.exists(zip_file):
        print(f"❌ 测试文件不存在: {zip_file}")
        return
    
    print("🔍 测试脚本查找逻辑...")
    print(f"📁 测试文件: {zip_file}")
    
    # 解压到临时目录
    temp_dir = "temp_test_scripts"
    if os.path.exists(temp_dir):
        import shutil
        shutil.rmtree(temp_dir)
    os.makedirs(temp_dir)
    
    with zipfile.ZipFile(zip_file, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)
    
    # 检查scripts目录
    scripts_dir = os.path.join(temp_dir, "scripts")
    if not os.path.exists(scripts_dir):
        print("❌ scripts目录不存在")
        return
    
    print(f"✅ scripts目录存在: {scripts_dir}")
    
    # 列出所有Python脚本
    python_files = []
    for file in os.listdir(scripts_dir):
        if file.endswith('.py'):
            python_files.append(file)
    
    print(f"📋 找到的Python脚本文件:")
    for i, file in enumerate(python_files, 1):
        print(f"  {i}. {file}")
    
    # 测试用例编号匹配
    test_cases = [
        "4G_Network_Connection_Test",
        "WiFi_Connection_Stability_Test", 
        "test_network_connection",
        "test_wifi_stability"
    ]
    
    print(f"\n🔍 测试用例编号匹配:")
    for test_case in test_cases:
        expected_file = test_case + ".py"
        expected_path = os.path.join(scripts_dir, expected_file)
        
        if os.path.exists(expected_path):
            print(f"✅ {test_case} -> {expected_file} (存在)")
        else:
            print(f"❌ {test_case} -> {expected_file} (不存在)")
    
    # 清理临时目录
    import shutil
    shutil.rmtree(temp_dir)
    
    print(f"\n📋 总结:")
    print(f"- 脚本查找路径: scripts/{{用例编号}}.py")
    print(f"- 不需要查找cases目录")
    print(f"- 不需要使用用例ID")
    print(f"- 脚本不存在时状态: BLOCKED")

if __name__ == "__main__":
    test_script_finding()
