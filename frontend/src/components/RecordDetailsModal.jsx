import React, { useEffect } from 'react';

/**
 * Read-only modal displaying all schema fields for a selected record.
 * Closes via X button, Close button, backdrop click, or Esc key.
 */
export default function RecordDetailsModal({
  record = null,
  columns = [],
  onClose,
}) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!record) return null;

  const data = record.data || {};

  const formatDateDisplay = (val) => {
    if (!val || typeof val !== 'string' || !val.match(/^\d{4}-\d{2}-\d{2}$/)) {
      return val != null ? String(val) : '—';
    }
    try {
      const [year, month, day] = val.split('-');
      const d = new Date(year, parseInt(month, 10) - 1, day);
      return d.toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return val;
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '20px',
      }}
    >
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{
          backgroundColor: '#fff',
          borderRadius: '12px',
          width: '100%',
          maxWidth: '680px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
        }}
      >
        {/* Header */}
        <div
          className="modal-header"
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 20px',
            borderBottom: '1px solid #e5e7eb',
          }}
        >
          <div>
            <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#111827' }}>
              Record Details #{record.id}
            </h3>
            <span style={{ fontSize: '0.8rem', color: '#6b7280' }}>
              Full view of record attributes
            </span>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5rem',
              lineHeight: 1,
              color: '#9ca3af',
              cursor: 'pointer',
            }}
          >
            &times;
          </button>
        </div>

        {/* Body */}
        <div
          className="modal-body"
          style={{
            padding: '20px',
            overflowY: 'auto',
          }}
        >
          <div
            className="details-grid"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
              gap: '16px',
            }}
          >
            {columns.map((col) => {
              const val = data[col.key];
              const displayVal =
                val === null || val === undefined || val === ''
                  ? '—'
                  : col.type === 'DATE'
                  ? formatDateDisplay(val)
                  : typeof val === 'number'
                  ? val.toLocaleString()
                  : typeof val === 'boolean'
                  ? val ? 'Yes' : 'No'
                  : String(val);

              return (
                <div key={col.key} className="detail-item" style={{ background: '#f9fafb', padding: '10px 12px', borderRadius: '8px' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', marginBottom: '4px' }}>
                    {col.label}
                  </div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 500, color: '#1f2937', wordBreak: 'break-word' }}>
                    {displayVal}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Footer */}
        <div
          className="modal-footer"
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            padding: '14px 20px',
            borderTop: '1px solid #e5e7eb',
          }}
        >
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
