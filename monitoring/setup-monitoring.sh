#!/bin/bash

# Mini-UPS Prometheus监控系统一键安装脚本
# 作者: Mini-UPS Development Team
# 用途: 快速搭建完整的监控系统

set -e

echo "🚀 开始安装Mini-UPS监控系统..."

# 创建监控目录结构
echo "📁 创建监控配置目录..."
mkdir -p monitoring/{prometheus/rules,grafana/{provisioning/datasources,provisioning/dashboards,dashboards},alertmanager}

# 创建Prometheus配置文件
echo "⚙️ 配置Prometheus..."
cat > monitoring/prometheus/prometheus.yml << 'EOF'
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

  # cAdvisor (容器指标)
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
EOF

# 创建告警规则
echo "🔔 配置告警规则..."
cat > monitoring/prometheus/rules/mini-ups-alerts.yml << 'EOF'
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

  # 系统CPU使用率过高
  - alert: HighCPUUsage
    expr: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
    for: 5m
    labels:
      severity: warning
      service: system
    annotations:
      summary: "系统CPU使用率过高"
      description: "CPU使用率超过80%: {{ $value }}%"

  # 系统内存使用率过高
  - alert: HighSystemMemoryUsage
    expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 0.9
    for: 5m
    labels:
      severity: critical
      service: system
    annotations:
      summary: "系统内存使用率过高"
      description: "内存使用率超过90%: {{ $value | humanizePercentage }}"

  # 磁盘空间不足
  - alert: LowDiskSpace
    expr: (1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) > 0.85
    for: 5m
    labels:
      severity: warning
      service: system
    annotations:
      summary: "磁盘空间不足"
      description: "磁盘 {{ $labels.mountpoint }} 使用率超过85%: {{ $value | humanizePercentage }}"
EOF

# 配置Grafana数据源
echo "📊 配置Grafana数据源..."
cat > monitoring/grafana/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF

# 配置Grafana仪表板自动加载
cat > monitoring/grafana/provisioning/dashboards/dashboards.yml << 'EOF'
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

# 复制仪表板JSON文件
echo "📈 安装预制仪表板..."
if [ -f "../docs/monitoring/grafana-dashboard-mini-ups.json" ]; then
    cp ../docs/monitoring/grafana-dashboard-mini-ups.json monitoring/grafana/dashboards/
    echo "✅ Mini-UPS仪表板已安装"
else
    echo "⚠️ 仪表板文件未找到，请手动导入"
fi

# 配置AlertManager
echo "🚨 配置告警管理器..."
cat > monitoring/alertmanager/alertmanager.yml << 'EOF'
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@mini-ups.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'webhook'

receivers:
  - name: 'webhook'
    webhook_configs:
      - url: 'http://127.0.0.1:5001/'
        send_resolved: true

  # 邮件通知配置示例 (需要配置SMTP服务器)
  - name: 'email-alerts'
    email_configs:
      - to: 'admin@mini-ups.com'
        subject: 'Mini-UPS告警: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          告警: {{ .Annotations.summary }}
          描述: {{ .Annotations.description }}
          时间: {{ .StartsAt }}
          状态: {{ .Status }}
          {{ end }}

  # Slack通知配置示例
  - name: 'slack-alerts'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts'
        title: 'Mini-UPS告警'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'dev', 'instance']
EOF

echo "🐳 启动监控系统..."

# 启动监控服务
cd monitoring
docker-compose -f docker-compose.monitoring.yml up -d

echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "📋 检查服务状态..."
docker-compose -f docker-compose.monitoring.yml ps

echo ""
echo "🎉 监控系统安装完成！"
echo ""
echo "📊 访问地址:"
echo "  • Prometheus:   http://localhost:9090"
echo "  • Grafana:      http://localhost:3001 (admin/admin123)"
echo "  • AlertManager: http://localhost:9093"
echo "  • Node Exporter: http://localhost:9100"
echo "  • cAdvisor:     http://localhost:8080"
echo ""
echo "📈 下一步操作:"
echo "  1. 确保Mini-UPS应用在端口8081运行"
echo "  2. 访问Grafana导入仪表板"
echo "  3. 配置告警通知渠道"
echo "  4. 根据需要调整告警阈值"
echo ""
echo "🔧 管理命令:"
echo "  • 查看日志: docker-compose -f docker-compose.monitoring.yml logs -f"
echo "  • 停止服务: docker-compose -f docker-compose.monitoring.yml down"
echo "  • 重启服务: docker-compose -f docker-compose.monitoring.yml restart"
echo ""

# 检查Mini-UPS是否运行
echo "🔍 检查Mini-UPS连接状态..."
if curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ Mini-UPS应用运行正常"
else
    echo "⚠️ Mini-UPS应用未运行，请启动后端服务"
    echo "   启动命令: ./start-local.sh"
fi

echo ""
echo "📚 更多信息请查看: docs/monitoring/PROMETHEUS_BEGINNER_GUIDE.md"