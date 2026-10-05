import React from 'react';

/**
 * FilterRow component:
 * Placed directly under the table header row.
 * Offers per-column filter inputs and dropdowns populated from /filter-options,
 * ageMin and ageMax range inputs, and a Clear Filters button.
 */
export default function FilterRow({
  filters,
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
    ([k, v]) => k !== 'search' && v !== '' && v !== null && v !== undefined
  );

  return (
    <>
      {filterOptionsError && (
        <tr className="filter-error-row">
          <th colSpan={11} className="filter-error-cell">
            <span className="filter-error-banner">
              ⚠️ Unable to load filter options: {filterOptionsError}
            </span>
          </th>
        </tr>
      )}

      <tr className="filter-row">
        {/* 1. Employee ID */}
        <th>
          <input
            type="text"
            placeholder="Filter ID..."
            className="filter-input"
            value={filters.employeeId || ''}
            onChange={(e) => handleChange('employeeId', e.target.value)}
            aria-label="Filter by Employee ID"
          />
        </th>

        {/* 2. Education */}
        <th>
          <select
            className="filter-select"
            value={filters.education || ''}
            onChange={(e) => handleChange('education', e.target.value)}
            aria-label="Filter by Education"
          >
            <option value="">All</option>
            {filterOptions.education?.map((edu) => (
              <option key={edu} value={edu}>
                {edu}
              </option>
            ))}
          </select>
        </th>

        {/* 3. Joining Year */}
        <th>
          <select
            className="filter-select"
            value={filters.joiningYear || ''}
            onChange={(e) => handleChange('joiningYear', e.target.value)}
            aria-label="Filter by Joining Year"
          >
            <option value="">All</option>
            {filterOptions.joiningYear?.map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
        </th>

        {/* 4. City */}
        <th>
          <select
            className="filter-select"
            value={filters.city || ''}
            onChange={(e) => handleChange('city', e.target.value)}
            aria-label="Filter by City"
          >
            <option value="">All</option>
            {filterOptions.city?.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </th>

        {/* 5. Payment Tier */}
        <th>
          <select
            className="filter-select"
            value={filters.paymentTier || ''}
            onChange={(e) => handleChange('paymentTier', e.target.value)}
            aria-label="Filter by Payment Tier"
          >
            <option value="">All</option>
            {filterOptions.paymentTier?.map((tier) => (
              <option key={tier} value={tier}>
                Tier {tier}
              </option>
            ))}
          </select>
        </th>

        {/* 6. Age (Min - Max) */}
        <th>
          <div className="age-filter-inputs">
            <input
              type="number"
              placeholder="Min"
              className="filter-input-small"
              value={filters.ageMin || ''}
              onChange={(e) => handleChange('ageMin', e.target.value)}
              min={filterOptions.minAge || 18}
              max={filterOptions.maxAge || 100}
              aria-label="Minimum Age"
            />
            <span className="age-sep">-</span>
            <input
              type="number"
              placeholder="Max"
              className="filter-input-small"
              value={filters.ageMax || ''}
              onChange={(e) => handleChange('ageMax', e.target.value)}
              min={filterOptions.minAge || 18}
              max={filterOptions.maxAge || 100}
              aria-label="Maximum Age"
            />
          </div>
        </th>

        {/* 7. Gender */}
        <th>
          <select
            className="filter-select"
            value={filters.gender || ''}
            onChange={(e) => handleChange('gender', e.target.value)}
            aria-label="Filter by Gender"
          >
            <option value="">All</option>
            {filterOptions.gender?.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </th>

        {/* 8. Ever Benched */}
        <th>
          <select
            className="filter-select"
            value={filters.everBenched || ''}
            onChange={(e) => handleChange('everBenched', e.target.value)}
            aria-label="Filter by Ever Benched"
          >
            <option value="">All</option>
            {filterOptions.everBenched?.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </th>

        {/* 9. Experience (yrs) */}
        <th>
          <select
            className="filter-select"
            value={filters.experienceInCurrentDomain || ''}
            onChange={(e) => handleChange('experienceInCurrentDomain', e.target.value)}
            aria-label="Filter by Experience"
          >
            <option value="">All</option>
            {filterOptions.experienceInCurrentDomain?.map((exp) => (
              <option key={exp} value={exp}>
                {exp} {exp === 1 ? 'yr' : 'yrs'}
              </option>
            ))}
          </select>
        </th>

        {/* 10. Status (Leave or Not) */}
        <th>
          <select
            className="filter-select"
            value={filters.leaveOrNot ?? ''}
            onChange={(e) => handleChange('leaveOrNot', e.target.value)}
            aria-label="Filter by Status"
          >
            <option value="">All</option>
            {filterOptions.leaveOrNot?.map((val) => (
              <option key={val} value={val}>
                {val === 1 ? 'Left' : 'Active'}
              </option>
            ))}
          </select>
        </th>

        {/* 11. Reset Filter Button */}
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
