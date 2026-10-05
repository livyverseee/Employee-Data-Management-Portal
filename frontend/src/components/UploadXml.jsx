import React, { useState, useRef } from 'react';
import { uploadDatasetApi } from '../services/api';

/**
 * UploadXml / DatasetUploadCard component for DEAN role.
 * Used for initial dataset onboarding when no dataset is active.
 */
export default function UploadXml({ onUploadSuccess, isOnboarding = false }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);

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
      if (!selected.name.toLowerCase().endsWith('.xml')) {
        setError('Only .xml files are supported');
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
      setError('Please select an XML file to upload');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const res = await uploadDatasetApi(file);
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      if (onUploadSuccess) {
        onUploadSuccess(res.data);
      }
    } catch (err) {
      const serverMessage = err.response?.data?.message || err.message || 'Failed to upload XML file';
      setError(serverMessage);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className={`upload-card ${isOnboarding ? 'onboarding-upload-card' : ''}`}>
      <div className="upload-header">
        <div className="upload-icon-circle">📁</div>
        <h3>{isOnboarding ? 'No Dataset Uploaded Yet' : 'Upload Employee Dataset'}</h3>
        <p className="upload-subtitle">
          {isOnboarding
            ? 'Get started by uploading an employee XML dataset. Once uploaded, all employee records will be parsed, validated, and indexed.'
            : 'Select an employee XML file to ingest into the portal.'}
        </p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

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
            accept=".xml"
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
                  <strong>Click to browse</strong> or drag & drop an XML file here
                </p>
                <p className="dropzone-hint">Supports sample-employees.xml or full dataset (.xml)</p>
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
            {uploading ? 'Processing XML & Ingesting...' : 'Upload & Initialize Dataset'}
          </button>
        </div>
      </form>
    </div>
  );
}
