# 🚀 Mini-UPS EC2 部署检查清单

## ✅ 代码完成状态

### 1. Docker 配置文件
- [x] `docker-compose.yml` - 完整系统 (包含 Amazon + World Simulator)
- [x] `docker-compose.t2micro.yml` - 精简版 (仅 UPS 服务，针对 1GB 内存优化)
- [x] `.env.t2micro` - 环境变量模板
- [x] `nginx/nginx.conf` - Nginx 反向代理配置 (可选)

### 2. CI/CD 配置
- [x] `.github/workflows/ci-cd.yml` - 完整 CI/CD 流水线
- [x] 支持自动构建 Docker 镜像
- [x] 推送到 GitHub Container Registry (ghcr.io)
- [x] 自动部署到 EC2
- [x] 健康检查和烟雾测试
- [x] 使用你现有的 GitHub Secrets

### 3. 部署脚本
- [x] `scripts/deploy/deploy-to-ec2.sh` - 手动部署脚本
- [x] `EC2_SETUP_GUIDE.md` - 完整设置指南

### 4. 配置文件更新
- [x] 健康检查端点修复为 `/actuator/health`
- [x] 镜像名称使用 ghcr.io 格式
- [x] 环境变量正确引用 GitHub Secrets
- [x] 内存限制优化 (t2.micro 专用)

## 🔧 需要你手动配置的部分

### 1. GitHub 仓库设置
需要在 GitHub 仓库中设置一个 Environment：
1. 去 GitHub Repository → Settings → Environments
2. 创建名为 `staging` 的环境
3. 可选：创建 `production` 环境并设置审批流程

### 2. GitHub Personal Access Token
需要生成用于访问 GitHub Container Registry 的 token：
1. 访问 https://github.com/settings/tokens
2. 生成包含 `read:packages` 权限的 token
3. 在 EC2 中使用此 token 登录 ghcr.io

### 3. 更新配置中的占位符
✅ 已更新配置中的用户名：
- `EC2_SETUP_GUIDE.md` 中的用户名已设置为 "Jaydccq"
- `.env.t2micro` 中的镜像地址已更新为 `ghcr.io/jaydccq/mini-ups/`

## 🚀 部署流程

### 方式1：自动 CI/CD (推荐)
```bash
# 推送到 main 分支触发自动部署
git add .
git commit -m "feat: setup EC2 CI/CD deployment"
git push origin main
```

### 方式2：手动部署 (测试用)
```bash
# 在本地执行
./scripts/deploy/deploy-to-ec2.sh
```

## 📋 EC2 准备步骤

### 1. 连接 EC2 并安装环境
```bash
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 安装 Docker 和 Docker Compose
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 退出并重新登录
exit
```

### 2. 设置项目环境
```bash
# 重新连接
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 验证安装
docker --version
docker-compose --version

# 创建网络
docker network create projectnet || true
```

### 3. 登录 GitHub Container Registry
```bash
# 使用你的 GitHub 用户名和生成的 token
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u Jaydccq --password-stdin
```

## 🌐 访问地址

部署成功后可访问：
- **前端应用**: http://44.219.181.190:3000
- **后端 API**: http://44.219.181.190:8081/api
- **健康检查**: http://44.219.181.190:8081/actuator/health
- **Swagger API 文档**: http://44.219.181.190:8081/swagger-ui.html

## 🔍 故障排查

### 检查部署状态
```bash
# 连接 EC2
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 进入项目目录
cd /home/ec2-user/mini-ups

# 检查容器状态
docker compose -f docker-compose.t2micro.yml ps

# 查看日志
docker compose -f docker-compose.t2micro.yml logs ups-backend
docker compose -f docker-compose.t2micro.yml logs ups-frontend

# 检查系统资源
free -h
docker stats --no-stream
```

### 常见问题
1. **镜像拉取失败** → 检查 GitHub Container Registry 登录
2. **内存不足** → 使用 `docker-compose.t2micro.yml` 而不是完整版
3. **端口占用** → 检查 EC2 安全组是否开放端口 3000, 8081
4. **健康检查失败** → 等待更长时间，Spring Boot 启动需要时间

## 📊 性能监控

### t2.micro 资源使用预期
- **总内存**: 1GB
- **PostgreSQL**: ~200MB
- **Redis**: ~80MB  
- **后端 (Spring Boot)**: ~400MB
- **前端 (Nginx)**: ~100MB
- **系统预留**: ~220MB

### 监控命令
```bash
# 实时资源使用
docker stats

# 内存使用情况
free -h

# 磁盘使用
df -h

# 检查日志
docker compose logs --tail=50 ups-backend
```

## ✅ 最终确认

所有代码和配置已完成，可以开始部署！

1. ✅ CI/CD 流水线配置完整
2. ✅ Docker 配置优化完成
3. ✅ 部署脚本准备就绪
4. ✅ 文档和指南完整
5. ✅ GitHub Secrets 配置正确
6. ✅ 健康检查端点修复
7. ✅ 内存优化适配 t2.micro

**下一步**: 执行 EC2 设置，然后推送代码触发自动部署！
