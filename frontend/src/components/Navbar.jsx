import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Navbar() {
  const { user, logoutUser } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logoutUser();
    navigate("/login");
  }

  const homeLink = user?.role === "CUSTOMER" ? "/dashboard" : "/support";

  return (
    <div className="navbar">
      <Link to={user ? homeLink : "/login"} className="navbar-brand">
        AI Incident &amp; Support Platform
      </Link>
      <div className="navbar-links">
        {user ? (
          <>
            <span className="navbar-user">
              {user.name} &middot; {user.role}
            </span>
            <button className="btn btn-secondary" onClick={handleLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        )}
      </div>
    </div>
  );
}
