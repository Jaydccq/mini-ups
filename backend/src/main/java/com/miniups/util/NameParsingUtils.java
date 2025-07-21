/**
 * Name Parsing Utilities
 * 
 * 功能说明：
 * - 提供智能的姓名解析功能
 * - 支持多种姓名格式和文化背景
 * - 处理各种边缘情况
 * 
 * 支持的格式：
 * - 西方姓名：firstName lastName
 * - 单名：name
 * - 复合姓名：firstName middleName lastName
 * - 带前缀/后缀的姓名：Mr./Dr./Jr./Sr.
 * 
 * 特性：
 * - 输入验证和清理
 * - 智能分割算法
 * - 国际化支持
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class NameParsingUtils {
    
    // 防止实例化
    private NameParsingUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // 常见的前缀和后缀
    private static final Set<String> PREFIXES = new HashSet<>(Arrays.asList(
        "Mr", "Mrs", "Ms", "Miss", "Dr", "Prof", "Sir", "Madam", "Lord", "Lady"
    ));
    
    private static final Set<String> SUFFIXES = new HashSet<>(Arrays.asList(
        "Jr", "Sr", "II", "III", "IV", "V", "PhD", "MD", "Esq"
    ));
    
    /**
     * 解析后的姓名结构
     */
    public static class ParsedName {
        private final String firstName;
        private final String lastName;
        private final String middleName;
        private final String prefix;
        private final String suffix;
        
        public ParsedName(String firstName, String lastName, String middleName, String prefix, String suffix) {
            this.firstName = firstName != null ? firstName.trim() : "";
            this.lastName = lastName != null ? lastName.trim() : "";
            this.middleName = middleName != null ? middleName.trim() : "";
            this.prefix = prefix != null ? prefix.trim() : "";
            this.suffix = suffix != null ? suffix.trim() : "";
        }
        
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getMiddleName() { return middleName; }
        public String getPrefix() { return prefix; }
        public String getSuffix() { return suffix; }
        
        public String getFullName() {
            StringBuilder fullName = new StringBuilder();
            if (!prefix.isEmpty()) {
                fullName.append(prefix).append(" ");
            }
            fullName.append(firstName);
            if (!middleName.isEmpty()) {
                fullName.append(" ").append(middleName);
            }
            if (!lastName.isEmpty()) {
                fullName.append(" ").append(lastName);
            }
            if (!suffix.isEmpty()) {
                fullName.append(" ").append(suffix);
            }
            return fullName.toString().trim();
        }
        
        public boolean hasLastName() {
            return !lastName.isEmpty();
        }
        
        public boolean isValidName() {
            return !firstName.isEmpty() && firstName.length() >= 1;
        }
    }
    
    /**
     * 解析完整姓名
     * 
     * @param fullName 完整姓名字符串
     * @return 解析后的姓名对象
     * @throws IllegalArgumentException 如果姓名为空或无效
     */
    public static ParsedName parseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        // 清理输入：移除多余空格，标准化格式
        String cleanedName = cleanName(fullName);
        String[] parts = cleanedName.split("\\s+");
        
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid name format");
        }
        
        return parseNameParts(parts);
    }
    
    /**
     * 验证姓名是否有效
     * 
     * @param fullName 完整姓名
     * @return 姓名是否有效
     */
    public static boolean isValidName(String fullName) {
        try {
            ParsedName parsed = parseName(fullName);
            return parsed.isValidName();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 生成用户名建议
     * 
     * @param fullName 完整姓名
     * @return 建议的用户名
     */
    public static String generateUsernameFromName(String fullName) {
        try {
            ParsedName parsed = parseName(fullName);
            StringBuilder username = new StringBuilder();
            
            // 使用名字的前几个字符
            username.append(parsed.getFirstName().toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
            
            // 如果有姓氏，添加姓氏的第一个字符
            if (parsed.hasLastName()) {
                username.append(parsed.getLastName().toLowerCase().charAt(0));
            }
            
            // 如果用户名太短，添加时间戳
            if (username.length() < 3) {
                username.append(System.currentTimeMillis() % 1000);
            }
            
            return username.toString();
        } catch (Exception e) {
            // 如果解析失败，生成通用用户名
            return "customer_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 清理姓名字符串
     * 
     * @param name 原始姓名
     * @return 清理后的姓名
     */
    private static String cleanName(String name) {
        return name
            .trim()
            .replaceAll("\\s+", " ")  // 多个空格替换为单个空格
            .replaceAll("[^\\p{L}\\s\\-'.]", "");  // 只保留字母、空格、连字符、撇号和点号
    }
    
    /**
     * 解析姓名组成部分
     * 
     * @param parts 姓名部分数组
     * @return 解析后的姓名对象
     */
    private static ParsedName parseNameParts(String[] parts) {
        String prefix = "";
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String suffix = "";
        
        int startIndex = 0;
        int endIndex = parts.length;
        
        // 检查前缀
        if (parts.length > 1 && isPrefix(parts[0])) {
            prefix = parts[0];
            startIndex = 1;
        }
        
        // 检查后缀
        if (parts.length > 1 && isSuffix(parts[parts.length - 1])) {
            suffix = parts[parts.length - 1];
            endIndex = parts.length - 1;
        }
        
        // 解析名字部分
        int namePartsCount = endIndex - startIndex;
        
        if (namePartsCount == 1) {
            // 单名情况
            firstName = parts[startIndex];
        } else if (namePartsCount == 2) {
            // 名 + 姓
            firstName = parts[startIndex];
            lastName = parts[startIndex + 1];
        } else if (namePartsCount >= 3) {
            // 名 + 中间名 + 姓（可能有多个中间名）
            firstName = parts[startIndex];
            lastName = parts[endIndex - 1];
            
            // 将中间的所有部分作为中间名
            StringBuilder middle = new StringBuilder();
            for (int i = startIndex + 1; i < endIndex - 1; i++) {
                if (middle.length() > 0) {
                    middle.append(" ");
                }
                middle.append(parts[i]);
            }
            middleName = middle.toString();
        }
        
        return new ParsedName(firstName, lastName, middleName, prefix, suffix);
    }
    
    /**
     * 检查是否为前缀
     * 
     * @param word 单词
     * @return 是否为前缀
     */
    private static boolean isPrefix(String word) {
        String normalized = word.replace(".", "");
        return PREFIXES.contains(normalized);
    }
    
    /**
     * 检查是否为后缀
     * 
     * @param word 单词
     * @return 是否为后缀
     */
    private static boolean isSuffix(String word) {
        String normalized = word.replace(".", "");
        return SUFFIXES.contains(normalized);
    }
}