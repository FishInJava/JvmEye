import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, ScatterChart } from 'echarts/charts'
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  BarChart,
  ScatterChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
  CanvasRenderer
])

/**
 * ECharts 生命周期 composable:初始化、自适应、销毁。
 *
 * @param {(props: object) => object} optionFactory 根据数据生成 echarts option
 * @returns {{ el: import('vue').Ref<HTMLElement|null>, update: (data: object) => void }}
 */
export function useEChart(optionFactory) {
  const el = ref(null)
  let chart = null
  let observer = null

  const update = (data) => {
    if (!chart) return
    chart.setOption(optionFactory(data || {}), { notMerge: true })
  }

  onMounted(async () => {
    if (!el.value) return
    chart = echarts.init(el.value, null, { renderer: 'canvas' })
    if (typeof ResizeObserver !== 'undefined') {
      observer = new ResizeObserver(() => chart && chart.resize())
      observer.observe(el.value)
    } else {
      window.addEventListener('resize', () => chart && chart.resize())
    }
  })

  onBeforeUnmount(() => {
    if (observer) observer.disconnect()
    if (chart) {
      chart.dispose()
      chart = null
    }
  })

  return { el, update }
}

/** 图表通用基础配置(深色主题、tooltip、grid)。 */
export const baseGrid = { left: 56, right: 24, top: 36, bottom: 28 }

export const baseTooltip = {
  trigger: 'axis',
  backgroundColor: 'rgba(17, 24, 39, 0.92)',
  borderColor: 'rgba(148, 163, 184, 0.25)',
  textStyle: { color: '#e5e7eb', fontSize: 12 }
}

export const axisStyle = {
  axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.35)' } },
  axisLabel: { color: '#94a3b8', fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)' } }
}
