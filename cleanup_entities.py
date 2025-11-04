#!/usr/bin/env python3
import re
import os
import glob

ENTITY_DIR = "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/model/entity"

# JPA annotations to remove
JPA_ANNOTATIONS = [
    r'@Entity\s*',
    r'@Table\([^)]*\)\s*',
    r'@Column\([^)]*\)\s*',
    r'@Id\s*',
    r'@GeneratedValue\([^)]*\)\s*',
    r'@Enumerated\([^)]*\)\s*',
    r'@ManyToOne\([^)]*\)\s*',
    r'@OneToMany\([^)]*\)\s*',
    r'@ManyToMany\([^)]*\)\s*',
    r'@JoinColumn\([^)]*\)\s*',
    r'@JoinTable\([^)]*\)\s*',
    r'@OneToOne\([^)]*\)\s*',
    r'@Index\([^)]*\)\s*',
    r'@MappedSuperclass\s*',
    r'@EntityListeners\([^)]*\)\s*',
    r'@CreatedDate\s*',
    r'@LastModifiedDate\s*',
    r'@Version\s*',
    r'@UpdateTimestamp\s*',
    r'@CreationTimestamp\s*',
]

# Import statements to remove
IMPORTS_TO_REMOVE = [
    r'import jakarta\.persistence\..*;',
    r'import org\.springframework\.data\.jpa\..*;',
    r'import org\.springframework\.data\.annotation\..*;',
    r'import org\.hibernate\..*;',
]

def clean_file(file_path):
    print(f"Processing {os.path.basename(file_path)}...")

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Remove import statements
    for pattern in IMPORTS_TO_REMOVE:
        content = re.sub(pattern, '', content, flags=re.MULTILINE)

    # Remove JPA annotations (line by line to be safer)
    lines = content.split('\n')
    cleaned_lines = []
    skip_next = False

    for line in lines:
        # Check if this line contains only a JPA annotation
        is_jpa_annotation = False
        for pattern in JPA_ANNOTATIONS:
            if re.match(r'^\s*' + pattern + r'$', line):
                is_jpa_annotation = True
                break

        if not is_jpa_annotation:
            # Remove annotations from the middle of lines
            for pattern in JPA_ANNOTATIONS:
                line = re.sub(pattern, '', line)
            cleaned_lines.append(line)

    # Remove multiple consecutive blank lines
    final_lines = []
    prev_blank = False
    for line in cleaned_lines:
        is_blank = line.strip() == ''
        if not (is_blank and prev_blank):
            final_lines.append(line)
        prev_blank = is_blank

    content = '\n'.join(final_lines)

    # Write back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"✓ Cleaned {os.path.basename(file_path)}")

# Process all Java files in entity directory
pattern = os.path.join(ENTITY_DIR, "*.java")
java_files = glob.glob(pattern)

# Exclude BaseEntity and User (already done)
exclude = ["BaseEntity.java", "User.java"]
java_files = [f for f in java_files if os.path.basename(f) not in exclude]

for file_path in java_files:
    clean_file(file_path)

print("\nEntity cleanup completed!")
