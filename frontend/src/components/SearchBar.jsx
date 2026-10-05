import React, { useState, useEffect, useRef } from 'react';

const CITIES = ['All', 'Bangalore', 'Pune', 'New Delhi'];
const GENDERS = ['All', 'Male', 'Female'];

/**
 * SearchBar component supporting:
 * - Free text input for Employee ID, city, or education (debounced ~400ms)
 * - City dropdown
 * - Gender dropdown
 * - Reset button
 */
export default function SearchBar({ filters, onFilterChange }) {
  const [searchInput, setSearchInput] = useState(filters.search || '');
  const [city, setCity] = useState(filters.city || 'All');
  const [gender, setGender] = useState(filters.gender || 'All');

  const isInitialMount = useRef(true);

  // Debounce search text input by 400ms
  useEffect(() => {
    // Skip debounce on initial mount if value matches parent
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }

    const handler = setTimeout(() => {
      onFilterChange({
        search: searchInput,
        city: city === 'All' ? '' : city,
        gender: gender === 'All' ? '' : gender,
      });
    }, 400);

    return () => clearTimeout(handler);
  }, [searchInput]);

  const handleCityChange = (e) => {
    const newCity = e.target.value;
    setCity(newCity);
    onFilterChange({
      search: searchInput,
      city: newCity === 'All' ? '' : newCity,
      gender: gender === 'All' ? '' : gender,
    });
  };

  const handleGenderChange = (e) => {
    const newGender = e.target.value;
    setGender(newGender);
    onFilterChange({
      search: searchInput,
      city: city === 'All' ? '' : city,
      gender: newGender === 'All' ? '' : newGender,
    });
  };

  const handleReset = () => {
    setSearchInput('');
    setCity('All');
    setGender('All');
    onFilterChange({
      search: '',
      city: '',
      gender: '',
    });
  };

  return (
    <div className="search-filter-card">
      <div className="search-filter-header">
        <h3>Filter & Search Employees</h3>
      </div>

      <div className="filters-row">
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label htmlFor="search-input">Search (ID, City, Education)</label>
          <input
            id="search-input"
            type="text"
            placeholder="Type ID, city, or education..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
        </div>

        <div className="form-group" style={{ marginBottom: 0 }}>
          <label htmlFor="city-select">City</label>
          <select id="city-select" value={city} onChange={handleCityChange}>
            {CITIES.map((c) => (
              <option key={c} value={c}>
                {c === 'All' ? 'All Cities' : c}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group" style={{ marginBottom: 0 }}>
          <label htmlFor="gender-select">Gender</label>
          <select id="gender-select" value={gender} onChange={handleGenderChange}>
            {GENDERS.map((g) => (
              <option key={g} value={g}>
                {g === 'All' ? 'All Genders' : g}
              </option>
            ))}
          </select>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button type="button" onClick={handleReset} className="btn btn-secondary">
            Reset
          </button>
        </div>
      </div>
    </div>
  );
}
