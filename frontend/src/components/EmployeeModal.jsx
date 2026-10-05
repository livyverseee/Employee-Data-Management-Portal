import React, { useEffect, useState } from 'react';
import { getEmployeeByIdApi } from '../services/api';

/**
 * Details modal popup for an individual employee.
 * - Fetches GET /api/employees/{id} on open.
 * - Displays all employee fields with friendly labels.
 * - Supports closing via X button, Close button, backdrop click, or Escape key.
 */
export default function EmployeeModal({ employeeId, onClose }) {
  const [employee, setEmployee] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Close modal when user presses Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Fetch full employee details by ID
  useEffect(() => {
    if (!employeeId) return;

    let isMounted = true;
    setLoading(true);
    setError(null);

    getEmployeeByIdApi(employeeId)
      .then((res) => {
        if (isMounted) {
          setEmployee(res.data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          const msg = err.response?.data?.message || err.message || 'Failed to fetch employee details';
          setError(msg);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [employeeId]);

  // Backdrop click handler (closes modal when clicking outside dialog content)
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div className="modal-content" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h3>Employee Details &mdash; {employeeId}</h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close dialog"
          >
            &times;
          </button>
        </div>

        <div className="modal-body">
          {loading && (
            <div className="loading-indicator">
              <span>Loading employee record...</span>
            </div>
          )}

          {error && <div className="alert alert-error">{error}</div>}

          {!loading && !error && employee && (
            <div className="modal-grid">
              <div className="modal-field-item">
                <div className="modal-field-label">Employee ID</div>
                <div className="modal-field-value">{employee.employeeId}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Education</div>
                <div className="modal-field-value">{employee.education}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">City</div>
                <div className="modal-field-value">{employee.city}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Age</div>
                <div className="modal-field-value">{employee.age} years</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Gender</div>
                <div className="modal-field-value">{employee.gender}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Joining Year</div>
                <div className="modal-field-value">{employee.joiningYear}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Payment Tier</div>
                <div className="modal-field-value">
                  <span className="badge badge-tier">Tier {employee.paymentTier}</span>
                </div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Ever Benched</div>
                <div className="modal-field-value">{employee.everBenched}</div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Experience In Domain</div>
                <div className="modal-field-value">
                  {employee.experienceInCurrentDomain} {employee.experienceInCurrentDomain === 1 ? 'year' : 'years'}
                </div>
              </div>

              <div className="modal-field-item">
                <div className="modal-field-label">Employment Status</div>
                <div className="modal-field-value">
                  <span className={`badge ${employee.leaveOrNot === 1 ? 'badge-left' : 'badge-active'}`}>
                    Left company: {employee.leaveOrNot === 1 ? 'Yes' : 'No'}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
