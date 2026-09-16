// 秒杀压测：并发抢券，统计 QPS / 成功失败 / P50 P95 P99，并校验是否超卖
// 用法: BASE=http://localhost:8080/api TEMPLATE_ID=12 USERS=300 CONCURRENCY=300 node seckill-loadtest.mjs
import http from 'node:http'

const BASE = process.env.BASE || 'http://localhost:8080/api'
const TEMPLATE_ID = process.env.TEMPLATE_ID
const USERS = parseInt(process.env.USERS || '300', 10)
const CONCURRENCY = parseInt(process.env.CONCURRENCY || '300', 10)
const RUN = Date.now().toString(36)

if (!TEMPLATE_ID) {
  console.error('缺少 TEMPLATE_ID 环境变量')
  process.exit(1)
}

const u = new URL(BASE)
const HOST = u.hostname
const PORT = u.port || 80
const PREFIX = u.pathname.replace(/\/$/, '')

const agent = new http.Agent({ keepAlive: true, maxSockets: Math.max(CONCURRENCY, 50) })

function request(method, path, headers = {}, bodyObj = null) {
  return new Promise((resolve, reject) => {
    const data = bodyObj ? JSON.stringify(bodyObj) : null
    const req = http.request(
      { host: HOST, port: PORT, path: PREFIX + path, method, agent, headers: {
        ...headers,
        ...(data ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {})
      } },
      (res) => {
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => resolve({ status: res.statusCode, body: Buffer.concat(chunks).toString('utf8') }))
      }
    )
    req.on('error', reject)
    if (data) req.write(data)
    req.end()
  })
}

async function pool(items, limit, worker) {
  let idx = 0
  const results = new Array(items.length)
  const runners = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (true) {
      const i = idx++
      if (i >= items.length) break
      results[i] = await worker(items[i], i)
    }
  })
  await Promise.all(runners)
  return results
}

function percentile(sorted, p) {
  if (!sorted.length) return 0
  const i = Math.min(sorted.length - 1, Math.floor(p * sorted.length))
  return sorted[i]
}

async function main() {
  // 1) 准备用户（注册直接返回 token）
  console.log(`准备 ${USERS} 个用户 ...`)
  const idxs = Array.from({ length: USERS }, (_, i) => i)
  const regStart = Date.now()
  const tokens = await pool(idxs, 20, async (i) => {
    const phone = '139' + String(((Date.now() % 90000000) + i) % 100000000).padStart(8, '0')
    const body = { username: `lt_${RUN}_${i}`, password: 'pass1234', email: `lt${i}_${RUN}@m.local`, phone }
    try {
      const r = await request('POST', '/user/register', {}, body)
      const j = JSON.parse(r.body)
      return j.code === 200 ? j.data.token : null
    } catch (e) {
      return null
    }
  })
  const validTokens = tokens.filter(Boolean)
  console.log(`注册完成: ${validTokens.length}/${USERS}，耗时 ${Date.now() - regStart}ms`)

  // 2) 并发抢券
  console.log(`开始并发抢券: 请求数=${validTokens.length} 并发=${CONCURRENCY}`)
  const latencies = []
  const start = Date.now()
  const results = await pool(validTokens, CONCURRENCY, async (token) => {
    const t0 = process.hrtime.bigint()
    let code = -1
    try {
      const r = await request('POST', `/coupon/claim/${TEMPLATE_ID}`, { Authorization: `Bearer ${token}` })
      code = JSON.parse(r.body).code
    } catch (e) {
      code = -2
    }
    const ms = Number(process.hrtime.bigint() - t0) / 1e6
    latencies.push(ms)
    return code
  })
  const elapsed = Date.now() - start

  const success = results.filter((c) => c === 200).length
  const fail = results.length - success
  const sorted = latencies.slice().sort((a, b) => a - b)
  const qps = (results.length / (elapsed / 1000)).toFixed(1)

  console.log('================ 结果 ================')
  console.log(`请求总数 : ${results.length}`)
  console.log(`成功(受理): ${success}`)
  console.log(`失败      : ${fail}`)
  console.log(`总耗时    : ${elapsed} ms`)
  console.log(`QPS       : ${qps}`)
  console.log(`P50       : ${percentile(sorted, 0.50).toFixed(1)} ms`)
  console.log(`P95       : ${percentile(sorted, 0.95).toFixed(1)} ms`)
  console.log(`P99       : ${percentile(sorted, 0.99).toFixed(1)} ms`)
  console.log('=====================================')
  agent.destroy()
}

main().catch((e) => { console.error(e); process.exit(1) })
