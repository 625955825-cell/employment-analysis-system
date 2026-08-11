/**
 * WebSocket 服务 - 实时消息推送
 */
class WebSocketService {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.reconnectInterval = 3000
    this.maxReconnectAttempts = 5
    this.reconnectAttempts = 0
    this.listeners = new Map()
    this.connected = false
  }

  connect(token) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      return
    }
    const wsUrl = `ws://${window.location.host}/ws/notifications?token=${encodeURIComponent(token)}`
    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('[WS] Connected')
        this.connected = true
        this.reconnectAttempts = 0
        this.emit('connected', {})
        // 心跳保活
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const raw = event.data
          if (raw === 'pong' || raw === 'ping') return
          const data = JSON.parse(raw)
          if (data.type === 'pong') return
          console.log('[WS] Message received:', data)
          this.emit('message', data)
          // 按类别通知
          if (data.category) {
            this.emit(`category:${data.category}`, data)
          }
        } catch (e) {
          console.warn('[WS] Failed to parse message:', e)
        }
      }

      this.ws.onerror = (err) => {
        console.error('[WS] Error:', err)
        this.connected = false
        this.emit('error', err)
      }

      this.ws.onclose = (event) => {
        console.log('[WS] Closed, code:', event.code)
        this.connected = false
        this.stopHeartbeat()
        this.emit('disconnected', { code: event.code })
        // 非正常关闭时尝试重连
        if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect(token)
        }
      }
    } catch (e) {
      console.error('[WS] Failed to connect:', e)
      this.scheduleReconnect(token)
    }
  }

  disconnect() {
    this.stopHeartbeat()
    clearTimeout(this.reconnectTimer)
    if (this.ws) {
      this.ws.close(1000)
      this.ws = null
    }
    this.connected = false
    this.reconnectAttempts = this.maxReconnectAttempts // 阻止重连
  }

  scheduleReconnect(token) {
    this.reconnectAttempts++
    const delay = this.reconnectInterval * Math.min(this.reconnectAttempts, 3)
    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`)
    this.reconnectTimer = setTimeout(() => {
      this.connect(token)
    }, delay)
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this._heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, 30000)
  }

  stopHeartbeat() {
    if (this._heartbeatTimer) {
      clearInterval(this._heartbeatTimer)
      this._heartbeatTimer = null
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }

  // 事件监听
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
    return () => this.off(event, callback)
  }

  off(event, callback) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      const index = callbacks.indexOf(callback)
      if (index > -1) callbacks.splice(index, 1)
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(cb => {
        try { cb(data) } catch (e) { console.error('[WS] Listener error:', e) }
      })
    }
  }

  isConnected() {
    return this.connected && this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

const wsService = new WebSocketService()
export default wsService
