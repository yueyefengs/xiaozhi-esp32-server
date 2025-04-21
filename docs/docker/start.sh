#!/bin/bash

# 启用错误检测
set -e

echo "Starting services..."

# 检查Nginx是否已安装
if [ ! -f /usr/sbin/nginx ]; then
    echo "Error: nginx is not installed"
    exit 1
fi

# 检查Nginx配置
echo "Checking nginx configuration..."
/usr/sbin/nginx -t || exit 1

# 确保Nginx日志目录存在
mkdir -p /var/log/nginx

# 启动Java后端（docker内监听8003端口）
echo "Starting Java application..."
java -jar /app/xiaozhi-esp32-api.jar \
  --server.port=8003 \
  --server.servlet.context-path=/xiaozhi \
  --spring.datasource.druid.url=${SPRING_DATASOURCE_DRUID_URL} \
  --spring.datasource.druid.username=${SPRING_DATASOURCE_DRUID_USERNAME} \
  --spring.datasource.druid.password=${SPRING_DATASOURCE_DRUID_PASSWORD} \
  --spring.data.redis.host=${SPRING_DATA_REDIS_HOST} \
  --spring.data.redis.port=${SPRING_DATA_REDIS_PORT} &

# 确保Java服务有时间启动
echo "Waiting for Java application to start..."
sleep 5

# 删除可能存在的pid文件
rm -f /var/run/nginx.pid

# 启动Nginx并保持前台运行
echo "Starting Nginx in foreground mode..."
exec /usr/sbin/nginx -g 'daemon off;'