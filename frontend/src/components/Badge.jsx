import React from "react";

export function StatusBadge({ status }) {
  return <span className={`badge badge-status-${status}`}>{status.replace("_", " ")}</span>;
}

export function PriorityBadge({ priority }) {
  return <span className={`badge badge-priority-${priority}`}>{priority}</span>;
}

export function SentimentBadge({ sentiment }) {
  return <span className={`badge badge-sentiment-${sentiment}`}>{sentiment}</span>;
}

export function AnalyzedByBadge({ analyzedBy }) {
  if (!analyzedBy) return null;
  const isMock = analyzedBy.toLowerCase().includes("mock");
  return (
    <span className={`badge ${isMock ? "badge-mock" : "badge-ai"}`}>
      {isMock ? "MOCK analysis" : `AI (${analyzedBy})`}
    </span>
  );
}
