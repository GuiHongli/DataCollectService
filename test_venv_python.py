#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试venv环境的使用
"""

import os
import sys
import subprocess

def test_venv_environment():
    """测试venv环境"""
    
    print("🔍 测试venv环境...")
    
    # 获取当前Python路径
    python_executable = sys.executable
    print(f"📋 当前Python路径: {python_executable}")
    
    # 检查是否在venv环境中
    if hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix):
        print("✅ 当前在虚拟环境中")
        print(f"   虚拟环境路径: {sys.prefix}")
    else:
        print("❌ 当前不在虚拟环境中")
    
    # 检查项目venv路径
    project_dir = os.getcwd()
    venv_python_path = os.path.join(project_dir, "venv", "bin", "python")
    
    if os.path.exists(venv_python_path):
        print(f"✅ 项目venv Python存在: {venv_python_path}")
        
        # 测试venv Python版本
        try:
            result = subprocess.run([venv_python_path, "--version"], 
                                  capture_output=True, text=True, timeout=10)
            if result.returncode == 0:
                print(f"📋 venv Python版本: {result.stdout.strip()}")
            else:
                print(f"❌ venv Python版本检查失败: {result.stderr}")
        except Exception as e:
            print(f"❌ venv Python版本检查异常: {e}")
    else:
        print(f"❌ 项目venv Python不存在: {venv_python_path}")
    
    # 检查venv中的包
    try:
        result = subprocess.run([venv_python_path, "-m", "pip", "list"], 
                              capture_output=True, text=True, timeout=10)
        if result.returncode == 0:
            print(f"📋 venv中的包:")
            lines = result.stdout.strip().split('\n')
            for line in lines[2:]:  # 跳过标题行
                if line.strip():
                    print(f"   {line.strip()}")
        else:
            print(f"❌ venv包列表检查失败: {result.stderr}")
    except Exception as e:
        print(f"❌ venv包列表检查异常: {e}")

def test_python_execution():
    """测试Python执行"""
    
    print("\n🔍 测试Python执行...")
    
    # 创建测试脚本
    test_script = """
import sys
import os

print("Python版本:", sys.version)
print("Python路径:", sys.executable)
print("当前工作目录:", os.getcwd())
print("环境变量PATH:", os.environ.get('PATH', '未设置'))

# 检查是否在venv中
if hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix):
    print("✅ 在虚拟环境中")
    print("虚拟环境路径:", sys.prefix)
else:
    print("❌ 不在虚拟环境中")

# 测试导入requests
try:
    import requests
    print("✅ requests模块可用")
except ImportError:
    print("❌ requests模块不可用")
"""
    
    # 写入测试脚本文件
    with open("test_venv_script.py", "w", encoding="utf-8") as f:
        f.write(test_script)
    
    print("📝 创建测试脚本: test_venv_script.py")
    
    # 使用venv Python执行测试脚本
    project_dir = os.getcwd()
    venv_python_path = os.path.join(project_dir, "venv", "bin", "python")
    
    if os.path.exists(venv_python_path):
        try:
            print(f"🚀 使用venv Python执行测试脚本: {venv_python_path}")
            result = subprocess.run([venv_python_path, "test_venv_script.py"], 
                                  capture_output=True, text=True, timeout=30)
            
            print("📥 执行结果:")
            print(result.stdout)
            
            if result.stderr:
                print("📥 错误输出:")
                print(result.stderr)
                
        except Exception as e:
            print(f"❌ 执行异常: {e}")
    else:
        print("❌ venv Python不存在，无法测试")
    
    # 清理测试文件
    if os.path.exists("test_venv_script.py"):
        os.remove("test_venv_script.py")
        print("🧹 清理测试文件")

def main():
    print("=" * 60)
    print("venv环境测试")
    print("=" * 60)
    
    # 测试venv环境
    test_venv_environment()
    
    # 测试Python执行
    test_python_execution()
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("📋 总结:")
    print("1. CaseExecuteService现在会优先使用项目venv中的Python")
    print("2. 如果venv不存在，会回退到系统Python")
    print("3. 工作目录设置为项目根目录")
    print("4. 确保Python脚本在正确的环境中执行")

if __name__ == "__main__":
    main()
