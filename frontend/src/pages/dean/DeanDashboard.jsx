import React, { useState, useEffect, useCallback } from 'react';
import Header from '../../components/Header';
import DatasetInfoBar from '../../components/DatasetInfoBar';
import UploadXml from '../../components/UploadXml';
import ReplaceDatasetModal from '../../components/ReplaceDatasetModal';
import DynamicTable from '../../components/DynamicTable';
import PaginationBar from '../../components/PaginationBar';
import RecordDetailsModal from '../../components/RecordDetailsModal';
import RecordMutationModal from '../../components/RecordMutationModal';
import Toast from '../../components/Toast';
import {
  getActiveDatasetApi,
  getRecordsApi,
  getFilterOptionsApi,
  deleteRecordApi,
  downloadDatasetXmlApi,
  exportExcelApi,
} from '../../services/api';

/**
 * Dean Dashboard (/dean/dashboard):
 * - Admin lifecycle: Centered Upload card when empty, Info bar with Replace & Download XML when active.
 * - Dynamic workforce directory: Schema-driven table, DynamicFilterRow, Global search.
 * - Add Employee & Edit Employee modals generated from schema with field error feedback.
 * - Row actions: Edit, Delete (with confirm). Highlight new row after Add and jump to last page.
 * - Export Excel (.xlsx) scoped to active filters.
 * - Enforces mandatory pagination with 10/25/50/100 page size options.
 */
export default function DeanDashboard() {
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

  // Global search & per-column filter parameters
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

  // Dynamic filter options (categories + ranges)
  const [filterOptions, setFilterOptions] = useState({});
  const [filterOptionsError, setFilterOptionsError] = useState(null);

  // Modals
  const [showReplaceModal, setShowReplaceModal] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);
  const [detailsRecord, setDetailsRecord] = useState(null);

  // Toast, Export, XML download, Highlight states
  const [toast, setToast] = useState(null);
  const [exportingExcel, setExportingExcel] = useState(false);
  const [downloadingXml, setDownloadingXml] = useState(false);
  const [highlightedId, setHighlightedId] = useState(null);

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

  // Fetch paginated records matching current criteria
  const fetchRecords = useCallback(async (pageOverride) => {
    if (!dataset.exists) {
      setRecords([]);
      setTotalPages(0);
      setTotalElements(0);
      return;
    }

    setLoadingRecords(true);

    const activePage = pageOverride !== undefined ? pageOverride : currentPage;
    const params = {
      page: activePage,
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
      if (pageOverride !== undefined) {
        setCurrentPage(pageOverride);
      }
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

  // Filter change resets to page 0
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

  // Upload success handler
  const handleInitialUploadSuccess = (newDataset) => {
    setToast({
      message: `Dataset "${newDataset.fileName}" initialized successfully!`,
      type: 'success',
    });
    setFilters({});
    setSearchTerm('');
    setCurrentPage(0);
    fetchDatasetInfo();
  };

  // Replace success handler
  const handleReplaceSuccess = (newDataset) => {
    setShowReplaceModal(false);
    setToast({
      message: `Dataset replaced successfully with "${newDataset.fileName}"!`,
      type: 'success',
    });
    setFilters({});
    setSearchTerm('');
    setCurrentPage(0);
    fetchDatasetInfo();
  };

  // Add record success handler: jump to last page and highlight new row
  const handleAddSuccess = async (savedRecord) => {
    setShowAddModal(false);
    setToast({ message: 'Employee added', type: 'success' });
    await fetchFilterOptions();
    const updatedDs = await fetchDatasetInfo();

    // Calculate last page
    const newTotal = (updatedDs.recordCount || totalElements + 1);
    const lastPage = Math.max(0, Math.ceil(newTotal / pageSize) - 1);

    setHighlightedId(savedRecord.id);
    fetchRecords(lastPage);

    // Remove highlight after 3.5 seconds
    setTimeout(() => {
      setHighlightedId(null);
    }, 3500);
  };

  // Edit record success handler
  const handleEditSuccess = (updatedRecord) => {
    setEditingRecord(null);
    setToast({ message: 'Employee updated successfully', type: 'success' });
    fetchRecords();
    fetchFilterOptions();
  };

  // Delete record handler
  const handleDeleteRecord = async (recordId) => {
    try {
      await deleteRecordApi(recordId);
      setToast({ message: 'Employee deleted', type: 'success' });

      // If this was the only row on the last page, step back one page
      let targetPage = currentPage;
      if (records.length === 1 && currentPage > 0) {
        targetPage = currentPage - 1;
        setCurrentPage(targetPage);
      }

      fetchRecords(targetPage);
      fetchFilterOptions();
      fetchDatasetInfo();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to delete record';
      setToast({ message: msg, type: 'error' });
    }
  };

  // Download XML handler
  const handleDownloadXml = async () => {
    setDownloadingXml(true);
    try {
      const res = await downloadDatasetXmlApi();
      const blob = new Blob([res.data], { type: 'application/xml;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'dataset.xml';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      setToast({ message: 'dataset.xml downloaded successfully', type: 'success' });
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to download XML';
      setToast({ message: msg, type: 'error' });
    } finally {
      setDownloadingXml(false);
    }
  };

  // Helper to extract active export parameters
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

  // Export Excel handler
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
    <div className="portal-container dean-portal">
      <Header role="DEAN" />

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
            <span>Loading active dataset...</span>
          </div>
        ) : !dataset.exists ? (
          /* ================= ONBOARDING / EMPTY VIEW ================= */
          <div className="onboarding-container" style={{ maxWidth: '640px', margin: '40px auto' }}>
            <UploadXml
              onUploadSuccess={handleInitialUploadSuccess}
              isOnboarding={true}
            />
          </div>
        ) : (
          /* ================= ACTIVE DATASET DASHBOARD ================= */
          <div className="active-dataset-view">
            {/* Metadata Info Bar */}
            <DatasetInfoBar
              dataset={dataset}
              isDean={true}
              onReplaceClick={() => setShowReplaceModal(true)}
              onDownloadXml={handleDownloadXml}
              downloadingXml={downloadingXml}
            />

            {/* Toolbar: Global Search, Add Employee, Export Button */}
            <div className="dashboard-toolbar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', gap: '14px', flexWrap: 'wrap' }}>
              {/* Left: Global Search & Matching count */}
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

              {/* Right: Add Employee & Single Excel Export button */}
              <div className="toolbar-actions" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => setShowAddModal(true)}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  <span>+</span> Add Employee
                </button>

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
                    backgroundColor: '#6366f1',
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
                canEdit={true}
                onRowClick={(rec) => setDetailsRecord(rec)}
                onEdit={(rec) => setEditingRecord(rec)}
                onDelete={handleDeleteRecord}
                filters={filters}
                filterOptions={filterOptions}
                filterOptionsError={filterOptionsError}
                onFilterChange={handleFilterChange}
                onClearFilters={handleClearFilters}
                highlightedId={highlightedId}
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

      {/* Add Record Modal */}
      {showAddModal && (
        <RecordMutationModal
          isOpen={true}
          isEdit={false}
          columns={columns}
          filterOptions={filterOptions}
          onClose={() => setShowAddModal(false)}
          onSuccess={handleAddSuccess}
        />
      )}

      {/* Edit Record Modal */}
      {editingRecord && (
        <RecordMutationModal
          isOpen={true}
          isEdit={true}
          record={editingRecord}
          columns={columns}
          filterOptions={filterOptions}
          onClose={() => setEditingRecord(null)}
          onSuccess={handleEditSuccess}
        />
      )}

      {/* Record Details Modal */}
      {detailsRecord && (
        <RecordDetailsModal
          record={detailsRecord}
          columns={columns}
          onClose={() => setDetailsRecord(null)}
        />
      )}

      {/* Replace Dataset Modal */}
      {showReplaceModal && (
        <ReplaceDatasetModal
          onClose={() => setShowReplaceModal(false)}
          onSuccess={handleReplaceSuccess}
        />
      )}
    </div>
  );
}
