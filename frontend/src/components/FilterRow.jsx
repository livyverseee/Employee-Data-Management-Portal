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
    <tr className="filter-row">
      {/* Employee ID */}
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

      {/* Education */}
      <th>
        <select
          className="filter-select"
          value={filters.education || ''}
          onChange={(e) => handleChange('education', e.target.value)}
          aria-label="Filter by Education"
        >
          <option value="">All</option>
          {filterOptions.educations?.map((edu) => (
            <option key={edu} value={edu}>
              {edu}
            </option>
          ))}
        </select>
      </th>

      {/* City */}
      <th>
        <select
          className="filter-select"
          value={filters.city || ''}
          onChange={(e) => handleChange('city', e.target.value)}
          aria-label="Filter by City"
        >
          <option value="">All</option>
          {filterOptions.cities?.map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>
      </th>

      {/* Age (Min - Max) */}
      <th>
        <div className="age-filter-inputs">
          <input
            type="number"
            placeholder="Min"
            className="filter-input-small"
            value={filters.ageMin || ''}
            onChange={(e) => handleChange('ageMin', e.target.value)}
            min="18"
            max="100"
            aria-label="Minimum Age"
          />
          <span className="age-sep">-</span>
          <input
            type="number"
            placeholder="Max"
            className="filter-input-small"
            value={filters.ageMax || ''}
            onChange={(e) => handleChange('ageMax', e.target.value)}
            min="18"
            max="100"
            aria-label="Maximum Age"
          />
        </div>
      </th>

      {/* Gender */}
      <th>
        <select
          className="filter-select"
          value={filters.gender || ''}
          onChange={(e) => handleChange('gender', e.target.value)}
          aria-label="Filter by Gender"
        >
          <option value="">All</option>
          {filterOptions.genders?.map((gender) => (
            <option key={gender} value={gender}>
              {gender}
            </option>
          ))}
        </select>
      </th>

      {/* Joining Year */}
      <th>
        <input
          type="number"
          placeholder="Year..."
          className="filter-input"
          value={filters.joiningYear || ''}
          onChange={(e) => handleChange('joiningYear', e.target.value)}
          aria-label="Filter by Joining Year"
        />
      </th>

      {/* Payment Tier */}
      <th>
        <select
          className="filter-select"
          value={filters.paymentTier || ''}
          onChange={(e) => handleChange('paymentTier', e.target.value)}
          aria-label="Filter by Payment Tier"
        >
          <option value="">All</option>
          {filterOptions.paymentTiers?.map((tier) => (
            <option key={tier} value={tier}>
              Tier {tier}
            </option>
          ))}
        </select>
      </th>

      {/* Status (Leave or Not) */}
      <th>
        <select
          className="filter-select"
          value={filters.leaveOrNot ?? ''}
          onChange={(e) => handleChange('leaveOrNot', e.target.value)}
          aria-label="Filter by Status"
        >
          <option value="">All</option>
          <option value="0">Active</option>
          <option value="1">Left</option>
        </select>
      </th>

      {/* Reset Filter Button */}
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
  );
}
