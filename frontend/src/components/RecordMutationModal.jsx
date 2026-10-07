import React, { useState, useEffect } from 'react';
import RecordForm from './RecordForm';
import { addRecordApi, updateRecordApi } from '../services/api';

/**
 * Modal dialog for Adding or Editing a record.
 * Wraps RecordForm and communicates with the backend.
 */
export default function RecordMutationModal({
  isOpen = false,
  isEdit = false,
  record = null,
  columns = [],
  filterOptions = {},
  onClose,
  onSuccess,
}) {
  const [serverErrors, setServerErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [generalError, setGeneralError] = useState(null);

  useEffect(() => {
    setServerErrors({});
    setGeneralError(null);
  }, [isOpen, record]);

  if (!isOpen) return null;

  const handleSubmit = async (formData) => {
    setSubmitting(true);
    setServerErrors({});
    setGeneralError(null);

    try {
      if (isEdit && record) {
        const res = await updateRecordApi(record.id, formData);
        onSuccess(res.data, true);
      } else {
        const res = await addRecordApi(formData);
        onSuccess(res.data, false);
      }
    } catch (err) {
      if (err.response?.status === 400 && err.response?.data?.errors) {
        setServerErrors(err.response.data.errors);
      } else {
        setGeneralError(err.response?.data?.message || err.message || 'Failed to save record');
      }
    } finally {
      setSubmitting(false);
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
          maxWidth: '720px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
        }}
      >
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
          <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#111827' }}>
            {isEdit ? `Edit Record #${record?.id}` : 'Add New Record'}
          </h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            disabled={submitting}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5rem',
              color: '#9ca3af',
              cursor: 'pointer',
            }}
          >
            &times;
          </button>
        </div>

        <div className="modal-body" style={{ padding: '20px', overflowY: 'auto' }}>
          {generalError && (
            <div className="alert alert-error" style={{ marginBottom: '14px', color: '#ef4444', backgroundColor: '#fef2f2', padding: '10px 14px', borderRadius: '6px' }}>
              {generalError}
            </div>
          )}

          <RecordForm
            columns={columns}
            initialData={isEdit && record ? record.data : {}}
            isEdit={isEdit}
            filterOptions={filterOptions}
            onSubmit={handleSubmit}
            onCancel={onClose}
            serverErrors={serverErrors}
            submitting={submitting}
          />
        </div>
      </div>
    </div>
  );
}
