import React, { useState, useEffect, useCallback } from 'react';
import Header from '../../components/Header';
import DatasetInfoBar from '../../components/DatasetInfoBar';
import DynamicTable from '../../components/DynamicTable';
import PaginationBar from '../../components/PaginationBar';
import RecordDetailsModal from '../../components/RecordDetailsModal';
import Toast from '../../components/Toast';
import {
  getActiveDatasetApi,
  getRecordsApi,
  getFilterOptionsApi,
  exportExcelApi,
} from '../../services/api';

/**
 * Employee Dashboard (/employee/dashboard):
 * - Read-only workforce directory for employees.
 * - Dynamic column filtering, global text search, detail inspection modal.
 * - Export Excel (.xlsx) scoped to active filters.
 * - No Add, Edit, Delete, Upload, Replace, or XML download anywhere.
 * - No dataset message: "The dean has not uploaded any data yet."
 * - Styled with the Teal/Green Employee portal theme.
 */
export default function EmployeeDashboard() {
  // Dataset metadata & schema
  const [dataset, setDataset] = useState({ exists: false, columns: [] });
  const [loadingDataset, setLoadingDataset] = useState(true);

  // Paginated records
  const [records, setRecords] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingRecords, setLoadingRecords] = useState(false);

  // Search & Filters
  const [filters, setFilters] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Debounce global search term (350ms) to ensure continuous typing without lagging or loss of focus
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Dynamic filter options
  const [filterOptions, setFilterOptions] = useState({});
  const [filterOptionsError, setFilterOptionsError] = useState(null);

  // Details Modal
  const [detailsRecord, setDetailsRecord] = useState(null);

  // Toast & Export states
  const [toast, setToast] = useState(null);
  const [exportingExcel, setExportingExcel] = useState(false);

  const columns = dataset.columns || [];

  // Fetch active dataset info
  const fetchDatasetInfo = useCallback(async () => {
    try {
      const res = await getActiveDatasetApi();
      setDataset(res.data);
      return res.data;
    } catch {
      setDataset({ exists: false, columns: [] });
      return { exists: false, columns: [] };
    } finally {
      setLoadingDataset(false);
    }
  }, []);

  // Fetch filter options
  const fetchFilterOptions = useCallback(async () => {
    try {
      setFilterOptionsError(null);
      const res = await getFilterOptionsApi();
      setFilterOptions(res.data || {});
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to load filter options';
      setFilterOptionsError(msg);
    }
  }, []);

  // Fetch records
  const fetchRecords = useCallback(async () => {
    if (!dataset.exists) {
      setRecords([]);
      setTotalPages(0);
      setTotalElements(0);
      return;
    }

    setLoadingRecords(true);

    const params = {
      page: currentPage,
      size: pageSize,
    };

    if (debouncedSearch && debouncedSearch.trim()) {
      params.search = debouncedSearch.trim();
    }

    Object.entries(filters).forEach(([key, val]) => {
      if (val !== '' && val !== null && val !== undefined && val !== 'all') {
        params[key] = val;
      }
    });

    try {
      const res = await getRecordsApi(params);
      const data = res.data;
      setRecords(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to load records';
      setToast({ message: msg, type: 'error' });
      setRecords([]);
    } finally {
      setLoadingRecords(false);
    }
  }, [dataset.exists, currentPage, pageSize, filters, debouncedSearch]);

  // Initial load
  useEffect(() => {
    fetchDatasetInfo();
  }, [fetchDatasetInfo]);

  // When active dataset, page, pageSize, filters, or debounced search changes, reload records
  useEffect(() => {
    if (dataset.exists) {
      fetchRecords();
    }
  }, [dataset.exists, currentPage, pageSize, filters, debouncedSearch, fetchRecords]);

  // Fetch filter options when active dataset is loaded/replaced
  useEffect(() => {
    if (dataset.exists) {
      fetchFilterOptions();
    }
  }, [dataset.id, dataset.exists, fetchFilterOptions]);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    setCurrentPage(0);
  };

  const handleClearFilters = () => {
    setFilters({});
    setSearchTerm('');
    setDebouncedSearch('');
    setCurrentPage(0);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0);
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setCurrentPage(0);
  };

  const getExportParams = () => {
    const params = {};
    if (searchTerm && searchTerm.trim()) {
      params.search = searchTerm.trim();
    }
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined && v !== 'all') {
        params[k] = v;
      }
    });
    return params;
  };

  // Export Excel
  const handleExportExcel = async () => {
    if (totalElements === 0) {
      setToast({ message: 'No records to export', type: 'info' });
      return;
    }

    setExportingExcel(true);
    try {
      const res = await exportExcelApi(getExportParams());
      const blob = new Blob([res.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'employee_data.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      setToast({ message: 'Excel file exported successfully (employee_data.xlsx)', type: 'success' });
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to export Excel';
      setToast({ message: msg, type: 'error' });
    } finally {
      setExportingExcel(false);
    }
  };

  return (
    <div className="portal-container employee-portal">
      <Header role="EMPLOYEE" />

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      <main className="dashboard-content" style={{ maxWidth: '1440px', margin: '0 auto', padding: '24px 20px' }}>
        {loadingDataset ? (
          <div className="loading-indicator">
            <span>Loading portal data...</span>
          </div>
        ) : !dataset.exists ? (
          /* ================= NO DATASET STATE ================= */
          <div className="empty-portal-card" style={{ maxWidth: '540px', margin: '60px auto', textAlign: 'center', padding: '40px 24px', backgroundColor: '#fff', borderRadius: '12px', border: '1px solid #e5e7eb' }}>
            <div className="empty-portal-icon" style={{ fontSize: '3rem', marginBottom: '12px' }}>📋</div>
            <h3 style={{ color: '#111827', marginBottom: '8px' }}>No Data Available</h3>
            <p style={{ color: '#6b7280', fontSize: '0.95rem' }}>
              The dean has not uploaded any data yet.
            </p>
          </div>
        ) : (
          /* ================= ACTIVE DATASET VIEW ================= */
          <div className="active-dataset-view">
            {/* Read-Only Info Bar */}
            <DatasetInfoBar dataset={dataset} isDean={false} />

            {/* Global Search & Single Excel Export Toolbar */}
            <div className="dashboard-toolbar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', gap: '14px', flexWrap: 'wrap' }}>
              <div className="toolbar-search-wrapper" style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1', minWidth: '280px', maxWidth: '520px' }}>
                <div className="search-input-wrapper" style={{ position: 'relative', width: '100%' }}>
                  <span className="search-icon" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }}>🔍</span>
                  <input
                    type="text"
                    placeholder="Search across all text columns..."
                    className="global-search-input"
                    value={searchTerm}
                    onChange={handleSearchChange}
                    style={{ width: '100%', padding: '9px 36px 9px 38px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '0.9rem' }}
                  />
                  {searchTerm && (
                    <button
                      type="button"
                      className="search-clear-btn"
                      onClick={() => { setSearchTerm(''); setDebouncedSearch(''); setCurrentPage(0); }}
                      style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.1rem', color: '#9ca3af' }}
                    >
                      &times;
                    </button>
                  )}
                </div>
                <span className="matching-records-badge" style={{ fontSize: '0.85rem', color: '#4b5563', whiteSpace: 'nowrap' }}>
                  {totalElements.toLocaleString()} matching {totalElements === 1 ? 'record' : 'records'}
                </span>
              </div>

              {/* Single Excel Export Button */}
              <div className="toolbar-actions" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleExportExcel}
                  disabled={exportingExcel || totalElements === 0}
                  title="Export filtered records as an Excel spreadsheet (.xlsx)"
                >
                  {exportingExcel ? 'Exporting...' : '📊 Export Excel'}
                </button>
              </div>
            </div>

            {/* Dynamic Records Card - Permanently mounted to guarantee input focus is preserved */}
            <div className="table-card" style={{ backgroundColor: '#fff', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', overflow: 'hidden', position: 'relative' }}>
              {loadingRecords && (
                <div
                  className="table-loading-bar"
                  style={{
                    height: '3px',
                    width: '100%',
                    backgroundColor: '#10b981',
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    zIndex: 5,
                  }}
                />
              )}

              <DynamicTable
                records={records}
                columns={columns}
                loading={loadingRecords}
                canEdit={false}
                onRowClick={(rec) => setDetailsRecord(rec)}
                filters={filters}
                filterOptions={filterOptions}
                filterOptionsError={filterOptionsError}
                onFilterChange={handleFilterChange}
                onClearFilters={handleClearFilters}
              />

              <PaginationBar
                currentPage={currentPage}
                pageSize={pageSize}
                totalPages={totalPages}
                totalElements={totalElements}
                onPageChange={(p) => setCurrentPage(p)}
                onPageSizeChange={handlePageSizeChange}
              />
            </div>
          </div>
        )}
      </main>

      {/* Record Details Modal */}
      {detailsRecord && (
        <RecordDetailsModal
          record={detailsRecord}
          columns={columns}
          onClose={() => setDetailsRecord(null)}
        />
      )}
    </div>
  );
}
