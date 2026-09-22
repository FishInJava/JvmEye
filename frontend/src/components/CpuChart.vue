<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import {
  axisNameStyle,
  axisStyle,
  barSeries,
  baseGrid,
  baseLegend,
  baseTooltip,
  lineSeries,
  timeAxis,
  useEChart
} from '../composables/useEChart'
import { formatTime, toPercent } from '../utils/format'

/** 比率转百分比;-1(采样不可用)为断点。 */
const percentOf = (ratio) => {
  const percent = toPercent(ratio)
  return percent === null ? null : Number(percent.toFixed(2))
}

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, valueFormatter: (v) => (v === null || v === undefined ? '-' : Number(v).toFixed(2)) },
    legend: baseLegend,
    xAxis: timeAxis(points.map((p) => formatTime(p.timestamp))),
    // 左轴 CPU %,右轴 LoadAverage
    yAxis: [
      { type: 'value', name: 'CPU %', max: 100, nameTextStyle: axisNameStyle, ...axisStyle },
      { type: 'value', name: 'Load', nameTextStyle: axisNameStyle, ...axisStyle, splitLine: { show: false } }
    ],
    series: [
      lineSeries('进程 CPU %', points.map((p) => percentOf(p.cpu.processCpuLoad)), { area: 0.2 }),
      lineSeries('系统 CPU %', points.map((p) => percentOf(p.cpu.systemCpuLoad)), { width: 1.5 }),
      lineSeries(
        'LoadAverage',
        points.map((p) => (p.cpu.systemLoadAverage < 0 ? null : Number(p.cpu.systemLoadAverage.toFixed(2)))),
        { width: 1.5, dash: 'dashed', yAxisIndex: 1 }
      )
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
