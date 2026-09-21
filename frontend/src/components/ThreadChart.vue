<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { useEChart, baseGrid, baseTooltip, axisStyle } from '../composables/useEChart'
import { formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  const times = points.map((p) => formatTime(p.timestamp))
  const deadlocked = points.map((p) => (p.thread.deadlocked ? p.thread.live : null))
  return {
    grid: baseGrid,
    tooltip: baseTooltip,
    legend: { top: 4, textStyle: { color: '#94a3b8', fontSize: 11 }, itemWidth: 14, itemHeight: 8 },
    xAxis: { type: 'category', data: times, boundaryGap: false, ...axisStyle },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [
      { name: '活跃线程', type: 'line', smooth: true, showSymbol: false,
        data: points.map((p) => p.thread.live), lineStyle: { width: 2 }, areaStyle: { opacity: 0.2 } },
      { name: '守护线程', type: 'line', smooth: true, showSymbol: false,
        data: points.map((p) => p.thread.daemon), lineStyle: { width: 1.5 } },
      { name: '峰值线程', type: 'line', smooth: true, showSymbol: false,
        data: points.map((p) => p.thread.peak), lineStyle: { width: 1.5, type: 'dashed' } },
      { name: '死锁标记', type: 'scatter', symbolSize: 9, itemStyle: { color: '#ef4444' },
        data: deadlocked }
    ]
  }
})

watch(() => props.points, (points) => update({ points }), { immediate: true })
</script>

<style scoped>
.chart {
  width: 100%;
  height: 100%;
}
</style>
