<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import {
  axisNameStyle,
  axisStyle,
  baseGrid,
  baseLegend,
  baseTooltip,
  chartColors,
  lineSeries,
  timeAxis,
  useEChart
} from '../composables/useEChart'
import { formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  // 死锁时刻打点:y 值取当时的活跃线程数,只是为了把标记放到合适的高度
  const deadlockMarks = points.map((p) => (p.thread.deadlocked ? p.thread.live : null))
  return {
    grid: baseGrid,
    tooltip: baseTooltip,
    legend: baseLegend,
    xAxis: timeAxis(points.map((p) => formatTime(p.timestamp))),
    yAxis: { type: 'value', minInterval: 1, ...axisStyle },
    series: [
      lineSeries('活跃线程', points.map((p) => p.thread.live), { area: 0.2 }),
      lineSeries('守护线程', points.map((p) => p.thread.daemon), { width: 1.5 }),
      lineSeries('峰值线程', points.map((p) => p.thread.peak), { width: 1.5, dash: 'dashed' }),
      {
        name: '死锁标记',
        type: 'scatter',
        symbolSize: 9,
        itemStyle: { color: chartColors.danger },
        data: deadlockMarks
      }
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
