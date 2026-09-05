import { useState, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';

export function useSession() {
  const [sessionId, setSessionId] = useState('');

  useEffect(() => {
    let sid = localStorage.getItem('agentshop_session_id');
    if (!sid) {
      sid = `sess_${uuidv4().split('-')[0]}`;
      localStorage.setItem('agentshop_session_id', sid);
    }
    setSessionId(sid);
  }, []);

  const resetSession = () => {
    const newSid = `sess_${uuidv4().split('-')[0]}`;
    localStorage.setItem('agentshop_session_id', newSid);
    setSessionId(newSid);
  };

  return { sessionId, resetSession };
}
