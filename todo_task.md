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
