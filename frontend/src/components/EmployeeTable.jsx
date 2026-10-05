import React from 'react';

/**
 * EmployeeTable displays the tabular employee data.
 * - Columns: Employee ID, Education, City, Age, Gender, Joining Year, Action (DEAN only).
 * - Clicking a table row triggers onRowClick(employeeId).
 * - Clicking the Delete button prompts confirmation, stops event propagation, and triggers onDelete(employeeId).
 */
export default function EmployeeTable({
  employees,
  role,
  onRowClick,
  onDelete,
}) {
  const isDean = role === 'DEAN';

  const handleDeleteClick = (e, employeeId) => {
    // Critical requirement: stop propagation so the row-click details modal does NOT open
    e.stopPropagation();

    if (window.confirm(`Delete record for employee ${employeeId}?`)) {
      onDelete(employeeId);
    }
  };

  if (!employees || employees.length === 0) {
    return (
      <div className="empty-state">
        <p>No records found</p>
      </div>
    );
  }

  return (
    <div className="table-responsive">
      <table>
        <thead>
          <tr>
            <th>Employee ID</th>
            <th>Education</th>
            <th>City</th>
            <th>Age</th>
            <th>Gender</th>
            <th>Joining Year</th>
            {isDean && <th>Action</th>}
          </tr>
        </thead>
        <tbody>
          {employees.map((emp) => (
            <tr
              key={emp.employeeId}
              onClick={() => onRowClick(emp.employeeId)}
              title="Click to view full employee details"
            >
              <td>
                <strong>{emp.employeeId}</strong>
              </td>
              <td>{emp.education}</td>
              <td>{emp.city}</td>
              <td>{emp.age}</td>
              <td>{emp.gender}</td>
              <td>{emp.joiningYear}</td>
              {isDean && (
                <td>
                  <button
                    type="button"
                    className="btn btn-danger-outline"
                    onClick={(e) => handleDeleteClick(e, emp.employeeId)}
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
