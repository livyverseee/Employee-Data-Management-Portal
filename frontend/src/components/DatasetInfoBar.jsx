import React from 'react';

/**
 * DatasetInfoBar displaying metadata according to role specifications:
 * - DEAN: "<fileName> (<sourceFormat>) | <recordCount> records | uploaded by <uploadedBy> on <date>"
 *         with "Replace dataset" and "Download as XML" links
 * - EMPLOYEE: "Data provided by <uploadedBy> | <fileName> | uploaded <date>"
 */
export default function DatasetInfoBar({
  dataset,
  isDean = false,
  onReplaceClick,
  onDownloadXml,
  downloadingXml = false,
}) {
  if (!dataset || !dataset.exists) return null;

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    try {
      return new Date(isoString).toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="dataset-info-bar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 18px', backgroundColor: '#f9fafb', borderRadius: '10px', border: '1px solid #e5e7eb', marginBottom: '18px', flexWrap: 'wrap', gap: '10px' }}>
      <div className="dataset-info-left" style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.92rem', color: '#374151', flexWrap: 'wrap' }}>
        <span style={{ fontSize: '1.2rem' }}>📁</span>

        {isDean ? (
          <>
            <strong>{dataset.fileName}</strong>
            <span style={{ backgroundColor: '#e0e7ff', color: '#4338ca', padding: '2px 8px', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600 }}>
              {dataset.sourceFormat || 'STRUCTURED'}
            </span>
            <span style={{ color: '#9ca3af' }}>|</span>
            <span><strong>{dataset.recordCount?.toLocaleString() || 0}</strong> records</span>
            <span style={{ color: '#9ca3af' }}>|</span>
            <span>uploaded by <strong>{dataset.uploadedBy}</strong> on {formatDate(dataset.uploadedAt)}</span>
          </>
        ) : (
          <>
            <span>Data provided by <strong>{dataset.uploadedBy}</strong></span>
            <span style={{ color: '#9ca3af' }}>|</span>
            <strong>{dataset.fileName}</strong>
            <span style={{ color: '#9ca3af' }}>|</span>
            <span>uploaded {formatDate(dataset.uploadedAt)}</span>
          </>
        )}
      </div>

      {isDean && (
        <div className="dataset-info-actions" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            type="button"
            className="link-action-btn"
            onClick={onDownloadXml}
            disabled={downloadingXml}
            style={{ background: 'none', border: 'none', color: '#4f46e5', textDecoration: 'underline', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 500 }}
            title="Download current data formatted as internal XML (dataset.xml)"
          >
            {downloadingXml ? 'Downloading...' : '📥 Download as XML'}
          </button>

          <button
            type="button"
            className="link-action-btn"
            onClick={onReplaceClick}
            style={{ background: 'none', border: 'none', color: '#4f46e5', textDecoration: 'underline', cursor: 'pointer', fontSize: '0.85rem', fontWeight: 500 }}
            title="Upload a new dataset to replace the current one"
          >
            ↻ Replace dataset
          </button>
        </div>
      )}
    </div>
  );
}
