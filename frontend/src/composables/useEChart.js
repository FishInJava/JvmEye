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
 * 图表内的次要文字色。
 *
 * canvas 渲染读不到 CSS 变量,所以图表配色集中在本文件;
 * 这个值与 styles.css 的 --text-dim 一致,两边要改一起改。
 */
const CHART_TEXT_DIM = '#94a3b8'

/**
 * ECharts 生命周期 composable:初始化、自适应、销毁。
 *
 * 图表跑在 canvas 里,拿不到 CSS 变量,所以图表配色统一由本文件导出,
 * 组件里不要再出现写死的色值。
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

  onMounted(() => {
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
  axisLabel: { color: CHART_TEXT_DIM, fontSize: 11 },
  splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)' } }
}

/** 图例:多个图表用的是同一套,抽出来避免复制粘贴。 */
export const baseLegend = {
  top: 4,
  textStyle: { color: CHART_TEXT_DIM, fontSize: 11 },
  itemWidth: 14,
  itemHeight: 8
}

/** y 轴标题(次数/耗时)样式。 */
export const axisNameStyle = { color: CHART_TEXT_DIM }

/** 图表专用色,与 styles.css 的语义色一一对应。 */
export const chartColors = {
  accent: 'rgba(76, 141, 255, 0.55)', // --accent 的柱状图版本
  danger: '#ef4444' // --danger
}

/**
 * x 轴:时间分类轴。
 *
 * @param {string[]} times 横轴刻度(formatTime 的结果)
 * @param {{ boundaryGap?: boolean }} options 柱状图为 true,折线图为 false
 */
export function timeAxis(times, { boundaryGap = false } = {}) {
  return { type: 'category', data: times, boundaryGap, ...axisStyle }
}

/**
 * 生成一条折线 series。
 *
 * @param {string} name   图例名
 * @param {number[]} values y 轴数据(null 表示断点)
 * @param {{ width?: number, dash?: 'dashed'|'dotted', area?: number, yAxisIndex?: number }} style
 *        线宽(px)、线型(不传为实线)、面积填充透明度(0 表示不填充)、绑定的 y 轴(双轴时用)
 */
export function lineSeries(name, values, { width = 2, dash, area = 0, yAxisIndex } = {}) {
  return {
    name,
    type: 'line',
    smooth: true,
    showSymbol: false,
    ...(yAxisIndex === undefined ? {} : { yAxisIndex }),
    data: values,
    lineStyle: { width, ...(dash ? { type: dash } : {}) },
    ...(area ? { areaStyle: { opacity: area } } : {})
  }
}

/**
 * 生成一个柱状 series。
 *
 * @param {string} name   图例名
 * @param {number[]} values y 轴数据
 * @param {{ color?: string, maxWidth?: number, yAxisIndex?: number }} style 柱色 / 柱最大宽度 / y 轴
 */
export function barSeries(name, values, { color = chartColors.accent, maxWidth = 10, yAxisIndex } = {}) {
  return {
    name,
    type: 'bar',
    ...(yAxisIndex === undefined ? {} : { yAxisIndex }),
    data: values,
    barMaxWidth: maxWidth,
    itemStyle: { color, borderRadius: [3, 3, 0, 0] }
  }
}
