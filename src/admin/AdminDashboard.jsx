import { createContext, useCallback, useContext, useEffect, useId, useMemo, useRef, useState } from "react";
import {
  LayoutDashboard,
  Package,
  ShoppingCart,
  CalendarDays,
  Users,
  Megaphone,
  Settings,
  LogOut,
  Menu,
  TrendingUp,
  AlertTriangle,
  Plus,
  Search,
  Pencil,
  Trash2,
  X,
  RefreshCw,
  Download,
  Upload,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import "./Admin.css";

/* =========================
   CONSTANTS
========================= */

const API_BASE_URL = "http://localhost:8080/api";

const ENDPOINTS = {
  products: `${API_BASE_URL}/admin/products`,
  orders: `${API_BASE_URL}/admin/orders`,
  customers: `${API_BASE_URL}/admin/orders/customers`,
  appointments: `${API_BASE_URL}/appointments`,
  banners: `${API_BASE_URL}/admin/marketing/banners`,
  offers: `${API_BASE_URL}/admin/marketing/offers`,
  websiteImages: `${API_BASE_URL}/admin/website-images`,
};

const LIST_SOURCES = {
  products: { url: ENDPOINTS.products, message: "Unable to load products" },
  orders: { url: ENDPOINTS.orders, message: "Unable to load orders" },
  appointments: { url: ENDPOINTS.appointments, message: "Unable to load appointments" },
  customers: { url: ENDPOINTS.customers, message: "Unable to load customers" },
  images: { url: ENDPOINTS.websiteImages, message: "Failed to load website images" },
};

const CLOUDINARY_CLOUD_NAME = "ydkweahp";
const CLOUDINARY_UPLOAD_PRESET = "nmk_opticals";
const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const IMAGE_ACCEPT = "image/png,image/jpeg,image/webp";
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const NOTICE_DURATION_MS = 3000;
const JSON_HEADERS = { "Content-Type": "application/json" };
const NETWORK_ERROR = "Network error. Check your connection and try again.";
const TIME_ZONE = "Asia/Kolkata";
const EMPTY_RANGE = { start: "", end: "" };
const PREFS_KEY = "nmk_admin_prefs";
const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

const MARKETING_MODAL = {
  overlayClass: "modal-overlay",
  panelClass: "modal-content",
  headerClass: "modal-header",
};

const MENU_ITEMS = [
  { name: "Dashboard", icon: LayoutDashboard },
  { name: "Products", icon: Package },
  { name: "Orders", icon: ShoppingCart },
  { name: "Appointments", icon: CalendarDays },
  { name: "Customers", icon: Users },
  { name: "Marketing", icon: Megaphone },
  { name: "Settings", icon: Settings },
];

const ORDER_STATUSES = [
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "SHIPPED", label: "Shipped" },
  { value: "OUT_FOR_DELIVERY", label: "Out for Delivery" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
];

const APPOINTMENT_STATUSES = [
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
];

const BANNER_PLACEMENTS = [
  { value: "HOME", label: "Home page (standard)" },
  { value: "HOME_HERO", label: "Home page (hero area)" },
];

const WEBSITE_IMAGE_MODES = [
  { mode: "url", label: "Paste URL" },
  { mode: "file", label: "Choose file" },
];

const PRODUCT_CATEGORIES = [
  "Eyeglasses",
  "Sunglasses",
  "Power Sunglasses",
  "Computer Glasses",
  "Contact Lenses",
  "Accessories",
];
const PRODUCT_GENDERS = ["Men", "Women", "Unisex", "Kids"];

const PRODUCT_SPECIFICATIONS = {
  Eyeglasses: [
    {
      key: "brand",
      label: "Brand",
      type: "select-custom",
      options: [
        "Ray-Ban", "Ogle", "Carrera", "Jaguar", "Puma",
        "Titan", "Fastrack", "Canzo", "Ollo", "Para", "Solo",
      ],
    },
    {
      key: "shape",
      label: "Frame Shape",
      type: "select-custom",
      options: [
        "Rectangle", "Square", "Oval", "Aviator",
        "Cat Eye", "Geometric", "Round",
      ],
    },
    {
      key: "frameStyle",
      label: "Frame Style",
      type: "select-custom",
      options: ["Full Rim", "Semi-Rimmed", "Rimless"],
    },
    {
      key: "size",
      label: "Size",
      type: "select-custom",
      options: ["XS", "S", "M", "L"],
    },
    {
      key: "frameColor",
      label: "Frame Color",
      type: "text",
    },
    {
      key: "material",
      label: "Frame Material",
      type: "text",
    },
  ],

  Sunglasses: [
    {
      key: "brand",
      label: "Brand",
      type: "select-custom",
      options: ["Ray-Ban", "Ogle", "Titan", "Fastrack"],
    },
    {
      key: "shape",
      label: "Frame Shape",
      type: "select-custom",
      options: [
        "Aviator", "Butterfly", "Phantos", "Pillow", "Sporty",
        "Teardrop", "Bugeye", "CatEye", "Clubmaster",
        "Geometric", "Navigator", "Oval", "Rectangle",
        "Round", "Square", "Wayfarer", "Wraparound",
        "Hexagon", "Irregular",
      ],
    },
    {
      key: "lensTechnology",
      label: "Lens Technology",
      type: "select-custom",
      options: [
        "Mirrored", "Polarized", "Gradient",
        "Transition", "UV-Protected",
      ],
    },
    {
      key: "size",
      label: "Size",
      type: "select-custom",
      options: ["XS", "S", "M", "L"],
    },
    {
      key: "frameStyle",
      label: "Frame Style",
      type: "select-custom",
      options: ["Full Rim", "Semi-Rimmed", "Rimless"],
    },
    {
      key: "frameColor",
      label: "Frame Color",
      type: "text",
    },
    {
      key: "material",
      label: "Frame Material",
      type: "text",
    },
    {
      key: "lensColor",
      label: "Lens Color",
      type: "text",
    },
  ],

  "Power Sunglasses": [
    {
      key: "brand",
      label: "Brand",
      type: "select-custom",
      options: ["Ray-Ban", "Ogle", "Titan", "Fastrack"],
    },
    {
      key: "shape",
      label: "Frame Shape",
      type: "select-custom",
      options: [
        "Aviator", "Butterfly", "Phantos", "Pillow", "Sporty",
        "Teardrop", "Bugeye", "CatEye", "Clubmaster",
        "Geometric", "Navigator", "Oval", "Rectangle",
        "Round", "Square", "Wayfarer", "Wraparound",
        "Hexagon", "Irregular",
      ],
    },
    {
      key: "lensTechnology",
      label: "Lens Technology",
      type: "select-custom",
      options: [
        "Mirrored", "Polarized", "Gradient",
        "Transition", "UV-Protected",
      ],
    },
    {
      key: "size",
      label: "Size",
      type: "select-custom",
      options: ["XS", "S", "M", "L"],
    },
    {
      key: "frameStyle",
      label: "Frame Style",
      type: "select-custom",
      options: ["Full Rim", "Semi-Rimmed", "Rimless"],
    },
    {
      key: "frameColor",
      label: "Frame Color",
      type: "text",
    },
    {
      key: "material",
      label: "Frame Material",
      type: "text",
    },
    {
      key: "lensColor",
      label: "Lens Color",
      type: "text",
    },
  ],

  "Computer Glasses": [
    {
      key: "brand",
      label: "Brand",
      type: "select-custom",
      options: [
        "Ray-Ban", "Ogle", "Carrera", "Jaguar", "Puma",
        "Titan", "Fastrack", "Canzo", "Ollo", "Para", "Solo",
      ],
    },
    {
      key: "shape",
      label: "Frame Shape",
      type: "select-custom",
      options: [
        "Rectangle", "Square", "Oval", "Aviator",
        "Cat Eye", "Geometric", "Round",
      ],
    },
    {
      key: "size",
      label: "Size",
      type: "select-custom",
      options: ["XS", "S", "M", "L"],
    },
    {
      key: "frameStyle",
      label: "Frame Style",
      type: "select-custom",
      options: ["Full Rim", "Semi-Rimmed", "Rimless"],
    },
    {
      key: "frameColor",
      label: "Frame Color",
      type: "text",
    },
    {
      key: "material",
      label: "Frame Material",
      type: "text",
    },
  ],

  "Contact Lenses": [
    {
      key: "brand",
      label: "Brand",
      type: "select-custom",
      options: ["Bausch + Lomb", "CooperVision", "Johnson & Johnson"],
    },
    {
      key: "disposability",
      label: "Disposability",
      type: "select-custom",
      options: ["Daily", "Fortnightly", "Monthly", "Yearly", "Quarterly"],
    },
    {
      key: "lensType",
      label: "Lens Type",
      type: "select-custom",
      options: ["Non Toric", "Toric", "Multifocal", "Colour"],
    },
    {
      key: "lensColor",
      label: "Lens Color",
      type: "text",
    },
  ],

  Accessories: [
    {
      key: "productType",
      label: "Product Type",
      type: "text",
    },
    {
      key: "brand",
      label: "Brand",
      type: "text",
    },
  ],
};
const PRODUCT_SPECIFICATION_CSV_KEYS = [
  ...new Set(
    Object.values(PRODUCT_SPECIFICATIONS)
      .flat()
      .map((spec) => spec.key)
  ),
];


// Multiple values in one cell (lens features, for example) are separated by this
const CSV_LIST_SEPARATOR = "; ";

const PRODUCT_CSV_REQUIRED = ["category", "gender", "price", "stockQuantity"];

const EMPTY_PRODUCT = {
  gender: "",
  category: "",
  description: "",
  price: "",
  stockQuantity: "",
  imageUrl: "",
  active: true,
  specifications: {},
};

const EMPTY_BANNER = {
  title: "",
  imageUrl: "",
  targetUrl: "",
  placement: "HOME",
  displayOrder: 0,
  active: true,
};

const EMPTY_OFFER = {
  message: "",
  backgroundColor: "#172554",
  textColor: "#FFFFFF",
  targetUrl: "",
  startDateTime: "",
  endDateTime: "",
  active: true,
};

// Admin preferences (stored in this browser)
const SORT_OPTIONS = [
  { value: "name", label: "Name" },
  { value: "category", label: "Category" },
  { value: "price", label: "Price" },
  { value: "stock", label: "Stock" },
  { value: "status", label: "Status" },
];

const DASHBOARD_ROW_OPTIONS = [3, 5, 10];

const DEFAULT_PREFS = {
  lowStockThreshold: 5,
  defaultProductSort: "name",
  dashboardRows: 5,
  confirmProductDelete: true,
};

/* =========================
   PREFERENCES
========================= */

const PrefsContext = createContext(DEFAULT_PREFS);
const usePrefs = () => useContext(PrefsContext);

// Keeps only valid values, so a corrupted saved setting can never break a page
const sanitizePrefs = (raw) => {
  const source = raw && typeof raw === "object" ? raw : {};
  const threshold = Number(source.lowStockThreshold);
  const rows = Number(source.dashboardRows);

  return {
    lowStockThreshold:
      Number.isInteger(threshold) && threshold >= 1 && threshold <= 500
        ? threshold
        : DEFAULT_PREFS.lowStockThreshold,
    defaultProductSort: SORT_OPTIONS.some((o) => o.value === source.defaultProductSort)
      ? source.defaultProductSort
      : DEFAULT_PREFS.defaultProductSort,
    dashboardRows: DASHBOARD_ROW_OPTIONS.includes(rows) ? rows : DEFAULT_PREFS.dashboardRows,
    confirmProductDelete:
      typeof source.confirmProductDelete === "boolean"
        ? source.confirmProductDelete
        : DEFAULT_PREFS.confirmProductDelete,
  };
};

const loadPrefs = () => {
  try {
    return sanitizePrefs(JSON.parse(localStorage.getItem(PREFS_KEY)));
  } catch {
    return sanitizePrefs(null);
  }
};

/* =========================
   FORMATTERS
========================= */

const formatINR = (amount, withDecimals = false) =>
  `₹${Number(amount || 0).toLocaleString(
    "en-IN",
    withDecimals ? { minimumFractionDigits: 2, maximumFractionDigits: 2 } : undefined
  )}`;

const formatDate = (value) =>
  value ? new Date(value).toLocaleDateString("en-IN", { timeZone: TIME_ZONE }) : "-";

const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString("en-IN", { timeZone: TIME_ZONE }) : "Date unavailable";

const formatIstDay = (date) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);

// Calendar day in India ("YYYY-MM-DD") for today
const todayInIndia = () => formatIstDay(new Date());

// Calendar day in India for a timestamp, or the value itself when it is already YYYY-MM-DD
const toIndiaDay = (value) => {
  if (!value) return "";
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return text;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? text.slice(0, 10) : formatIstDay(date);
};

const inDateRange = (value, start, end) => {
  const day = toIndiaDay(value);
  return (!start || day >= start) && (!end || day <= end);
};

// Case-insensitive match of the search term against any of the given fields
// Products no longer have a typed name: the title is the brand
const getProductTitle = (product) =>
  (product?.specifications?.brand || product?.specifications?.productType || product?.name || product?.category || "").toString().trim();

const matchesTerm = (term, ...fields) => !term || fields.join(" ").toLowerCase().includes(term);

/* =========================
   VALIDATION
========================= */

const isValidHttpUrl = (value) => {
  try {
    return ["http:", "https:"].includes(new URL(value).protocol);
  } catch {
    return false;
  }
};

// Links may be a full URL or an in-site path such as /products
const isValidLink = (value) =>
  !value || (value.startsWith("/") && !value.startsWith("//")) || isValidHttpUrl(value);

const LINK_ERROR = "Destination must be a full URL (https://…) or a page path (/products).";

const isWholeNumber = (value) => value !== "" && Number.isInteger(Number(value)) && Number(value) >= 0;

const isLowStock = (product, threshold = DEFAULT_PREFS.lowStockThreshold) =>
  Number(product.stockQuantity) <= threshold;

const customerKey = (customer) => customer.email || customer.phone || customer.customerName || "";

const getImageFileError = (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) return "Only JPG, PNG and WEBP images are allowed.";
  if (file.size > MAX_IMAGE_SIZE) return "Image size must be less than 5 MB.";
  return null;
};

const validateProduct = (form) => {
  if (!form.gender.trim() || !form.category.trim()) {
    return "Gender and category are required.";
  }
  if (form.price === "" || !Number.isFinite(Number(form.price)) || Number(form.price) < 0) {
    return "Enter a valid product price.";
  }
  if (!isWholeNumber(form.stockQuantity)) return "Enter a valid stock quantity.";
  if (form.imageUrl && !isValidHttpUrl(form.imageUrl.trim())) return "Enter a valid product image URL.";
  return null;
};

const validateBanner = (form) => {
  if (!form.title.trim() || !form.imageUrl.trim()) return "Banner title and image are required.";
  if (!isValidHttpUrl(form.imageUrl.trim())) return "Enter a valid image URL.";
  if (!isValidLink(form.targetUrl.trim())) return LINK_ERROR;
  if (!isWholeNumber(form.displayOrder)) return "Display order must be a whole number, 0 or higher.";
  return null;
};

const validateOffer = (form) => {
  if (!form.message.trim()) return "Offer message is required.";
  if (!isValidLink(form.targetUrl.trim())) return LINK_ERROR;
  if (form.startDateTime && form.endDateTime && new Date(form.endDateTime) <= new Date(form.startDateTime)) {
    return "End date and time must be after the start.";
  }
  return null;
};

const validatePrefs = (prefs) => {
  const threshold = Number(prefs.lowStockThreshold);
  if (!Number.isInteger(threshold) || threshold < 1 || threshold > 500) {
    return "Low stock threshold must be a whole number between 1 and 500.";
  }
  return null;
};

const buildProductPayload = (data) => {
  // Keep only real answers: no empty values, no "Other" placeholder, no gender duplicate
  const specifications = Object.fromEntries(
    Object.entries(data.specifications || {})
      .map(([key, value]) => [key, typeof value === "string" ? value.trim() : value])
      .filter(
        ([key, value]) =>
          key !== "gender" &&
          value !== "__custom__" &&
          value !== "" &&
          !(Array.isArray(value) && value.length === 0)
      )
  );
  const gender = data.gender || data.specifications?.gender || "";

  return {
    // The API still stores a name, so it is filled from the brand (not typed by the admin)
    name: String(specifications.brand || specifications.productType || data.name || data.category || "").trim(),
    gender,
    category: (data.category || "").trim(),
    description: (data.description || "").trim(),
    price: Number(data.price),
    stockQuantity: Number(data.stockQuantity),
    imageUrl: (data.imageUrl || "").trim(),
    active: data.active,
    specifications,
  };
};

const getPreviewSrc = (edit, fallback) => {
  if (edit.mode === "file" && edit.previewUrl) return edit.previewUrl;
  if (edit.mode === "url" && isValidHttpUrl(edit.url.trim())) return edit.url.trim();
  return fallback;
};

const sortValue = (product, key) => {
  switch (key) {
    case "price":
      return Number(product.price ?? 0);
    case "stock":
      return Number(product.stockQuantity ?? 0);
    case "status":
      return product.active ? 1 : 0;
    case "category":
      return product.category ?? "";
    default:
      return getProductTitle(product);
  }
};

/* =========================
   CSV EXPORT
========================= */

// Cells starting with = + - @ are prefixed, so Excel cannot run them as formulas
const escapeCsv = (value) => {
  let text = value == null ? "" : String(value);
  if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
  return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
};

// Builds a CSV file in the browser and downloads it. Returns false when there is no data.
function downloadCsv(filename, rows) {
  if (!rows.length) return false;

  const headers = Object.keys(rows[0]);
  const lines = [headers.join(","), ...rows.map((row) => headers.map((h) => escapeCsv(row[h])).join(","))];

  // BOM so Excel reads UTF-8 (₹, accents) correctly
  const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Revoke later, so browsers that start the download asynchronously still get the file
  setTimeout(() => URL.revokeObjectURL(url), 1000);

  return true;
}

/* =========================
   CSV IMPORT
========================= */

// Parse CSV while supporting quoted values, commas and newlines
function parseCsv(text) {
  const rows = [];
  let row = [];
  let value = "";
  let quoted = false;

  text = text.replace(/^\uFEFF/, "");

  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    const next = text[i + 1];

    if (char === '"' && quoted && next === '"') {
      value += '"';
      i++;
    } else if (char === '"') {
      quoted = !quoted;
    } else if (char === "," && !quoted) {
      row.push(value.trim());
      value = "";
    } else if ((char === "\n" || char === "\r") && !quoted) {
      if (char === "\r" && next === "\n") i++;

      row.push(value.trim());

      if (row.some((cell) => cell !== "")) {
        rows.push(row);
      }

      row = [];
      value = "";
    } else {
      value += char;
    }
  }

  row.push(value.trim());

  if (row.some((cell) => cell !== "")) {
    rows.push(row);
  }

  if (rows.length < 2) {
    throw new Error("CSV must contain headers and at least one product.");
  }

  const headers = rows[0].map((header) => header.trim());

  return rows.slice(1).map((cells) =>
    Object.fromEntries(
      headers.map((header, index) => [header, cells[index] ?? ""])
    )
  );
}


const EXPORT_TARGETS = [
  {
    source: "products",
    label: "Export products",
    filename: "products.csv",
    
map: (p) => {
  const row = {
    id: p.id,
    name: p.name ?? "",
    category: p.category ?? "",
    gender: p.gender ?? p.specifications?.gender ?? "",
    price: p.price ?? "",
    stockQuantity: p.stockQuantity ?? "",
    active: p.active ? "Yes" : "No",
    imageUrl: p.imageUrl ?? "",
    description: p.description ?? "",
  };

  PRODUCT_SPECIFICATION_CSV_KEYS.forEach((key) => {
    const value = p.specifications?.[key];
    row[key] = Array.isArray(value) ? value.join(CSV_LIST_SEPARATOR) : value ?? "";
  });

  return row;
},

  },
  {
    source: "orders",
    label: "Export orders",
    filename: "orders.csv",
    map: (o) => ({
      id: o.id,
      orderDate: o.orderDate,
      customerName: o.customerName,
      phone: o.phone,
      email: o.email,
      city: o.city,
      state: o.state,
      pincode: o.pincode,
      totalAmount: o.totalAmount,
      paymentStatus: o.paymentStatus,
      orderStatus: o.orderStatus,
    }),
  },
  {
    source: "appointments",
    label: "Export appointments",
    filename: "appointments.csv",
    map: (a) => ({
      id: a.id,
      customerName: a.customerName,
      phone: a.phone,
      email: a.email,
      appointmentDate: a.appointmentDate,
      appointmentTime: a.appointmentTime,
      status: a.status,
    }),
  },
];

/* =========================
   API HELPERS
========================= */

// Uses the server's message when it has one, otherwise the fallback
async function readErrorMessage(response, fallback) {
  try {
    const text = await response.text();
    if (!text) return fallback;
    try {
      const json = JSON.parse(text);
      return json.message || json.error || fallback;
    } catch {
      return text.length <= 200 ? text : fallback;
    }
  } catch {
    return fallback;
  }
}

async function uploadToCloudinary(file, folder) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);
  formData.append("folder", folder);

  let response;
  try {
    response = await fetch(`https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`, {
      method: "POST",
      body: formData,
    });
  } catch {
    throw new Error(NETWORK_ERROR);
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error?.message || "Image upload failed.");
  return data.secure_url;
}

const saveWebsiteImage = (api, imageKey, title, imageUrl, fallbackMessage) =>
  api.saveRecord({
    url: `${ENDPOINTS.websiteImages}/${imageKey}`,
    method: "PUT",
    payload: { title, imageUrl },
    fallbackMessage,
  });

// Calls "<endpoint>/clear" for every endpoint and reports what happened to each one,
// so a partial failure is shown instead of hidden
async function clearEndpoints(api, endpoints, fallbackMessage) {
  const results = await Promise.allSettled(
    endpoints.map((url) => api.deleteRecord(`${url}/clear`, fallbackMessage))
  );

  return {
    sessionEnded: results.some((r) => r.status === "fulfilled" && r.value === false),
    failures: results.filter((r) => r.status === "rejected").map((r) => r.reason?.message || fallbackMessage),
  };
}

/* =========================
   API HOOK
   Auth header, expired sessions, and error handling in one place.
========================= */

function useAdminApi(token, onSessionEnded) {
  return useMemo(() => {
    // Returns null (after ending the session) on 401/403.
    // endSessionOnAuthError: false is used where 401/403 means "wrong input",
    // such as a wrong password, rather than "expired session".
    const authFetch = async (url, options = {}, { endSessionOnAuthError = true } = {}) => {
      let response;
      try {
        response = await fetch(url, {
          ...options,
          headers: { ...(options.headers || {}), Authorization: `Bearer ${token}` },
        });
      } catch {
        throw new Error(NETWORK_ERROR);
      }

      if (endSessionOnAuthError && (response.status === 401 || response.status === 403)) {
        onSessionEnded();
        return null;
      }

      return response;
    };

    // true = saved, false = session ended, throws on failure
    const saveRecord = async ({ url, method, payload, fallbackMessage }) => {
      const response = await authFetch(url, { method, headers: JSON_HEADERS, body: JSON.stringify(payload) });

      if (!response) return false;
      if (!response.ok) throw new Error(await readErrorMessage(response, fallbackMessage));
      return true;
    };

    // true = deleted, false = session ended, throws on failure
    const deleteRecord = async (url, fallbackMessage) => {
      const response = await authFetch(url, { method: "DELETE" });

      if (!response) return false;
      if (!response.ok) throw new Error(await readErrorMessage(response, fallbackMessage));
      return true;
    };

    // Parsed JSON, or null when the session ended
    const getJson = async (url, fallbackMessage) => {
      const response = await authFetch(url);

      if (!response) return null;
      if (!response.ok) throw new Error(fallbackMessage);
      return response.json();
    };

    const verifyPassword = async (password) => {
      const response = await authFetch(
        `${API_BASE_URL}/admin/auth/verify-password`,
        { method: "POST", headers: JSON_HEADERS, body: JSON.stringify({ password }) },
        { endSessionOnAuthError: false }
      );

      if (!response.ok) throw new Error(await readErrorMessage(response, "Incorrect admin password."));
      return true;
    };

    return { saveRecord, deleteRecord, getJson, verifyPassword };
  }, [token, onSessionEnded]);
}

/* =========================
   DIALOG BEHAVIOUR
   Escape to close, Tab stays inside the dialog, focus moves in and returns on close.
========================= */

function useDialog(ref, { onClose, closable = true, lockScroll = true }) {
  const onCloseRef = useRef(onClose);
  const closableRef = useRef(closable);

  useEffect(() => {
    onCloseRef.current = onClose;
    closableRef.current = closable;
  });

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    if (lockScroll) document.body.style.overflow = "hidden";
    (ref.current?.querySelector(FOCUSABLE_SELECTOR) ?? ref.current)?.focus();

    const onKey = (e) => {
      if (e.key === "Escape") {
        if (closableRef.current) onCloseRef.current();
        return;
      }

      if (e.key !== "Tab" || !ref.current) return;

      const items = [...ref.current.querySelectorAll(FOCUSABLE_SELECTOR)];
      if (items.length === 0) return;

      const first = items[0];
      const last = items[items.length - 1];

      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKey);

    return () => {
      if (lockScroll) document.body.style.overflow = "";
      document.removeEventListener("keydown", onKey);
      previouslyFocused?.focus?.();
    };
  }, [ref, lockScroll]);
}

/* =========================
   SHARED HOOKS
========================= */

// Confirmation popup shared by the whole admin. Resolves true (Yes) or false (No / Escape).
const ConfirmContext = createContext(() => Promise.resolve(false));
const useConfirm = () => useContext(ConfirmContext);

/*
  Two-step clear flow used by Dashboard, Orders and Appointments:
    idle -> confirm (warning) -> password -> delete
*/
function useBulkClear({ api, onNotice, onReload }) {
  const [step, setStep] = useState("idle"); // "idle" | "confirm" | "password"
  const [clearing, setClearing] = useState(false);
  const [error, setError] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [pendingClear, setPendingClear] = useState(null);

  const reset = () => {
    setStep("idle");
    setAdminPassword("");
    setPendingClear(null);
    setError("");
  };

  // Start the flow with the page's own endpoints and messages
  const clear = (options) => {
    setError("");
    setAdminPassword("");
    setPendingClear(options);
    setStep("confirm");
  };

  const cancel = () => {
    if (!clearing) reset();
  };

  const confirmPassword = async () => {
    if (!adminPassword.trim() || !pendingClear || clearing) return;

    setClearing(true);
    setError("");

    try {
      await api.verifyPassword(adminPassword);

      const { sessionEnded, failures } = await clearEndpoints(
        api,
        pendingClear.endpoints,
        pendingClear.fallbackMessage
      );

      if (sessionEnded) {
        setError("Session ended. Please log in again.");
        return;
      }

      // Refresh even after a partial failure, so the screen matches the server
      pendingClear.onCleared?.();
      await onReload();

      if (failures.length) {
        setError(`${failures[0]} Some data may not have been cleared. Please try again.`);
        return;
      }

      onNotice(pendingClear.successMessage);
      reset();
    } catch (err) {
      setError(err.message || pendingClear.fallbackMessage);
    } finally {
      setClearing(false);
    }
  };

  return {
    step,
    clearing,
    error,
    warning: pendingClear?.warning ?? "",
    clear,
    confirmYes: () => setStep("password"),
    cancel,
    adminPassword,
    setAdminPassword,
    confirmPassword,
  };
}

function useStatusUpdater({ api, baseUrl, label, onNotice, onReload }) {
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");

  const update = async (id, status) => {
    setError("");
    setBusyId(id);

    try {
      const saved = await api.saveRecord({
        url: `${baseUrl}/${id}/status`,
        method: "PUT",
        payload: { status },
        fallbackMessage: `Unable to update ${label.toLowerCase()} status`,
      });

      if (saved) {
        onNotice(`${label} #${id} updated.`);
        await onReload();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  };

  return { busyId, error, update };
}

/* =========================
   SHARED UI
========================= */

function Modal({
  title,
  subtitle,
  onClose,
  closable = true,
  overlayClass = "admin-modal-overlay",
  panelClass = "admin-product-modal",
  headerClass = "admin-modal-heading",
  children,
}) {
  const titleId = useId();
  const panelRef = useRef(null);
  useDialog(panelRef, { onClose, closable });

  return (
    <div className={overlayClass}>
      <div
        ref={panelRef}
        className={panelClass}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <div className={headerClass}>
          <div>
            <h2 id={titleId}>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>

          <button
            type="button"
            className="admin-modal-close"
            onClick={onClose}
            disabled={!closable}
            aria-label="Close dialog"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </div>

        {children}
      </div>
    </div>
  );
}

// Popup anchored to the top of the screen, used for every confirmation in the admin
function TopPopup({ title, onCancel, busy = false, children }) {
  const titleId = useId();
  const panelRef = useRef(null);
  useDialog(panelRef, { onClose: onCancel, closable: !busy, lockScroll: false });

  return (
    <div
      className="admin-top-popup-overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div
        ref={panelRef}
        className="admin-top-popup"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <h3 id={titleId}>{title}</h3>
        {children}
      </div>
    </div>
  );
}

// Yes / No popup driven by askConfirm() from the root component
function ConfirmPopup({ options, onResult }) {
  if (!options) return null;

  return (
    <TopPopup title={options.title} onCancel={() => onResult(false)}>
      <p className="admin-top-popup-text">{options.message}</p>

      <div className="admin-top-popup-actions">
        <button type="button" className="admin-secondary-button" onClick={() => onResult(false)}>
          {options.cancelLabel || "Cancel"}
        </button>

        <button
          type="button"
          className={options.danger ? "admin-clear-button" : "admin-primary-button"}
          onClick={() => onResult(true)}
        >
          {options.confirmLabel || "Confirm"}
        </button>
      </div>
    </TopPopup>
  );
}

// Both steps of the clear flow: warning first, then the admin password
function ClearDataPopup({ bulk }) {
  if (bulk.step === "confirm") {
    return (
      <TopPopup title="Permanent Delete" onCancel={bulk.cancel}>
        <p className="admin-top-popup-text">
          <strong>Warning:</strong> {bulk.warning} Do you want to continue?
        </p>

        <div className="admin-top-popup-actions">
          <button type="button" className="admin-secondary-button" onClick={bulk.cancel}>
            No, cancel
          </button>

          <button type="button" className="admin-clear-button" onClick={bulk.confirmYes}>
            Yes, continue
          </button>
        </div>
      </TopPopup>
    );
  }

  if (bulk.step === "password") {
    return (
      <TopPopup title="Enter Admin Password" onCancel={bulk.cancel} busy={bulk.clearing}>
        <p className="admin-top-popup-text">Enter your admin password to permanently delete the data.</p>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            bulk.confirmPassword();
          }}
        >
          <input
            className="admin-top-popup-input"
            type="password"
            value={bulk.adminPassword}
            onChange={(e) => bulk.setAdminPassword(e.target.value)}
            placeholder="Enter admin password"
            autoComplete="current-password"
            required
            disabled={bulk.clearing}
          />

          <ErrorMessage>{bulk.error}</ErrorMessage>

          <div className="admin-top-popup-actions">
            <button type="button" className="admin-secondary-button" onClick={bulk.cancel} disabled={bulk.clearing}>
              Cancel
            </button>

            <button
              type="submit"
              className="admin-clear-button"
              disabled={bulk.clearing || !bulk.adminPassword.trim()}
            >
              {bulk.clearing ? "Deleting..." : "Confirm Delete"}
            </button>
          </div>
        </form>
      </TopPopup>
    );
  }

  return null;
}

function StatusBadge({ active }) {
  return <span className={`admin-status ${active ? "available" : "inactive"}`}>{active ? "Active" : "Inactive"}</span>;
}

function StatCard({ label, value, hint, icon: Icon, tone }) {
  return (
    <div className="admin-stat-card">
      {Icon && (
        <div className={`admin-stat-icon ${tone || ""}`} aria-hidden="true">
          <Icon size={23} />
        </div>
      )}
      <p>{label}</p>
      <h2>{value}</h2>
      {hint && <span>{hint}</span>}
    </div>
  );
}

function ErrorMessage({ children }) {
  if (!children) return null;
  return (
    <div className="admin-error-message" role="alert">
      {children}
    </div>
  );
}

function EmptyRow({ colSpan, children }) {
  return (
    <tr>
      <td colSpan={colSpan} className="admin-empty">
        {children}
      </td>
    </tr>
  );
}

function PageHeading({ title, description, action }) {
  return (
    <div className="admin-page-heading">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}

function RefreshButton({ onClick, loading, label = "Refresh" }) {
  return (
    <button type="button" className="admin-secondary-button" onClick={onClick} disabled={loading}>
      <RefreshCw size={16} aria-hidden="true" />
      {loading ? "Loading..." : label}
    </button>
  );
}

function ClearButton({ label, onClick, busy }) {
  return (
    <button type="button" className="admin-clear-button" onClick={onClick} disabled={busy}>
      <Trash2 size={16} aria-hidden="true" />
      {busy ? "Deleting..." : label}
    </button>
  );
}

function SearchBox({ label, placeholder, value, onChange }) {
  return (
    <div className="admin-search-box">
      <Search size={19} aria-hidden="true" />
      <input
        type="search"
        placeholder={placeholder}
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}

// Start and end date inputs that keep each other in range
function DateRangeFilter({ start, end, onChange }) {
  return (
    <div className="admin-date-filter">
      <label>
        Start Date
        <input type="date" value={start} max={end || undefined} onChange={(e) => onChange({ start: e.target.value, end })} />
      </label>

      <label>
        End Date
        <input type="date" value={end} min={start || undefined} onChange={(e) => onChange({ start, end: e.target.value })} />
      </label>
    </div>
  );
}

// Sortable column header. Uses a real button, so it works with the keyboard.
function SortableTh({ label, sortKey, sort, onSort }) {
  const active = sort.key === sortKey;
  const arrow = active ? (sort.direction === "asc" ? "↑" : "↓") : "";

  return (
    <th aria-sort={active ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}>
      <button type="button" className="admin-sort-button" onClick={() => onSort(sortKey)}>
        {label} <span aria-hidden="true">{arrow}</span>
      </button>
    </th>
  );
}

function FormGroup({ id, label, required, full, children }) {
  return (
    <div className={`admin-form-group${full ? " admin-form-full" : ""}`}>
      <label htmlFor={id}>
        {label}
        {required && " *"}
      </label>
      {children}
    </div>
  );
}

// Shared change handler for forms whose fields are named after their state keys
const handleFieldChange = (setForm) => (e) => {
  const { name, value, type, checked } = e.target;
  setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
};

/* =========================
   DASHBOARD
========================= */

function DashboardPage({ api, products, orders, appointments, refreshing, onRefresh, onViewProducts, onNotice }) {
  const { lowStockThreshold, dashboardRows } = usePrefs();
  const [range, setRange] = useState(EMPTY_RANGE);
  const bulk = useBulkClear({ api, onNotice, onReload: onRefresh });

  const stats = useMemo(() => {
    const today = todayInIndia();
    const filteredOrders = orders.filter((o) => inDateRange(o.orderDate, range.start, range.end));
    const filteredAppointments = appointments.filter((a) => inDateRange(a.appointmentDate, range.start, range.end));
    const bookable = filteredAppointments.filter((a) => a.status !== "CANCELLED");
    const paid = filteredOrders.filter((o) => o.paymentStatus === "PAID");

    return {
      totalAppointments: filteredAppointments.length,
      todayAppointments: bookable.filter((a) => a.appointmentDate === today).length,
      upcomingAppointments: bookable.filter((a) => a.appointmentDate > today).length,
      pastAppointments: bookable.filter((a) => a.appointmentDate < today).length,
      totalProducts: products.length,
      lowStock: products.filter((p) => isLowStock(p, lowStockThreshold)).length,
      totalOrders: filteredOrders.length,
      paidOrders: paid.length,
      revenue: paid.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0),
    };
  }, [products, orders, appointments, range, lowStockThreshold]);

  const cards = [
    { label: "Total Appointments", value: stats.totalAppointments, hint: "Customer eye checkup bookings", icon: CalendarDays, tone: "gold" },
    { label: "Today's Appointments", value: stats.todayAppointments, hint: "Scheduled for today" },
    { label: "Upcoming Appointments", value: stats.upcomingAppointments, hint: "Future bookings" },
    { label: "Past Appointments", value: stats.pastAppointments, hint: "Previous bookings" },
    { label: "Total Products", value: stats.totalProducts, hint: "Products in catalogue", icon: Package, tone: "blue" },
    { label: "Total Orders", value: stats.totalOrders, hint: `${stats.paidOrders} paid orders`, icon: ShoppingCart, tone: "gold" },
    { label: "Total Revenue", value: formatINR(stats.revenue), hint: "Successfully paid orders", icon: TrendingUp, tone: "green" },
    { label: "Low Stock", value: stats.lowStock, hint: `${lowStockThreshold} or fewer units`, icon: AlertTriangle, tone: "red" },
  ];

  // Dashboard figures come from orders and appointments, so both are cleared
  const handleClear = () =>
    bulk.clear({
      endpoints: [ENDPOINTS.orders, ENDPOINTS.appointments],
      warning:
        "This will permanently delete ALL orders, their order items, and ALL appointments. Products will not be affected. This cannot be undone.",
      successMessage: "Dashboard, orders and appointments data permanently deleted.",
      fallbackMessage: "Unable to clear dashboard data.",
      onCleared: () => setRange(EMPTY_RANGE),
    });

  return (
    <section className="admin-content">
      <PageHeading
        title="Dashboard Overview"
        description="Here's what's happening at your store."
        action={
          <div className="admin-dashboard-actions">
            <RefreshButton onClick={onRefresh} loading={refreshing} />
            <ClearButton label="Clear Dashboard" onClick={handleClear} busy={bulk.clearing} />
          </div>
        }
      />

      <DateRangeFilter start={range.start} end={range.end} onChange={setRange} />

      <div className="admin-stats-grid">
        {cards.map((card) => (
          <StatCard key={card.label} {...card} />
        ))}
      </div>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <h3>Product Overview</h3>
          <button type="button" className="admin-text-button" onClick={onViewProducts}>
            View all
          </button>
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th scope="col">Product</th>
                <th scope="col">Category</th>
                <th scope="col">Price</th>
                <th scope="col">Stock</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              {products.slice(0, dashboardRows).map((product) => (
                <tr key={product.id}>
                  <td>{getProductTitle(product)}</td>
                  <td>{product.category}</td>
                  <td>{formatINR(product.price)}</td>
                  <td className={isLowStock(product, lowStockThreshold) ? "admin-stock-low" : ""}>
                    {product.stockQuantity}
                  </td>
                  <td>
                    <StatusBadge active={product.active} />
                  </td>
                </tr>
              ))}

              {products.length === 0 && <EmptyRow colSpan={5}>No products to display.</EmptyRow>}
            </tbody>
          </table>
        </div>
      </div>

      <ClearDataPopup bulk={bulk} />
    </section>
  );
}

/* =========================
   PRODUCTS
========================= */


// Dropdown with an "Other" option. Custom mode is kept in state, so an empty custom
// text box stays open while the admin types.
function CustomSelectField({ field, id, value, onChange }) {
  const current = value ?? "";
  const isPredefined = field.options.includes(current);
  const [customMode, setCustomMode] = useState(!isPredefined && current !== "");
  const selectValue = customMode ? "__custom__" : isPredefined ? current : "";

  return (
    <div className="admin-form-group">
      <label htmlFor={id}>{field.label}</label>

      <select
        id={id}
        value={selectValue}
        onChange={(e) => {
          const next = e.target.value;
          if (next === "__custom__") {
            setCustomMode(true);
            onChange("");
          } else {
            setCustomMode(false);
            onChange(next);
          }
        }}
      >
        <option value="">Select {field.label}</option>
        {field.options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
        <option value="__custom__">Other (enter custom value)</option>
      </select>

      {customMode && (
        <input
          type="text"
          value={current}
          onChange={(e) => onChange(e.target.value)}
          placeholder={`Enter custom ${field.label.toLowerCase()}`}
        />
      )}
    </div>
  );
}

// Admin-facing explanation of the product CSV columns, shown above the products table
function CsvColumnsGuide() {
  const columns = [
    { name: "id", note: "Leave blank to add a new product. Enter an existing product ID to update that product." },
    { name: "name", note: "Optional. The product title is taken from the brand specification; this is used only when there is no brand." },
    { name: "category", note: "Required. Must match a category exactly, for example Eyeglasses or Contact Lenses." },
    { name: "gender", note: "Required. Men, Women, Unisex or Kids." },
    { name: "price", note: "Required. Number in rupees, for example 1299." },
    { name: "stockQuantity", note: "Required. Whole number, 0 or more." },
    { name: "active", note: "Yes or No. Blank keeps the current value, or Yes for a new product." },
    { name: "imageUrl", note: "Optional. Full https:// link to the product image." },
    { name: "description", note: "Optional. Text may contain commas and quotes." },
    { name: "Specification columns", note: `${PRODUCT_SPECIFICATION_CSV_KEYS.join(", ")}. Only the columns for the product's category are used. Separate several values with semicolons, e.g. Mirror; Polarized.` },
  ];

  return (
    <details className="csv-guide">
      <summary>Product CSV columns and import rules</summary>

      <div className="csv-guide-body">
        <p>
          Export and import use the same columns. Export a file, edit it in Excel or Google Sheets, then import it back.
          Required columns are: {PRODUCT_CSV_REQUIRED.join(", ")}.
        </p>

        <dl className="csv-guide-list">
          {columns.map((column) => (
            <div key={column.name}>
              <dt>{column.name}</dt>
              <dd>{column.note}</dd>
            </div>
          ))}
        </dl>

        <p>
          Blank cells keep the current value when updating an existing product. Rows with errors are skipped, and each
          error shows its row number. Keep the file as CSV (UTF-8) when saving.
        </p>
      </div>
    </details>
  );
}

function SpecField({ field, value, onChange }) {
  if (field.type === "multiselect") {
    const selected = Array.isArray(value) ? value : [];

    return (
      <fieldset className="admin-form-group admin-fieldset">
        <legend className="admin-field-label">{field.label}</legend>
        <div className="admin-spec-checkboxes">
          {field.options.map((option) => (
            <label key={option} className="admin-checkbox-label">
              <input
                type="checkbox"
                checked={selected.includes(option)}
                onChange={(e) =>
                  onChange(
                    e.target.checked
                      ? [...selected, option]
                      : selected.filter((item) => item !== option)
                  )
                }
              />
              {option}
            </label>
          ))}
        </div>
      </fieldset>
    );
  }

  const id = `spec-${field.key}`;

  if (field.type === "select-custom") {
    return <CustomSelectField field={field} id={id} value={value} onChange={onChange} />;
  }

  return (
    <div className="admin-form-group">
      <label htmlFor={id}>{field.label}</label>

      {field.type === "select" ? (
        <select
          id={id}
          value={value || ""}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">Select {field.label}</option>
          {field.options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      ) : (
        <input
          id={id}
          type="text"
          value={value || ""}
          onChange={(e) => onChange(e.target.value)}
          placeholder={`Enter ${field.label.toLowerCase()}`}
        />
      )}
    </div>
  );
}


function ProductsPage({ api, products, loading, onReload, onNotice }) {
  const csvInputRef = useRef(null);
const [importingCsv, setImportingCsv] = useState(false);
  const prefs = usePrefs();
  const confirm = useConfirm();

  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [priceFilter, setPriceFilter] = useState("");
  const [stockFilter, setStockFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [sort, setSort] = useState({ key: prefs.defaultProductSort, direction: "asc" });

  const [modal, setModal] = useState(null); // null | { mode: "add" } | { mode: "edit", id }
  const [form, setForm] = useState(EMPTY_PRODUCT);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [keepDetails, setKeepDetails] = useState(false);
  const [rowBusyId, setRowBusyId] = useState(null);
  const [pageError, setPageError] = useState("");

  const categories = useMemo(
    () => [...new Set(products.map((p) => p.category).filter(Boolean))].sort(),
    [products]
  );

  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase();
    const direction = sort.direction === "asc" ? 1 : -1;

    return products
      .filter(
        (p) =>
          matchesTerm(term, getProductTitle(p), p.category) &&
          (categoryFilter === "ALL" || p.category === categoryFilter) &&
          String(p.price ?? "").includes(priceFilter.trim()) &&
          String(p.stockQuantity ?? "").includes(stockFilter.trim()) &&
          (statusFilter === "ALL" || (statusFilter === "ACTIVE") === Boolean(p.active))
      )
      .sort((a, b) => {
        const x = sortValue(a, sort.key);
        const y = sortValue(b, sort.key);
        const result =
          typeof x === "number" && typeof y === "number"
            ? x - y
            : String(x).localeCompare(String(y), undefined, { numeric: true, sensitivity: "base" });

        return result * direction;
      });
  }, [products, search, categoryFilter, priceFilter, stockFilter, statusFilter, sort]);

  const handleSort = (key) =>
    setSort((current) => ({
      key,
      direction: current.key === key && current.direction === "asc" ? "desc" : "asc",
    }));

  const closeModal = useCallback(() => {
    setModal(null);
    setFormError("");
    setKeepDetails(false);
  }, []);

  const openAdd = () => {
    setForm(EMPTY_PRODUCT);
    setFormError("");
    setKeepDetails(false);
    setModal({ mode: "add" });
  };

  const openEdit = (product) => {
    setForm({
      name: product.name ?? "",
      gender: product.gender ?? product.specifications?.gender ?? "",
      category: product.category ?? "",
      description: product.description ?? "",
      price: product.price ?? "",
      stockQuantity: product.stockQuantity ?? "",
      imageUrl: product.imageUrl ?? "",
      active: product.active ?? true,
      specifications: product.specifications || {},
    });
    setFormError("");
    setModal({ mode: "edit", id: product.id });
  };

  // Changing the category clears specifications that belong to the old category
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    const nextValue = type === "checkbox" ? checked : value;

    setForm((prev) =>
      name === "category" ? { ...prev, category: value, specifications: {} } : { ...prev, [name]: nextValue }
    );
  };

  const handleSpecChange = (key, value) =>
    setForm((prev) => ({ ...prev, specifications: { ...(prev.specifications || {}), [key]: value } }));
  const handleProductImageUpload = async (e) => {
  const file = e.target.files?.[0];
  e.target.value = "";

  if (!file) return;

  const problem = getImageFileError(file);

  if (problem) {
    setFormError(problem);
    return;
  }

  setFormError("");
  setUploadingImage(true);

  try {
    const imageUrl = await uploadToCloudinary(file, "nmk-products");

    setForm((prev) => ({
      ...prev,
      imageUrl,
    }));
  } catch (err) {
    setFormError(err.message);
  } finally {
    setUploadingImage(false);
  }
};
  const handleSubmit = async (e) => {
    e.preventDefault();

    const problem = validateProduct(form);
    if (problem) {
      setFormError(problem);
      return;
    }

    const isEdit = modal?.mode === "edit";
    setSaving(true);
    setFormError("");

    try {
      const saved = await api.saveRecord({
        url: isEdit ? `${ENDPOINTS.products}/${modal.id}` : ENDPOINTS.products,
        method: isEdit ? "PUT" : "POST",
        payload: buildProductPayload(form),
        fallbackMessage: "Unable to save product",
      });

      if (!saved) return;

      // "Keep same details": clear only the product-specific fields and stay open
      if (!isEdit && keepDetails) {
        setForm((prev) => ({ ...prev, name: "", imageUrl: "", price: "", stockQuantity: "" }));
        onNotice("Product added. Enter the next product's details.");
        await onReload();
        setTimeout(() => document.getElementById("product-gender")?.focus(), 0);
        return;
      }

      closeModal();
      onNotice(isEdit ? "Product updated." : "Product added.");
      await onReload();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

const handleProductCsvImport = async (e) => {
  const file = e.target.files?.[0];
  e.target.value = "";

  if (!file) return;

  setPageError("");
  setImportingCsv(true);

  try {
    const text = await file.text();
    const rows = parseCsv(text);

    const missingColumns = PRODUCT_CSV_REQUIRED.filter((column) => !(column in rows[0]));
    if (missingColumns.length) {
      throw new Error(`The CSV file is missing required column(s): ${missingColumns.join(", ")}.`);
    }

    let created = 0;
    let updated = 0;
    let sessionEnded = false;
    const errors = [];

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];

      try {
        const id = row.id?.trim();

        const existingProduct = id
          ? products.find((p) => String(p.id) === id)
          : null;

        if (id && !existingProduct) {
          throw new Error(`Product ID ${id} does not exist.`);
        }

        const category = row.category?.trim() ?? "";
        const categorySpecifications = PRODUCT_SPECIFICATIONS[category];

        if (!categorySpecifications) {
          throw new Error(`Invalid product category: ${category || "(empty)"}`);
        }

        // A blank cell keeps the current value when updating, so data is never erased by accident
        const cell = (name) => (row[name] ?? "").trim();
        const pick = (name, currentValue) => (cell(name) !== "" ? cell(name) : existingProduct ? String(currentValue ?? "") : "");

        const specifications = {};

        categorySpecifications.forEach((spec) => {
          const raw = cell(spec.key);

          if (raw !== "") {
            specifications[spec.key] =
              spec.type === "multiselect"
                ? raw.split(";").map((item) => item.trim()).filter(Boolean)
                : raw;
          } else if (existingProduct?.specifications?.[spec.key] !== undefined) {
            specifications[spec.key] = existingProduct.specifications[spec.key];
          }
        });

        const activeCell = cell("active").toLowerCase();

        const product = {
          name: pick("name", existingProduct?.name),
          gender: pick("gender", existingProduct?.gender),
          category,
          description: pick("description", existingProduct?.description),
          price: pick("price", existingProduct?.price),
          stockQuantity: pick("stockQuantity", existingProduct?.stockQuantity),
          imageUrl: pick("imageUrl", existingProduct?.imageUrl),
          active:
            activeCell === ""
              ? existingProduct
                ? Boolean(existingProduct.active)
                : true
              : ["yes", "true", "1"].includes(activeCell),
          specifications,
        };

        const problem = validateProduct(product);

        if (problem) {
          throw new Error(problem);
        }

        const saved = await api.saveRecord({
          url: existingProduct
            ? `${ENDPOINTS.products}/${id}`
            : ENDPOINTS.products,
          method: existingProduct ? "PUT" : "POST",
          payload: buildProductPayload(product),
          fallbackMessage: "Unable to save product",
        });

        // false means the admin session ended: stop instead of counting rows as saved
        if (!saved) {
          sessionEnded = true;
          break;
        }

        if (existingProduct) {
          updated++;
        } else {
          created++;
        }
      } catch (err) {
        errors.push(`CSV row ${i + 2}: ${err.message}`);
      }
    }

    if (sessionEnded) return;

    await onReload();

    if (errors.length) {
      setPageError(
        `Import completed: ${created} created, ${updated} updated, ${errors.length} failed.\n${errors.join("\n")}`
      );
    } else {
      onNotice(`CSV import completed: ${created} created, ${updated} updated.`);
    }
  } catch (err) {
    setPageError(err.message);
  } finally {
    setImportingCsv(false);
  }
};


  const runRowAction = async (productId, action) => {
    setPageError("");
    setRowBusyId(productId);

    try {
      await action();
    } catch (err) {
      setPageError(err.message);
    } finally {
      setRowBusyId(null);
    }
  };

  const toggleActive = (product) =>
    runRowAction(product.id, async () => {
      const saved = await api.saveRecord({
        url: `${ENDPOINTS.products}/${product.id}`,
        method: "PUT",
        payload: buildProductPayload({ ...product, active: !product.active }),
        fallbackMessage: "Unable to update product status",
      });
      if (saved) await onReload();
    });

  const deleteProduct = async (product) => {
    if (prefs.confirmProductDelete) {
      const ok = await confirm({
        title: "Delete product",
        message: `Delete "${getProductTitle(product)}"? This cannot be undone.`,
        confirmLabel: "Yes, delete",
        danger: true,
      });
      if (!ok) return;
    }

    runRowAction(product.id, async () => {
      const deleted = await api.deleteRecord(`${ENDPOINTS.products}/${product.id}`, "Unable to delete product");
      if (deleted) {
        onNotice("Product deleted.");
        await onReload();
      }
    });
  };

  const specFields = PRODUCT_SPECIFICATIONS[form.category] || [];

  return (
    <section className="admin-content">
      
<PageHeading
  title="Products Management"
  description="Manage your optical store products."
  action={
    <div className="admin-dashboard-actions">
      <input
        ref={csvInputRef}
        type="file"
        accept=".csv,text/csv"
        hidden
        onChange={handleProductCsvImport}
      />

      <button
        type="button"
        className="admin-secondary-button"
        onClick={() => csvInputRef.current?.click()}
        disabled={importingCsv}
      >
        <Upload size={18} aria-hidden="true" />
        {importingCsv ? "Importing..." : "Import CSV"}
      </button>

      <button
        type="button"
        className="admin-primary-button"
        onClick={openAdd}
      >
        <Plus size={18} aria-hidden="true" /> Add Product
      </button>
    </div>
  }
/>


      <ErrorMessage>{pageError}</ErrorMessage>

      <div className="admin-product-toolbar">
        <SearchBox
          label="Search products"
          placeholder="Search by brand or category..."
          value={search}
          onChange={setSearch}
        />
        <RefreshButton onClick={onReload} loading={loading} />
      </div>

      <CsvColumnsGuide />

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <h3>All Products</h3>
          <span aria-live="polite">{visibleProducts.length} products</span>
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table admin-products-table">
            <thead>
              <tr>
                <SortableTh label="Product" sortKey="name" sort={sort} onSort={handleSort} />
                <SortableTh label="Category" sortKey="category" sort={sort} onSort={handleSort} />
                <SortableTh label="Price" sortKey="price" sort={sort} onSort={handleSort} />
                <SortableTh label="Stock" sortKey="stock" sort={sort} onSort={handleSort} />
                <SortableTh label="Status" sortKey="status" sort={sort} onSort={handleSort} />
                <th scope="col">Actions</th>
              </tr>

              <tr className="admin-filter-row">
                <th />
                <th>
                  <select
                    aria-label="Filter by category"
                    value={categoryFilter}
                    onChange={(e) => setCategoryFilter(e.target.value)}
                  >
                    <option value="ALL">All Categories</option>
                    {categories.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </th>
                <th>
                  <input
                    type="search"
                    inputMode="decimal"
                    placeholder="Filter price"
                    aria-label="Filter by price"
                    value={priceFilter}
                    onChange={(e) => setPriceFilter(e.target.value)}
                  />
                </th>
                <th>
                  <input
                    type="search"
                    inputMode="numeric"
                    placeholder="Filter stock"
                    aria-label="Filter by stock quantity"
                    value={stockFilter}
                    onChange={(e) => setStockFilter(e.target.value)}
                  />
                </th>
                <th>
                  <select
                    aria-label="Filter by status"
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                  </select>
                </th>
                <th />
              </tr>
            </thead>

            <tbody>
              {loading && <EmptyRow colSpan={6}>Loading products...</EmptyRow>}

              {!loading &&
                visibleProducts.map((product) => {
                  const busy = rowBusyId === product.id;

                  return (
                    <tr key={product.id}>
                      <td>
                        <div className="admin-product-name">
                          {product.imageUrl ? (
                            <img src={product.imageUrl} alt="" />
                          ) : (
                            <div className="admin-product-placeholder" aria-hidden="true">
                              <Package size={21} />
                            </div>
                          )}
                          <strong>{getProductTitle(product)}</strong>
                        </div>
                      </td>

                      <td>{product.category}</td>
                      <td>{formatINR(product.price)}</td>

                      <td className={isLowStock(product, prefs.lowStockThreshold) ? "admin-stock-low" : ""}>
                        {product.stockQuantity}
                      </td>

                      <td>
                        <button
                          type="button"
                          className={`admin-status-toggle ${product.active ? "active" : "inactive"}`}
                          onClick={() => toggleActive(product)}
                          disabled={busy}
                          aria-label={`Mark ${getProductTitle(product)} as ${product.active ? "inactive" : "active"}`}
                        >
                          {product.active ? "Active" : "Inactive"}
                        </button>
                      </td>

                      <td>
                        <div className="admin-action-buttons">
                          <button
                            type="button"
                            className="admin-edit-button"
                            title="Edit product"
                            aria-label={`Edit ${getProductTitle(product)}`}
                            onClick={() => openEdit(product)}
                            disabled={busy}
                          >
                            <Pencil size={17} aria-hidden="true" />
                          </button>

                          <button
                            type="button"
                            className="admin-delete-button"
                            title="Delete product"
                            aria-label={`Delete ${getProductTitle(product)}`}
                            onClick={() => deleteProduct(product)}
                            disabled={busy}
                          >
                            <Trash2 size={17} aria-hidden="true" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}

              {!loading && visibleProducts.length === 0 && <EmptyRow colSpan={6}>No products found.</EmptyRow>}
            </tbody>
          </table>
        </div>
      </div>

      {modal && (
        <Modal
          title={modal.mode === "edit" ? "Edit Product" : "Add New Product"}
          subtitle="Enter your product information below."
          onClose={closeModal}
          closable={!saving}
        >
          <form onSubmit={handleSubmit}>
            <div className="admin-form-grid">
              <FormGroup id="product-gender" label="Gender" required>
                <select id="product-gender" name="gender" value={form.gender} onChange={handleChange} required>
                  <option value="">Select gender</option>
                  {PRODUCT_GENDERS.map((gender) => (
                    <option key={gender} value={gender}>
                      {gender}
                    </option>
                  ))}
                </select>
              </FormGroup>

              <FormGroup id="product-category" label="Category" required>
                <select id="product-category" name="category" value={form.category} onChange={handleChange} required>
                  <option value="">Select category</option>
                  {form.category && !PRODUCT_CATEGORIES.includes(form.category) && (
                    <option value={form.category}>{form.category}</option>
                  )}
                  {PRODUCT_CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </FormGroup>

              {specFields.map((field) => (
                <SpecField
                  key={field.key}
                  field={field}
                  value={form.specifications?.[field.key]}
                  onChange={(value) => handleSpecChange(field.key, value)}
                />
              ))}

              <FormGroup id="product-price" label="Price (₹)" required>
                <input
                  id="product-price"
                  type="number"
                  name="price"
                  min="0"
                  step="0.01"
                  value={form.price}
                  onChange={handleChange}
                  placeholder="Enter price"
                  required
                />
              </FormGroup>

              <FormGroup id="product-stock" label="Stock Quantity" required>
                <input
                  id="product-stock"
                  type="number"
                  name="stockQuantity"
                  min="0"
                  step="1"
                  value={form.stockQuantity}
                  onChange={handleChange}
                  placeholder="Enter stock quantity"
                  required
                />
              </FormGroup>

              <FormGroup id="product-image" label="Product Image URL" full>
  <input
    id="product-image"
    type="url"
    name="imageUrl"
    value={form.imageUrl}
    onChange={handleChange}
    placeholder="Paste image URL or choose a file"
    disabled={uploadingImage}
  />

  <label htmlFor="product-image-file">
    Or Choose Image File
  </label>

  <input
    id="product-image-file"
    type="file"
    accept={IMAGE_ACCEPT}
    onChange={handleProductImageUpload}
    disabled={saving || uploadingImage}
  />

  <small>
    {uploadingImage
      ? "Uploading image..."
      : "JPG, PNG or WEBP. Maximum 5 MB."}
  </small>

  {form.imageUrl && isValidHttpUrl(form.imageUrl) && (
    <img
      src={form.imageUrl}
      alt="Product preview"
      style={{
        width: "120px",
        height: "120px",
        objectFit: "contain",
        marginTop: "10px",
      }}
    />
  )}
</FormGroup>

              <FormGroup id="product-description" label="Description" full>
                <textarea
                  id="product-description"
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  placeholder="Enter product description"
                  rows={3}
                />
              </FormGroup>

              <div className="admin-form-group admin-form-full">
                <label className="admin-checkbox-label">
                  <input type="checkbox" name="active" checked={form.active} onChange={handleChange} />
                  Make this product visible in the store
                </label>
              </div>

              {modal.mode === "add" && (
                <div className="admin-form-group admin-form-full">
                  <label className="admin-checkbox-label">
                    <input type="checkbox" checked={keepDetails} onChange={(e) => setKeepDetails(e.target.checked)} />
                    Keep same details for the next product (only name, image, price and stock are cleared)
                  </label>
                </div>
              )}
            </div>

            <ErrorMessage>{formError}</ErrorMessage>

            <div className="admin-modal-actions">
              <button type="button" className="admin-secondary-button" onClick={closeModal} disabled={saving}>
                Cancel
              </button>

              <button
  type="submit"
  className="admin-primary-button"
  disabled={saving || uploadingImage}
>
  {uploadingImage
    ? "Uploading Image..."
    : saving
      ? "Saving..."
      : modal.mode === "edit"
        ? "Update Product"
        : "Add Product"}
</button>
            </div>
          </form>
        </Modal>
      )}
    </section>
  );
}

/* =========================
   ORDERS
   Compact list. "View details" opens the full order in a dialog.
========================= */

const ORDER_STATUS_LABELS = Object.fromEntries(ORDER_STATUSES.map((s) => [s.value, s.label]));

const orderTrackingLink = (order) =>
  order.trackingToken ? `${window.location.origin}/track/${encodeURIComponent(order.trackingToken)}` : "";

const isOrderPaid = (order) => String(order.paymentStatus || "").toUpperCase() === "PAID";

const countUnits = (order) => (order.items ?? []).reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);

// Plain-language lens summary for one order line
const lensSummary = (item) => {
  if (!item.prescriptionType) return "-";
  if (item.prescriptionType === "Frame Only") return "Frame only";

  return [item.prescriptionType, item.lensType, item.photochromic ? "Photochromic" : null]
    .filter(Boolean)
    .join(" · ");
};

function OrderStatusBadge({ status }) {
  const value = status || "PENDING";

  return (
    <span className="admin-order-status" data-status={value}>
      {ORDER_STATUS_LABELS[value] ?? value}
    </span>
  );
}

function OrderDetailsDialog({ api, order, onClose, onNotice, onReload }) {
  const { busyId, error: statusError, update } = useStatusUpdater({
    api,
    baseUrl: ENDPOINTS.orders,
    label: "Order",
    onNotice,
    onReload,
  });

  const [courierName, setCourierName] = useState(order.courierName || "");
  const [trackingNumber, setTrackingNumber] = useState(order.trackingNumber || "");
  const [deliveryError, setDeliveryError] = useState("");
  const [savingDelivery, setSavingDelivery] = useState(false);
  const [copied, setCopied] = useState(false);

  // Show the saved courier details for this order, including after a reload
  useEffect(() => {
    setCourierName(order.courierName || "");
    setTrackingNumber(order.trackingNumber || "");
    setDeliveryError("");
  }, [order.id, order.courierName, order.trackingNumber]);

  const paid = isOrderPaid(order);
  const locked = ["DELIVERED", "CANCELLED"].includes(order.orderStatus);
  const trackingUrl = orderTrackingLink(order);
  const items = order.items ?? [];
  const statusBusy = busyId === order.id;

  const saveDelivery = async (e) => {
    e.preventDefault();

    if (!courierName.trim() || !trackingNumber.trim()) {
      setDeliveryError("Enter both the courier name and the tracking number.");
      return;
    }

    setDeliveryError("");
    setSavingDelivery(true);

    try {
      const saved = await api.saveRecord({
        url: `${ENDPOINTS.orders}/${order.id}/delivery`,
        method: "PUT",
        payload: { courierName: courierName.trim(), trackingNumber: trackingNumber.trim() },
        fallbackMessage: "Unable to save delivery details",
      });

      if (saved) {
        onNotice("Delivery details saved.");
        await onReload();
      }
    } catch (err) {
      setDeliveryError(err.message);
    } finally {
      setSavingDelivery(false);
    }
  };

  const copyTrackingLink = async () => {
    try {
      await navigator.clipboard.writeText(trackingUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      onNotice("Copying is not available here. Select the link and copy it manually.");
    }
  };

  return (
    <Modal
      title={`Order #${order.id}`}
      subtitle={`${formatDateTime(order.orderDate)} · ${ORDER_STATUS_LABELS[order.orderStatus] ?? order.orderStatus}`}
      onClose={onClose}
      panelClass="admin-order-modal"
    >
      <div className="order-detail-grid">
        <section className="order-detail-card" aria-labelledby={`order-${order.id}-customer`}>
          <h4 id={`order-${order.id}-customer`}>Customer</h4>
          <p className="order-detail-main">{order.customerName || "-"}</p>
          <p>{order.phone ? <a href={`tel:${order.phone}`}>{order.phone}</a> : "No phone"}</p>
          <p className="order-detail-small">{order.email || "No email"}</p>
        </section>

        <section className="order-detail-card" aria-labelledby={`order-${order.id}-address`}>
          <h4 id={`order-${order.id}-address`}>Delivery address</h4>
          <p>{order.address || "Not provided"}</p>
          <p className="order-detail-small">
            {[order.city, order.state].filter(Boolean).join(", ")}
            {order.pincode ? ` – ${order.pincode}` : ""}
          </p>
        </section>

        <section className="order-detail-card" aria-labelledby={`order-${order.id}-payment`}>
          <h4 id={`order-${order.id}-payment`}>Payment</h4>
          <p>
            <span className={`admin-status ${paid ? "available" : "inactive"}`}>{order.paymentStatus || "PENDING"}</span>
          </p>
          <p className="order-detail-small">Payment ID: {order.razorpayPaymentId || "Not available"}</p>
          <p className="order-detail-small">Razorpay order: {order.razorpayOrderId || "Not available"}</p>
        </section>
      </div>

      <section className="order-section" aria-labelledby={`order-${order.id}-items`}>
        <h4 id={`order-${order.id}-items`}>Items</h4>

        <div className="admin-table-scroll">
          <table className="admin-table order-items-table">
            <thead>
              <tr>
                <th scope="col">Product</th>
                <th scope="col">Lens details</th>
                <th scope="col">Qty</th>
                <th scope="col">Unit price</th>
                <th scope="col">Line total</th>
              </tr>
            </thead>

            <tbody>
              {items.map((item) => (
                <tr key={item.id ?? `${item.productId}-${item.productName}`}>
                  <td>{item.productName}</td>
                  <td className="order-lens-cell">{lensSummary(item)}</td>
                  <td>{item.quantity}</td>
                  <td>{formatINR(item.price, true)}</td>
                  <td>{formatINR(Number(item.price) * Number(item.quantity), true)}</td>
                </tr>
              ))}

              {items.length === 0 && <EmptyRow colSpan={5}>No items recorded for this order.</EmptyRow>}
            </tbody>

            <tfoot>
              <tr>
                <td colSpan={4}>Order total</td>
                <td>{formatINR(order.totalAmount, true)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <div className="order-actions-grid">
        <section className="order-detail-card" aria-labelledby={`order-${order.id}-status`}>
          <h4 id={`order-${order.id}-status`}>Order status</h4>

          <label htmlFor={`order-status-${order.id}`} className="order-detail-small">
            Delivery status
          </label>

          <select
            id={`order-status-${order.id}`}
            value={order.orderStatus || "PENDING"}
            disabled={statusBusy}
            onChange={(e) => update(order.id, e.target.value)}
          >
            {ORDER_STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>

          {statusBusy && <small role="status">Updating…</small>}
          <ErrorMessage>{statusError}</ErrorMessage>

          <p className="order-detail-small">
            Shipping needs payment to be confirmed and the courier and tracking details to be saved.
          </p>
        </section>

        <section className="order-detail-card" aria-labelledby={`order-${order.id}-link`}>
          <h4 id={`order-${order.id}-link`}>Customer tracking link</h4>

          {trackingUrl ? (
            <>
              <div className="order-link-row">
                <input
                  type="text"
                  readOnly
                  value={trackingUrl}
                  aria-label="Customer tracking link"
                  onFocus={(e) => e.target.select()}
                />

                <button type="button" className="admin-secondary-button" onClick={copyTrackingLink}>
                  {copied ? "Copied" : "Copy"}
                </button>
              </div>

              <a className="admin-text-button" href={trackingUrl} target="_blank" rel="noopener noreferrer">
                Open customer view
              </a>
            </>
          ) : (
            <p className="order-detail-small">No tracking link is available for this order.</p>
          )}
        </section>
      </div>

      <section className="order-section order-detail-card" aria-labelledby={`order-${order.id}-delivery`}>
        <h4 id={`order-${order.id}-delivery`}>Courier and tracking</h4>

        {!paid && (
          <p className="order-detail-small">Delivery details can be added once payment is confirmed.</p>
        )}

        {paid && locked && (
          <p className="order-detail-small">
            Delivery details are locked for {ORDER_STATUS_LABELS[order.orderStatus]?.toLowerCase()} orders.
          </p>
        )}

        <form className="delivery-form" onSubmit={saveDelivery} noValidate>
          <FormGroup id={`courier-${order.id}`} label="Courier name">
            <input
              id={`courier-${order.id}`}
              type="text"
              value={courierName}
              onChange={(e) => setCourierName(e.target.value)}
              placeholder="e.g. Delhivery"
              disabled={!paid || locked || savingDelivery}
            />
          </FormGroup>

          <FormGroup id={`tracking-${order.id}`} label="Tracking number">
            <input
              id={`tracking-${order.id}`}
              type="text"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              placeholder="Courier tracking number"
              disabled={!paid || locked || savingDelivery}
            />
          </FormGroup>

          <button
            type="submit"
            className="admin-primary-button"
            disabled={!paid || locked || savingDelivery}
          >
            {savingDelivery ? "Saving…" : "Save delivery"}
          </button>
        </form>

        <ErrorMessage>{deliveryError}</ErrorMessage>
      </section>
    </Modal>
  );
}

function OrdersPage({ api, orders, loading, onReload, onNotice }) {
  const [search, setSearch] = useState("");
  const [range, setRange] = useState(EMPTY_RANGE);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedId, setSelectedId] = useState(null);

  const bulk = useBulkClear({ api, onNotice, onReload });

  const visibleOrders = useMemo(() => {
    const term = search.trim().toLowerCase();

    return orders.filter(
      (o) =>
        matchesTerm(term, o.id, o.customerName, o.email, o.phone) &&
        (statusFilter === "ALL" || (o.orderStatus || "PENDING") === statusFilter) &&
        inDateRange(o.orderDate, range.start, range.end)
    );
  }, [orders, search, statusFilter, range]);

  const selectedOrder = orders.find((o) => o.id === selectedId) ?? null;

  // Close the dialog if its order disappears, for example after clearing orders
  useEffect(() => {
    if (selectedId !== null && !loading && !orders.some((o) => o.id === selectedId)) {
      setSelectedId(null);
    }
  }, [orders, selectedId, loading]);

  const resetFilters = () => {
    setSearch("");
    setRange(EMPTY_RANGE);
    setStatusFilter("ALL");
  };

  const handleClear = () =>
    bulk.clear({
      endpoints: [ENDPOINTS.orders],
      warning: "This will permanently delete ALL orders and their order items. This cannot be undone.",
      successMessage: "All orders permanently deleted.",
      fallbackMessage: "Unable to delete orders.",
      onCleared: () => {
        resetFilters();
        setSelectedId(null);
      },
    });

  const handleRefresh = async () => {
    resetFilters();
    await onReload();
  };

  return (
    <section className="admin-content">
      <PageHeading
        title="Orders Management"
        description="Review orders and manage deliveries. Select View details to see the full order."
        action={<RefreshButton onClick={handleRefresh} loading={loading} />}
      />

      <div className="admin-dashboard-actions">
        <DateRangeFilter start={range.start} end={range.end} onChange={setRange} />
        <ClearButton label="Clear Orders" onClick={handleClear} busy={bulk.clearing} />
      </div>

      <div className="admin-product-toolbar">
        <SearchBox
          label="Search orders"
          placeholder="Search order ID, customer or phone…"
          value={search}
          onChange={setSearch}
        />

        <select
          className="admin-filter-select"
          aria-label="Filter orders by delivery status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All delivery statuses</option>
          {ORDER_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        <span aria-live="polite">
          {visibleOrders.length} of {orders.length} orders
        </span>
      </div>

      <div className="admin-table-card">
        <div className="admin-table-scroll">
          <table className="admin-table admin-order-table">
            <thead>
              <tr>
                <th scope="col">Order</th>
                <th scope="col">Date</th>
                <th scope="col">Customer</th>
                <th scope="col">Items</th>
                <th scope="col">Total</th>
                <th scope="col">Payment</th>
                <th scope="col">Status</th>
                <th scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>

            <tbody>
              {loading && <EmptyRow colSpan={8}>Loading orders…</EmptyRow>}

              {!loading &&
                visibleOrders.map((order) => {
                  const paid = isOrderPaid(order);
                  const selected = order.id === selectedId;

                  return (
                    <tr key={order.id} className={selected ? "is-selected" : ""}>
                      <td>
                        <strong>#{order.id}</strong>
                      </td>

                      <td>{formatDate(order.orderDate)}</td>

                      <td>
                        <span className="order-cell-name">{order.customerName || "-"}</span>
                        {order.phone && <small className="order-cell-sub">{order.phone}</small>}
                      </td>

                      <td>{countUnits(order)}</td>

                      <td className="order-cell-amount">{formatINR(order.totalAmount, true)}</td>

                      <td>
                        <span className={`admin-status ${paid ? "available" : "inactive"}`}>
                          {order.paymentStatus || "PENDING"}
                        </span>
                      </td>

                      <td>
                        <OrderStatusBadge status={order.orderStatus} />
                      </td>

                      <td>
                        <button
                          type="button"
                          className="admin-text-button"
                          aria-haspopup="dialog"
                          onClick={() => setSelectedId(order.id)}
                          aria-label={`View details for order ${order.id}`}
                        >
                          View details
                        </button>
                      </td>
                    </tr>
                  );
                })}

              {!loading && visibleOrders.length === 0 && (
                <EmptyRow colSpan={8}>
                  {orders.length === 0 ? "No orders received yet." : "No matching orders found."}
                </EmptyRow>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {selectedOrder && (
        <OrderDetailsDialog
          key={selectedOrder.id}
          api={api}
          order={selectedOrder}
          onClose={() => setSelectedId(null)}
          onNotice={onNotice}
          onReload={onReload}
        />
      )}

      <ClearDataPopup bulk={bulk} />
    </section>
  );
}

/* =========================
   APPOINTMENTS
========================= */

function AppointmentsPage({ api, appointments, loading, onReload, onNotice }) {
  const confirm = useConfirm();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [range, setRange] = useState(EMPTY_RANGE);

  const bulk = useBulkClear({ api, onNotice, onReload });
  const { busyId, error: statusError, update } = useStatusUpdater({
    api,
    baseUrl: ENDPOINTS.appointments,
    label: "Appointment",
    onNotice,
    onReload,
  });

  const counts = useMemo(() => {
    const result = { total: appointments.length };
    APPOINTMENT_STATUSES.forEach((s) => {
      result[s.value] = appointments.filter((a) => a.status === s.value).length;
    });
    return result;
  }, [appointments]);

  const visibleAppointments = useMemo(() => {
    const term = search.trim().toLowerCase();

    return appointments.filter(
      (a) =>
        matchesTerm(term, a.customerName, a.phone, a.email) &&
        (statusFilter === "ALL" || a.status === statusFilter) &&
        inDateRange(a.appointmentDate, range.start, range.end)
    );
  }, [appointments, search, statusFilter, range]);

  const resetFilters = () => {
    setSearch("");
    setStatusFilter("ALL");
    setRange(EMPTY_RANGE);
  };

  const handleClear = () =>
    bulk.clear({
      endpoints: [ENDPOINTS.appointments],
      warning: "This will permanently delete ALL appointment records. This cannot be undone.",
      successMessage: "All appointments permanently deleted.",
      fallbackMessage: "Unable to delete appointments.",
      onCleared: resetFilters,
    });

  const handleRefresh = async () => {
    resetFilters();
    await onReload();
  };

  const cancelAppointment = async (appointment) => {
    const ok = await confirm({
      title: "Cancel appointment",
      message: `Cancel the eye checkup appointment for ${appointment.customerName}?`,
      confirmLabel: "Yes, cancel it",
      cancelLabel: "Keep it",
      danger: true,
    });
    if (ok) update(appointment.id, "CANCELLED");
  };

  return (
    <section className="admin-content">
      <PageHeading
        title="Eye Checkup Appointments"
        description="Manage customer eye checkup bookings."
        action={<RefreshButton onClick={handleRefresh} loading={loading} />}
      />

      <ErrorMessage>{statusError}</ErrorMessage>

      <div className="admin-stats-grid">
        <StatCard label="Total Appointments" value={counts.total} />
        {APPOINTMENT_STATUSES.map((s) => (
          <StatCard key={s.value} label={s.label} value={counts[s.value]} />
        ))}
      </div>

      <div className="admin-dashboard-actions">
        <DateRangeFilter start={range.start} end={range.end} onChange={setRange} />
        <ClearButton label="Clear Appointments" onClick={handleClear} busy={bulk.clearing} />
      </div>

      <div className="admin-product-toolbar">
        <SearchBox
          label="Search appointments"
          placeholder="Search by customer name or phone..."
          value={search}
          onChange={setSearch}
        />

        <select
          className="admin-filter-select"
          aria-label="Filter appointments by status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All statuses</option>
          {APPOINTMENT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        <span aria-live="polite">{visibleAppointments.length} appointments displayed</span>
      </div>

      {loading ? (
        <p className="admin-loading">Loading appointments...</p>
      ) : visibleAppointments.length === 0 ? (
        <div className="admin-placeholder">
          <CalendarDays size={35} aria-hidden="true" />
          <h3>No appointments found</h3>
          <p>Customer bookings will appear here.</p>
        </div>
      ) : (
        <div className="admin-orders-list">
          {visibleAppointments.map((appointment) => {
            const busy = busyId === appointment.id;
            const cancelled = appointment.status === "CANCELLED";

            return (
              <article className="admin-order-card" key={appointment.id}>
                <div className="admin-order-header">
                  <div>
                    <h3>{appointment.customerName}</h3>
                    <span>Appointment #{appointment.id}</span>
                  </div>

                  <span className="admin-status-badge" data-status={appointment.status}>
                    {appointment.status}
                  </span>
                </div>

                <div className="admin-order-details">
                  <div>
                    <h4>Customer Details</h4>
                    <p>
                      <strong>Phone:</strong> {appointment.phone}
                    </p>
                    <p>
                      <strong>Email:</strong> {appointment.email || "Not provided"}
                    </p>
                  </div>

                  <div>
                    <h4>Appointment Details</h4>
                    <p>
                      <strong>Date:</strong> {appointment.appointmentDate}
                    </p>
                    <p>
                      <strong>Time:</strong> {appointment.appointmentTime}
                    </p>
                  </div>
                </div>

                <div className="admin-order-footer">
                  <div>
                    <label htmlFor={`appointment-status-${appointment.id}`}>Update Status</label>

                    <select
                      id={`appointment-status-${appointment.id}`}
                      value={appointment.status}
                      disabled={busy}
                      onChange={(e) => update(appointment.id, e.target.value)}
                    >
                      {APPOINTMENT_STATUSES.map((s) => (
                        <option key={s.value} value={s.value}>
                          {s.label}
                        </option>
                      ))}
                    </select>

                    {busy && <small role="status">Updating...</small>}
                  </div>

                  <button
                    type="button"
                    className="admin-appointment-cancel"
                    disabled={cancelled || busy}
                    onClick={() => cancelAppointment(appointment)}
                  >
                    <X size={16} aria-hidden="true" />
                    Cancel Appointment
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <ClearDataPopup bulk={bulk} />
    </section>
  );
}

/* =========================
   CUSTOMERS
========================= */

function CustomersPage({ orders, customers, loading, onReload }) {
  const [search, setSearch] = useState("");
  const [selectedKey, setSelectedKey] = useState(null);

  const selected = useMemo(
    () => customers.find((c) => customerKey(c) === selectedKey) ?? null,
    [customers, selectedKey]
  );

  const visibleCustomers = useMemo(() => {
    const term = search.trim().toLowerCase();
    return customers.filter((c) => matchesTerm(term, c.customerName, c.email, c.phone));
  }, [customers, search]);

  const selectedOrders = useMemo(() => {
    if (!selected) return [];

    return orders.filter((order) =>
      selected.email
        ? order.email?.trim().toLowerCase() === selected.email.trim().toLowerCase()
        : order.phone === selected.phone
    );
  }, [orders, selected]);

  const totals = useMemo(
    () => ({
      orders: customers.reduce((sum, c) => sum + Number(c.totalOrders || 0), 0),
      spent: customers.reduce((sum, c) => sum + Number(c.totalSpent || 0), 0),
    }),
    [customers]
  );

  return (
    <section className="admin-content">
      <PageHeading
        title="Customers"
        description="Manage your registered shopping customers."
        action={<RefreshButton onClick={onReload} loading={loading} />}
      />

      <div className="admin-stats-grid">
        <StatCard label="Total Customers" value={customers.length} />
        <StatCard label="Total Customer Orders" value={totals.orders} />
        <StatCard label="Total Customer Spending" value={formatINR(totals.spent, true)} />
      </div>

      <div className="admin-product-toolbar">
        <SearchBox
          label="Search customers"
          placeholder="Search by name, email or phone..."
          value={search}
          onChange={setSearch}
        />
      </div>

      {loading ? (
        <p className="admin-loading">Loading customers...</p>
      ) : (
        <>
          <div className="admin-table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th scope="col">Customer</th>
                  <th scope="col">Email</th>
                  <th scope="col">Phone</th>
                  <th scope="col">Total Orders</th>
                  <th scope="col">Total Spent</th>
                </tr>
              </thead>

              <tbody>
                {visibleCustomers.map((customer) => {
                  const key = customerKey(customer);
                  const isSelected = key === selectedKey;

                  return (
                    <tr
                      key={key || customer.customerName}
                      className={isSelected ? "customer-row selected" : "customer-row"}
                    >
                      <td>
                        <button
                          type="button"
                          className="admin-row-select"
                          aria-pressed={isSelected}
                          onClick={() => setSelectedKey(key)}
                        >
                          {customer.customerName || "-"}
                        </button>
                      </td>
                      <td>{customer.email || "-"}</td>
                      <td>{customer.phone || "-"}</td>
                      <td>{customer.totalOrders || 0}</td>
                      <td>{formatINR(customer.totalSpent, true)}</td>
                    </tr>
                  );
                })}

                {visibleCustomers.length === 0 && (
                  <EmptyRow colSpan={5}>
                    {customers.length === 0 ? "No customers found." : "No matching customers found."}
                  </EmptyRow>
                )}
              </tbody>
            </table>
          </div>

          {selected && (
            <div className="customer-history">
              <div className="admin-section-header">
                <div>
                  <h2>{selected.customerName}</h2>
                  <p>{selected.email || "No email"}</p>
                  <p>{selected.phone || "No phone"}</p>
                </div>

                <button type="button" className="admin-secondary-button" onClick={() => setSelectedKey(null)}>
                  <X size={16} aria-hidden="true" />
                  Close
                </button>
              </div>

              <h3>Order History</h3>

              {selectedOrders.length === 0 ? (
                <p>No orders found for this customer.</p>
              ) : (
                <div className="admin-table-wrapper">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th scope="col">Order ID</th>
                        <th scope="col">Order Date</th>
                        <th scope="col">Amount</th>
                        <th scope="col">Payment</th>
                        <th scope="col">Delivery Status</th>
                      </tr>
                    </thead>

                    <tbody>
                      {selectedOrders.map((order) => (
                        <tr key={order.id}>
                          <td>#{order.id}</td>
                          <td>{formatDate(order.orderDate)}</td>
                          <td>{formatINR(order.totalAmount, true)}</td>
                          <td>{order.paymentStatus || "-"}</td>
                          <td>{order.orderStatus || "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </>
      )}
    </section>
  );
}

/* =========================
   MARKETING
========================= */

function BannerModal({ api, initial, onClose, onSaved }) {
  const [form, setForm] = useState(initial.data);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const isEdit = Boolean(initial.id);
  const handleChange = handleFieldChange(setForm);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    const problem = getImageFileError(file);
    if (problem) {
      setError(problem);
      return;
    }

    setError("");
    setUploading(true);

    try {
      const imageUrl = await uploadToCloudinary(file, "nmk-marketing");
      setForm((prev) => ({ ...prev, imageUrl }));
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const problem = validateBanner(form);
    if (problem) {
      setError(problem);
      return;
    }

    setSaving(true);
    setError("");

    try {
      const saved = await api.saveRecord({
        url: isEdit ? `${ENDPOINTS.banners}/${initial.id}` : ENDPOINTS.banners,
        method: isEdit ? "PUT" : "POST",
        payload: {
          title: form.title.trim(),
          imageUrl: form.imageUrl.trim(),
          targetUrl: form.targetUrl.trim(),
          placement: form.placement,
          displayOrder: Number(form.displayOrder),
          active: form.active,
        },
        fallbackMessage: "Unable to save banner",
      });

      if (saved) onSaved(isEdit ? "Banner updated." : "Banner added.");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const busy = saving || uploading;
  const previewReady = Boolean(form.imageUrl) && isValidHttpUrl(form.imageUrl);

  return (
    <Modal title={isEdit ? "Edit Banner" : "Add Banner"} onClose={onClose} closable={!busy} {...MARKETING_MODAL}>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="banner-title">Banner Title *</label>
          <input id="banner-title" type="text" name="title" value={form.title} onChange={handleChange} placeholder="Enter banner title" required />
        </div>

        <div className="form-group">
          <label htmlFor="banner-image">Image URL *</label>
          <input id="banner-image" type="url" name="imageUrl" value={form.imageUrl} onChange={handleChange} placeholder="https://example.com/banner.jpg" required />
        </div>

        <div className="form-group">
          <label htmlFor="banner-upload">Or upload from your computer</label>
          <input id="banner-upload" type="file" accept={IMAGE_ACCEPT} onChange={handleUpload} disabled={busy} />
          <small role="status">{uploading ? "Uploading..." : "JPG, PNG or WEBP. Maximum 5 MB."}</small>
        </div>

        {previewReady && (
          <div className="form-group">
            <span className="admin-field-label">Preview</span>
            <img src={form.imageUrl} alt="Banner preview" className="admin-banner-preview" />
          </div>
        )}

        <div className="form-group">
          <label htmlFor="banner-target">Destination</label>
          <input id="banner-target" type="text" name="targetUrl" value={form.targetUrl} onChange={handleChange} placeholder="/products or https://example.com" />
        </div>

        <div className="form-group">
          <label htmlFor="banner-placement">Display Placement</label>
          <select id="banner-placement" name="placement" value={form.placement} onChange={handleChange}>
            {BANNER_PLACEMENTS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="banner-order">Display Order</label>
          <input id="banner-order" type="number" name="displayOrder" min="0" step="1" value={form.displayOrder} onChange={handleChange} />
        </div>

        <div className="form-group">
          <label className="admin-checkbox-label">
            <input type="checkbox" name="active" checked={form.active} onChange={handleChange} />
            Active
          </label>
        </div>

        <ErrorMessage>{error}</ErrorMessage>

        <div className="modal-actions">
          <button type="button" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button type="submit" disabled={busy}>
            {saving ? "Saving..." : isEdit ? "Update Banner" : "Save Banner"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

// Trims ISO timestamps to the "YYYY-MM-DDTHH:mm" form that datetime-local inputs use
const toLocalInput = (value) => (value ? String(value).slice(0, 16) : "");

function OfferModal({ api, existing, onClose, onSaved }) {
  const [form, setForm] = useState(() =>
    existing
      ? {
          message: existing.message || "",
          backgroundColor: existing.backgroundColor || EMPTY_OFFER.backgroundColor,
          textColor: existing.textColor || EMPTY_OFFER.textColor,
          targetUrl: existing.targetUrl || "",
          startDateTime: existing.startDateTime || "",
          endDateTime: existing.endDateTime || "",
          active: existing.active ?? true,
        }
      : EMPTY_OFFER
  );
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const handleChange = handleFieldChange(setForm);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const problem = validateOffer(form);
    if (problem) {
      setError(problem);
      return;
    }

    setSaving(true);
    setError("");

    try {
      const saved = await api.saveRecord({
        url: existing ? `${ENDPOINTS.offers}/${existing.id}` : ENDPOINTS.offers,
        method: existing ? "PUT" : "POST",
        payload: {
          message: form.message.trim(),
          backgroundColor: form.backgroundColor,
          textColor: form.textColor,
          targetUrl: form.targetUrl.trim(),
          startDateTime: form.startDateTime || null,
          endDateTime: form.endDateTime || null,
          active: form.active,
        },
        fallbackMessage: "Unable to save offer",
      });

      if (saved) onSaved("Offer bar saved.");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal title={existing ? "Edit Offer Bar" : "Create Offer Bar"} onClose={onClose} closable={!saving} {...MARKETING_MODAL}>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="offer-message">Offer Message *</label>
          <input id="offer-message" type="text" name="message" value={form.message} onChange={handleChange} placeholder="Example: Get 20% off on sunglasses!" required />
        </div>

        <div className="form-group">
          <label htmlFor="offer-bg">Background Colour</label>
          <input id="offer-bg" type="color" name="backgroundColor" value={form.backgroundColor} onChange={handleChange} />
        </div>

        <div className="form-group">
          <label htmlFor="offer-text">Text Colour</label>
          <input id="offer-text" type="color" name="textColor" value={form.textColor} onChange={handleChange} />
        </div>

        <div className="form-group">
          <label htmlFor="offer-target">Destination</label>
          <input id="offer-target" type="text" name="targetUrl" value={form.targetUrl} onChange={handleChange} placeholder="/products" />
        </div>

        <div className="form-group">
          <label htmlFor="offer-start">Start Date and Time</label>
          <input id="offer-start" type="datetime-local" name="startDateTime" value={toLocalInput(form.startDateTime)} onChange={handleChange} />
        </div>

        <div className="form-group">
          <label htmlFor="offer-end">End Date and Time</label>
          <input id="offer-end" type="datetime-local" name="endDateTime" value={toLocalInput(form.endDateTime)} onChange={handleChange} />
        </div>

        <div className="form-group">
          <label className="admin-checkbox-label">
            <input type="checkbox" name="active" checked={form.active} onChange={handleChange} />
            Enable Offer Bar
          </label>
        </div>

        <div className="admin-offer-preview" style={{ background: form.backgroundColor, color: form.textColor }}>
          {form.message || "Your offer preview will appear here"}
        </div>

        <ErrorMessage>{error}</ErrorMessage>

        <div className="modal-actions">
          <button type="button" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save Offer"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

// Single-image slots on the customer website. Each one is saved under its own image key,
// and the customer site reads the same keys (HERO, LOGO, STORE).
const HERO_IMAGE_SLOT = {
  imageKey: "HERO",
  title: "Homepage Hero Image",
  heading: "Homepage Hero Image",
  description: "The large picture shown beside the headline at the top of the customer website home page.",
  noun: "hero image",
  urlLabel: "Hero Image URL",
  uploadLabel: "Or Upload Hero Image",
  hint: "Use a bright, wide photo, at least 1200 px wide. JPG, PNG or WEBP, maximum 5 MB.",
  updateLabel: "Update Hero Image",
  removeLabel: "Remove Hero Image",
  updatedNotice: "Homepage hero image updated successfully.",
  removedNotice: "Custom hero image removed. The default image is shown on the website.",
  removeMessage: "Remove the custom hero image? The default hero image will be shown instead.",
};

const LOGO_IMAGE_SLOT = {
  imageKey: "LOGO",
  title: "Website Logo",
  heading: "Customer Website Logo",
  description: "Change the logo displayed in the customer website header.",
  noun: "logo",
  urlLabel: "Logo Image URL",
  uploadLabel: "Or Upload Logo",
  hint: "JPG, PNG or WEBP, maximum 5 MB.",
  updateLabel: "Update Logo",
  removeLabel: "Remove Logo",
  updatedNotice: "Website logo updated successfully.",
  removedNotice: "Custom logo removed.",
  removeMessage: "Remove the custom website logo? The store name will be shown instead.",
};

const STORE_IMAGE_SLOT = {
  imageKey: "STORE",
  title: "Store Image",
  heading: "Our Store Image",
  description: "Change the showroom image displayed in the customer website.",
  noun: "store image",
  urlLabel: "Store Image URL",
  uploadLabel: "Or Upload Store Image",
  hint: "JPG, PNG or WEBP, maximum 5 MB.",
  updateLabel: "Update Store Image",
  removeLabel: "Remove Store Image",
  updatedNotice: "Store image updated successfully.",
  removedNotice: "Custom store image removed. The default image is shown on the website.",
  removeMessage: "Remove the custom store image? The default store image will be shown instead.",
};

// One editor for every single-image slot: shows the saved image, previews a new one before
// saving, accepts a URL or an uploaded file, and can go back to the website's default image.
function SingleImageEditor({ api, images, loading, onReload, onNotice, slot }) {
  const confirm = useConfirm();
  const saved = images.find((image) => image.imageKey === slot.imageKey);
  const savedUrl = saved?.imageUrl || "";
  const fieldId = `website-image-${slot.imageKey.toLowerCase()}`;

  const [imageUrl, setImageUrl] = useState(savedUrl);
  const [file, setFile] = useState(null);
  const [filePreview, setFilePreview] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  // Keep the URL box in sync when the saved image changes
  useEffect(() => {
    setImageUrl(savedUrl);
  }, [savedUrl]);

  // Local preview of a chosen file, released when it is replaced or cleared
  useEffect(() => {
    if (!file) {
      setFilePreview("");
      return undefined;
    }

    const objectUrl = URL.createObjectURL(file);
    setFilePreview(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  const trimmed = imageUrl.trim();
  const pendingUrl = filePreview || (isValidHttpUrl(trimmed) && trimmed !== savedUrl ? trimmed : "");
  const previewSrc = pendingUrl || savedUrl;
  const canSave = Boolean(file) || (trimmed !== "" && trimmed !== savedUrl);

  const handleFileChange = (e) => {
    const selected = e.target.files?.[0];
    e.target.value = "";
    if (!selected) return;

    const problem = getImageFileError(selected);
    if (problem) return setError(problem);

    setFile(selected);
    setError("");
  };

  const handleSave = async () => {
    setError("");

    if (!file && !trimmed) return setError(`Choose a ${slot.noun} or enter an image URL.`);
    if (!file && !isValidHttpUrl(trimmed)) return setError("Enter a valid image URL.");

    setSaving(true);

    try {
      const finalUrl = file ? await uploadToCloudinary(file, "nmk-website") : trimmed;
      const done = await saveWebsiteImage(api, slot.imageKey, slot.title, finalUrl, `Failed to update ${slot.noun}`);

      if (done) {
        setFile(null);
        onNotice(slot.updatedNotice);
        await onReload();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async () => {
    const ok = await confirm({
      title: `Remove ${slot.noun}`,
      message: slot.removeMessage,
      confirmLabel: "Yes, remove",
      danger: true,
    });
    if (!ok) return;

    setError("");
    setSaving(true);

    try {
      const done = await saveWebsiteImage(api, slot.imageKey, slot.title, "", `Failed to remove ${slot.noun}`);

      if (done) {
        setImageUrl("");
        setFile(null);
        onNotice(slot.removedNotice);
        await onReload();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-table-card logo-editor">
      <div className="admin-table-heading">
        <div>
          <h3>{slot.heading}</h3>
          <p>{slot.description}</p>
        </div>
      </div>

      {loading && !saved ? (
        <div className="admin-empty">Loading {slot.noun}...</div>
      ) : (
        <div className="logo-editor-content">
          {previewSrc && (
            <div className="form-group">
              <span className="admin-field-label">{pendingUrl ? "Preview (not saved yet)" : "Current image"}</span>
              <div className="logo-preview">
                <img src={previewSrc} alt={`${slot.heading} preview`} style={{ maxWidth: "100%" }} />
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor={`${fieldId}-url`}>{slot.urlLabel}</label>
            <input
              id={`${fieldId}-url`}
              type="url"
              value={imageUrl}
              onChange={(e) => {
                setImageUrl(e.target.value);
                setFile(null);
                setError("");
              }}
              placeholder="https://example.com/image.jpg"
              disabled={saving}
            />
          </div>

          <div className="form-group">
            <label htmlFor={`${fieldId}-file`}>{slot.uploadLabel}</label>
            <input
              id={`${fieldId}-file`}
              type="file"
              accept={IMAGE_ACCEPT}
              disabled={saving}
              onChange={handleFileChange}
            />
            <small>{file ? `Selected: ${file.name}` : slot.hint}</small>
          </div>

          <ErrorMessage>{error}</ErrorMessage>

          <div className="logo-editor-actions">
            <button
              type="button"
              className="admin-primary-button"
              onClick={handleSave}
              disabled={saving || !canSave}
            >
              {saving ? "Saving..." : slot.updateLabel}
            </button>

            <button
              type="button"
              className="admin-secondary-button"
              onClick={handleRemove}
              disabled={saving || !savedUrl}
            >
              {slot.removeLabel}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function MarketingPage({ api, banners, offer, loading, onReload, onNotice, images, imagesLoading, onReloadImages }) {
  const confirm = useConfirm();
  const [bannerModal, setBannerModal] = useState(null); // null | { id, data }
  const [offerModalOpen, setOfferModalOpen] = useState(false);
  const [pageError, setPageError] = useState("");
  const [deletingId, setDeletingId] = useState(null);

  const openAddBanner = () => setBannerModal({ id: null, data: EMPTY_BANNER });

  const openEditBanner = (banner) =>
    setBannerModal({
      id: banner.id,
      data: {
        title: banner.title || "",
        imageUrl: banner.imageUrl || "",
        targetUrl: banner.targetUrl || "",
        placement: banner.placement || "HOME",
        displayOrder: banner.displayOrder ?? 0,
        active: banner.active ?? true,
      },
    });

  const deleteBanner = async (banner) => {
    const ok = await confirm({
      title: "Delete banner",
      message: `Delete the banner "${banner.title}"? This cannot be undone.`,
      confirmLabel: "Yes, delete",
      danger: true,
    });
    if (!ok) return;

    setPageError("");
    setDeletingId(banner.id);

    try {
      const deleted = await api.deleteRecord(`${ENDPOINTS.banners}/${banner.id}`, "Failed to delete banner");
      if (deleted) {
        onNotice("Banner deleted.");
        await onReload();
      }
    } catch (err) {
      setPageError(err.message);
    } finally {
      setDeletingId(null);
    }
  };

  const handleBannerSaved = async (message) => {
    setBannerModal(null);
    onNotice(message);
    await onReload();
  };

  const handleOfferSaved = async (message) => {
    setOfferModalOpen(false);
    onNotice(message);
    await onReload();
  };

  // Everything on this page comes from two sources: marketing data and website images
  const refreshAll = () => Promise.all([onReload(), onReloadImages()]);
  const imageProps = { api, images, loading: imagesLoading, onReload: onReloadImages, onNotice };

  return (
    <section className="admin-content">
      <PageHeading
        title="Marketing Management"
        description="Manage the homepage hero image, logo, store and category images, banners and promotional offers."
        action={<RefreshButton onClick={refreshAll} loading={loading || imagesLoading} />}
      />

      <ErrorMessage>{pageError}</ErrorMessage>

      <SingleImageEditor {...imageProps} slot={HERO_IMAGE_SLOT} />
      <SingleImageEditor {...imageProps} slot={LOGO_IMAGE_SLOT} />
      <SingleImageEditor {...imageProps} slot={STORE_IMAGE_SLOT} />

      <div className="admin-table-card category-images-card">
        <div className="admin-table-heading">
          <div>
            <h3>Shop by Category Images</h3>
            <p>Shown on the customer site in this order: Eyeglasses, Sunglasses, Power Sunglasses, Computer Glasses, Contact Lenses, Accessories.</p>
          </div>
        </div>

        <div className="category-images-body">
          <CategoryImagesEditor {...imageProps} />
        </div>
      </div>

      {/* BANNERS */}
      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Website Marketing Banners</h3>
            <span>{banners.length} banners</span>
          </div>

          <button type="button" className="admin-primary-button" onClick={openAddBanner}>
            <Plus size={17} aria-hidden="true" />
            Add Banner
          </button>
        </div>

        {loading ? (
          <div className="admin-empty">Loading banners...</div>
        ) : banners.length === 0 ? (
          <div className="admin-empty">No marketing banners added yet.</div>
        ) : (
          <div className="admin-table-scroll">
            <table className="admin-table">
              <thead>
                <tr>
                  <th scope="col">Preview</th>
                  <th scope="col">Title</th>
                  <th scope="col">Placement</th>
                  <th scope="col">Order</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>

              <tbody>
                {banners.map((banner) => {
                  const busy = deletingId === banner.id;

                  return (
                    <tr key={banner.id}>
                      <td>
                        <img src={banner.imageUrl} alt="" className="admin-banner-thumb" />
                      </td>
                      <td>{banner.title}</td>
                      <td>{BANNER_PLACEMENTS.find((p) => p.value === banner.placement)?.label ?? banner.placement}</td>
                      <td>{banner.displayOrder}</td>
                      <td>
                        <StatusBadge active={banner.active} />
                      </td>
                      <td>
                        <div className="admin-action-buttons">
                          <button
                            type="button"
                            className="admin-edit-button"
                            title="Edit banner"
                            aria-label={`Edit banner ${banner.title}`}
                            onClick={() => openEditBanner(banner)}
                            disabled={busy}
                          >
                            <Pencil size={17} aria-hidden="true" />
                          </button>

                          <button
                            type="button"
                            className="admin-delete-button"
                            title="Delete banner"
                            aria-label={`Delete banner ${banner.title}`}
                            onClick={() => deleteBanner(banner)}
                            disabled={busy}
                          >
                            <Trash2 size={17} aria-hidden="true" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* OFFER BAR */}
      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Promotional Offer Bar</h3>
            <p>The message shown across the top of the website.</p>
          </div>
        </div>

        {offer ? (
          <div className="admin-offer-summary">
            <div
              className="admin-offer-preview"
              style={{
                background: offer.backgroundColor || EMPTY_OFFER.backgroundColor,
                color: offer.textColor || EMPTY_OFFER.textColor,
              }}
            >
              {offer.message}
            </div>

            <p>
              <strong>Status:</strong> {offer.active ? "Active" : "Inactive"}
            </p>
            <p>
              <strong>Start:</strong> {offer.startDateTime ? formatDateTime(offer.startDateTime) : "Not specified"}
            </p>
            <p>
              <strong>End:</strong> {offer.endDateTime ? formatDateTime(offer.endDateTime) : "Not specified"}
            </p>
          </div>
        ) : (
          <div className="admin-empty">No promotional offer configured.</div>
        )}

        <div className="admin-offer-footer">
          <button type="button" className="admin-primary-button" onClick={() => setOfferModalOpen(true)}>
            <Pencil size={16} aria-hidden="true" />
            {offer ? "Edit Offer" : "Create Offer"}
          </button>
        </div>
      </div>

      {bannerModal && (
        <BannerModal api={api} initial={bannerModal} onClose={() => setBannerModal(null)} onSaved={handleBannerSaved} />
      )}

      {offerModalOpen && (
        <OfferModal api={api} existing={offer} onClose={() => setOfferModalOpen(false)} onSaved={handleOfferSaved} />
      )}
    </section>
  );
}

/* =========================
   CATEGORY IMAGES
   The "Shop by Category" pictures. They are edited from the Marketing page.
========================= */

// Category image slots, in the order shown on the customer site
const CATEGORY_IMAGE_SLOTS = [
  { imageKey: "CATEGORY_EYEGLASSES", title: "Eyeglasses" },
  { imageKey: "CATEGORY_SUNGLASSES", title: "Sunglasses" },
  { imageKey: "CATEGORY_POWER_SUNGLASSES", title: "Power Sunglasses" },
  { imageKey: "CATEGORY_COMPUTER_GLASSES", title: "Computer Glasses" },
  { imageKey: "CATEGORY_CONTACT_LENSES", title: "Contact Lenses" },
  { imageKey: "CATEGORY_ACCESSORIES", title: "Accessories" },
];

function CategoryImagesEditor({ api, images, loading, onReload, onNotice }) {
  // Only one image can be edited at a time:
  // { imageKey, mode: "url" | "file", url, file, previewUrl, error, saving }
  const [edit, setEdit] = useState(null);
  const previewUrl = edit?.previewUrl;

  // Every category is listed in store order, including ones without a custom image yet
  const slots = CATEGORY_IMAGE_SLOTS.map(
    (slot) => images.find((image) => image.imageKey === slot.imageKey) ?? { ...slot, imageUrl: "" }
  );

  // Release the object URL when it is replaced, the editor closes, or the page unmounts
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const openEditor = (image) =>
    setEdit({ imageKey: image.imageKey, mode: "url", url: "", file: null, previewUrl: "", error: "", saving: false });

  const closeEditor = () => setEdit(null);

  // Ignores updates after the editor has closed
  const updateEdit = (changes) => setEdit((prev) => (prev ? { ...prev, ...changes } : prev));

  const switchMode = (mode) => updateEdit({ mode, error: "", file: null, previewUrl: "" });

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    const problem = getImageFileError(file);
    if (problem) return updateEdit({ error: problem });

    updateEdit({ file, previewUrl: URL.createObjectURL(file), error: "" });
  };

  const save = async (image) => {
    const current = edit;
    if (!current) return;

    updateEdit({ saving: true, error: "" });

    try {
      let imageUrl;

      if (current.mode === "file") {
        if (!current.file) throw new Error("Please choose an image file.");
        imageUrl = await uploadToCloudinary(current.file, "nmk-website");
      } else {
        imageUrl = current.url.trim();
        if (!imageUrl) throw new Error("Please enter an image URL.");
        if (!isValidHttpUrl(imageUrl)) throw new Error("Please enter a valid image URL.");
      }

      const done = await saveWebsiteImage(api, image.imageKey, image.title, imageUrl, "Failed to save category image");

      if (!done) return updateEdit({ saving: false });

      closeEditor();
      onNotice(`${image.title} image updated.`);
      await onReload();
    } catch (err) {
      updateEdit({ saving: false, error: err.message });
    }
  };

  if (loading && images.length === 0) {
    return <p className="wi-message">Loading category images...</p>;
  }

  return (
    <div className="wi-grid">
      {slots.map((image) => {
        const isEditing = edit?.imageKey === image.imageKey;
        const previewSrc = isEditing ? getPreviewSrc(edit, image.imageUrl) : image.imageUrl;

        return (
          <div className="wi-card" key={image.id ?? image.imageKey}>
            {previewSrc ? (
              <img className="wi-image" src={previewSrc} alt={`${image.title} category`} />
            ) : (
              <div
                className="wi-image"
                role="img"
                aria-label={`${image.title}: no custom image, the website default is shown`}
                style={{
                  display: "grid",
                  placeItems: "center",
                  padding: "12px",
                  textAlign: "center",
                  fontSize: "13px",
                  color: "#6b7280",
                  background: "#f3f4f6",
                }}
              >
                No custom image yet. The website default is shown.
              </div>
            )}

            <h3 className="wi-title">{image.title}</h3>

            {!isEditing && (
              <button type="button" className="wi-edit-button" onClick={() => openEditor(image)} disabled={Boolean(edit)}>
                <Pencil size={16} aria-hidden="true" />
                Edit
              </button>
            )}

            {isEditing && (
              <div className="wi-edit-panel">
                <div className="wi-edit-header">
                  <span>Edit image</span>
                  <button
                    type="button"
                    className="wi-close"
                    onClick={closeEditor}
                    disabled={edit.saving}
                    aria-label="Close editor"
                    title="Close"
                  >
                    <X size={16} aria-hidden="true" />
                  </button>
                </div>

                <div className="wi-tabs" role="group" aria-label="Image source">
                  {WEBSITE_IMAGE_MODES.map((tab) => (
                    <button
                      key={tab.mode}
                      type="button"
                      className={`wi-tab ${edit.mode === tab.mode ? "active" : ""}`}
                      onClick={() => switchMode(tab.mode)}
                      disabled={edit.saving}
                      aria-pressed={edit.mode === tab.mode}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                {edit.mode === "url" ? (
                  <input
                    className="wi-input"
                    type="url"
                    aria-label="Image URL"
                    placeholder="https://example.com/image.jpg"
                    value={edit.url}
                    onChange={(e) => updateEdit({ url: e.target.value, error: "" })}
                    disabled={edit.saving}
                  />
                ) : (
                  <div className="wi-file-row">
                    <input
                      type="file"
                      aria-label="Image file"
                      accept={IMAGE_ACCEPT}
                      onChange={handleFileChange}
                      disabled={edit.saving}
                    />
                    {edit.file && <small>{edit.file.name}</small>}
                  </div>
                )}

                {edit.error && (
                  <p className="wi-error" role="alert">
                    {edit.error}
                  </p>
                )}

                <div className="wi-actions">
                  <button type="button" className="wi-cancel" onClick={closeEditor} disabled={edit.saving}>
                    Cancel
                  </button>

                  <button type="button" className="wi-save" onClick={() => save(image)} disabled={edit.saving}>
                    {edit.saving ? "Saving..." : "Save"}
                  </button>
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

/* =========================
   SETTINGS
========================= */

function SettingsPage({ prefs, onSavePrefs, onResetPrefs, products, orders, appointments, username, onLogout, onNotice }) {
  const confirm = useConfirm();
  // Edits stay in a draft until "Save changes" is clicked
  const [draft, setDraft] = useState(prefs);
  const [formError, setFormError] = useState("");

  useEffect(() => {
    setDraft(prefs);
  }, [prefs]);

  const dirty = JSON.stringify(draft) !== JSON.stringify(prefs);
  const dataSources = { products, orders, appointments };

  const updateDraft = (key, value) => setDraft((prev) => ({ ...prev, [key]: value }));

  const saveSettings = () => {
    const problem = validatePrefs(draft);
    if (problem) {
      setFormError(problem);
      return;
    }

    setFormError("");
    onSavePrefs(sanitizePrefs(draft));
    onNotice("Settings saved.");
  };

  const discardChanges = () => {
    setDraft(prefs);
    setFormError("");
  };

  const resetSettings = async () => {
    const ok = await confirm({
      title: "Reset settings",
      message: "Reset all settings on this page to their default values?",
      confirmLabel: "Yes, reset",
      danger: true,
    });
    if (!ok) return;

    onResetPrefs();
    setFormError("");
    onNotice("Settings reset to default.");
  };

  const exportData = (target) => {
    const rows = (dataSources[target.source] || []).map(target.map);

    if (!downloadCsv(target.filename, rows)) {
      onNotice(`No ${target.source} to export.`);
      return;
    }

    onNotice(`${rows.length} ${target.source} exported.`);
  };

  return (
    <section className="admin-content">
      <PageHeading
        title="Settings"
        description="Customise the admin panel. Preferences are saved in this browser."
        action={
          <div className="admin-dashboard-actions">
            <button type="button" className="admin-secondary-button" onClick={discardChanges} disabled={!dirty}>
              Discard
            </button>
            <button type="button" className="admin-primary-button" onClick={saveSettings} disabled={!dirty}>
              Save changes
            </button>
          </div>
        }
      />

      <ErrorMessage>{formError}</ErrorMessage>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Inventory</h3>
            <p>Control how low stock and product lists behave.</p>
          </div>
        </div>

        <div className="admin-form-grid settings-grid">
          <FormGroup id="set-threshold" label="Low stock alert (units)">
            <input
              id="set-threshold"
              type="number"
              min="1"
              max="500"
              step="1"
              value={draft.lowStockThreshold}
              onChange={(e) => updateDraft("lowStockThreshold", e.target.value)}
            />
          </FormGroup>

          <FormGroup id="set-sort" label="Default product sort">
            <select
              id="set-sort"
              value={draft.defaultProductSort}
              onChange={(e) => updateDraft("defaultProductSort", e.target.value)}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </FormGroup>
        </div>
      </div>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Dashboard and safety</h3>
            <p>Choose what the dashboard shows and how careful deletes should be.</p>
          </div>
        </div>

        <div className="admin-form-grid settings-grid">
          <FormGroup id="set-rows" label="Rows in Product Overview">
            <select
              id="set-rows"
              value={draft.dashboardRows}
              onChange={(e) => updateDraft("dashboardRows", Number(e.target.value))}
            >
              {DASHBOARD_ROW_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n} rows
                </option>
              ))}
            </select>
          </FormGroup>

          <div className="admin-form-group">
            <label className="admin-checkbox-label">
              <input
                type="checkbox"
                checked={draft.confirmProductDelete}
                onChange={(e) => updateDraft("confirmProductDelete", e.target.checked)}
              />
              Ask before deleting a product
            </label>
          </div>
        </div>
      </div>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Export data</h3>
            <p>Download a copy of your store data as a CSV file (opens in Excel).</p>
          </div>
        </div>

        <div className="admin-dashboard-actions settings-export">
          {EXPORT_TARGETS.map((target) => (
            <button key={target.source} type="button" className="admin-secondary-button" onClick={() => exportData(target)}>
              <Download size={16} aria-hidden="true" />
              {target.label} ({(dataSources[target.source] || []).length})
            </button>
          ))}
        </div>
      </div>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Session</h3>
            <p>Signed in as {username}.</p>
          </div>

          <button type="button" className="admin-clear-button" onClick={onLogout}>
            <LogOut size={16} aria-hidden="true" />
            Logout
          </button>
        </div>
      </div>

      <div className="admin-table-card">
        <div className="admin-table-heading">
          <div>
            <h3>Reset</h3>
            <p>Restore all settings on this page to their default values.</p>
          </div>

          <button type="button" className="admin-secondary-button" onClick={resetSettings}>
            <RefreshCw size={16} aria-hidden="true" />
            Reset to default
          </button>
        </div>
      </div>
    </section>
  );
}

/* =========================
   ROOT COMPONENT
   Owns the session, shared data, navigation, notices, preferences and confirmations.
========================= */

export default function AdminDashboard() {
  const navigate = useNavigate();
  const token = sessionStorage.getItem("adminToken");
  const username = sessionStorage.getItem("adminUsername") || "Admin";

  const [activePage, setActivePage] = useState("Dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [dashboardRefreshing, setDashboardRefreshing] = useState(false);
  const [prefs, setPrefs] = useState(loadPrefs);

  const [lists, setLists] = useState({ products: [], orders: [], appointments: [], customers: [], images: [] });
  const [loading, setLoading] = useState({});

  const [banners, setBanners] = useState([]);
  const [offer, setOffer] = useState(null);
  const [marketingLoading, setMarketingLoading] = useState(false);

  const [confirmOptions, setConfirmOptions] = useState(null);
  const confirmResolver = useRef(null);

  const { products, orders, appointments, customers, images } = lists;

  // Opens the shared confirmation popup and resolves with the user's answer
  const askConfirm = useCallback(
    (options) =>
      new Promise((resolve) => {
        confirmResolver.current?.(false);
        confirmResolver.current = resolve;
        setConfirmOptions(options);
      }),
    []
  );

  const settleConfirm = (value) => {
    confirmResolver.current?.(value);
    confirmResolver.current = null;
    setConfirmOptions(null);
  };

  const savePrefs = (next) => {
    const clean = sanitizePrefs(next);
    setPrefs(clean);
    try {
      localStorage.setItem(PREFS_KEY, JSON.stringify(clean));
    } catch {
      // Storage full or blocked: the settings still apply for this session
    }
  };

  const resetPrefs = () => savePrefs(DEFAULT_PREFS);

  const endSession = useCallback(() => {
    sessionStorage.removeItem("adminToken");
    sessionStorage.removeItem("adminUsername");
    navigate("/admin", { replace: true });
  }, [navigate]);

  const logout = async () => {
    const ok = await askConfirm({
      title: "Log out",
      message: "Are you sure you want to log out?",
      confirmLabel: "Yes, log out",
    });
    if (ok) endSession();
  };

  const api = useAdminApi(token, endSession);
  const { getJson } = api;

  // Loads one list into state, with its own loading flag
  const loadList = useCallback(
    async (key) => {
      const { url, message } = LIST_SOURCES[key];

      setLoading((prev) => ({ ...prev, [key]: true }));
      try {
        const data = await getJson(url, message);
        if (data) setLists((prev) => ({ ...prev, [key]: data }));
      } catch (err) {
        setError(err.message || message);
      } finally {
        setLoading((prev) => ({ ...prev, [key]: false }));
      }
    },
    [getJson]
  );

  const loadMarketing = useCallback(async () => {
    setMarketingLoading(true);
    try {
      const [bannerData, offerData] = await Promise.all([
        getJson(ENDPOINTS.banners, "Failed to load marketing data"),
        getJson(ENDPOINTS.offers, "Failed to load marketing data"),
      ]);

      if (!bannerData || !offerData) return;

      setBanners(bannerData);
      setOffer(offerData[0] ?? null);
    } catch (err) {
      setError(err.message || "Failed to load marketing data");
    } finally {
      setMarketingLoading(false);
    }
  }, [getJson]);

  // Initial load, or redirect when there is no session
  useEffect(() => {
    if (!token) {
      navigate("/admin", { replace: true });
      return;
    }

    loadList("products");
    loadList("orders");
    loadList("appointments");
  }, [token, navigate, loadList]);

  // Notices disappear after a few seconds
  useEffect(() => {
    if (!notice) return undefined;
    const timer = setTimeout(() => setNotice(""), NOTICE_DURATION_MS);
    return () => clearTimeout(timer);
  }, [notice]);

  // Escape closes the mobile navigation drawer
  useEffect(() => {
    if (!sidebarOpen) return undefined;

    const onKey = (e) => {
      if (e.key === "Escape") setSidebarOpen(false);
    };

    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [sidebarOpen]);

  // The Marketing page also edits the website images, so it loads both
  const reloadImages = useCallback(() => loadList("images"), [loadList]);

  const reloaders = {
    Products: () => loadList("products"),
    Orders: () => loadList("orders"),
    Appointments: () => loadList("appointments"),
    Customers: () => Promise.all([loadList("customers"), loadList("orders")]),
    Marketing: () => Promise.all([loadMarketing(), reloadImages()]),
  };

  const refreshDashboard = async () => {
    setDashboardRefreshing(true);
    try {
      await Promise.all([reloaders.Products(), reloaders.Orders(), reloaders.Appointments()]);
    } finally {
      setDashboardRefreshing(false);
    }
  };

  const navigateTo = (page) => {
    setActivePage(page);
    setSidebarOpen(false);
    setError("");
    reloaders[page]?.();
  };

  const renderPage = () => {
    switch (activePage) {
      case "Dashboard":
        return (
          <DashboardPage
            api={api}
            products={products}
            orders={orders}
            appointments={appointments}
            refreshing={dashboardRefreshing}
            onRefresh={refreshDashboard}
            onViewProducts={() => navigateTo("Products")}
            onNotice={setNotice}
          />
        );
      case "Products":
        return (
          <ProductsPage
            api={api}
            products={products}
            loading={loading.products}
            onReload={reloaders.Products}
            onNotice={setNotice}
          />
        );
      case "Orders":
        return (
          <OrdersPage api={api} orders={orders} loading={loading.orders} onReload={reloaders.Orders} onNotice={setNotice} />
        );
      case "Appointments":
        return (
          <AppointmentsPage
            api={api}
            appointments={appointments}
            loading={loading.appointments}
            onReload={reloaders.Appointments}
            onNotice={setNotice}
          />
        );
      case "Customers":
        return (
          <CustomersPage
            orders={orders}
            customers={customers}
            loading={loading.customers}
            onReload={reloaders.Customers}
          />
        );
      case "Marketing":
        return (
          <MarketingPage
            api={api}
            banners={banners}
            offer={offer}
            loading={marketingLoading}
            onReload={loadMarketing}
            onNotice={setNotice}
            images={images}
            imagesLoading={loading.images}
            onReloadImages={reloadImages}
          />
        );
      case "Settings":
        return (
          <SettingsPage
            prefs={prefs}
            onSavePrefs={savePrefs}
            onResetPrefs={resetPrefs}
            products={products}
            orders={orders}
            appointments={appointments}
            username={username}
            onLogout={logout}
            onNotice={setNotice}
          />
        );
      default:
        return null;
    }
  };

  return (
    <ConfirmContext.Provider value={askConfirm}>
      <PrefsContext.Provider value={prefs}>
        <div className="admin-layout">
          {sidebarOpen && (
            <button
              type="button"
              className="admin-sidebar-overlay"
              aria-label="Close navigation menu"
              onClick={() => setSidebarOpen(false)}
            />
          )}

          <aside id="admin-sidebar" className={`admin-sidebar ${sidebarOpen ? "open" : ""}`}>
            <div className="admin-sidebar-brand">
              <h2>
                NMK <span>OPTICALS</span>
              </h2>
              <p>ADMIN PANEL</p>
            </div>

            <div className="admin-menu-label">MAIN MENU</div>

            <nav className="admin-navigation" aria-label="Admin">
              {MENU_ITEMS.map((item) => {
                const Icon = item.icon;
                const isActive = activePage === item.name;

                return (
                  <button
                    key={item.name}
                    type="button"
                    className={`admin-nav-item ${isActive ? "active" : ""}`}
                    aria-current={isActive ? "page" : undefined}
                    onClick={() => navigateTo(item.name)}
                  >
                    <Icon size={19} aria-hidden="true" />
                    <span>{item.name}</span>
                  </button>
                );
              })}
            </nav>

            <button type="button" className="admin-logout-button" onClick={logout}>
              <LogOut size={19} aria-hidden="true" />
              <span>Logout</span>
            </button>
          </aside>

          <main className="admin-main">
            <header className="admin-topbar">
              <button
                type="button"
                className="admin-mobile-menu"
                onClick={() => setSidebarOpen(true)}
                aria-label="Open navigation menu"
                aria-controls="admin-sidebar"
                aria-expanded={sidebarOpen}
              >
                <Menu size={23} aria-hidden="true" />
              </button>

              <div>
                <h2>{activePage}</h2>
                <p>Welcome back, {username}</p>
              </div>

              <div className="admin-profile">
                <div className="admin-avatar" aria-hidden="true">
                  {username.charAt(0).toUpperCase()}
                </div>
                <span>{username}</span>
              </div>
            </header>

            <div className="admin-page-messages">
              <ErrorMessage>{error}</ErrorMessage>
              {notice && (
                <div className="admin-success-message" role="status">
                  {notice}
                </div>
              )}
            </div>

            {renderPage()}
          </main>

          <ConfirmPopup options={confirmOptions} onResult={settleConfirm} />
        </div>
      </PrefsContext.Provider>
    </ConfirmContext.Provider>
  );
}
