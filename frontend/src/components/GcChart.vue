<template>
  <div ref="el" class="chart"></div>
</template>

<script setup>
import { watch } from 'vue'
import { axisNameStyle, axisStyle, baseGrid, baseLegend, baseTooltip, barSeries, lineSeries, timeAxis, useEChart } from '../composables/useEChart'
import { formatTime } from '../utils/format'

const props = defineProps({
  points: { type: Array, default: () => [] }
})

const { el, update } = useEChart((data) => {
  const points = data.points || []
  return {
    grid: baseGrid,
    tooltip: { ...baseTooltip, axisPointer: { type: 'shadow' } },
    legend: baseLegend,
    xAxis: timeAxis(points.map((p) => formatTime(p.timestamp)), { boundaryGap: true }),
    // 左轴累计次数,右轴累计耗时(ms)
    yAxis: [
      { type: 'value', name: '次数', nameTextStyle: axisNameStyle, ...axisStyle },
      { type: 'value', name: '耗时(ms)', nameTextStyle: axisNameStyle, ...axisStyle, splitLine: { show: false } }
    ],
    series: [
      barSeries('GC 累计次数', points.map((p) => p.gc.totalCollectionCount)),
      lineSeries('GC 累计耗时', points.map((p) => p.gc.totalCollectionTimeMs), { area: 0.18, yAxisIndex: 1 })
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
