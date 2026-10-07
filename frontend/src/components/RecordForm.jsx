import React, { useState, useEffect } from 'react';

/**
 * Reusable dynamic record form generated from dataset schema columns.
 * Used for both Add and Edit modals.
 * - TEXT: text input (maxlength 500)
 * - NUMBER: number input (step 1 for INTEGER, "any" for DECIMAL)
 * - DATE: date picker (type="date")
 * - Required indicator for non-nullable columns
 * - Prefills max + 1 for sequential columns when adding
 * - Renders per-field backend error messages
 */
export default function RecordForm({
  columns = [],
  initialData = {},
  isEdit = false,
  filterOptions = {},
  onSubmit,
  onCancel,
  serverErrors = {},
  submitting = false,
}) {
  const [formData, setFormData] = useState({});

  useEffect(() => {
    const data = { ...initialData };

    // For new records, prefill sequential columns with max + 1
    if (!isEdit) {
      columns.forEach((col) => {
        if (col.sequential && (data[col.key] === undefined || data[col.key] === '')) {
          const range = filterOptions[col.key];
          if (range && range.max !== undefined && range.max !== null) {
            data[col.key] = Number(range.max) + 1;
          } else {
            data[col.key] = 1;
          }
        }
      });
    }

    setFormData(data);
  }, [initialData?.id, isEdit]);

  const handleChange = (key, value) => {
    setFormData((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="record-form">
      <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '14px', maxHeight: '60vh', overflowY: 'auto', padding: '4px' }}>
        {columns.map((col) => {
          const key = col.key;
          const isRequired = !col.nullable;
          const fieldError = serverErrors && serverErrors[key];
          const val = formData[key] !== undefined && formData[key] !== null ? formData[key] : '';

          return (
            <div key={key} className="form-group" style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <label htmlFor={`field_${key}`} style={{ fontSize: '0.82rem', fontWeight: 600, color: '#374151' }}>
                {col.label}
                {isRequired && <span style={{ color: '#ef4444', marginLeft: '3px' }}>*</span>}
                {col.sequential && <span style={{ fontSize: '0.72rem', color: '#6b7280', marginLeft: '6px' }}>(sequential)</span>}
              </label>

              {col.type === 'NUMBER' ? (
                <input
                  id={`field_${key}`}
                  type="number"
                  step={col.numberKind === 'DECIMAL' ? 'any' : '1'}
                  value={val}
                  onChange={(e) => handleChange(key, e.target.value)}
                  placeholder={`Enter ${col.label.toLowerCase()}`}
                  style={{
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: fieldError ? '1px solid #ef4444' : '1px solid #d1d5db',
                    fontSize: '0.875rem',
                  }}
                />
              ) : col.type === 'DATE' ? (
                <input
                  id={`field_${key}`}
                  type="date"
                  value={val}
                  onChange={(e) => handleChange(key, e.target.value)}
                  style={{
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: fieldError ? '1px solid #ef4444' : '1px solid #d1d5db',
                    fontSize: '0.875rem',
                  }}
                />
              ) : (
                <input
                  id={`field_${key}`}
                  type="text"
                  maxLength={500}
                  value={val}
                  onChange={(e) => handleChange(key, e.target.value)}
                  placeholder={`Enter ${col.label.toLowerCase()}`}
                  style={{
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: fieldError ? '1px solid #ef4444' : '1px solid #d1d5db',
                    fontSize: '0.875rem',
                  }}
                />
              )}

              {/* Per-field error message */}
              {fieldError && (
                <span className="field-error-text" style={{ color: '#ef4444', fontSize: '0.75rem', marginTop: '2px' }}>
                  {fieldError}
                </span>
              )}
            </div>
          );
        })}
      </div>

      <div className="form-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px', paddingTop: '16px', borderTop: '1px solid #e5e7eb' }}>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={submitting}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={submitting}
        >
          {submitting ? 'Saving...' : isEdit ? 'Save Changes' : 'Add Record'}
        </button>
      </div>
    </form>
  );
}
