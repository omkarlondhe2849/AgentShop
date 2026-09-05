import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export default function PaymentStatusPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('processing');
  
  const paymentId = searchParams.get('razorpay_payment_id');
  const paymentStatus = searchParams.get('razorpay_payment_link_status');
  const referenceId = searchParams.get('razorpay_payment_link_reference_id');

  useEffect(() => {
    if (paymentStatus === 'paid') {
      setStatus('success');
      toast.success(`Payment successful! ID: ${paymentId}`);
    } else if (paymentStatus === 'failed' || paymentStatus === 'cancelled') {
      setStatus('failed');
      toast.error('Payment failed or cancelled.');
    } else if (paymentStatus) {
      setStatus('failed');
    } else {
      // If there are no params, just default to error or wait
      setStatus('failed');
    }
  }, [paymentStatus, paymentId]);

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
      minHeight: '80vh', padding: '20px', textAlign: 'center'
    }}>
      {status === 'processing' && (
        <>
          <Loader2 size={64} color="var(--rzp-blue)" className="spinner" style={{ animation: 'spin 1s linear infinite', marginBottom: '20px' }} />
          <h2>Verifying Payment...</h2>
          <p style={{ color: 'var(--text-secondary)' }}>Please wait while we confirm your transaction.</p>
        </>
      )}

      {status === 'success' && (
        <div style={{ background: 'var(--bg-secondary)', padding: '40px', borderRadius: '16px', border: '1px solid var(--border-color)', maxWidth: '400px' }}>
          <CheckCircle2 size={64} color="var(--success-color)" style={{ marginBottom: '20px' }} />
          <h2 style={{ marginBottom: '10px' }}>Payment Successful!</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>
            Thank you for your purchase. Your order #{referenceId?.replace('order_', '') || ''} is confirmed.
          </p>
          <div style={{ background: 'var(--bg-primary)', padding: '12px', borderRadius: '8px', marginBottom: '20px', fontSize: '0.9rem', wordBreak: 'break-all' }}>
            <strong>Transaction ID:</strong> <br/> {paymentId}
          </div>
          <button className="btn btn-primary" onClick={() => navigate('/chat')} style={{ width: '100%' }}>
            Return to Chat
          </button>
        </div>
      )}

      {status === 'failed' && (
        <div style={{ background: 'var(--bg-secondary)', padding: '40px', borderRadius: '16px', border: '1px solid var(--border-color)', maxWidth: '400px' }}>
          <XCircle size={64} color="var(--danger-color)" style={{ marginBottom: '20px' }} />
          <h2 style={{ marginBottom: '10px' }}>Payment Failed</h2>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '20px' }}>
            We couldn't process your payment. You can try again from the chat.
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/chat')} style={{ width: '100%' }}>
            Return to Chat
          </button>
        </div>
      )}
      
      <style>{`
        @keyframes spin { 100% { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
