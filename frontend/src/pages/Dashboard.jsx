import React, { useState, useEffect, useCallback } from 'react';
import Header from '../components/Header';
import UploadXml from '../components/UploadXml';
import SearchBar from '../components/SearchBar';
import EmployeeTable from '../components/EmployeeTable';
import Pagination from '../components/Pagination';
import EmployeeModal from '../components/EmployeeModal';
import {
  getEmployeesApi,
  deleteEmployeeApi,
  exportExcelApi,
} from '../services/api';

/**
 * Shared Dashboard component parameterized by role ('DEAN' or 'EMPLOYEE').
 */
export default function Dashboard({ role }) {
  const [employees, setEmployees] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [filters, setFilters] = useState({
    search: '',
    city: '',
    gender: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [exporting, setExporting] = useState(false);

  // Selected employee ID for details modal
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(null);

  const isDean = role === 'DEAN';

  // Fetch employees matching current filters and page
  const fetchEmployees = useCallback(async () => {
    setLoading(true);
    setError(null);

    const params = {
      page: currentPage,
      size: pageSize,
    };
    if (filters.search) params.search = filters.search;
    if (filters.city) params.city = filters.city;
    if (filters.gender) params.gender = filters.gender;

    try {
      const res = await getEmployeesApi(params);
      const data = res.data;
      setEmployees(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      if (err.response && err.response.status === 403) {
        setError("You don't have permission");
      } else {
        const msg = err.response?.data?.message || err.message || 'Failed to load employees';
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, filters]);

  useEffect(() => {
    fetchEmployees();
  }, [fetchEmployees]);

  // When filters change: reset page to 0 and update filter values
  const handleFilterChange = (newFilters) => {
    setCurrentPage(0);
    setFilters(newFilters);
  };

  // Delete employee (DEAN only)
  const handleDeleteEmployee = async (id) => {
    setError(null);
    try {
      await deleteEmployeeApi(id);
      // Refresh table after deletion
      fetchEmployees();
    } catch (err) {
      if (err.response && err.response.status === 403) {
        setError("You don't have permission");
      } else {
        const msg = err.response?.data?.message || err.message || 'Failed to delete employee';
        setError(msg);
      }
    }
  };

  // Export Excel with current filters
  const handleExportExcel = async () => {
    setExporting(true);
    setError(null);

    const params = {};
    if (filters.search) params.search = filters.search;
    if (filters.city) params.city = filters.city;
    if (filters.gender) params.gender = filters.gender;

    try {
      const response = await exportExcelApi(params);

      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'employee_data.xlsx');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      if (err.response && err.response.status === 403) {
        setError("You don't have permission");
      } else {
        const msg = err.response?.data?.message || err.message || 'Failed to export Excel';
        setError(msg);
      }
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="dashboard-container">
      {/* Top Header */}
      <Header role={role} />

      {/* XML Upload Section (DEAN only) */}
      {isDean && <UploadXml onUploadSuccess={fetchEmployees} />}

      {/* Search & Filters */}
      <SearchBar filters={filters} onFilterChange={handleFilterChange} />

      {/* Global Error Banner */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Employee Data Table Card */}
      <div className="table-card">
        <div style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border)' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 600 }}>Employee Records</h3>
          <button
            type="button"
            className="btn btn-outline"
            onClick={handleExportExcel}
            disabled={exporting || totalElements === 0}
          >
            {exporting ? 'Preparing Excel...' : 'Export Excel'}
          </button>
        </div>

        {loading ? (
          <div className="loading-indicator">
            <span>Loading employees...</span>
          </div>
        ) : (
          <>
            <EmployeeTable
              employees={employees}
              role={role}
              onRowClick={(id) => setSelectedEmployeeId(id)}
              onDelete={handleDeleteEmployee}
            />

            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              totalElements={totalElements}
              onPageChange={(newPage) => setCurrentPage(newPage)}
            />
          </>
        )}
      </div>

      {/* Employee Details Modal */}
      {selectedEmployeeId && (
        <EmployeeModal
          employeeId={selectedEmployeeId}
          onClose={() => setSelectedEmployeeId(null)}
        />
      )}
    </div>
  );
}
