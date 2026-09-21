<template>
  <div class="target-selector">
    <div class="selector-head">
      <div>
        <h2 class="panel-title" style="margin-bottom: 4px">本机 JVM 列表</h2>
        <p class="dim selector-tip">
          通过 JDK Attach API 发现本机 JVM。目标需与 JvmEye <b>同一用户、同一架构</b> 才可连接。
        </p>
      </div>
      <button class="btn" :disabled="loading" @click="$emit('reload')">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '发现中…' : '重新发现' }}
      </button>
    </div>

    <div v-if="loading && targets.length === 0" class="empty">
      <span class="spinner"></span> 正在扫描本机 JVM…
    </div>

    <div v-else-if="targets.length === 0" class="empty">未发现可连接的 JVM 进程</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>PID</th>
          <th>主类 / 显示名</th>
          <th>JVM 版本</th>
          <th>Java Home</th>
          <th>状态</th>
          <th style="width: 90px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in targets" :key="item.pid" :class="{ 'row-disabled': !item.attachable }">
          <td class="mono">{{ item.pid }}</td>
          <td>
            <div class="cell-main" :title="item.displayName">{{ item.mainClass || item.displayName }}</div>
            <div class="cell-sub dim" :title="item.mainArgs">
              {{ item.displayName }}{{ item.self ? ' · 当前进程' : '' }}
            </div>
          </td>
          <td class="dim">{{ item.jvmVersion || '-' }}</td>
          <td class="dim cell-home" :title="item.javaHome">{{ item.javaHome || '-' }}</td>
          <td>
            <span v-if="item.attachable" class="badge badge-ok">可连接</span>
            <span v-else class="badge badge-danger" :title="item.reason">不可连接</span>
            <div v-if="!item.attachable && item.reason" class="cell-sub danger">{{ item.reason }}</div>
          </td>
          <td>
            <button class="btn btn-sm btn-primary" :disabled="!item.attachable || connectingPid !== null"
                    @click="$emit('connect', item.pid)">
              <span v-if="connectingPid === item.pid" class="spinner"></span>
              {{ connectingPid === item.pid ? '连接中' : '监控' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  targets: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  connectingPid: { type: Number, default: null }
})

defineEmits(['connect', 'reload'])
</script>

<style scoped>
.selector-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.selector-tip {
  margin: 0;
  font-size: 12px;
}

.cell-main {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-sub {
  font-size: 11px;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-home {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.row-disabled {
  opacity: 0.62;
}

.danger {
  color: #fca5a5;
}
</style>
