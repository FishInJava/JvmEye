<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { useEChart, baseGrid, baseTooltip, axisStyle } from '../composables/useEChart'
import { formatBytes, formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  const times = points.map((p) => formatTime(p.timestamp))
  const series = [
    { name: '堆 used', type: 'line', smooth: true, showSymbol: false, data: points.map((p) => p.memory.heapUsed),
      lineStyle: { width: 2 }, areaStyle: { opacity: 0.25 } },
    { name: '堆 committed', type: 'line', smooth: true, showSymbol: false, data: points.map((p) => p.memory.heapCommitted),
      lineStyle: { width: 1.5, type: 'dashed' } },
    { name: '堆 max', type: 'line', smooth: true, showSymbol: false, data: points.map((p) => p.memory.heapMax),
      lineStyle: { width: 1, type: 'dotted' } },
    { name: '非堆 used', type: 'line', smooth: true, showSymbol: false, data: points.map((p) => p.memory.nonHeapUsed),
      lineStyle: { width: 1.5 } }
  ]
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, valueFormatter: (v) => formatBytes(v) },
    legend: { top: 4, textStyle: { color: '#94a3b8', fontSize: 11 }, itemWidth: 14, itemHeight: 8 },
    xAxis: { type: 'category', data: times, boundaryGap: false, ...axisStyle },
    yAxis: { type: 'value', ...axisStyle, axisLabel: { ...axisStyle.axisLabel, formatter: (v) => formatBytes(v, 0) } },
    series
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
