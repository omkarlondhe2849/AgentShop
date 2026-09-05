# AgentShop — Agentic Commerce on Razorpay
**Track 01: AI Growth & Agentic Commerce**

AgentShop is an orchestrated specialist-agent e-commerce platform that uses **Generative UI** to turn natural language into interactive React checkout components, with every money action explainable, bounded, and gated.

---

## 🎯 Problem Taste
E-commerce UIs are passive: users drill through categories, filters, and pages. This limits merchant revenue and makes stores completely opaque to AI buyers.

AgentShop replaces this with an **intent-driven shopping loop**. A user states what they want, a Supervisor Orchestrator routes the intent to a specialist agent (Sales or Checkout), and the agent executes strictly typed Java tools that return structured JSON. The React frontend intercepts this JSON and renders native, interactive checkout components mid-conversation — Generative UI.

## 🛠️ Build Quality

### Frontend (React · Vite · Framer Motion)
- **Generative UI Parser**: Reads the agent's response stream, extracts custom delimiter tags (`__PRODUCT_LIST_START__`, `__COUPON_LIST_START__`), and safely renders interactive React cards mid-conversation. All rendering happens client-side; the LLM never generates HTML.
- **Insights Dashboard**: Built with Recharts — Top Products, Revenue Trends, Category Distribution, Agent Revenue Leaderboard.
- **Audit Trail Viewer**: Filterable, searchable table of every AI action with session IDs and timestamps.

### Backend (Spring Boot 3 · Java 25 · Spring AI)
- **Supervisor Orchestrator**: Routes intents to isolated `SalesAgent` / `CheckoutAgent` classes. The Sales Agent hands cart context forward to the Checkout Agent mid-session — real inter-agent coordination, not just prompt switching.
- **Checkout Pipeline**: A chain of `CheckoutValidator` implementations run sequentially before any Razorpay API call is made:
  - `CartNotEmptyValidator` → `DiscountCeilingValidator` → `MaxAmountValidator` → `AddressValidator`
- **Asynchronous Audit Trail**: An event-driven `@Async` logging system that tracks every tool execution into an immutable log.

## 🤖 AI Judgment (The right tool in the right place)
The LLM is never allowed to touch the database or Razorpay directly. It can only output JSON intents that trigger strictly typed Java tools (`ShoppingTools.java`). When a user asks to checkout:

1. The LLM calls the `initiateCheckout` tool
2. The tool calculates the cart total on the backend
3. The `CheckoutPipeline` runs all validators (discount ceiling, max amount, etc.)
4. Only if every gate passes does the pipeline call the Razorpay API
5. The generated payment link URL is returned to the LLM, which wraps it in Generative UI for the user to click

The LLM can *request* — but the tool layer *decides*.

## 🔐 Explainable, Bounded, Gated

| Requirement | How we satisfy it | Code reference |
|---|---|---|
| **Explainable** | Every tool call (search, cart add, checkout, payment link) fires a Spring Event that the `AuditEventListener` persists asynchronously. The `/audit-trail` page renders the full session trace. | `AuditService.java`, `AuditEventListener.java` |
| **Bounded** | `MaxAmountValidator` enforces a merchant-configured ceiling on single order totals (₹500,000). `CheckoutPipeline` runs this before any Razorpay call. | `MaxAmountValidator.java`, `application.yml` |
| **Gated** | `DiscountCeilingValidator` enforces a hard 30% discount cap. If the LLM hallucinated a 90% coupon, the tool layer blocks checkout before Razorpay ever sees the amount. Idempotency guard in `CheckoutPipeline` prevents duplicate payment links on double-clicks (60s cooldown per session). | `DiscountCeilingValidator.java`, `CheckoutPipeline.java` |

## 🚨 What Broke at 2 AM

**1. The `@Async` Audit Crash**
Our `AuditService.logAction()` was annotated with `@Async` but returned `AuditLog` (a JPA entity). Spring's CGLIB proxy explicitly forbids returning domain objects from async methods — it can only handle `void` or `Future`. The checkout agent started failing silently because the audit event listener was crashing on every event. Fix: changed the return type to `void`. The proxy wrapper worked, the audit trail resumed, and checkout stopped crashing.

**2. The Generative UI Delimiter System**
We wanted the LLM to drive the frontend UI, but it kept mixing markdown with JSON, crashing `JSON.parse()` and taking down the chat page. We engineered a strict delimiter tagging system (`__PRODUCT_LIST_START__`, `__COUPON_LIST_START__`). The frontend parser splits the response at these tags, renders markdown normally, and only JSON-parses the isolated payloads. This decoupled approach also massively reduced token usage — the LLM now outputs a small structured payload instead of verbose UI descriptions.

**3. Prompt Injection → Discount Gate**
During testing, we tried asking the chatbot to "apply a 90% discount" directly. The coupon system didn't have one, but we realized nothing *structurally* prevented a future prompt injection from exploiting this. We added the `DiscountCeilingValidator` as a hard gate in the checkout pipeline: any discount exceeding the merchant-configured ceiling (30%) is blocked before Razorpay sees it. The LLM can request anything — the tool layer gates it.

## 🔮 What We'd Build Next
- **Rate limiting on tool calls**: Cap the number of searches/cart mutations per session to prevent abuse loops.
- **Razorpay webhook signature verification**: Validate `X-Razorpay-Signature` on payment callbacks for production deployment.
- **Multi-turn memory across sessions**: Persist user preferences (e.g., "prefers wireless headphones") across sessions for personalized upselling.
- **Fraud detection**: Flag sessions with repeated discount application/removal cycles.

---

## 🚀 How to Run Locally

### Prerequisites
- Java 25+, Node.js 18+, Maven

### Backend
```bash
cd backend
./apache-maven-3.9.6/bin/mvn.cmd spring-boot:run
# Runs on http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install && npm run dev
# Runs on http://localhost:5173
```

### Configuration
Razorpay keys and merchant limits are in `backend/src/main/resources/application.yml`:
```yaml
razorpay:
  key-id: rzp_test_YOUR_KEY_HERE
  key-secret: YOUR_KEY_SECRET_HERE

agentshop:
  max-order-amount: 50000000      # ₹500,000 ceiling
  max-discount-percent: 30        # Hard discount gate
```

---
*Built for the Razorpay Buildathon 2026*
