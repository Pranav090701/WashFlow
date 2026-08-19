export const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const blankCustomerProfile = { fullName: "", phoneNumber: "", address: "", profilePictureUrl: "" };
export const blankCar = { brand: "", model: "", color: "", year: new Date().getFullYear(), plateNumber: "" };
export const blankWasherProfile = {
  fullName: "",
  phoneNumber: "",
  profilePictureUrl: "",
  serviceArea: "",
  pincode: "",
  experience: 1,
  pricing: 299,
};

export function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

export function toIsoDate(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

export function formatAmount(value, currency = "INR") {
  if (value == null || value === "") return "-";
  const amount = Number(value);
  if (Number.isNaN(amount)) return `${value} ${currency}`;
  return `${amount.toFixed(2)} ${currency}`;
}

export function formatSubunits(value, currency = "INR") {
  if (!value) return currency;
  return `${(Number(value) / 100).toFixed(2)} ${currency}`;
}

export function displayDate(value) {
  if (!value) return "-";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
}

export function displayTime(value) {
  if (!value) return "-";
  return String(value).slice(0, 5);
}

export function errorMessage(error) {
  if (!error) return "Request failed";
  if (typeof error === "string") return error;
  return error.message || error.error || "Request failed";
}

export async function readResponse(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export function createApi(token, onUnauthorized) {
  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
    const data = await readResponse(response);
    if (!response.ok) {
      if (response.status === 401) onUnauthorized?.();
      throw data || { message: response.statusText };
    }
    return data;
  }

  return {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: "POST", body }),
    put: (path, body) => request(path, { method: "PUT", body }),
    patch: (path, body) => request(path, { method: "PATCH", body }),
    delete: (path) => request(path, { method: "DELETE" }),
  };
}

export function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve(window.Razorpay);
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(window.Razorpay);
    script.onerror = () => reject(new Error("Unable to load Razorpay Checkout"));
    document.body.appendChild(script);
  });
}

export async function run(onToast, action, successMessage) {
  try {
    await action();
    onToast(successMessage);
  } catch (error) {
    onToast(errorMessage(error));
  }
}

export async function saveOrUpdate(api, path, payload) {
  try {
    return await api.post(path, payload);
  } catch (error) {
    if (error?.status === 409 || String(error?.message || "").toLowerCase().includes("already")) {
      return api.put(path, payload);
    }
    throw error;
  }
}
