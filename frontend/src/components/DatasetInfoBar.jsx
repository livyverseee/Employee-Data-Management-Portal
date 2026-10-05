import React from 'react';

/**
 * DatasetInfoBar displays metadata for the currently active dataset.
 * - Shows: File name, Uploaded by, Upload timestamp, Total records.
 * - If canReplace (DEAN), provides a "Replace Dataset" action button.
 */
export default function DatasetInfoBar({ dataset, canReplace = false, onReplaceClick }) {
  if (!dataset || !dataset.exists) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    try {
      return new Date(isoString).toLocaleString('en-US', {
        dateStyle: 'medium',
        timeStyle: 'short',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="dataset-info-bar">
      <div className="dataset-info-left">
        <div className="dataset-tag">
          <span className="dataset-dot"></span>
          ACTIVE DATASET
        </div>
        <div className="dataset-details">
          <span className="dataset-filename" title={dataset.fileName}>
            📄 {dataset.fileName}
          </span>
          <span className="dataset-meta-item">
            Uploaded by <strong>{dataset.uploadedBy}</strong>
          </span>
          <span className="dataset-meta-sep">&bull;</span>
          <span className="dataset-meta-item">{formatDate(dataset.uploadedAt)}</span>
          <span className="dataset-meta-sep">&bull;</span>
          <span className="dataset-record-count">
            <strong>{dataset.recordCount?.toLocaleString() || 0}</strong> records
          </span>
        </div>
      </div>

      {canReplace && (
        <div className="dataset-info-actions">
          <button
            type="button"
            className="btn btn-outline btn-sm replace-btn"
            onClick={onReplaceClick}
          >
            ↻ Replace Dataset
          </button>
        </div>
      )}
    </div>
  );
}
