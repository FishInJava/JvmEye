/**
 * 多周期轮询:把一组"每 N 毫秒跑一次"的任务收拢成 start()/stop() 两个动作。
 *
 * 之前 DashboardView 自己保存两个 timer 变量,重复的 start/clear 代码;
 * 新加轮询任务时只要往数组里加一行,不用再动生命周期代码。
 *
 * @param {Array<{ intervalMs: number, run: () => void }>} tasks 轮询任务列表
 * @returns {{ start: () => void, stop: () => void }} start 会先 stop 一次,可重复调用
 */
export function usePolling(tasks) {
  let timers = []

  /** 停止全部轮询,幂等。 */
  function stop() {
    for (const timer of timers) {
      window.clearInterval(timer)
    }
    timers = []
  }

  /** 开始轮询;不会立刻执行一次,需要立即跑请自己调 task.run()。 */
  function start() {
    stop()
    for (const task of tasks) {
      timers.push(window.setInterval(task.run, task.intervalMs))
    }
  }

  return { start, stop }
}
