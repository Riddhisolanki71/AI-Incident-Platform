import React, { useEffect, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import * as api from "../api/api";
import { useAuth } from "../context/AuthContext";
import { StatusBadge, PriorityBadge, SentimentBadge, AnalyzedByBadge } from "../components/Badge";

const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

export default function TicketDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const isStaff = user?.role === "SUPPORT_AGENT" || user?.role === "ADMIN";

  const [ticket, setTicket] = useState(null);
  const [agents, setAgents] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [noteText, setNoteText] = useState("");
  const [savingNote, setSavingNote] = useState(false);

  const loadTicket = useCallback(async () => {
    try {
      const data = await api.getTicket(id);
      setTicket(data);
    } catch (err) {
      setError(err.message);
    }
  }, [id]);

  useEffect(() => {
    setLoading(true);
    const tasks = [loadTicket()];
    if (isStaff) tasks.push(api.getAgents().then(setAgents));
    Promise.all(tasks).finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleStatusChange(e) {
    try {
      const updated = await api.updateTicketStatus(id, e.target.value);
      setTicket(updated);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAssign(e) {
    const agentId = e.target.value;
    if (!agentId) return;
    try {
      const updated = await api.assignTicket(id, Number(agentId));
      setTicket(updated);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleAddNote(e) {
    e.preventDefault();
    if (!noteText.trim()) return;
    setSavingNote(true);
    try {
      const updated = await api.addTicketNote(id, noteText);
      setTicket(updated);
      setNoteText("");
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingNote(false);
    }
  }

  if (loading) return <div className="main-content loading-state">Loading ticket...</div>;
  if (error && !ticket) return <div className="main-content error-banner">{error}</div>;
  if (!ticket) return null;

  const analysis = ticket.analysis;

  return (
    <div className="main-content">
      <div className="page-header">
        <div>
          <Link to={isStaff ? "/support" : "/dashboard"}>&larr; Back</Link>
          <h1>{ticket.title}</h1>
        </div>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <StatusBadge status={ticket.status} />
          <PriorityBadge priority={ticket.priority} />
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="ticket-grid">
        <div>
          <div className="card" style={{ marginBottom: "1.25rem" }}>
            <h2 className="section-title">Description</h2>
            <p style={{ whiteSpace: "pre-wrap" }}>{ticket.description}</p>
          </div>

          <div className="card" style={{ marginBottom: "1.25rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2 className="section-title">AI Analysis</h2>
              <AnalyzedByBadge analyzedBy={analysis?.analyzedBy} />
            </div>
            {analysis ? (
              <>
                <div className="detail-row">
                  <span className="label">AI-suggested category</span>
                  <span>{analysis.aiCategory}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Severity</span>
                  <span>{analysis.severity}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Sentiment</span>
                  <span><SentimentBadge sentiment={analysis.sentiment} /></span>
                </div>
                <div className="detail-row">
                  <span className="label">Emotional intensity</span>
                  <span>{analysis.emotionalIntensity} / 10</span>
                </div>
                <div className="detail-row">
                  <span className="label">AI-suggested priority</span>
                  <span>{analysis.aiPriority}</span>
                </div>
                <div style={{ marginTop: "0.85rem" }}>
                  <div className="label" style={{ fontSize: "0.8rem", marginBottom: "0.25rem" }}>Summary</div>
                  <p style={{ margin: 0 }}>{analysis.summary}</p>
                </div>
                <div style={{ marginTop: "0.85rem" }}>
                  <div className="label" style={{ fontSize: "0.8rem", marginBottom: "0.25rem" }}>
                    Suggested resolution
                  </div>
                  <p style={{ margin: 0 }}>{analysis.suggestedResolution}</p>
                </div>
              </>
            ) : (
              <p className="empty-state">No analysis available for this ticket.</p>
            )}
          </div>

          {isStaff && (
            <div className="card">
              <h2 className="section-title">Internal Notes</h2>
              {ticket.notes && ticket.notes.length > 0 ? (
                ticket.notes.map((n) => (
                  <div className="note-item" key={n.id}>
                    <div className="note-meta">
                      {n.authorName} &middot; {new Date(n.createdAt).toLocaleString()}
                    </div>
                    <div>{n.content}</div>
                  </div>
                ))
              ) : (
                <p className="empty-state" style={{ padding: "0.5rem 0" }}>No internal notes yet.</p>
              )}
              <form onSubmit={handleAddNote} style={{ marginTop: "0.75rem" }}>
                <textarea
                  className="form-control"
                  placeholder="Add an internal note (not visible to the customer)..."
                  value={noteText}
                  onChange={(e) => setNoteText(e.target.value)}
                  style={{ minHeight: 70 }}
                />
                <button className="btn btn-primary" style={{ marginTop: "0.5rem" }} disabled={savingNote}>
                  {savingNote ? "Saving..." : "Add note"}
                </button>
              </form>
            </div>
          )}
        </div>

        <div>
          <div className="card" style={{ marginBottom: "1.25rem" }}>
            <h2 className="section-title">Ticket Info</h2>
            <div className="detail-row">
              <span className="label">Customer</span>
              <span>{ticket.customer?.name}</span>
            </div>
            <div className="detail-row">
              <span className="label">Category</span>
              <span>{ticket.category}</span>
            </div>
            <div className="detail-row">
              <span className="label">Assigned agent</span>
              <span>{ticket.assignedAgent?.name || "Unassigned"}</span>
            </div>
            <div className="detail-row">
              <span className="label">Created</span>
              <span>{new Date(ticket.createdAt).toLocaleString()}</span>
            </div>
            <div className="detail-row">
              <span className="label">Updated</span>
              <span>{new Date(ticket.updatedAt).toLocaleString()}</span>
            </div>
          </div>

          {isStaff && (
            <div className="card">
              <h2 className="section-title">Manage Ticket</h2>
              <div className="form-group">
                <label>Status</label>
                <select className="form-control" value={ticket.status} onChange={handleStatusChange}>
                  {STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Assign to agent</label>
                <select className="form-control" defaultValue="" onChange={handleAssign}>
                  <option value="">Select an agent...</option>
                  {agents.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name} ({a.role})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
