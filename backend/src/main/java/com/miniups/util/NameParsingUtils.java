/**
 * Name Parsing Utilities
 * 
 * Function Description:
 * - Provides intelligent name parsing functionality
 * - Supports multiple name formats and cultural backgrounds
 * - Handles various edge cases
 * 
 * Supported Formats:
 * - Western names: firstName lastName
 * - Single name: name
 * - Compound names: firstName middleName lastName
 * - Names with prefixes/suffixes: Mr./Dr./Jr./Sr.
 * 
 * Features:
 * - Input validation and cleaning
 * - Smart splitting algorithm
 * - Internationalization support
 * 
 *
 
 */
package com.miniups.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NameParsingUtils {
    
    // Prevent instantiation
    private NameParsingUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // Common prefixes and suffixes
    private static final Set<String> PREFIXES = new HashSet<>(Arrays.asList(
        "Mr", "Mrs", "Ms", "Miss", "Dr", "Prof", "Sir", "Madam", "Lord", "Lady"
    ));
    
    private static final Set<String> SUFFIXES = new HashSet<>(Arrays.asList(
        "Jr", "Sr", "II", "III", "IV", "V", "PhD", "MD", "Esq"
    ));
    
    /**
     * Parsed name structure
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
     * Parse full name
     * 
     * @param fullName Full name string
     * @return Parsed name object
     * @throws IllegalArgumentException If name is null or invalid
     */
    public static ParsedName parseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        // Clean input: remove extra spaces, standardize format
        String cleanedName = cleanName(fullName);
        String[] parts = cleanedName.split("\\s+");
        
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid name format");
        }
        
        return parseNameParts(parts);
    }
    
    /**
     * Validate if name is valid
     * 
     * @param fullName Full name
     * @return Whether the name is valid
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
     * Generate username suggestion
     * 
     * @param fullName Full name
     * @return Suggested username
     */
    public static String generateUsernameFromName(String fullName) {
        try {
            ParsedName parsed = parseName(fullName);
            StringBuilder username = new StringBuilder();
            
            // Use first few characters of first name
            username.append(parsed.getFirstName().toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
            
            // If has last name, add first character of last name
            if (parsed.hasLastName()) {
                username.append(parsed.getLastName().toLowerCase().charAt(0));
            }
            
            // If username is too short, add random suffix
            if (username.length() < 3) {
                username.append(UUID.randomUUID().toString().substring(0, 4));
            }
            
            return username.toString();
        } catch (Exception e) {
            // If parsing fails, generate generic username
            return "customer_" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * Clean name string
     * 
     * @param name Original name
     * @return Cleaned name
     */
    private static String cleanName(String name) {
        return name
            .trim()
            .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
            .replaceAll("[^\\p{L}\\s\\-'.]", "");  // Only keep letters, spaces, hyphens, apostrophes, and periods
    }
    
    /**
     * Parse name components
     * 
     * @param parts Name parts array
     * @return Parsed name object
     */
    private static ParsedName parseNameParts(String[] parts) {
        String prefix = "";
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String suffix = "";
        
        int startIndex = 0;
        int endIndex = parts.length;
        
        // Check for prefix
        if (parts.length > 1 && isPrefix(parts[0])) {
            prefix = parts[0];
            startIndex = 1;
        }
        
        // Check for suffix
        if (parts.length > 1 && isSuffix(parts[parts.length - 1])) {
            suffix = parts[parts.length - 1];
            endIndex = parts.length - 1;
        }
        
        // Parse name parts
        int namePartsCount = endIndex - startIndex;
        
        if (namePartsCount == 1) {
            // Single name case
            firstName = parts[startIndex];
        } else if (namePartsCount == 2) {
            // First name + Last name
            firstName = parts[startIndex];
            lastName = parts[startIndex + 1];
        } else if (namePartsCount >= 3) {
            // First name + Middle name(s) + Last name
            firstName = parts[startIndex];
            lastName = parts[endIndex - 1];
            
            // Combine all middle parts as middle name
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
     * Check if word is a prefix
     * 
     * @param word Word
     * @return Whether it is a prefix
     */
    private static boolean isPrefix(String word) {
        String normalized = word.replace(".", "");
        return PREFIXES.contains(normalized);
    }
    
    /**
     * Check if word is a suffix
     * 
     * @param word Word
     * @return Whether it is a suffix
     */
    private static boolean isSuffix(String word) {
        String normalized = word.replace(".", "");
        return SUFFIXES.contains(normalized);
    }
}