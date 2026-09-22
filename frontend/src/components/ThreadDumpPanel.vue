<template>
  <div class="dump">
    <div class="toolbar">
      <button class="btn btn-primary" :disabled="loading" @click="load">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '采集中…' : '获取线程 Dump' }}
      </button>
      <input v-model.trim="filter" class="form-control filter-input" type="text" placeholder="按线程名 / 状态过滤" :disabled="!dump" />
      <label class="toggle">
        <input v-model="onlyDeadlocked" type="checkbox" :disabled="!dump" />
        只看死锁
      </label>
      <span v-if="dump" class="toolbar-info">
        {{ dump.threadCount }} 个线程 · 死锁 {{ dump.deadlockedCount }} · {{ formatTime(dump.timestamp) }}
      </span>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="!dump && !loading" class="empty">点击“获取线程 Dump”查看目标 JVM 的线程与堆栈</div>

    <div v-else-if="dump" class="scroll-box">
      <div v-if="filteredThreads.length === 0" class="empty">没有匹配的线程</div>
      <table v-else class="table dump-table">
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th>线程名</th>
            <th class="col-state">状态</th>
            <th class="col-flag">守护</th>
            <th class="col-count">阻塞次数</th>
            <th class="col-count">等待次数</th>
            <th class="col-toggle"></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="thread in filteredThreads" :key="thread.id">
            <tr :class="{ 'row-deadlock': thread.deadlocked }" @click="toggle(thread.id)">
              <td class="mono">{{ thread.id }}</td>
              <td>
                <div class="thread-name cell-ellipsis" :title="thread.name">{{ thread.name }}</div>
                <div v-if="thread.lockName" class="cell-sub dim cell-ellipsis" :title="lockTitle(thread)">
                  等待 {{ thread.lockName }}<template v-if="thread.lockOwnerName"> · 持有者 {{ thread.lockOwnerName }}</template>
                </div>
              </td>
              <td><span class="chip" :class="stateClass(thread.state)">{{ thread.state }}</span></td>
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
/** 已展开的线程 id 集合(点一行展开详情)。 */
const expanded = ref(new Set())

/** 按关键字过滤 + 只看死锁。关键字同时匹配线程名和状态。 */
const filteredThreads = computed(() => {
  if (!dump.value || !dump.value.threads) return []
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
    // 死锁线程默认展开,方便直接看堆栈
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
  // 换成新 Set,保证 ref 能感知到变化
  expanded.value = next
}

/** 线程状态 → chip 配色。 */
const STATE_CHIP_CLASS = {
  RUNNABLE: 'chip-ok',
  TIMED_WAITING: 'chip-warn',
  WAITING: 'chip-warn',
  BLOCKED: 'chip-danger'
}

function stateClass(state) {
  return STATE_CHIP_CLASS[state] || 'chip-muted'
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

/* 过滤框占满剩余宽度,给线程名留地方 */
.filter-input {
  flex: 1;
  min-width: 160px;
}

.dump-table {
  font-size: 12px;
}

.dump-table tbody tr {
  cursor: pointer;
}

/*
 * 死锁行整行淡红,优先级要高于全局 .table tbody tr:hover,
 * 所以加一层 .row-deadlock 提高选择器权重,不用 !important。
 */
.table tbody tr.row-deadlock,
.table tbody tr.row-deadlock:hover {
  background: var(--danger-wash);
}

.thread-name {
  max-width: 240px;
}

.cell-sub {
  font-size: 11px;
  max-width: 240px;
}

/* 展开行:比面板底色更暗,区分主次 */
.detail-row td {
  background: var(--panel-sunken);
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
  max-height: var(--stack-max-height);
  overflow: auto;
}

.stack li {
  font-size: 11px;
  line-height: 1.6;
  word-break: break-all;
}

.frame-class {
  color: var(--code-text);
}

.frame-loc {
  color: var(--text-faint);
}

.locks ul {
  margin: 4px 0 0;
  padding-left: 20px;
}

.locks li {
  font-size: 11px;
  color: var(--code-text);
}
</style>
