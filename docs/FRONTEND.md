# 代码可读性约定(写代码前先读)

这份文件记录本项目"怎么写才看得懂"的约定。它们不是风格癖好,而是之前 review 里
被指出过的问题,后续提交请遵守。

---

## 1. 后端返回:不要用 `Map` 拼 JSON

Controller 只用 Spring 提供的对象:

| 场景 | 写法 |
|---|---|
| 成功 | `ResponseEntity<XxxResult>`,其中 `XxxResult` 是 `com.jvmeeye.controller.dto` 下的 record |
| 失败 | `GlobalExceptionHandler` → `ResponseEntity<ProblemDetail>`(RFC 7807) |
| 只是转发 service 结果 | 直接返回 record(如 `MetricsSnapshot`) |

为什么:字段名写错、字段类型改错在编译期就能发现,前端和 `docs/API.md` 也能对着
record 读;`Map<String, Object>` 三者都不成立。

```java
// ✅ 好
@PostMapping("/targets/{pid}/connect")
public ResponseEntity<ConnectResult> connect(@PathVariable long pid) { ... }

// ❌ 曾经的样子
public Map<String, Object> connect(@PathVariable long pid) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("connected", true);
    ...
}
```

---

## 2. 前端 JS

1. **没有魔法数字。** 阈值、条数、周期一律抽成 `const` 并写在注释里说明为什么是这个值:
   `const HEAP_WARN_PERCENT = 85` / `const MIN_BAR_PERCENT = 2` / `const HISTORY_INTERVAL = 6000`。
2. **请求只走 `src/api.js`。** 组件里不出现 `axios.get`,解包统一由 `payload()` 完成;
   错误提示统一用 `errorMessage(error)`,不要再写 `e?.response?.data?.message`。
3. **重复三次就抽公共函数。** 格式化去 `utils/format.js`,图表配置去 `composables/useEChart.js`,
   轮询/生命周期去 `composables/`。
4. **状态 → class 用映射表**,不要写一长串 `if`:
   ```js
   const STATE_CHIP_CLASS = { RUNNABLE: 'chip-ok', BLOCKED: 'chip-danger' }
   ```
5. **每个 computed / function 上方一行注释,说"为什么"而不是"它是什么"**。
   `/** 死锁线程默认展开,方便直接看堆栈 */` 是有用的注释;
   `/** 获取线程 Dump */` 不是。
6. **同一对象字面量只写一份**,用 `const IDLE_STATUS = {...}` 复用,避免两处写法漂移。

---

## 3. 前端 CSS

1. **颜色、圆角、列宽、高度只能用 `styles.css` 的 token**(`var(--accent)` / `var(--danger-wash)` …)。
   组件里禁止出现 `#hex` 和 `rgba(...)`——这些值散落之后,改主题时根本找不全。
   需要新颜色:在 `:root` 里补一个语义 token,别在组件里写死。
   > 图表跑在 canvas 里读不到 CSS 变量,所以图表配色集中放在 `composables/useEChart.js` 顶部。
2. **跨组件样式才进 `styles.css`,并放进已有的分节**(按钮 / 面板 / 表格 / 表单 / 工具条 …),
   每个分节在文件头的目录里能查到。组件私有的才写 `<style scoped>`。
3. **新写 class 前先搜一遍**。已有通用件:`btn / panel / table / badge / chip / alert / empty /
   spinner / field / toggle / toolbar / toolbar-info / progress / scroll-box / cell-ellipsis / col-*`。
   两个面板里出现同名的 `.toolbar-info` 就是复制粘贴留下的,以后不允许。
4. **模板里不写 `style="width: 46px"`**,列宽用 `.col-rank` / `.col-count` / `.col-state` …
   (列宽一律在 `styles.css` 的"表格与列宽"一节)。
5. **不要用 `!important` 覆盖全局样式**,提高选择器权重:
   `.table tbody tr.row-deadlock:hover` 而不是 `.row-deadlock:hover { ... !important }`。
6. **命名用语义全称**:`.thread-name` / `.detail-row` 可以,`.dump-` / `.hist-` 前缀和
   `share-bar`、`bar-head` 这种要看上下文才懂的缩写不要。
7. 组件私有 class 里也优先复用 token,只有真正的页面专属尺寸(如 `.target-name { max-width: 420px }`)
   才保留字面量。

---

## 4. 改完要做的检查

```bash
cd frontend && npm run build      # 必须无警告通过
mvn -o test                       # 后端契约改动要同步 SecurityRulesTests
bash scripts/build-all.sh          # 打包前同步 dist → src/main/resources/static
```

接口行为变了(字段名、状态码)记得同步 `docs/API.md` 和前端调用方。

---

## 5. 已知遗留(不要求一次改完)

- `views/DashboardView.vue` 仍承担"状态机 + 轮询 + 全部数据流",后续可按
  `composables/useMonitorTarget.js` 拆出连接/断开/刷新逻辑。
- 组件里还有少量视图专属的字面量(`max-width: 420px`、`minmax(520px, 1fr)`),属正常布局参数。
