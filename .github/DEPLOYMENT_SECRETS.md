# GitHub Actions Deployment Secrets Configuration

This document outlines all the secrets required for the Mini-UPS CI/CD pipeline to function properly.

## 🔐 Required Secrets

### Repository Settings
Navigate to your GitHub repository → Settings → Secrets and variables → Actions

### Core Secrets

#### **GitHub Token** (Auto-configured)
- **Name**: `GITHUB_TOKEN`
- **Description**: Automatically provided by GitHub Actions
- **Usage**: Docker image registry authentication, repository access
- **Value**: Auto-generated (no configuration needed)

#### **Container Registry** (Optional)
- **Name**: `CONTAINER_REGISTRY`
- **Description**: Custom container registry URL (defaults to ghcr.io)
- **Usage**: Docker image storage
- **Example**: `ghcr.io` or `your-registry.com`

### EC2 Deployment Secrets

#### **Staging Environment**
- **Name**: `STAGING_HOST`
- **Description**: IP address or hostname of staging EC2 instance
- **Value**: `44.219.181.190` (your current EC2 IP)
- **Example**: `staging.yourcompany.com`

- **Name**: `STAGING_USER`
- **Description**: SSH username for staging server
- **Value**: `ec2-user`
- **Example**: `ubuntu` or `ec2-user`

- **Name**: `STAGING_SSH_KEY`
- **Description**: Private SSH key for staging server access
- **Value**: Contents of your `mini-ups-key.pem` file
- **Format**: 
  ```
  -----BEGIN RSA PRIVATE KEY-----
  MIIEpAIBAAKCAQEA...
  ...
  -----END RSA PRIVATE KEY-----
  ```

#### **Production Environment**
- **Name**: `PRODUCTION_HOST`
- **Description**: IP address or hostname of production EC2 instance
- **Value**: Your production server IP
- **Example**: `prod.yourcompany.com`

- **Name**: `PRODUCTION_USER`
- **Description**: SSH username for production server
- **Value**: `ec2-user`
- **Example**: `ubuntu` or `ec2-user`

- **Name**: `PRODUCTION_SSH_KEY`
- **Description**: Private SSH key for production server access
- **Value**: Contents of your production server SSH key
- **Format**: Same as staging SSH key

### Database Secrets (Production)

- **Name**: `DB_PASSWORD`
- **Description**: Production database password
- **Value**: Strong password for production PostgreSQL
- **Example**: `SuperSecurePassword123!`

- **Name**: `GRAFANA_PASSWORD`
- **Description**: Grafana admin password for monitoring
- **Value**: Admin password for Grafana dashboard
- **Example**: `AdminPassword456!`

### AWS Secrets (Optional - for advanced deployment)

- **Name**: `AWS_ACCESS_KEY_ID`
- **Description**: AWS access key for S3, ECR, etc.
- **Usage**: Alternative container registry, backups
- **Value**: Your AWS access key

- **Name**: `AWS_SECRET_ACCESS_KEY`
- **Description**: AWS secret access key
- **Usage**: Pair with AWS_ACCESS_KEY_ID
- **Value**: Your AWS secret key

- **Name**: `AWS_REGION`
- **Description**: AWS region for resources
- **Value**: `us-east-1`
- **Default**: `us-east-1`

## 🚀 Quick Setup Guide

### 1. Prepare SSH Key
```bash
# On your local machine where you have the EC2 key
cat ~/Downloads/mini-ups-key.pem
# Copy the entire output including BEGIN/END lines
```

### 2. Add Secrets to GitHub
1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret from the list above

### 3. Environment-Specific Secrets

#### Staging Secrets
```bash
STAGING_HOST=44.219.181.190
STAGING_USER=ec2-user
STAGING_SSH_KEY=[paste your mini-ups-key.pem content]
```

#### Production Secrets (when ready)
```bash
PRODUCTION_HOST=[your-production-ip]
PRODUCTION_USER=ec2-user
PRODUCTION_SSH_KEY=[your-production-ssh-key]
DB_PASSWORD=YourStrongProductionPassword
```

## 🔧 Environment Configuration

### GitHub Environments
The workflow uses GitHub Environments for approval gates:

1. **staging** - Auto-deploy from main branch
2. **production** - Manual approval required

#### Setting up Environments:
1. Go to **Settings** → **Environments**
2. Create `staging` environment
3. Create `production` environment
4. For production, add **Required reviewers** (yourself)

### SSH Key Security
- Use separate SSH keys for staging and production
- Regularly rotate SSH keys
- Limit SSH key permissions on EC2 instances
- Consider using AWS Systems Manager Session Manager for enhanced security

## 🧪 Testing the Pipeline

### Manual Deployment
```bash
# Trigger staging deployment
git push origin main

# Trigger production deployment
# Go to Actions tab → Run workflow → Select "production"
```

### Local Testing
```bash
# Test SSH connection
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# Test Docker commands on EC2
docker compose ps
docker compose logs
```

## 🔍 Troubleshooting

### Common Issues

#### SSH Connection Failed
```
Permission denied (publickey)
```
**Solution**: Check SSH key format in GitHub secrets, ensure no extra spaces/characters

#### Docker Command Failed
```
docker: command not found
```
**Solution**: Install Docker on EC2 instance
```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user
```

#### Health Check Failed
```
curl: (7) Failed to connect
```
**Solution**: Check application startup logs
```bash
docker compose logs backend
docker compose logs frontend
```

### Debug Commands for EC2

```bash
# Check running containers
docker compose ps

# View application logs
docker compose logs -f backend
docker compose logs -f frontend

# Check system resources
free -h
df -h
docker system df

# Network connectivity
curl -I http://localhost:8081/api/health
curl -I http://localhost:3000
```

## 🔒 Security Best Practices

1. **Rotate Secrets Regularly**
   - Change SSH keys every 90 days
   - Update database passwords quarterly
   - Monitor access logs

2. **Principle of Least Privilege**
   - GitHub secrets only accessible to necessary workflows
   - EC2 security groups allow only required ports
   - Database users have minimal required permissions

3. **Monitoring and Alerting**
   - Set up CloudWatch alarms for EC2 instances
   - Monitor failed deployment attempts
   - Track SSH access logs

4. **Backup Strategy**
   - Regular database backups
   - Docker image versioning
   - Configuration file backups

## 📚 Additional Resources

- [GitHub Actions Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [EC2 Security Best Practices](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security.html)
- [Docker Compose Production Deployment](https://docs.docker.com/compose/production/)

---

## 🎯 Next Steps

1. Add all required secrets to GitHub repository
2. Set up GitHub environments with approval gates
3. Test deployment to staging
4. When ready, deploy to production with manual approval
5. Set up monitoring and alerting for production environment

Remember to replace placeholder values with your actual configuration!