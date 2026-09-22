<template>
  <div class="dashboard">
    <section class="topbar panel">
      <div class="target-block">
        <template v-if="status.connected">
          <span class="status-dot status-dot-live"></span>
          <div class="target-meta">
            <div class="target-title">
              <span class="target-pid">PID {{ status.pid }}</span>
              <span class="target-name" :title="status.displayName">{{ status.displayName || '未知目标' }}</span>
              <span class="badge badge-ok">已连接</span>
            </div>
            <div class="target-sub dim">
              缓冲 {{ status.bufferedPoints ?? 0 }} 点 · 采样间隔 {{ intervalLabel }} · 更新于 {{ formatTime(lastUpdated) }}
            </div>
          </div>
        </template>
        <template v-else>
          <span class="status-dot status-dot-off"></span>
          <div class="target-meta">
            <div class="target-title">
              <span class="target-name">未连接目标</span>
              <span class="badge badge-muted">空闲</span>
            </div>
            <div class="target-sub dim">从本机 JVM 列表中选择一个进程开始监控</div>
          </div>
        </template>
      </div>

      <div class="actions">
        <button class="btn" :disabled="refreshing" @click="refreshNow">手动刷新</button>
        <button class="btn" :disabled="refreshing" @click="reloadTargets">重新发现</button>
        <button v-if="status.connected" class="btn btn-danger" :disabled="disconnecting" @click="onDisconnect">
          {{ disconnecting ? '断开中…' : '断开连接' }}
        </button>
        <button class="btn" @click="onLogout">退出登录</button>
      </div>
    </section>

    <p v-if="error" class="alert alert-error dashboard-error">{{ error }}</p>

    <TargetSelector
      v-if="!status.connected"
      :targets="targets"
      :loading="targetsLoading"
      :connecting-pid="connectingPid"
      @connect="onConnect"
      @reload="reloadTargets"
    />

    <template v-else>
      <MetricCards :current="current" />

      <div class="chart-grid">
        <section class="panel chart-panel">
          <h2 class="panel-title">堆内存 <span class="hint">used / committed / max(非堆 used)</span></h2>
          <MemoryChart :points="historyPoints" />
        </section>
        <section class="panel chart-panel">
          <h2 class="panel-title">GC <span class="hint">累计次数 / 累计耗时</span></h2>
          <GcChart :points="historyPoints" />
        </section>
        <section class="panel chart-panel">
          <h2 class="panel-title">线程 <span class="hint">live / daemon / peak</span></h2>
          <ThreadChart :points="historyPoints" />
        </section>
        <section class="panel chart-panel">
          <h2 class="panel-title">CPU 与负载 <span class="hint">进程 / 系统 / LoadAverage</span></h2>
          <CpuChart :points="historyPoints" />
        </section>
      </div>

      <div class="diagnostics-grid">
        <section class="panel">
          <h2 class="panel-title">诊断 · 线程 Dump</h2>
          <ThreadDumpPanel />
        </section>
        <section class="panel">
          <h2 class="panel-title">诊断 · 类直方图</h2>
          <HistogramPanel />
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import TargetSelector from '../components/TargetSelector.vue'
import MetricCards from '../components/MetricCards.vue'
import MemoryChart from '../components/MemoryChart.vue'
import GcChart from '../components/GcChart.vue'
import ThreadChart from '../components/ThreadChart.vue'
import CpuChart from '../components/CpuChart.vue'
import ThreadDumpPanel from '../components/ThreadDumpPanel.vue'
import HistogramPanel from '../components/HistogramPanel.vue'
import { errorMessage, metricsApi, monitorApi, targetsApi } from '../api'
import { formatTime } from '../utils/format'
import { usePolling } from '../composables/usePolling'

/** 实时指标轮询周期(ms)。 */
const CURRENT_INTERVAL = 2000
/** 历史序列轮询周期(ms)。实时值 2s 一次,折线图 6s 拉一次就够。 */
const HISTORY_INTERVAL = 6000
/** 每次请求的历史点数,与后端缓冲容量一致。 */
const HISTORY_POINTS = 300

/** 没有连接目标时的状态占位;连上 / 断开都以它为基准,避免两处写法不一致。 */
const IDLE_STATUS = { connected: false, pid: null, displayName: null, bufferedPoints: 0, connectedAt: null }

const router = useRouter()

// 两个轮询任务:实时指标 2s、历史序列 6s
const { start: startPolling, stop: stopPolling } = usePolling([
  { intervalMs: CURRENT_INTERVAL, run: loadCurrent },
  { intervalMs: HISTORY_INTERVAL, run: loadHistory }
])

const status = ref({ ...IDLE_STATUS })
const targets = ref([])
const targetsLoading = ref(false)
const connectingPid = ref(null)
const disconnecting = ref(false)
const current = ref(null)
const historyPoints = ref([])
const lastUpdated = ref(null)
const error = ref('')
const refreshing = ref(false)

const intervalLabel = computed(() => `${CURRENT_INTERVAL / 1000}s`)

async function refreshStatus() {
  try {
    status.value = await monitorApi.status()
    return status.value.connected
  } catch (e) {
    return false
  }
}

async function reloadTargets() {
  targetsLoading.value = true
  error.value = ''
  try {
    targets.value = await targetsApi.list(false)
  } catch (e) {
    error.value = `获取本机 JVM 列表失败:${errorMessage(e)}`
  } finally {
    targetsLoading.value = false
  }
}

async function loadCurrent() {
  try {
    current.value = await metricsApi.current()
    lastUpdated.value = Date.now()
    error.value = ''
  } catch (e) {
    if (e?.response?.status === 409) {
      // 目标已断开:回到选择态
      handleTargetLost('目标连接已断开(可能进程已退出),请重新选择')
      return
    }
    error.value = `获取指标失败:${errorMessage(e)}`
  }
}

async function loadHistory() {
  try {
    const data = await metricsApi.history(HISTORY_POINTS)
    historyPoints.value = data.points || []
  } catch (e) {
    if (e?.response?.status === 409) {
      handleTargetLost('目标连接已断开(可能进程已退出),请重新选择')
    }
  }
}

/** 目标断开了(进程退出 / 连接被关闭):回到选择目标界面。 */
function handleTargetLost(message) {
  stopPolling()
  status.value = { ...IDLE_STATUS }
  current.value = null
  historyPoints.value = []
  error.value = message
  reloadTargets()
}

async function refreshNow() {
  refreshing.value = true
  try {
    await Promise.all([loadCurrent(), loadHistory(), refreshStatus()])
  } finally {
    refreshing.value = false
  }
}

async function onConnect(pid) {
  connectingPid.value = pid
  error.value = ''
  try {
    const result = await monitorApi.connect(pid)
    status.value = { ...status.value, ...result, connected: true, bufferedPoints: 0 }
    historyPoints.value = []
    await Promise.all([loadCurrent(), loadHistory()])
    startPolling()
  } catch (e) {
    error.value = `连接 PID ${pid} 失败:${errorMessage(e)}`
  } finally {
    connectingPid.value = null
  }
}

async function onDisconnect() {
  disconnecting.value = true
  try {
    await monitorApi.disconnectAll()
    handleTargetLost('')
    error.value = ''
  } catch (e) {
    error.value = `断开失败:${errorMessage(e)}`
  } finally {
    disconnecting.value = false
  }
}

async function onLogout() {
  try {
    await axios.post('/logout', null, { withCredentials: true })
  } catch (e) {
    // 忽略,直接回登录页
  }
  router.replace('/login')
}

async function initialise() {
  stopPolling()
  const connected = await refreshStatus()
  if (connected) {
    await Promise.all([loadCurrent(), loadHistory()])
    startPolling()
  } else {
    await reloadTargets()
  }
}

/** keep-alive 命中时(如登录后从 /login 返回)不会再次 onMounted,需要主动补齐初始化。 */
let wasDeactivated = false

onMounted(() => {
  initialise()
})

onActivated(() => {
  if (!wasDeactivated) return
  wasDeactivated = false
  initialise()
})

onDeactivated(() => {
  wasDeactivated = true
  stopPolling()
})

onBeforeUnmount(() => {
  stopPolling()
})

// 供模板使用
defineOptions({ name: 'DashboardView' })
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.target-block {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: none;
}

.status-dot-live {
  background: var(--success);
  box-shadow: 0 0 0 4px var(--ok-bg);
  animation: pulse 2s ease-in-out infinite;
}

.status-dot-off {
  background: var(--text-faint);
  box-shadow: 0 0 0 4px var(--muted-bg);
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

.target-meta {
  min-width: 0;
}

.target-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.target-pid {
  font-family: var(--mono);
  font-size: 12px;
  color: var(--accent);
}

.target-name {
  font-weight: 600;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-sub {
  font-size: 12px;
  margin-top: 2px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dashboard-error {
  margin: 0;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(460px, 1fr));
  gap: 16px;
}

.chart-panel {
  min-width: 0;
}

.chart-panel :deep(.chart) {
  width: 100%;
  height: var(--chart-height);
}

.diagnostics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(520px, 1fr));
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .chart-grid,
  .diagnostics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
