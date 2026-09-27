import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import * as api from "../api/api";

const CATEGORIES = ["PAYMENT", "ACCOUNT", "TECHNICAL", "DELIVERY", "PRODUCT", "OTHER"];

export default function CreateTicket() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", category: "OTHER" });
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
      const ticket = await api.createTicket(form);
      navigate(`/tickets/${ticket.id}`);
    } catch (err) {
      setError(err.message || "Could not create ticket");
      setDetails(err.details || []);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="main-content" style={{ maxWidth: 640 }}>
      <div className="page-header">
        <h1>Submit a support ticket</h1>
      </div>
      <div className="card">
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
            <label>Title</label>
            <input
              className="form-control"
              name="title"
              maxLength={200}
              value={form.title}
              onChange={handleChange}
              placeholder="Short summary of the issue"
              required
            />
          </div>
          <div className="form-group">
            <label>Category</label>
            <select className="form-control" name="category" value={form.category} onChange={handleChange}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              className="form-control"
              name="description"
              minLength={10}
              maxLength={4000}
              value={form.description}
              onChange={handleChange}
              placeholder="Describe what happened in as much detail as possible..."
              required
            />
          </div>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? "Submitting & analyzing..." : "Submit ticket"}
          </button>
          <p className="mock-notice">
            After you submit, the ticket is automatically analyzed (category, severity, sentiment,
            priority, and a suggested resolution) before you're taken to the ticket page.
          </p>
        </form>
      </div>
    </div>
  );
}
