# Docker 打包与虚拟环境部署保姆级文档

这份文档按“第一次接触 Docker 也能照着做”的标准写。默认你要在一个干净的虚拟环境里操作，例如：

- Windows 上的 WSL2 Ubuntu；
- VMware / VirtualBox 里的 Ubuntu 虚拟机；
- 云服务器上的 Ubuntu 测试机。

如果你说的“虚拟环境”是 Python 的 `venv`，这个项目不需要它。本项目是 Spring Boot + Vue + MySQL，隔离环境主要靠“虚拟机 / WSL2 + Docker”。

## 0. 先看清楚当前项目的 Docker 状态

当前仓库已经有：

```text
compose.yaml
.env.example
```

当前 `compose.yaml` 只定义了 MySQL 8.4 服务：

```text
mysql -> 127.0.0.1:3306 -> 数据库 erp
```

当前仓库还没有这些文件：

```text
backend/Dockerfile
frontend/Dockerfile
compose.app.yaml
```

所以现在可以直接执行的方案是：

```text
Docker 跑 MySQL
后端打成 JAR 后运行
前端打成 dist 后用 Nginx 容器托管
```

如果老师要求“前端、后端、数据库全部都是 Docker 镜像并且一条 compose 启动”，那需要后续再新增 Dockerfile 和完整 compose 文件。本文最后有“完整容器化改造清单”。

## 1. 最终效果是什么

照本文操作完，你会得到一个可交付包：

```text
release/erp-docker-package/
├── backend/
│   └── erp-backend-0.0.1-SNAPSHOT.jar
├── frontend/
│   └── dist/
├── nginx/
│   └── default.conf
├── compose.yaml
├── .env
└── START.md
```

启动后：

| 服务 | 运行方式 | 默认地址 |
| --- | --- | --- |
| MySQL | Docker Compose | `127.0.0.1:3306` |
| 后端 | Java JAR | `127.0.0.1:8080` |
| 前端 | Nginx Docker 容器 | `http://127.0.0.1:8081` |

访问系统时打开：

```text
http://127.0.0.1:8081
```

## 2. 虚拟环境准备

### 2.1 推荐配置

虚拟机最低配置建议：

| 项目 | 建议 |
| --- | --- |
| CPU | 2 核或以上 |
| 内存 | 4 GB 或以上 |
| 磁盘 | 30 GB 或以上 |
| 系统 | Ubuntu 22.04 / 24.04 |
| 网络 | 能访问互联网，方便下载依赖和镜像 |

### 2.2 检查系统版本

在虚拟环境终端里执行：

```bash
cat /etc/os-release
uname -a
```

确认是 Ubuntu 或兼容的 Linux 环境。

## 3. 安装基础软件

### 3.1 更新系统包

```bash
sudo apt update
sudo apt upgrade -y
```

### 3.2 安装 Git、curl、解压工具

```bash
sudo apt install -y git curl unzip ca-certificates gnupg lsb-release
```

### 3.3 安装 JDK 17

```bash
sudo apt install -y openjdk-17-jdk
java -version
```

看到类似下面内容即可：

```text
openjdk version "17..."
```

### 3.4 安装 Maven

当前仓库只有 Windows 的 `mvnw.cmd`，没有 Linux 的 `mvnw`，所以在 Linux 虚拟环境里需要安装 Maven：

```bash
sudo apt install -y maven
mvn -version
```

### 3.5 安装 Node.js 20 和 npm

如果你的虚拟环境里已经有 Node.js 20+，可以跳过安装，只检查版本：

```bash
node --version
npm --version
```

如果没有 Node.js 20+，推荐用 nvm 安装：

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20
node --version
npm --version
```

版本要求：

```text
node >= 20
npm >= 10
```

### 3.6 安装 Docker Engine 和 Docker Compose 插件

Ubuntu 中推荐使用 Docker 官方 apt 仓库安装。

设置 Docker apt 仓库：

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
```

安装 Docker：

```bash
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

检查版本：

```bash
docker --version
docker compose version
```

如果普通用户执行 `docker ps` 提示权限不足，执行：

```bash
sudo usermod -aG docker $USER
```

然后退出虚拟环境终端，重新登录，再执行：

```bash
docker ps
```

## 4. 获取项目代码

### 4.1 从 GitHub 克隆

如果代码已经上传到仓库，推荐在虚拟环境里克隆：

```bash
mkdir -p ~/workspace
cd ~/workspace
git clone <你的仓库地址> ERP
cd ERP
```

例如：

```bash
git clone https://github.com/3341332618/<仓库名>.git ERP
```

### 4.2 从 Windows 复制项目

如果还没有推到 GitHub，可以把整个项目文件夹压缩后复制到虚拟机。

不要复制这些目录：

```text
frontend/node_modules/
frontend/dist/
backend/target/
logs/
uploads/
```

复制后进入项目：

```bash
cd ~/workspace/ERP
```

检查关键文件是否存在：

```bash
ls
ls backend
ls frontend
ls compose.yaml
```

## 5. 配置环境变量

在项目根目录执行：

```bash
cp .env.example .env
```

查看 `.env`：

```bash
cat .env
```

默认内容应类似：

```properties
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=erp
DB_USERNAME=erp
DB_PASSWORD=erp_local_password
MYSQL_DATABASE=erp
MYSQL_USER=erp
MYSQL_PASSWORD=erp_local_password
MYSQL_ROOT_PASSWORD=erp_root_local_password
SPRING_PROFILES_ACTIVE=local
```

解释一下：

| 变量 | 用途 |
| --- | --- |
| `MYSQL_DATABASE` | Docker MySQL 第一次初始化时创建的库名 |
| `MYSQL_USER` | Docker MySQL 第一次初始化时创建的业务用户 |
| `MYSQL_PASSWORD` | Docker MySQL 业务用户密码 |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 密码 |
| `DB_HOST` | 后端连接 MySQL 的地址 |
| `DB_PORT` | 后端连接 MySQL 的端口 |
| `DB_NAME` | 后端连接的数据库名 |
| `DB_USERNAME` | 后端连接数据库的用户名 |
| `DB_PASSWORD` | 后端连接数据库的密码 |

当前文档使用“后端 JAR 跑在虚拟机本机，MySQL 跑在 Docker 容器”的方式，所以：

```properties
DB_HOST=127.0.0.1
DB_PORT=3306
```

不用改。

## 6. 启动 Docker MySQL

在项目根目录执行：

```bash
docker compose up -d mysql
```

查看状态：

```bash
docker compose ps
```

正常会看到 `mysql` 状态为 `running`，健康检查可能需要等一会儿才变成 `healthy`。

查看日志：

```bash
docker compose logs -f mysql
```

看到 MySQL 初始化完成后，按 `Ctrl + C` 退出日志查看。注意这只是退出日志，不会停止 MySQL。

## 7. 后端打包

进入后端目录：

```bash
cd ~/workspace/ERP/backend
```

执行测试：

```bash
mvn test
```

如果只是打包，不想跑测试，可以执行：

```bash
mvn clean package -DskipTests
```

推荐正式交付前执行完整打包：

```bash
mvn clean package
```

打包成功后检查 JAR：

```bash
ls -lh target/*.jar
```

应该看到：

```text
target/erp-backend-0.0.1-SNAPSHOT.jar
```

## 8. 后端运行验证

仍然在 `backend` 目录下执行：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=erp
export DB_USERNAME=erp
export DB_PASSWORD=erp_local_password
export SPRING_PROFILES_ACTIVE=local

java -jar target/erp-backend-0.0.1-SNAPSHOT.jar
```

看到 Spring Boot 启动完成后，新开一个终端验证登录接口：

```bash
curl -i -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"superadmin","password":"123456"}'
```

正常会返回 JSON，并带有 token。只要不是连接失败、500、数据库错误，就说明后端已经能连上 Docker MySQL。

如果要后台运行后端：

```bash
cd ~/workspace/ERP/backend
nohup java -jar target/erp-backend-0.0.1-SNAPSHOT.jar > ../logs/backend.log 2>&1 &
```

如果 `logs` 目录不存在，先创建：

```bash
mkdir -p ~/workspace/ERP/logs
```

查看后端日志：

```bash
tail -f ~/workspace/ERP/logs/backend.log
```

停止后端：

```bash
ps -ef | grep erp-backend
kill <进程ID>
```

## 9. 前端打包

进入前端目录：

```bash
cd ~/workspace/ERP/frontend
```

安装依赖：

```bash
npm install
```

执行测试：

```bash
npm test -- --run
```

生产构建：

```bash
npm run build
```

检查产物：

```bash
ls -lh dist
```

正常会看到：

```text
index.html
assets/
```

## 10. 用 Nginx Docker 容器托管前端

因为前端已经构建成静态文件，所以可以直接用 Nginx 容器托管 `frontend/dist`。

### 10.1 创建 Nginx 配置

回到项目根目录：

```bash
cd ~/workspace/ERP
mkdir -p deploy/nginx
```

创建配置文件：

```bash
cat > deploy/nginx/default.conf <<'EOF'
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://host.docker.internal:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
```

解释：

- `/` 访问 Vue 前端页面；
- `/api/` 转发到后端 `8080`；
- `host.docker.internal` 代表 Docker 容器访问虚拟机宿主网络；
- Linux 下需要在启动容器时加 `--add-host=host.docker.internal:host-gateway`。

### 10.2 启动 Nginx 前端容器

```bash
docker rm -f erp-frontend-nginx 2>/dev/null || true

docker run -d \
  --name erp-frontend-nginx \
  --add-host=host.docker.internal:host-gateway \
  -p 8081:80 \
  -v "$(pwd)/frontend/dist:/usr/share/nginx/html:ro" \
  -v "$(pwd)/deploy/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine
```

查看容器：

```bash
docker ps
```

查看 Nginx 日志：

```bash
docker logs -f erp-frontend-nginx
```

浏览器访问：

```text
http://127.0.0.1:8081
```

如果是在虚拟机里运行，Windows 浏览器访问虚拟机 IP：

```bash
ip addr
```

找到虚拟机 IP 后访问：

```text
http://<虚拟机IP>:8081
```

## 11. 制作交付包

回到项目根目录：

```bash
cd ~/workspace/ERP
```

创建交付目录：

```bash
rm -rf release/erp-docker-package
mkdir -p release/erp-docker-package/backend
mkdir -p release/erp-docker-package/frontend
mkdir -p release/erp-docker-package/nginx
```

复制后端 JAR：

```bash
cp backend/target/erp-backend-0.0.1-SNAPSHOT.jar release/erp-docker-package/backend/
```

复制前端构建产物：

```bash
cp -r frontend/dist release/erp-docker-package/frontend/
```

复制 Docker Compose 和环境变量：

```bash
cp compose.yaml release/erp-docker-package/
cp .env release/erp-docker-package/
```

复制 Nginx 配置：

```bash
cp deploy/nginx/default.conf release/erp-docker-package/nginx/
```

创建交付包启动说明：

````bash
cat > release/erp-docker-package/START.md <<'EOF'
# ERP 交付包启动说明

## 1. 启动 MySQL

```bash
docker compose up -d mysql
docker compose ps
```

## 2. 启动后端

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=erp
export DB_USERNAME=erp
export DB_PASSWORD=erp_local_password
export SPRING_PROFILES_ACTIVE=local

java -jar backend/erp-backend-0.0.1-SNAPSHOT.jar
```

## 3. 启动前端 Nginx

```bash
docker rm -f erp-frontend-nginx 2>/dev/null || true

docker run -d \
  --name erp-frontend-nginx \
  --add-host=host.docker.internal:host-gateway \
  -p 8081:80 \
  -v "$(pwd)/frontend/dist:/usr/share/nginx/html:ro" \
  -v "$(pwd)/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine
```

## 4. 访问系统

```text
http://127.0.0.1:8081
```

默认账号：

```text
superadmin / 123456
student01 / 123456
```
EOF
````

打成压缩包：

```bash
cd release
tar -czf erp-docker-package.tar.gz erp-docker-package
ls -lh erp-docker-package.tar.gz
```

最后交付这个文件：

```text
release/erp-docker-package.tar.gz
```

## 12. 在另一台虚拟环境里验证交付包

把 `erp-docker-package.tar.gz` 复制到另一台虚拟机后：

```bash
tar -xzf erp-docker-package.tar.gz
cd erp-docker-package
```

启动 MySQL：

```bash
docker compose up -d mysql
docker compose ps
```

启动后端：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=erp
export DB_USERNAME=erp
export DB_PASSWORD=erp_local_password
export SPRING_PROFILES_ACTIVE=local

java -jar backend/erp-backend-0.0.1-SNAPSHOT.jar
```

新开终端启动前端：

```bash
docker rm -f erp-frontend-nginx 2>/dev/null || true

docker run -d \
  --name erp-frontend-nginx \
  --add-host=host.docker.internal:host-gateway \
  -p 8081:80 \
  -v "$(pwd)/frontend/dist:/usr/share/nginx/html:ro" \
  -v "$(pwd)/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine
```

访问：

```text
http://127.0.0.1:8081
```

## 13. Windows PowerShell 对照命令

如果你不在 Linux 虚拟机里，而是在 Windows 本机操作，命令大致如下。

### 13.1 启动 MySQL

```powershell
cd "D:\ai work\ERP"
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d mysql
docker compose ps
```

### 13.2 后端打包

Windows 项目里有 `mvnw.cmd`，所以用：

```powershell
cd "D:\ai work\ERP\backend"
.\mvnw.cmd clean package
```

生成：

```text
backend\target\erp-backend-0.0.1-SNAPSHOT.jar
```

### 13.3 前端打包

```powershell
cd "D:\ai work\ERP\frontend"
npm.cmd install
npm.cmd run build
```

生成：

```text
frontend\dist\
```

### 13.4 Windows 运行后端

```powershell
cd "D:\ai work\ERP"
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "erp"
$env:DB_USERNAME = "erp"
$env:DB_PASSWORD = "erp_local_password"
$env:SPRING_PROFILES_ACTIVE = "local"
java -jar .\backend\target\erp-backend-0.0.1-SNAPSHOT.jar
```

### 13.5 Windows 验证后端

```powershell
Invoke-WebRequest `
  -Uri "http://127.0.0.1:8080/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"superadmin","password":"123456"}'
```

## 14. 常见错误处理

### 14.1 `docker: permission denied`

原因：当前 Linux 用户没有 Docker 权限。

处理：

```bash
sudo usermod -aG docker $USER
```

然后退出终端重新登录。

### 14.2 MySQL 端口 3306 被占用

检查：

```bash
sudo lsof -i :3306
```

如果你想改成 3307，编辑 `.env`：

```properties
DB_PORT=3307
```

然后重启：

```bash
docker compose down
docker compose up -d mysql
```

### 14.3 后端连接数据库失败

检查 MySQL 是否启动：

```bash
docker compose ps
docker compose logs mysql --tail=100
```

检查 `.env`：

```bash
cat .env
```

后端 JAR 跑在虚拟机本机时，应该使用：

```properties
DB_HOST=127.0.0.1
DB_PORT=3306
```

### 14.4 前端页面打开了，但接口 502

检查后端是否启动：

```bash
curl -i http://127.0.0.1:8080/api/auth/login
```

检查 Nginx 日志：

```bash
docker logs erp-frontend-nginx --tail=100
```

如果 Nginx 容器访问不到后端，确认启动容器时带了：

```bash
--add-host=host.docker.internal:host-gateway
```

### 14.5 修改 `.env` 后 MySQL 用户密码没变

MySQL 容器只会在数据卷第一次创建时读取初始化账号密码。已经存在数据卷后，改 `.env` 不会自动改旧账号。

如果是测试环境，可以清空数据卷重建：

```bash
docker compose down -v
docker compose up -d mysql
```

这会删除 Docker MySQL 数据卷里的数据。

### 14.6 Maven 下载依赖很慢

这是网络问题。可以多试几次：

```bash
mvn clean package -DskipTests
```

如果你在学校网络环境，可以考虑配置 Maven 镜像源。

### 14.7 npm 下载依赖很慢

可以先清理后重试：

```bash
npm cache verify
npm install
```

如果仍然很慢，再配置 npm 镜像源。

## 15. Docker 常用命令速查

```bash
# 查看容器
docker ps

# 查看全部容器，包括已停止
docker ps -a

# 查看镜像
docker images

# 查看 compose 服务
docker compose ps

# 启动 MySQL
docker compose up -d mysql

# 停止 MySQL，但保留数据
docker compose stop mysql

# 停止并删除 MySQL 容器，但保留数据卷
docker compose down

# 停止并删除 MySQL 容器，同时删除数据卷
docker compose down -v

# 查看 MySQL 日志
docker compose logs -f mysql

# 查看前端 Nginx 日志
docker logs -f erp-frontend-nginx

# 删除前端 Nginx 容器
docker rm -f erp-frontend-nginx
```

## 16. 完整容器化改造清单

如果你后面希望做到“一条命令启动前端、后端、MySQL”，需要新增这些文件：

```text
backend/Dockerfile
frontend/Dockerfile
deploy/nginx/default.conf
compose.app.yaml
.dockerignore
```

目标命令会变成：

```bash
docker compose -f compose.app.yaml up -d --build
```

完整容器化时，后端容器连接 MySQL 不能再用 `127.0.0.1`，应该用 compose 服务名：

```properties
DB_HOST=mysql
DB_PORT=3306
```

这是因为在 Docker 网络里：

```text
127.0.0.1 = 当前容器自己
mysql = compose 里的 MySQL 服务
```

当前仓库还没有这些 Dockerfile，所以不要直接执行 `docker compose -f compose.app.yaml ...`。如果要我继续做完整容器化，我需要新增 Dockerfile 和完整 compose 配置。

## 17. 最推荐的交付步骤

按顺序做：

```bash
# 1. 进入项目
cd ~/workspace/ERP

# 2. 准备环境变量
cp .env.example .env

# 3. 启动 MySQL
docker compose up -d mysql

# 4. 后端测试和打包
cd backend
mvn clean package
cd ..

# 5. 前端测试和打包
cd frontend
npm install
npm test -- --run
npm run build
cd ..

# 6. 准备 Nginx 配置
mkdir -p deploy/nginx

# 7. 启动后端
cd backend
java -jar target/erp-backend-0.0.1-SNAPSHOT.jar
```

后端启动后，新开一个终端启动前端 Nginx：

```bash
cd ~/workspace/ERP

docker rm -f erp-frontend-nginx 2>/dev/null || true

docker run -d \
  --name erp-frontend-nginx \
  --add-host=host.docker.internal:host-gateway \
  -p 8081:80 \
  -v "$(pwd)/frontend/dist:/usr/share/nginx/html:ro" \
  -v "$(pwd)/deploy/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:1.27-alpine
```

浏览器访问：

```text
http://127.0.0.1:8081
```
## 18. 参考链接

- Docker Engine Ubuntu 官方安装文档：<https://docs.docker.com/engine/install/ubuntu/>
- Docker Compose 插件官方安装文档：<https://docs.docker.com/compose/install/linux/>

本文里的 Docker 安装命令按 Docker 官方 Ubuntu apt 仓库方式整理；项目运行、打包、端口、数据库和 Nginx 代理步骤按当前仓库的 `compose.yaml`、`.env.example`、`backend/pom.xml`、`frontend/package.json` 和 `frontend/vite.config.ts` 编写。
