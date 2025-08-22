#!/bin/bash

# Mini-UPS Security Group Configuration Script
# This script helps configure AWS Security Groups for external access

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
EC2_HOST="44.219.181.190"
AWS_REGION="us-east-1"

echo -e "${BLUE}🔐 Mini-UPS Security Group Setup${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Function to check if AWS CLI is installed
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        echo -e "${YELLOW}⚠️  AWS CLI not found. Installing it would allow automatic security group configuration.${NC}"
        echo -e "${YELLOW}   For now, please configure manually using the AWS Console.${NC}"
        echo ""
        return 1
    fi
    return 0
}

# Function to check if user is authenticated with AWS
check_aws_auth() {
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        echo -e "${RED}❌ Not authenticated with AWS. Please run 'aws configure' first.${NC}"
        return 1
    fi
    return 0
}

# Function to find security group for the instance
find_security_group() {
    local instance_ip="$1"
    
    echo -e "${YELLOW}🔍 Looking up instance and security group...${NC}"
    
    # Get instance ID from public IP
    INSTANCE_ID=$(aws ec2 describe-instances \
        --region "$AWS_REGION" \
        --filters "Name=ip-address,Values=$instance_ip" \
        --query "Reservations[*].Instances[*].InstanceId" \
        --output text 2>/dev/null)
    
    if [[ -z "$INSTANCE_ID" ]]; then
        echo -e "${RED}❌ Could not find EC2 instance with IP $instance_ip${NC}"
        return 1
    fi
    
    # Get security group ID
    SECURITY_GROUP_ID=$(aws ec2 describe-instances \
        --region "$AWS_REGION" \
        --instance-ids "$INSTANCE_ID" \
        --query "Reservations[*].Instances[*].SecurityGroups[*].GroupId" \
        --output text 2>/dev/null | head -n1)
    
    if [[ -z "$SECURITY_GROUP_ID" ]]; then
        echo -e "${RED}❌ Could not find security group for instance${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Found instance: $INSTANCE_ID${NC}"
    echo -e "${GREEN}✅ Found security group: $SECURITY_GROUP_ID${NC}"
    return 0
}

# Function to check if port rule exists
check_port_rule() {
    local sg_id="$1"
    local port="$2"
    
    aws ec2 describe-security-groups \
        --region "$AWS_REGION" \
        --group-ids "$sg_id" \
        --query "SecurityGroups[*].IpPermissions[?FromPort==\`$port\` && ToPort==\`$port\` && IpRanges[?CidrIp=='0.0.0.0/0']]" \
        --output text 2>/dev/null | grep -q "tcp"
}

# Function to add security group rule
add_port_rule() {
    local sg_id="$1"
    local port="$2"
    local description="$3"
    
    if check_port_rule "$sg_id" "$port"; then
        echo -e "${GREEN}✅ Port $port is already open${NC}"
        return 0
    fi
    
    echo -e "${YELLOW}📝 Adding rule for port $port ($description)...${NC}"
    
    aws ec2 authorize-security-group-ingress \
        --region "$AWS_REGION" \
        --group-id "$sg_id" \
        --protocol tcp \
        --port "$port" \
        --cidr 0.0.0.0/0 \
        --source-group-description "$description" >/dev/null 2>&1
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Port $port opened successfully${NC}"
    else
        echo -e "${RED}❌ Failed to open port $port${NC}"
    fi
}

# Manual configuration instructions
show_manual_instructions() {
    echo -e "${BLUE}📋 Manual Security Group Configuration${NC}"
    echo -e "${BLUE}=====================================${NC}"
    echo ""
    echo -e "${YELLOW}Please follow these steps in the AWS Console:${NC}"
    echo ""
    echo -e "1. Go to: ${GREEN}https://console.aws.amazon.com/ec2/v2/home?region=$AWS_REGION#SecurityGroups${NC}"
    echo -e "2. Find the security group for your EC2 instance (IP: $EC2_HOST)"
    echo -e "3. Click on the security group ID"
    echo -e "4. Go to the ${GREEN}\"Inbound rules\"${NC} tab"
    echo -e "5. Click ${GREEN}\"Edit inbound rules\"${NC}"
    echo -e "6. Add the following rules:"
    echo ""
    echo -e "${BLUE}┌─────────────────────────────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│ Type    Protocol  Port Range  Source     Description            │${NC}"
    echo -e "${BLUE}├─────────────────────────────────────────────────────────────────┤${NC}"
    echo -e "${BLUE}│ HTTP    TCP       3000        0.0.0.0/0  UPS Frontend           │${NC}"
    echo -e "${BLUE}│ HTTP    TCP       8081        0.0.0.0/0  UPS Backend API        │${NC}"
    echo -e "${BLUE}│ HTTP    TCP       8080        0.0.0.0/0  Amazon Service         │${NC}"
    echo -e "${BLUE}│ HTTP    TCP       15672       0.0.0.0/0  RabbitMQ Management    │${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "7. Click ${GREEN}\"Save rules\"${NC}"
    echo ""
    echo -e "${GREEN}After configuring, your services will be accessible at:${NC}"
    echo -e "   🌐 UPS Frontend: ${YELLOW}http://$EC2_HOST:3000${NC}"
    echo -e "   🔌 UPS Backend API: ${YELLOW}http://$EC2_HOST:8081${NC}"
    echo -e "   🛒 Amazon Service: ${YELLOW}http://$EC2_HOST:8080${NC}"
    echo -e "   📊 RabbitMQ Management: ${YELLOW}http://$EC2_HOST:15672${NC}"
}

# Automatic configuration
configure_security_groups() {
    echo -e "${YELLOW}🔧 Configuring security groups automatically...${NC}"
    
    if ! find_security_group "$EC2_HOST"; then
        echo -e "${RED}❌ Could not find instance or security group${NC}"
        show_manual_instructions
        return 1
    fi
    
    echo ""
    echo -e "${YELLOW}📝 Adding security group rules...${NC}"
    
    # Required ports for Mini-UPS
    add_port_rule "$SECURITY_GROUP_ID" "3000" "Mini-UPS Frontend"
    add_port_rule "$SECURITY_GROUP_ID" "8081" "Mini-UPS Backend API"
    add_port_rule "$SECURITY_GROUP_ID" "8080" "Amazon Service"
    add_port_rule "$SECURITY_GROUP_ID" "15672" "RabbitMQ Management UI"
    
    echo ""
    echo -e "${GREEN}🎉 Security group configuration completed!${NC}"
    echo ""
    echo -e "${GREEN}Your services should now be accessible at:${NC}"
    echo -e "   🌐 UPS Frontend: ${YELLOW}http://$EC2_HOST:3000${NC}"
    echo -e "   🔌 UPS Backend API: ${YELLOW}http://$EC2_HOST:8081${NC}"
    echo -e "   🛒 Amazon Service: ${YELLOW}http://$EC2_HOST:8080${NC}"
    echo -e "   📊 RabbitMQ Management: ${YELLOW}http://$EC2_HOST:15672${NC}"
    echo ""
    echo -e "${BLUE}💡 Test connectivity with:${NC}"
    echo -e "   ${YELLOW}curl -I http://$EC2_HOST:3000${NC}"
    echo -e "   ${YELLOW}curl -I http://$EC2_HOST:8081/actuator/health${NC}"
}

# Test connectivity
test_connectivity() {
    echo -e "${YELLOW}🔍 Testing connectivity to services...${NC}"
    echo ""
    
    # Test each service
    services=(
        "3000:UPS Frontend"
        "8081:UPS Backend API"
        "8080:Amazon Service"
        "15672:RabbitMQ Management"
    )
    
    for service in "${services[@]}"; do
        port="${service%%:*}"
        name="${service##*:}"
        
        echo -n "Testing $name (port $port)... "
        
        if curl -s --max-time 5 "http://$EC2_HOST:$port" >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Accessible${NC}"
        else
            if curl -s --max-time 5 -I "http://$EC2_HOST:$port" >/dev/null 2>&1; then
                echo -e "${GREEN}✅ Accessible${NC}"
            else
                echo -e "${RED}❌ Not accessible${NC}"
            fi
        fi
    done
}

# Main execution
main() {
    case "${1:-auto}" in
        "auto")
            if check_aws_cli && check_aws_auth; then
                configure_security_groups
            else
                show_manual_instructions
            fi
            ;;
        "manual")
            show_manual_instructions
            ;;
        "test")
            test_connectivity
            ;;
        *)
            echo "Usage: $0 [auto|manual|test]"
            echo ""
            echo "Commands:"
            echo "  auto   - Automatically configure security groups (requires AWS CLI)"
            echo "  manual - Show manual configuration instructions"
            echo "  test   - Test connectivity to services"
            ;;
    esac
}

main "$@"