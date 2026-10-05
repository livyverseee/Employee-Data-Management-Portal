import React, { useState, useEffect } from 'react';
import { updateEmployeeApi } from '../services/api';

/**
 * Modal dialog for editing an existing employee (DEAN only).
 * - Client-side validation matching backend constraints.
 * - Handles 409 Conflict if employeeId duplicates another record in active dataset.
 * - Supports Escape key, Cancel, Backdrop click.
 */
export default function EmployeeEditModal({ employee, onClose, onSaveSuccess }) {
  const [formData, setFormData] = useState({
    employeeId: employee.employeeId || '',
    education: employee.education || 'Bachelors',
    joiningYear: employee.joiningYear || 2020,
    city: employee.city || 'Bangalore',
    paymentTier: employee.paymentTier || 3,
    age: employee.age || 25,
    gender: employee.gender || 'Male',
    everBenched: employee.everBenched || 'No',
    experienceInCurrentDomain: employee.experienceInCurrentDomain ?? 2,
    leaveOrNot: employee.leaveOrNot ?? 0,
  });

  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);

  // Close modal on Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    let parsedValue = value;
    if (type === 'number') {
      parsedValue = value === '' ? '' : Number(value);
    }
    setFormData((prev) => ({
      ...prev,
      [name]: parsedValue,
    }));
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.employeeId.trim()) {
      newErrors.employeeId = 'Employee ID is required';
    }
    if (!formData.education.trim()) {
      newErrors.education = 'Education is required';
    }
    if (!formData.city.trim()) {
      newErrors.city = 'City is required';
    }
    if (!formData.joiningYear || formData.joiningYear < 1900 || formData.joiningYear > 2100) {
      newErrors.joiningYear = 'Joining year must be between 1900 and 2100';
    }
    if (!formData.age || formData.age < 18 || formData.age > 100) {
      newErrors.age = 'Age must be between 18 and 100';
    }
    if (![1, 2, 3].includes(Number(formData.paymentTier))) {
      newErrors.paymentTier = 'Payment Tier must be 1, 2, or 3';
    }
    if (formData.experienceInCurrentDomain < 0 || formData.experienceInCurrentDomain > 50) {
      newErrors.experienceInCurrentDomain = 'Experience must be between 0 and 50';
    }
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError(null);

    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    setErrors({});
    setSaving(true);

    try {
      const payload = {
        employeeId: formData.employeeId.trim(),
        education: formData.education.trim(),
        joiningYear: Number(formData.joiningYear),
        city: formData.city.trim(),
        paymentTier: Number(formData.paymentTier),
        age: Number(formData.age),
        gender: formData.gender,
        everBenched: formData.everBenched,
        experienceInCurrentDomain: Number(formData.experienceInCurrentDomain),
        leaveOrNot: Number(formData.leaveOrNot),
      };

      const res = await updateEmployeeApi(employee.id, payload);
      onSaveSuccess(res.data);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to update employee';
      setServerError(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div className="modal-content modal-edit-content" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h3>Edit Employee &mdash; {employee.employeeId}</h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close dialog"
            disabled={saving}
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {serverError && <div className="alert alert-error">{serverError}</div>}

            <div className="modal-form-grid">
              <div className="form-group">
                <label htmlFor="edit-emp-id">Employee ID</label>
                <input
                  id="edit-emp-id"
                  name="employeeId"
                  type="text"
                  value={formData.employeeId}
                  onChange={handleChange}
                  disabled={saving}
                  className={errors.employeeId ? 'input-error' : ''}
                />
                {errors.employeeId && <span className="form-error-text">{errors.employeeId}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="edit-education">Education</label>
                <select
                  id="edit-education"
                  name="education"
                  value={formData.education}
                  onChange={handleChange}
                  disabled={saving}
                >
                  <option value="Bachelors">Bachelors</option>
                  <option value="Masters">Masters</option>
                  <option value="PHD">PHD</option>
                </select>
                {errors.education && <span className="form-error-text">{errors.education}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="edit-city">City</label>
                <input
                  id="edit-city"
                  name="city"
                  type="text"
                  value={formData.city}
                  onChange={handleChange}
                  disabled={saving}
                  className={errors.city ? 'input-error' : ''}
                />
                {errors.city && <span className="form-error-text">{errors.city}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="edit-joining-year">Joining Year</label>
                <input
                  id="edit-joining-year"
                  name="joiningYear"
                  type="number"
                  value={formData.joiningYear}
                  onChange={handleChange}
                  disabled={saving}
                  className={errors.joiningYear ? 'input-error' : ''}
                />
                {errors.joiningYear && <span className="form-error-text">{errors.joiningYear}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="edit-age">Age</label>
                <input
                  id="edit-age"
                  name="age"
                  type="number"
                  value={formData.age}
                  onChange={handleChange}
                  disabled={saving}
                  className={errors.age ? 'input-error' : ''}
                />
                {errors.age && <span className="form-error-text">{errors.age}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="edit-gender">Gender</label>
                <select
                  id="edit-gender"
                  name="gender"
                  value={formData.gender}
                  onChange={handleChange}
                  disabled={saving}
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="edit-tier">Payment Tier</label>
                <select
                  id="edit-tier"
                  name="paymentTier"
                  value={formData.paymentTier}
                  onChange={handleChange}
                  disabled={saving}
                >
                  <option value={1}>Tier 1</option>
                  <option value={2}>Tier 2</option>
                  <option value={3}>Tier 3</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="edit-benched">Ever Benched</label>
                <select
                  id="edit-benched"
                  name="everBenched"
                  value={formData.everBenched}
                  onChange={handleChange}
                  disabled={saving}
                >
                  <option value="No">No</option>
                  <option value="Yes">Yes</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="edit-experience">Experience in Domain (years)</label>
                <input
                  id="edit-experience"
                  name="experienceInCurrentDomain"
                  type="number"
                  value={formData.experienceInCurrentDomain}
                  onChange={handleChange}
                  disabled={saving}
                  className={errors.experienceInCurrentDomain ? 'input-error' : ''}
                />
                {errors.experienceInCurrentDomain && (
                  <span className="form-error-text">{errors.experienceInCurrentDomain}</span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="edit-leave">Employment Status</label>
                <select
                  id="edit-leave"
                  name="leaveOrNot"
                  value={formData.leaveOrNot}
                  onChange={handleChange}
                  disabled={saving}
                >
                  <option value={0}>Active (Still Working)</option>
                  <option value={1}>Left Company</option>
                </select>
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={saving}
            >
              {saving ? 'Saving Changes...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
