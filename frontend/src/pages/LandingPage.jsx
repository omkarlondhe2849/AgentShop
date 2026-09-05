import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  MessageSquare, ShieldCheck, Zap, Bot, ArrowRight,
  ShoppingCart, Sparkles, Layers, Activity, CreditCard,
  Brain, Globe, BarChart3, Lock, Workflow, Search
} from 'lucide-react'

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } }
}
const item = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] } }
}

export default function LandingPage() {
  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', color: 'var(--text-primary)' }}>

      {/* ── Hero ── */}
      <section style={{
        position: 'relative', padding: '7rem 2rem 5rem', textAlign: 'center',
        overflow: 'hidden',
        background: 'var(--gradient-hero-bg)',
      }}>
        {/* Grid pattern overlay */}
        <div style={{
          position: 'absolute', inset: 0, opacity: 0.06,
          backgroundImage: 'linear-gradient(var(--border-color) 1px, transparent 1px), linear-gradient(90deg, var(--border-color) 1px, transparent 1px)',
          backgroundSize: '60px 60px',
        }} />

        <div style={{ position: 'relative', zIndex: 1, maxWidth: '900px', margin: '0 auto' }}>
          <motion.div
            initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              padding: '6px 18px', borderRadius: '50px',
              border: '1px solid var(--pill-border)',
              background: 'var(--pill-bg)',
              fontSize: '0.85rem', color: 'var(--accent-primary)', fontWeight: 600,
              marginBottom: '2.5rem',
            }}
          >
            <Sparkles size={14} /> Razorpay Buildathon 2026 · AI Agentic Commerce
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            style={{
              fontSize: 'clamp(2.8rem, 6vw, 5rem)', fontWeight: 900,
              lineHeight: 1.05, letterSpacing: '-0.05em', marginBottom: '1.5rem',
              background: 'var(--gradient-heading)',
              WebkitBackgroundClip: 'text', color: 'transparent',
            }}
          >
            Commerce That<br />Thinks for Itself
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            style={{
              fontSize: '1.2rem', color: 'var(--text-secondary)', maxWidth: '650px',
              margin: '0 auto 3rem', lineHeight: 1.6, fontWeight: 400
            }}
          >
            A multi-agent AI storefront that autonomously recommends products, negotiates prices, and executes payments through Razorpay — no human intervention needed.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
            style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}
          >
            <Link to="/chat" className="btn-primary" style={{
              transition: 'transform 0.2s, box-shadow 0.2s',
            }}
              onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = 'var(--shadow-glow)' }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = 'none' }}
            >
              <Bot size={18} /> Try the AI Agent <ArrowRight size={16} />
            </Link>
            <Link to="/store" className="btn-secondary" style={{
              transition: 'all 0.2s',
            }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--border-active)'; e.currentTarget.style.color = 'var(--text-primary)' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border-color)'; e.currentTarget.style.color = 'var(--text-secondary)' }}
            >
              <ShoppingCart size={18} /> Browse Store
            </Link>
          </motion.div>

          {/* Tech stack pills */}
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.6, duration: 0.6 }}
            style={{
              marginTop: '4rem', display: 'flex', alignItems: 'center', justifyContent: 'center',
              gap: '12px', flexWrap: 'wrap',
            }}
          >
            {['Spring AI', 'Gemini 2.0', 'Razorpay APIs', 'Multi-Agent', 'RAG'].map(tech => (
              <span key={tech} style={{
                padding: '6px 16px', borderRadius: '8px', fontSize: '0.8rem',
                fontWeight: 500, color: 'var(--text-muted)',
                border: '1px solid var(--border-color)',
                background: 'var(--bg-secondary)',
              }}>{tech}</span>
            ))}
          </motion.div>
        </div>
      </section>

      {/* ── How It Works ── */}
      <section style={{ padding: '6rem 2rem', maxWidth: '1100px', margin: '0 auto' }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
          style={{ textAlign: 'center', marginBottom: '4rem' }}
        >
          <h2 style={{ fontSize: '2.5rem', fontWeight: 900, letterSpacing: '-0.03em', marginBottom: '1rem', color: 'var(--text-primary)' }}>
            How It Works
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '550px', margin: '0 auto' }}>
            Three simple steps from discovery to delivery — all powered by autonomous AI agents.
          </p>
        </motion.div>

        <motion.div
          variants={container} initial="hidden" whileInView="show" viewport={{ once: true }}
          style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '2rem' }}
        >
          {[
            { step: '01', icon: <Search size={24} />, title: 'Describe What You Want', desc: '"Find me wireless earbuds under ₹3,000 with good bass." The AI understands natural language and searches the catalog intelligently.', gradient: 'linear-gradient(135deg, #528FF0, #3d7de0)' },
            { step: '02', icon: <Brain size={24} />, title: 'Agent Recommends & Negotiates', desc: 'The Sales Agent finds the best matches, suggests coupons, cross-sells complementary products, and builds your cart autonomously.', gradient: 'linear-gradient(135deg, #7877c6, #5b5acd)' },
            { step: '03', icon: <CreditCard size={24} />, title: 'Secure Razorpay Checkout', desc: 'The Checkout Agent generates a secure Razorpay payment link. Bounded limits, audit trails, and graceful failure recovery built-in.', gradient: 'linear-gradient(135deg, #1ABC9C, #16a085)' },
          ].map((s, i) => (
            <motion.div key={s.step} variants={item} style={{
              background: 'var(--card-bg)', borderRadius: '20px', padding: '2.5rem',
              border: '1px solid var(--card-border)', position: 'relative', overflow: 'hidden',
              boxShadow: 'var(--shadow-sm)',
            }}>
              <span style={{ position: 'absolute', top: '20px', right: '24px', fontSize: '4rem', fontWeight: 900, color: 'var(--step-number-color)', lineHeight: 1 }}>
                {s.step}
              </span>
              <div style={{
                width: '48px', height: '48px', borderRadius: '12px',
                background: s.gradient,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: '1.5rem', color: '#fff',
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              }}>
                {s.icon}
              </div>
              <h3 style={{ fontSize: '1.3rem', fontWeight: 800, marginBottom: '0.75rem', color: 'var(--text-primary)' }}>{s.title}</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>{s.desc}</p>
            </motion.div>
          ))}
        </motion.div>
      </section>

      {/* ── Bento Feature Grid ── */}
      <section style={{ padding: '4rem 2rem 6rem', maxWidth: '1100px', margin: '0 auto' }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
          style={{ textAlign: 'center', marginBottom: '4rem' }}
        >
          <h2 style={{ fontSize: '2.5rem', fontWeight: 900, letterSpacing: '-0.03em', marginBottom: '1rem', color: 'var(--text-primary)' }}>
            Built Different
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '550px', margin: '0 auto' }}>
            Not just another chatbot wrapper. This is a production-grade agentic system.
          </p>
        </motion.div>

        <motion.div
          variants={container} initial="hidden" whileInView="show" viewport={{ once: true }}
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(6, 1fr)',
            gridAutoRows: 'auto',
            gap: '1.25rem',
          }}
        >
          {/* Big card */}
          <motion.div variants={item} style={{
            gridColumn: 'span 4', background: 'var(--card-bg)', borderRadius: '20px', padding: '2.5rem',
            border: '1px solid var(--card-border)', boxShadow: 'var(--shadow-sm)',
          }}>
            <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'var(--accent-primary-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem', color: 'var(--accent-primary)' }}>
              <Workflow size={24} />
            </div>
            <h3 style={{ fontSize: '1.6rem', fontWeight: 800, marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Multi-Agent Orchestration</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, fontSize: '1rem', maxWidth: '450px' }}>
              A central Orchestrator routes every user message to the right specialist — Sales Agent, Support Agent (RAG-powered), or Checkout Agent. Each has its own tools and guardrails.
            </p>
          </motion.div>

          {/* Accent card */}
          <motion.div variants={item} style={{
            gridColumn: 'span 2', background: 'linear-gradient(135deg, #7877c6, #5b5acd)', borderRadius: '20px', padding: '2rem',
            display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
            boxShadow: '0 8px 24px rgba(91, 90, 205, 0.25)',
          }}>
            <ShieldCheck size={36} color="rgba(255,255,255,0.9)" />
            <div style={{ marginTop: '1.5rem' }}>
              <h3 style={{ fontSize: '1.3rem', fontWeight: 800, marginBottom: '0.5rem', color: '#fff' }}>Bounded Execution</h3>
              <p style={{ fontSize: '0.95rem', color: 'rgba(255,255,255,0.8)', lineHeight: 1.5 }}>
                Max order limits. Explicit confirmations. Zero hidden fees.
              </p>
            </div>
          </motion.div>

          {/* Row of 3 */}
          <motion.div variants={item} style={{
            gridColumn: 'span 2', background: 'var(--card-bg)', borderRadius: '20px', padding: '2rem',
            border: '1px solid var(--card-border)', boxShadow: 'var(--shadow-sm)',
          }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'var(--accent-warning-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.25rem', color: 'var(--accent-warning)' }}>
              <Activity size={22} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Full Audit Trail</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, fontSize: '0.9rem' }}>
              Every agent action — from search to payment — is logged with reasoning and timestamps.
            </p>
          </motion.div>

          <motion.div variants={item} style={{
            gridColumn: 'span 2', background: 'var(--card-bg)', borderRadius: '20px', padding: '2rem',
            border: '1px solid var(--card-border)', boxShadow: 'var(--shadow-sm)',
          }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'var(--accent-success-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.25rem', color: 'var(--accent-success)' }}>
              <Globe size={22} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Agent-to-Agent API</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, fontSize: '0.9rem' }}>
              Schema.org structured APIs let external AI buyers autonomously browse and purchase.
            </p>
          </motion.div>

          <motion.div variants={item} style={{
            gridColumn: 'span 2', background: 'var(--card-bg)', borderRadius: '20px', padding: '2rem',
            border: '1px solid var(--card-border)', boxShadow: 'var(--shadow-sm)',
          }}>
            <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'var(--accent-danger-light)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.25rem', color: 'var(--accent-danger)' }}>
              <Zap size={22} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Graceful Recovery</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, fontSize: '0.9rem' }}>
              Payment failed? The agent explains why, retries, generates a fresh link. No dead ends.
            </p>
          </motion.div>
        </motion.div>
      </section>

      {/* ── CTA ── */}
      <section style={{
        padding: '6rem 2rem', textAlign: 'center',
        background: 'var(--cta-bg)',
        borderTop: '1px solid var(--border-color)',
      }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
        >
          <h2 style={{ fontSize: '2.5rem', fontWeight: 900, marginBottom: '1rem', letterSpacing: '-0.03em', color: 'var(--text-primary)' }}>
            Ready to see it in action?
          </h2>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '500px', margin: '0 auto 2.5rem', fontSize: '1.05rem' }}>
            Start a conversation with the AI agent. Watch it recommend, negotiate, and checkout — all autonomously.
          </p>
          <Link to="/chat" className="btn-primary" style={{
            display: 'inline-flex', alignItems: 'center', gap: '8px',
            padding: '16px 32px', fontSize: '1.05rem', fontWeight: 700,
            borderRadius: '12px', textDecoration: 'none',
            transition: 'transform 0.2s, box-shadow 0.2s',
          }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = 'var(--shadow-glow)' }}
            onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = 'none' }}
          >
            <MessageSquare size={18} /> Start Shopping Now <ArrowRight size={16} />
          </Link>
        </motion.div>
      </section>
    </div>
  )
}
