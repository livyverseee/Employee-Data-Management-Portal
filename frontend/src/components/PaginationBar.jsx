import React from 'react';

/**
 * Shared PaginationBar component complying with Section 6 specifications:
 * - "Rows per page" dropdown (10, 25, 50, 100)
 * - First (<<), Prev (<), page numbers (current page +- 2 with ellipsis), Next (>), Last (>>)
 * - Text: "Showing 1-10 of 689 records"
 * - Buttons disable at boundaries
 */
export default function PaginationBar({
  currentPage = 0,
  pageSize = 10,
  totalPages = 0,
  totalElements = 0,
  onPageChange,
  onPageSizeChange,
}) {
  const isFirst = currentPage <= 0;
  const isLast = currentPage >= totalPages - 1 || totalPages === 0;

  const startRecord = totalElements === 0 ? 0 : currentPage * pageSize + 1;
  const endRecord = Math.min((currentPage + 1) * pageSize, totalElements);

  // Generate page numbers to show (current page +/- 2)
  const getPageNumbers = () => {
    if (totalPages <= 1) return [0];

    const pages = [];
    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages - 1, currentPage + 2);

    for (let p = start; p <= end; p++) {
      pages.push(p);
    }
    return pages;
  };

  const pages = getPageNumbers();
  const showFirstEllipsis = pages.length > 0 && pages[0] > 0;
  const showLastEllipsis = pages.length > 0 && pages[pages.length - 1] < totalPages - 1;

  return (
    <div className="pagination-bar" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderTop: '1px solid #e5e7eb', flexWrap: 'wrap', gap: '12px' }}>
      {/* Left: Showing range text */}
      <div className="pagination-info" style={{ fontSize: '0.9rem', color: '#4b5563' }}>
        Showing <strong>{startRecord}-{endRecord}</strong> of <strong>{totalElements.toLocaleString()}</strong> records
      </div>

      {/* Middle: Page Controls */}
      <div className="pagination-controls" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
        {/* First */}
        <button
          type="button"
          className="btn-page-nav"
          onClick={() => onPageChange(0)}
          disabled={isFirst}
          title="First Page"
        >
          &laquo;
        </button>

        {/* Prev */}
        <button
          type="button"
          className="btn-page-nav"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirst}
          title="Previous Page"
        >
          &lsaquo;
        </button>

        {/* Ellipsis from 0 */}
        {showFirstEllipsis && (
          <>
            <button
              type="button"
              className="btn-page-number"
              onClick={() => onPageChange(0)}
            >
              1
            </button>
            {pages[0] > 1 && <span className="pagination-ellipsis">&hellip;</span>}
          </>
        )}

        {/* Number buttons */}
        {totalPages > 0 && pages.map((p) => (
          <button
            key={p}
            type="button"
            className={`btn-page-number ${p === currentPage ? 'active' : ''}`}
            onClick={() => onPageChange(p)}
          >
            {p + 1}
          </button>
        ))}

        {/* Ellipsis to last */}
        {showLastEllipsis && (
          <>
            {pages[pages.length - 1] < totalPages - 2 && <span className="pagination-ellipsis">&hellip;</span>}
            <button
              type="button"
              className="btn-page-number"
              onClick={() => onPageChange(totalPages - 1)}
            >
              {totalPages}
            </button>
          </>
        )}

        {/* Next */}
        <button
          type="button"
          className="btn-page-nav"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLast}
          title="Next Page"
        >
          &rsaquo;
        </button>

        {/* Last */}
        <button
          type="button"
          className="btn-page-nav"
          onClick={() => onPageChange(totalPages - 1)}
          disabled={isLast}
          title="Last Page"
        >
          &raquo;
        </button>
      </div>

      {/* Right: Rows per page selector */}
      <div className="pagination-size" style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem', color: '#4b5563' }}>
        <span>Rows per page:</span>
        <select
          value={pageSize}
          onChange={(e) => onPageSizeChange && onPageSizeChange(Number(e.target.value))}
          style={{ padding: '4px 8px', borderRadius: '6px', border: '1px solid #d1d5db', backgroundColor: '#fff', fontSize: '0.9rem' }}
        >
          <option value={10}>10</option>
          <option value={25}>25</option>
          <option value={50}>50</option>
          <option value={100}>100</option>
        </select>
      </div>
    </div>
  );
}
