# GitHub Actions CI/CD 完全教程 - 从零开始

## 📚 目录

1. [什么是CI/CD](#什么是cicd)
2. [GitHub Actions基础概念](#github-actions基础概念)
3. [YAML语法快速入门](#yaml语法快速入门)
4. [创建第一个工作流程](#创建第一个工作流程)
5. [Mini-UPS项目CI/CD详解](#mini-ups项目cicd详解)
6. [环境和密钥配置](#环境和密钥配置)
7. [常见工作流程模式](#常见工作流程模式)
8. [调试和故障排除](#调试和故障排除)
9. [最佳实践和优化](#最佳实践和优化)
10. [进阶功能](#进阶功能)

---

## 1. 什么是CI/CD

### CI/CD基本概念

**CI (Continuous Integration) 持续集成**
- 开发者频繁地将代码变更合并到主分支
- 每次合并都会触发自动化测试
- 目标：尽早发现和修复问题

**CD (Continuous Delivery/Deployment) 持续交付/部署**
- **持续交付**：代码变更自动构建、测试，准备好部署
- **持续部署**：代码变更自动部署到生产环境

### 为什么需要CI/CD？

```
传统开发流程：
开发 → 手动测试 → 手动构建 → 手动部署 → 出现问题 → 回滚 → 修复

CI/CD流程：
开发 → 自动测试 → 自动构建 → 自动部署 → 自动监控 → 快速反馈
```

**优势：**
- ✅ 减少人为错误
- ✅ 提高部署频率
- ✅ 缩短反馈周期
- ✅ 提高代码质量
- ✅ 降低部署风险

---

## 2. GitHub Actions基础概念

### 核心组件

```
Repository (仓库)
    └── .github/workflows/
        └── ci-cd.yml (工作流程文件)
            ├── Workflow (工作流程)
            │   ├── Event (触发事件)
            │   └── Jobs (任务)
            │       ├── Job 1
            │       │   └── Steps (步骤)
            │       │       ├── Step 1
            │       │       └── Step 2
            │       └── Job 2
            └── Actions (动作)
```

### 关键术语

**Workflow (工作流程)**
- 一个自动化过程，由一个或多个job组成
- 由YAML文件定义，存储在`.github/workflows/`目录

**Event (事件)**
- 触发workflow的特定活动
- 例如：push、pull_request、schedule等

**Job (任务)**
- workflow中的一组steps
- 可以并行或顺序执行

**Step (步骤)**
- job中的单个任务
- 可以运行action或shell命令

**Action (动作)**
- 可重用的代码单元
- 可以是官方的、第三方的或自定义的

**Runner (运行器)**
- 执行workflow的服务器
- GitHub提供托管的runner，也可以自托管

---

## 3. YAML语法快速入门

### 基本语法

```yaml
# 注释使用井号
key: value                    # 键值对
string_value: "Hello World"   # 字符串
number_value: 42             # 数字
boolean_value: true          # 布尔值

# 列表
list:
  - item1
  - item2
  - item3

# 或者内联格式
inline_list: [item1, item2, item3]

# 对象
object:
  nested_key: nested_value
  another_key: another_value

# 多行字符串
multiline_string: |
  这是一个
  多行字符串
  保持换行符

# 折叠字符串
folded_string: >
  这是一个
  折叠的字符串
  换行符会被替换为空格
```

### GitHub Actions特定语法

```yaml
name: 工作流程名称

# 触发条件
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

# 环境变量
env:
  NODE_VERSION: '18'

# 任务
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: 检出代码
        uses: actions/checkout@v4
      
      - name: 运行命令
        run: echo "Hello World"
```

---

## 4. 创建第一个工作流程

### 步骤1：创建工作流程文件

在你的仓库中创建文件：`.github/workflows/hello-world.yml`

```yaml
name: Hello World CI

# 触发条件：当推送到main分支时
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

# 任务
jobs:
  hello:
    # 运行环境
    runs-on: ubuntu-latest
    
    # 步骤
    steps:
    # 步骤1：检出代码
    - name: 检出代码
      uses: actions/checkout@v4
    
    # 步骤2：运行Hello World
    - name: 运行Hello World
      run: |
        echo "Hello, World!"
        echo "当前目录: $(pwd)"
        echo "文件列表:"
        ls -la
    
    # 步骤3：显示系统信息
    - name: 显示系统信息
      run: |
        echo "操作系统: $(uname -a)"
        echo "CPU信息: $(nproc) cores"
        echo "内存信息:"
        free -h
```

### 步骤2：提交并推送

```bash
git add .github/workflows/hello-world.yml
git commit -m "添加Hello World工作流程"
git push origin main
```

### 步骤3：查看结果

1. 进入GitHub仓库页面
2. 点击"Actions"标签
3. 查看工作流程运行状态

---

## 5. Mini-UPS项目CI/CD详解

### 整体架构

```
CI/CD Pipeline 流程图：

推送代码到main分支
    ↓
┌─────────────────────────────────┐
│         触发工作流程              │
└─────────────────────────────────┘
    ↓
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   后端测试      │  │   前端测试      │  │   安全扫描      │
│ - Java测试     │  │ - TypeScript   │  │ - 漏洞扫描      │
│ - Maven构建    │  │ - ESLint       │  │ - 依赖检查      │
│ - 数据库测试    │  │ - 单元测试      │  │               │
└─────────────────┘  └─────────────────┘  └─────────────────┘
    ↓                    ↓                    ↓
┌─────────────────────────────────────────────────────────────┐
│                    Docker镜像构建                           │
│ - 后端镜像 (Spring Boot)                                   │
│ - 前端镜像 (React + Nginx)                                │
└─────────────────────────────────────────────────────────────┘
    ↓
┌─────────────────┐         ┌─────────────────┐
│   部署到Staging │   →     │   部署到生产环境  │
│ - 健康检查      │         │ - 备份策略       │
│ - 烟雾测试      │         │ - 零停机部署      │
└─────────────────┘         └─────────────────┘
```

### 工作流程文件结构详解

我们的`ci-cd.yml`文件包含以下主要部分：

#### 1. 触发条件和环境变量

```yaml
name: Mini-UPS CI/CD Pipeline

# 触发条件
on:
  push:
    branches: [ main ]          # 推送到main分支时触发
  pull_request:
    branches: [ main ]          # 创建PR到main分支时触发
  workflow_dispatch:            # 手动触发
    inputs:
      deploy_environment:       # 选择部署环境
        description: 'Environment to deploy to'
        required: true
        default: 'staging'
        type: choice
        options:
        - staging
        - production

# 全局环境变量
env:
  JAVA_VERSION: '17'           # Java版本
  NODE_VERSION: '20'           # Node.js版本
  DOCKER_BUILDKIT: 1          # 启用Docker BuildKit
  AWS_REGION: us-east-1       # AWS区域
```

**解释：**
- `workflow_dispatch`允许手动触发部署
- 环境变量在所有job中可用
- `choice`类型输入提供下拉选择

#### 2. 后端测试任务 (backend-test)

```yaml
backend-test:
  name: Backend Tests
  runs-on: ubuntu-latest
  
  # 服务容器 - 在job运行时启动的Docker容器
  services:
    postgres:
      image: postgres:15
      env:
        POSTGRES_PASSWORD: abc123
        POSTGRES_USER: postgres
        POSTGRES_DB: ups_db_test
      options: >-
        --health-cmd pg_isready
        --health-interval 10s
        --health-timeout 5s
        --health-retries 5
      ports:
        - 5432:5432
    
    redis:
      image: redis:7-alpine
      options: >-
        --health-cmd "redis-cli ping"
        --health-interval 10s
        --health-timeout 5s
        --health-retries 5
      ports:
        - 6379:6379
```

**关键点：**
- `services`定义测试所需的数据库和缓存
- `health-cmd`确保服务启动完成后再运行测试
- 端口映射使runner可以访问服务

#### 3. 步骤详解

**Java环境设置：**
```yaml
- name: Set up JDK ${{ env.JAVA_VERSION }}
  uses: actions/setup-java@v4
  with:
    java-version: ${{ env.JAVA_VERSION }}
    distribution: 'temurin'        # Eclipse Temurin JDK发行版
```

**依赖缓存：**
```yaml
- name: Cache Maven dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2                    # Maven本地仓库路径
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    restore-keys: ${{ runner.os }}-m2
```

**解释：**
- 缓存基于`pom.xml`文件的哈希值
- 加速构建过程，避免重复下载依赖

**运行测试：**
```yaml
- name: Run backend tests
  working-directory: ./backend
  run: |
    mvn clean test \
      -Dspring.profiles.active=test \
      -DfailIfNoTests=false \
      -Dmaven.test.failure.ignore=false \
      --batch-mode \
      --fail-at-end \
      --show-version
  env:
    TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/ups_db_test
    MAVEN_OPTS: >-
      -Xmx2048m
      -XX:+UseG1GC
      --add-opens java.base/java.lang=ALL-UNNAMED
```

**关键参数：**
- `--batch-mode`: 非交互模式，适合CI环境
- `--fail-at-end`: 运行所有测试，最后报告失败
- `MAVEN_OPTS`: JVM参数，解决Java 17兼容性问题

#### 4. 前端测试任务

```yaml
frontend-test:
  name: Frontend Tests
  runs-on: ubuntu-latest

  steps:
  - name: Set up Node.js ${{ env.NODE_VERSION }}
    uses: actions/setup-node@v4
    with:
      node-version: ${{ env.NODE_VERSION }}

  - name: Install frontend dependencies
    working-directory: ./frontend
    run: |
      npm ci --legacy-peer-deps --prefer-offline --no-audit

  - name: Run frontend tests
    working-directory: ./frontend
    run: npm run test
    env:
      CI: true                     # 启用CI模式
      NODE_ENV: test              # 设置为测试环境
```

**npm ci vs npm install：**
- `npm ci`更适合CI环境
- 基于`package-lock.json`安装精确版本
- 更快，更可靠

#### 5. Docker构建和推送

```yaml
docker-build:
  name: Build and Push Docker Images
  runs-on: ubuntu-latest
  needs: [backend-test, frontend-test, security-scan]  # 依赖其他任务成功
  if: (github.ref == 'refs/heads/main' || github.event_name == 'workflow_dispatch')
  
  permissions:
    contents: read
    packages: write               # 需要写入GitHub Packages权限

  steps:
  - name: Log in to Container Registry
    uses: docker/login-action@v3
    with:
      registry: ghcr.io           # GitHub Container Registry
      username: ${{ github.actor }}
      password: ${{ secrets.GITHUB_TOKEN }}

  - name: Build and push backend image
    uses: docker/build-push-action@v5
    with:
      context: ./backend
      push: true
      tags: ${{ steps.meta-backend.outputs.tags }}
      cache-from: type=gha        # 使用GitHub Actions缓存
      cache-to: type=gha,mode=max
```

**镜像标签策略：**
```yaml
tags: |
  type=ref,event=branch          # 分支名作为标签
  type=ref,event=pr             # PR号作为标签
  type=sha,prefix=sha-          # Git SHA作为标签
  type=raw,value=latest,enable={{is_default_branch}}  # main分支标记为latest
```

#### 6. 部署任务

```yaml
deploy-staging:
  name: Deploy to Staging
  runs-on: ubuntu-latest
  needs: [docker-build]
  environment: staging            # 使用GitHub环境

  steps:
  - name: Deploy to EC2 via SSH
    uses: appleboy/ssh-action@v1.0.3
    with:
      host: ${{ secrets.STAGING_HOST }}
      username: ${{ secrets.STAGING_USER }}
      key: ${{ secrets.STAGING_SSH_KEY }}
      script: |
        cd /home/ec2-user/mini-ups
        
        # 使用构建好的镜像部署
        echo "BACKEND_IMAGE=${{ needs.docker-build.outputs.backend-image }}" >> .env.staging
        echo "FRONTEND_IMAGE=${{ needs.docker-build.outputs.frontend-image }}" >> .env.staging
        
        # Docker Compose部署
        docker compose --env-file .env.staging down
        docker compose --env-file .env.staging pull
        docker compose --env-file .env.staging up -d
        
        # 健康检查
        for i in {1..20}; do
          if curl -f http://localhost:8081/api/health; then
            echo "✅ 部署成功!"
            break
          fi
          sleep 5
        done
```

---

## 6. 环境和密钥配置

### GitHub Secrets配置

#### 步骤1：添加Repository Secrets

1. 进入GitHub仓库设置
2. 点击"Secrets and variables" → "Actions"
3. 点击"New repository secret"

**常用密钥：**

```bash
# 服务器连接
STAGING_HOST=your-staging-server.com
STAGING_USER=ec2-user
STAGING_SSH_KEY=-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----

PRODUCTION_HOST=your-production-server.com
PRODUCTION_USER=ec2-user
PRODUCTION_SSH_KEY=-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----

# 数据库密码
POSTGRES_PASSWORD=your-secure-password
REDIS_PASSWORD=your-redis-password

# JWT密钥
JWT_SECRET=your-very-long-and-secure-jwt-secret-key

# 云服务
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
```

#### 步骤2：配置Environment

1. 进入仓库设置 → "Environments"
2. 创建`staging`和`production`环境
3. 为生产环境添加保护规则：
   - Required reviewers (需要审核者)
   - Wait timer (等待时间)
   - Deployment branches (限制分支)

### 环境变量文件

**项目根目录创建环境配置：**

`.env.ci` (CI环境配置):
```bash
# CI环境配置
ENVIRONMENT=ci
SPRING_PROFILES_ACTIVE=test

# 数据库配置
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=ups_db_test
POSTGRES_USER=postgres
POSTGRES_PASSWORD=abc123

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379

# 应用配置
API_BASE_URL=http://localhost:8081
WS_BASE_URL=ws://localhost:8081
```

`.env.staging` (Staging环境配置):
```bash
# Staging环境配置
ENVIRONMENT=staging
SPRING_PROFILES_ACTIVE=staging

# 数据库配置
POSTGRES_HOST=staging-db.internal
POSTGRES_PORT=5432
POSTGRES_DB=ups_db_staging

# Redis配置
REDIS_HOST=staging-redis.internal
REDIS_PORT=6379

# 应用配置
API_BASE_URL=https://staging-api.your-domain.com
WS_BASE_URL=wss://staging-api.your-domain.com
```

---

## 7. 常见工作流程模式

### 模式1：简单的测试和构建

```yaml
name: Simple CI

on:
  push:
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '18'
    - run: npm ci
    - run: npm test
    - run: npm run build
```

### 模式2：多环境矩阵测试

```yaml
name: Matrix Testing

on: [push, pull_request]

jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node-version: [16, 18, 20]
        
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: ${{ matrix.node-version }}
    - run: npm ci
    - run: npm test
```

### 模式3：条件部署

```yaml
name: Conditional Deployment

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - run: npm ci && npm test

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
    - name: Deploy to production
      run: echo "部署到生产环境"
```

### 模式4：并行任务

```yaml
name: Parallel Jobs

on: [push]

jobs:
  frontend:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Test frontend
      run: echo "前端测试"

  backend:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Test backend
      run: echo "后端测试"

  integration:
    needs: [frontend, backend]  # 等待前端和后端任务完成
    runs-on: ubuntu-latest
    steps:
    - name: Integration tests
      run: echo "集成测试"
```

### 模式5：定时任务

```yaml
name: Scheduled Tasks

on:
  schedule:
    - cron: '0 2 * * *'        # 每天凌晨2点运行
    - cron: '0 8 * * 1'        # 每周一上午8点运行

jobs:
  backup:
    runs-on: ubuntu-latest
    steps:
    - name: 数据库备份
      run: echo "执行数据库备份"
  
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - name: 安全扫描
      run: echo "执行安全扫描"
```

---

## 8. 调试和故障排除

### 常见问题和解决方案

#### 问题1：测试失败

**症状：**
```
Error: Tests failed with exit code 1
```

**解决步骤：**

1. **查看详细日志：**
```yaml
- name: Run tests with verbose output
  run: |
    npm test -- --verbose
    # 或者对于Maven
    mvn test -X
```

2. **保存测试报告：**
```yaml
- name: Upload test results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: test-results
    path: |
      target/surefire-reports/
      coverage/
```

3. **本地复现：**
```bash
# 使用相同的环境变量
export CI=true
export NODE_ENV=test
npm test
```

#### 问题2：依赖安装失败

**症状：**
```
npm ERR! network request to https://registry.npmjs.org/... failed
```

**解决方案：**

1. **使用缓存：**
```yaml
- name: Cache dependencies
  uses: actions/cache@v4
  with:
    path: |
      ~/.npm
      ~/.m2
    key: ${{ runner.os }}-deps-${{ hashFiles('**/package-lock.json', '**/pom.xml') }}
```

2. **重试机制：**
```yaml
- name: Install with retry
  run: |
    for i in {1..3}; do
      npm ci && break
      echo "重试 $i/3..."
      sleep 5
    done
```

#### 问题3：Docker构建失败

**症状：**
```
ERROR: failed to solve: process "/bin/sh -c npm ci" didn't complete successfully
```

**调试步骤：**

1. **启用构建日志：**
```yaml
- name: Build with debug
  uses: docker/build-push-action@v5
  with:
    context: .
    push: false
    tags: debug:latest
    outputs: type=docker
    progress: plain        # 显示详细构建日志
```

2. **多阶段构建优化：**
```dockerfile
# 使用多阶段构建减小镜像大小
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:18-alpine AS production
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

#### 问题4：部署失败

**症状：**
```
SSH connection failed
```

**解决步骤：**

1. **验证SSH密钥：**
```yaml
- name: Test SSH connection
  run: |
    echo "${{ secrets.STAGING_SSH_KEY }}" > key.pem
    chmod 600 key.pem
    ssh -i key.pem -o StrictHostKeyChecking=no ${{ secrets.STAGING_USER }}@${{ secrets.STAGING_HOST }} "echo 'SSH连接成功'"
```

2. **分步部署：**
```yaml
- name: Deploy step by step
  run: |
    # 1. 检查服务器状态
    ssh user@host "docker --version"
    
    # 2. 拉取镜像
    ssh user@host "docker pull $IMAGE_NAME"
    
    # 3. 停止旧服务
    ssh user@host "docker compose down" || true
    
    # 4. 启动新服务
    ssh user@host "docker compose up -d"
    
    # 5. 健康检查
    ssh user@host "curl -f http://localhost:8080/health"
```

### 调试技巧

#### 1. 启用Debug日志

```yaml
- name: Enable debug logging
  run: |
    echo "ACTIONS_STEP_DEBUG=true" >> $GITHUB_ENV
    echo "ACTIONS_RUNNER_DEBUG=true" >> $GITHUB_ENV
```

#### 2. 使用tmate进行远程调试

```yaml
- name: Setup tmate session
  if: failure()        # 仅在失败时启动
  uses: mxschmitt/action-tmate@v3
  timeout-minutes: 30
```

#### 3. 条件执行调试步骤

```yaml
- name: Debug on failure
  if: failure()
  run: |
    echo "=== 系统信息 ==="
    uname -a
    echo "=== 环境变量 ==="
    env | sort
    echo "=== 磁盘空间 ==="
    df -h
    echo "=== 进程列表 ==="
    ps aux
```

#### 4. 输出变量检查

```yaml
- name: Check variables
  run: |
    echo "Event name: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "SHA: ${{ github.sha }}"
    echo "Actor: ${{ github.actor }}"
    echo "Repository: ${{ github.repository }}"
```

---

## 9. 最佳实践和优化

### 性能优化

#### 1. 依赖缓存策略

```yaml
# 多层缓存
- name: Cache dependencies
  uses: actions/cache@v4
  with:
    path: |
      ~/.npm
      ~/.m2
      ~/.cache/pip
      ~/.cargo
    key: ${{ runner.os }}-deps-${{ hashFiles('**/package-lock.json', '**/pom.xml', '**/requirements.txt', '**/Cargo.toml') }}
    restore-keys: |
      ${{ runner.os }}-deps-
      ${{ runner.os }}-
```

#### 2. 并行执行优化

```yaml
jobs:
  # 快速失败策略
  quick-check:
    runs-on: ubuntu-latest
    steps:
    - name: 语法检查
      run: npm run lint
  
  # 并行测试
  test:
    needs: quick-check    # 语法检查通过后再运行测试
    strategy:
      fail-fast: false   # 不要因为一个失败就停止所有测试
      matrix:
        test-suite: [unit, integration, e2e]
    runs-on: ubuntu-latest
    steps:
    - name: Run ${{ matrix.test-suite }} tests
      run: npm run test:${{ matrix.test-suite }}
```

#### 3. Docker构建优化

```yaml
- name: Build with cache
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: ${{ env.IMAGE_NAME }}:latest
    cache-from: |
      type=gha
      type=registry,ref=${{ env.IMAGE_NAME }}:cache
    cache-to: |
      type=gha,mode=max
      type=registry,ref=${{ env.IMAGE_NAME }}:cache,mode=max
    build-args: |
      BUILDKIT_INLINE_CACHE=1
```

### 安全最佳实践

#### 1. 密钥管理

```yaml
# ❌ 错误做法
- name: Deploy
  run: |
    echo "password123" | ssh user@server

# ✅ 正确做法
- name: Deploy
  env:
    SSH_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
  run: |
    echo "$SSH_KEY" > key.pem
    chmod 600 key.pem
    ssh -i key.pem user@server "DB_PASSWORD='$DB_PASSWORD' docker compose up -d"
    rm key.pem
```

#### 2. 权限最小化

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read      # 只读代码
      packages: write     # 写入包registry
      # 不给不必要的权限
```

#### 3. 输出清理

```yaml
- name: Deploy with clean output
  run: |
    # 部署脚本
    deploy.sh 2>&1 | sed 's/password=[^[:space:]]*/password=***/g'
```

### 可维护性

#### 1. 复用配置

**创建可复用的Action：**

`.github/actions/setup-environment/action.yml`:
```yaml
name: 'Setup Environment'
description: 'Setup Node.js and cache dependencies'
inputs:
  node-version:
    description: 'Node.js version'
    required: false
    default: '18'

runs:
  using: 'composite'
  steps:
  - uses: actions/setup-node@v4
    with:
      node-version: ${{ inputs.node-version }}
  - uses: actions/cache@v4
    with:
      path: ~/.npm
      key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
```

**使用复用Action：**
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: ./.github/actions/setup-environment
      with:
        node-version: '20'
```

#### 2. 工作流程模板

**创建模板：** `.github/workflow-templates/ci.yml`
```yaml
name: CI Template

on:
  push:
    branches: [ $default-branch ]
  pull_request:
    branches: [ $default-branch ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Run tests
      run: |
        # 添加你的测试命令
        echo "运行测试"
```

#### 3. 环境配置管理

**使用environment文件：**
```yaml
- name: Load environment
  run: |
    case "${{ github.ref }}" in
      refs/heads/main)
        cp .env.production .env
        ;;
      refs/heads/staging)
        cp .env.staging .env
        ;;
      *)
        cp .env.development .env
        ;;
    esac
```

### 监控和通知

#### 1. 状态检查

```yaml
- name: Health check with retry
  run: |
    max_attempts=30
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
      if curl -f http://localhost:8080/health; then
        echo "✅ 应用健康检查通过"
        exit 0
      fi
      
      echo "⏳ 健康检查失败，重试 $attempt/$max_attempts"
      sleep 10
      attempt=$((attempt + 1))
    done
    
    echo "❌ 健康检查失败"
    exit 1
```

#### 2. 通知集成

```yaml
- name: Notify on failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: failure
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
    message: |
      🚨 部署失败!
      分支: ${{ github.ref }}
      提交: ${{ github.sha }}
      作者: ${{ github.actor }}
```

---

## 10. 进阶功能

### 自定义Actions

#### 创建JavaScript Action

**action.yml:**
```yaml
name: 'Custom Deploy Action'
description: 'Deploy application with custom logic'
inputs:
  environment:
    description: 'Deployment environment'
    required: true
  api-key:
    description: 'API key for deployment'
    required: true

runs:
  using: 'node20'
  main: 'index.js'
```

**index.js:**
```javascript
const core = require('@actions/core');
const github = require('@actions/github');

async function run() {
  try {
    const environment = core.getInput('environment');
    const apiKey = core.getInput('api-key');
    
    console.log(`部署到环境: ${environment}`);
    
    // 部署逻辑
    await deployToEnvironment(environment, apiKey);
    
    core.setOutput('deployment-url', `https://${environment}.example.com`);
  } catch (error) {
    core.setFailed(error.message);
  }
}

async function deployToEnvironment(env, key) {
  // 实际部署逻辑
  console.log('部署完成');
}

run();
```

### 工作流程调用

#### 可复用工作流程

**.github/workflows/reusable-deploy.yml:**
```yaml
name: Reusable Deploy

on:
  workflow_call:
    inputs:
      environment:
        required: true
        type: string
      version:
        required: true
        type: string
    secrets:
      deploy-key:
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}
    steps:
    - name: Deploy version ${{ inputs.version }}
      env:
        DEPLOY_KEY: ${{ secrets.deploy-key }}
      run: |
        echo "部署版本 ${{ inputs.version }} 到 ${{ inputs.environment }}"
```

**调用可复用工作流程：**
```yaml
name: Main Workflow

on: [push]

jobs:
  deploy-staging:
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: staging
      version: ${{ github.sha }}
    secrets:
      deploy-key: ${{ secrets.STAGING_DEPLOY_KEY }}
```

### 条件逻辑和表达式

#### 复杂条件

```yaml
jobs:
  deploy:
    if: |
      github.event_name == 'push' &&
      (
        github.ref == 'refs/heads/main' ||
        startsWith(github.ref, 'refs/heads/release/')
      ) &&
      !contains(github.event.head_commit.message, '[skip deploy]')
```

#### 动态矩阵

```yaml
jobs:
  generate-matrix:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
    steps:
    - id: set-matrix
      run: |
        if [ "${{ github.ref }}" == "refs/heads/main" ]; then
          echo "matrix={\"env\":[\"staging\",\"production\"]}" >> $GITHUB_OUTPUT
        else
          echo "matrix={\"env\":[\"development\"]}" >> $GITHUB_OUTPUT
        fi

  deploy:
    needs: generate-matrix
    strategy:
      matrix: ${{ fromJson(needs.generate-matrix.outputs.matrix) }}
    runs-on: ubuntu-latest
    steps:
    - name: Deploy to ${{ matrix.env }}
      run: echo "部署到 ${{ matrix.env }}"
```

### 高级调试

#### 工作流程可视化

```yaml
- name: Generate workflow diagram
  run: |
    echo "```mermaid" > workflow-diagram.md
    echo "graph TD" >> workflow-diagram.md
    echo "  A[Checkout] --> B[Test]" >> workflow-diagram.md
    echo "  B --> C[Build]" >> workflow-diagram.md
    echo "  C --> D[Deploy]" >> workflow-diagram.md
    echo "```" >> workflow-diagram.md
```

#### 性能分析

```yaml
- name: Performance analysis
  run: |
    echo "=== 工作流程开始时间: $(date -Iseconds)" >> performance.log
    echo "=== 可用资源:" >> performance.log
    echo "CPU: $(nproc) cores" >> performance.log
    echo "Memory: $(free -h | grep Mem | awk '{print $2}')" >> performance.log
    echo "Disk: $(df -h / | tail -1 | awk '{print $4}')" >> performance.log
```

---

## 🎯 实践练习

### 练习1：创建基础CI工作流程

创建一个简单的Node.js项目CI工作流程：

1. 在项目中添加`.github/workflows/basic-ci.yml`
2. 实现以下功能：
   - 检出代码
   - 设置Node.js环境
   - 安装依赖
   - 运行测试
   - 构建项目

### 练习2：添加Docker支持

扩展上面的工作流程：

1. 添加Docker镜像构建
2. 推送到GitHub Container Registry
3. 实现镜像缓存优化

### 练习3：实现多环境部署

创建完整的部署流程：

1. 配置staging和production环境
2. 实现条件部署逻辑
3. 添加健康检查和回滚机制

---

## 📚 参考资源

### 官方文档
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [Action Marketplace](https://github.com/marketplace?type=actions)

### 实用工具
- [Actions/checkout](https://github.com/actions/checkout)
- [Actions/setup-node](https://github.com/actions/setup-node)
- [Actions/setup-java](https://github.com/actions/setup-java)
- [Docker/build-push-action](https://github.com/docker/build-push-action)

### 最佳实践
- [Security hardening](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [Performance optimization](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#optimizing-workflows)

---

**恭喜！🎉** 你已经掌握了GitHub Actions CI/CD的完整知识体系。从基础概念到高级功能，你现在可以为任何项目创建强大的自动化工作流程。

记住，CI/CD是一个持续改进的过程。开始时保持简单，然后根据项目需求逐步添加更多功能。