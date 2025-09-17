# 🚀 Prometheus监控系统快速上手指南

## 一键安装（推荐新手）

```bash
# 1. 进入项目根目录
cd /Users/hongxichen/Desktop/mini-ups

# 2. 运行一键安装脚本
./monitoring/setup-monitoring.sh

# 3. 等待安装完成，访问监控界面
```

## 📊 监控地址速查

| 服务 | 地址 | 用户名/密码 | 功能 |
|-----|------|------------|------|
| **Grafana** | http://localhost:3001 | admin/admin123 | 可视化仪表板 |
| **Prometheus** | http://localhost:9090 | 无需登录 | 指标查询和告警 |
| **AlertManager** | http://localhost:9093 | 无需登录 | 告警管理 |
| **Node Exporter** | http://localhost:9100 | 无需登录 | 系统指标 |
| **cAdvisor** | http://localhost:8080 | 无需登录 | 容器指标 |

## 🎯 快速验证

### 1. 检查数据收集
```bash
# 验证Prometheus能收集到Mini-UPS数据
curl "http://localhost:9090/api/v1/query?query=up{job='mini-ups-backend'}"

# 应该返回: {"status":"success","data":{"result":[{"value":[时间戳,"1"]}]}}
```

### 2. 查看关键指标
```bash
# HTTP请求总数
curl "http://localhost:9090/api/v1/query?query=http_server_requests_total"

# JVM内存使用
curl "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes"

# 业务指标 - 订单创建数
curl "http://localhost:9090/api/v1/query?query=miniups_shipments_created_total"
```

## 📈 使用Grafana仪表板

### 1. 登录Grafana
- 访问: http://localhost:3001
- 用户名: `admin`
- 密码: `admin123`

### 2. 查看预制仪表板
- 左侧菜单 → **Dashboards** → **Browse**
- 找到 **"Mini-UPS监控仪表板"**
- 点击查看实时监控数据

### 3. 仪表板功能
- **HTTP请求速率**: 实时API调用频率
- **响应时间分位数**: P50/P95/P99响应时间
- **应用状态**: 服务是否正常运行
- **业务指标**: 订单、WebSocket、消息队列状态
- **JVM监控**: 内存使用情况
- **追踪号段**: 剩余ID数量监控

## 🔔 告警配置

### 已配置的告警规则

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| **应用停止运行** | 服务无响应 | Critical | 1分钟 |
| **高错误率** | 错误率>5% | Warning | 2分钟 |
| **响应时间过长** | P95>500ms | Warning | 3分钟 |
| **内存使用过高** | JVM堆内存>80% | Warning | 5分钟 |
| **追踪号段不足** | 剩余<1000个 | Critical | 1分钟 |
| **WebSocket连接过多** | 连接数>100 | Warning | 2分钟 |

### 查看告警状态
```bash
# 查看当前告警
curl http://localhost:9090/api/v1/alerts

# 查看告警规则
curl http://localhost:9090/api/v1/rules
```

## 🛠 常用操作

### 服务管理
```bash
# 进入监控目录
cd monitoring

# 查看服务状态
docker-compose -f docker-compose.monitoring.yml ps

# 查看日志
docker-compose -f docker-compose.monitoring.yml logs -f

# 重启服务
docker-compose -f docker-compose.monitoring.yml restart

# 停止服务
docker-compose -f docker-compose.monitoring.yml down

# 清理数据重新开始
docker-compose -f docker-compose.monitoring.yml down -v
```

### 配置修改
```bash
# 修改Prometheus配置后重新加载
curl -X POST http://localhost:9090/-/reload

# 修改告警规则后验证语法
docker exec prometheus promtool check rules /etc/prometheus/rules/mini-ups-alerts.yml
```

## 🔍 故障排除

### 问题1: Prometheus无法收集Mini-UPS数据
```bash
# 检查Mini-UPS是否运行
curl http://localhost:8081/actuator/health

# 检查Prometheus配置
curl http://localhost:9090/targets
```

### 问题2: Grafana无法显示数据
1. 确认数据源配置正确 (Configuration → Data Sources)
2. 检查查询语句是否正确
3. 验证时间范围设置

### 问题3: 告警不触发
```bash
# 检查告警规则
curl http://localhost:9090/api/v1/rules

# 查看告警状态
curl http://localhost:9090/api/v1/alerts
```

## 📚 学习资源

### PromQL查询语言
```promql
# 基础查询
http_server_requests_total

# 速率计算 (每秒请求数)
rate(http_server_requests_total[1m])

# 聚合查询
sum(rate(http_server_requests_total[1m])) by (uri)

# 分位数计算
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

### 有用的查询示例
```promql
# 应用是否运行
up{job="mini-ups-backend"}

# 错误率
sum(rate(http_server_requests_total{status!~"2.."}[5m])) / sum(rate(http_server_requests_total[5m]))

# 内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# 业务指标
miniups_shipments_created_total
miniups_websocket_connections_active
leaf_segment_remaining{biz_tag="tracking_number"}
```

## 🎯 下一步优化

1. **自定义仪表板**: 根据业务需求创建专属监控面板
2. **告警优化**: 调整阈值，减少误报
3. **通知集成**: 配置邮件/Slack/企业微信通知
4. **长期存储**: 配置数据备份和归档策略
5. **安全加固**: 添加认证和访问控制

---

## 🆘 获取帮助

- 📖 完整指南: `docs/monitoring/PROMETHEUS_BEGINNER_GUIDE.md`
- 🛠 配置文件: `monitoring/` 目录下所有配置
- 📊 仪表板: `docs/monitoring/grafana-dashboard-mini-ups.json`
- 🐛 问题反馈: 项目Issues

现在你可以开始监控Mini-UPS系统了！ 🎉