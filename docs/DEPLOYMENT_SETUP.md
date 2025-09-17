# Mini-UPS Automatic Deployment Setup

This guide will help you set up automatic deployment for your Mini-UPS project on AWS EC2.

## Prerequisites

1. **EC2 Instance**: Running Amazon Linux 2023
2. **SSH Key**: `~/Downloads/mini-ups-key.pem`
3. **EC2 IP**: `44.219.181.190`
4. **Git Repository**: Your Mini-UPS repository on GitHub

## Quick Setup

### Step 1: Setup Your Git Repository

First, make sure you have pushed your code to a Git repository:

```bash
# If you haven't already, initialize git in your project
cd /Users/hongxichen/Desktop/mini-ups
git add .
git commit -m "Initial commit for auto-deployment"
git push origin main
```

### Step 2: Update Auto-Deploy Script with Your Repository

Edit the auto-deploy script to use your repository:

```bash
# Edit the script
nano scripts/auto-deploy.sh

# Find this line and replace with your actual repository URL:
GIT_REPO="https://github.com/YOUR_USERNAME/mini-ups.git"
```

### Step 3: Run the Setup

```bash
# Make sure you're in the project directory
cd /Users/hongxichen/Desktop/mini-ups

# Setup auto-deployment (replace with your actual git repo URL)
./scripts/auto-deploy.sh -r https://github.com/YOUR_USERNAME/mini-ups.git setup
```

### Step 4: Start Monitoring

```bash
# Start the auto-deployment monitoring
./scripts/auto-deploy.sh start
```

### Step 5: Configure Security Groups (AWS Console)

1. Go to AWS EC2 Console → Security Groups
2. Find your EC2 instance's security group
3. Add the following inbound rules:

| Type | Protocol | Port Range | Source | Description |
|------|----------|------------|---------|------------|
| HTTP | TCP | 3000 | 0.0.0.0/0 | UPS Frontend |
| HTTP | TCP | 8081 | 0.0.0.0/0 | UPS Backend API |
| HTTP | TCP | 8080 | 0.0.0.0/0 | Amazon Service |
| HTTP | TCP | 15672 | 0.0.0.0/0 | RabbitMQ Management (optional) |

## How It Works

1. **Git Monitoring**: The system checks your Git repository every 30 seconds for new commits
2. **Automatic Deployment**: When new commits are detected:
   - Pulls the latest code
   - Stops existing services
   - Rebuilds Docker images
   - Starts services
   - Performs health checks
3. **External Access**: Services are configured to bind to all network interfaces (0.0.0.0)

## Accessing Your Application

Once deployed and security groups are configured, you can access:

- **UPS Frontend**: http://44.219.181.190:3000
- **UPS Backend API**: http://44.219.181.190:8081
- **Amazon Service**: http://44.219.181.190:8080
- **API Documentation**: http://44.219.181.190:8081/swagger-ui.html
- **RabbitMQ Management**: http://44.219.181.190:15672 (guest/guest)

## Useful Commands

```bash
# Check deployment status
./scripts/auto-deploy.sh status

# View deployment logs
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190 'tail -f auto-deploy.log'

# Manual deployment
./scripts/auto-deploy.sh manual

# Stop auto-deployment
./scripts/auto-deploy.sh stop

# Start auto-deployment
./scripts/auto-deploy.sh start
```

## Development Workflow

1. **Make Changes**: Edit your code locally
2. **Commit & Push**: 
   ```bash
   git add .
   git commit -m "Your changes"
   git push origin main
   ```
3. **Automatic Deployment**: The system will automatically detect changes and redeploy
4. **Verify**: Check http://44.219.181.190:3000 in your browser

## Monitoring

- **Service Status**: `./scripts/auto-deploy.sh status`
- **Application Logs**: SSH to server and check `docker-compose logs`
- **Deployment Logs**: `ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190 'tail -f auto-deploy.log'`

## Troubleshooting

### If deployment fails:
1. Check the deployment logs
2. Verify your Git repository URL is correct
3. Ensure your EC2 instance has internet access
4. Check Docker and Docker Compose are installed

### If you can't access from browser:
1. Verify security group rules are correctly configured
2. Check if services are running: `./scripts/auto-deploy.sh status`
3. Test local connectivity: `curl http://44.219.181.190:3000`

## Security Notes

- Change default passwords in `.env.production`
- Use strong JWT secrets
- Consider using HTTPS in production
- Regularly update dependencies and system packages

## Next Steps

1. **SSL/HTTPS**: Set up SSL certificates for secure connections
2. **Domain Name**: Configure a custom domain name
3. **Monitoring**: Add application performance monitoring
4. **Backup**: Set up automated database backups