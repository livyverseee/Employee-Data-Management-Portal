import React from 'react';

/**
 * FilterRow component:
 * Dynamically renders per-column filter controls derived from the dataset schema.
 * - Categorical fields -> Dropdown selects with distinct values from /filter-options
 * - Numeric fields -> Min / Max range inputs
 * - Other fields -> Text filter inputs
 * - Reset button to clear active filters
 */
export default function FilterRow({
  schema = [],
  filters = {},
  filterOptions = {},
  filterOptionsError = null,
  onFilterChange,
  onClearFilters,
}) {
  const handleChange = (key, value) => {
    onFilterChange({
      ...filters,
      [key]: value === '' ? '' : value,
    });
  };

  const hasActiveFilters = Object.entries(filters).some(
    ([k, v]) => k !== 'search' && k !== 'searchField' && v !== '' && v !== null && v !== undefined
  );

  const formatOptionLabel = (fieldName, val) => {
    if (val === null || val === undefined) return '';
    if (fieldName === 'paymentTier') return `Tier ${val}`;
    if (fieldName === 'leaveOrNot') return val === 1 ? 'Left' : 'Active';
    if (typeof val === 'boolean') return val ? 'Yes' : 'No';
    return String(val);
  };

  const optionsMap = filterOptions.options || {};
  const rangesMap = filterOptions.ranges || {};

  return (
    <>
      {filterOptionsError && (
        <tr className="filter-error-row">
          <th colSpan={schema.length + 1} className="filter-error-cell">
            <span className="filter-error-banner">
              ⚠️ Unable to load filter options: {filterOptionsError}
            </span>
          </th>
        </tr>
      )}

      <tr className="filter-row">
        {schema.map((field) => {
          const fieldName = field.name;
          const distinctOptions = optionsMap[fieldName];
          const hasDistinct = Array.isArray(distinctOptions) && distinctOptions.length > 0;
          const range = rangesMap[fieldName];

          return (
            <th key={fieldName}>
              {hasDistinct ? (
                /* Categorical dropdown */
                <select
                  className="filter-select"
                  value={filters[fieldName] ?? ''}
                  onChange={(e) => handleChange(fieldName, e.target.value)}
                  aria-label={`Filter by ${field.label}`}
                >
                  <option value="">All</option>
                  {distinctOptions.map((opt) => (
                    <option key={String(opt)} value={String(opt)}>
                      {formatOptionLabel(fieldName, opt)}
                    </option>
                  ))}
                </select>
              ) : field.type === 'number' ? (
                /* Numeric range filter */
                <div className="age-filter-inputs">
                  <input
                    type="number"
                    placeholder="Min"
                    className="filter-input-small"
                    value={filters[`${fieldName}Min`] || ''}
                    onChange={(e) => handleChange(`${fieldName}Min`, e.target.value)}
                    min={range?.min}
                    max={range?.max}
                    aria-label={`Min ${field.label}`}
                  />
                  <span className="age-sep">-</span>
                  <input
                    type="number"
                    placeholder="Max"
                    className="filter-input-small"
                    value={filters[`${fieldName}Max`] || ''}
                    onChange={(e) => handleChange(`${fieldName}Max`, e.target.value)}
                    min={range?.min}
                    max={range?.max}
                    aria-label={`Max ${field.label}`}
                  />
                </div>
              ) : (
                /* General text filter */
                <input
                  type="text"
                  placeholder={`Filter...`}
                  className="filter-input"
                  value={filters[fieldName] || ''}
                  onChange={(e) => handleChange(fieldName, e.target.value)}
                  aria-label={`Filter by ${field.label}`}
                />
              )}
            </th>
          );
        })}

        {/* Actions column: Reset button */}
        <th>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onClearFilters}
            disabled={!hasActiveFilters}
            title="Reset all column filters"
          >
            Reset
          </button>
        </th>
      </tr>
    </>
  );
}
