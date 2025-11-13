#!/usr/bin/env python3
"""
Lombok Compilation Error Fixer for Spring Boot Project

This script fixes the following Lombok issues:
1. Replaces @Slf4j with explicit logger declarations
2. Adds necessary imports (Logger, LoggerFactory)
3. Preserves all existing code structure and formatting

Author: Mini-UPS Development Team
Version: 1.0
"""

import re
from pathlib import Path
from typing import List, Dict, Tuple


class LombokFixer:
    """Fixes Lombok compilation errors by replacing @Slf4j with explicit loggers"""

    def __init__(self):
        self.changes_report: List[Dict[str, str]] = []

    def fix_slf4j_annotation(self, file_path: Path) -> bool:
        """
        Remove @Slf4j annotation and add explicit logger field

        Args:
            file_path: Path to the Java file

        Returns:
            True if changes were made, False otherwise
        """
        content = file_path.read_text(encoding='utf-8')
        original_content = content

        # Check if file has @Slf4j annotation
        if '@Slf4j' not in content:
            return False

        # Extract class name for logger
        class_match = re.search(r'public\s+(?:class|interface|enum|record)\s+(\w+)', content)
        if not class_match:
            print(f"WARNING: Could not extract class name from {file_path}")
            return False

        class_name = class_match.group(1)

        # Check if logger field already exists
        if re.search(r'private\s+static\s+final\s+Logger\s+log\s*=', content):
            print(f"INFO: Logger field already exists in {file_path}, skipping...")
            return False

        # Remove @Slf4j annotation (can be on its own line or with other annotations)
        content = re.sub(r'@Slf4j\s*\n', '', content)
        content = re.sub(r'@Slf4j\s+', '', content)

        # Remove lombok.extern.slf4j.Slf4j import
        content = re.sub(r'import\s+lombok\.extern\.slf4j\.Slf4j;\s*\n', '', content)

        # Check if SLF4J imports already exist
        has_logger_import = 'import org.slf4j.Logger;' in content
        has_logger_factory_import = 'import org.slf4j.LoggerFactory;' in content

        # Add SLF4J imports if not present
        imports_to_add = []
        if not has_logger_import:
            imports_to_add.append('import org.slf4j.Logger;')
        if not has_logger_factory_import:
            imports_to_add.append('import org.slf4j.LoggerFactory;')

        if imports_to_add:
            # Find the last import statement
            import_pattern = r'(import\s+[^;]+;\s*\n)'
            imports = list(re.finditer(import_pattern, content))

            if imports:
                last_import = imports[-1]
                insert_pos = last_import.end()

                # Insert new imports after the last import
                new_imports = '\n'.join(imports_to_add) + '\n'
                content = content[:insert_pos] + new_imports + content[insert_pos:]

        # Add logger field after class declaration
        # Find the opening brace of the class
        class_pattern = rf'(public\s+(?:class|interface|enum|@interface)\s+{class_name}[^{{]*\{{)\s*\n'
        class_match = re.search(class_pattern, content)

        if class_match:
            insert_pos = class_match.end()
            logger_field = f'\n    private static final Logger log = LoggerFactory.getLogger({class_name}.class);\n'
            content = content[:insert_pos] + logger_field + content[insert_pos:]
        else:
            print(f"WARNING: Could not find class declaration insertion point in {file_path}")
            return False

        # Only write if content changed
        if content != original_content:
            file_path.write_text(content, encoding='utf-8')
            self.changes_report.append({
                'file': str(file_path),
                'change': f'Replaced @Slf4j with explicit logger in class {class_name}'
            })
            return True

        return False

    def process_files(self, file_paths: List[str]) -> None:
        """
        Process multiple files and fix Lombok issues

        Args:
            file_paths: List of absolute file paths to process
        """
        for file_path_str in file_paths:
            file_path = Path(file_path_str)

            if not file_path.exists():
                print(f"WARNING: File not found: {file_path}")
                continue

            if not file_path.suffix == '.java':
                print(f"WARNING: Not a Java file: {file_path}")
                continue

            print(f"Processing: {file_path.name}")

            try:
                changed = self.fix_slf4j_annotation(file_path)
                if changed:
                    print(f"  ✓ Fixed @Slf4j issue in {file_path.name}")
                else:
                    print(f"  - No changes needed for {file_path.name}")
            except Exception as e:
                print(f"  ✗ ERROR processing {file_path.name}: {e}")

    def print_report(self) -> None:
        """Print a summary report of all changes made"""
        print("\n" + "=" * 80)
        print("LOMBOK FIX SUMMARY REPORT")
        print("=" * 80)

        if not self.changes_report:
            print("\nNo changes were made. All files are already correct.")
        else:
            print(f"\nTotal files modified: {len(self.changes_report)}")
            print("\nChanges made:")
            for i, change in enumerate(self.changes_report, 1):
                print(f"\n{i}. {Path(change['file']).name}")
                print(f"   {change['change']}")
                print(f"   Path: {change['file']}")

        print("\n" + "=" * 80)


def main():
    """Main entry point for the Lombok fixer script"""

    # List of files with @Slf4j issues that need fixing
    files_to_fix = [
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/config/RabbitMQConfig.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/config/KafkaConfig.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/config/MetricsConfig.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/service/WebSocketRabbitMQService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/rag/controller/MockRagController.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/rag/config/RagDatabaseInitializer.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/rag/service/RagQueryService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/service/EventPublisherService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/service/OutboxPollerService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/service/DriverService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/controller/AdminController.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/shortlink/idgen/LeafIdGeneratorService.java",
        "/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/shortlink/idgen/SegmentBuffer.java",
    ]

    print("Lombok Compilation Error Fixer")
    print("=" * 80)
    print(f"Processing {len(files_to_fix)} files...")
    print()

    fixer = LombokFixer()
    fixer.process_files(files_to_fix)
    fixer.print_report()

    print("\n✓ Lombok fix script completed successfully!")


if __name__ == "__main__":
    main()
