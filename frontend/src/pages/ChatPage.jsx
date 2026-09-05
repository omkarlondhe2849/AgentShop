import { useState, useRef, useEffect, useCallback } from 'react'
import {
  Send, Bot, User, ShoppingCart, Sparkles, RefreshCw, Trash2,
  ExternalLink, CheckCircle2, AlertCircle, ArrowRight, Shield, Zap, Package,
  MapPin, Mic, MicOff, Volume2, VolumeX, Copy, Check, Clock, Download, ThumbsUp, ThumbsDown, Heart
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import ReactMarkdown from 'react-markdown'
import { sendMessage, getConversationHistory, createSession, simulatePaymentSuccess, simulatePaymentFailure } from '../services/api'
import { toast } from 'sonner'

// ── Utility: strip markdown for TTS ──
function stripMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/#{1,6}\s?/g, '')
    .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/[_~]/g, '')
    .replace(/[-•]\s/g, '')
    .replace(/\n{2,}/g, '. ')
    .replace(/\n/g, ' ')
    .trim()
}

// ── Utility: format timestamp ──
function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
}

export default function ChatPage() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [sessionId, setSessionId] = useState(null)
  const [activeCategory, setActiveCategory] = useState(null)
  const [isListening, setIsListening] = useState(false)
  const [speakingMsgId, setSpeakingMsgId] = useState(null)
  const [copiedMsgId, setCopiedMsgId] = useState(null)
  const [reactions, setReactions] = useState({}) // { msgId: 'up' | 'down' | 'heart' }
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)
  const recognitionRef = useRef(null)

  // ── Browser Notifications Init ──
  useEffect(() => {
    if ('Notification' in window && Notification.permission !== 'granted' && Notification.permission !== 'denied') {
      Notification.requestPermission()
    }
  }, [])

  // ── Speech Recognition init ──
  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    if (SpeechRecognition) {
      const rec = new SpeechRecognition()
      rec.continuous = false
      rec.interimResults = false
      rec.lang = 'en-IN'
      recognitionRef.current = rec
    }
  }, [])

  const toggleListen = () => {
    const rec = recognitionRef.current
    if (!rec) {
      toast.error('Voice shopping is not supported in this browser.')
      return
    }
    if (isListening) {
      rec.stop()
      setIsListening(false)
    } else {
      rec.onresult = (event) => {
        const transcript = event.results[0][0].transcript
        setInput(transcript)
        setIsListening(false)
        handleSend(transcript)
      }
      rec.onerror = () => setIsListening(false)
      rec.onend = () => setIsListening(false)
      rec.start()
      setIsListening(true)
      toast.info('🎙️ Listening... Speak now!')
    }
  }

  // ── Text-to-Speech ──
  const handleSpeak = useCallback((msgId, content) => {
    const synth = window.speechSynthesis
    if (!synth) {
      toast.error('Text-to-Speech not supported in this browser.')
      return
    }
    if (speakingMsgId === msgId) {
      synth.cancel()
      setSpeakingMsgId(null)
      return
    }
    synth.cancel()
    const text = stripMarkdown(content)
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.rate = 1
    utterance.pitch = 1
    utterance.lang = 'en-IN'

    // Try to pick a good voice
    const voices = synth.getVoices()
    const preferred = voices.find(v => v.lang.startsWith('en') && v.name.toLowerCase().includes('female'))
      || voices.find(v => v.lang.startsWith('en-IN'))
      || voices.find(v => v.lang.startsWith('en'))
    if (preferred) utterance.voice = preferred

    utterance.onend = () => setSpeakingMsgId(null)
    utterance.onerror = () => setSpeakingMsgId(null)
    setSpeakingMsgId(msgId)
    synth.speak(utterance)
  }, [speakingMsgId])

  // Clean up speech on unmount
  useEffect(() => {
    return () => window.speechSynthesis?.cancel()
  }, [])

  // ── Copy to Clipboard ──
  const handleCopy = useCallback(async (msgId, content) => {
    try {
      const text = stripMarkdown(content)
      await navigator.clipboard.writeText(text)
      setCopiedMsgId(msgId)
      toast.success('Copied to clipboard!')
      setTimeout(() => setCopiedMsgId(null), 2000)
    } catch {
      toast.error('Failed to copy')
    }
  }, [])

  // ── Emoji Reactions ──
  const toggleReaction = (msgId, type) => {
    setReactions(prev => {
      const current = prev[msgId]
      if (current === type) {
        const copy = { ...prev }
        delete copy[msgId]
        return copy
      }
      return { ...prev, [msgId]: type }
    })
  }

  // ── Export Chat ──
  const handleDownloadChat = () => {
    if (messages.length === 0) return
    let content = "🛒 AgentShop Session Transcript\n"
    content += "Generated on: " + new Date().toLocaleString('en-IN') + "\n\n"
    content += "=========================================\n\n"
    
    messages.forEach(msg => {
      const role = msg.role === 'user' ? 'You' : 'ShopBot AI'
      content += `[${formatTime(msg.timestamp)}] ${role}:\n`
      content += stripMarkdown(msg.content) + "\n\n"
      content += "-----------------------------------------\n\n"
    })

    const blob = new Blob([content], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `agentshop-transcript-${Date.now()}.txt`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    toast.success('Chat transcript downloaded!')
  }

  // ── Auto-resize textarea ──
  const handleInputChange = (e) => {
    setInput(e.target.value)
    const el = e.target
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }

  const getSessionKey = () => {
    const userStr = localStorage.getItem('agentShopUser')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        return `agentshop_active_session_id_${user.email}`
      } catch (e) {}
    }
    return 'agentshop_active_session_id_guest'
  }

  // Initialize or restore persistent session
  useEffect(() => {
    initOrRestoreSession()
    
    const handleAuthChange = () => {
      setMessages([]) // Clear UI on auth change
      initOrRestoreSession()
    }
    
    window.addEventListener('auth-change', handleAuthChange)
    return () => window.removeEventListener('auth-change', handleAuthChange)
  }, [])

  const initOrRestoreSession = async () => {
    const sessionKey = getSessionKey()
    let currentId = localStorage.getItem(sessionKey)
    
    if (!currentId) {
      try {
        const { sessionId: newId } = await createSession()
        currentId = newId
        localStorage.setItem(sessionKey, currentId)
      } catch (e) {
        currentId = 'session-' + Date.now()
        localStorage.setItem(sessionKey, currentId)
      }
    }
    
    setSessionId(currentId)
    await loadHistory(currentId)
  }

  const loadHistory = async (sid) => {
    try {
      const history = await getConversationHistory(sid)
      if (history && history.length > 0) {
        const formatted = history.map((item, idx) => ({
          id: item.id ? item.id.toString() : idx.toString(),
          role: item.role ? item.role.toLowerCase() : 'assistant',
          content: item.content,
          timestamp: item.timestamp ? new Date(item.timestamp).getTime() : Date.now()
        }))
        setMessages(formatted)
      } else {
        // First-time welcome
        setMessages([{
          id: 'welcome',
          role: 'assistant',
          content: `👋 **Welcome to AgentShop!** I'm your AI Shopping Copilot.\n\nI can help you:\n- 🔍 **Discover & compare** products across Electronics, Fashion, Home & Food\n- 🎯 **Filter by budget** (e.g. *"Show earbuds under ₹3,000"*)\n- 🛒 **Manage your cart** and automatically receive intelligent upsell deals\n- 💳 **Complete bounded checkout** with Razorpay\n\nHow can I help you today? ✨`,
          timestamp: Date.now()
        }])
      }
    } catch (err) {
      console.warn('Could not load past history:', err)
      setMessages([{
        id: 'welcome',
        role: 'assistant',
        content: `👋 **Welcome back!** How can I assist you with your shopping today?`,
        timestamp: Date.now()
      }])
    }
  }

  const handleResetSession = async () => {
    try {
      window.speechSynthesis?.cancel()
      setSpeakingMsgId(null)
      const { sessionId: newId } = await createSession()
      const sessionKey = getSessionKey()
      localStorage.setItem(sessionKey, newId)
      setSessionId(newId)
      setMessages([{
        id: 'welcome-' + Date.now(),
        role: 'assistant',
        content: `✨ **New session started!** What products would you like to explore?`,
        timestamp: Date.now()
      }])
      toast.success('Started a fresh shopping session')
    } catch (err) {
      toast.error('Failed to create new session')
    }
  }

  // Scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const handleSend = async (customText = null) => {
    const textToSend = typeof customText === 'string' ? customText : input.trim()
    if (!textToSend || isLoading) return

    const userMsg = {
      id: 'msg-' + Date.now(),
      role: 'user',
      content: textToSend,
      timestamp: Date.now(),
    }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    // Reset textarea height
    if (inputRef.current) inputRef.current.style.height = 'auto'
    setIsLoading(true)

    try {
      const response = await sendMessage(sessionId, textToSend)
      const assistantMsg = {
        id: 'resp-' + Date.now(),
        role: 'assistant',
        content: response.message || 'I processed your request.',
        timestamp: response.timestamp || Date.now(),
      }
      setMessages(prev => [...prev, assistantMsg])

      if (response.message && (response.message.includes('Payment Link:') || response.message.includes('Click here to pay'))) {
        toast.success('Razorpay Order & Payment Link ready!', { duration: 4000 })
      }
    } catch (err) {
      console.error('Chat error:', err)
      const errorMsg = {
        id: 'err-' + Date.now(),
        role: 'assistant',
        content: '⚠️ I had a temporary issue connecting to the store engine. Please try sending your message again.',
        timestamp: Date.now(),
      }
      setMessages(prev => [...prev, errorMsg])
      toast.error('Connection issue with backend')
    } finally {
      setIsLoading(false)
      inputRef.current?.focus()
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleSimulatePayment = async (orderIdString) => {
    try {
      toast.loading('Simulating Razorpay webhook callback...')
      const res = await simulatePaymentSuccess(orderIdString)
      toast.dismiss()
      toast.success(`🎉 Payment Captured for Order #${res.orderId || ''}! Cart cleared & logged.`)
      
      // Fire browser notification
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('Payment Successful!', {
          body: `Order #${res.orderId || '1'} has been confirmed. Thank you!`,
          icon: '/vite.svg'
        })
      }

      setMessages(prev => [...prev, {
        id: 'sim-pay-' + Date.now(),
        role: 'assistant',
        content: `🎉 **Payment Verified & Captured!**\n\n- **Order ID:** #${res.orderId || '1'}\n- **Amount Paid:** ₹${res.amount || '0'}\n- **Status:** **PAID** (Verified via Razorpay Webhook)\n\nYour items will be shipped shortly. Thank you for shopping with AgentShop! 🚚`,
        timestamp: Date.now()
      }])
    } catch (e) {
      toast.dismiss()
      toast.error('Could not simulate payment: ' + e.message)
    }
  }

  const handleSimulateFailure = async (orderIdString) => {
    try {
      toast.loading('Simulating Razorpay payment failure...')
      const res = await simulatePaymentFailure(orderIdString)
      toast.dismiss()
      toast.error(`❌ Payment Failed for Order #${res.orderId || ''}!`)
      
      setMessages(prev => [...prev, {
        id: 'sim-fail-' + Date.now(),
        role: 'assistant',
        content: `❌ **Payment Failed!**\n\n- **Order ID:** #${res.orderId || '1'}\n- **Status:** **FAILED** (Webhook notified)\n- **Reason:** Bank declined transaction (simulated)\n\nNo money was deducted. You can safely retry checkout.`,
        timestamp: Date.now()
      }])
    } catch (e) {
      toast.dismiss()
      toast.error('Could not simulate failure: ' + e.message)
    }
  }

  // Extract Razorpay order ID or link from message content
  const extractPaymentLink = (content) => {
    if (!content) return null
    const linkMatch = content.match(/https:\/\/rzp\.io\/i\/([a-zA-Z0-9_-]+)/)
    const orderMatch = content.match(/Order #(\d+)/i)
    if (linkMatch) {
      return {
        url: linkMatch[0],
        paymentLinkId: linkMatch[1],
        orderId: orderMatch ? orderMatch[1] : '1',
        mockOrderId: 'order_mock_' + linkMatch[1]
      }
    }
    return null
  }

  const categories = [
    { name: 'Electronics', query: 'Show me best electronics & gadgets' },
    { name: 'Fashion', query: 'Show me trending fashion & clothing' },
    { name: 'Home & Kitchen', query: 'Show me kitchen appliances' },
    { name: 'Food & Beverages', query: 'Show me organic snacks & coffee' }
  ]

  const quickPrompts = [
    "Wireless earbuds under ₹3,000",
    "Smartphones with top cameras",
    "What's currently in my cart?",
    "Proceed to checkout"
  ]

  return (
    <div className="chat-page-container">
      {/* Left Sidebar */}
      <aside className="chat-sidebar">
        <div className="chat-sidebar-header">
          <span className="sidebar-title">
            <Sparkles size={16} color="var(--rzp-blue)" />
            Shopping Controls
          </span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={handleResetSession}
            title="Start fresh conversation"
            style={{ padding: '4px 8px' }}
          >
            <RefreshCw size={13} />
            <span>New Chat</span>
          </button>
        </div>

        {/* Categories */}
        <div>
          <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Browse Departments
          </div>
          <div className="category-pills">
            {categories.map((c) => (
              <button
                key={c.name}
                className={`category-pill ${activeCategory === c.name ? 'active' : ''}`}
                onClick={() => {
                  setActiveCategory(c.name)
                  handleSend(c.query)
                }}
              >
                {c.name}
              </button>
            ))}
          </div>
        </div>

        {/* Quick Budget Filters */}
        <div>
          <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Quick Budget Searches
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <button className="btn btn-secondary btn-sm" style={{ justifyContent: 'flex-start' }} onClick={() => handleSend("Show me items under ₹1500")}>
              💰 Budget Picks (Under ₹1,500)
            </button>
            <button className="btn btn-secondary btn-sm" style={{ justifyContent: 'flex-start' }} onClick={() => handleSend("Show me headphones under ₹5000")}>
              🎧 Audio Gear (Under ₹5,000)
            </button>
            <button className="btn btn-secondary btn-sm" style={{ justifyContent: 'flex-start' }} onClick={() => handleSend("Show me premium devices under ₹50000")}>
              📱 Flagship Tech (Under ₹50,000)
            </button>
          </div>
        </div>

        {/* Guardrail Bounding Banner */}
        <div style={{ marginTop: 'auto' }} className="guardrail-banner">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', fontWeight: 700, color: 'var(--rzp-blue)' }}>
            <Shield size={14} />
            Bounded Money Guardrails
          </div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', marginTop: '4px', lineHeight: 1.5 }}>
            Max transaction bound: <strong>₹50,000</strong>. Every tool reasoning is logged to the merchant audit trail.
          </div>
        </div>
      </aside>

      {/* Main Chat Area */}
      <main className="chat-main-stage">
        {/* Top Chat Bar */}
        <div className="chat-header-bar">
          <div className="bot-profile">
            <div className="bot-avatar">
              <Bot size={20} />
              <div className="online-pulse" />
            </div>
            <div className="bot-info">
              <h3>
                ShopBot AI Copilot
                <span className="partner-badge" style={{ fontSize: '0.68rem', padding: '2px 8px' }}>
                  Gemini Flash 2.0
                </span>
              </h3>
              <div className="bot-status">● Ready & connected to live catalog</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              className="btn btn-secondary btn-sm"
              onClick={handleDownloadChat}
              title="Download chat transcript"
            >
              <Download size={14} />
              <span>Export</span>
            </button>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => handleSend("View my cart")}
            >
              <ShoppingCart size={14} />
              <span>Cart</span>
            </button>
            <button
              className="btn btn-primary btn-sm"
              onClick={() => handleSend("Proceed to checkout")}
            >
              <Zap size={14} />
              <span>Checkout</span>
            </button>
          </div>
        </div>

        {/* Message Feed */}
        <div className="chat-feed">
          <AnimatePresence>
            {messages.map((msg) => {
              const paymentInfo = msg.role === 'assistant' ? extractPaymentLink(msg.content) : null
              const isAssistant = msg.role === 'assistant'

              return (
                <motion.div
                  key={msg.id}
                  className={`message-row ${msg.role}`}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <div className="msg-bubble">
                    {msg.content === '__ACTION_REQUEST_ADDRESS__' ? (
                      (() => {
                        const msgIndex = messages.findIndex(m => m.id === msg.id);
                        const isSubmitted = messages.slice(msgIndex + 1).some(m => 
                          (m.role === 'user' && m.content.startsWith('Deliver to:')) ||
                          (m.role === 'assistant' && (m.content.includes('Payment Link') || m.content.includes('Order #')))
                        );
                        
                        if (isSubmitted) {
                          return (
                            <div className="address-form-container" style={{ padding: '16px', background: 'var(--bg-tertiary)', borderRadius: '12px', border: '1px solid var(--border-color)', opacity: 0.7 }}>
                              <h4 style={{ margin: '0', fontSize: '1rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <CheckCircle2 size={16} color="var(--rzp-teal)" /> Address submitted
                              </h4>
                            </div>
                          );
                        }
                        
                        return (
                      <div className="address-form-container" style={{ padding: '16px', background: 'var(--bg-tertiary)', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
                        <h4 style={{ margin: '0 0 12px 0', fontSize: '1rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <MapPin size={16} color="var(--rzp-blue)" /> Where should we deliver?
                        </h4>
                        <form onSubmit={async (e) => {
                          e.preventDefault()
                          const address = e.target.elements.address.value
                          if (!address.trim()) return
                          
                          setMessages(prev => [...prev, {
                            id: Date.now(),
                            role: 'user',
                            content: `Deliver to: ${address}`,
                            timestamp: Date.now(),
                          }])
                          
                          setIsLoading(true)
                          try {
                            const userStr = localStorage.getItem('agentShopUser')
                            const user = userStr ? JSON.parse(userStr) : { name: 'Guest', email: 'guest@agentshop.com' }
                            const res = await fetch(`/api/v1/checkout?sessionId=${sessionId}`, {
                              method: 'POST',
                              headers: { 'Content-Type': 'application/json' },
                              body: JSON.stringify({
                                sessionId: sessionId,
                                customerName: user.name,
                                customerEmail: user.email,
                                customerPhone: "9999999999",
                                deliveryAddress: address
                              })
                            })
                            
                            const data = await res.json()
                            if (res.ok) {
                              setMessages(prev => [...prev, {
                                id: Date.now(),
                                role: 'assistant',
                                content: data.message || `Checkout successful!`,
                                timestamp: Date.now(),
                              }])
                            } else {
                              toast.error(data.error || 'Checkout failed')
                              setMessages(prev => [...prev, {
                                id: Date.now(),
                                role: 'assistant',
                                content: `❌ Error: ${data.error}`,
                                timestamp: Date.now(),
                              }])
                            }
                          } catch (err) {
                            toast.error('Failed to communicate with server')
                          } finally {
                            setIsLoading(false)
                          }
                        }}>
                          <textarea 
                            name="address" 
                            required
                            placeholder="Enter your full delivery address..."
                            rows={3}
                            style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid var(--border-color)', marginBottom: '10px', fontSize: '0.9rem', fontFamily: 'var(--font-body)', background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
                          />
                          <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
                            <Zap size={14} /> Proceed to Payment
                          </button>
                        </form>
                      </div>
                      );
                    })()
                    ) : (
                    (() => {
                      const contentStr = msg.content || '';
                      const parts = contentStr.split(/__PRODUCT_LIST_START__([\s\S]*?)__PRODUCT_LIST_END__/);
                      
                      return parts.map((part, index) => {
                        if (index % 2 === 1) { // This is the JSON part
                          try {
                            const products = JSON.parse(part);
                            return (
                              <div key={index} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', margin: '15px 0' }}>
                                {products.map(p => (
                                  <div key={p.id} className="product-card" style={{ display: 'flex', flexDirection: 'column', padding: '14px', background: 'var(--bg-secondary)', borderRadius: '12px', border: '1px solid var(--border-color)', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', transition: 'transform 0.2s, box-shadow 0.2s' }}>
                                    {p.imageUrl && (
                                      <div style={{ marginBottom: '10px', borderRadius: '8px', overflow: 'hidden', height: '120px' }}>
                                        <img src={p.imageUrl} alt={p.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                      </div>
                                    )}
                                    <div style={{ fontWeight: 'bold', fontSize: '0.95rem', color: 'var(--text-primary)', marginBottom: '8px' }}>{p.name}</div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>{p.description?.substring(0, 60)}...</div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto', paddingTop: '12px' }}>
                                      <div style={{ fontWeight: 800, color: 'var(--rzp-blue)' }}>₹{p.price || p.offers?.price}</div>
                                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>⭐ {p.rating || p.aggregateRating?.ratingValue}</div>
                                    </div>
                                    {p.matchScore && (
                                      <div style={{ marginTop: '8px', padding: '4px 8px', background: 'var(--success-color)', color: '#fff', borderRadius: '4px', fontSize: '0.7rem', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '4px', width: 'fit-content' }}>
                                        🎯 {p.matchScore}% Match
                                      </div>
                                    )}
                                    <button 
                                      className="btn btn-primary btn-sm"
                                      style={{ marginTop: '12px', width: '100%' }}
                                      onClick={() => handleSend(`add ${p.id}`)}
                                      disabled={isLoading}
                                    >
                                      <ShoppingCart size={14} /> Add to Cart
                                    </button>
                                  </div>
                                ))}
                              </div>
                            );
                          } catch(e) {
                            return <div key={index} style={{ color: 'red', fontSize: '0.8rem' }}>Error parsing products</div>;
                          }
                        }
                        
                        // Parse coupons inside the non-JSON text part
                        const couponParts = part.split(/__COUPON_LIST_START__([\s\S]*?)__COUPON_LIST_END__/);
                        
                        return couponParts.map((cPart, cIndex) => {
                          if (cIndex % 2 === 1) { // COUPON JSON
                            try {
                              const coupons = JSON.parse(cPart);
                              return (
                                <div key={`coupon-${index}-${cIndex}`} style={{ display: 'grid', gap: '8px', margin: '15px 0' }}>
                                  {coupons.map(c => (
                                    <div key={c.code} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', background: 'var(--pill-bg)', border: '1px dashed var(--pill-border)', borderRadius: '8px' }}>
                                      <div>
                                        <div style={{ fontWeight: 'bold', color: 'var(--rzp-blue)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                          <Sparkles size={14} /> {c.code} - {c.title}
                                        </div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{c.description}</div>
                                      </div>
                                      <button 
                                        className="btn btn-secondary btn-sm" 
                                        onClick={() => handleSend(`apply ${c.code}`)}
                                        disabled={isLoading}
                                      >
                                        Apply
                                      </button>
                                    </div>
                                  ))}
                                </div>
                              );
                            } catch(e) {
                              return <div key={`coupon-${index}-${cIndex}`} style={{ color: 'red', fontSize: '0.8rem' }}>Error parsing coupons</div>;
                            }
                          }
                          
                          if (!cPart.trim()) return null;
                          return (
                            <ReactMarkdown
                              key={`md-${index}-${cIndex}`}
                              components={{
                                a: ({ href, children }) => (
                                  <a href={href} target="_blank" rel="noopener noreferrer" style={{ fontWeight: 700 }}>
                                    {children}
                                  </a>
                                ),
                              }}
                            >
                              {cPart}
                            </ReactMarkdown>
                          );
                        });
                      });
                    })()
                    )}

                    {/* Interactive Embedded Checkout Card */}
                    {paymentInfo && (
                      <div className="checkout-action-card">
                        <div className="checkout-header">
                          <span className="rzp-tag">
                            <Zap size={12} /> Checkout Options
                          </span>
                          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                            Order #{paymentInfo.orderId}
                          </span>
                        </div>
                        
                        <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
                          <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                            Choose your preferred payment method:
                          </div>

                          {/* Razorpay Option */}
                          <div style={{ padding: '16px', border: '1px solid var(--border-color)', borderRadius: '12px', background: 'var(--bg-primary)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                              <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <Shield size={16} color="var(--rzp-blue)" /> Pay Online (Razorpay)
                              </div>
                              <div style={{ fontSize: '0.7rem', background: 'var(--pill-bg)', color: 'var(--rzp-blue)', padding: '4px 10px', borderRadius: '12px', fontWeight: 700 }}>
                                Extra 15% Off
                              </div>
                            </div>
                            <div className="checkout-actions-btns">
                              <a
                                href={paymentInfo.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="btn btn-primary btn-sm"
                                style={{ flex: 1, padding: '10px' }}
                              >
                                <ExternalLink size={14} />
                                <span>Pay on Razorpay</span>
                              </a>
                              <button
                                className="btn btn-success btn-sm"
                                style={{ flex: 1 }}
                                onClick={() => handleSimulatePayment(paymentInfo.orderId || paymentInfo.paymentLinkId)}
                              >
                                <CheckCircle2 size={14} />
                                <span>Simulate Success</span>
                              </button>
                            </div>
                          </div>

                          {/* Cash on Delivery Option */}
                          <div style={{ padding: '16px', border: '1px solid var(--border-color)', borderRadius: '12px', background: 'var(--bg-primary)' }}>
                            <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px' }}>
                              <Package size={16} color="var(--text-primary)" /> Cash on Delivery (COD)
                            </div>
                            <button
                              className="btn btn-secondary btn-sm"
                              style={{ width: '100%', padding: '10px' }}
                              onClick={() => {
                                toast.success(`Order #${paymentInfo.orderId} placed with Cash on Delivery!`);
                                setMessages(prev => [...prev, {
                                  id: 'sim-pay-' + Date.now(),
                                  role: 'assistant',
                                  content: `🎉 **Order Confirmed via Cash on Delivery!**\n\n- **Order ID:** #${paymentInfo.orderId}\n- **Status:** **CONFIRMED**\n\nPlease keep exact cash ready at the time of delivery. Your items will be shipped shortly. 🚚`,
                                  timestamp: Date.now()
                                }]);
                              }}
                            >
                              <CheckCircle2 size={14} /> Place COD Order
                            </button>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* ── Footer: Timestamp + Action Buttons ── */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <span className="msg-timestamp">
                      {formatTime(msg.timestamp)}
                    </span>
                    {isAssistant && msg.content !== '__ACTION_REQUEST_ADDRESS__' && (
                      <div className="msg-action-bar">
                        <button
                          className={`msg-action-btn ${speakingMsgId === msg.id ? 'active' : ''}`}
                          onClick={() => handleSpeak(msg.id, msg.content)}
                          title={speakingMsgId === msg.id ? 'Stop reading' : 'Read aloud'}
                        >
                          {speakingMsgId === msg.id ? <VolumeX size={14} /> : <Volume2 size={14} />}
                          <span className="tooltip-text">{speakingMsgId === msg.id ? 'Stop' : 'Read aloud'}</span>
                        </button>
                        <button
                          className={`msg-action-btn ${copiedMsgId === msg.id ? 'active' : ''}`}
                          onClick={() => handleCopy(msg.id, msg.content)}
                          title="Copy to clipboard"
                        >
                          {copiedMsgId === msg.id ? <Check size={14} /> : <Copy size={14} />}
                          <span className="tooltip-text">{copiedMsgId === msg.id ? 'Copied!' : 'Copy'}</span>
                        </button>
                        <div style={{ width: '1px', height: '12px', background: 'var(--border-color)', margin: '0 4px' }} />
                        <button className={`msg-action-btn ${reactions[msg.id] === 'up' ? 'active-green' : ''}`} onClick={() => toggleReaction(msg.id, 'up')} title="Helpful">
                          <ThumbsUp size={13} />
                        </button>
                        <button className={`msg-action-btn ${reactions[msg.id] === 'down' ? 'active-red' : ''}`} onClick={() => toggleReaction(msg.id, 'down')} title="Not helpful">
                          <ThumbsDown size={13} />
                        </button>
                        <button className={`msg-action-btn ${reactions[msg.id] === 'heart' ? 'active-red' : ''}`} onClick={() => toggleReaction(msg.id, 'heart')} title="Love this">
                          <Heart size={13} />
                        </button>
                      </div>
                    )}
                  </div>
                </motion.div>
              )
            })}
          </AnimatePresence>

          {/* Reasoning Visualizer (Typing indicator) */}
          {isLoading && (
            <motion.div
              className="message-row assistant"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            >
              <div className="msg-bubble reasoning-bubble" style={{ display: 'flex', flexDirection: 'column', gap: '8px', padding: '12px 18px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Bot size={16} color="var(--rzp-blue)" className="spin-slow" />
                  <span style={{ fontSize: '0.82rem', color: 'var(--rzp-blue)', fontWeight: 700 }}>
                    Agent Reasoning...
                  </span>
                </div>
                <div style={{ paddingLeft: '24px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                   <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }} style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}><CheckCircle2 size={12} color="var(--rzp-teal)" /> Parsing user intent</motion.div>
                   <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.8 }} style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '6px' }}><RefreshCw size={12} color="var(--rzp-blue)" className="spin-fast" /> Executing tools & checking bounds</motion.div>
                </div>
              </div>
            </motion.div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Suggestion Chips */}
        <div style={{ padding: '0 1.5rem 8px', display: 'flex', gap: '8px', overflowX: 'auto', background: 'var(--bg-secondary)' }}>
          {quickPrompts.map((prompt) => (
            <button
              key={prompt}
              className="category-pill"
              style={{ whiteSpace: 'nowrap', fontSize: '0.75rem' }}
              onClick={() => handleSend(prompt)}
            >
              {prompt}
            </button>
          ))}
        </div>

        {/* Chat Input Container */}
        <div className="chat-input-container">
          <div className="input-box-wrapper">
            <textarea
              ref={inputRef}
              className="chat-textarea"
              placeholder="Ask anything (e.g. 'Add product 2', 'Earbuds under ₹3000', 'Checkout')..."
              value={input}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              rows={1}
              disabled={isLoading}
            />
            <button
              className="voice-btn"
              onClick={toggleListen}
              disabled={isLoading}
              title={isListening ? 'Stop listening' : 'Voice Shopping — Click to speak'}
            >
              {isListening ? (
                <span className="voice-btn-inner listening">
                  <MicOff size={16} />
                </span>
              ) : (
                <span className="voice-btn-inner">
                  <Mic size={16} />
                </span>
              )}
            </button>
            <button
              className="chat-send-btn"
              onClick={() => handleSend()}
              disabled={!input.trim() || isLoading}
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}
