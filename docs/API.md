# JvmEye API 契约

Base URL: `http://localhost:8080`

所有 `/api/**` 需要登录(基于 Session Cookie,`JSESSIONID`)。
未登录访问 `/api/**` 返回 `302` 跳转 `/login`;前端应统一处理 `401/302` → 跳登录页。

错误响应统一结构(RFC 7807 `ProblemDetail`,由 Spring `org.springframework.http.ProblemDetail` 生成):

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "detail": "当前没有已连接的监控目标...", "instance": "/api/metrics/current", "timestamp": 1727000000000 }
```

| 字段 | 来源 | 说明 |
|---|---|---|
| `title` | Spring | HTTP 状态描述,如 `Conflict` / `Not Found` |
| `status` | Spring | HTTP 状态码 |
| `detail` | 后端 | 错误原因(对应旧字段 `message`) |
| `instance` | 后端 | 触发错误的请求路径(对应旧字段 `path`) |
| `timestamp` | 后端 | 错误发生时间(epoch 毫秒),额外补充字段 |

> 成功响应均为扁平 JSON,字段即后端 record 的组件名;Controller 不使用 `Map` 拼装,字段含义以本文档与 `com.jvmeeye.controller.dto` 下的 record 定义为准。

---

## 1. 认证

### POST `/login`

Spring Security 表单登录,`application/x-www-form-urlencoded`。

| 参数 | 说明 |
|---|---|
| `username` | 用户名(默认 `admin`) |
| `password` | 密码(默认 `admin123`) |

- 成功:`302` → `/`(跟随重定向后拿到首页 HTML),并种下 `JSESSIONID`
- 失败:`302` → `/login?error`

### POST `/logout`

退出登录,`302` → `/login?logout`。

---

## 2. 发现

### GET `/api/targets`

列出本机可发现的 JVM。

| Query | 类型 | 默认 | 说明 |
|---|---|---|---|
| `includeSelf` | boolean | `false` | 是否包含 JvmEye 自身 |

```json
{
  "timestamp": 1727000000000,
  "count": 3,
  "targets": [
    {
      "pid": 25924,
      "displayName": "Target",
      "mainClass": "Target",
      "mainArgs": "-Xmx512m",
      "jvmVersion": "21.0.12",
      "jvmName": "OpenJDK 64-Bit Server VM",
      "javaHome": "/usr/lib/jvm/java-21-openjdk-amd64",
      "user": "1000",
      "attachable": true,
      "self": false,
      "reason": null
    }
  ]
}
```

> `attachable=false` 时 `reason` 说明原因(通常是"目标进程属于其他用户,需要同用户或 root 运行")。

---

## 3. 连接管理

### POST `/api/targets/{pid}/connect`

```json
{ "connected": true, "pid": 25924, "displayName": "Target", "connectedAt": 1727000000000 }
```

错误:`404` pid 不存在;`409` attach/连接失败(权限、架构、目标已退出)。

### DELETE `/api/targets/{pid}/disconnect`

```json
{ "connected": false, "pid": 25924, "closed": 1 }
```

### DELETE `/api/targets/disconnect`

断开全部:`{ "connected": false, "closed": 1 }`

### GET `/api/monitor/status`

```json
{ "connected": true, "pid": 25924, "displayName": "Target", "bufferedPoints": 12, "connectedAt": 1727000000000, "closeReason": null }
```

---

## 4. 指标

### GET `/api/metrics/current`

```json
{
  "timestamp": 1727000000000,
  "pid": 25924,
  "displayName": "Target",
  "runtime": {
    "jvmName": "25924@host", "jvmVersion": "21.0.12+8-1_22.04", "jvmVendor": "Ubuntu",
    "specVersion": "21", "vmName": "OpenJDK 64-Bit Server VM", "vmVendor": "Ubuntu",
    "managementSpecVersion": "1.2", "startTime": 1726999000000, "uptimeMs": 1000000,
    "uptimeText": "00:16:40", "inputArguments": ["-Xmx512m"]
  },
  "memory": {
    "heapUsed": 178257920, "heapCommitted": 536870912, "heapMax": 536870912, "heapInit": 33554432,
    "heapUsedPercent": 0.332,
    "nonHeapUsed": 22020096, "nonHeapCommitted": 27262976, "nonHeapMax": -1,
    "objectPendingFinalizationCount": 0,
    "pools": [
      { "name": "G1 Eden Space", "type": "HEAP", "used": 1, "committed": 2, "max": 3,
        "peakUsed": 4, "peakMax": 5, "usageThreshold": null }
    ]
  },
  "gc": {
    "collectors": [ { "name": "G1 Young Generation", "collectionCount": 12, "collectionTimeMs": 87 } ],
    "totalCollectionCount": 12,
    "totalCollectionTimeMs": 87,
    "lastGc": { "name": "G1 Young Generation", "id": 12, "startTime": 1726999999000, "endTime": 1726999999050, "durationMs": 50 }
  },
  "thread": {
    "live": 30, "daemon": 24, "peak": 33, "totalStarted": 40,
    "currentThreadCpuTimeMs": 120, "currentThreadUserTimeMs": 100,
    "states": { "NEW": 0, "RUNNABLE": 8, "BLOCKED": 0, "WAITING": 12, "TIMED_WAITING": 10, "TERMINATED": 0 },
    "deadlocked": false, "deadlockedCount": 0, "deadlockedThreadIds": []
  },
  "cpu": {
    "processCpuLoad": 0.05, "systemCpuLoad": 0.11, "systemLoadAverage": 0.42,
    "availableProcessors": 8, "processCpuTimeMs": 3456
  },
  "classLoading": { "loaded": 5421, "totalLoaded": 5421, "unloaded": 0, "verbose": false },
  "compilation": { "name": "HotSpot 64-Bit Tiered Compilers", "totalCompilationTimeMs": 12345 }
}
```

> `max = -1` 表示未设置上限;`processCpuLoad = -1` 表示最近一次采样不可用(首次采集常见)。
> `compilation` / `lastGc` 在 `non_null` 序列化策略下可能整体缺失。

### GET `/api/metrics/history?points=300`

```json
{
  "pid": 25924, "displayName": "Target",
  "from": 1727000000000, "to": 1727000030000,
  "intervalMs": 1000, "requestedPoints": 300, "returnedPoints": 3,
  "points": [ { "...": "与 /api/metrics/current 结构一致" } ]
}
```

> 未连接目标时:`409`。

---

## 5. 诊断

### GET `/api/diagnostics/thread-dump`

```json
{
  "timestamp": 1727000000000, "pid": 25924, "displayName": "Target",
  "threadCount": 30, "deadlockedCount": 2, "deadlockedThreadIds": [31, 32],
  "threads": [
    {
      "id": 1, "name": "main", "state": "RUNNABLE", "priority": 5, "daemon": false,
      "suspended": false, "inNative": false,
      "blockedCount": 0, "blockedTimeMs": -1, "waitedCount": 1, "waitedTimeMs": 0,
      "lockName": "0x0000000700000001", "lockOwnerName": "worker-1",
      "deadlocked": true,
      "stack": [ { "className": "java.lang.Object", "methodName": "wait", "fileName": "Object.java", "lineNumber": 502, "nativeMethod": false } ],
      "lockedMonitors": [ { "identity": "java.lang.Object#12345678", "stackDepth": 2, "stackFrame": "Target.main(Target.java:10)" } ],
      "lockedSynchronizers": []
    }
  ]
}
```

### GET `/api/diagnostics/histogram?top=30`

```json
{
  "timestamp": 1727000000000, "pid": 25924, "displayName": "Target", "top": 30,
  "totalInstances": 1234567, "totalBytes": 987654321, "totalClasses": 4211,
  "entries": [ { "rank": 1, "name": "[B (java.base@21.0.12)", "instances": 19878, "bytes": 67770584 } ]
}
```

> **该接口会触发目标 JVM 一次 Full GC**,不宜高频轮询。
