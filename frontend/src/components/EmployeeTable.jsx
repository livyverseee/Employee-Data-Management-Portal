import React from 'react';
import FilterRow from './FilterRow';

/**
 * EmployeeTable displays the tabular employee data.
 * - Renders a per-column FilterRow directly beneath the column headers.
 * - Shows Edit and Delete buttons for DEAN (with stopPropagation).
 * - Shows read-only rows with detail modal trigger for EMPLOYEE.
 */
export default function EmployeeTable({
  employees = [],
  canEdit = false,
  onRowClick,
  onEdit,
  onDelete,
  filters,
  filterOptions,
  onFilterChange,
  onClearFilters,
}) {
  const handleDeleteClick = (e, id, employeeId) => {
    e.stopPropagation(); // Prevent row click details modal from opening
    if (window.confirm(`Are you sure you want to delete employee record ${employeeId}?`)) {
      onDelete(id, employeeId);
    }
  };

  const handleEditClick = (e, emp) => {
    e.stopPropagation(); // Prevent row click details modal from opening
    onEdit(emp);
  };

  return (
    <div className="table-responsive">
      <table className="employee-table">
        <thead>
          <tr>
            <th>Employee ID</th>
            <th>Education</th>
            <th>City</th>
            <th>Age</th>
            <th>Gender</th>
            <th>Joining Year</th>
            <th>Payment Tier</th>
            <th>Status</th>
            <th style={{ minWidth: canEdit ? '140px' : '80px' }}>
              {canEdit ? 'Actions' : 'Actions'}
            </th>
          </tr>

          {/* Per-column filter row */}
          {filters && onFilterChange && (
            <FilterRow
              filters={filters}
              filterOptions={filterOptions}
              onFilterChange={onFilterChange}
              onClearFilters={onClearFilters}
            />
          )}
        </thead>

        <tbody>
          {employees.length === 0 ? (
            <tr>
              <td colSpan={9} className="empty-state-cell">
                <div className="empty-state">
                  <p>No employee records match the selected criteria.</p>
                </div>
              </td>
            </tr>
          ) : (
            employees.map((emp) => (
              <tr
                key={emp.id}
                onClick={() => onRowClick(emp.id)}
                className="employee-row"
                title="Click to view details"
              >
                <td>
                  <strong>{emp.employeeId}</strong>
                </td>
                <td>{emp.education}</td>
                <td>{emp.city}</td>
                <td>{emp.age}</td>
                <td>{emp.gender}</td>
                <td>{emp.joiningYear}</td>
                <td>
                  <span className="badge badge-tier">Tier {emp.paymentTier}</span>
                </td>
                <td>
                  <span
                    className={`badge ${
                      emp.leaveOrNot === 1 ? 'badge-left' : 'badge-active'
                    }`}
                  >
                    {emp.leaveOrNot === 1 ? 'Left' : 'Active'}
                  </span>
                </td>
                <td>
                  {canEdit ? (
                    <div className="table-action-btns">
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={(e) => handleEditClick(e, emp)}
                        title="Edit employee record"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger-outline btn-sm"
                        onClick={(e) => handleDeleteClick(e, emp.id, emp.employeeId)}
                        title="Delete employee record"
                      >
                        Delete
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        onRowClick(emp.id);
                      }}
                      title="View details"
                    >
                      View
                    </button>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
