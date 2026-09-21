<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { useEChart, baseGrid, baseTooltip, axisStyle } from '../composables/useEChart'
import { formatTime, toPercent } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  const times = points.map((p) => formatTime(p.timestamp))
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, valueFormatter: (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(2)}`) },
    legend: { top: 4, textStyle: { color: '#94a3b8', fontSize: 11 }, itemWidth: 14, itemHeight: 8 },
    xAxis: { type: 'category', data: times, boundaryGap: false, ...axisStyle },
    yAxis: [
      { type: 'value', name: 'CPU %', max: 100, nameTextStyle: { color: '#94a3b8' }, ...axisStyle },
      { type: 'value', name: 'Load', nameTextStyle: { color: '#94a3b8' }, ...axisStyle, splitLine: { show: false } }
    ],
    series: [
      {
        name: '进程 CPU %', type: 'line', smooth: true, showSymbol: false,
        data: points.map((p) => {
          const percent = toPercent(p.cpu.processCpuLoad)
          return percent === null ? null : Number(percent.toFixed(2))
        }),
        lineStyle: { width: 2 }, areaStyle: { opacity: 0.2 }, connectNulls: false
      },
      {
        name: '系统 CPU %', type: 'line', smooth: true, showSymbol: false,
        data: points.map((p) => {
          const percent = toPercent(p.cpu.systemCpuLoad)
          return percent === null ? null : Number(percent.toFixed(2))
        }),
        lineStyle: { width: 1.5 }, connectNulls: false
      },
      {
        name: 'LoadAverage', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1,
        data: points.map((p) => (p.cpu.systemLoadAverage < 0 ? null : Number(p.cpu.systemLoadAverage.toFixed(2)))),
        lineStyle: { width: 1.5, type: 'dashed' }, connectNulls: false
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
