import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/** 我的最近会话 */
export function getConversations() {
  return request({ url: '/api/ai/conversations', method: 'get' })
}

/** 指定会话的消息列表（后端做归属校验） */
export function getMessages(conversationId) {
  return request({ url: `/api/ai/conversations/${conversationId}/messages`, method: 'get' })
}

/** 热门问题（快捷提问 chips） */
export function getHotFaq() {
  return request({ url: '/api/ai/faq/hot', method: 'get' })
}

/**
 * 流式对话：EventSource 无法带 Authorization 头，故用 fetch + ReadableStream 手工解析 SSE 分帧。
 * 事件：meta（会话/消息ID）/ tool（工具进度）/ products（商品卡片）/ delta（逐字）/ done / error。
 *
 * @param {{conversationId?: string|number, content: string}} payload
 * @param {{onMeta?, onTool?, onProducts?, onDelta?, onDone?, onError?}} handlers
 * @param {AbortSignal} [signal]
 */
export async function chatStream(payload, handlers = {}, signal) {
  const { onError } = handlers
  let response
  try {
    response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`,
      },
      body: JSON.stringify(payload),
      signal,
    })
  } catch (e) {
    if (e.name !== 'AbortError' && onError) onError('网络异常，请检查网络后重试')
    return
  }

  if (!response.ok || !response.body) {
    if (onError) onError('智能客服暂时不可用，请稍后再试')
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let index
      while ((index = buffer.indexOf('\n\n')) >= 0) {
        const frame = buffer.slice(0, index)
        buffer = buffer.slice(index + 2)
        handleFrame(frame, handlers)
      }
    }
  } catch (e) {
    if (e.name !== 'AbortError' && onError) onError('回答中断，请稍后再试')
  }
}

function handleFrame(frame, handlers) {
  let data = ''
  frame.split('\n').forEach((line) => {
    const text = line.trim()
    if (text.startsWith('data:')) data += text.slice(5).trim()
  })
  if (!data) return

  let event
  try {
    event = JSON.parse(data)
  } catch (e) {
    return
  }

  switch (event.type) {
    case 'meta':
      handlers.onMeta && handlers.onMeta(event)
      break
    case 'tool':
      handlers.onTool && handlers.onTool(event)
      break
    case 'products':
      handlers.onProducts && handlers.onProducts(event)
      break
    case 'delta':
      handlers.onDelta && handlers.onDelta(event.content || '')
      break
    case 'done':
      handlers.onDone && handlers.onDone(event)
      break
    case 'error':
      handlers.onError && handlers.onError(event.msg || '智能客服暂时不可用，请稍后再试')
      break
    default:
      break
  }
}
