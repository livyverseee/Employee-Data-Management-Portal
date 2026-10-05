import React, { useEffect } from 'react';

/**
 * Lightweight floating Toast notification component.
 * Supports types: 'success', 'error', 'info'.
 * Auto-dismisses after duration (default 4000ms).
 */
export default function Toast({ message, type = 'success', onClose, duration = 4000 }) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => {
      onClose();
    }, duration);
    return () => clearTimeout(timer);
  }, [message, duration, onClose]);

  if (!message) return null;

  return (
    <div className={`toast toast-${type}`} role="alert">
      <div className="toast-icon">
        {type === 'success' && '✓'}
        {type === 'error' && '✕'}
        {type === 'info' && 'ℹ'}
      </div>
      <div className="toast-message">{message}</div>
      <button type="button" className="toast-close" onClick={onClose} aria-label="Close notification">
        &times;
      </button>
    </div>
  );
}
