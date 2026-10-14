/* Shared helpers used by every page. */
const App = (() => {
    let currentUser = null;

    const $ = (id) => document.getElementById(id);
    const money = (n) => Number(n || 0).toFixed(2);
    const when = (iso) => iso ? new Date(iso).toLocaleString("en-FJ", { dateStyle: "medium", timeStyle: "short" }) : "";
    const day = (iso) => iso ? new Date(iso + (iso.length === 10 ? "T00:00:00" : "")).toLocaleDateString("en-FJ", { weekday: "short", day: "numeric", month: "short" }) : "";
    const time = (iso) => iso ? new Date(iso).toLocaleTimeString("en-FJ", { hour: "numeric", minute: "2-digit" }) : "";
    const label = (s) => String(s || "").replaceAll("_", " ").toLowerCase().replace(/^./, (c) => c.toUpperCase());

    /** Escapes text before putting it into HTML (stops script injection). */
    function esc(text) {
        const div = document.createElement("div");
        div.textContent = text == null ? "" : String(text);
        return div.innerHTML;
    }

    /** Calls the server. Errors carry the server's message, list and HTTP status. */
    async function api(method, url, body) {
        const options = { method, headers: {}, credentials: "same-origin" };
        if (body instanceof FormData) {
            options.body = body;
        } else if (body !== undefined) {
            options.headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        if (response.status === 204) return null;
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const err = new Error(data.error || "Something went wrong (" + response.status + ").");
            err.list = data.errors || [];
            err.status = response.status;
            throw err;
        }
        return data;
    }

    /** Shows a message box: type is "ok", "error" or "info". */
    function show(boxOrId, type, text, list) {
        const box = typeof boxOrId === "string" ? $(boxOrId) : boxOrId;
        if (!box) return;
        box.className = "msg " + type;
        box.innerHTML = esc(text) + (list && list.length > 1
            ? "<ul>" + list.map((e) => "<li>" + esc(e) + "</li>").join("") + "</ul>" : "");
    }

    function clear(boxOrId) {
        const box = typeof boxOrId === "string" ? $(boxOrId) : boxOrId;
        if (box) { box.className = "msg"; box.innerHTML = ""; }
    }

    /** Colour class for a status pill. */
    function pillClass(status) {
        if (["SUCCESS", "CONFIRMED", "DELIVERED", "ACTIVE"].includes(status)) return "pill good";
        if (["FAILED", "CANCELLED", "REFUNDED"].includes(status)) return "pill bad";
        if (["PARTIALLY_REFUNDED", "SCHEDULED", "DELIVERY_SCHEDULED", "SAVED"].includes(status)) return "pill warn";
        return "pill";
    }

    async function loadUser() {
        try { currentUser = await api("GET", "/api/auth/me"); }
        catch (e) { currentUser = null; }
        return currentUser;
    }

    /** Draws the top bar with links that suit who is logged in. */
    function renderHeader(active) {
        const header = document.querySelector(".site-header");
        if (!header) return;
        const u = currentUser;
        const link = (href, text, key) => `<a href="${href}"${key === active ? ' aria-current="page"' : ""}>${text}</a>`;
        header.innerHTML = `
            <div class="bar">
                <a class="brand" href="index.html">Bula Maleka Supermarket</a>
                <nav class="site-nav" aria-label="Main">
                    ${link("index.html", "Home", "home")}
                    ${link("cart.html", "Shop", "shop")}
                    ${u ? link("checkout.html", "Checkout", "checkout") : ""}
                    ${u ? link("account.html", "My account", "account") : ""}
                    ${u && u.role === "ADMIN" ? link("admin.html", "Staff", "admin") : ""}
                    ${u ? `<span class="who">Hi, ${esc(u.fullName.split(" ")[0])}</span><button type="button" id="logoutBtn">Log out</button>`
                        : link("login.html", "Log in / Register", "login")}
                </nav>
            </div>`;
        const logout = document.getElementById("logoutBtn");
        if (logout) logout.onclick = async () => {
            await api("POST", "/api/auth/logout").catch(() => {});
            location.href = "index.html";
        };
    }

    /** Sends visitors who are not logged in to the login page, then back here. */
    function requireLogin() {
        if (!currentUser) {
            location.href = "login.html?next=" + encodeURIComponent(location.pathname.replace(/^\//, "") + location.search);
            return false;
        }
        return true;
    }

    function newKey() {
        return (window.crypto && crypto.randomUUID) ? crypto.randomUUID()
            : Date.now().toString(36) + Math.random().toString(36).slice(2);
    }

    return {
        $, api, esc, money, when, day, time, label, show, clear, pillClass, loadUser, renderHeader, requireLogin, newKey,
        get user() { return currentUser; }
    };
})();
