import React, { useState, useRef } from 'react';
import { uploadDatasetApi } from '../services/api';

/**
 * Centered "Upload Employee Data" card for DEAN role when no dataset exists.
 * Accepts: .xlsx, .csv, .json, .xml
 * Explains that the file is converted to XML and stored.
 */
export default function UploadXml({ onUploadSuccess, isOnboarding = true }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);

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

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const selected = e.dataTransfer.files[0];
      if (!isAcceptedFormat(selected.name)) {
        setError('Accepted formats: .xlsx, .csv, .json, .xml');
        setFile(null);
        return;
      }
      setFile(selected);
      setError(null);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a file to upload (.xlsx, .csv, .json, .xml)');
      return;
    }

    setUploading(true);
    setError(null);
    setSuccess(null);

    try {
      const res = await uploadDatasetApi(file);
      setSuccess(`Dataset "${file.name}" uploaded, converted to XML, and indexed successfully!`);
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      if (onUploadSuccess) {
        onUploadSuccess(res.data);
      }
    } catch (err) {
      const serverMessage = err.response?.data?.message || err.message || 'Failed to upload file';
      setError(serverMessage);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className={`upload-card ${isOnboarding ? 'onboarding-upload-card' : ''}`}>
      <div className="upload-header">
        <div className="upload-icon-circle">📂</div>
        <h3>Upload Employee Data</h3>
        <p className="upload-subtitle">
          Upload any flat structured table (.xlsx, .csv, .json, .xml).
          The file is automatically converted to internal XML, schema types are inferred, and records are stored in a dedicated table.
        </p>
        <p style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: '4px' }}>
          Limits: 10 MB maximum, up to 100,000 rows, 60 columns.
        </p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form onSubmit={handleUpload} className="upload-form-box">
        <div
          className={`dropzone ${dragActive ? 'dropzone-active' : ''} ${file ? 'has-file' : ''}`}
          onDragEnter={handleDrag}
          onDragOver={handleDrag}
          onDragLeave={handleDrag}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current && fileInputRef.current.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,.csv,.json,.xml"
            onChange={handleFileChange}
            disabled={uploading}
            style={{ display: 'none' }}
          />

          <div className="dropzone-content">
            {file ? (
              <div className="dropzone-file-selected">
                <span className="selected-filename">📄 {file.name}</span>
                <span className="selected-filesize">
                  ({(file.size / 1024).toFixed(1)} KB)
                </span>
                <button
                  type="button"
                  className="btn-change-file"
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    if (fileInputRef.current) fileInputRef.current.value = '';
                  }}
                >
                  Change file
                </button>
              </div>
            ) : (
              <div>
                <p className="dropzone-text">
                  <strong>Click to browse</strong> or drag & drop your data file here
                </p>
                <p className="dropzone-hint">Accepted formats: .xlsx, .csv, .json, .xml</p>
              </div>
            )}
          </div>
        </div>

        <div className="upload-actions">
          <button
            type="submit"
            className="btn btn-primary btn-lg"
            disabled={!file || uploading}
          >
            {uploading ? 'Converting to XML & Ingesting...' : 'Upload & Process Dataset'}
          </button>
        </div>
      </form>
    </div>
  );
}
