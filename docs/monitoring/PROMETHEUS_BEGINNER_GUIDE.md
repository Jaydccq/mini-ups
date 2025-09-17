# Prometheus + Grafana 监控系统完整新手指南

## 🎯 目标
从零开始为Mini-UPS项目配置完整的监控系统，包括Prometheus数据收集、Grafana可视化仪表板和告警配置。

## 📋 准备工作

### 系统要求
- Docker和Docker Compose (推荐)
- 或者本地安装Prometheus和Grafana
- Mini-UPS项目已启动并运行在端口8081

### 预期结果
- ✅ Prometheus收集Mini-UPS应用指标
- ✅ Grafana展示实时监控仪表板
- ✅ 配置基础告警规则
- ✅ 可视化业务指标（订单、追踪、车辆等）

---

## 🐳 方式一：Docker Compose 一键部署（推荐）

### 1. 创建监控配置目录
```bash
mkdir -p monitoring/{prometheus,grafana,alertmanager}
cd monitoring
```

### 2. 创建Prometheus配置文件
```bash
# 创建 prometheus/prometheus.yml
cat > prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s      # 每15秒收集一次数据
  evaluation_interval: 15s  # 每15秒评估告警规则

# 告警规则文件
rule_files:
  - "rules/*.yml"

# 告警管理器配置
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

# 数据源配置
scrape_configs:
  # Mini-UPS应用监控
  - job_name: 'mini-ups-backend'
    static_configs:
      - targets: ['host.docker.internal:8081']  # Mini-UPS后端地址
    metrics_path: '/actuator/prometheus'         # Spring Boot Actuator端点
    scrape_interval: 10s                        # 10秒收集一次

  # Prometheus自我监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node Exporter (系统指标)
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
EOF
```

### 3. 创建告警规则文件
```bash
# 创建 prometheus/rules/mini-ups-alerts.yml
mkdir -p prometheus/rules
cat > prometheus/rules/mini-ups-alerts.yml << 'EOF'
groups:
- name: mini-ups-alerts
  rules:
  # 高错误率告警
  - alert: HighErrorRate
    expr: |
      (
        sum(rate(http_server_requests_total{status!~"2.."}[5m])) /
        sum(rate(http_server_requests_total[5m]))
      ) > 0.05
    for: 2m
    labels:
      severity: warning
      service: mini-ups
    annotations:
      summary: "Mini-UPS错误率过高"
      description: "错误率已达到 {{ $value | humanizePercentage }}，超过5%阈值"

  # 响应时间过长告警
  - alert: HighResponseTime
    expr: |
      histogram_quantile(0.95,
        sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
      ) > 0.5
    for: 3m
    labels:
      severity: warning
      service: mini-ups
    annotations:
      summary: "Mini-UPS响应时间过长"
      description: "95%的请求响应时间超过500ms: {{ $value }}s"

  # 应用程序停止运行
  - alert: ApplicationDown
    expr: up{job="mini-ups-backend"} == 0
    for: 1m
    labels:
      severity: critical
      service: mini-ups
    annotations:
      summary: "Mini-UPS应用程序停止运行"
      description: "Mini-UPS后端服务已停止响应"

  # JVM内存使用过高
  - alert: HighMemoryUsage
    expr: |
      (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8
    for: 5m
    labels:
      severity: warning
      service: mini-ups
    annotations:
      summary: "JVM内存使用率过高"
      description: "JVM堆内存使用率超过80%: {{ $value | humanizePercentage }}"

  # 追踪号段剩余不足 (业务指标)
  - alert: LowTrackingIDSegment
    expr: leaf_segment_remaining{biz_tag="tracking_number"} < 1000
    for: 1m
    labels:
      severity: critical
      service: mini-ups
    annotations:
      summary: "追踪号段剩余不足"
      description: "追踪号段剩余数量仅有 {{ $value }} 个，需要及时补充"

  # WebSocket连接数异常
  - alert: WebSocketConnectionsHigh
    expr: miniups_websocket_connections_active > 100
    for: 2m
    labels:
      severity: warning
      service: mini-ups
    annotations:
      summary: "WebSocket连接数过高"
      description: "当前WebSocket连接数: {{ $value }}，可能存在连接泄漏"
EOF
```

### 4. 创建完整的Docker Compose文件
```bash
# 创建 docker-compose.monitoring.yml
cat > docker-compose.monitoring.yml << 'EOF'
version: '3.8'

services:
  # Prometheus 服务器
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/rules:/etc/prometheus/rules
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    restart: unless-stopped
    networks:
      - monitoring

  # Grafana 可视化
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3001:3000"  # 避免与前端3000端口冲突
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
    restart: unless-stopped
    networks:
      - monitoring

  # 告警管理器
  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
      - '--web.external-url=http://localhost:9093'
    restart: unless-stopped
    networks:
      - monitoring

  # 系统监控 (Node Exporter)
  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    restart: unless-stopped
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:

networks:
  monitoring:
    driver: bridge
EOF
```

### 5. 配置Grafana数据源和仪表板
```bash
# 创建Grafana配置目录
mkdir -p grafana/{provisioning/datasources,provisioning/dashboards,dashboards}

# 自动配置Prometheus数据源
cat > grafana/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

# 自动加载仪表板配置
cat > grafana/provisioning/dashboards/dashboards.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'Mini-UPS Dashboards'
    orgId: 1
    folder: 'Mini-UPS'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF
```

### 6. 创建告警管理器配置
```bash
# 创建 alertmanager/alertmanager.yml
cat > alertmanager/alertmanager.yml << 'EOF'
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@mini-ups.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://127.0.0.1:5001/'

  # 邮件通知配置示例
  - name: 'email-alerts'
    email_configs:
      - to: 'admin@mini-ups.com'
        subject: 'Mini-UPS告警: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          告警: {{ .Annotations.summary }}
          描述: {{ .Annotations.description }}
          时间: {{ .StartsAt }}
          {{ end }}

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'dev', 'instance']
EOF
```

### 7. 启动监控系统
```bash
# 启动所有监控服务
docker-compose -f docker-compose.monitoring.yml up -d

# 检查服务状态
docker-compose -f docker-compose.monitoring.yml ps

# 查看日志
docker-compose -f docker-compose.monitoring.yml logs -f
```

---

## 🌐 访问监控系统

### 服务地址
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin123)
- **AlertManager**: http://localhost:9093
- **Node Exporter**: http://localhost:9100

### 验证连接
```bash
# 检查Prometheus是否能收集到Mini-UPS数据
curl "http://localhost:9090/api/v1/query?query=up{job='mini-ups-backend'}"

# 检查指标是否可用
curl "http://localhost:9090/api/v1/query?query=http_server_requests_total"
```

---

## 📊 创建Grafana仪表板

### 1. 登录Grafana
- 访问 http://localhost:3001
- 用户名: `admin`
- 密码: `admin123`

### 2. 验证数据源
- 左侧菜单 → Configuration → Data Sources
- 确认Prometheus数据源已自动配置
- 点击"Test"按钮验证连接

### 3. 创建Mini-UPS监控仪表板

#### Panel 1: 系统概览
```json
{
  "title": "应用状态概览",
  "type": "stat",
  "targets": [
    {
      "expr": "up{job='mini-ups-backend'}",
      "legendFormat": "应用状态"
    }
  ]
}
```

#### Panel 2: HTTP请求速率
```json
{
  "title": "HTTP请求速率 (请求/秒)",
  "type": "graph",
  "targets": [
    {
      "expr": "sum(rate(http_server_requests_total[1m])) by (uri)",
      "legendFormat": "{{uri}}"
    }
  ]
}
```

#### Panel 3: 响应时间分布
```json
{
  "title": "响应时间分位数",
  "type": "graph",
  "targets": [
    {
      "expr": "histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
      "legendFormat": "P50"
    },
    {
      "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
      "legendFormat": "P95"
    },
    {
      "expr": "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
      "legendFormat": "P99"
    }
  ]
}
```

#### Panel 4: 业务指标
```json
{
  "title": "业务指标",
  "type": "graph",
  "targets": [
    {
      "expr": "miniups_shipments_created_total",
      "legendFormat": "订单创建总数"
    },
    {
      "expr": "miniups_shipments_delivered_total",
      "legendFormat": "订单配送总数"
    },
    {
      "expr": "miniups_websocket_connections_active",
      "legendFormat": "活跃WebSocket连接"
    }
  ]
}
```

### 4. 导入预制仪表板
我们将创建一个完整的JSON仪表板配置，你可以直接导入使用。

---

## 🔔 配置告警

### 1. 在Grafana中设置告警
- Dashboard → Panel → Alert → Create Alert
- 设置查询条件和阈值
- 配置通知渠道

### 2. 测试告警
```bash
# 停止Mini-UPS应用来触发告警
# 在1分钟后应该收到"ApplicationDown"告警

# 查看告警状态
curl http://localhost:9090/api/v1/alerts
```

---

## 🛠 故障排除

### 常见问题

#### 1. Prometheus无法连接到Mini-UPS
```bash
# 检查Mini-UPS是否运行
curl http://localhost:8081/actuator/prometheus

# 检查Docker网络连接
docker exec prometheus wget -qO- http://host.docker.internal:8081/actuator/health
```

#### 2. Grafana无法显示数据
- 确认数据源配置正确
- 检查查询语句语法
- 验证时间范围设置

#### 3. 告警不触发
```bash
# 检查告警规则语法
docker exec prometheus promtool check rules /etc/prometheus/rules/mini-ups-alerts.yml

# 查看告警状态
curl http://localhost:9090/api/v1/rules
```

### 日志查看
```bash
# 查看各服务日志
docker-compose -f docker-compose.monitoring.yml logs prometheus
docker-compose -f docker-compose.monitoring.yml logs grafana
docker-compose -f docker-compose.monitoring.yml logs alertmanager
```

---

## 🎯 下一步

1. **自定义仪表板**: 根据业务需求创建更多监控面板
2. **告警优化**: 调整告警阈值，减少误报
3. **数据保留**: 配置长期数据存储策略
4. **安全配置**: 设置认证和访问控制
5. **性能调优**: 优化查询和存储性能

## 📚 学习资源

- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [PromQL查询语言指南](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [告警规则最佳实践](https://prometheus.io/docs/practices/alerting/)

这份指南将帮助你快速建立完整的监控系统。如果遇到问题，请检查日志并参考故障排除部分。