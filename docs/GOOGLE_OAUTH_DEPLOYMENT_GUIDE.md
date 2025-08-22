# Google OAuth 集成部署指南

## 概述

本指南介绍如何为Mini-UPS系统部署Google OAuth 2.0集成功能。集成后，用户可以使用Google账户快速登录，无需单独注册。

## 前置条件

- Java 17+
- Maven 3.6+
- PostgreSQL 数据库
- Google Cloud Platform账户
- 运行中的Mini-UPS系统

## 部署步骤

### 1. Google Cloud Console配置

#### 1.1 创建OAuth2客户端ID

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 选择或创建项目
3. 启用Google+ API和Google Identity API
4. 导航到 **APIs & Services** > **Credentials**
5. 点击 **Create Credentials** > **OAuth client ID**
6. 选择 **Web application**
7. 配置重定向URI：

**开发环境：**
```
http://localhost:8081/login/oauth2/code/google
```

**生产环境：**
```
https://your-domain.com/api/login/oauth2/code/google
```

8. 记录下生成的 `Client ID` 和 `Client Secret`

#### 1.2 配置OAuth同意屏幕

1. 导航到 **OAuth consent screen**
2. 选择 **External** 用户类型
3. 填写必要信息：
   - App name: "Mini-UPS"
   - User support email: 你的邮箱
   - Developer contact information: 你的邮箱
4. 添加scopes: `openid`, `profile`, `email`

### 2. 数据库迁移

执行数据库迁移以支持OAuth2用户：

```sql
-- 添加OAuth2支持字段
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 添加唯一约束和索引
ALTER TABLE users ADD CONSTRAINT uk_users_provider_id UNIQUE (auth_provider, provider_id);
CREATE INDEX idx_users_auth_provider ON users(auth_provider);
CREATE INDEX idx_users_provider_id ON users(provider_id) WHERE provider_id IS NOT NULL;

-- 更新现有用户数据
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
```

### 3. 环境变量配置

#### 3.1 开发环境

在 `backend/.env` 或环境变量中设置：

```bash
# Google OAuth2配置
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# 前端重定向URL
OAUTH2_REDIRECT_URI=http://localhost:3000/auth/callback
OAUTH2_ERROR_REDIRECT_URI=http://localhost:3000/login
OAUTH2_FAILURE_REDIRECT_URI=http://localhost:3000/login
```

#### 3.2 生产环境

```bash
# Google OAuth2配置
GOOGLE_CLIENT_ID=your-production-google-client-id
GOOGLE_CLIENT_SECRET=your-production-google-client-secret

# 前端重定向URL
OAUTH2_REDIRECT_URI=https://your-domain.com/auth/callback
OAUTH2_ERROR_REDIRECT_URI=https://your-domain.com/login
OAUTH2_FAILURE_REDIRECT_URI=https://your-domain.com/login
```

### 4. 应用配置验证

确认 `application.yml` 包含以下配置：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid,profile,email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"

app:
  oauth2:
    redirect-uri: ${OAUTH2_REDIRECT_URI:http://localhost:3000/auth/callback}
    error-redirect-uri: ${OAUTH2_ERROR_REDIRECT_URI:http://localhost:3000/login}
    failure-redirect-uri: ${OAUTH2_FAILURE_REDIRECT_URI:http://localhost:3000/login}
```

### 5. 编译和部署

```bash
# 编译项目
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn clean compile

# 运行测试
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn test -Dspring.profiles.active=test

# 启动应用
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 6. 前端集成

在前端应用中添加Google登录按钮：

```html
<!-- 简单的链接方式 -->
<a href="/oauth2/authorization/google" class="google-login-btn">
  使用Google登录
</a>
```

#### 6.1 处理OAuth2回调

创建 `/auth/callback` 页面处理OAuth2成功回调：

```javascript
// /auth/callback 页面
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
const error = urlParams.get('error');

if (token) {
    // 保存JWT令牌
    localStorage.setItem('authToken', token);
    
    // 重定向到主页面
    window.location.href = '/dashboard';
} else if (error) {
    // 显示错误信息
    const message = urlParams.get('message') || 'OAuth2登录失败';
    alert(decodeURIComponent(message));
    
    // 重定向到登录页面
    window.location.href = '/login';
}
```

## 验证部署

### 1. 基本功能测试

1. 访问应用登录页面
2. 点击"使用Google登录"按钮
3. 完成Google授权流程
4. 验证是否成功登录并获得JWT令牌

### 2. 数据库验证

检查新用户是否正确创建：

```sql
SELECT 
    username, 
    email, 
    auth_provider, 
    provider_id, 
    first_name, 
    last_name 
FROM users 
WHERE auth_provider = 'GOOGLE';
```

### 3. 日志检查

检查应用日志确认OAuth2流程：

```bash
# 检查成功日志
grep "OAuth2 authentication completed successfully" logs/mini-ups.log

# 检查错误日志
grep "OAuth2 authentication failed" logs/mini-ups.log
```

## 故障排除

### 常见问题

#### 1. "redirect_uri_mismatch" 错误

**原因**：Google Console中配置的重定向URI与实际请求的URI不匹配

**解决方案**：
- 检查Google Console中的重定向URI配置
- 确认环境变量 `OAUTH2_REDIRECT_URI` 设置正确
- 注意URI的完整性（包括协议、域名、端口）

#### 2. "invalid_client" 错误

**原因**：Client ID或Client Secret配置错误

**解决方案**：
- 验证环境变量 `GOOGLE_CLIENT_ID` 和 `GOOGLE_CLIENT_SECRET`
- 确认Google Console中的凭据是否正确

#### 3. 用户创建失败

**原因**：数据库约束冲突或缺少必要字段

**解决方案**：
- 检查数据库迁移是否正确执行
- 验证users表结构包含新字段
- 检查应用日志中的详细错误信息

#### 4. JWT令牌无法生成

**原因**：用户信息不完整或JWT配置问题

**解决方案**：
- 检查 `JWT_SECRET` 环境变量设置
- 验证OAuth2用户信息是否包含必要字段
- 检查JwtTokenProvider配置

### 调试技巧

#### 1. 启用详细日志

在 `application.yml` 中添加：

```yaml
logging:
  level:
    com.miniups.security: DEBUG
    com.miniups.service.AuthService: DEBUG
    org.springframework.security.oauth2: DEBUG
```

#### 2. 测试OAuth2端点

```bash
# 测试授权端点
curl -i "http://localhost:8081/oauth2/authorization/google"

# 应该返回302重定向到Google
```

#### 3. 检查安全配置

确认SecurityConfig中OAuth2端点已允许访问：

```java
.requestMatchers("/oauth2/**", "/login/oauth2/code/*").permitAll()
```

## 安全考虑

### 1. 客户端密钥管理

- 使用环境变量管理敏感信息
- 生产环境使用密钥管理服务（如AWS Secrets Manager）
- 定期轮换客户端密钥

### 2. 重定向URI安全

- 使用HTTPS在生产环境
- 限制重定向URI到可信域名
- 避免使用通配符重定向URI

### 3. 用户数据保护

- OAuth2用户的密码字段设为null
- 实施适当的账户关联验证
- 记录OAuth2相关的安全事件

## 监控和维护

### 1. 监控指标

- OAuth2登录成功率
- 新用户注册转化率
- 账户关联操作频率
- OAuth2相关错误率

### 2. 定期维护

- 检查Google OAuth2配额使用情况
- 更新OAuth2相关依赖
- 审查OAuth2安全配置
- 备份OAuth2用户数据

## 扩展功能

### 1. 添加其他OAuth2提供商

可以轻松添加GitHub、Facebook等其他提供商：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
```

### 2. 账户关联功能

实现安全的账户关联流程，允许用户将多个OAuth2提供商关联到同一账户。

### 3. 单点注销

实现OAuth2提供商的单点注销功能，提升安全性。

---

## 总结

Google OAuth2集成为Mini-UPS系统提供了便捷的社交登录功能，提升了用户体验。通过本指南的步骤，你应该能够成功部署和配置OAuth2功能。

如遇到问题，请参考故障排除部分或查看应用日志获取更多信息。