import { useState } from 'react';
import axios from 'axios';
import API_URL from '../config/api';

export function usePaymentSimulation() {
  const [paymentStatus, setPaymentStatus] = useState(null); // 'success', 'failed', null
  const [isSimulating, setIsSimulating] = useState(false);

  const simulatePayment = async (orderId, status) => {
    setIsSimulating(true);
    setPaymentStatus(null);
    try {
      const endpoint = status === 'success' 
        ? `${API_URL}/api/internal/orders/${orderId}/mark-paid`
        : `${API_URL}/api/internal/orders/${orderId}/mark-failed`;
      
      const payload = status === 'success' ? { paymentId: `pay_sim_${Date.now()}` } : {};
      
      await axios.post(endpoint, payload);
      setPaymentStatus(status);
    } catch (error) {
      console.error('Payment simulation failed:', error);
      alert('Failed to simulate payment. Check console.');
    } finally {
      setIsSimulating(false);
    }
  };

  return { simulatePayment, paymentStatus, isSimulating };
}
