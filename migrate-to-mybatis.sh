#!/bin/bash

# MyBatis Migration Script
# This script removes JPA annotations from Entity classes

ENTITY_DIR="/Users/hongxichen/Desktop/mini-ups/backend/src/main/java/com/miniups/model/entity"

echo "Starting migration of Entity classes..."

# List of entity files to migrate
ENTITIES=(
    "Shipment.java"
    "Truck.java"
    "Driver.java"
    "ShipmentPackage.java"
    "AddressChange.java"
    "ShipmentStatusHistory.java"
    "TruckLocationHistory.java"
    "CommunicationLog.java"
    "AuditLog.java"
    "OutboxEvent.java"
    "TrackingSequence.java"
    "LeafAlloc.java"
)

for entity in "${ENTITIES[@]}"; do
    file="$ENTITY_DIR/$entity"

    if [ ! -f "$file" ]; then
        echo "Skipping $entity (file not found)"
        continue
    fi

    echo "Processing $entity..."

    # Create backup
    cp "$file" "$file.bak"

    # Remove JPA import statements
    sed -i '' '/^import jakarta\.persistence\./d' "$file"
    sed -i '' '/^import org\.springframework\.data\.jpa\./d' "$file"
    sed -i '' '/^import org\.springframework\.data\.annotation\./d' "$file"

    # Remove JPA annotations (preserve validation annotations)
    sed -i '' '/@Entity/d' "$file"
    sed -i '' '/@Table/d' "$file"
    sed -i '' '/@Column/d' "$file"
    sed -i '' '/@Id/d' "$file"
    sed -i '' '/@GeneratedValue/d' "$file"
    sed -i '' '/@Enumerated/d' "$file"
    sed -i '' '/@ManyToOne/d' "$file"
    sed -i '' '/@OneToMany/d' "$file"
    sed -i '' '/@ManyToMany/d' "$file"
    sed -i '' '/@JoinColumn/d' "$file"
    sed -i '' '/@JoinTable/d' "$file"
    sed -i '' '/@OneToOne/d' "$file"
    sed -i '' '/@Index/d' "$file"
    sed -i '' '/@MappedSuperclass/d' "$file"
    sed -i '' '/@EntityListeners/d' "$file"
    sed -i '' '/@CreatedDate/d' "$file"
    sed -i '' '/@LastModifiedDate/d' "$file"
    sed -i '' '/@Version/d' "$file"

    # Clean up multiple blank lines
    sed -i '' '/^$/N;/^\n$/d' "$file"

    echo "✓ Completed $entity"
done

echo ""
echo "Entity migration completed!"
echo "Backup files created with .bak extension"
echo ""
echo "Next: Run the Repository migration script"
