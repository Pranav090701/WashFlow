import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const blankCustomerProfile = { fullName: "", phoneNumber: "", profilePictureUrl: "" };
const blankCar = { brand: "", model: "", color: "", year: new Date().getFullYear(), plateNumber: "" };
const blankWasherProfile = {
  fullName: "",
  phoneNumber: "",
  profilePictureUrl: "",
  serviceArea: "",
  pincode: "",
  experience: 1,
  pricing: 299,
};

function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

function toIsoDate(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

function formatAmount(value, currency = "INR") {
  if (value == null || value === "") return "-";
  const amount = Number(value);
  if (Number.isNaN(amount)) return `${value} ${currency}`;
  return `${amount.toFixed(2)} ${currency}`;
}

function formatSubunits(value, currency = "INR") {
  if (!value) return currency;
  return `${(Number(value) / 100).toFixed(2)} ${currency}`;
}

async function readResponse(response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function errorMessage(error) {
  if (!error) return "Request failed";
  if (typeof error === "string") return error;
  return error.message || error.error || "Request failed";
}

function useApi(token, onUnauthorized) {
  return useMemo(() => {
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
  }, [token, onUnauthorized]);
}

function loadRazorpay() {
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

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("carwash.jwt") || "");
  const [toast, setToast] = useState("");
  const claims = useMemo(() => decodeJwt(token), [token]);
  const role = claims?.role || "";

  useEffect(() => {
    if (!toast) return undefined;
    const timeoutId = window.setTimeout(() => setToast(""), 6000);
    return () => window.clearTimeout(timeoutId);
  }, [toast]);

  function clearSession(message = "Session expired. Please login again.") {
    localStorage.removeItem("carwash.jwt");
    setToken("");
    setToast(message);
  }

  const api = useApi(token, () => clearSession());

  function saveToken(nextToken) {
    localStorage.setItem("carwash.jwt", nextToken);
    setToken(nextToken);
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">WashFlow</p>
          <h1>{token ? "Dashboard" : "Sign in"}</h1>
        </div>
        {token ? (
          <div className="session">
            <span className="role-pill">{role}</span>
            <span>{claims?.email}</span>
            <button className="button secondary" onClick={() => clearSession("Logged out")}>Logout</button>
          </div>
        ) : null}
      </header>

      {toast ? <div className="toast">{toast}</div> : null}

      {!token ? (
        <AuthPanel onToken={saveToken} onToast={setToast} />
      ) : (
        <main className="workspace">
          {role === "CUSTOMER" ? <CustomerApp api={api} claims={claims} onToast={setToast} /> : null}
          {role === "WASHER" ? <WasherApp api={api} claims={claims} onToast={setToast} /> : null}
          {role === "ADMIN" ? <AdminApp api={api} onToast={setToast} /> : null}
        </main>
      )}
    </div>
  );
}

function AuthPanel({ onToken, onToast }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ email: "", password: "", role: "CUSTOMER" });
  const [busy, setBusy] = useState(false);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    try {
      const path = mode === "login" ? "/auth/login" : "/auth/register";
      const body = mode === "login" ? { email: form.email, password: form.password } : form;
      const response = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await readResponse(response);
      if (!response.ok) throw data;
      if (mode === "login") {
        onToken(data);
        onToast("Login successful");
      } else {
        onToast("Account created. Verify your email before login.");
        setMode("login");
      }
    } catch (error) {
      onToast(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-card">
        <div className="segmented">
          <button type="button" className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
          <button type="button" className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
        </div>
        <form onSubmit={submit} className="form-grid">
          <Field label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} />
          <Field label="Password" type="password" value={form.password} onChange={(value) => update("password", value)} />
          {mode === "register" ? (
            <label className="field">
              <span>Role</span>
              <select value={form.role} onChange={(event) => update("role", event.target.value)}>
                <option value="CUSTOMER">Customer</option>
                <option value="WASHER">Washer</option>
              </select>
            </label>
          ) : null}
          <button className="button primary" disabled={busy}>{busy ? "Working..." : mode === "login" ? "Login" : "Create Account"}</button>
        </form>
      </section>
    </main>
  );
}

function CustomerApp({ api, claims, onToast }) {
  const [tab, setTab] = useState("book");
  return (
    <>
      <Tabs value={tab} onChange={setTab} items={[["book", "Book"], ["bookings", "Bookings"], ["profile", "Profile"]]} />
      {tab === "book" ? <BookTab api={api} claims={claims} onToast={onToast} /> : null}
      {tab === "bookings" ? <CustomerBookingsTab api={api} onToast={onToast} /> : null}
      {tab === "profile" ? <CustomerProfileTab api={api} claims={claims} onToast={onToast} /> : null}
    </>
  );
}

function BookTab({ api, claims, onToast }) {
  const [search, setSearch] = useState({ serviceArea: "Pune", pincode: "411001" });
  const [washers, setWashers] = useState([]);
  const [selectedWasher, setSelectedWasher] = useState("");
  const [slotDate, setSlotDate] = useState(toIsoDate(1));
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState("");
  const [lastPayment, setLastPayment] = useState(null);
  const [paymentNotice, setPaymentNotice] = useState("");
  const minBookDate = toIsoDate(0);
  const maxBookDate = toIsoDate(2);

  async function findWashers(event) {
    event.preventDefault();
    await run(onToast, async () => {
      const result = await api.get(`/washer/by-area?serviceArea=${encodeURIComponent(search.serviceArea)}&pincode=${encodeURIComponent(search.pincode)}`);
      setWashers(Array.isArray(result) ? result : []);
      setSelectedWasher(result?.[0]?.userId || "");
      setSlots([]);
      setSelectedSlot("");
      return result;
    }, "Washers loaded");
  }

  async function fetchSlots(showToast = true) {
    if (!selectedWasher) {
      onToast("Select a washer");
      return;
    }
    const action = async () => {
      const result = await api.get(`/slots/available?washerId=${selectedWasher}&date=${slotDate}`);
      setSlots(Array.isArray(result) ? result : []);
      setSelectedSlot(result?.[0] || "");
      return result;
    };

    if (showToast) {
      await run(onToast, action, "Slots loaded");
    } else {
      await action();
    }
  }

  async function loadSlots() {
    await fetchSlots(true);
  }

  async function releaseSelectedSlot() {
    if (!selectedWasher || !selectedSlot) return;
    const query = new URLSearchParams({
      washerId: selectedWasher,
      date: slotDate,
      startTime: selectedSlot,
    });
    await api.delete(`/slots/lock?${query.toString()}`);
    await fetchSlots(false);
  }

  async function payNow() {
    await run(onToast, async () => {
      if (!selectedWasher || !selectedSlot) {
        throw new Error("Select a washer and slot");
      }

      await api.post("/slots/lock", { washerId: selectedWasher, date: slotDate, startTime: selectedSlot });
      let payment;
      try {
        payment = await api.post("/payments/initiate", { washerId: selectedWasher, date: slotDate, slotTime: selectedSlot });
        setLastPayment(payment);
        setPaymentNotice("Checkout opened");
      } catch (error) {
        await releaseSelectedSlot();
        throw error;
      }

      const Razorpay = await loadRazorpay().catch(async (error) => {
        await releaseSelectedSlot();
        throw error;
      });
      await new Promise((resolve, reject) => {
        const checkout = new Razorpay({
          key: payment.razorpayKeyId,
          amount: payment.amountSubunits,
          currency: payment.currency,
          name: "WashFlow",
          description: `${slotDate} ${selectedSlot}`,
          order_id: payment.razorpayOrderId,
          prefill: { email: claims?.email || "" },
          handler: async (response) => {
            try {
              const verified = await api.post("/payments/verify", {
                paymentId: payment.paymentId,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });
              setLastPayment(verified);
              setPaymentNotice("Payment verified");
              resolve(verified);
            } catch (error) {
              reject(error);
            }
          },
          modal: {
            ondismiss: () => {
              setPaymentNotice("Checkout cancelled");
              setLastPayment((current) => current ? { ...current, uiStatus: "CHECKOUT_CANCELLED" } : current);
              releaseSelectedSlot().catch(() => {});
              reject(new Error("Payment cancelled."));
            },
          },
        });
        checkout.open();
      });
      return payment;
    }, "Payment completed");
  }

  return (
    <div className="page-grid">
      <Panel title="Find Washers" wide>
        <form className="toolbar" onSubmit={findWashers}>
          <Field label="Area" value={search.serviceArea} onChange={(value) => setSearch({ ...search, serviceArea: value })} />
          <Field label="Pincode" value={search.pincode} onChange={(value) => setSearch({ ...search, pincode: value })} />
          <button className="button primary">Search</button>
        </form>
        <DataTable rows={washers} columns={[["fullName", "Washer"], ["serviceArea", "Area"], ["pincode", "Pincode"], ["experience", "Experience"], ["averageRating", "Rating"]]} onSelect={(row) => setSelectedWasher(row.userId)} />
      </Panel>

      <Panel title="Select Slot" wide>
        <div className="toolbar">
          <label className="field">
            <span>Washer</span>
            <select value={selectedWasher} onChange={(event) => setSelectedWasher(event.target.value)}>
              <option value="">Select washer</option>
              {washers.map((washer) => <option key={washer.userId} value={washer.userId}>{washer.fullName || washer.userId}</option>)}
            </select>
          </label>
          <Field label="Date" type="date" min={minBookDate} max={maxBookDate} value={slotDate} onChange={setSlotDate} />
          <button className="button secondary" type="button" onClick={loadSlots}>Load Slots</button>
        </div>
        <div className="slot-grid">
          {slots.map((slot) => <button key={slot} type="button" className={slot === selectedSlot ? "slot active" : "slot"} onClick={() => setSelectedSlot(slot)}>{slot}</button>)}
        </div>
        <div className="toolbar compact">
          <button className="button primary" type="button" disabled={!selectedWasher || !selectedSlot} onClick={payNow}>Pay With Razorpay</button>
          {lastPayment ? <span className={lastPayment.uiStatus === "CHECKOUT_CANCELLED" ? "pill warning" : "pill"}>{lastPayment.uiStatus || lastPayment.status} - {formatSubunits(lastPayment.amountSubunits, lastPayment.currency)}</span> : null}
          {paymentNotice ? <span className="muted small">{paymentNotice}</span> : null}
        </div>
      </Panel>
    </div>
  );
}

function CustomerBookingsTab({ api, onToast }) {
  const [bookings, setBookings] = useState([]);
  const [ratingDrafts, setRatingDrafts] = useState({});

  async function loadBookings() {
    await run(onToast, async () => {
      const result = await api.get("/bookings/me");
      setBookings(Array.isArray(result) ? result : []);
      return result;
    }, "Bookings loaded");
  }

  useEffect(() => { loadBookings(); }, []);

  function updateRating(bookingId, field, value) {
    setRatingDrafts((current) => ({ ...current, [bookingId]: { ratingScore: 5, review: "", ...(current[bookingId] || {}), [field]: value } }));
  }

  async function submitRating(booking) {
    const draft = ratingDrafts[booking.bookingId] || { ratingScore: 5, review: "" };
    await run(onToast, () => api.post(`/washer/${booking.washerId}/ratings`, {
      bookingId: booking.bookingId,
      ratingScore: Number(draft.ratingScore || 5),
      review: draft.review || "",
    }), "Rating submitted");
  }

  return (
    <Panel title="Booked Slots" wide>
      <div className="toolbar compact"><button className="button secondary" type="button" onClick={loadBookings}>Refresh</button></div>
      <div className="booking-list">
        {bookings.length ? bookings.map((booking) => (
          <article className="booking-row" key={booking.bookingId}>
            <div><strong>{booking.date} at {booking.startTime}</strong><span>{booking.status} - {formatAmount(booking.price)}</span></div>
            <code>{booking.bookingId}</code>
            {booking.status === "COMPLETED" ? (
              <div className="rating-row">
                <Field label="Score" type="number" value={ratingDrafts[booking.bookingId]?.ratingScore || 5} onChange={(value) => updateRating(booking.bookingId, "ratingScore", value)} />
                <Field label="Review" value={ratingDrafts[booking.bookingId]?.review || ""} onChange={(value) => updateRating(booking.bookingId, "review", value)} />
                <button className="button primary" type="button" onClick={() => submitRating(booking)}>Rate</button>
              </div>
            ) : null}
          </article>
        )) : <div className="empty">No bookings</div>}
      </div>
    </Panel>
  );
}

function CustomerProfileTab({ api, claims, onToast }) {
  const [section, setSection] = useState("profile");
  const [profile, setProfile] = useState(blankCustomerProfile);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);
  const [cars, setCars] = useState([]);
  const [car, setCar] = useState(blankCar);
  const [showCarForm, setShowCarForm] = useState(false);

  async function loadProfile() {
    try {
      const result = await api.get(`/customerProfile/${claims.sub}`);
      setProfile({ fullName: result.fullName || "", phoneNumber: result.phoneNumber || "", profilePictureUrl: result.profilePictureUrl || "" });
      setProfileLoaded(true);
      setEditingProfile(false);
    } catch {
      setProfileLoaded(false);
      setEditingProfile(true);
    }
  }

  async function loadCars() {
    await run(onToast, async () => {
      const result = await api.get("/cars");
      setCars(Array.isArray(result) ? result : []);
      return result;
    }, "Vehicles loaded");
  }

  useEffect(() => { loadProfile(); loadCars(); }, []);

  async function saveProfile(event) {
    event.preventDefault();
    await run(onToast, async () => { await saveOrUpdate(api, "/customerProfile", profile); await loadProfile(); }, "Profile saved");
  }

  async function addCar(event) {
    event.preventDefault();
    await run(onToast, async () => {
      await api.post("/cars", { ...car, year: Number(car.year) });
      setCar(blankCar);
      setShowCarForm(false);
      await loadCars();
    }, "Vehicle added");
  }

  return (
    <Panel title="Profile" wide>
      <Tabs value={section} onChange={setSection} small items={[["profile", "Details"], ["vehicles", "Vehicles"]]} />
      {section === "profile" ? (
        !editingProfile && profileLoaded ? (
          <div className="summary-grid">
            <Summary label="Full name" value={profile.fullName} />
            <Summary label="Phone" value={profile.phoneNumber} />
            <Summary label="Photo URL" value={profile.profilePictureUrl || "-"} />
            <button className="button secondary" type="button" onClick={() => setEditingProfile(true)}>Edit</button>
          </div>
        ) : (
          <form className="form-grid two" onSubmit={saveProfile}>
            <Field label="Full name" value={profile.fullName} onChange={(value) => setProfile({ ...profile, fullName: value })} />
            <Field label="Phone" value={profile.phoneNumber} onChange={(value) => setProfile({ ...profile, phoneNumber: value })} />
            <Field label="Photo URL" value={profile.profilePictureUrl} onChange={(value) => setProfile({ ...profile, profilePictureUrl: value })} />
            <button className="button primary">Save</button>
          </form>
        )
      ) : null}
      {section === "vehicles" ? (
        <div>
          <div className="toolbar compact">
            <button className="button secondary" type="button" onClick={loadCars}>Refresh</button>
            <button className="button primary" type="button" onClick={() => setShowCarForm((value) => !value)}>{showCarForm ? "Close" : "Add Vehicle"}</button>
          </div>
          {showCarForm ? (
            <form className="form-grid two inline-form" onSubmit={addCar}>
              <Field label="Brand" value={car.brand} onChange={(value) => setCar({ ...car, brand: value })} />
              <Field label="Model" value={car.model} onChange={(value) => setCar({ ...car, model: value })} />
              <Field label="Color" value={car.color} onChange={(value) => setCar({ ...car, color: value })} />
              <Field label="Year" type="number" value={car.year} onChange={(value) => setCar({ ...car, year: value })} />
              <Field label="Plate number" value={car.plateNumber} onChange={(value) => setCar({ ...car, plateNumber: value })} />
              <button className="button primary">Save Vehicle</button>
            </form>
          ) : null}
          <DataTable rows={cars} columns={[["brand", "Brand"], ["model", "Model"], ["color", "Color"], ["year", "Year"], ["plateNumber", "Plate"]]} />
        </div>
      ) : null}
    </Panel>
  );
}

function WasherApp({ api, claims, onToast }) {
  const [tab, setTab] = useState("profile");
  return (
    <>
      <Tabs value={tab} onChange={setTab} items={[["profile", "Profile"], ["availability", "Availability"], ["work", "Work"]]} />
      {tab === "profile" ? <WasherProfileTab api={api} claims={claims} onToast={onToast} /> : null}
      {tab === "availability" ? <WasherAvailabilityTab api={api} claims={claims} onToast={onToast} /> : null}
      {tab === "work" ? <WasherWorkTab api={api} onToast={onToast} /> : null}
    </>
  );
}

function WasherProfileTab({ api, claims, onToast }) {
  const [profile, setProfile] = useState(blankWasherProfile);
  const [editing, setEditing] = useState(false);
  const [loaded, setLoaded] = useState(false);

  async function loadProfile() {
    try {
      const result = await api.get(`/washer/${claims.sub}`);
      setProfile({
        fullName: result.fullName || "",
        phoneNumber: result.phoneNumber || "",
        profilePictureUrl: result.profilePictureUrl || "",
        serviceArea: result.serviceArea || "",
        pincode: result.pincode || "",
        experience: result.experience || 1,
        pricing: result.pricing || 299,
      });
      setLoaded(true);
      setEditing(false);
    } catch {
      setLoaded(false);
      setEditing(true);
    }
  }

  useEffect(() => { loadProfile(); }, []);

  async function saveProfile(event) {
    event.preventDefault();
    await run(onToast, async () => {
      await saveOrUpdate(api, "/washer", { ...profile, experience: Number(profile.experience), pricing: Number(profile.pricing) });
      await loadProfile();
    }, "Profile saved");
  }

  return (
    <Panel title="Washer Profile" wide>
      {!editing && loaded ? (
        <div className="summary-grid">
          <Summary label="Full name" value={profile.fullName} />
          <Summary label="Area" value={profile.serviceArea} />
          <Summary label="Pincode" value={profile.pincode} />
          <Summary label="Experience" value={`${profile.experience} years`} />
          <Summary label="Display price" value={formatAmount(profile.pricing)} />
          <button className="button secondary" type="button" onClick={() => setEditing(true)}>Edit</button>
        </div>
      ) : (
        <form className="form-grid two" onSubmit={saveProfile}>
          <Field label="Full name" value={profile.fullName} onChange={(value) => setProfile({ ...profile, fullName: value })} />
          <Field label="Phone" value={profile.phoneNumber} onChange={(value) => setProfile({ ...profile, phoneNumber: value })} />
          <Field label="Area" value={profile.serviceArea} onChange={(value) => setProfile({ ...profile, serviceArea: value })} />
          <Field label="Pincode" value={profile.pincode} onChange={(value) => setProfile({ ...profile, pincode: value })} />
          <Field label="Experience" type="number" value={profile.experience} onChange={(value) => setProfile({ ...profile, experience: value })} />
          <Field label="Display price" type="number" value={profile.pricing} onChange={(value) => setProfile({ ...profile, pricing: value })} />
          <Field label="Photo URL" value={profile.profilePictureUrl} onChange={(value) => setProfile({ ...profile, profilePictureUrl: value })} />
          <button className="button primary">Save</button>
        </form>
      )}
    </Panel>
  );
}

function WasherAvailabilityTab({ api, claims, onToast }) {
  const [availability, setAvailability] = useState(true);
  return (
    <Panel title="Availability">
      <label className="switch-row">
        <input type="checkbox" checked={availability} onChange={(event) => setAvailability(event.target.checked)} />
        <span>{availability ? "Available" : "Unavailable"}</span>
      </label>
      <button className="button primary" type="button" onClick={() => run(onToast, () => api.patch(`/washer/availability?availability=${availability}`), "Availability updated")}>Update</button>
      <p className="muted small">Washer ID: {claims?.sub}</p>
    </Panel>
  );
}

function WasherWorkTab({ api, onToast }) {
  const [bookingId, setBookingId] = useState("");
  return (
    <Panel title="Complete Booking">
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); run(onToast, () => api.patch(`/bookings/${bookingId}/complete`), "Booking completed"); }}>
        <Field label="Booking ID" value={bookingId} onChange={setBookingId} />
        <button className="button primary">Mark Completed</button>
      </form>
    </Panel>
  );
}

function AdminApp({ api, onToast }) {
  const [tab, setTab] = useState("bookings");
  return (
    <>
      <Tabs value={tab} onChange={setTab} items={[["bookings", "Bookings"], ["payments", "Payments"], ["operations", "Operations"]]} />
      {tab === "bookings" ? <AdminBookings api={api} onToast={onToast} /> : null}
      {tab === "payments" ? <AdminPayments api={api} onToast={onToast} /> : null}
      {tab === "operations" ? <AdminOperations api={api} onToast={onToast} /> : null}
    </>
  );
}

function AdminBookings({ api, onToast }) {
  const [bookings, setBookings] = useState([]);
  const [status, setStatus] = useState("");
  async function loadBookings() {
    await run(onToast, async () => {
      const path = status ? `/admin/bookings/status/${status}` : "/admin/bookings";
      const result = await api.get(path);
      setBookings(Array.isArray(result) ? result : []);
      return result;
    }, "Bookings loaded");
  }
  useEffect(() => { loadBookings(); }, []);
  return (
    <Panel title="Bookings" wide>
      <div className="toolbar compact">
        <label className="field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All</option><option value="CONFIRMED">Confirmed</option><option value="COMPLETED">Completed</option><option value="CANCELLED">Cancelled</option></select></label>
        <button className="button secondary" type="button" onClick={loadBookings}>Refresh</button>
      </div>
      <DataTable rows={bookings} columns={[["bookingId", "Booking"], ["customerId", "Customer"], ["washerId", "Washer"], ["date", "Date"], ["startTime", "Time"], ["status", "Status"], ["price", "Price"]]} />
    </Panel>
  );
}

function AdminPayments({ api, onToast }) {
  const [payments, setPayments] = useState([]);
  const [status, setStatus] = useState("");
  async function loadPayments() {
    await run(onToast, async () => {
      const path = status ? `/admin/payments/status/${status}` : "/admin/payments";
      const result = await api.get(path);
      setPayments(Array.isArray(result) ? result : []);
      return result;
    }, "Payments loaded");
  }
  useEffect(() => { loadPayments(); }, []);
  return (
    <Panel title="Payments" wide>
      <div className="toolbar compact">
        <label className="field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All</option><option value="INITIATED">Initiated</option><option value="SUCCESS">Success</option><option value="FAILED">Failed</option><option value="BOOKING_CONFIRM_FAILED">Booking confirm failed</option></select></label>
        <button className="button secondary" type="button" onClick={loadPayments}>Refresh</button>
      </div>
      <DataTable rows={payments} columns={[["paymentId", "Payment"], ["customerId", "Customer"], ["washerId", "Washer"], ["date", "Date"], ["slotTime", "Time"], ["status", "Status"], ["amount", "Amount"], ["bookingId", "Booking"]]} />
    </Panel>
  );
}

function AdminOperations({ api, onToast }) {
  return (
    <Panel title="Operations">
      <button className="button primary" type="button" onClick={() => run(onToast, () => api.post("/slots/generate", {}), "Slots generated")}>Generate Slots</button>
    </Panel>
  );
}

async function run(onToast, action, successMessage) {
  try {
    await action();
    onToast(successMessage);
  } catch (error) {
    onToast(errorMessage(error));
  }
}

async function saveOrUpdate(api, path, payload) {
  try {
    return await api.post(path, payload);
  } catch (error) {
    if (error?.status === 409 || String(error?.message || "").toLowerCase().includes("already")) {
      return api.put(path, payload);
    }
    throw error;
  }
}

function Tabs({ value, onChange, items, small }) {
  return (
    <nav className={small ? "tabs small-tabs" : "tabs"}>
      {items.map(([key, label]) => <button key={key} type="button" className={value === key ? "active" : ""} onClick={() => onChange(key)}>{label}</button>)}
    </nav>
  );
}

function Panel({ title, children, wide }) {
  return <section className={wide ? "panel wide" : "panel"}><h2>{title}</h2>{children}</section>;
}

function Field({ label, value, onChange, type = "text", ...inputProps }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} {...inputProps} />
    </label>
  );
}

function Summary({ label, value }) {
  return <div className="summary-item"><span>{label}</span><strong>{value || "-"}</strong></div>;
}

function DataTable({ rows, columns, onSelect }) {
  if (!rows?.length) return <div className="empty">No records</div>;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{columns.map(([key, label]) => <th key={key}>{label}</th>)}</tr></thead>
        <tbody>{rows.map((row, index) => <tr key={row.id || row.userId || row.bookingId || row.paymentId || index} onClick={() => onSelect?.(row)}>{columns.map(([key]) => <td key={key}>{String(row[key] ?? "-")}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
