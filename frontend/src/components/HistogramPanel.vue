<template>
  <div class="histogram">
    <div class="toolbar">
      <button class="btn btn-primary" :disabled="loading" @click="load">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '采集中…' : '获取类直方图' }}
      </button>
      <select v-model.number="top" class="form-control top-select" :disabled="loading">
        <option v-for="option in topOptions" :key="option" :value="option">Top {{ option }}</option>
      </select>
      <span v-if="histogram" class="toolbar-info">
        {{ histogram.totalClasses }} 个类 · 共 {{ formatBytes(histogram.totalBytes) }} / {{ formatNumber(histogram.totalInstances) }} 实例
      </span>
    </div>

    <p class="alert alert-info hist-warning">
      ⚠ 该操作等价于 <b>jcmd GC.class_histogram</b>,会在目标 JVM 触发一次 Full GC,请谨慎高频使用。
    </p>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="!histogram && !loading" class="empty">点击“获取类直方图”查看目标 JVM 的对象分布</div>

    <div v-else-if="histogram" class="scroll-box">
      <div class="bar-head">
        <span>Top {{ histogram.entries.length }} · 按占用字节降序</span>
        <span class="dim">{{ formatTime(histogram.timestamp) }}</span>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th class="col-rank">#</th>
            <th>类名(模块)</th>
            <th class="col-count">实例数</th>
            <th class="col-bytes">占用</th>
            <th class="col-share">占比</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in histogram.entries" :key="entry.rank">
            <td class="dim mono">{{ entry.rank }}</td>
            <td class="cell-name cell-ellipsis" :title="entry.name">{{ entry.name }}</td>
            <td class="mono">{{ formatNumber(entry.instances) }}</td>
            <td class="mono">{{ formatBytes(entry.bytes) }}</td>
            <td>
              <div class="share">
                <!-- 过渡条复用全局 .progress,只换颜色和粗细 -->
                <div class="progress share-bar"><span :style="{ width: shareWidth(entry.bytes) }"></span></div>
                <span class="share-text">{{ shareText(entry.bytes) }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { diagnosticsApi, errorMessage } from '../api'
import { formatBytes, formatNumber, formatTime } from '../utils/format'

/** 可选返回条数。 */
const topOptions = [20, 30, 50, 100]
const DEFAULT_TOP = 30

/** 占用比条的最小可见宽度:否则小占比的类看起来像是 0。 */
const MIN_BAR_PERCENT = 2

const top = ref(DEFAULT_TOP)
const histogram = ref(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    histogram.value = await diagnosticsApi.histogram(top.value)
  } catch (e) {
    error.value = `获取类直方图失败:${errorMessage(e)}`
  } finally {
    loading.value = false
  }
}

/** 该类字节数占总字节数的比例。 */
function shareText(bytes) {
  const total = histogram.value?.totalBytes
  if (!total) return '-'
  return `${((bytes / total) * 100).toFixed(2)}%`
}

/** 占用比条宽度:以当前最大类为 100%,并保证至少有 MIN_BAR_PERCENT 可见。 */
function shareWidth(bytes) {
  const entries = histogram.value?.entries
  if (!entries?.length) return '0%'
  const max = Math.max(...entries.map((entry) => entry.bytes), 1)
  return `${Math.max(MIN_BAR_PERCENT, (bytes / max) * 100).toFixed(1)}%`
}
</script>

<style scoped>
.histogram {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* 下拉框在工具条里不该被拉伸 */
.top-select {
  width: auto;
}

.hist-warning {
  margin: 0;
  font-size: 12px;
}

/* 表头悬浮:结果区内部滚动,横向标题留底 */
.bar-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  font-size: 11px;
  color: var(--text-dim);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  background: var(--panel);
  z-index: 1;
}

.cell-name {
  max-width: 320px;
  font-family: var(--mono);
  font-size: 11px;
}

.share {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 蓝→紫,跟内存占用条(蓝→绿)区分开 */
.share-bar {
  --progress-end: var(--accent-grad-end);
  --progress-height: 5px;
  flex: 1;
}

.share-text {
  font-size: 11px;
  color: var(--text-dim);
  min-width: 46px;
  text-align: right;
}
</style>
