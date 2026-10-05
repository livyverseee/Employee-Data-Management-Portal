import React, { useState, useEffect, useRef } from 'react';
import { replaceDatasetApi } from '../services/api';

/**
 * ReplaceDatasetModal:
 * Allows DEAN to upload a new XML dataset to replace the current active dataset.
 * Server parses and validates FIRST before removing existing records.
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

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = e.target.files[0];
      if (!selected.name.toLowerCase().endsWith('.xml')) {
        setError('Only .xml files are supported');
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
      setError('Please select an XML file to replace the dataset');
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
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div className="modal-content" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h3>Replace Active Dataset</h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            disabled={uploading}
            aria-label="Close dialog"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleReplace}>
          <div className="modal-body">
            <div className="alert alert-warning" style={{ backgroundColor: '#fffbeb', borderColor: '#fef3c7', color: '#92400e' }}>
              <strong>Caution:</strong> Replacing the dataset will validate the new XML first, delete all current employee records, and load the new dataset. If the file is invalid, existing records will remain untouched.
            </div>

            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group" style={{ marginTop: '16px' }}>
              <label htmlFor="replace-xml-file">Select New XML File</label>
              <input
                id="replace-xml-file"
                ref={fileInputRef}
                type="file"
                accept=".xml"
                onChange={handleFileChange}
                disabled={uploading}
              />
              <span className="field-hint">Accepts well-formed Employee XML files (.xml)</span>
            </div>
          </div>

          <div className="modal-footer">
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
              {uploading ? 'Processing & Replacing...' : 'Confirm & Replace Dataset'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
