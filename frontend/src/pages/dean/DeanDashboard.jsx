import React, { useState, useEffect, useCallback } from 'react';
import Header from '../../components/Header';
import DatasetInfoBar from '../../components/DatasetInfoBar';
import UploadXml from '../../components/UploadXml';
import ReplaceDatasetModal from '../../components/ReplaceDatasetModal';
import EmployeeTable from '../../components/EmployeeTable';
import Pagination from '../../components/Pagination';
import EmployeeModal from '../../components/EmployeeModal';
import EmployeeEditModal from '../../components/EmployeeEditModal';
import Toast from '../../components/Toast';
import {
  getActiveDatasetApi,
  getEmployeesApi,
  getFilterOptionsApi,
  deleteEmployeeApi,
  exportCsvApi,
  exportExcelApi,
} from '../../services/api';

/**
 * Dean Dashboard (/dean/dashboard):
 * - Administrative dataset lifecycle management (Upload, Replace).
 * - Full Employee management (Filter, Search, Edit, Delete, Details modal, CSV export).
 * - Styled with the Indigo/Purple Dean portal theme.
 */
export default function DeanDashboard() {
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
    experienceInCurrentDomain: '',
    leaveOrNot: '',
  });

  // Dynamic filter options populated from backend
  const [filterOptions, setFilterOptions] = useState({});
  const [filterOptionsError, setFilterOptionsError] = useState(null);

  // Modals & Popups
  const [showReplaceModal, setShowReplaceModal] = useState(false);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(null);
  const [editingEmployee, setEditingEmployee] = useState(null);

  // Toast notification
  const [toast, setToast] = useState(null);
  const [exportingExcel, setExportingExcel] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);

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

  // Fetch filter options (distinct values from active dataset)
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

    // Append non-empty filters
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
      const msg = err.response?.data?.message || 'Failed to fetch employees';
      setToast({ type: 'error', message: msg });
    } finally {
      setLoadingEmployees(false);
    }
  }, [dataset.exists, currentPage, pageSize, filters]);

  // Initial load: check active dataset
  useEffect(() => {
    fetchDatasetInfo().then((ds) => {
      if (ds && ds.exists) {
        fetchFilterOptions();
      }
    });
  }, [fetchDatasetInfo, fetchFilterOptions]);

  // Refetch employees when dataset status, page, or filters change
  useEffect(() => {
    if (dataset.exists) {
      fetchEmployees();
    }
  }, [dataset.exists, fetchEmployees]);

  // Handle filter changes from column filter row or search input
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
      experienceInCurrentDomain: '',
      leaveOrNot: '',
    });
  };

  // On initial XML upload success
  const handleInitialUploadSuccess = (uploadedInfo) => {
    setDataset(uploadedInfo);
    setCurrentPage(0);
    fetchFilterOptions();
    setToast({
      type: 'success',
      message: `Dataset "${uploadedInfo.fileName}" uploaded successfully with ${uploadedInfo.recordCount} records!`,
    });
  };

  // On replace dataset success
  const handleReplaceSuccess = (replacedInfo) => {
    setShowReplaceModal(false);
    setDataset(replacedInfo);
    setCurrentPage(0);
    handleClearFilters();
    fetchFilterOptions();
    setToast({
      type: 'success',
      message: `Active dataset replaced with "${replacedInfo.fileName}" (${replacedInfo.recordCount} records).`,
    });
  };

  // On employee edit success
  const handleEditSuccess = (updatedEmp) => {
    setEditingEmployee(null);
    setToast({
      type: 'success',
      message: `Employee ${updatedEmp.employeeId} updated successfully.`,
    });
    fetchEmployees();
    fetchFilterOptions();
  };

  // On delete employee
  const handleDeleteEmployee = async (id, empId) => {
    try {
      await deleteEmployeeApi(id);
      setToast({
        type: 'success',
        message: `Employee ${empId} deleted successfully.`,
      });
      fetchEmployees();
      fetchFilterOptions();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete employee';
      setToast({ type: 'error', message: msg });
    }
  };

  // Export filtered dataset to Excel (.xlsx)
  const handleExportExcel = async () => {
    if (totalElements === 0) {
      setToast({ type: 'info', message: 'No records to export' });
      return;
    }

    setExportingExcel(true);
    const params = {};
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined) {
        params[k] = v;
      }
    });

    try {
      const response = await exportExcelApi(params);
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'employees.xlsx';
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

      setToast({ type: 'success', message: 'Excel export downloaded successfully (employees.xlsx).' });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to export Excel file';
      setToast({ type: 'error', message: msg });
    } finally {
      setExportingExcel(false);
    }
  };

  // Export filtered dataset to CSV
  const handleExportCsv = async () => {
    if (totalElements === 0) {
      setToast({ type: 'info', message: 'No records to export' });
      return;
    }

    setExportingCsv(true);
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
      link.download = 'employees.csv';
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

      setToast({ type: 'success', message: 'CSV export downloaded successfully (employees.csv).' });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to export CSV file';
      setToast({ type: 'error', message: msg });
    } finally {
      setExportingCsv(false);
    }
  };

  return (
    <div className="dashboard-wrapper portal-theme-dean">
      <Header role="DEAN" />

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
            <span>Checking active dataset...</span>
          </div>
        ) : !dataset.exists ? (
          /* ================= ONBOARDING VIEW ================= */
          <div className="onboarding-container">
            <UploadXml
              onUploadSuccess={handleInitialUploadSuccess}
              isOnboarding={true}
            />
          </div>
        ) : (
          /* ================= ACTIVE DATASET VIEW ================= */
          <div className="active-dataset-view">
            {/* Active Dataset Info Bar */}
            <DatasetInfoBar
              dataset={dataset}
              canReplace={true}
              onReplaceClick={() => setShowReplaceModal(true)}
            />

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
                  className="btn btn-primary"
                  onClick={handleExportExcel}
                  disabled={exportingExcel || exportingCsv || totalElements === 0}
                  title="Export filtered records as Excel (.xlsx)"
                >
                  {exportingExcel ? 'Exporting...' : '📊 Export Excel'}
                </button>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={handleExportCsv}
                  disabled={exportingExcel || exportingCsv || totalElements === 0}
                  title="Export filtered records as CSV (.csv)"
                >
                  {exportingCsv ? 'Exporting...' : '📄 Export CSV'}
                </button>
              </div>
            </div>

            {/* Employee Records Card with Per-Column Filter Row */}
            <div className="table-card">
              <div className="table-card-header">
                <div>
                  <h3>Employee Records</h3>
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
                    canEdit={true}
                    onRowClick={(id) => setSelectedEmployeeId(id)}
                    onEdit={(emp) => setEditingEmployee(emp)}
                    onDelete={handleDeleteEmployee}
                    filters={filters}
                    filterOptions={filterOptions}
                    filterOptionsError={filterOptionsError}
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

      {/* Edit Modal (DEAN only) */}
      {editingEmployee && (
        <EmployeeEditModal
          employee={editingEmployee}
          onClose={() => setEditingEmployee(null)}
          onSaveSuccess={handleEditSuccess}
        />
      )}

      {/* Replace Dataset Modal (DEAN only) */}
      {showReplaceModal && (
        <ReplaceDatasetModal
          onClose={() => setShowReplaceModal(false)}
          onSuccess={handleReplaceSuccess}
        />
      )}
    </div>
  );
}
