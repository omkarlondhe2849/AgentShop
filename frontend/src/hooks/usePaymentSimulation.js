import { useState } from 'react';
import axios from 'axios';

export function usePaymentSimulation() {
  const [paymentStatus, setPaymentStatus] = useState(null); // 'success', 'failed', null
  const [isSimulating, setIsSimulating] = useState(false);

  const simulatePayment = async (orderId, status) => {
    setIsSimulating(true);
    setPaymentStatus(null);
    try {
      const endpoint = status === 'success' 
        ? `http://localhost:8080/api/internal/orders/${orderId}/mark-paid`
        : `http://localhost:8080/api/internal/orders/${orderId}/mark-failed`;
      
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
