import React from "react";

export function Tabs({ value, onChange, items, compact }) {
  return (
    <nav className={compact ? "tabs compact-tabs" : "tabs"}>
      {items.map(([key, label]) => (
        <button key={key} type="button" className={value === key ? "active" : ""} onClick={() => onChange(key)}>
          {label}
        </button>
      ))}
    </nav>
  );
}

export function Panel({ title, eyebrow, actions, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-head">
        <div>
          {eyebrow ? <p className="panel-eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
        </div>
        {actions ? <div className="panel-actions">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

export function Field({ label, value, onChange, type = "text", ...inputProps }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} {...inputProps} />
    </label>
  );
}

export function SelectField({ label, value, onChange, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {children}
      </select>
    </label>
  );
}

export function Summary({ label, value }) {
  return (
    <div className="summary-item">
      <span>{label}</span>
      <strong>{value || "-"}</strong>
    </div>
  );
}

export function StatusChip({ value }) {
  const normalized = String(value || "").toLowerCase().replaceAll("_", "-");
  return <span className={`status-chip ${normalized}`}>{String(value || "UNKNOWN").replaceAll("_", " ")}</span>;
}

export function EmptyState({ title, text, visual }) {
  return (
    <div className="empty-state">
      {visual === "car" ? (
        <div className="empty-visual" aria-hidden="true">
          <span className="sparkle one">✦</span>
          <span className="car-icon">🚘</span>
          <span className="sparkle two">✦</span>
        </div>
      ) : null}
      <strong>{title}</strong>
      {text ? <span>{text}</span> : null}
    </div>
  );
}

export function DataTable({ rows, columns, onSelect }) {
  if (!rows?.length) return <EmptyState title="No records" />;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map(([key, label]) => <th key={key}>{label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id || row.userId || row.bookingId || row.paymentId || index} onClick={() => onSelect?.(row)}>
              {columns.map(([key, , render]) => <td key={key}>{render ? render(row) : String(row[key] ?? "-")}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
