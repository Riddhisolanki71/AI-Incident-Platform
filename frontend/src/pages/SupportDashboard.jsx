import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import * as api from "../api/api";
import StatsCards from "../components/StatsCards";
import { StatusBadge, PriorityBadge } from "../components/Badge";

const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];
const PRIORITIES = ["LOW", "MEDIUM", "HIGH"];
const CATEGORIES = ["PAYMENT", "ACCOUNT", "TECHNICAL", "DELIVERY", "PRODUCT", "OTHER"];

export default function SupportDashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [filters, setFilters] = useState({ status: "", priority: "", category: "", search: "" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadTickets = useCallback(async () => {
    try {
      const data = await api.searchTickets(filters);
      setTickets(data);
    } catch (err) {
      setError(err.message);
    }
  }, [filters]);

  // Stats are loaded once - they don't depend on the ticket filters.
  useEffect(() => {
    api.getStats().then(setStats).catch((err) => setError(err.message));
  }, []);

  // Tickets are (re)loaded whenever the filters change, including the initial
  // mount. Previously there was a *second* effect that also called
  // loadTickets() on mount, so the first page load fired /api/tickets twice -
  // a real duplicate-request bug. This single effect now covers both cases.
  useEffect(() => {
    setLoading(true);
    loadTickets().finally(() => setLoading(false));
  }, [loadTickets]);

  function handleFilterChange(e) {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  }

  return (
    <div className="main-content">
      <div className="page-header">
        <h1>Support Dashboard</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <StatsCards stats={stats} />

      <div className="filters-bar">
        <select name="status" value={filters.status} onChange={handleFilterChange}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select name="priority" value={filters.priority} onChange={handleFilterChange}>
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
        <select name="category" value={filters.category} onChange={handleFilterChange}>
          <option value="">All categories</option>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <input
          name="search"
          placeholder="Search title or description..."
          value={filters.search}
          onChange={handleFilterChange}
        />
      </div>

      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div className="loading-state">Loading tickets...</div>
        ) : tickets.length === 0 ? (
          <div className="empty-state">No tickets match these filters.</div>
        ) : (
          <table className="ticket-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Customer</th>
                <th>Category</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Assigned</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.id} onClick={() => navigate(`/tickets/${t.id}`)}>
                  <td>{t.title}</td>
                  <td>{t.customer?.name}</td>
                  <td>{t.category}</td>
                  <td>
                    <StatusBadge status={t.status} />
                  </td>
                  <td>
                    <PriorityBadge priority={t.priority} />
                  </td>
                  <td>{t.assignedAgent?.name || "-"}</td>
                  <td>{new Date(t.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
