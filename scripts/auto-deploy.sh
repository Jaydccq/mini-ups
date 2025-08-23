#!/bin/bash

# Mini-UPS Automated Git Deployment Script
# This script sets up automatic deployment when Git changes are detected

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_ENV=${DEPLOY_ENV:-production}

# Default values
EC2_HOST="44.219.181.190"
EC2_USER="ec2-user"
SSH_KEY="$HOME/Downloads/mini-ups-key.pem"
GIT_REPO="https://github.com/YOUR_USERNAME/mini-ups.git"  # Update this with your actual repo
GIT_BRANCH="main"
POLL_INTERVAL=30  # seconds

# Usage function
usage() {
    echo -e "${BLUE}Auto-Deploy Setup for Mini-UPS${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS] COMMAND"
    echo ""
    echo "Commands:"
    echo "  setup       Setup auto-deployment on EC2 server"
    echo "  start       Start auto-deployment monitoring"
    echo "  stop        Stop auto-deployment monitoring"
    echo "  status      Check auto-deployment status"
    echo "  manual      Trigger manual deployment"
    echo ""
    echo "Options:"
    echo "  -h, --host          EC2 host IP (default: 44.219.181.190)"
    echo "  -u, --user          EC2 SSH user (default: ec2-user)"
    echo "  -k, --key           Path to SSH private key (default: ~/Downloads/mini-ups-key.pem)"
    echo "  -r, --repo          Git repository URL"
    echo "  -b, --branch        Git branch to monitor (default: main)"
    echo "  -i, --interval      Polling interval in seconds (default: 30)"
    echo "  --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 setup                                    # Setup with defaults"
    echo "  $0 -r https://github.com/user/repo.git setup   # Setup with custom repo"
    echo "  $0 start                                    # Start monitoring"
    echo "  $0 manual                                   # Manual deployment"
    exit 1
}

# Parse command line arguments
COMMAND=""
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            EC2_HOST="$2"
            shift 2
            ;;
        -u|--user)
            EC2_USER="$2"
            shift 2
            ;;
        -k|--key)
            SSH_KEY="$2"
            shift 2
            ;;
        -r|--repo)
            GIT_REPO="$2"
            shift 2
            ;;
        -b|--branch)
            GIT_BRANCH="$2"
            shift 2
            ;;
        -i|--interval)
            POLL_INTERVAL="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        setup|start|stop|status|manual)
            COMMAND="$1"
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

if [[ -z "$COMMAND" ]]; then
    echo -e "${RED}Error: Command is required${NC}"
    usage
fi

# Function to run SSH commands
ssh_exec() {
    local cmd="$1"
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=30 "$EC2_USER@$EC2_HOST" "$cmd"
}

# Function to copy files via SCP
scp_copy() {
    local src="$1"
    local dst="$2"
    scp -i "$SSH_KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
        -o ConnectTimeout=30 -r "$src" "$EC2_USER@$EC2_HOST:$dst"
}

# Function to log with timestamp
log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# Setup auto-deployment on EC2
setup_auto_deploy() {
    echo -e "${BLUE}🚀 Setting up Auto-Deployment${NC}"
    echo -e "${BLUE}=================================${NC}"
    echo -e "EC2 Host: ${GREEN}$EC2_HOST${NC}"
    echo -e "Git Repo: ${GREEN}$GIT_REPO${NC}"
    echo -e "Branch: ${GREEN}$GIT_BRANCH${NC}"
    echo -e "Poll Interval: ${GREEN}${POLL_INTERVAL}s${NC}"
    echo ""

    # Test connection
    echo -e "${YELLOW}📡 Testing connection...${NC}"
    if ! ssh_exec "echo 'Connection successful'" >/dev/null 2>&1; then
        echo -e "${RED}❌ Failed to connect to EC2 instance${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Connection successful${NC}"

    # Install dependencies on EC2
    echo -e "${YELLOW}📦 Installing dependencies on EC2...${NC}"
    ssh_exec "
        set -e
        
        # Update system
        sudo yum update -y
        
        # Install Docker if not already installed
        if ! command -v docker &> /dev/null; then
            echo '⚙️ Installing Docker...'
            sudo yum install -y docker
            sudo systemctl start docker
            sudo systemctl enable docker
            sudo usermod -aG docker $USER
        fi
        
        # Install Docker Compose if not already installed
        if ! command -v docker-compose &> /dev/null; then
            echo '⚙️ Installing Docker Compose...'
            sudo curl -L \"https://github.com/docker/compose/releases/latest/download/docker-compose-\$(uname -s)-\$(uname -m)\" -o /usr/local/bin/docker-compose
            sudo chmod +x /usr/local/bin/docker-compose
        fi
        
        # Install Git if not already installed
        if ! command -v git &> /dev/null; then
            echo '⚙️ Installing Git...'
            sudo yum install -y git
        fi
        
        echo '✅ Dependencies installed'
    "

    # Setup project directory and clone repository
    echo -e "${YELLOW}📁 Setting up project directory...${NC}"
    ssh_exec "
        set -e
        cd /home/$EC2_USER
        
        # Remove existing directory if it exists
        if [[ -d 'mini-ups' ]]; then
            echo '🗑️ Removing existing project directory...'
            sudo rm -rf mini-ups
        fi
        
        # Clone repository
        echo '📥 Cloning repository...'
        git clone -b $GIT_BRANCH $GIT_REPO mini-ups
        
        cd mini-ups
        
        # Create necessary directories
        mkdir -p logs data backup
        
        # Make scripts executable
        chmod +x scripts/*.sh
        
        echo '✅ Project cloned and configured'
    "

    # Create auto-deployment script on server
    echo -e "${YELLOW}📝 Creating auto-deployment script on server...${NC}"
    ssh_exec "
        cat > /home/$EC2_USER/auto-deploy-daemon.sh << 'EOF'
#!/bin/bash

# Auto-deployment daemon for Mini-UPS
set -e

PROJECT_DIR=\"/home/$EC2_USER/mini-ups\"
LOCK_FILE=\"/tmp/mini-ups-deploy.lock\"
LOG_FILE=\"/home/$EC2_USER/auto-deploy.log\"
GIT_BRANCH=\"$GIT_BRANCH\"
POLL_INTERVAL=$POLL_INTERVAL

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e \"[\$(date '+%Y-%m-%d %H:%M:%S')] \$1\" | tee -a \"\$LOG_FILE\"
}

check_and_deploy() {
    cd \"\$PROJECT_DIR\"
    
    # Fetch latest changes
    git fetch origin \"\$GIT_BRANCH\" >/dev/null 2>&1
    
    # Check if there are new commits
    LOCAL_COMMIT=\$(git rev-parse HEAD)
    REMOTE_COMMIT=\$(git rev-parse origin/\$GIT_BRANCH)
    
    if [[ \"\$LOCAL_COMMIT\" != \"\$REMOTE_COMMIT\" ]]; then
        log \"${YELLOW}📥 New commits detected, starting deployment...${NC}\"
        
        # Create lock file
        echo \"\$\$\" > \"\$LOCK_FILE\"
        
        # Pull latest changes
        log \"${YELLOW}⬇️ Pulling latest changes...${NC}\"
        git pull origin \"\$GIT_BRANCH\"
        
        # Stop existing services
        log \"${YELLOW}⏹️ Stopping existing services...${NC}\"
        docker-compose -f docker-compose.production.yml down || true
        
        # Build and start services
        log \"${YELLOW}🔨 Building and starting services...${NC}\"
        docker-compose -f docker-compose.production.yml up -d --build
        
        # Wait for services to be ready
        log \"${YELLOW}⏳ Waiting for services to be ready...${NC}\"
        sleep 60
        
        # Health check
        if curl -f -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
            log \"${GREEN}✅ Deployment successful! Backend is healthy${NC}\"
        else
            log \"${RED}❌ Deployment may have issues - backend health check failed${NC}\"
        fi
        
        # Remove lock file
        rm -f \"\$LOCK_FILE\"
        
        log \"${GREEN}🎉 Auto-deployment completed${NC}\"
    fi
}

# Main monitoring loop
log \"${GREEN}🚀 Auto-deployment daemon started${NC}\"
log \"${GREEN}📍 Monitoring repository: $GIT_REPO${NC}\"
log \"${GREEN}🌿 Branch: \$GIT_BRANCH${NC}\"
log \"${GREEN}⏱️ Poll interval: \${POLL_INTERVAL}s${NC}\"

while true; do
    # Check if another deployment is running
    if [[ -f \"\$LOCK_FILE\" ]]; then
        PID=\$(cat \"\$LOCK_FILE\")
        if kill -0 \"\$PID\" 2>/dev/null; then
            log \"${YELLOW}⏳ Another deployment is running (PID: \$PID), waiting...${NC}\"
            sleep \"\$POLL_INTERVAL\"
            continue
        else
            # Stale lock file, remove it
            rm -f \"\$LOCK_FILE\"
        fi
    fi
    
    # Check for updates and deploy if needed
    check_and_deploy
    
    # Wait before next check
    sleep \"\$POLL_INTERVAL\"
done
EOF
        
        chmod +x /home/$EC2_USER/auto-deploy-daemon.sh
        echo '✅ Auto-deployment script created'
    "

    # Create systemd service for auto-deployment
    echo -e "${YELLOW}⚙️ Creating systemd service...${NC}"
    ssh_exec "
        sudo tee /etc/systemd/system/mini-ups-auto-deploy.service > /dev/null << EOF
[Unit]
Description=Mini-UPS Auto-Deployment Daemon
After=network.target docker.service
Requires=docker.service

[Service]
Type=simple
User=$EC2_USER
WorkingDirectory=/home/$EC2_USER
ExecStart=/home/$EC2_USER/auto-deploy-daemon.sh
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=mini-ups-auto-deploy

[Install]
WantedBy=multi-user.target
EOF
        
        sudo systemctl daemon-reload
        sudo systemctl enable mini-ups-auto-deploy
        
        echo '✅ Systemd service created and enabled'
    "

    echo -e "${GREEN}✅ Auto-deployment setup completed!${NC}"
    echo ""
    echo -e "${BLUE}🎯 Next Steps:${NC}"
    echo -e "1. Run: ${YELLOW}$0 start${NC} to start monitoring"
    echo -e "2. Make changes to your code and push to ${YELLOW}$GIT_BRANCH${NC} branch"
    echo -e "3. The system will automatically detect changes and redeploy"
    echo ""
    echo -e "${BLUE}📋 Useful Commands:${NC}"
    echo -e "- Check status: ${YELLOW}$0 status${NC}"
    echo -e "- View logs: ${YELLOW}ssh -i $SSH_KEY $EC2_USER@$EC2_HOST 'tail -f auto-deploy.log'${NC}"
    echo -e "- Manual deploy: ${YELLOW}$0 manual${NC}"
}

# Start auto-deployment monitoring
start_monitoring() {
    echo -e "${YELLOW}🚀 Starting auto-deployment monitoring...${NC}"
    
    ssh_exec "
        sudo systemctl start mini-ups-auto-deploy
        sudo systemctl status mini-ups-auto-deploy --no-pager -l
    "
    
    echo -e "${GREEN}✅ Auto-deployment monitoring started${NC}"
    echo -e "${BLUE}📋 Monitor logs with:${NC}"
    echo -e "   ${YELLOW}ssh -i $SSH_KEY $EC2_USER@$EC2_HOST 'tail -f auto-deploy.log'${NC}"
}

# Stop auto-deployment monitoring
stop_monitoring() {
    echo -e "${YELLOW}⏹️ Stopping auto-deployment monitoring...${NC}"
    
    ssh_exec "
        sudo systemctl stop mini-ups-auto-deploy
        sudo systemctl status mini-ups-auto-deploy --no-pager -l
    "
    
    echo -e "${GREEN}✅ Auto-deployment monitoring stopped${NC}"
}

# Check auto-deployment status
check_status() {
    echo -e "${YELLOW}📊 Checking auto-deployment status...${NC}"
    
    ssh_exec "
        echo '=== Auto-Deploy Service Status ==='
        sudo systemctl status mini-ups-auto-deploy --no-pager -l
        echo ''
        echo '=== Recent Auto-Deploy Logs ==='
        tail -20 auto-deploy.log || echo 'No logs available yet'
        echo ''
        echo '=== Docker Services Status ==='
        docker-compose -f mini-ups/docker-compose.production.yml ps
    "
}

# Manual deployment
manual_deploy() {
    echo -e "${YELLOW}🔧 Triggering manual deployment...${NC}"
    
    ssh_exec "
        cd /home/$EC2_USER/mini-ups
        
        echo '⬇️ Pulling latest changes...'
        git pull origin $GIT_BRANCH
        
        echo '⏹️ Stopping services...'
        docker-compose -f docker-compose.production.yml down
        
        echo '🔨 Building and starting services...'
        docker-compose -f docker-compose.production.yml up -d --build
        
        echo '⏳ Waiting for services...'
        sleep 60
        
        echo '🔍 Health check...'
        if curl -f -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
            echo '✅ Manual deployment successful!'
        else
            echo '⚠️ Services started but health check failed'
        fi
        
        echo '📊 Service status:'
        docker-compose -f docker-compose.production.yml ps
    "
}

# Execute command
case $COMMAND in
    setup)
        setup_auto_deploy
        ;;
    start)
        start_monitoring
        ;;
    stop)
        stop_monitoring
        ;;
    status)
        check_status
        ;;
    manual)
        manual_deploy
        ;;
    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}"
        usage
        ;;
esac