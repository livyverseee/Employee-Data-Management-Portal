import React, { useState, useEffect, useCallback } from 'react';
import Header from '../../components/Header';
import DatasetInfoBar from '../../components/DatasetInfoBar';
import EmployeeTable from '../../components/EmployeeTable';
import Pagination from '../../components/Pagination';
import EmployeeModal from '../../components/EmployeeModal';
import Toast from '../../components/Toast';
import {
  getActiveDatasetApi,
  getEmployeesApi,
  getFilterOptionsApi,
  exportCsvApi,
} from '../../services/api';

/**
 * Employee Dashboard (/employee/dashboard):
 * - View-only workforce directory for employees.
 * - Dynamic column filtering, global search, detail inspection, and CSV export.
 * - Cannot upload, replace, edit, or delete datasets or records.
 * - Styled with the Teal/Green Employee portal theme.
 */
export default function EmployeeDashboard() {
  // Dataset metadata
  const [dataset, setDataset] = useState({ exists: false });
  const [loadingDataset, setLoadingDataset] = useState(true);

  // Employee list & pagination
  const [employees, setEmployees] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingEmployees, setLoadingEmployees] = useState(false);

  // Filters state
  const [filters, setFilters] = useState({
    search: '',
    employeeId: '',
    education: '',
    city: '',
    joiningYear: '',
    paymentTier: '',
    ageMin: '',
    ageMax: '',
    gender: '',
    everBenched: '',
    leaveOrNot: '',
  });

  // Dynamic filter options populated from active dataset
  const [filterOptions, setFilterOptions] = useState({});

  // Details Modal
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(null);

  // Toast & Export state
  const [toast, setToast] = useState(null);
  const [exporting, setExporting] = useState(false);

  // Fetch active dataset info
  const fetchDatasetInfo = useCallback(async () => {
    try {
      const res = await getActiveDatasetApi();
      setDataset(res.data);
      return res.data;
    } catch {
      setDataset({ exists: false });
      return { exists: false };
    } finally {
      setLoadingDataset(false);
    }
  }, []);

  // Fetch distinct filter options
  const fetchFilterOptions = useCallback(async () => {
    try {
      const res = await getFilterOptionsApi();
      setFilterOptions(res.data || {});
    } catch {
      // Ignored if no active dataset
    }
  }, []);

  // Fetch employees matching active criteria
  const fetchEmployees = useCallback(async () => {
    if (!dataset.exists) {
      setEmployees([]);
      setTotalPages(0);
      setTotalElements(0);
      return;
    }

    setLoadingEmployees(true);

    const params = {
      page: currentPage,
      size: pageSize,
    };

    Object.entries(filters).forEach(([key, val]) => {
      if (val !== '' && val !== null && val !== undefined) {
        params[key] = val;
      }
    });

    try {
      const res = await getEmployeesApi(params);
      const data = res.data;
      setEmployees(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to load employees';
      setToast({ type: 'error', message: msg });
    } finally {
      setLoadingEmployees(false);
    }
  }, [dataset.exists, currentPage, pageSize, filters]);

  // Initial load
  useEffect(() => {
    fetchDatasetInfo().then((ds) => {
      if (ds && ds.exists) {
        fetchFilterOptions();
      }
    });
  }, [fetchDatasetInfo, fetchFilterOptions]);

  // Refetch when filters or page changes
  useEffect(() => {
    if (dataset.exists) {
      fetchEmployees();
    }
  }, [dataset.exists, fetchEmployees]);

  // Handle filter changes
  const handleFilterChange = (newFilters) => {
    setCurrentPage(0);
    setFilters(newFilters);
  };

  // Clear all filters
  const handleClearFilters = () => {
    setCurrentPage(0);
    setFilters({
      search: '',
      employeeId: '',
      education: '',
      city: '',
      joiningYear: '',
      paymentTier: '',
      ageMin: '',
      ageMax: '',
      gender: '',
      everBenched: '',
      leaveOrNot: '',
    });
  };

  // Export CSV
  const handleExportCsv = async () => {
    setExporting(true);
    const params = {};
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) {
        params[k] = v;
      }
    });

    try {
      const response = await exportCsvApi(params);
      const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `employees_${dataset.fileName || 'export'}.csv`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

      setToast({ type: 'success', message: 'CSV export downloaded successfully.' });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to export CSV';
      setToast({ type: 'error', message: msg });
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="dashboard-wrapper portal-theme-employee">
      <Header role="EMPLOYEE" />

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <main className="dashboard-content">
        {loadingDataset ? (
          <div className="loading-indicator">
            <span>Loading portal data...</span>
          </div>
        ) : !dataset.exists ? (
          /* ================= NO DATASET STATE ================= */
          <div className="empty-portal-card">
            <div className="empty-portal-icon">📋</div>
            <h3>No Dataset Uploaded</h3>
            <p>
              No employee dataset has been uploaded by the Dean yet. Please check back later or
              contact the portal administrator.
            </p>
          </div>
        ) : (
          /* ================= ACTIVE DATASET VIEW ================= */
          <div className="active-dataset-view">
            {/* Read-Only Info Bar */}
            <DatasetInfoBar dataset={dataset} canReplace={false} />

            {/* Global Search & Export Toolbar */}
            <div className="dashboard-toolbar">
              <div className="search-input-wrapper">
                <span className="search-icon">🔍</span>
                <input
                  type="text"
                  placeholder="Quick search by ID, City, or Education..."
                  className="global-search-input"
                  value={filters.search}
                  onChange={(e) =>
                    handleFilterChange({ ...filters, search: e.target.value })
                  }
                />
                {filters.search && (
                  <button
                    type="button"
                    className="search-clear-btn"
                    onClick={() => handleFilterChange({ ...filters, search: '' })}
                  >
                    &times;
                  </button>
                )}
              </div>

              <div className="toolbar-actions">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={handleExportCsv}
                  disabled={exporting || totalElements === 0}
                  title="Export filtered records to CSV"
                >
                  {exporting ? 'Generating CSV...' : '📥 Export CSV'}
                </button>
              </div>
            </div>

            {/* Employee Records Card */}
            <div className="table-card">
              <div className="table-card-header">
                <div>
                  <h3>Workforce Directory</h3>
                  <p className="table-card-sub">
                    Showing {employees.length} of {totalElements.toLocaleString()} records
                  </p>
                </div>
              </div>

              {loadingEmployees ? (
                <div className="loading-indicator">
                  <span>Loading employees...</span>
                </div>
              ) : (
                <>
                  <EmployeeTable
                    employees={employees}
                    canEdit={false}
                    onRowClick={(id) => setSelectedEmployeeId(id)}
                    filters={filters}
                    filterOptions={filterOptions}
                    onFilterChange={handleFilterChange}
                    onClearFilters={handleClearFilters}
                  />

                  <Pagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    totalElements={totalElements}
                    onPageChange={(p) => setCurrentPage(p)}
                  />
                </>
              )}
            </div>
          </div>
        )}
      </main>

      {/* Details Modal */}
      {selectedEmployeeId && (
        <EmployeeModal
          id={selectedEmployeeId}
          onClose={() => setSelectedEmployeeId(null)}
        />
      )}
    </div>
  );
}
