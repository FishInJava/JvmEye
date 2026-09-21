# JvmEye — 轻量级 JVM 监控软件计划

## Context

在空目录 `JvmEye/`(项目根:`/home/hbz/MyProject01/JvmEye`)从零构建一个 JVM 监控软件:

- **后端**:Spring Boot 3.x + Java 21 + Maven
- **监控方式**:JDK Attach API 自动探查本机所有可连接的 JVM,通过 JMX 连接目标进程采集指标
- **前端**:独立 Vue 3 + Vite + ECharts 单页应用(dev 时代理到后端,生产构建产物放入后端 static 由同一 jar 提供)
- **第一版单目标**:前端在发现的 JVM 列表中选择一个进行监控,架构上预留多目标扩展
- **安全**:Spring Security 表单登录,用户配置在 `application.yml`(内网工具,CSRF 关闭)

### 功能范围(基础 + 诊断)

| 类别 | 指标 |
|---|---|
| 内存 | 堆 used/committed/max、非堆、各内存池(Memory Pool)明细与峰值 |
| GC | 各收集器次数/累计耗时、最近一次 GC 信息 |
| 线程 | 活跃/守护/峰值线程数、线程状态分布、死锁检测、**线程 Dump** |
| CPU | 进程 CPU 负载、系统 LoadAverage、核数 |
| 类加载 | 已加载/累计/卸载 |
| 运行时 | JVM 名称/版本/厂商、启动时间、运行时长 |
| 诊断 | **类直方图(Histogram)**、线程 Dump(带堆栈) |

## Approach

1. **发现(Discovery)**:`com.sun.tools.attach.VirtualMachine.list()` 列出本机 JVM(pid + 显示名),读取 `agentProperties` 得到 main class / 启动参数,判断可 attach 性(同用户、同架构)。对每个候选记录 `localConnectorAddress`(attach 后获取),供后续 JMX 连接。
2. **连接**:选中目标后通过 `JMXConnectorFactory.connect(localConnectorAddress)` 建立 JMX 连接;`JmxConnectionManager` 持有当前唯一活动连接(v1 单目标,内部用 `ConcurrentHashMap` 结构为多目标预留)。
3. **采集**:基于标准 MBean(`MemoryMXBean`、`GarbageCollectorMXBean`、`ThreadMXBean`、`com.sun.management.OperatingSystemMXBean`、`ClassLoadingMXBean`、`RuntimeMXBean`、`MemoryPoolMXBean`)。后台 `@Scheduled` 每秒采样一次,写入内存环形缓冲(默认 600 点 / 10 分钟),供图表查询;当前值实时查询。
4. **诊断**:
   - 线程 Dump:`ThreadMXBean.dumpAllThreads(true, true)` + `findDeadlockedThreads()`
   - 类直方图:`DiagnosticCommandMBean`(`com.sun.management:type=DiagnosticCommand`)调用 `gcClassHistogram`,解析文本输出取 Top N
5. **安全**:Spring Security `SecurityFilterChain` —— `/api/**` 需登录,`/login`、静态资源放行;内存用户(bcrypt)由 yml 配置;CSRF 关闭(内网工具)。
6. **前端**:Vue 3 + Vite + vue-router + axios + ECharts。页面:登录页 → 控制台(目标选择器 + 内存/GC/线程/CPU 图表 + 指标卡 + 线程 Dump / 直方图抽屉)。dev 经 vite proxy 访问 `:8080`,生产 `npm run build` 后 `dist` 复制到 `src/main/resources/static`,单 jar 部署。

## 目录结构 / 关键文件

```
JvmEye/
├── pom.xml                                  # Spring Boot 3.3+,java 21,lombok,security
├── README.md
├── PLAN.md
├── src/main/java/com/jvmeeye/
│   ├── JvmEyeApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java              # 表单登录 + 放行规则
│   │   └── WebConfig.java                   # SPA history 模式转发(可选)
│   ├── discovery/
│   │   ├── JvmDiscoveryService.java         # VirtualMachine.list() + agentProperties
│   │   └── dto/JvmProcessInfo.java          # pid/displayName/mainClass/attachable
│   ├── monitor/
│   │   ├── JmxConnectionManager.java        # 活动连接管理(预留多目标)
│   │   ├── JmxTargetSession.java            # 单目标会话:MBean 代理 + 采样缓冲
│   │   ├── MetricsSampler.java              # @Scheduled 1s 采样入环形缓冲
│   │   ├── MetricsQueryService.java         # 当前值 / 历史序列组装
│   │   └── dto/                             # MemoryInfo/GcInfo/ThreadInfo/CpuInfo/ClassInfo/RuntimeInfo/MetricsSnapshot/MetricsHistory
│   ├── diagnostics/
│   │   ├── ThreadDumpService.java           # dumpAllThreads + 死锁检测
│   │   └── HeapHistogramService.java        # DiagnosticCommandMBean.gcClassHistogram
│   ├── controller/
│   │   ├── DiscoveryController.java         # GET  /api/targets
│   │   ├── MonitorController.java           # POST /api/targets/{pid}/connect · DELETE /api/targets/disconnect
│   │   │                                    # GET  /api/metrics/current · GET /api/metrics/history
│   │   └── DiagnosticsController.java       # GET  /api/diagnostics/thread-dump · GET /api/diagnostics/histogram
│   └── exception/GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml                      # server.port=8080 · jvmeeye.user/password · 采样参数
│   └── static/                              # 前端构建产物(运行期存在,.gitkeep 占位)
├── src/test/java/com/jvmeeye/               # 上下文加载、Discovery 解析、Security 规则测试
└── frontend/
    ├── package.json · vite.config.js        # proxy /api → http://localhost:8080
    ├── index.html
    └── src/
        ├── main.js · App.vue · router.js · api.js(axios 实例)
        ├── views/LoginView.vue · DashboardView.vue
        └── components/TargetSelector.vue · MemoryChart.vue · GcChart.vue · ThreadChart.vue
                        · CpuChart.vue · MetricCards.vue · ThreadDumpPanel.vue · HistogramPanel.vue
```

## Reuse

- **JDK 自带能力,无需引库**:`com.sun.tools.attach.VirtualMachine`(jdk.attach 模块,Java 21 直接可用)、`javax.management.*`(JMX)、`com.sun.management.OperatingSystemMXBean`(进程 CPU)、`com.sun.management:type=DiagnosticCommand`(jcmd 能力)。
- Spring Boot 自动配置(Web/Security);前端用 ECharts 官方 vue 封装习惯写法(直接 `echarts.init` 即可,不强依赖封装包)。
- 项目当前为空仓库,无历史代码可复用;本计划即为全部基线。

## Steps

### 阶段 1:项目骨架与安全
- [ ] `pom.xml`:Spring Boot 3.3+、`java.version=21`、依赖 web / security / lombok / test;打包插件
- [ ] `JvmEyeApplication` + `application.yml`(端口、采样间隔与缓冲长度、登录用户)
- [ ] `SecurityConfig`:表单登录、`/api/**` 鉴权、静态资源与 `/login` 放行、CSRF 关闭、内存用户
- [ ] 最小 `index.html` 占位,启动验证:`mvn spring-boot:run` → 8080 可访问并跳转登录

### 阶段 2:JVM 发现与 JMX 连接
- [ ] `JvmDiscoveryService`:`VirtualMachine.list()` + agentProperties(mainClass、jvmArgs、isAttachable),排序去重(排除自身可选)
- [ ] `JmxConnectionManager` + `JmxTargetSession`:connect/disconnect,异常时自动清理;连接状态查询 API
- [ ] `DiscoveryController` / `MonitorController`(connect、disconnect)
- [ ] 用 `curl` + 登录 cookie 验证:能列出本机 JVM 并成功连接本机另一个 Java 进程

### 阶段 3:指标采集与查询
- [ ] dto + `JmxTargetSession` 中缓存各 MBean 代理
- [ ] `MetricsSampler`:`@Scheduled(fixedDelay=1000)` 采样,写入每目标 `ArrayBlockingQueue`/`ConcurrentLinkedQueue` 有界环形缓冲(默认 600 点)
- [ ] `MetricsQueryService`:current(实时)+ history(最近 N 点,按指标序列化给图表)
- [ ] 验证:连接目标后每秒数据推进,GCCount/HeapUsed/ThreadCount 随负载变化

### 阶段 4:诊断能力
- [ ] `ThreadDumpService`:`dumpAllThreads(true,true)`、状态统计、`findDeadlockedThreads` 标记;输出结构化 JSON
- [ ] `HeapHistogramService`:调 `gcClassHistogram`,解析为 `className/instances/bytes` 列表,支持 `topN` 参数
- [ ] 验证:制造死锁/负载的测试进程,dump 与直方图正确返回

### 阶段 5:前端(Vue 3 + Vite + ECharts)
- [ ] `frontend/` 脚手架:`npm create vite`(vue 模板)+ vue-router + axios + echarts
- [ ] `api.js`:axios 实例(`withCredentials`),封装 targets/connect/metrics/diagnostics 调用
- [ ] 登录页(账号密码 → POST `/login`,成功跳 Dashboard;401 处理)
- [ ] Dashboard:目标选择器(列出 `/api/targets`,点击连接)、指标卡、4 张折线图(堆内存、GC 次数/耗时、线程数、CPU),2s 轮询
- [ ] 诊断面板:线程 Dump(可折叠堆栈、死锁高亮)、Top N 类直方图表格
- [ ] vite proxy 联调通过

### 阶段 6:打包与文档
- [ ] 前端 `npm run build` → `dist` 复制到 `src/main/resources/static`(提供 `scripts/build-all.sh` 或 maven-frontend-plugin profile)
- [ ] `mvn package` 产出单 jar,`java -jar` 启动全流程验证(登录 → 发现 → 连接 → 图表 → dump)
- [ ] README:构建、运行、目标 JVM 要求(同用户/同架构、JDK 附件机制说明)、API 列表、配置项
- [ ] 单元/集成测试:Spring 上下文、Security 规则、DiscoveryService 解析逻辑(可 mock VirtualMachine)

## Verification

1. **编译**:`mvn -q clean package` 通过
2. **启动**:`java -jar target/jvme-eye-*.jar`,浏览器打开 `http://localhost:8080`,未登录被重定向到登录页;用 yml 配置的账号登录成功
3. **发现**:另开终端启动一个测试 Java 进程(如 `java -jar` 一个简单死循环程序),`GET /api/targets` 能列出它(pid、mainClass)
4. **连接与指标**:前端选择该进程连接,`/api/metrics/current` 返回完整快照;对测试进程制造内存/线程负载,图表 2s 内可见变化;断开后 API 返回 409/404 合理错误
5. **诊断**:`/api/diagnostics/thread-dump` 能看到测试进程线程与堆栈;`/api/diagnostics/histogram` 返回 Top N 类列表
6. **单 jar 部署**:前端页面由 Spring Boot 直接提供(非 vite dev server),刷新深链不 404

## 已知限制(v1,后续迭代)

- 仅本机 JVM 发现(依赖 attach API,目标需同用户同架构);远程 `host:port` JMX 连接列为后续
- 单目标监控;多目标 = `JmxConnectionManager` 内 map 化 + 前端多卡片视图
- 历史数据仅在内存(重启清空);持久化(SQLite/H2)列为后续
- 无告警;登录为用户名密码内存配置,无角色体系
