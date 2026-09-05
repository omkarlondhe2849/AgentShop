const API_BASE = '/api';

export async function sendMessage(sessionId, message) {
  const response = await fetch(`${API_BASE}/chat/message`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, message }),
  });
  if (!response.ok) throw new Error('Failed to send message');
  return response.json();
}

export async function getConversationHistory(sessionId) {
  const response = await fetch(`${API_BASE}/chat/history/${sessionId}`);
  if (!response.ok) throw new Error('Failed to get history');
  return response.json();
}

export async function createSession() {
  const response = await fetch(`${API_BASE}/chat/new-session`, { method: 'POST' });
  if (!response.ok) throw new Error('Failed to create session');
  return response.json();
}

export async function getCatalog() {
  const response = await fetch(`${API_BASE}/v1/catalog`);
  if (!response.ok) throw new Error('Failed to get catalog');
  return response.json();
}

export async function getDashboardStats(range = 'all') {
  const response = await fetch(`${API_BASE}/dashboard/stats?range=${range}`);
  if (!response.ok) throw new Error('Failed to get stats');
  return response.json();
}

export async function getAuditTrail(sessionId, range = 'all') {
  const params = new URLSearchParams();
  if (sessionId) params.append('sessionId', sessionId);
  if (range) params.append('range', range);
  
  const response = await fetch(`${API_BASE}/dashboard/audit-trail?${params.toString()}`);
  if (!response.ok) throw new Error('Failed to get audit trail');
  return response.json();
}

export async function getConversations() {
  const response = await fetch(`${API_BASE}/dashboard/conversations`);
  if (!response.ok) throw new Error('Failed to get conversations');
  return response.json();
}

export async function simulatePaymentSuccess(razorpayOrderId) {
  const response = await fetch(
    `${API_BASE}/webhooks/simulate/payment-success?razorpayOrderId=${razorpayOrderId}`,
    { method: 'POST' }
  );
  if (!response.ok) throw new Error('Failed to simulate payment');
  return response.json();
}

export async function simulatePaymentFailure(razorpayOrderId) {
  const response = await fetch(
    `${API_BASE}/webhooks/simulate/payment-failure?razorpayOrderId=${razorpayOrderId}`,
    { method: 'POST' }
  );
  if (!response.ok) throw new Error('Failed to simulate failure');
  return response.json();
}
