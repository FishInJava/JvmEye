<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { lineSeries, timeAxis, useEChart, baseGrid, baseLegend, baseTooltip, axisStyle } from '../composables/useEChart'
import { formatBytes, formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, valueFormatter: (v) => formatBytes(v) },
    legend: baseLegend,
    xAxis: timeAxis(points.map((p) => formatTime(p.timestamp))),
    yAxis: {
      type: 'value',
      ...axisStyle,
      // 坐标轴刻度是字节数,取 0 位小数
      axisLabel: { ...axisStyle.axisLabel, formatter: (v) => formatBytes(v, 0) }
    },
    series: [
      lineSeries('堆 used', points.map((p) => p.memory.heapUsed), { area: 0.25 }),
      lineSeries('堆 committed', points.map((p) => p.memory.heapCommitted), { width: 1.5, dash: 'dashed' }),
      lineSeries('堆 max', points.map((p) => p.memory.heapMax), { width: 1, dash: 'dotted' }),
      lineSeries('非堆 used', points.map((p) => p.memory.nonHeapUsed), { width: 1.5 })
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
