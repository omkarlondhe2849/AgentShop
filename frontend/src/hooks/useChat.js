import { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';

export function useChat(sessionId) {
  const [messages, setMessages] = useState([]);
  const [cart, setCart] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [addressFormOpen, setAddressFormOpen] = useState(false);
  const messagesEndRef = useRef(null);

  // Auto-scroll
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };
  useEffect(() => { scrollToBottom(); }, [messages, isLoading]);

  const fetchCart = useCallback(async () => {
    if (!sessionId) return;
    try {
      const { data } = await axios.get(`http://localhost:8080/api/v1/cart/${sessionId}`);
      setCart({ items: data.items || [], totalAmount: data.totalPrice || 0 });
    } catch (error) {
      console.error('Failed to fetch cart:', error);
    }
  }, [sessionId]);

  const sendMessage = async (text) => {
    if (!text.trim()) return;

    const userMsg = { id: Date.now(), text, sender: 'user' };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    try {
      const response = await axios.post('http://localhost:8080/api/chat', {
        sessionId,
        message: text,
      });

      let botText = response.data;
      let addressRequested = false;

      // Handle the hidden Action token
      if (botText.includes('__ACTION_REQUEST_ADDRESS__')) {
        addressRequested = true;
        setAddressFormOpen(true);
        botText = botText.replace('__ACTION_REQUEST_ADDRESS__', '').trim();
      }

      if (botText) {
          const botMsg = { id: Date.now() + 1, text: botText, sender: 'bot' };
          setMessages((prev) => [...prev, botMsg]);
      }

      if (addressRequested) {
        // Do nothing specific, form is open
      } else {
        // Only refresh cart if an action might have changed it
        if (text.toLowerCase().includes('add') || text.toLowerCase().includes('remove') || text.toLowerCase().includes('cart')) {
          fetchCart();
        }
      }
    } catch (error) {
      console.error('Chat API Error:', error);
      setMessages((prev) => [
        ...prev,
        { id: Date.now() + 1, text: 'Oops, I encountered a temporary glitch. Could you repeat that?', sender: 'bot' },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCheckoutSubmit = async (addressData) => {
    setAddressFormOpen(false);
    setIsLoading(true);
    setMessages((prev) => [...prev, { id: Date.now(), text: 'Processing your order...', sender: 'bot', system: true }]);

    try {
        const payload = {
            sessionId,
            customerName: addressData.name,
            customerEmail: addressData.email,
            deliveryAddress: addressData.address
        };
        const response = await axios.post('http://localhost:8080/api/v1/checkout', payload);
        
        if (response.data && response.data.message) {
            setMessages((prev) => [...prev, { id: Date.now() + 1, text: response.data.message, sender: 'bot' }]);
        }
        fetchCart();
    } catch (error) {
        setMessages((prev) => [...prev, { id: Date.now() + 1, text: 'Failed to process checkout. Please try again.', sender: 'bot' }]);
    } finally {
        setIsLoading(false);
    }
  };

  const cancelCheckout = () => {
      setAddressFormOpen(false);
      setMessages((prev) => [...prev, { id: Date.now(), text: 'Checkout cancelled.', sender: 'bot', system: true }]);
  };


  useEffect(() => {
    if (sessionId && messages.length === 0) {
      setMessages([
        {
          id: 1,
          text: `Hi there! 👋 I'm **ShopBot**, your AI shopping assistant. \n\nI can help you:\n• Search for products (e.g., _"show me wireless headphones under ₹3000"_)\n• Check product details\n• Manage your cart\n• Complete your purchase\n\nWhat are you looking for today?`,
          sender: 'bot'
        }
      ]);
      fetchCart();
    }
  }, [sessionId, messages.length, fetchCart]);

  return {
    messages,
    sendMessage,
    isLoading,
    cart,
    addressFormOpen,
    handleCheckoutSubmit,
    cancelCheckout,
    messagesEndRef
  };
}
