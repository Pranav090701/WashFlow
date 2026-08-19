import React, { useEffect, useMemo, useState } from "react";
import {
  API_BASE,
  blankCar,
  blankCustomerProfile,
  blankWasherProfile,
  createApi,
  decodeJwt,
  displayDate,
  displayTime,
  errorMessage,
  formatAmount,
  formatSubunits,
  loadRazorpay,
  readResponse,
  run,
  saveOrUpdate,
  toIsoDate,
} from "./lib.js";
import { DataTable, EmptyState, Field, Metric, Panel, SelectField, StatusChip, Summary, Tabs } from "./ui.jsx";

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem("carwash.jwt") || "");
  const [toast, setToast] = useState("");
  const [displayName, setDisplayName] = useState("");
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
    setDisplayName("");
    setToast(message);
  }

  const api = useMemo(() => createApi(token, () => clearSession()), [token]);

  function saveToken(nextToken) {
    localStorage.setItem("carwash.jwt", nextToken);
    setToken(nextToken);
  }

  if (!token) {
    return (
      <>
        {toast ? <Toast message={toast} /> : null}
        <AuthPanel onToken={saveToken} onToast={setToast} />
      </>
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <div className="brand-mark">W</div>
          <div>
            <p className="eyebrow">WashFlow</p>
            <h1>{role === "CUSTOMER" ? "Book a service" : role === "WASHER" ? "Washer workspace" : "Admin console"}</h1>
          </div>
        </div>
        <div className="session">
          <div className="profile-menu" title={displayName || claims?.email || "Profile"}>
            <span className="profile-dot">{initials(displayName || claims?.email || "Profile")}</span>
            {displayName ? <span className="session-name">Hi, {displayName}</span> : null}
          </div>
          <button className="button secondary" onClick={() => clearSession("Logged out")}>Logout</button>
        </div>
      </header>
      {toast ? <Toast message={toast} /> : null}
      <main className="workspace">
        {role === "CUSTOMER" ? <CustomerApp api={api} claims={claims} onToast={setToast} onDisplayName={setDisplayName} displayName={displayName} /> : null}
        {role === "WASHER" ? <WasherApp api={api} claims={claims} onToast={setToast} /> : null}
        {role === "ADMIN" ? <AdminApp api={api} onToast={setToast} /> : null}
      </main>
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
        onToast("Account created. Check your email to verify the account.");
        setMode("login");
      }
    } catch (error) {
      onToast(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-brand">
        <div className="brand-mark large">W</div>
        <p className="eyebrow">WashFlow</p>
        <h1>Fresh car care, booked around your day.</h1>
        <p className="auth-copy">Choose a nearby washer, pick a convenient time, and manage every booking from one calm workspace.</p>
        <div className="auth-showcase" aria-hidden="true">
          <div className="water-spray">
            <span />
            <span />
            <span />
            <span />
          </div>
          <div className="wash-lane">
            <span />
            <span />
            <span />
          </div>
          <div className="showcase-car">
            <div className="car-top" />
            <div className="car-body" />
            <div className="car-wheel left" />
            <div className="car-wheel right" />
          </div>
        </div>
      </section>
      <section className="auth-card">
        <div className="auth-card-head">
          <h2>{mode === "login" ? "Welcome back" : "Create account"}</h2>
          <p>{mode === "login" ? "Continue to your WashFlow account." : "Start with a customer or washer account."}</p>
        </div>
        <div className="segmented">
          <button type="button" className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
          <button type="button" className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
        </div>
        <form onSubmit={submit} className="form-grid">
          <Field label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} />
          <Field label="Password" type="password" value={form.password} onChange={(value) => update("password", value)} />
          {mode === "register" ? (
            <SelectField label="Role" value={form.role} onChange={(value) => update("role", value)}>
              <option value="CUSTOMER">Customer</option>
              <option value="WASHER">Washer</option>
            </SelectField>
          ) : null}
          <button className="button primary block" disabled={busy}>{busy ? "Working..." : mode === "login" ? "Login" : "Create Account"}</button>
        </form>
      </section>
    </main>
  );
}

function CustomerApp({ api, claims, onToast, onDisplayName, displayName }) {
  const [tab, setTab] = useState("book");

  useEffect(() => {
    let active = true;
    api.get(`/customerProfile/${claims.sub}`)
      .then((profile) => {
        if (active) onDisplayName(profile?.fullName || "");
      })
      .catch(() => {
        if (active) onDisplayName("");
      });
    return () => { active = false; };
  }, [api, claims.sub, onDisplayName]);

  return (
    <div className="customer-shell">
      <section className="welcome-strip">
        <div>
          <p className="panel-eyebrow">Welcome</p>
          <h2>{displayName ? `Hi ${displayName}, ready for a fresh ride?` : "Ready for a fresh ride?"}</h2>
        </div>
        <span>Book, pay, and track your wash in one place.</span>
      </section>
      <Tabs value={tab} onChange={setTab} items={[["book", "Book"], ["bookings", "Bookings"], ["profile", "Profile"]]} />
      {tab === "book" ? <BookTab api={api} claims={claims} onToast={onToast} /> : null}
      {tab === "bookings" ? <CustomerBookingsTab api={api} onToast={onToast} /> : null}
      {tab === "profile" ? <CustomerProfileTab api={api} claims={claims} onToast={onToast} /> : null}
    </div>
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
  const selectedWasherDetails = washers.find((washer) => washer.userId === selectedWasher);

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

  async function releaseSelectedSlot() {
    if (!selectedWasher || !selectedSlot) return;
    const query = new URLSearchParams({ washerId: selectedWasher, date: slotDate, startTime: selectedSlot });
    await api.delete(`/slots/lock?${query.toString()}`);
    await fetchSlots(false);
  }

  async function payNow() {
    await run(onToast, async () => {
      if (!selectedWasher || !selectedSlot) throw new Error("Select a washer and slot");

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
          description: `${displayDate(slotDate)} at ${selectedSlot}`,
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
    }, "Payment completed");
  }

  return (
    <div className={selectedWasher && selectedSlot ? "booking-layout with-checkout" : "booking-layout"}>
      <Panel title="Find and book" eyebrow="Customer booking" className="booking-primary">
        <form className="search-bar" onSubmit={findWashers}>
          <Field label="Area" value={search.serviceArea} onChange={(value) => setSearch({ ...search, serviceArea: value })} />
          <Field label="Pincode" value={search.pincode} onChange={(value) => setSearch({ ...search, pincode: value })} />
          <button className="button primary">Find Washers</button>
        </form>

        <div className="section-label">
          <span>Available washers</span>
          <strong>{washers.length}</strong>
        </div>
        <div className="washer-grid">
          {washers.length ? washers.map((washer) => (
            <button
              className={selectedWasher === washer.userId ? "washer-card selected" : "washer-card"}
              type="button"
              key={washer.userId}
              onClick={() => setSelectedWasher(washer.userId)}
            >
              <div className="avatar">{initials(washer.fullName || "Washer")}</div>
              <div>
                <strong>{washer.fullName || "Washer"}</strong>
                <span>{washer.serviceArea || "-"} - {washer.pincode || "-"}</span>
              </div>
              <div className="washer-meta">
                <span>{washer.experience ?? "-"} yrs</span>
                <span>{washer.averageRating ?? "New"} rating</span>
              </div>
            </button>
          )) : <EmptyState title="No washers loaded" text="Search by service area and pincode." />}
        </div>
      </Panel>

      <Panel title="Schedule" eyebrow="Available slots" className="booking-secondary">
        <div className="slot-controls">
          <SelectField label="Washer" value={selectedWasher} onChange={setSelectedWasher}>
            <option value="">Select washer</option>
            {washers.map((washer) => <option key={washer.userId} value={washer.userId}>{washer.fullName || washer.userId}</option>)}
          </SelectField>
          <Field label="Date" type="date" min={minBookDate} max={maxBookDate} value={slotDate} onChange={setSlotDate} />
          <button className="button secondary" type="button" onClick={() => fetchSlots(true)}>Load Slots</button>
        </div>
        <div className="slot-grid">
          {slots.length ? slots.map((slot) => (
            <button key={slot} type="button" className={slot === selectedSlot ? "slot active" : "slot"} onClick={() => setSelectedSlot(slot)}>
              {slot}
            </button>
          )) : <EmptyState title="No slots selected" text="Choose a washer and load slots." visual="car" />}
        </div>
      </Panel>

      {selectedWasher && selectedSlot ? <aside className="checkout-panel">
        <div className="checkout-card">
          <p className="panel-eyebrow">Checkout</p>
          <h2>Booking summary</h2>
          <div className="summary-line"><span>Washer</span><strong>{selectedWasherDetails?.fullName || "Not selected"}</strong></div>
          <div className="summary-line"><span>Date</span><strong>{displayDate(slotDate)}</strong></div>
          <div className="summary-line"><span>Slot</span><strong>{selectedSlot || "Not selected"}</strong></div>
          <div className="summary-line total"><span>Amount</span><strong>299.00 INR</strong></div>
          <button className="button primary block" type="button" disabled={!selectedWasher || !selectedSlot} onClick={payNow}>
            Pay With Razorpay
          </button>
          {lastPayment ? (
            <div className={lastPayment.uiStatus === "CHECKOUT_CANCELLED" ? "payment-state warning" : "payment-state"}>
              <StatusChip value={lastPayment.uiStatus || lastPayment.status} />
              <span>{formatSubunits(lastPayment.amountSubunits, lastPayment.currency)}</span>
            </div>
          ) : null}
          {paymentNotice ? <p className="muted small">{paymentNotice}</p> : null}
        </div>
      </aside> : null}
    </div>
  );
}

function CustomerBookingsTab({ api, onToast }) {
  const [bookings, setBookings] = useState([]);
  const [washerNames, setWasherNames] = useState({});
  const [ratingDrafts, setRatingDrafts] = useState({});
  const [submittedRatings, setSubmittedRatings] = useState({});

  async function loadBookings() {
    await run(onToast, async () => {
      const result = await api.get("/bookings/me");
      const bookingRows = Array.isArray(result) ? result : [];
      setBookings(bookingRows);
      await loadWasherNames(bookingRows);
      return result;
    }, "Bookings loaded");
  }

  async function loadWasherNames(bookingRows) {
    const missingWasherIds = [...new Set(bookingRows.map((booking) => booking.washerId).filter(Boolean))]
      .filter((washerId) => !washerNames[washerId]);

    if (!missingWasherIds.length) return;

    const profiles = await Promise.all(
      missingWasherIds.map(async (washerId) => {
        try {
          const profile = await api.get(`/washer/${washerId}`);
          return [washerId, profile?.fullName || "WashFlow washer"];
        } catch {
          return [washerId, "WashFlow washer"];
        }
      })
    );

    setWasherNames((current) => ({ ...current, ...Object.fromEntries(profiles) }));
  }

  useEffect(() => { loadBookings(); }, []);

  function updateRating(bookingId, field, value) {
    setRatingDrafts((current) => ({ ...current, [bookingId]: { ratingScore: 5, review: "", ...(current[bookingId] || {}), [field]: value } }));
  }

  async function submitRating(booking) {
    const draft = ratingDrafts[booking.bookingId] || { ratingScore: 5, review: "" };
    try {
      await api.post(`/washer/${booking.washerId}/ratings`, {
        bookingId: booking.bookingId,
        ratingScore: Number(draft.ratingScore || 5),
        review: draft.review || "",
      });
      setSubmittedRatings((current) => ({ ...current, [booking.bookingId]: true }));
      onToast("Thanks for rating your washer");
    } catch (error) {
      onToast(errorMessage(error));
    }
  }

  const confirmed = bookings.filter((booking) => booking.status === "CONFIRMED").length;
  const completed = bookings.filter((booking) => booking.status === "COMPLETED").length;

  return (
    <div className="screen-grid">
      <Panel title="Booked slots" eyebrow="Customer schedule" actions={<button className="button secondary" type="button" onClick={loadBookings}>Refresh</button>}>
        <div className="metrics-row">
          <Metric label="Total" value={bookings.length} />
          <Metric label="Confirmed" value={confirmed} />
          <Metric label="Completed" value={completed} />
        </div>
        <div className="booking-list polished">
          {bookings.length ? bookings.map((booking) => (
            <article className="booking-card" key={booking.bookingId}>
              <div className="booking-date">
                <strong>{displayDate(booking.date)}</strong>
                <span>{displayTime(booking.startTime)} - {displayTime(booking.endTime)}</span>
              </div>
              <div className="booking-main">
                <div>
                  <StatusChip value={booking.status} />
                  <h3>{formatAmount(booking.price)}</h3>
                </div>
                <p className="booking-washer-name">{washerNames[booking.washerId] || "WashFlow washer"}</p>
              </div>
              {booking.status === "COMPLETED" ? (
                submittedRatings[booking.bookingId] ? (
                  <div className="rating-thanks">Thanks for rating your washer.</div>
                ) : (
                  <div className="rating-panel">
                    <div>
                      <strong>Rate your washer</strong>
                      <span>How was the wash experience?</span>
                    </div>
                    <div className="face-rating" aria-label="Rate your washer">
                      {[
                        [1, "Bad", "😕"],
                        [2, "Okay", "🙂"],
                        [3, "Good", "😊"],
                        [4, "Great", "😄"],
                        [5, "Loved it", "🤩"],
                      ].map(([value, label, face]) => (
                        <button
                          key={value}
                          type="button"
                          className={(ratingDrafts[booking.bookingId]?.ratingScore || 5) === value ? "face active" : "face"}
                          onClick={() => updateRating(booking.bookingId, "ratingScore", value)}
                        >
                          <span>{face}</span>
                          <small>{label}</small>
                        </button>
                      ))}
                    </div>
                    <div className="review-row">
                      <Field label="Review" value={ratingDrafts[booking.bookingId]?.review || ""} onChange={(value) => updateRating(booking.bookingId, "review", value)} />
                      <button className="button primary" type="button" onClick={() => submitRating(booking)}>Submit Rating</button>
                    </div>
                  </div>
                )
              ) : null}
            </article>
          )) : <EmptyState title="No bookings yet" text="Confirmed bookings appear here after payment." />}
        </div>
      </Panel>
    </div>
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
    <div className="profile-layout">
      <Panel title="Profile" eyebrow="Customer account" className="profile-panel">
        <Tabs value={section} onChange={setSection} compact items={[["profile", "Details"], ["vehicles", "Vehicles"]]} />
        {section === "profile" ? (
          !editingProfile && profileLoaded ? (
            <div className="profile-summary">
              <div className="profile-avatar">{initials(profile.fullName || claims.email || "Customer")}</div>
              <div className="summary-grid">
                <Summary label="Full name" value={profile.fullName} />
                <Summary label="Phone" value={profile.phoneNumber} />
                <Summary label="Address" value={profile.address} />
                <Summary label="Email" value={claims.email} />
              </div>
              <button className="button secondary" type="button" onClick={() => setEditingProfile(true)}>Edit Profile</button>
            </div>
          ) : (
            <form className="form-grid two" onSubmit={saveProfile}>
              <Field label="Full name" value={profile.fullName} onChange={(value) => setProfile({ ...profile, fullName: value })} />
              <Field label="Phone" value={profile.phoneNumber} onChange={(value) => setProfile({ ...profile, phoneNumber: value })} />
              <Field label="Address" value={profile.address} onChange={(value) => setProfile({ ...profile, address: value })} />
              <Field label="Photo URL" value={profile.profilePictureUrl} onChange={(value) => setProfile({ ...profile, profilePictureUrl: value })} />
              <button className="button primary">Save Profile</button>
            </form>
          )
        ) : null}
        {section === "vehicles" ? (
          <div className="vehicles-view">
            <div className="panel-actions spaced">
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
            <div className="vehicle-grid">
              {cars.length ? cars.map((vehicle, index) => (
                <article className="vehicle-card" key={vehicle.id || vehicle.carId || vehicle.plateNumber || index}>
                  <div className="vehicle-icon">{String(vehicle.brand || "V").slice(0, 1).toUpperCase()}</div>
                  <div>
                    <strong>{vehicle.brand} {vehicle.model}</strong>
                    <span>{vehicle.color || "-"} - {vehicle.year || "-"}</span>
                    <code>{vehicle.plateNumber || "-"}</code>
                  </div>
                </article>
              )) : <EmptyState title="No vehicles" text="Saved vehicles will appear here." />}
            </div>
          </div>
        ) : null}
      </Panel>
    </div>
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
    <Panel title="Washer Profile" eyebrow="Service account">
      {!editing && loaded ? (
        <div className="summary-grid washer-summary">
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
    <Panel title="Availability" eyebrow="Washer status">
      <label className="switch-row">
        <input type="checkbox" checked={availability} onChange={(event) => setAvailability(event.target.checked)} />
        <span>{availability ? "Available" : "Unavailable"}</span>
      </label>
      <button className="button primary" type="button" onClick={() => run(onToast, () => api.patch(`/washer/availability?availability=${availability}`), "Availability updated")}>Update</button>
    </Panel>
  );
}

function WasherWorkTab({ api, onToast }) {
  const [bookings, setBookings] = useState([]);
  const [detailsByBooking, setDetailsByBooking] = useState({});
  const [openBookingId, setOpenBookingId] = useState("");

  async function loadWork() {
    await run(onToast, async () => {
      const result = await api.get("/bookings/washer/me");
      const rows = Array.isArray(result) ? result : [];
      setBookings(rows.filter(isUpcomingWasherBooking));
      return result;
    }, "Work loaded");
  }

  async function completeBooking(booking) {
    await run(onToast, async () => {
      await api.patch(`/bookings/${booking.bookingId}/complete`);
      await loadWork();
    }, "Booking completed");
  }

  async function toggleDetails(booking) {
    if (openBookingId === booking.bookingId) {
      setOpenBookingId("");
      return;
    }

    setOpenBookingId(booking.bookingId);
    if (detailsByBooking[booking.bookingId]) return;

    await run(onToast, async () => {
      const [profile, cars] = await Promise.all([
        api.get(`/customerProfile/${booking.customerId}`),
        api.get(`/cars/customer/${booking.customerId}`),
      ]);
      setDetailsByBooking((current) => ({
        ...current,
        [booking.bookingId]: {
          profile: profile || {},
          cars: Array.isArray(cars) ? cars : [],
        },
      }));
    }, "Booking details loaded");
  }

  useEffect(() => { loadWork(); }, []);

  return (
    <Panel title="Upcoming bookings" eyebrow="Assigned work" actions={<button className="button secondary" type="button" onClick={loadWork}>Refresh</button>}>
      <div className="booking-list polished">
        {bookings.length ? bookings.map((booking) => (
          <article className="booking-card washer-work-card" key={booking.bookingId}>
            <div className="booking-date">
              <strong>{displayDate(booking.date)}</strong>
              <span>{displayTime(booking.startTime)} - {displayTime(booking.endTime)}</span>
            </div>
            <div className="booking-main">
              <div>
                <StatusChip value={booking.status} />
                <h3>{formatAmount(booking.price)}</h3>
              </div>
              <p className="booking-washer-name">Customer appointment</p>
              <div className="work-actions">
                <button className="button secondary compact-action" type="button" onClick={() => toggleDetails(booking)}>
                  {openBookingId === booking.bookingId ? "Hide Details" : "Details"}
                </button>
                <button className="button primary compact-action" type="button" onClick={() => completeBooking(booking)}>Mark Completed</button>
              </div>
            </div>
            {openBookingId === booking.bookingId ? <WasherBookingDetails details={detailsByBooking[booking.bookingId]} /> : null}
          </article>
        )) : <EmptyState title="No upcoming bookings" text="Confirmed customer bookings will appear here." visual="car" />}
      </div>
    </Panel>
  );
}

function WasherBookingDetails({ details }) {
  if (!details) return <div className="booking-details-panel">Loading booking details...</div>;

  const profile = details.profile || {};
  const cars = details.cars || [];

  return (
    <div className="booking-details-panel">
      <div className="detail-grid">
        <Summary label="Customer" value={profile.fullName || "Customer"} />
        <Summary label="Phone" value={profile.phoneNumber} />
        <Summary label="Service address" value={profile.address} />
      </div>
      <div className="vehicle-grid compact-vehicles">
        {cars.length ? cars.map((vehicle, index) => (
          <article className="vehicle-card" key={vehicle.id || vehicle.plateNumber || index}>
            <div className="vehicle-icon">CAR</div>
            <div>
              <strong>{vehicle.brand} {vehicle.model}</strong>
              <span>{vehicle.color} · {vehicle.year}</span>
              <code>{vehicle.plateNumber}</code>
            </div>
          </article>
        )) : <EmptyState title="No vehicles saved" />}
      </div>
    </div>
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
  const [customerNames, setCustomerNames] = useState({});
  const [washerNames, setWasherNames] = useState({});

  async function loadBookings(nextStatus = status) {
    await run(onToast, async () => {
      const path = nextStatus ? `/admin/bookings/status/${nextStatus}` : "/admin/bookings";
      const result = await api.get(path);
      const rows = Array.isArray(result) ? result : [];
      setBookings(rows);
      await loadDisplayNames(api, rows, customerNames, setCustomerNames, washerNames, setWasherNames);
      return result;
    }, "Bookings loaded");
  }

  function changeStatus(value) {
    setStatus(value);
    loadBookings(value);
  }

  useEffect(() => { loadBookings(); }, []);
  return (
    <Panel title="Bookings" eyebrow="Admin operations" actions={<button className="button secondary" type="button" onClick={() => loadBookings()}>Refresh</button>}>
      <div className="toolbar compact">
        <SelectField label="Status" value={status} onChange={changeStatus}>
          <option value="">All</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </SelectField>
      </div>
      <DataTable rows={bookings} columns={[
        ["customer", "Customer", (row) => customerNames[row.customerId] || "Customer"],
        ["washer", "Washer", (row) => washerNames[row.washerId] || "Washer"],
        ["date", "Date", (row) => displayDate(row.date)],
        ["startTime", "Time", (row) => `${displayTime(row.startTime)} - ${displayTime(row.endTime)}`],
        ["status", "Status", (row) => <StatusChip value={row.status} />],
        ["price", "Amount", (row) => formatAmount(row.price)],
      ]} />
    </Panel>
  );
}

function AdminPayments({ api, onToast }) {
  const [payments, setPayments] = useState([]);
  const [status, setStatus] = useState("");
  const [customerNames, setCustomerNames] = useState({});
  const [washerNames, setWasherNames] = useState({});

  async function loadPayments(nextStatus = status) {
    await run(onToast, async () => {
      const path = nextStatus ? `/admin/payments/status/${nextStatus}` : "/admin/payments";
      const result = await api.get(path);
      const rows = Array.isArray(result) ? result : [];
      setPayments(rows);
      await loadDisplayNames(api, rows, customerNames, setCustomerNames, washerNames, setWasherNames);
      return result;
    }, "Payments loaded");
  }

  function changeStatus(value) {
    setStatus(value);
    loadPayments(value);
  }

  useEffect(() => { loadPayments(); }, []);
  return (
    <Panel title="Payments" eyebrow="Admin operations" actions={<button className="button secondary" type="button" onClick={() => loadPayments()}>Refresh</button>}>
      <div className="toolbar compact">
        <SelectField label="Status" value={status} onChange={changeStatus}>
          <option value="">All</option>
          <option value="INITIATED">Initiated</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILED">Failed / Cancelled</option>
          <option value="BOOKING_CONFIRM_FAILED">Booking confirm failed</option>
        </SelectField>
      </div>
      <DataTable rows={payments} columns={[
        ["customer", "Customer", (row) => customerNames[row.customerId] || row.customerEmail || "Customer"],
        ["washer", "Washer", (row) => washerNames[row.washerId] || "Washer"],
        ["date", "Date", (row) => displayDate(row.date)],
        ["slotTime", "Slot", (row) => displayTime(row.slotTime)],
        ["status", "Status", (row) => <StatusChip value={row.status} />],
        ["amount", "Amount", (row) => formatAmount(row.amount, row.currency)],
      ]} />
    </Panel>
  );
}

function AdminOperations({ api, onToast }) {
  return (
    <Panel title="Operations" eyebrow="Slot management">
      <button className="button primary" type="button" onClick={() => run(onToast, () => api.post("/slots/generate", {}), "Slots generated")}>Generate Slots</button>
    </Panel>
  );
}

async function loadDisplayNames(api, rows, customerNames, setCustomerNames, washerNames, setWasherNames) {
  const missingCustomerIds = [...new Set(rows.map((row) => row.customerId).filter(Boolean))]
    .filter((customerId) => !customerNames[customerId]);
  const missingWasherIds = [...new Set(rows.map((row) => row.washerId).filter(Boolean))]
    .filter((washerId) => !washerNames[washerId]);

  if (missingCustomerIds.length) {
    const profiles = await Promise.all(missingCustomerIds.map(async (customerId) => {
      try {
        const profile = await api.get(`/customerProfile/${customerId}`);
        return [customerId, profile?.fullName || profile?.email || "Customer"];
      } catch {
        return [customerId, "Customer"];
      }
    }));
    setCustomerNames((current) => ({ ...current, ...Object.fromEntries(profiles) }));
  }

  if (missingWasherIds.length) {
    const profiles = await Promise.all(missingWasherIds.map(async (washerId) => {
      try {
        const profile = await api.get(`/washer/${washerId}`);
        return [washerId, profile?.fullName || "Washer"];
      } catch {
        return [washerId, "Washer"];
      }
    }));
    setWasherNames((current) => ({ ...current, ...Object.fromEntries(profiles) }));
  }
}

function isUpcomingWasherBooking(booking) {
  if (booking.status !== "CONFIRMED") return false;
  if (!booking.date || !booking.startTime) return true;
  const start = new Date(`${booking.date}T${displayTime(booking.startTime)}:00`);
  return Number.isNaN(start.getTime()) || start >= new Date();
}

function Toast({ message }) {
  return <div className="toast">{message}</div>;
}

function initials(value) {
  return String(value || "")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "W";
}
