import React, { useState, useEffect, useRef } from 'react';
import { replaceDatasetApi } from '../services/api';

/**
 * ReplaceDatasetModal:
 * Allows DEAN to upload a new structured file (.xlsx, .csv, .json, .xml) to replace the current active dataset.
 * Server validates, parses, and converts the file first before activating the new table and dropping the old table.
 */
export default function ReplaceDatasetModal({ onClose, onSuccess }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const isAcceptedFormat = (fileName) => {
    const lower = fileName.toLowerCase();
    return lower.endsWith('.xlsx') || lower.endsWith('.csv') || lower.endsWith('.json') || lower.endsWith('.xml');
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = e.target.files[0];
      if (!isAcceptedFormat(selected.name)) {
        setError('Accepted formats: .xlsx, .csv, .json, .xml');
        setFile(null);
        return;
      }
      setFile(selected);
      setError(null);
    }
  };

  const handleReplace = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a file to replace the dataset');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const response = await replaceDatasetApi(file);
      onSuccess(response.data);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to replace dataset';
      setError(msg);
    } finally {
      setUploading(false);
    }
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget && !uploading) {
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick} style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
      <div className="modal-content" role="dialog" aria-modal="true" style={{ backgroundColor: '#fff', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '0', overflow: 'hidden' }}>
        <div className="modal-header" style={{ padding: '16px 20px', borderBottom: '1px solid #e5e7eb', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#111827' }}>Replace Active Dataset</h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            disabled={uploading}
            style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: '#9ca3af' }}
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleReplace}>
          <div className="modal-body" style={{ padding: '20px' }}>
            <div
              className="alert alert-warning"
              style={{ backgroundColor: '#fffbeb', border: '1px solid #fef3c7', color: '#92400e', padding: '12px', borderRadius: '8px', fontSize: '0.85rem' }}
            >
              <strong>Caution:</strong> Replacing the dataset will validate and load the new file first, then atomically activate the new dataset and drop the previous table. If the file is invalid, existing records will remain completely untouched.
            </div>

            {error && <div className="alert alert-error" style={{ color: '#ef4444', marginTop: '12px', fontSize: '0.85rem' }}>{error}</div>}

            <div className="form-group" style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label htmlFor="replace-xml-file" style={{ fontSize: '0.875rem', fontWeight: 600, color: '#374151' }}>
                Select Replacement File (.xlsx, .csv, .json, .xml)
              </label>
              <input
                id="replace-xml-file"
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.csv,.json,.xml"
                onChange={handleFileChange}
                disabled={uploading}
                style={{ padding: '8px', border: '1px solid #d1d5db', borderRadius: '6px' }}
              />
              {file ? (
                <div style={{ marginTop: '6px', fontSize: '0.85rem', color: '#4f46e5' }}>
                  Selected: <strong>{file.name}</strong> ({(file.size / 1024).toFixed(1)} KB)
                </div>
              ) : (
                <span style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                  Max 10 MB, up to 100,000 rows, 60 columns.
                </span>
              )}
            </div>
          </div>

          <div className="modal-footer" style={{ padding: '14px 20px', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={uploading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={!file || uploading}
            >
              {uploading ? 'Validating & Ingesting...' : 'Confirm & Replace Dataset'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
