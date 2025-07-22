# 三服务统一部署需求文档

## 介绍

本需求旨在建立一个完整的Mini-UPS分布式系统部署方案，包含UPS物流服务、Amazon电商模拟服务和World Simulator环境模拟服务的统一编排、CI/CD流水线和AWS云端部署。

## 需求

### 需求1 - 统一Docker服务编排

**用户故事：** 作为开发者，我需要通过单一命令启动所有三个服务（UPS、Amazon、World Simulator），使它们能够相互通信并正常工作。

#### 验收标准

1. While 在项目根目录，when 执行 `docker compose up` 时，the 系统 shall 同时启动UPS服务（Spring Boot + React）、Amazon服务（Flask）和World Simulator服务（C++）。
2. While 服务启动后，when Amazon系统创建订单时，the UPS服务 shall 能够接收到订单并进行处理。  
3. While UPS服务处理订单时，when 需要车辆调度时，the World Simulator shall 提供实时的车辆位置和仓库信息。
4. While 系统运行中，when 任一服务重启时，the 其他服务 shall 保持正常运行且能重新建立连接。

### 需求2 - GitHub Actions CI/CD流水线

**用户故事：** 作为开发者，我需要在代码提交到GitHub后自动进行构建、测试和部署，确保代码质量和快速发布。

#### 验收标准

1. While 代码推送到main分支，when 触发GitHub Actions时，the 系统 shall 自动执行Java 17环境下的后端测试。
2. While 后端测试通过，when 进行前端构建时，the 系统 shall 成功构建React应用并验证TypeScript类型检查。
3. While 构建成功，when 执行集成测试时，the 系统 shall 验证三个服务之间的通信接口。
4. While 所有测试通过，when 部署阶段时，the 系统 shall 自动构建Docker镜像并推送到容器仓库。

### 需求3 - AWS EC2自动化部署

**用户故事：** 作为运维人员，我需要将应用自动部署到AWS EC2实例上，并能通过公网访问各个服务的Web界面。

#### 验收标准

1. While GitHub Actions部署流水线执行，when 连接到AWS EC2时，the 系统 shall 自动拉取最新的Docker镜像并更新服务。
2. While 部署完成后，when 访问公网IP时，the UPS前端界面 shall 可通过 http://公网IP:3000 访问。
3. While 服务运行中，when 访问Amazon服务时，the Amazon界面 shall 可通过 http://公网IP:8080 访问。
4. While 系统部署后，when 查看服务状态时，the 所有服务 shall 处于健康状态并能正常响应健康检查。

### 需求4 - 服务间通信测试

**用户故事：** 作为测试人员，我需要通过Web界面验证三个服务之间的端到端功能，确保整个业务流程正常运行。

#### 验收标准

1. While 在Amazon Web界面，when 用户下单购买商品时，the UPS系统 shall 自动创建对应的运输订单。
2. While UPS创建运输订单，when 系统分配车辆时，the World Simulator shall 返回可用车辆信息和最优路径。
3. While 车辆开始配送，when 包裹状态更新时，the 用户 shall 能在UPS追踪界面看到实时位置信息。
4. While 配送完成，when 订单状态变更时，the Amazon系统 shall 收到通知并更新订单状态为已送达。

### 需求5 - 日志管理和监控

**用户故事：** 作为系统管理员，我需要能够查看各个服务的运行日志，快速定位和解决问题。

#### 验收标准

1. While 系统运行中，when 查看应用日志时，the 每个服务 shall 将日志输出到指定的日志文件中。
2. While 发生错误，when 查看错误日志时，the 系统 shall 记录详细的错误堆栈和上下文信息。
3. While 在AWS EC2上，when 执行日志查看命令时，the 管理员 shall 能够实时查看所有服务的日志输出。
4. While 服务异常，when 查看健康检查时，the 系统 shall 提供各服务的健康状态和性能指标。

### 需求6 - 成本优化部署

**用户故事：** 作为项目负责人，我需要在保证功能完整的前提下，尽可能降低AWS部署成本。

#### 验收标准

1. While 选择AWS资源，when 部署到EC2时，the 系统 shall 使用t4g.micro实例（Free Tier符合条件）。
2. While 配置网络，when 设置VPC时，the 系统 shall 使用单一公共子网避免NAT Gateway费用。
3. While 数据存储，when 使用数据库时，the 系统 shall 优先使用Docker容器化数据库而非RDS服务。
4. While 监控成本，when 部署完成后，the 预计月成本 shall 控制在$10以内（不含数据传输费用）。

## 业务流程示意

```
用户下单(Amazon Web) → 创建UPS订单 → 分配车辆(World Simulator) → 
实时追踪(UPS Web) → 配送完成 → 状态同步(Amazon)
```

## 系统集成点

1. **Amazon ↔ UPS**: HTTP REST API通信 (端口 8080 ↔ 8081)
2. **UPS ↔ World Simulator**: TCP Socket通信 (端口 12345)  
3. **Amazon ↔ World Simulator**: TCP Socket通信 (端口 23456)
4. **前端访问**: UPS React界面 (端口 3000), Amazon Flask界面 (端口 8080)