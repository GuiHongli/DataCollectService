#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
创建正确的用例集文件（简化版，不依赖pandas）
"""

import zipfile
import os
import csv
import xlsxwriter
from datetime import datetime

def create_correct_case_csv():
    """创建正确的case.csv文件"""
    
    # 基于实际的Python脚本文件名创建用例数据
    test_cases = [
        {
            "用例_名称": "4G网络连接测试",
            "用例_编号": "4G_Network_Connection_Test",
            "用例_逻辑组网": "4G标准网络;4G弱网环境",
            "用例_测试步骤": "1. 启动测试应用\n2. 检查网络连接状态\n3. 执行网络连接测试\n4. 验证连接稳定性",
            "用例_预期结果": "应用成功连接到4G网络，网络状态显示正常，连接稳定"
        },
        {
            "用例_名称": "WiFi连接稳定性测试",
            "用例_编号": "WiFi_Connection_Stability_Test",
            "用例_逻辑组网": "WiFi标准网络;WiFi弱网环境",
            "用例_测试步骤": "1. 连接到WiFi网络\n2. 进行数据传输测试\n3. 监控连接稳定性\n4. 测试网络切换",
            "用例_预期结果": "WiFi连接稳定，数据传输正常，网络切换流畅"
        },
        {
            "用例_名称": "网络连接测试",
            "用例_编号": "test_network_connection",
            "用例_逻辑组网": "4G标准网络;5G高速网络",
            "用例_测试步骤": "1. 启动网络连接测试\n2. 检查网络连接状态\n3. 执行连接测试\n4. 记录测试结果",
            "用例_预期结果": "网络连接测试通过，连接状态正常"
        },
        {
            "用例_名称": "WiFi稳定性测试",
            "用例_编号": "test_wifi_stability",
            "用例_逻辑组网": "WiFi标准网络;WiFi弱网环境",
            "用例_测试步骤": "1. 连接到WiFi网络\n2. 执行稳定性测试\n3. 监控连接质量\n4. 验证稳定性",
            "用例_预期结果": "WiFi连接稳定，稳定性测试通过"
        }
    ]
    
    # 保存为CSV文件
    output_file = "correct_case.csv"
    
    with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['用例_名称', '用例_编号', '用例_逻辑组网', '用例_测试步骤', '用例_预期结果']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for case in test_cases:
            writer.writerow(case)
    
    print(f"✅ 已创建正确的用例集CSV文件: {output_file}")
    print("\n📋 用例信息:")
    for i, case in enumerate(test_cases, 1):
        print(f"{i}. {case['用例_编号']} - {case['用例_名称']}")
    
    return output_file

def create_correct_case_xlsx():
    """创建正确的case.xlsx文件"""
    
    # 基于实际的Python脚本文件名创建用例数据
    test_cases = [
        {
            "用例_名称": "4G网络连接测试",
            "用例_编号": "4G_Network_Connection_Test",
            "用例_逻辑组网": "4G标准网络;4G弱网环境",
            "用例_测试步骤": "1. 启动测试应用\n2. 检查网络连接状态\n3. 执行网络连接测试\n4. 验证连接稳定性",
            "用例_预期结果": "应用成功连接到4G网络，网络状态显示正常，连接稳定"
        },
        {
            "用例_名称": "WiFi连接稳定性测试",
            "用例_编号": "WiFi_Connection_Stability_Test",
            "用例_逻辑组网": "WiFi标准网络;WiFi弱网环境",
            "用例_测试步骤": "1. 连接到WiFi网络\n2. 进行数据传输测试\n3. 监控连接稳定性\n4. 测试网络切换",
            "用例_预期结果": "WiFi连接稳定，数据传输正常，网络切换流畅"
        },
        {
            "用例_名称": "网络连接测试",
            "用例_编号": "test_network_connection",
            "用例_逻辑组网": "4G标准网络;5G高速网络",
            "用例_测试步骤": "1. 启动网络连接测试\n2. 检查网络连接状态\n3. 执行连接测试\n4. 记录测试结果",
            "用例_预期结果": "网络连接测试通过，连接状态正常"
        },
        {
            "用例_名称": "WiFi稳定性测试",
            "用例_编号": "test_wifi_stability",
            "用例_逻辑组网": "WiFi标准网络;WiFi弱网环境",
            "用例_测试步骤": "1. 连接到WiFi网络\n2. 执行稳定性测试\n3. 监控连接质量\n4. 验证稳定性",
            "用例_预期结果": "WiFi连接稳定，稳定性测试通过"
        }
    ]
    
    # 创建Excel文件
    output_file = "case.xlsx"
    workbook = xlsxwriter.Workbook(output_file)
    worksheet = workbook.add_worksheet()
    
    # 定义表头
    headers = ['用例_名称', '用例_编号', '用例_逻辑组网', '用例_测试步骤', '用例_预期结果']
    
    # 写入表头
    for col, header in enumerate(headers):
        worksheet.write(0, col, header)
    
    # 写入数据
    for row, case in enumerate(test_cases, 1):
        worksheet.write(row, 0, case['用例_名称'])
        worksheet.write(row, 1, case['用例_编号'])
        worksheet.write(row, 2, case['用例_逻辑组网'])
        worksheet.write(row, 3, case['用例_测试步骤'])
        worksheet.write(row, 4, case['用例_预期结果'])
    
    workbook.close()
    
    print(f"✅ 已创建正确的用例集Excel文件: {output_file}")
    
    return output_file

def create_new_testcase_zip():
    """创建新的用例集ZIP文件"""
    
    # 创建临时目录
    temp_dir = "temp_testcase"
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)
    
    # 创建scripts目录
    scripts_dir = os.path.join(temp_dir, "scripts")
    if not os.path.exists(scripts_dir):
        os.makedirs(scripts_dir)
    
    # 复制Python脚本文件
    source_zip = "/Users/zhengtengsong/projects/ghl/cursor/DataCollectService/uploads/testcase/网络测试用例集_v1.0_1755242581880.zip"
    
    with zipfile.ZipFile(source_zip, 'r') as zip_ref:
        # 提取scripts目录下的Python文件
        for file_info in zip_ref.filelist:
            if file_info.filename.startswith('scripts/') and file_info.filename.endswith('.py'):
                zip_ref.extract(file_info.filename, temp_dir)
                print(f"📁 提取文件: {file_info.filename}")
    
    # 创建正确的case.csv和case.xlsx
    case_csv = create_correct_case_csv()
    case_xlsx = create_correct_case_xlsx()
    
    # 复制到临时目录
    import shutil
    shutil.copy(case_csv, temp_dir)
    shutil.copy(case_xlsx, temp_dir)
    
    # 创建新的ZIP文件
    timestamp = int(datetime.now().timestamp() * 1000)
    new_zip_name = f"网络测试用例集_v1.0_{timestamp}.zip"
    
    with zipfile.ZipFile(new_zip_name, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_dir)
                zipf.write(file_path, arcname)
                print(f"📦 添加文件到ZIP: {arcname}")
    
    # 清理临时文件
    shutil.rmtree(temp_dir)
    os.remove(case_csv)
    os.remove(case_xlsx)
    
    print(f"\n✅ 已创建新的用例集ZIP文件: {new_zip_name}")
    print(f"📁 文件路径: {os.path.abspath(new_zip_name)}")
    
    return new_zip_name

def main():
    print("=" * 60)
    print("创建正确的用例集文件（简化版）")
    print("=" * 60)
    
    # 创建新的用例集ZIP文件
    new_zip_file = create_new_testcase_zip()
    
    print("\n" + "=" * 60)
    print("完成！")
    print("=" * 60)
    print("📋 下一步操作:")
    print("1. 将新创建的ZIP文件上传到系统")
    print("2. 更新用例集信息")
    print("3. 重新解析用例数据")
    print(f"4. 新文件: {new_zip_file}")

if __name__ == "__main__":
    main()
