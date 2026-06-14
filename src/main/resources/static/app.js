const output = document.querySelector("#output");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const tabs = document.querySelectorAll(".tab");
const meButton = document.querySelector("#me-button");
const logoutButton = document.querySelector("#logout-button");
const adminPanel = document.querySelector("#admin-panel");
const adminUsersButton = document.querySelector("#admin-users-button");
const adminUsersStatus = document.querySelector("#admin-users-status");
const adminUsersList = document.querySelector("#admin-users-list");

let accessToken = localStorage.getItem("access-token");
let refreshToken = localStorage.getItem("refresh-token");
let currentUser = null;

function show(data) {
    output.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

function setSession(data) {
    accessToken = data.accessToken;
    refreshToken = data.refreshToken || null;
    localStorage.setItem("access-token", accessToken);
    if (refreshToken) {
        localStorage.setItem("refresh-token", refreshToken);
    } else {
        localStorage.removeItem("refresh-token");
    }
    show(data);
    loadCurrentUser();
}

async function request(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
    }

    const response = await fetch(path, { ...options, headers });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || "Erro na requisicao.");
    }
    return data;
}

function formData(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    if (form.querySelector("[name='rememberMe']")) {
        data.rememberMe = data.rememberMe === "true";
    }
    return data;
}

function setCurrentUser(user) {
    currentUser = user;
    const isAdmin = currentUser?.role === "ADMIN";
    adminPanel.classList.toggle("hidden", !isAdmin);
    if (!isAdmin) {
        clearAdminUsers();
    }
}

function clearAdminUsers() {
    adminUsersList.innerHTML = "";
    adminUsersStatus.textContent = "Use sua sessao de administrador para carregar os usuarios.";
}

async function loadCurrentUser() {
    if (!accessToken) {
        setCurrentUser(null);
        return;
    }

    try {
        const user = await request("/api/users/me");
        setCurrentUser(user);
    } catch {
        setCurrentUser(null);
    }
}

function renderAdminUsers(users) {
    adminUsersList.innerHTML = "";

    if (!users.length) {
        adminUsersStatus.textContent = "Nenhum usuario cadastrado.";
        return;
    }

    adminUsersStatus.textContent = `${users.length} usuario(s) encontrado(s).`;
    users.forEach((user) => {
        const item = document.createElement("article");
        const details = document.createElement("div");
        const name = document.createElement("strong");
        const email = document.createElement("span");
        const role = document.createElement("span");

        item.className = "user-item";
        role.className = "role-badge";
        name.textContent = user.name;
        email.textContent = user.email;
        role.textContent = user.role;

        details.append(name, email);
        item.append(details, role);
        adminUsersList.appendChild(item);
    });
}

tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
        tabs.forEach((item) => item.classList.remove("active"));
        tab.classList.add("active");
        loginForm.classList.toggle("hidden", tab.dataset.tab !== "login");
        registerForm.classList.toggle("hidden", tab.dataset.tab !== "register");
    });
});

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        setSession(await request("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(formData(loginForm))
        }));
    } catch (error) {
        show(error.message);
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        setSession(await request("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(formData(registerForm))
        }));
    } catch (error) {
        show(error.message);
    }
});

meButton.addEventListener("click", async () => {
    try {
        const user = await request("/api/users/me");
        setCurrentUser(user);
        show(user);
    } catch (error) {
        show(error.message);
        setCurrentUser(null);
    }
});

adminUsersButton.addEventListener("click", async () => {
    adminUsersButton.disabled = true;
    adminUsersStatus.textContent = "Carregando usuarios...";
    adminUsersList.innerHTML = "";

    try {
        renderAdminUsers(await request("/api/admin/users"));
    } catch (error) {
        adminUsersStatus.textContent = error.message;
    } finally {
        adminUsersButton.disabled = false;
    }
});

logoutButton.addEventListener("click", async () => {
    if (refreshToken) {
        try {
            await request("/api/auth/logout", {
                method: "POST",
                body: JSON.stringify({ refreshToken })
            });
        } catch (error) {
            show(error.message);
        }
    }
    accessToken = null;
    refreshToken = null;
    setCurrentUser(null);
    localStorage.removeItem("access-token");
    localStorage.removeItem("refresh-token");
    show("Nenhum usuario autenticado.");
});

if (accessToken) {
    show("Token encontrado no navegador. Clique em Buscar /api/users/me.");
    loadCurrentUser();
} else {
    setCurrentUser(null);
}
