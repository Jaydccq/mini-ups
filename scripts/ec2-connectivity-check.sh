#!/bin/bash

# EC2 Instance Connectivity and Deployment Check Script
# Run this script locally to test connectivity to your EC2 instance

set -e

# Configuration
EC2_HOST="${1:-44.219.181.190}"  # Replace with your actual EC2 IP
EC2_USER="${2:-ubuntu}"           # EC2 username
SSH_KEY_PATH="${3:-~/.ssh/id_rsa}" # Path to SSH private key

echo "🔍 EC2 Instance Connectivity Check"
echo "================================="
echo "Host: $EC2_HOST"
echo "User: $EC2_USER"
echo "SSH Key: $SSH_KEY_PATH"
echo ""

# Function to check command availability
check_command() {
    if command -v "$1" &> /dev/null; then
        echo "✅ $1 is available"
        return 0
    else
        echo "❌ $1 is not available"
        return 1
    fi
}

# Function to check port accessibility
check_port() {
    local host=$1
    local port=$2
    local service=$3
    
    echo -n "Checking $service ($host:$port)... "
    if timeout 10 bash -c "</dev/tcp/$host/$port" 2>/dev/null; then
        echo "✅ Accessible"
        return 0
    else
        echo "❌ Not accessible"
        return 1
    fi
}

# Check local prerequisites
echo "🔧 Checking Local Prerequisites:"
check_command ssh
check_command curl
check_command docker
echo ""

# Network connectivity tests
echo "🌐 Network Connectivity Tests:"
check_port "$EC2_HOST" "22" "SSH"
check_port "$EC2_HOST" "80" "HTTP"
check_port "$EC2_HOST" "443" "HTTPS"
check_port "$EC2_HOST" "8081" "UPS Backend"
check_port "$EC2_HOST" "3000" "UPS Frontend"
echo ""

# SSH Key validation
echo "🔐 SSH Key Validation:"
if [[ -f "$SSH_KEY_PATH" ]]; then
    echo "✅ SSH key exists at: $SSH_KEY_PATH"
    
    # Check key permissions
    key_perms=$(stat -c "%a" "$SSH_KEY_PATH" 2>/dev/null || stat -f "%A" "$SSH_KEY_PATH" 2>/dev/null || echo "unknown")
    if [[ "$key_perms" == "600" ]] || [[ "$key_perms" == "0600" ]]; then
        echo "✅ SSH key has correct permissions ($key_perms)"
    else
        echo "⚠️  SSH key permissions: $key_perms (should be 600)"
        echo "   Fix with: chmod 600 $SSH_KEY_PATH"
    fi
    
    # Check if key is encrypted
    if grep -q "ENCRYPTED" "$SSH_KEY_PATH" 2>/dev/null; then
        echo "⚠️  SSH key is encrypted (may require passphrase)"
    else
        echo "✅ SSH key is not encrypted"
    fi
else
    echo "❌ SSH key not found at: $SSH_KEY_PATH"
    echo "   Generate one with: ssh-keygen -t rsa -b 4096 -f $SSH_KEY_PATH"
fi
echo ""

# Test SSH connection
echo "🔌 SSH Connection Test:"
echo "Attempting to connect to $EC2_USER@$EC2_HOST..."

if timeout 30 ssh -i "$SSH_KEY_PATH" \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o LogLevel=ERROR \
    "$EC2_USER@$EC2_HOST" "echo 'SSH connection successful'" 2>/dev/null; then
    echo "✅ SSH connection successful"
else
    echo "❌ SSH connection failed"
    echo ""
    echo "🔧 Troubleshooting SSH Issues:"
    echo "1. Check if EC2 instance is running in AWS Console"
    echo "2. Verify Security Group allows SSH (port 22) from your IP"
    echo "3. Confirm the SSH key pair is correctly configured"
    echo "4. Try connecting manually: ssh -i $SSH_KEY_PATH $EC2_USER@$EC2_HOST"
    echo ""
fi

# If SSH works, check EC2 instance status
if timeout 10 ssh -i "$SSH_KEY_PATH" \
    -o ConnectTimeout=5 \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o LogLevel=ERROR \
    "$EC2_USER@$EC2_HOST" "echo 'test'" &>/dev/null; then
    
    echo ""
    echo "📋 EC2 Instance Status:"
    
    # Get system info
    ssh -i "$SSH_KEY_PATH" \
        -o StrictHostKeyChecking=no \
        -o UserKnownHostsFile=/dev/null \
        -o LogLevel=ERROR \
        "$EC2_USER@$EC2_HOST" '
        echo "OS: $(uname -a)"
        echo "Uptime: $(uptime)"
        echo "Memory: $(free -h | head -2 | tail -1)"
        echo "Disk: $(df -h / | tail -1)"
        echo ""
        
        echo "Docker Status:"
        if command -v docker &> /dev/null; then
            echo "✅ Docker is installed: $(docker --version)"
            if systemctl is-active --quiet docker; then
                echo "✅ Docker service is running"
            else
                echo "❌ Docker service is not running"
            fi
        else
            echo "❌ Docker is not installed"
        fi
        
        echo ""
        echo "Running Containers:"
        if command -v docker &> /dev/null && systemctl is-active --quiet docker; then
            docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No containers running"
        else
            echo "Cannot check - Docker not available"
        fi
        
        echo ""
        echo "Project Directory:"
        if [[ -d "/home/'$EC2_USER'/mini-ups" ]]; then
            echo "✅ Project directory exists"
            echo "Files: $(ls -la /home/'$EC2_USER'/mini-ups | wc -l) items"
            if [[ -d "/home/'$EC2_USER'/mini-ups/.git" ]]; then
                echo "✅ Git repository present"
                cd /home/'$EC2_USER'/mini-ups
                echo "Current branch: $(git branch --show-current 2>/dev/null || echo 'unknown')"
                echo "Last commit: $(git log -1 --pretty=format:"%h %s" 2>/dev/null || echo 'unknown')"
            else
                echo "❌ No git repository"
            fi
        else
            echo "❌ Project directory missing"
        fi
    '
fi

echo ""
echo "🎯 Deployment Readiness Checklist:"
echo ""

# Checklist items
checklist_items=(
    "EC2 instance is running and accessible via SSH"
    "Docker is installed and running on EC2"
    "Security groups allow inbound traffic on ports 22, 80, 443, 3000, 8081"
    "SSH key is properly configured in GitHub Secrets"
    "EC2_HOST, EC2_USER, EC2_SSH_KEY secrets are set in GitHub"
    "Project directory exists on EC2 (/home/ubuntu/mini-ups)"
    "Git is available on EC2 for repository cloning"
    "Sufficient disk space (>2GB free) on EC2"
    "Sufficient memory (t2.micro has 1GB RAM)"
    "GitHub Container Registry authentication is working"
)

echo "Manual verification needed:"
for i in "${!checklist_items[@]}"; do
    echo "$((i+1)). ${checklist_items[i]}"
done

echo ""
echo "🚀 If all checks pass, try deploying with:"
echo "1. Manual deployment: cd /home/ubuntu/mini-ups && ./scripts/debug-deployment.sh"
echo "2. GitHub Actions: Trigger 'Deploy to Staging' workflow"
echo ""
echo "📞 Support Commands:"
echo "- SSH into instance: ssh -i $SSH_KEY_PATH $EC2_USER@$EC2_HOST"
echo "- Copy files to instance: scp -i $SSH_KEY_PATH file.txt $EC2_USER@$EC2_HOST:/home/$EC2_USER/"
echo "- Run deployment debug: ssh -i $SSH_KEY_PATH $EC2_USER@$EC2_HOST 'cd mini-ups && ./scripts/debug-deployment.sh'"
echo ""
echo "✨ Connectivity check complete!"