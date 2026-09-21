<template>
  <div class="login-page">
    <div class="login-card panel">
      <div class="login-head">
        <div class="login-logo">◉</div>
        <h1 class="login-title">JvmEye</h1>
        <p class="login-sub">轻量级本机 JVM 监控 · 发现 / 指标 / 诊断</p>
      </div>

      <form class="login-form" @submit.prevent="onSubmit">
        <label class="field">
          <span class="field-label">用户名</span>
          <input v-model.trim="username" type="text" autocomplete="username" placeholder="admin"
                 :disabled="loading" required />
        </label>

        <label class="field">
          <span class="field-label">密码</span>
          <input v-model="password" type="password" autocomplete="current-password"
                 placeholder="••••••••" :disabled="loading" required />
        </label>

        <p v-if="errorText" class="alert alert-error">{{ errorText }}</p>

        <button class="btn btn-primary login-submit" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <p class="login-tip dim">默认账号 admin / admin123(可在后端 application.yml 中修改)</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const errorText = ref('')

onMounted(() => {
  if (route.query.error !== undefined) {
    errorText.value = '用户名或密码错误'
  }
  if (route.query.logout !== undefined) {
    errorText.value = '已退出登录,请重新登录'
  }
})

async function onSubmit() {
  if (loading.value) return
  loading.value = true
  errorText.value = ''
  try {
    // Spring Security 表单登录,直接提交到 /login(不带 /api 前缀)
    await axios.post('/login', new URLSearchParams({
      username: username.value,
      password: password.value
    }), {
      withCredentials: true,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      maxRedirects: 5,
      validateStatus: (status) => status >= 200 && status < 400
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    errorText.value = '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 32px 28px 24px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
}

.login-head {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  font-size: 34px;
  color: var(--accent);
  line-height: 1;
}

.login-title {
  margin: 10px 0 4px;
  font-size: 22px;
  letter-spacing: 0.6px;
}

.login-sub {
  margin: 0;
  font-size: 12px;
  color: var(--text-dim);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: var(--text-dim);
}

.field input {
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(11, 15, 24, 0.7);
  color: var(--text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.field input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(76, 141, 255, 0.18);
}

.login-submit {
  padding: 10px;
  font-size: 14px;
  margin-top: 4px;
}

.login-tip {
  margin: 18px 0 0;
  text-align: center;
  font-size: 11px;
}
</style>
