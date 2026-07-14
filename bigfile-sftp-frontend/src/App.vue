<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

const API_BASE = '/api/sftp-config'

const form = reactive({
  host: '',
  port: 22,
  username: '',
  password: '',
  sftpPath: '',
  bandwidthLimitMbps: '',
  parallelCount: 1,
  targetType: 'LOCAL',
  syncPath: '',
  targetHost: '',
  targetPort: 22,
  targetUsername: '',
  targetPassword: '',
  targetPath: '',
})

const loading = ref(false)
const syncing = ref(false)
const status = ref('')
const error = ref('')
const syncResult = ref(null)

const canSubmit = computed(() => {
  const port = Number(form.port)
  const targetPort = Number(form.targetPort)
  const parallelCount = Number(form.parallelCount)
  const bandwidthLimit = String(form.bandwidthLimitMbps ?? '').trim()
  const bandwidthLimitValid = !bandwidthLimit || Number(bandwidthLimit) >= 0.01
  const parallelCountValid = Number.isInteger(parallelCount) && parallelCount >= 1 && parallelCount <= 16
  const localTargetValid = form.targetType === 'LOCAL' && form.syncPath.trim()
  const sftpTargetValid = form.targetType === 'SFTP' && form.targetHost.trim() && Number.isInteger(targetPort) && targetPort >= 1 && targetPort <= 65535 && form.targetUsername.trim() && form.targetPassword && form.targetPath.trim().startsWith('/')
  return form.host.trim() && Number.isInteger(port) && port >= 1 && port <= 65535 && form.username.trim() && form.password && form.sftpPath.trim().startsWith('/') && bandwidthLimitValid && parallelCountValid && (localTargetValid || sftpTargetValid)
})

onMounted(loadConfig)

async function loadConfig() {
  loading.value = true
  error.value = ''
  try {
    const data = await request(API_BASE)
    Object.assign(form, data)
    form.bandwidthLimitMbps = data.bandwidthLimitMbps ?? ''
    form.parallelCount = data.parallelCount ?? 1
    form.targetType = data.targetType || 'LOCAL'
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  loading.value = true
  status.value = ''
  error.value = ''
  syncResult.value = null
  try {
    const data = await request(API_BASE, {
      method: 'POST',
      body: JSON.stringify(payload()),
    })
    Object.assign(form, data)
    form.bandwidthLimitMbps = data.bandwidthLimitMbps ?? ''
    form.parallelCount = data.parallelCount ?? 1
    form.targetType = data.targetType || 'LOCAL'
    status.value = '配置保存成功'
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function syncFiles() {
  syncing.value = true
  status.value = ''
  error.value = ''
  syncResult.value = null
  try {
    const data = await request(`${API_BASE}/sync`, {
      method: 'POST',
      body: JSON.stringify(payload()),
    })
    syncResult.value = data
    status.value = data.message
  } catch (err) {
    error.value = err.message
  } finally {
    syncing.value = false
  }
}

function payload() {
  return {
    host: form.host.trim(),
    port: Number(form.port),
    username: form.username.trim(),
    password: form.password,
    sftpPath: form.sftpPath.trim(),
    bandwidthLimitMbps: parseBandwidthLimit(),
    parallelCount: Number(form.parallelCount),
    targetType: form.targetType,
    syncPath: form.syncPath.trim(),
    targetHost: form.targetHost.trim(),
    targetPort: form.targetPort ? Number(form.targetPort) : null,
    targetUsername: form.targetUsername.trim(),
    targetPassword: form.targetPassword,
    targetPath: form.targetPath.trim(),
  }
}

function normalizePort() {
  form.port = String(form.port).replace(/\D/g, '').slice(0, 10)
}

function normalizeTargetPort() {
  form.targetPort = String(form.targetPort).replace(/\D/g, '').slice(0, 10)
}

function normalizeParallelCount() {
  form.parallelCount = String(form.parallelCount).replace(/\D/g, '').slice(0, 2)
}

function normalizeBandwidthLimit() {
  const normalized = String(form.bandwidthLimitMbps ?? '')
    .replace(/[^\d.]/g, '')
    .replace(/(\..*)\./g, '$1')
  form.bandwidthLimitMbps = normalized.slice(0, 10)
}

function parseBandwidthLimit() {
  const value = String(form.bandwidthLimitMbps ?? '').trim()
  return value ? Number(value) : null
}

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const detail = Array.isArray(data.errors) ? `：${data.errors.join('；')}` : ''
    throw new Error(`${data.message || '请求失败'}${detail}`)
  }
  return data
}
</script>

<template>
  <main class="page">
    <section class="panel">
      <div class="header">
        <p class="eyebrow">Bigfile SFTP</p>
        <h1>SFTP 文件同步配置</h1>
        <p class="description">保存 SFTP 连接信息，并通过 Apache Camel 将远端文件同步到本地路径或指定的远程SFTP服务器。</p>
      </div>

      <form class="form" @submit.prevent="saveConfig">
        <label>
          <span>SFTP服务器地址</span>
          <input v-model.trim="form.host" maxlength="255" required placeholder="例如：192.168.1.10" />
        </label>

        <label>
          <span>SFTP服务器端口</span>
          <input v-model="form.port" maxlength="10" inputmode="numeric" pattern="\d{1,10}" required @input="normalizePort" />
        </label>

        <label>
          <span>用户名</span>
          <input v-model.trim="form.username" maxlength="20" required autocomplete="username" />
        </label>

        <label>
          <span>密码</span>
          <input v-model="form.password" maxlength="50" required type="password" autocomplete="current-password" />
        </label>

        <label class="wide">
          <span>SFTP文件路径</span>
          <input v-model.trim="form.sftpPath" maxlength="255" required placeholder="例如：/data/upload" />
        </label>

        <label class="wide">
          <span>总传输限速（MB/s）</span>
          <input v-model="form.bandwidthLimitMbps" maxlength="10" inputmode="decimal" placeholder="多个文件共享，例如：5 或 0.3，留空表示不限速" @input="normalizeBandwidthLimit" />
        </label>

        <label class="wide">
          <span>并发文件数</span>
          <input v-model="form.parallelCount" maxlength="2" inputmode="numeric" pattern="\d{1,2}" required placeholder="1-16" @input="normalizeParallelCount" />
        </label>

        <div class="wide target-switch">
          <span>同步目标</span>
          <label class="radio-item">
            <input v-model="form.targetType" type="radio" value="LOCAL" />
            本地文件同步路径
          </label>
          <label class="radio-item">
            <input v-model="form.targetType" type="radio" value="SFTP" />
            远程SFTP服务
          </label>
        </div>

        <label v-if="form.targetType === 'LOCAL'" class="wide">
          <span>文件同步路径</span>
          <input v-model.trim="form.syncPath" maxlength="255" required placeholder="例如：D:\\sftp-sync 或 /data/sftp-sync" />
        </label>

        <template v-if="form.targetType === 'SFTP'">
          <label>
            <span>目标SFTP服务器地址</span>
            <input v-model.trim="form.targetHost" maxlength="255" required placeholder="例如：192.168.1.20" />
          </label>

          <label>
            <span>目标SFTP服务器端口</span>
            <input v-model="form.targetPort" maxlength="10" inputmode="numeric" pattern="\d{1,10}" required @input="normalizeTargetPort" />
          </label>

          <label>
            <span>目标用户名</span>
            <input v-model.trim="form.targetUsername" maxlength="20" required autocomplete="off" />
          </label>

          <label>
            <span>目标密码</span>
            <input v-model="form.targetPassword" maxlength="50" required type="password" autocomplete="off" />
          </label>

          <label class="wide">
            <span>目标SFTP文件路径</span>
            <input v-model.trim="form.targetPath" maxlength="255" required placeholder="例如：/data/sftp-sync" />
          </label>
        </template>

        <div class="actions wide">
          <button :disabled="loading || syncing || !canSubmit" type="submit">
            {{ loading ? '保存中...' : '保存' }}
          </button>
          <button :disabled="loading || syncing || !canSubmit" class="secondary" type="button" @click="syncFiles">
            {{ syncing ? '同步中...' : '数据同步' }}
          </button>
        </div>
      </form>

      <p v-if="status" class="message success">{{ status }}</p>
      <p v-if="error" class="message error">{{ error }}</p>

      <section v-if="syncResult" class="result">
        <h2>同步结果</h2>
        <p>源URI：{{ syncResult.sourceUri }}</p>
        <p>远端路径：{{ syncResult.remotePath }}</p>
        <p>传输限速：{{ syncResult.bandwidthLimit }}</p>
        <p>目标路径：{{ syncResult.targetPath }}</p>
        <p>同步文件数：{{ syncResult.transferredCount }}</p>
        <h3>远端目录扫描</h3>
        <ul v-if="syncResult.remoteScan?.length">
          <li v-for="entry in syncResult.remoteScan" :key="entry">{{ entry }}</li>
        </ul>
        <p v-else>扫描结果为空</p>
        <h3>已同步文件</h3>
        <ul v-if="syncResult.files?.length">
          <li v-for="file in syncResult.files" :key="file">{{ file }}</li>
        </ul>
        <p v-else>暂无已同步文件</p>
        <h3 v-if="syncResult.failedFiles?.length">失败文件</h3>
        <ul v-if="syncResult.failedFiles?.length">
          <li v-for="file in syncResult.failedFiles" :key="file">{{ file }}</li>
        </ul>
      </section>
    </section>
  </main>
</template>
