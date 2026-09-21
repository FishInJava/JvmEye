<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { useEChart, baseGrid, baseTooltip, axisStyle } from '../composables/useEChart'
import { formatDuration, formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  const times = points.map((p) => formatTime(p.timestamp))
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, axisPointer: { type: 'shadow' } },
    legend: { top: 4, textStyle: { color: '#94a3b8', fontSize: 11 }, itemWidth: 14, itemHeight: 8 },
    xAxis: { type: 'category', data: times, boundaryGap: true, ...axisStyle },
    yAxis: [
      { type: 'value', name: '次数', nameTextStyle: { color: '#94a3b8' }, ...axisStyle },
      { type: 'value', name: '耗时(ms)', nameTextStyle: { color: '#94a3b8' }, ...axisStyle, splitLine: { show: false } }
    ],
    series: [
      {
        name: 'GC 累计次数', type: 'bar', barMaxWidth: 10,
        data: points.map((p) => p.gc.totalCollectionCount),
        itemStyle: { color: 'rgba(76, 141, 255, 0.55)', borderRadius: [3, 3, 0, 0] }
      },
      {
        name: 'GC 累计耗时', type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1,
        data: points.map((p) => p.gc.totalCollectionTimeMs),
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.18 }
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
