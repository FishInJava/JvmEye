<template>
  <div class="cards">
    <article class="card">
      <header class="card-head">
        <span class="card-title">堆内存</span>
        <span v-if="heapPercent !== null" class="badge" :class="heapPercent > 85 ? 'badge-danger' : 'badge-ok'">
          {{ heapPercent.toFixed(1) }}%
        </span>
      </header>
      <div class="card-value">{{ formatBytes(memory?.heapUsed) }}</div>
      <div class="card-sub dim">
        committed {{ formatBytes(memory?.heapCommitted) }} / max {{ formatBytes(memory?.heapMax) }}
      </div>
      <div class="progress"><span :style="{ width: progressWidth }"></span></div>
      <dl class="card-extra">
        <div><dt>非堆</dt><dd>{{ formatBytes(memory?.nonHeapUsed) }}</dd></div>
        <div><dt>待回收</dt><dd>{{ memory?.objectPendingFinalizationCount ?? '-' }}</dd></div>
        <div><dt>内存池</dt><dd>{{ memory?.pools?.length ?? 0 }} 个</dd></div>
      </dl>
    </article>

    <article class="card">
      <header class="card-head"><span class="card-title">GC</span></header>
      <div class="card-value">{{ formatNumber(gc?.totalCollectionCount) }}</div>
      <div class="card-sub dim">累计次数 · 总耗时 {{ formatDuration(gc?.totalCollectionTimeMs) }}</div>
      <dl class="card-extra">
        <div><dt>收集器</dt><dd class="mono">{{ collectorNames }}</dd></div>
        <div><dt>最近 GC</dt><dd>{{ lastGcLabel }}</dd></div>
      </dl>
    </article>

    <article class="card">
      <header class="card-head">
        <span class="card-title">线程</span>
        <span v-if="thread?.deadlocked" class="badge badge-danger">检测到死锁</span>
      </header>
      <div class="card-value">{{ thread?.live ?? '-' }}</div>
      <div class="card-sub dim">峰值 {{ thread?.peak ?? '-' }} · 守护 {{ thread?.daemon ?? '-' }} · 累计启动 {{ formatNumber(thread?.totalStarted) }}</div>
      <div class="chips">
        <span v-for="(count, state) in threadStates" :key="state" class="chip" :class="chipClass(state, count)">
          {{ state }} {{ count }}
        </span>
      </div>
    </article>

    <article class="card">
      <header class="card-head"><span class="card-title">进程 CPU</span></header>
      <div class="card-value">{{ formatPercent(cpu?.processCpuLoad) }}</div>
      <div class="card-sub dim">
        系统 {{ formatPercent(cpu?.systemCpuLoad) }} · LoadAverage {{ formatNumber3(cpu?.systemLoadAverage) }} · {{ cpu?.availableProcessors ?? '-' }} 核
      </div>
      <dl class="card-extra">
        <div><dt>CPU 时间</dt><dd>{{ formatDuration(cpu?.processCpuTimeMs) }}</dd></div>
        <div><dt>采样线程</dt><dd>{{ formatDuration(thread?.currentThreadCpuTimeMs) }}</dd></div>
      </dl>
    </article>

    <article class="card">
      <header class="card-head"><span class="card-title">类加载</span></header>
      <div class="card-value">{{ formatNumber(classLoading?.loaded) }}</div>
      <div class="card-sub dim">已加载 · 累计 {{ formatNumber(classLoading?.totalLoaded) }} · 卸载 {{ formatNumber(classLoading?.unloaded) }}</div>
      <dl class="card-extra">
        <div><dt>Verbose</dt><dd>{{ classLoading?.verbose ? '开启' : '关闭' }}</dd></div>
        <div><dt>JIT</dt><dd>{{ compilation ? formatDuration(compilation.totalCompilationTimeMs) : '-' }}</dd></div>
      </dl>
    </article>

    <article class="card">
      <header class="card-head"><span class="card-title">运行时</span></header>
      <div class="card-value card-value-sm">{{ runtime?.jvmVersion || '-' }}</div>
      <div class="card-sub dim" :title="runtime?.vmName">{{ runtime?.vmName || '-' }}</div>
      <dl class="card-extra">
        <div><dt>厂商</dt><dd>{{ runtime?.jvmVendor || '-' }}</dd></div>
        <div><dt>运行时长</dt><dd>{{ runtime?.uptimeText || formatDuration(runtime?.uptimeMs) }}</dd></div>
      </dl>
    </article>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatBytes, formatDuration, formatNumber, formatPercent } from '../utils/format'

const props = defineProps({
  current: { type: Object, default: null }
})

const memory = computed(() => props.current?.memory || {})
const gc = computed(() => props.current?.gc || {})
const thread = computed(() => props.current?.thread || {})
const cpu = computed(() => props.current?.cpu || {})
const classLoading = computed(() => props.current?.classLoading || {})
const compilation = computed(() => props.current?.compilation || null)
const runtime = computed(() => props.current?.runtime || {})

const heapPercent = computed(() => {
  const max = memory.value.heapMax
  if (!max || max <= 0) return null
  return (memory.value.heapUsed / max) * 100
})

const progressWidth = computed(() => `${Math.min(100, heapPercent.value ?? 0).toFixed(1)}%`)

const threadStates = computed(() => {
  const states = thread.value.states || {}
  return Object.fromEntries(Object.entries(states).filter(([, count]) => count > 0))
})

const collectorNames = computed(() => (gc.value.collectors || []).map((c) => c.name).join(' / ') || '-')

const lastGcLabel = computed(() => {
  const last = gc.value.lastGc
  if (!last) return '-'
  return `${last.name} · ${last.durationMs}ms`
})

function chipClass(state, count) {
  if (state === 'BLOCKED' && count > 0) return 'chip-danger'
  if (state === 'RUNNABLE') return 'chip-ok'
  return 'chip-muted'
}

function formatNumber3(value) {
  if (value === null || value === undefined || value < 0) return '-'
  return value.toFixed(2)
}
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.card {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title {
  font-size: 12px;
  color: var(--text-dim);
  letter-spacing: 0.4px;
}

.card-value {
  font-size: 24px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.card-value-sm {
  font-size: 17px;
}

.card-sub {
  font-size: 11px;
}

.progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.progress span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #4c8dff, #22c55e);
  transition: width 0.4s ease;
}

.card-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  margin: 4px 0 0;
}

.card-extra div {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
}

.card-extra dt {
  font-size: 11px;
  color: var(--text-dim);
}

.card-extra dd {
  margin: 0;
  font-size: 11px;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}

.chip {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  border: 1px solid transparent;
}

.chip-ok {
  color: #86efac;
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.25);
}

.chip-danger {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.16);
  border-color: rgba(239, 68, 68, 0.3);
}

.chip-muted {
  color: var(--text-dim);
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.2);
}
</style>
