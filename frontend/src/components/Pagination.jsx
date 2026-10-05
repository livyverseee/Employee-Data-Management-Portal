import React from 'react';

/**
 * Pagination control component.
 * Displays Prev / Next buttons, "Page X of Y", and total record count.
 * Disables buttons at boundaries (page 0 or last page).
 */
export default function Pagination({
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
}) {
  const displayPage = totalPages === 0 ? 0 : currentPage + 1;
  const isFirstPage = currentPage <= 0;
  const isLastPage = currentPage >= totalPages - 1 || totalPages === 0;

  return (
    <div className="pagination-container">
      <div>
        Total Records: <strong>{totalElements.toLocaleString()}</strong>
      </div>
      <div className="pagination-controls">
        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirstPage}
        >
          &larr; Prev
        </button>

        <span>
          Page <strong>{displayPage}</strong> of <strong>{totalPages || 1}</strong>
        </span>

        <button
          type="button"
          className="btn btn-secondary"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLastPage}
        >
          Next &rarr;
        </button>
      </div>
    </div>
  );
}
