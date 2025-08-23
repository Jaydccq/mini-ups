# GitHub Actions EC2 安全组配置指南

## 问题
GitHub Actions无法通过SSH连接到EC2实例，因为安全组阻止了连接。

## 解决方案

### 方法1: 允许所有IP访问SSH（推荐用于开发环境）

#### 步骤1: 登录AWS Console
1. 登录 [AWS Console](https://console.aws.amazon.com/)
2. 进入 **EC2** 服务
3. 在左侧菜单选择 **Security Groups**

#### 步骤2: 找到你的安全组
1. 找到你EC2实例使用的安全组
2. 点击安全组ID进入详情页面

#### 步骤3: 编辑入站规则
点击 **Inbound rules** 标签页，然后点击 **Edit inbound rules**

添加以下规则：

| 类型 | 协议 | 端口范围 | 源 | 描述 |
|------|------|----------|-----|------|
| SSH | TCP | 22 | 0.0.0.0/0 | GitHub Actions SSH Access |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP Access |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS Access |
| Custom TCP | TCP | 3000 | 0.0.0.0/0 | UPS Frontend |
| Custom TCP | TCP | 8081 | 0.0.0.0/0 | UPS Backend API |
| Custom TCP | TCP | 5432 | 0.0.0.0/0 | PostgreSQL (如果需要外部访问) |
| Custom TCP | TCP | 6380 | 0.0.0.0/0 | Redis (如果需要外部访问) |

#### 步骤4: 保存规则
点击 **Save rules** 保存配置

---

### 方法2: 只允许GitHub Actions IP范围（更安全）

#### 获取GitHub Actions IP范围
```bash
# 获取GitHub的IP地址范围
curl -s https://api.github.com/meta | jq -r '.actions[]'
```

#### 当前GitHub Actions IP范围（2025年）：
- 13.64.0.0/16
- 13.65.0.0/16
- 13.68.0.0/16
- 13.69.0.0/16
- 13.70.0.0/16
- 13.71.0.0/16
- 13.72.0.0/16
- 13.73.0.0/16
- 13.74.0.0/16
- 13.75.0.0/16
- 更多...

> **注意**: GitHub的IP范围会定期更新，建议使用API获取最新范围

#### 安全组配置（限制IP范围）
对于SSH端口22，为每个GitHub IP范围创建单独的规则：

| 类型 | 协议 | 端口范围 | 源 | 描述 |
|------|------|----------|-----|------|
| SSH | TCP | 22 | 13.64.0.0/16 | GitHub Actions 1 |
| SSH | TCP | 22 | 13.65.0.0/16 | GitHub Actions 2 |
| SSH | TCP | 22 | 13.68.0.0/16 | GitHub Actions 3 |
| ... | ... | ... | ... | ... |
| SSH | TCP | 22 | YOUR_LOCAL_IP/32 | Your Local Access |

---

### 方法3: 使用AWS Systems Manager (最安全)

如果你想避免直接SSH访问，可以使用AWS Systems Manager Session Manager：

#### 步骤1: 为EC2实例添加IAM角色
1. 创建IAM角色，附加 `AmazonSSMManagedInstanceCore` 策略
2. 将角色附加到EC2实例

#### 步骤2: 修改GitHub Actions工作流
```yaml
- name: Connect via Session Manager
  run: |
    # 安装Session Manager plugin
    curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" -o "session-manager-plugin.deb"
    sudo dpkg -i session-manager-plugin.deb
    
    # 使用Session Manager执行命令
    aws ssm start-session --target ${{ secrets.EC2_INSTANCE_ID }}
```

---

## 推荐配置（用于开发/测试环境）

### 安全组入站规则：
```
规则 1: SSH
- 类型: SSH
- 协议: TCP
- 端口: 22
- 源: 0.0.0.0/0
- 描述: GitHub Actions and Development Access

规则 2: HTTP
- 类型: HTTP
- 协议: TCP
- 端口: 80
- 源: 0.0.0.0/0
- 描述: HTTP Access

规则 3: HTTPS
- 类型: HTTPS
- 协议: TCP
- 端口: 443
- 源: 0.0.0.0/0
- 描述: HTTPS Access

规则 4: Frontend
- 类型: Custom TCP
- 协议: TCP
- 端口: 3000
- 源: 0.0.0.0/0
- 描述: UPS Frontend Application

规则 5: Backend API
- 类型: Custom TCP
- 协议: TCP
- 端口: 8081
- 源: 0.0.0.0/0
- 描述: UPS Backend API
```

### 出站规则：
通常保持默认的 "All traffic" 出站规则，或者添加：
```
规则 1: All Outbound
- 类型: All Traffic
- 协议: All
- 端口: All
- 目标: 0.0.0.0/0
- 描述: Allow all outbound traffic
```

---

## 验证配置

### 1. 测试SSH连接
```bash
# 从本地测试
ssh -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 测试端口连通性
nc -zv 44.219.181.190 22
nc -zv 44.219.181.190 3000
nc -zv 44.219.181.190 8081
```

### 2. 使用在线工具测试
- [Port Checker](https://www.portchecktool.com/)
- 输入你的EC2 IP和端口号测试连通性

### 3. 检查EC2实例状态
```bash
# 在AWS Console中确认：
# - 实例状态: running
# - 公共IP: 44.219.181.190
# - 安全组: 正确配置
```

---

## 安全建议

### 生产环境安全措施：
1. **限制SSH访问**：只允许必要的IP地址
2. **使用密钥对**：禁用密码登录
3. **定期更新**：保持系统和软件更新
4. **监控日志**：启用CloudTrail和VPC Flow Logs
5. **最小权限原则**：只开放必要的端口

### 开发环境权衡：
- **便利性** vs **安全性**
- 开发阶段可以使用 0.0.0.0/0
- 生产环境必须限制IP范围

---

## 故障排除

### 常见问题：
1. **连接超时**：检查安全组和网络ACL
2. **权限被拒绝**：检查SSH密钥和用户名
3. **实例不响应**：检查实例状态和系统日志

### 调试命令：
```bash
# 测试端口连通性
telnet 44.219.181.190 22

# 详细SSH调试
ssh -vvv -i ~/Downloads/mini-ups-key.pem ec2-user@44.219.181.190

# 检查安全组（需要AWS CLI）
aws ec2 describe-security-groups --group-ids sg-your-security-group-id
```

---

## 快速修复脚本

```bash
#!/bin/bash
# 快速配置安全组（需要AWS CLI和适当权限）

SECURITY_GROUP_ID="sg-your-security-group-id"  # 替换为你的安全组ID

# 添加SSH规则（所有IP）
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 22 \
    --cidr 0.0.0.0/0

# 添加HTTP规则
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

# 添加HTTPS规则
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0

# 添加Frontend规则
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 3000 \
    --cidr 0.0.0.0/0

# 添加Backend API规则
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 8081 \
    --cidr 0.0.0.0/0

echo "安全组规则配置完成！"
```