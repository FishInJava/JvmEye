<template>
  <div class="app-shell">
    <header class="app-topbar">
      <div class="brand">
        <span class="brand-logo">◉</span>
        <span class="brand-name">JvmEye</span>
        <span class="brand-sub">轻量级 JVM 监控</span>
      </div>
      <nav class="app-nav">
        <router-link to="/" class="nav-link">控制台</router-link>
      </nav>
    </header>
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <keep-alive include="DashboardView">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
  </div>
</template>

<script setup>
// 顶层布局:顶栏 + 内容区。Dashboard 使用 keep-alive 避免轮询被反复重建。
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-topbar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 24px;
  height: 56px;
  background: rgba(15, 20, 32, 0.85);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.brand-logo {
  color: var(--accent);
  font-size: 18px;
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 12px;
  color: var(--text-dim);
}

.app-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-dim);
  text-decoration: none;
  transition: all 0.15s ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--text);
  background: rgba(76, 141, 255, 0.12);
}

.app-main {
  flex: 1;
  padding: 24px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}
</style>
