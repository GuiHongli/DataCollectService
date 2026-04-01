package com.datacollect.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具类
 * 用于将中文转换为拼音
 * 
 * @author system
 * @since 2024-01-01
 */
public class PinyinUtil {
    
    /**
     * 将中文转换为拼音（无音调，小写）
     * 
     * @param chinese 中文字符串
     * @return 拼音字符串，如果输入为空或null则返回空字符串
     */
    public static String toPinyin(String chinese) {
        if (chinese == null || chinese.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE); // 小写
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE); // 无音调
        
        char[] chars = chinese.trim().toCharArray();
        for (char c : chars) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                // 是中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]); // 取第一个读音
                    } else {
                        pinyin.append(c); // 如果无法转换，保留原字符
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    pinyin.append(c); // 转换失败，保留原字符
                }
            } else {
                // 非中文字符，直接保留
                pinyin.append(c);
            }
        }
        
        return pinyin.toString();
    }
    
    /**
     * 将中文转换为拼音（首字母大写，无音调）
     * 
     * @param chinese 中文字符串
     * @return 拼音字符串，首字母大写
     */
    public static String toPinyinWithCapitalize(String chinese) {
        String pinyin = toPinyin(chinese);
        if (pinyin != null && !pinyin.isEmpty()) {
            return pinyin.substring(0, 1).toUpperCase() + pinyin.substring(1);
        }
        return pinyin;
    }
    
    /**
     * 将文件名中的中文转换为拼音
     * 保留文件扩展名和其他非中文字符
     * 
     * @param filename 文件名（例如：用例集名称_版本.zip）
     * @return 转换后的文件名（例如：yongliijimingcheng_banben.zip）
     */
    public static String convertFileNameToPinyin(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return filename;
        }
        
        // 分离文件名和扩展名
        int lastDotIndex = filename.lastIndexOf('.');
        String nameWithoutExt;
        String extension;
        
        if (lastDotIndex > 0) {
            nameWithoutExt = filename.substring(0, lastDotIndex);
            extension = filename.substring(lastDotIndex);
        } else {
            nameWithoutExt = filename;
            extension = "";
        }
        
        // 将文件名部分转换为拼音
        String pinyinName = toPinyin(nameWithoutExt);
        
        // 组合文件名和扩展名
        return pinyinName + extension;
    }
}


