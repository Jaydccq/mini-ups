# 实施计划

## 阶段一：Docker统一编排 (预计2-3小时)

- [ ] 1.1 创建统一的docker-compose.yml文件
  - 整合三个服务的Docker配置
  - 配置统一网络 `mini-ups-network`
  - 设置服务间依赖关系和启动顺序
  - _需求: 需求1.1, 需求1.4_

- [ ] 1.2 配置数据库端口隔离
  - UPS PostgreSQL: 5431端口
  - Amazon PostgreSQL: 15432端口  
  - World Simulator PostgreSQL: 5433端口
  - _需求: 需求1.1_

- [ ] 1.3 修改World Simulator的Docker配置
  - 更新Dockerfile以支持统一网络
  - 配置wait-for-it脚本确保数据库就绪
  - 验证TCP端口12345和23456的暴露
  - _需求: 需求1.1, 需求1.3_

- [ ] 1.4 修改Amazon服务的网络配置
  - 移除对外部projectnet的依赖
  - 更新环境变量指向World Simulator容器
  - 配置与UPS服务的REST API通信
  - _需求: 需求1.2_

- [ ] 1.5 更新UPS服务配置
  - 修改application-docker.yml配置
  - 设置WORLD_SIMULATOR_HOST和AMAZON_API_URL
  - 确保健康检查和依赖管理正确
  - _需求: 需求1.1, 需求1.2_

- [ ] 1.6 创建环境变量配置文件
  - 创建.env.production文件
  - 配置数据库密码和JWT密钥
  - 设置服务间通信参数
  - _需求: 需求6.3_

## 阶段二：GitHub Actions CI/CD流水线 (预计3-4小时)

- [ ] 2.1 创建主要工作流文件 (.github/workflows/deploy.yml)
  - 设置Java 17和Node.js环境
  - 配置条件触发 (main分支推送)
  - 添加并行任务支持
  - _需求: 需求2.1, 需求2.2_

- [ ] 2.2 配置后端测试阶段
  - 设置Maven缓存优化
  - 运行Spring Boot测试套件
  - 生成测试覆盖率报告
  - _需求: 需求2.1_

- [ ] 2.3 配置前端构建和测试
  - 设置Node.js依赖缓存
  - 运行TypeScript类型检查
  - 执行Vitest单元测试
  - 构建生产版本验证
  - _需求: 需求2.2_

- [ ] 2.4 设置Docker镜像构建和推送
  - 配置Docker Buildx for ARM64
  - 构建多服务Docker镜像
  - 推送到Docker Hub (配置secrets)
  - 镜像标签管理 (latest + git-sha)
  - _需求: 需求2.4_

- [ ] 2.5 配置集成测试阶段
  - 在GitHub Actions环境启动服务栈
  - 运行跨服务API测试
  - 验证服务间TCP通信
  - _需求: 需求2.3_

- [ ] 2.6 设置部署到EC2阶段
  - 配置SSH密钥 (GitHub Secrets)
  - 实现远程部署脚本
  - 添加部署后健康检查
  - _需求: 需求3.1_

## 阶段三：AWS基础设施准备 (预计1-2小时)

- [ ] 3.1 创建AWS EC2实例
  - 选择t4g.micro (ARM64架构)
  - 配置Ubuntu 22.04 LTS AMI
  - 设置8GB gp3存储
  - 创建SSH密钥对
  - _需求: 需求3.2, 需求6.1_

- [ ] 3.2 配置Security Group
  - SSH: 端口22 (限制IP)
  - HTTP: 端口3000, 8080, 8081
  - TCP: 端口12345, 23456 (World Simulator)
  - 其他管理端口: 5672, 15672 (可选)
  - _需求: 需求3.2, 需求3.3_

- [ ] 3.3 分配Elastic IP
  - 创建Elastic IP地址
  - 关联到EC2实例
  - 记录公网IP用于配置
  - _需求: 需求3.2_

- [ ] 3.4 准备EC2环境
  - SSH连接并更新系统
  - 安装Docker Engine和Docker Compose
  - 创建应用目录结构
  - 配置防火墙规则 (ufw)
  - _需求: 需求3.1_

## 阶段四：部署脚本和配置 (预计2-3小时)

- [ ] 4.1 创建部署脚本 (scripts/deploy-aws.sh)
  - 实现远程代码拉取
  - Docker镜像更新逻辑
  - 服务滚动重启
  - 健康检查验证
  - _需求: 需求3.1, 需求3.4_

- [ ] 4.2 创建服务管理脚本
  - start-production.sh: 生产环境启动
  - logs-production.sh: 日志查看
  - backup-production.sh: 数据备份
  - health-check.sh: 健康状态检查
  - _需求: 需求5.1, 需求5.3_

- [ ] 4.3 配置日志管理
  - 创建日志目录结构 (/var/log/mini-ups/)
  - 配置logrotate自动清理
  - 设置Docker容器日志驱动
  - _需求: 需求5.1, 需求5.2_

- [ ] 4.4 创建监控脚本
  - 系统资源监控 (CPU, 内存, 磁盘)
  - 服务健康状态检查
  - 数据库连接测试
  - API端点响应测试
  - _需求: 需求5.4_

- [ ] 4.5 设置备份恢复机制
  - 数据库自动备份脚本
  - 应用配置文件备份
  - 恢复流程文档
  - _需求: 需求6.3_

## 阶段五：端到端测试验证 (预计2小时)

- [ ] 5.1 部署验证测试
  - 验证所有服务正常启动
  - 检查服务健康检查状态
  - 测试Web界面访问
  - _需求: 需求3.4_

- [ ] 5.2 Amazon→UPS订单流程测试
  - 在Amazon Web界面创建测试订单
  - 验证UPS系统接收订单
  - 检查数据库记录创建
  - _需求: 需求4.1_

- [ ] 5.3 UPS→World Simulator通信测试
  - 测试车辆信息获取
  - 验证TCP Socket连接
  - 检查实时位置更新
  - _需求: 需求4.2, 需求4.3_

- [ ] 5.4 完整业务流程测试
  - 端到端订单处理流程
  - 跨系统状态同步验证
  - 用户界面功能测试
  - _需求: 需求4.4_

- [ ] 5.5 性能和稳定性测试
  - 负载测试 (并发订单创建)
  - 服务重启恢复测试
  - 长时间运行稳定性测试
  - _需求: 需求1.4_

## 阶段六：文档和交付 (预计1小时)

- [ ] 6.1 更新部署文档
  - 完整部署指南
  - 故障排除文档
  - 维护操作手册
  - _需求: All_

- [ ] 6.2 创建监控仪表板
  - 服务状态概览页面
  - 关键指标展示
  - 快速问题定位工具
  - _需求: 需求5.4_

- [ ] 6.3 成本优化验证
  - AWS费用预估验证
  - 资源使用优化建议
  - 监控设置确保不超预算
  - _需求: 需求6.1, 需求6.2, 需求6.4_

## 关键配置文件清单

需要创建/修改的主要文件：

- `docker-compose.unified.yml` - 统一服务编排
- `.env.production` - 生产环境变量
- `.github/workflows/deploy.yml` - CI/CD流水线
- `scripts/deploy-aws.sh` - AWS部署脚本
- `scripts/health-check.sh` - 健康检查脚本
- `scripts/logs-production.sh` - 日志查看脚本
- `docs/DEPLOYMENT_AWS.md` - AWS部署指南

## 预计完成时间

- **总时长**: 11-15小时
- **关键路径**: Docker编排 → GitHub Actions → AWS部署 → 测试验证
- **并行任务**: AWS基础设施准备可与脚本开发并行进行
- **里程碑**: 每个阶段完成后进行验证测试

## 风险点和应急预案

- **Docker网络问题**: 准备网络调试工具和文档
- **AWS连接问题**: 备用SSH密钥和连接方式
- **端口冲突**: 端口映射清单和冲突解决方案
- **内存不足**: 监控脚本和资源优化配置