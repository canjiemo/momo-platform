# Quick Start Guide

快速开始指南 - Seer Fitness EDU 先知智慧体育校园系统

## 系统要求

- **Java**: JDK 17+
- **PostgreSQL**: 14+
- **Redis**: 6.0+
- **Maven**: 3.6+ (可选，项目包含 Maven Wrapper)

## 快速启动（5分钟）

### 1. 安装 PostgreSQL

**macOS**:
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Ubuntu/Debian**:
```bash
sudo apt-get update
sudo apt-get install postgresql-14
sudo systemctl start postgresql
```

**Docker**:
```bash
docker run --name seer-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:14
```

### 2. 安装 Redis

**macOS**:
```bash
brew install redis
brew services start redis
```

**Ubuntu/Debian**:
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

**Docker**:
```bash
docker run --name seer-redis \
  -p 6379:6379 \
  -d redis:7-alpine
```

### 3. 创建数据库

```bash
# 连接到 PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE seer_fitness_edu ENCODING 'UTF8';

# 退出
\q
```

### 4. 初始化数据库表

```bash
cd seer-fitness-edu

# 执行建表脚本
psql -U postgres -d seer_fitness_edu -f seer-fitness-boot/src/main/resources/db/pgsql/001_create_tables.sql

# 执行初始数据脚本
psql -U postgres -d seer_fitness_edu -f seer-fitness-boot/src/main/resources/db/pgsql/002_init_data.sql
```

### 5. 配置数据库连接

编辑 `seer-fitness-boot/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource  # 必须指定
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/seer_fitness_edu
    username: postgres
    password: postgres  # 改为你的密码
```

**重要**: 必须指定 `type: com.alibaba.druid.pool.DruidDataSource` 以确保 Druid 连接池正确初始化。

### 6. 构建并运行

```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
java -jar seer-fitness-boot/target/seer-fitness-boot-1.0.0.jar
```

或者直接使用 Maven 运行：
```bash
mvn spring-boot:run -pl seer-fitness-boot
```

### 7. 验证安装

打开浏览器访问：

- **应用地址**: http://localhost:8080
- **Druid监控**: http://localhost:8080/druid/
  - 用户名: `admin`
  - 密码: `admin123`

**测试数据库连接**:
```bash
# 检查管理员用户
psql -U postgres -d seer_fitness_edu -c "SELECT username, real_name, admin_flag FROM sys_user WHERE username = 'admin';"
```

应该看到：
```
 username | real_name  | admin_flag
----------+------------+------------
 admin    | 超级管理员  | t
```

## 默认账号

- **用户名**: `admin`
- **密码**: `admin123`

⚠️ **重要**: 首次登录后请立即修改密码！

## 开发环境配置

使用开发环境配置文件：

```bash
# 运行开发环境
java -jar seer-fitness-boot/target/seer-fitness-boot-1.0.0.jar --spring.profiles.active=dev
```

开发环境特性：
- 详细的调试日志
- Druid 监控面板启用
- 本地数据库连接

## 生产环境配置

使用生产环境配置文件：

```bash
# 设置环境变量
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export REDIS_HOST=your_redis_host
export REDIS_PASSWORD=your_redis_password
export JWT_SECRET=your_jwt_secret_key_at_least_256_bits

# 运行生产环境
java -jar seer-fitness-boot/target/seer-fitness-boot-1.0.0.jar --spring.profiles.active=prod
```

## 故障排查

### PostgreSQL 连接失败

```bash
# 检查 PostgreSQL 是否运行
pg_isready -U postgres

# 查看 PostgreSQL 日志
tail -f /usr/local/var/log/postgresql@14.log  # macOS
sudo tail -f /var/log/postgresql/postgresql-14-main.log  # Ubuntu
```

### Redis 连接失败

```bash
# 测试 Redis 连接
redis-cli ping
# 应该返回: PONG
```

### 应用启动失败

检查日志文件：
```bash
tail -f logs/seer-fitness-edu.log
```

常见问题：
1. **端口占用**: 修改 `application.yml` 中的 `server.port`
2. **数据库未初始化**: 重新执行 SQL 脚本
3. **Redis 未启动**: 启动 Redis 服务

## Docker Compose 快速启动

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: seer_fitness_edu
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

启动服务：
```bash
docker-compose up -d
```

## 下一步

1. 阅读 [CLAUDE.md](./CLAUDE.md) 了解项目架构
2. 查看 [API 文档](#) (如果有)
3. 阅读数据库设计 [db/README.md](./seer-fitness-boot/src/main/resources/db/README.md)

## 技术支持

遇到问题？
1. 检查日志文件: `logs/seer-fitness-edu.log`
2. 查看 Druid 监控: http://localhost:8080/druid/
3. 参考 [CLAUDE.md](./CLAUDE.md) 文档

---

🎉 恭喜！您已成功启动 Seer Fitness EDU 系统！
