# JvmEye

轻量级本地 JVM 监控工具，基于 **JDK Attach API** 自动发现本机可连接 JVM，通过 **JMX** 采集运行时指标，并提供诊断能力。前端为 **Vue 3 + Vite + ECharts** 单页应用，后端打包为独立 **Spring Boot** jar，开箱即用。

---

## 功能特性

### 实时监控
- **内存**：堆 used/committed/max、非堆、各内存池（Memory Pool）明细与峰值
- **GC**：各收集器次数/累计耗时、最近一次 GC 信息
- **线程**：活跃/守护/峰值线程数、线程状态分布、死锁检测
- **CPU**：进程 CPU 负载、系统 LoadAverage、核数
- **类加载**：已加载/累计/卸载
- **运行时**：JVM 名称/版本/厂商、启动时间、运行时长

### 诊断能力
- **线程 Dump**：全量线程堆栈，死锁高亮，可折叠查看
- **类直方图（Histogram）**：基于 `DiagnosticCommandMBean` 输出 Top N 类实例与字节占用

### 安全与部署
- Spring Security 表单登录，`/api/**` 鉴权，CSRF 关闭（内网工具）
- 前端构建产物内嵌，单 jar 部署，无需额外 Web 服务器

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5.x、Java 21、Maven |
| 前端 | Vue 3、Vite、ECharts、Axios |
| 安全 | Spring Security（表单登录 + BCrypt） |
| 发现 | JDK Attach API（`com.sun.tools.attach.VirtualMachine`） |
| 采集 | JMX（MemoryMXBean、GarbageCollectorMXBean、ThreadMXBean 等） |

---

## 快速开始

### 环境要求

- JDK 21
- Maven 3.8+
- Node.js 18+（仅开发阶段需要）

### 构建

#### 方式一：直接构建前端并打包（推荐）

```bash
cd /home/hbz/MyProject01/JvmEye
bash scripts/build-all.sh
```

该脚本会自动完成：前端 `npm install` → `npm run build` → 复制 `dist` 到 `src/main/resources/static` → `mvn package -DskipTests`。

#### 方式二：分步构建

```bash
# 1. 构建前端
cd frontend
npm install
npm run build

# 2. 复制前端产物到后端 static
cp -r dist/* ../src/main/resources/static/

# 3. Maven 打包
cd ..
mvn package -DskipTests
```

### 运行

```bash
java -jar target/jvme-eye.jar
```

访问 `http://localhost:8080`，默认会被重定向到登录页。

### 配置

通过 `src/main/resources/application.yml` 配置：

```yaml
server:
  port: 8080

jvmeeye:
  user: admin
  password: "{bcrypt}your-bcrypt-password"
  sampling:
    interval-ms: 1000       # 采样间隔（毫秒）
    history-size: 600       # 环形缓冲容量（默认 600 点 = 10 分钟）
```

> 密码支持 `{bcrypt}` 前缀。若未提供前缀，启动时会自动 BCrypt 编码并写入配置。

---

## 使用说明

1. **启动**：运行 `java -jar target/jvme-eye.jar`
2. **登录**：浏览器打开 `http://localhost:8080`，输入 `application.yml` 中配置的用户名和密码
3. **发现目标**：登录后在目标选择器查看本机可 attach 的 JVM 列表
4. **连接监控**：选择目标 JVM 后点击连接，进入 Dashboard 查看实时指标
5. **诊断**：点击“线程 Dump”查看全量线程堆栈与死锁标记；点击“类直方图”查看 Top N 类实例分布

### 目标 JVM 要求

- 与 JvmEye **同一用户** 运行
- **同一架构**（32/64 位一致）
- 目标 JVM 需支持 Attach 机制（标准 HotSpot 均可）

---

## 项目结构

```
JvmEye/
├── pom.xml                           # Maven 配置
├── PLAN.md                           # 项目计划
├── review.md                         # 验收报告
├── README.md                         # 项目说明
├── docs/
│   └── API.md                        # API 文档
├── frontend/                         # Vue 3 + Vite 前端
│   ├── src/
│   │   ├── views/                    # 页面：LoginView、DashboardView
│   │   ├── components/               # 组件：图表、目标选择器、诊断面板
│   │   ├── api.js                    # axios 实例
│   │   ├── router.js                 # 路由
│   │   └── main.js                   # 入口
│   ├── package.json
│   └── vite.config.js                # proxy /api → http://localhost:8080
├── scripts/
│   └── build-all.sh                  # 一键构建脚本
├── src/main/java/com/jvmeeye/
│   ├── JvmEyeApplication.java        # 启动类
│   ├── config/                       # Security、Properties、UserConfig
│   ├── controller/                   # REST 控制器
│   ├── discovery/                    # JVM 发现（Attach API）
│   ├── monitor/                      # JMX 连接、指标采样与查询
│   ├── diagnostics/                  # 线程 Dump、类直方图
│   └── exception/                    # 全局异常处理
└── src/main/resources/
    └── static/                       # 前端构建产物（运行期存在）
```

---

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/targets` | 列出本机可 attach 的 JVM |
| POST | `/api/targets/{pid}/connect` | 连接目标 JVM |
| DELETE | `/api/targets/{pid}/disconnect` | 断开目标 JVM |
| DELETE | `/api/targets/disconnect` | 断开当前连接 |
| GET | `/api/metrics/current` | 获取当前指标快照 |
| GET | `/api/metrics/history` | 获取历史指标序列 |
| GET | `/api/diagnostics/thread-dump` | 线程 Dump |
| GET | `/api/diagnostics/histogram` | 类直方图 |

详细接口说明见 [docs/API.md](docs/API.md)。

---

## 测试

```bash
mvn test
```

当前包含 11 个测试用例，覆盖上下文加载、Security 规则、JVM 发现解析、类直方图解析。

---

## 已知限制（v1）

- 仅支持**本机** JVM 发现与监控（依赖 Attach API）
- **单目标**监控（架构已预留多目标，前端未实现多卡片）
- 历史数据**仅内存存储**，重启清空
- 无告警体系
- 内存用户，无角色/权限体系

---

## 后续迭代

- 多目标监控与前端多卡片视图
- WebSocket 推送替代轮询
- 历史数据持久化（SQLite / H2）
- 远程 JVM 通过 host:port JMX 连接
- 告警规则与通知

---

## License

MIT
