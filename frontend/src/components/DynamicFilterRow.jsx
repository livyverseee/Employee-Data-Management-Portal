import React, { useState, useEffect, useRef, useCallback } from 'react';

/**
 * Dynamic Filter Row rendered directly under the table header.
 * Generates appropriate controls based on column filterType:
 * - CATEGORY: dropdown populated from filter-options (applies immediately)
 * - TEXT_SEARCH: debounced text input (local state preserves continuous typing)
 * - RANGE: Min / Max number inputs with placeholders (local state preserves continuous typing)
 * - DATE_RANGE: From / To date pickers
 * - Actions column: "Reset" button
 *
 * All typed inputs use stable, controlled local state with 350ms debouncing, Enter flush,
 * and onBlur flush. This completely prevents focus loss and cursor jumping during continuous typing.
 */
export default function DynamicFilterRow({
  columns = [],
  filters = {},
  filterOptions = {},
  filterOptionsError = null,
  onFilterChange,
  onClearFilters,
}) {
  // Helper to extract typed filter values from the filters prop
  const extractTypedFilters = useCallback((srcFilters) => {
    const res = {};
    if (!srcFilters) return res;
    Object.entries(srcFilters).forEach(([k, v]) => {
      if (
        k.startsWith('like_') ||
        k.startsWith('min_') ||
        k.startsWith('max_') ||
        k.startsWith('from_') ||
        k.startsWith('to_')
      ) {
        res[k] = v != null ? String(v) : '';
      }
    });
    return res;
  }, []);

  // Local state for all typed inputs to guarantee continuous typing without focus loss
  const [localInputs, setLocalInputs] = useState(() => extractTypedFilters(filters));

  // Ref tracking the last filters object we dispatched upstream to avoid feedback loops
  const lastDispatchedFiltersRef = useRef(filters);
  const debounceTimerRef = useRef(null);

  // Sync from external filters (e.g. on Reset button click, dataset replacement, or initial load)
  useEffect(() => {
    if (filters !== lastDispatchedFiltersRef.current) {
      setLocalInputs(extractTypedFilters(filters));
      lastDispatchedFiltersRef.current = filters;
    }
  }, [filters, extractTypedFilters]);

  // Flush any pending changes to parent
  const flushChanges = useCallback(
    (pendingInputs) => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = null;
      }

      const current = pendingInputs !== undefined ? pendingInputs : localInputs;
      const updated = { ...filters };
      let changed = false;

      // Update or add typed values
      Object.entries(current).forEach(([k, v]) => {
        const trimmed = v != null ? String(v).trim() : '';
        const existing = filters[k] != null ? String(filters[k]).trim() : '';
        if (trimmed !== existing) {
          changed = true;
          if (trimmed !== '') {
            updated[k] = trimmed;
          } else {
            delete updated[k];
          }
        }
      });

      // Remove any keys that were cleared from current
      Object.keys(filters).forEach((k) => {
        if (
          (k.startsWith('like_') ||
            k.startsWith('min_') ||
            k.startsWith('max_') ||
            k.startsWith('from_') ||
            k.startsWith('to_')) &&
          !(k in current)
        ) {
          changed = true;
          delete updated[k];
        }
      });

      if (changed) {
        lastDispatchedFiltersRef.current = updated;
        onFilterChange(updated);
      }
    },
    [filters, localInputs, onFilterChange]
  );

  // Handle keystroke / input changes with 350ms debounce
  const handleInputChange = (filterKey, val) => {
    const nextInputs = { ...localInputs, [filterKey]: val };
    setLocalInputs(nextInputs);

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(() => {
      flushChanges(nextInputs);
    }, 350);
  };

  // Immediate flush on Enter key press
  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      flushChanges();
    }
  };

  // Immediate flush on input blur
  const handleBlur = () => {
    flushChanges();
  };

  // Clean up debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  // Dropdown changes apply immediately (select doesn't suffer from typing focus loss)
  const handleDropdownChange = (colKey, val) => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    const updated = { ...filters };
    const filterKey = `eq_${colKey}`;
    if (val !== '' && val !== 'all') {
      updated[filterKey] = val;
    } else {
      delete updated[filterKey];
    }
    lastDispatchedFiltersRef.current = updated;
    onFilterChange(updated);
  };

  // Clear filters handler
  const handleReset = () => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = null;
    }
    setLocalInputs({});
    lastDispatchedFiltersRef.current = {};
    if (onClearFilters) {
      onClearFilters();
    }
  };

  return (
    <tr className="filter-row">
      {columns.map((col) => {
        const key = col.key;
        const filterType = col.filterType;
        const colOptions = filterOptions[key];

        return (
          <th key={key} className="filter-cell" style={{ padding: '6px 8px', verticalAlign: 'middle' }}>
            {/* CATEGORY: Dropdown */}
            {filterType === 'CATEGORY' && (
              <select
                className="filter-control filter-select"
                value={filters[`eq_${key}`] || 'all'}
                onChange={(e) => handleDropdownChange(key, e.target.value)}
                style={{ width: '100%', minWidth: '100px', fontSize: '0.82rem', padding: '4px' }}
                aria-label={`Filter by ${col.label}`}
              >
                <option value="all">All</option>
                {filterOptionsError ? (
                  <option disabled>{filterOptionsError}</option>
                ) : Array.isArray(colOptions) && colOptions.length > 0 ? (
                  colOptions.map((opt) => (
                    <option key={String(opt)} value={String(opt)}>
                      {String(opt)}
                    </option>
                  ))
                ) : null}
              </select>
            )}

            {/* TEXT_SEARCH: Debounced Text Input */}
            {filterType === 'TEXT_SEARCH' && (
              <input
                key={`text_${key}`}
                type="text"
                placeholder={`Search ${col.label}...`}
                className="filter-control filter-input"
                value={localInputs[`like_${key}`] ?? ''}
                onChange={(e) => handleInputChange(`like_${key}`, e.target.value)}
                onKeyDown={handleKeyDown}
                onBlur={handleBlur}
                style={{ width: '100%', minWidth: '110px', fontSize: '0.82rem', padding: '4px 6px' }}
                aria-label={`Search ${col.label}`}
              />
            )}

            {/* RANGE: Min & Max Number Inputs */}
            {filterType === 'RANGE' && (
              <div className="range-filter-group" style={{ display: 'flex', gap: '4px' }}>
                <input
                  key={`min_${key}`}
                  type="number"
                  placeholder="Min"
                  title={colOptions && colOptions.min !== undefined ? `Min available: ${colOptions.min}` : 'Minimum value'}
                  step={col.numberKind === 'DECIMAL' ? 'any' : '1'}
                  className="filter-control range-input"
                  value={localInputs[`min_${key}`] ?? ''}
                  onChange={(e) => handleInputChange(`min_${key}`, e.target.value)}
                  onKeyDown={handleKeyDown}
                  onBlur={handleBlur}
                  style={{ width: '50%', minWidth: '55px', fontSize: '0.78rem', padding: '4px' }}
                  aria-label={`Min ${col.label}`}
                />
                <input
                  key={`max_${key}`}
                  type="number"
                  placeholder="Max"
                  title={colOptions && colOptions.max !== undefined ? `Max available: ${colOptions.max}` : 'Maximum value'}
                  step={col.numberKind === 'DECIMAL' ? 'any' : '1'}
                  className="filter-control range-input"
                  value={localInputs[`max_${key}`] ?? ''}
                  onChange={(e) => handleInputChange(`max_${key}`, e.target.value)}
                  onKeyDown={handleKeyDown}
                  onBlur={handleBlur}
                  style={{ width: '50%', minWidth: '55px', fontSize: '0.78rem', padding: '4px' }}
                  aria-label={`Max ${col.label}`}
                />
              </div>
            )}

            {/* DATE_RANGE: From & To Date Pickers */}
            {filterType === 'DATE_RANGE' && (
              <div className="date-range-filter-group" style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                <input
                  key={`from_${key}`}
                  type="date"
                  className="filter-control date-input"
                  value={localInputs[`from_${key}`] ?? ''}
                  onChange={(e) => handleInputChange(`from_${key}`, e.target.value)}
                  onKeyDown={handleKeyDown}
                  onBlur={handleBlur}
                  style={{ width: '100%', minWidth: '100px', fontSize: '0.75rem', padding: '2px' }}
                  title={colOptions && colOptions.min !== undefined ? `From (earliest: ${colOptions.min})` : 'From date'}
                  aria-label={`From ${col.label}`}
                />
                <input
                  key={`to_${key}`}
                  type="date"
                  className="filter-control date-input"
                  value={localInputs[`to_${key}`] ?? ''}
                  onChange={(e) => handleInputChange(`to_${key}`, e.target.value)}
                  onKeyDown={handleKeyDown}
                  onBlur={handleBlur}
                  style={{ width: '100%', minWidth: '100px', fontSize: '0.75rem', padding: '2px' }}
                  title={colOptions && colOptions.max !== undefined ? `To (latest: ${colOptions.max})` : 'To date'}
                  aria-label={`To ${col.label}`}
                />
              </div>
            )}
          </th>
        );
      })}

      {/* Actions Column: Reset button */}
      <th className="filter-cell filter-actions-cell" style={{ textAlign: 'center', minWidth: '90px' }}>
        <button
          type="button"
          className="btn-clear-filters"
          onClick={handleReset}
          style={{
            fontSize: '0.75rem',
            padding: '4px 8px',
            backgroundColor: '#f3f4f6',
            color: '#374151',
            border: '1px solid #d1d5db',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
          title="Reset all active filters"
        >
          Reset
        </button>
      </th>
    </tr>
  );
}
