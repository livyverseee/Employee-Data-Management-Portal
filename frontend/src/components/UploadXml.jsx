import React, { useState, useRef } from 'react';
import { uploadXmlApi } from '../services/api';

/**
 * Upload XML component for DEAN role.
 * Allows uploading .xml files, displays upload progress / results, and refreshes the employee table.
 */
export default function UploadXml({ onUploadSuccess }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setMessage(null);
      setError(null);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select an XML file to upload');
      return;
    }

    if (!file.name.toLowerCase().endsWith('.xml')) {
      setError('Only .xml files are supported');
      return;
    }

    setUploading(true);
    setMessage(null);
    setError(null);

    try {
      const res = await uploadXmlApi(file);
      const { recordsSaved } = res.data;
      setMessage(`Upload successful! ${recordsSaved} employee record(s) processed and stored in database.`);
      setFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      // Trigger parent table refresh
      if (onUploadSuccess) {
        onUploadSuccess();
      }
    } catch (err) {
      const serverMessage = err.response?.data?.message || err.message || 'Failed to upload XML file';
      setError(serverMessage);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="upload-card">
      <h3>Import Employee XML Dataset</h3>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <form onSubmit={handleUpload} className="upload-form">
        <input
          ref={fileInputRef}
          type="file"
          accept=".xml"
          onChange={handleFileChange}
          disabled={uploading}
        />
        <button
          type="submit"
          className="btn btn-primary"
          disabled={!file || uploading}
        >
          {uploading ? 'Processing XML...' : 'Upload XML'}
        </button>
      </form>
    </div>
  );
}
