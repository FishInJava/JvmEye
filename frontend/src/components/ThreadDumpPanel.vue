<template>
  <div class="dump">
    <div class="dump-toolbar">
      <button class="btn btn-primary" :disabled="loading" @click="load">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '采集中…' : '获取线程 Dump' }}
      </button>
      <input v-model.trim="filter" class="filter-input" type="text" placeholder="按线程名 / 状态过滤" :disabled="!dump" />
      <label class="toggle">
        <input v-model="onlyDeadlocked" type="checkbox" :disabled="!dump" />
        只看死锁
      </label>
      <span v-if="dump" class="dim toolbar-info">
        {{ dump.threadCount }} 个线程 · 死锁 {{ dump.deadlockedCount }} · {{ formatTime(dump.timestamp) }}
      </span>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="!dump && !loading" class="empty">点击“获取线程 Dump”查看目标 JVM 的线程与堆栈</div>

    <div v-else-if="dump" class="dump-body">
      <div v-if="filteredThreads.length === 0" class="empty">没有匹配的线程</div>
      <table v-else class="table threads-table">
        <thead>
          <tr>
            <th style="width: 64px">ID</th>
            <th>线程名</th>
            <th style="width: 110px">状态</th>
            <th style="width: 60px">守护</th>
            <th style="width: 76px">阻塞次数</th>
            <th style="width: 76px">等待次数</th>
            <th style="width: 40px"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="thread in filteredThreads" :key="thread.id">
            <tr :class="{ 'row-deadlock': thread.deadlocked }" @click="toggle(thread.id)">
              <td class="mono">{{ thread.id }}</td>
              <td>
                <div class="thread-name" :title="thread.name">{{ thread.name }}</div>
                <div v-if="thread.lockName" class="cell-sub dim" :title="lockTitle(thread)">
                  等待 {{ thread.lockName }}<template v-if="thread.lockOwnerName"> · 持有者 {{ thread.lockOwnerName }}</template>
                </div>
              </td>
              <td><span class="state" :class="stateClass(thread.state)">{{ thread.state }}</span></td>
              <td class="dim">{{ thread.daemon ? '是' : '否' }}</td>
              <td class="dim">{{ thread.blockedCount }}</td>
              <td class="dim">{{ thread.waitedCount }}</td>
              <td class="dim">{{ expanded.has(thread.id) ? '▲' : '▼' }}</td>
            </tr>
            <tr v-if="expanded.has(thread.id)" :key="`${thread.id}-detail`" class="detail-row">
              <td colspan="7">
                <div class="detail">
                  <div v-if="thread.deadlocked" class="alert alert-error detail-deadlock">
                    ⚠ 该线程处于死锁状态
                  </div>
                  <div class="detail-meta">
                    <span>优先级 {{ thread.priority }}</span>
                    <span>inNative: {{ thread.inNative ? '是' : '否' }}</span>
                    <span v-if="thread.lockName">锁: <b class="mono">{{ thread.lockName }}</b></span>
                    <span v-if="thread.lockOwnerName">锁持有者: <b>{{ thread.lockOwnerName }}</b></span>
                    <span v-if="thread.blockedTimeMs >= 0">阻塞累计 {{ thread.blockedTimeMs }}ms</span>
                    <span v-if="thread.waitedTimeMs >= 0">等待累计 {{ thread.waitedTimeMs }}ms</span>
                  </div>
                  <div class="stack-title">堆栈({{ thread.stack.length }} 帧)</div>
                  <ol class="stack">
                    <li v-for="(frame, index) in thread.stack" :key="index" class="mono">
                      <span class="frame-class">{{ frame.className }}.{{ frame.methodName }}</span>
                      <span class="frame-loc">({{ frame.fileName || 'Unknown Source' }}:{{ frame.nativeMethod ? 'native' : frame.lineNumber }})</span>
                    </li>
                  </ol>
                  <div v-if="thread.lockedMonitors?.length" class="locks">
                    <div class="stack-title">持有的监视器锁</div>
                    <ul>
                      <li v-for="(lock, index) in thread.lockedMonitors" :key="index" class="mono">
                        {{ lock.identity }}<template v-if="lock.stackFrame"> — {{ lock.stackFrame }}</template>
                      </li>
                    </ul>
                  </div>
                  <div v-if="thread.lockedSynchronizers?.length" class="locks">
                    <div class="stack-title">持有的同步器</div>
                    <ul>
                      <li v-for="(lock, index) in thread.lockedSynchronizers" :key="index" class="mono">{{ lock.identity }}</li>
                    </ul>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { diagnosticsApi, errorMessage } from '../api'
import { formatTime } from '../utils/format'

const dump = ref(null)
const loading = ref(false)
const error = ref('')
const filter = ref('')
const onlyDeadlocked = ref(false)
const expanded = ref(new Set())

const filteredThreads = computed(() => {
  if (!dump.value) return []
  const keyword = filter.value.toLowerCase()
  return dump.value.threads.filter((thread) => {
    if (onlyDeadlocked.value && !thread.deadlocked) return false
    if (!keyword) return true
    return thread.name.toLowerCase().includes(keyword) || thread.state.toLowerCase().includes(keyword)
  })
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    dump.value = await diagnosticsApi.threadDump()
    if (dump.value.deadlockedCount > 0) {
      for (const id of dump.value.deadlockedThreadIds) {
        expanded.value.add(id)
      }
    }
  } catch (e) {
    error.value = `获取线程 Dump 失败:${errorMessage(e)}`
  } finally {
    loading.value = false
  }
}

function toggle(id) {
  const next = new Set(expanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expanded.value = next
}

function stateClass(state) {
  if (state === 'BLOCKED') return 'state-danger'
  if (state === 'RUNNABLE') return 'state-ok'
  if (state === 'TIMED_WAITING' || state === 'WAITING') return 'state-warn'
  return 'state-muted'
}

function lockTitle(thread) {
  return `锁:${thread.lockName}${thread.lockOwnerName ? ` / 持有者:${thread.lockOwnerName}` : ''}`
}
</script>

<style scoped>
.dump {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.dump-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-input {
  flex: 1;
  min-width: 160px;
  padding: 7px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(11, 15, 24, 0.7);
  color: var(--text);
  font-size: 13px;
  outline: none;
}

.filter-input:focus {
  border-color: var(--accent);
}

.toggle {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-dim);
  cursor: pointer;
}

.toolbar-info {
  font-size: 12px;
}

.dump-body {
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.threads-table {
  font-size: 12px;
}

.threads-table tbody tr {
  cursor: pointer;
}

.row-deadlock {
  background: rgba(239, 68, 68, 0.1);
}

.row-deadlock:hover {
  background: rgba(239, 68, 68, 0.16) !important;
}

.thread-name {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state {
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 11px;
}

.state-ok {
  color: #86efac;
  background: rgba(34, 197, 94, 0.14);
}

.state-warn {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.14);
}

.state-danger {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.18);
}

.state-muted {
  color: var(--text-dim);
  background: rgba(148, 163, 184, 0.12);
}

.detail-row td {
  background: rgba(11, 15, 24, 0.55);
  padding: 12px;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-deadlock {
  margin: 0;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 11px;
  color: var(--text-dim);
}

.stack-title {
  font-size: 11px;
  color: var(--text-dim);
}

.stack {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 260px;
  overflow: auto;
}

.stack li {
  font-size: 11px;
  line-height: 1.6;
  word-break: break-all;
}

.frame-class {
  color: #cbd5f5;
}

.frame-loc {
  color: #64748b;
}

.locks ul {
  margin: 4px 0 0;
  padding-left: 20px;
}

.locks li {
  font-size: 11px;
  color: #cbd5f5;
}
</style>
