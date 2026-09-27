import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as api from "../api/api";
import { StatusBadge, PriorityBadge } from "../components/Badge";

export default function CustomerDashboard() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .getMyTickets()
      .then(setTickets)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="main-content">
      <div className="page-header">
        <h1>My Tickets</h1>
        <Link to="/tickets/new" className="btn btn-primary">
          + New Ticket
        </Link>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div className="loading-state">Loading your tickets...</div>
        ) : tickets.length === 0 ? (
          <div className="empty-state">
            You haven't submitted any tickets yet. <Link to="/tickets/new">Create one</Link>.
          </div>
        ) : (
          <table className="ticket-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <TicketRow key={t.id} ticket={t} />
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function TicketRow({ ticket }) {
  const navigate = useNavigate();
  return (
    <tr onClick={() => navigate(`/tickets/${ticket.id}`)}>
      <td>{ticket.title}</td>
      <td>{ticket.category}</td>
      <td>
        <StatusBadge status={ticket.status} />
      </td>
      <td>
        <PriorityBadge priority={ticket.priority} />
      </td>
      <td>{new Date(ticket.createdAt).toLocaleString()}</td>
    </tr>
  );
}
