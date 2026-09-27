const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api";

function getToken() {
  return localStorage.getItem("token");
}

async function request(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };

  if (auth) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  // Some endpoints (logout) may return 204 with no body. A non-JSON body (e.g.
  // an HTML error page from a proxy in front of the API) shouldn't crash the
  // app with an uncaught JSON.parse error - fall back to null and let the
  // generic "Request failed with status ..." message below take over.
  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    // A 401 means the token is missing/invalid/expired. Clear the stale
    // session immediately so the next render sees a logged-out state instead
    // of hanging onto a token that will just keep failing - previously the
    // stored token/user were left in place until the person manually logged
    // out, even though every request was already failing.
    if (response.status === 401 && auth) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      // localStorage changes alone don't re-render React state, so the app
      // would otherwise keep showing a "logged in" UI (with every subsequent
      // request failing) until a manual refresh. AuthContext listens for this
      // and clears its in-memory user immediately.
      window.dispatchEvent(new Event("auth:session-expired"));
    }

    const message = data?.message || data?.error || `Request failed with status ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.details = data?.details;
    throw error;
  }

  return data;
}

// ---- Auth ----
export const register = (payload) => request("/auth/register", { method: "POST", body: payload, auth: false });
export const login = (payload) => request("/auth/login", { method: "POST", body: payload, auth: false });
export const logout = () => request("/auth/logout", { method: "POST" });

// ---- Tickets ----
export const createTicket = (payload) => request("/tickets", { method: "POST", body: payload });
export const getMyTickets = () => request("/tickets/mine");
export const getTicket = (id) => request(`/tickets/${id}`);
export const searchTickets = (params = {}) => {
  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== "")
  ).toString();
  return request(`/tickets${query ? `?${query}` : ""}`);
};
export const updateTicketStatus = (id, status) =>
  request(`/tickets/${id}/status`, { method: "PATCH", body: { status } });
export const assignTicket = (id, agentId) =>
  request(`/tickets/${id}/assign`, { method: "PATCH", body: { agentId } });
export const addTicketNote = (id, content) =>
  request(`/tickets/${id}/notes`, { method: "POST", body: { content } });

// ---- Users ----
export const getAgents = () => request("/users/agents");

// ---- Stats ----
export const getStats = () => request("/stats");
