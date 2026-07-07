@echo off
echo ================================
echo 启动乐尚代驾监控系统
echo ================================

echo.
echo [1/3] 启动 Prometheus...
cd monitoring\prometheus
docker-compose up -d
if %errorlevel% neq 0 (
    echo Prometheus 启动失败！
    exit /b 1
)
echo ✓ Prometheus 启动成功 (http://localhost:9090)

echo.
echo [2/3] 启动 Grafana...
cd ..\grafana
docker-compose up -d
if %errorlevel% neq 0 (
    echo Grafana 启动失败！
    exit /b 1
)
echo ✓ Grafana 启动成功 (http://localhost:3000)

echo.
echo [3/3] 等待服务就绪...
timeout /t 10 /nobreak

echo.
echo ================================
echo 监控系统启动完成！
echo ================================
echo Prometheus: http://localhost:9090
echo Grafana: http://localhost:3000
echo 默认账号密码: admin / admin
echo.
echo 请在 Grafana 中导入仪表盘: monitoring/grafana/dashboards/microservices-dashboard.json
