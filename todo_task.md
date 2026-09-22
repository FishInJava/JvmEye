<!-- pi-todo:mode off -->
# Tasks

## 2026-09-22 · Controller层的返回不要用Map,用Spring中提供的对象

- [已完成] 梳理所有 Controller，找出返回 Map 的接口 · 2026-09-22 13:16 · 共 4 处使用 Map：MonitorController(connect/disconnect/disconnectAll)、DiscoveryController(list)、GlobalExceptionHandler(错误体)。MetricsController/DiagnosticsController 已是类型化 record。

## 2026-09-22 · Controller层的返回不要用Map,用Spring中提供的对象

- [已完成] 统一封装为 ResponseEntity/统一响应体对象，并同步修改前端解析逻辑 · 2026-09-22 13:19 · MonitorController×3 / DiscoveryController×1 改为 ResponseEntity+record(dto 包);GlobalExceptionHandler 改为 ResponseEntity<ProblemDetail>(RFC 7807);同步更新 SecurityRulesTests、api.js errorMessage、docs/API.md。mvn test 11/11 通过。

## 2026-09-22 · 前端代码为什么js和css这么难懂?源码阶段不要这样搞

- [已完成] 审查前端 js/css 的可读性问题（命名、结构、魔法值、注释缺失） · 2026-09-22 13:43 · 确诊 6 类可读性问题:颜色/尺寸字面量散落(26 处 hex+rgba,与 :root 的 token 并存)、.toolbar-info/.filter-input/.top-select 在两个面板复制粘贴、模板内联 style=width 定列宽、11 处魔法数字、!important 提权重、命名靠上下文(dump-/hist-/share-bar)。另发现全局 .field 与登录页局部类撞名会导致样式串台,已改名为 .form-control 并让登录页复用。

## 2026-09-22 · 前端代码为什么js和css这么难懂?源码阶段不要这样搞

- [已完成] 提出并执行可读性重构方案（拆分/命名/注释规范） · 2026-09-22 13:44 · styles.css 重排为 11 节+目录,新增语义 token(状态色三件套/圆角/底槽/滚动高度/图表高度)、复用件(.form-control/.toggle/.toolbar/.toolbar-info/.progress/.scroll-box/.cell-ellipsis/.col-*/.chip);图表公共配置抽到 useEChart(lineSeries/barSeries/timeAxis/baseLegend/chartColors);轮询抽成 composables/usePolling;新增 docs/FRONTEND.md 可读性约定并更新 README 结构。组件内 hex/rgba 仅剩 1 处(登录卡投影)。前端构建 + mvn test(11/11)+ 一键打包 + 起 jar 实测接口均通过。

## 2026-09-22 14:05 · Controllerc层的对象的都统一包装，检查所有 Controller 代码并修改

- [已完成] 审查并统一所有 Controller 返回 ResponseEntity（含 MetricsController / DiagnosticsController） · 2026-09-22 16:15 · 5 个 Controller 已全部 ResponseEntity 化:MonitorController×5 / DiscoveryController / MetricsController×2 / DiagnosticsController×2 / GlobalExceptionHandler(ProblemDetail)。Metrics/Diagnostics 仅包 ResponseEntity.ok(record),未加外层信封,HTTP 响应体逐字段不变;统一了 4 个类注释说明响应体来源与错误约定。mvn test 11/11 通过。
- [已完成] 验证：mvn test + 确认前端/api 文档无需同步改动（HTTP 响应体本身不变） · 2026-09-22 16:17 · mvn test 11/11 通过;起 jar 实测 5 个接口:目标列表/连接/status/metrics·current+history/diagnostics·thread-dump+histogram 响应体与改造前逐字段一致(未包信封,仅外层 ResponseEntity),未连接时仍为 ProblemDetail 409。前端 api.js 只取 r.data、docs/API.md 文档描述的仍是原字段,均无需改动。
