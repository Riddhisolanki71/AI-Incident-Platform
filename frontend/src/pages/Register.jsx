import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Register() {
  const { registerUser } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [details, setDetails] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setDetails([]);
    setSubmitting(true);
    try {
      // New self-registrations are always CUSTOMER accounts (enforced by the backend).
      const user = await registerUser(form);
      navigate(user.role === "CUSTOMER" ? "/dashboard" : "/support");
    } catch (err) {
      setError(err.message || "Registration failed");
      setDetails(err.details || []);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1 style={{ fontSize: "1.3rem", marginTop: 0 }}>Create your account</h1>
        {error && (
          <div className="error-banner">
            {error}
            {details.length > 0 && (
              <ul style={{ margin: "0.4rem 0 0 1rem", padding: 0 }}>
                {details.map((d) => (
                  <li key={d}>{d}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Full name</label>
            <input
              className="form-control"
              name="name"
              value={form.name}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input
              className="form-control"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>
          <div className="form-group">
            <label>Password (min 8 characters)</label>
            <input
              className="form-control"
              type="password"
              name="password"
              minLength={8}
              value={form.password}
              onChange={handleChange}
              required
            />
          </div>
          <button className="btn btn-primary btn-block" type="submit" disabled={submitting}>
            {submitting ? "Creating account..." : "Register"}
          </button>
        </form>
        <p style={{ fontSize: "0.875rem", marginTop: "1rem" }}>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
