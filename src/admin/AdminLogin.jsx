
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    LockKeyhole,
    UserRound,
    Eye,
    EyeOff
} from "lucide-react";
import "./Admin.css";

const API_URL = "http://localhost:8080/api/admin/auth/login";

export default function AdminLogin() {
    const navigate = useNavigate();

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const response = await fetch(API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || "Login failed");
            }

            sessionStorage.setItem("adminToken", data.token);
            sessionStorage.setItem("adminUsername", data.username);

            navigate("/admin/dashboard");

        } catch (err) {
            setError(err.message || "Unable to connect to server");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="admin-login-page">
            <div className="admin-login-card">

                <div className="admin-brand">
                    <h1>NMK <span>OPTICALS</span></h1>
                    <p>ADMINISTRATION PORTAL</p>
                </div>

                <div className="admin-login-heading">
                    <h2>Welcome Back</h2>
                    <p>Sign in to manage your optical store.</p>
                </div>

                <form onSubmit={handleLogin}>

                    <label>Username</label>

                    <div className="admin-input-group">
                        <UserRound size={18} />
                        <input
                            type="text"
                            placeholder="Enter username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>

                    <label>Password</label>

                    <div className="admin-input-group">
                        <LockKeyhole size={18} />
                        <input
                            type={showPassword ? "text" : "password"}
                            placeholder="Enter password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />

                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowPassword(!showPassword)}
                        >
                            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>

                    {error && (
                        <p className="admin-error">{error}</p>
                    )}

                    <button
                        type="submit"
                        className="admin-login-button"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Sign In"}
                    </button>

                </form>

                <p className="admin-login-footer">
                    NMK OPTICALS · STORE MANAGEMENT
                </p>

            </div>
        </div>
    );
}
