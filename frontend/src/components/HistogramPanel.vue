<template>
  <div class="histogram">
    <div class="hist-toolbar">
      <button class="btn btn-primary" :disabled="loading" @click="load">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '采集中…' : '获取类直方图' }}
      </button>
      <select v-model.number="top" class="top-select" :disabled="loading">
        <option v-for="option in topOptions" :key="option" :value="option">Top {{ option }}</option>
      </select>
      <span v-if="histogram" class="dim toolbar-info">
        {{ histogram.totalClasses }} 个类 · 共 {{ formatBytes(histogram.totalBytes) }} / {{ formatNumber(histogram.totalInstances) }} 实例
      </span>
    </div>

    <p class="alert alert-info hist-warning">
      ⚠ 该操作等价于 <b>jcmd GC.class_histogram</b>,会在目标 JVM 触发一次 Full GC,请谨慎高频使用。
    </p>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="!histogram && !loading" class="empty">点击“获取类直方图”查看目标 JVM 的对象分布</div>

    <div v-else-if="histogram" class="hist-body">
      <div class="bar-head">
        <span>Top {{ histogram.entries.length }} · 按占用字节降序</span>
        <span class="dim">{{ formatTime(histogram.timestamp) }}</span>
      </div>
      <table class="table">
        <thead>
          <tr>
            <th style="width: 46px">#</th>
            <th>类名(模块)</th>
            <th style="width: 100px">实例数</th>
            <th style="width: 110px">占用</th>
            <th style="width: 120px">占比</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in histogram.entries" :key="entry.rank">
            <td class="dim mono">{{ entry.rank }}</td>
            <td class="cell-name" :title="entry.name">{{ entry.name }}</td>
            <td class="mono">{{ formatNumber(entry.instances) }}</td>
            <td class="mono">{{ formatBytes(entry.bytes) }}</td>
            <td>
              <div class="share">
                <div class="share-bar"><span :style="{ width: shareWidth(entry.bytes) }"></span></div>
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

const topOptions = [20, 30, 50, 100]
const top = ref(30)
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

function shareText(bytes) {
  if (!histogram.value || !histogram.value.totalBytes) return '-'
  return `${((bytes / histogram.value.totalBytes) * 100).toFixed(2)}%`
}

function shareWidth(bytes) {
  if (!histogram.value || !histogram.value.totalBytes) return '0%'
  const max = Math.max(...histogram.value.entries.map((e) => e.bytes), 1)
  return `${Math.max(2, (bytes / max) * 100).toFixed(1)}%`
}
</script>

<style scoped>
.histogram {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.hist-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.top-select {
  padding: 7px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--text);
  font-size: 13px;
}

.toolbar-info {
  font-size: 12px;
}

.hist-warning {
  margin: 0;
  font-size: 12px;
}

.hist-body {
  border: 1px solid var(--border);
  border-radius: 10px;
  max-height: 420px;
  overflow: auto;
}

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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--mono);
  font-size: 11px;
}

.share {
  display: flex;
  align-items: center;
  gap: 6px;
}

.share-bar {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.share-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #4c8dff, #a855f7);
}

.share-text {
  font-size: 11px;
  color: var(--text-dim);
  min-width: 46px;
  text-align: right;
}
</style>
