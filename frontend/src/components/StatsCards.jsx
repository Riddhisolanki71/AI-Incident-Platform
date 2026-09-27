import React from "react";

export default function StatsCards({ stats }) {
  if (!stats) return null;

  const items = [
    { label: "Total Tickets", value: stats.totalTickets },
    { label: "Open", value: stats.openTickets },
    { label: "In Progress", value: stats.inProgressTickets },
    { label: "Resolved", value: stats.resolvedTickets },
    { label: "Closed", value: stats.closedTickets },
    { label: "High Priority", value: stats.highPriorityTickets },
  ];

  return (
    <div className="stats-grid">
      {items.map((item) => (
        <div className="card stat-card" key={item.label}>
          <div className="stat-value">{item.value}</div>
          <div className="stat-label">{item.label}</div>
        </div>
      ))}
    </div>
  );
}
