import React from 'react';
import DynamicFilterRow from './DynamicFilterRow';

/**
 * Shared DynamicTable component:
 * - Columns generated dynamically from active dataset schema (in position order)
 * - Horizontal scrolling wrapper for wide tables
 * - Dates formatted like "04 Apr 2018" for display
 * - DynamicFilterRow embedded directly below headers
 * - Row actions: Edit & Delete for DEAN (with stopPropagation and confirm dialog), View for EMPLOYEE
 * - Clicking row opens details modal
 * - Supports temporary row highlighting for newly added records
 */
export default function DynamicTable({
  records = [],
  columns = [],
  loading = false,
  canEdit = false,
  onRowClick,
  onEdit,
  onDelete,
  filters = {},
  filterOptions = {},
  filterOptionsError = null,
  onFilterChange,
  onClearFilters,
  highlightedId = null,
}) {
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

  const renderCellValue = (recordData, col) => {
    const val = recordData[col.key];
    if (val === null || val === undefined || val === '') {
      return <span style={{ color: '#9ca3af' }}>—</span>;
    }

    if (col.type === 'DATE') {
      return formatDateDisplay(val);
    }

    if (typeof val === 'number') {
      return val.toLocaleString();
    }

    if (typeof val === 'boolean') {
      return val ? 'Yes' : 'No';
    }

    return String(val);
  };

  return (
    <div className="table-responsive-wrapper" style={{ overflowX: 'auto', width: '100%' }}>
      <table className="dynamic-data-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr className="table-header-row">
            {columns.map((col) => (
              <th
                key={col.key}
                className="table-header-cell"
                style={{
                  padding: '12px 10px',
                  textAlign: col.type === 'NUMBER' ? 'right' : 'left',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  whiteSpace: 'nowrap',
                }}
              >
                {col.label}
              </th>
            ))}
            <th
              className="table-header-cell actions-header"
              style={{
                padding: '12px 10px',
                textAlign: 'center',
                fontWeight: 600,
                fontSize: '0.85rem',
                minWidth: canEdit ? '120px' : '80px',
              }}
            >
              Actions
            </th>
          </tr>

          {/* Dynamic Filter Row under the headers */}
          <DynamicFilterRow
            columns={columns}
            filters={filters}
            filterOptions={filterOptions}
            filterOptionsError={filterOptionsError}
            onFilterChange={onFilterChange}
            onClearFilters={onClearFilters}
          />
        </thead>

        <tbody style={{ opacity: loading ? 0.65 : 1, transition: 'opacity 0.2s ease' }}>
          {records.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + 1}
                className="empty-table-cell"
                style={{ textAlign: 'center', padding: '36px 16px', color: '#6b7280' }}
              >
                {loading ? (
                  <>
                    <div style={{ fontSize: '1.4rem', marginBottom: '8px' }}>⏳</div>
                    <strong>Loading records...</strong>
                  </>
                ) : (
                  <>
                    <div style={{ fontSize: '1.4rem', marginBottom: '8px' }}>🔍</div>
                    <strong>No matching records found</strong>
                    <p style={{ margin: '4px 0 0 0', fontSize: '0.85rem' }}>
                      Try adjusting or clearing your search filters.
                    </p>
                  </>
                )}
              </td>
            </tr>
          ) : (
            records.map((rec) => {
              const data = rec.data || {};
              const isHighlighted = highlightedId === rec.id;

              return (
                <tr
                  key={rec.id}
                  className={`table-body-row ${isHighlighted ? 'row-highlighted' : ''}`}
                  onClick={() => onRowClick && onRowClick(rec)}
                  style={{
                    cursor: 'pointer',
                    transition: 'background-color 0.2s',
                    backgroundColor: isHighlighted ? '#fef3c7' : undefined,
                  }}
                  title="Click to view full record details"
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className="table-data-cell"
                      style={{
                        padding: '10px',
                        textAlign: col.type === 'NUMBER' ? 'right' : 'left',
                        fontSize: '0.875rem',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {renderCellValue(data, col)}
                    </td>
                  ))}

                  {/* Actions Cell */}
                  <td
                    className="table-data-cell actions-cell"
                    style={{ textAlign: 'center', padding: '8px' }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div style={{ display: 'flex', gap: '6px', justifyContent: 'center' }}>
                      {canEdit ? (
                        <>
                          <button
                            type="button"
                            className="btn btn-sm btn-outline"
                            onClick={(e) => {
                              e.stopPropagation();
                              onEdit && onEdit(rec);
                            }}
                            title="Edit this record"
                            style={{ padding: '3px 8px', fontSize: '0.78rem' }}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-danger-outline"
                            onClick={(e) => {
                              e.stopPropagation();
                              if (window.confirm(`Are you sure you want to delete this record (ID ${rec.id})?`)) {
                                onDelete && onDelete(rec.id);
                              }
                            }}
                            title="Delete this record"
                            style={{ padding: '3px 8px', fontSize: '0.78rem', color: '#dc2626', borderColor: '#fca5a5' }}
                          >
                            Delete
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-sm btn-outline"
                          onClick={(e) => {
                            e.stopPropagation();
                            onRowClick && onRowClick(rec);
                          }}
                          style={{ padding: '3px 8px', fontSize: '0.78rem' }}
                        >
                          View
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
