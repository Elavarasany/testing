
import { lazy, Suspense } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useLocation,
} from "react-router-dom";
import CustomerWebsite from "./CustomerWebsite";

// Admin pages load on demand.
const AdminLogin = lazy(() => import("./admin/AdminLogin"));
const AdminDashboard = lazy(() => import("./admin/AdminDashboard"));

const TOKEN_KEY = "adminToken";
const LOGIN_PATH = "/admin";
const DASHBOARD_PATH = "/admin/dashboard";

const isAdminAuthenticated = () =>
  Boolean(sessionStorage.getItem(TOKEN_KEY));

function ProtectedAdminRoute({ children }) {
  const location = useLocation();

  if (!isAdminAuthenticated()) {
    return (
      <Navigate
        to={LOGIN_PATH}
        replace
        state={{ from: location.pathname + location.search }}
      />
    );
  }

  return children;
}

function GuestOnlyRoute({ children }) {
  return isAdminAuthenticated()
    ? <Navigate to={DASHBOARD_PATH} replace />
    : children;
}

function PageLoader() {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
      }}
    >
      Loading…
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Customer storefront */}
          <Route path="/" element={<CustomerWebsite />} />

          {/* Public customer order tracking */}
          <Route path="/track/:token" element={<CustomerWebsite />} />

          {/* Admin login */}
          <Route
            path={LOGIN_PATH}
            element={
              <GuestOnlyRoute>
                <AdminLogin />
              </GuestOnlyRoute>
            }
          />

          {/* Protected admin dashboard */}
          <Route
            path={DASHBOARD_PATH}
            element={
              <ProtectedAdminRoute>
                <AdminDashboard />
              </ProtectedAdminRoute>
            }
          />

          {/* Unknown routes */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
