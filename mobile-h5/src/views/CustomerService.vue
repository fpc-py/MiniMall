<template>
  <div class="page cs-page">
    <van-nav-bar title="智能客服" fixed placeholder left-arrow @click-left="goBack">
      <template #right>
        <span class="nav-handoff" @click="sendText('转人工')">转人工</span>
      </template>
    </van-nav-bar>

    <div ref="listRef" class="cs-body">
      <!-- 开场 -->
      <div class="intro rv" style="--i: 0">
        <span class="intro-orb">
          <span class="intro-ring" aria-hidden="true"></span>
          <van-icon name="chat-o" size="24" color="#fff" />
        </span>
        <div class="intro-title">商城智能客服</div>
        <div class="intro-sub">我可以帮你查订单、查退款、找商品、解答退换货与发票等问题</div>
      </div>

      <!-- 快捷提问 -->
      <div v-if="!messages.length" class="hot-wrap rv" style="--i: 1">
        <span class="eyebrow">QUICK ASK</span>
        <div class="hot-chips">
          <button v-for="(q, i) in hotFaq" :key="i" class="hot-chip" :style="{ '--i': i }" @click="sendText(q)">
            {{ q }}
          </button>
        </div>
      </div>

      <!-- 消息流 -->
      <div class="msg-list">
        <div v-for="(msg, i) in messages" :key="msg.key" class="msg" :class="msg.role" :style="{ '--i': i }">
          <template v-if="msg.role === 'system'">
            <div class="sys-pill">{{ msg.content }}</div>
          </template>
          <template v-else>
            <span v-if="msg.role === 'assistant'" class="msg-avatar">
              <van-icon name="chat-o" size="16" color="#fff" />
            </span>
            <div class="msg-main">
              <div v-if="msg.toolText" class="tool-tip">
                <van-loading size="12" color="#7cf2ff" />
                <span>{{ msg.toolText }}</span>
              </div>
              <div class="bubble">
                <span class="bubble-text">{{ msg.content }}</span>
                <span v-if="msg.pending" class="caret" aria-hidden="true"></span>
              </div>
              <div v-if="msg.cards && msg.cards.length" class="cards">
                <div
                  v-for="card in msg.cards"
                  :key="card.id"
                  class="card"
                  @click="$router.push(`/product/${card.id}`)"
                >
                  <div class="card-img">
                    <img v-if="card.mainImage" :src="card.mainImage" alt="" />
                    <van-icon v-else name="photo-o" size="28" color="#6d7490" />
                  </div>
                  <div class="card-name van-multi-ellipsis--l2">{{ card.name }}</div>
                  <div class="card-price num">¥{{ card.minPrice }}</div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 输入栏 -->
    <div class="cs-input">
      <van-field
        v-model="draft"
        class="cs-field"
        type="textarea"
        rows="1"
        autosize
        maxlength="500"
        placeholder="描述你的问题，例如：我的订单到哪了"
        :disabled="streaming"
        @keyup.enter="onEnter"
      />
      <button class="send-btn" :class="{ disabled: !canSend }" :disabled="!canSend" @click="sendText(draft)">
        <van-loading v-if="streaming" size="18" color="#fff" />
        <van-icon v-else name="send" size="19" color="#fff" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { chatStream, getConversations, getHotFaq, getMessages } from '@/api/ai'

/** 工具名 → 进行中文案 */
const TOOL_TEXT = {
  search_faq: '正在查阅帮助中心…',
  query_order: '正在查询你的订单…',
  query_refund: '正在查询退款进度…',
  search_product: '正在为你找商品…',
  handoff_to_human: '正在为你转接人工客服…',
}

const router = useRouter()

const listRef = ref(null)
const hotFaq = ref([])
const messages = ref([])
const draft = ref('')
const conversationId = ref(null)
const streaming = ref(false)

let controller = null
let seed = 0

const canSend = computed(() => !streaming.value && draft.value.trim().length > 0)

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/')
  }
}

function pushMessage(message) {
  seed += 1
  messages.value.push({ key: seed, pending: false, cards: [], toolText: '', ...message })
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function loadHotFaq() {
  try {
    const res = await getHotFaq()
    hotFaq.value = res.data || []
  } catch (e) {
    hotFaq.value = []
  }
}

/** 恢复最近一次会话，让历史消息可续接 */
async function loadHistory() {
  try {
    const res = await getConversations()
    const latest = (res.data || [])[0]
    if (!latest) return
    conversationId.value = latest.id
    const msgRes = await getMessages(latest.id)
    const list = msgRes.data || []
    list.forEach((item) => {
      seed += 1
      messages.value.push({
        key: seed,
        role: item.role === 2 ? 'assistant' : 'user',
        content: item.content || '',
        pending: false,
        cards: [],
        toolText: '',
      })
    })
    if (latest.status === 1) {
      pushMessage({ role: 'system', content: '本会话已转接人工客服' })
    }
    scrollToBottom()
  } catch (e) {
    // 历史加载失败不阻塞新对话
  }
}

function onEnter(event) {
  event.preventDefault()
  sendText(draft.value)
}

function sendText(text) {
  const content = (text || '').trim()
  if (!content) {
    showToast('请输入你的问题')
    return
  }
  if (streaming.value) return

  pushMessage({ role: 'user', content })
  draft.value = ''

  seed += 1
  messages.value.push({
    key: seed,
    role: 'assistant',
    content: '',
    pending: true,
    cards: [],
    toolText: '',
  })
  // 后续要持续改写这一条消息，必须持有数组里的响应式元素，直接改原始对象不会触发渲染
  const reply = messages.value[messages.value.length - 1]
  streaming.value = true
  scrollToBottom()

  controller = new AbortController()
  const payload = { content }
  if (conversationId.value) {
    payload.conversationId = conversationId.value
  }

  chatStream(
    payload,
    {
      onMeta: (event) => {
        conversationId.value = event.conversationId
      },
      onTool: (event) => {
        reply.toolText = event.status === 'running' ? TOOL_TEXT[event.name] || '正在处理…' : ''
      },
      onProducts: (event) => {
        reply.cards = event.data || []
      },
      onDelta: (content) => {
        reply.content += content
        scrollToBottom()
      },
      onDone: (event) => {
        reply.pending = false
        reply.toolText = ''
        if (!reply.content) {
          reply.content = '抱歉，我没能给出回答，请换个说法再试。'
        }
        if (event.handoff) {
          pushMessage({ role: 'system', content: '已为你转接人工客服，工作时间 9:00-21:00' })
        }
      },
      onError: (msg) => {
        reply.pending = false
        reply.toolText = ''
        reply.content = msg
      },
    },
    controller.signal
  ).finally(() => {
    streaming.value = false
    reply.pending = false
    reply.toolText = ''
    scrollToBottom()
  })
}

onMounted(() => {
  loadHotFaq()
  loadHistory()
})

onUnmounted(() => {
  if (controller) controller.abort()
})
</script>

<style scoped>
.cs-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.cs-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px var(--gutter) 12px;
  -webkit-overflow-scrolling: touch;
}

/* ---------- 开场 ---------- */
.intro {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 18px 0 6px;
  text-align: center;
}
.intro-orb {
  position: relative;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: var(--glow-neon);
  margin-bottom: 4px;
}
.intro-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: conic-gradient(from 140deg, var(--neon), var(--cyan), var(--violet), var(--neon));
  opacity: 0.7;
  filter: blur(8px);
  z-index: 0;
  animation: glowPulse 3.4s var(--ease-out) infinite;
}
.intro-orb .van-icon {
  position: relative;
  z-index: 1;
}
.intro-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.03em;
}
.intro-sub {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.62;
  max-width: 260px;
}

/* ---------- 快捷提问 ---------- */
.hot-wrap {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.hot-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hot-chip {
  padding: 7px 13px;
  font-size: 12px;
  font-family: inherit;
  color: var(--text-2);
  border-radius: var(--r-pill);
  border: 1px solid var(--glass-line);
  background: var(--glass);
  backdrop-filter: blur(var(--blur));
  animation: riseIn 0.5s var(--ease-out) both;
  animation-delay: calc(var(--i, 0) * 55ms);
  transition: transform var(--dur-fast) var(--ease-spring), border-color var(--dur-fast);
}
.hot-chip:active {
  transform: scale(0.96);
  border-color: rgba(255, 46, 147, 0.5);
}

/* ---------- 消息流 ---------- */
.msg-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}
.msg {
  display: flex;
  gap: 8px;
  animation: riseIn 0.34s var(--ease-out) both;
}
.msg.user {
  justify-content: flex-end;
}
.msg.system {
  justify-content: center;
}
.sys-pill {
  font-size: 11px;
  color: var(--text-3);
  padding: 5px 12px;
  border-radius: var(--r-pill);
  background: var(--glass);
  border: 1px solid var(--glass-line);
}
.msg-avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(135deg, var(--neon), var(--violet));
  box-shadow: 0 6px 18px -10px rgba(255, 46, 147, 0.9);
}
.msg-main {
  max-width: 78%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msg.user .msg-main {
  align-items: flex-end;
}
.bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  border-radius: var(--r-md);
  border: 1px solid var(--glass-line);
  background-image: linear-gradient(158deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.022));
  backdrop-filter: blur(var(--blur)) saturate(150%);
  box-shadow: var(--shadow-card);
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.user .bubble {
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  border-color: transparent;
  color: #fff;
  box-shadow: var(--glow-neon);
}
/* 逐字输出时的光标 */
.caret {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--cyan);
  box-shadow: 0 0 8px var(--cyan);
  animation: fadeIn 0.62s steps(1, end) infinite alternate;
}

/* 工具进度提示 */
.tool-tip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: var(--cyan-soft);
  padding: 5px 11px;
  border-radius: var(--r-pill);
  border: 1px solid rgba(36, 229, 255, 0.35);
  background: rgba(36, 229, 255, 0.08);
  animation: fadeIn 0.3s var(--ease-out) both;
}

/* ---------- 商品卡片 ---------- */
.cards {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
  max-width: 100%;
}
.card {
  flex-shrink: 0;
  width: 116px;
  padding: 8px;
  border-radius: var(--r-sm);
  border: 1px solid var(--glass-line);
  background: var(--glass);
  backdrop-filter: blur(var(--blur));
  transition: transform var(--dur-fast) var(--ease-spring);
}
.card:active {
  transform: scale(0.97);
  border-color: rgba(255, 46, 147, 0.45);
}
.card-img {
  height: 84px;
  border-radius: var(--r-xs);
  overflow: hidden;
  background: var(--glass-2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-name {
  margin-top: 7px;
  font-size: 12px;
  color: var(--text-1);
  line-height: 1.4;
  min-height: 33px;
}
.card-price {
  margin-top: 5px;
  font-size: 14px;
  font-weight: 700;
  color: var(--neon-soft);
}

/* ---------- 输入栏 ---------- */
.cs-input {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px var(--gutter) calc(10px + env(safe-area-inset-bottom));
  backdrop-filter: blur(var(--blur-lg)) saturate(150%);
  background-image: linear-gradient(180deg, rgba(10, 12, 20, 0.6), rgba(5, 6, 11, 0.95));
  border-top: 1px solid var(--glass-line);
}
.cs-field {
  flex: 1;
  padding: 9px 14px !important;
  border-radius: var(--r-md);
  background: var(--glass) !important;
  border: 1px solid var(--glass-line);
  --van-field-input-text-color: var(--text-1);
}
.send-btn {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: linear-gradient(135deg, var(--neon), #ff5ea8 55%, var(--violet));
  box-shadow: var(--glow-neon);
  transition: transform var(--dur-fast) var(--ease-spring), opacity var(--dur-fast);
}
.send-btn:active {
  transform: scale(0.93);
}
.send-btn.disabled {
  opacity: 0.42;
  box-shadow: none;
}

.nav-handoff {
  font-size: 13px;
  color: var(--neon-soft);
  letter-spacing: 0.03em;
}
</style>
