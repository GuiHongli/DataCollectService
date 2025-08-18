#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试Python执行器配置
"""

import os
import sys
import subprocess

def test_python_executor():
    """测试Python执行器配置"""
    
    print("🔍 测试Python执行器配置...")
    
    # 获取当前目录
    current_dir = os.getcwd()
    print(f"当前目录: {current_dir}")
    
    # 检查是否是CaseExecuteService目录
    if "CaseExecuteService" in current_dir:
        print("✅ 当前在CaseExecuteService目录")
        
        # 计算DataCollectService目录路径
        data_collect_service_dir = current_dir.replace("/CaseExecuteService", "/DataCollectService")
        print(f"DataCollectService目录: {data_collect_service_dir}")
        
        # 检查DataCollectService的venv是否存在
        venv_python_path = os.path.join(data_collect_service_dir, "venv", "bin", "python")
        print(f"venv Python路径: {venv_python_path}")
        
        if os.path.exists(venv_python_path):
            print("✅ DataCollectService venv存在")
            
            # 测试venv中的Python
            try:
                result = subprocess.run([venv_python_path, "--version"], 
                                      capture_output=True, text=True, timeout=10)
                if result.returncode == 0:
                    print(f"✅ venv Python版本: {result.stdout.strip()}")
                else:
                    print(f"❌ venv Python执行失败: {result.stderr}")
            except Exception as e:
                print(f"❌ venv Python测试异常: {e}")
        else:
            print("❌ DataCollectService venv不存在")
        
        # 测试系统Python3
        try:
            result = subprocess.run(["python3", "--version"], 
                                  capture_output=True, text=True, timeout=10)
            if result.returncode == 0:
                print(f"✅ 系统Python3版本: {result.stdout.strip()}")
            else:
                print(f"❌ 系统Python3执行失败: {result.stderr}")
        except Exception as e:
            print(f"❌ 系统Python3测试异常: {e}")
        
        # 测试系统Python
        try:
            result = subprocess.run(["python", "--version"], 
                                  capture_output=True, text=True, timeout=10)
            if result.returncode == 0:
                print(f"✅ 系统Python版本: {result.stdout.strip()}")
            else:
                print(f"❌ 系统Python执行失败: {result.stderr}")
        except Exception as e:
            print(f"❌ 系统Python测试异常: {e}")
        
        # 测试工作目录切换
        print(f"\n🔍 测试工作目录切换...")
        print(f"当前工作目录: {os.getcwd()}")
        
        # 切换到DataCollectService目录
        os.chdir(data_collect_service_dir)
        print(f"切换后工作目录: {os.getcwd()}")
        
        # 检查venv中的Python包
        try:
            result = subprocess.run([venv_python_path, "-c", "import requests; print('requests模块可用')"], 
                                  capture_output=True, text=True, timeout=10)
            if result.returncode == 0:
                print(f"✅ {result.stdout.strip()}")
            else:
                print(f"❌ requests模块不可用: {result.stderr}")
        except Exception as e:
            print(f"❌ 测试requests模块异常: {e}")
        
        # 切换回原目录
        os.chdir(current_dir)
        print(f"切换回原目录: {os.getcwd()}")
        
    else:
        print("❌ 当前不在CaseExecuteService目录")

def main():
    print("=" * 60)
    print("Python执行器配置测试")
    print("=" * 60)
    
    # 测试Python执行器配置
    test_python_executor()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. CaseExecuteService应该使用DataCollectService目录下的venv")
    print("2. 如果venv不存在，则使用系统Python3")
    print("3. 工作目录应该设置为DataCollectService目录")
    print("4. 这样可以确保Python脚本能够访问到正确的依赖")

if __name__ == "__main__":
    main()

