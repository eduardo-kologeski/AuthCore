const output = document.querySelector("#output");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const tabs = document.querySelectorAll(".tab");
const meButton = document.querySelector("#me-button");
const logoutButton = document.querySelector("#logout-button");

let accessToken = localStorage.getItem("access-token");
let refreshToken = localStorage.getItem("refresh-token");

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
        show(await request("/api/users/me"));
    } catch (error) {
        show(error.message);
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
    localStorage.removeItem("access-token");
    localStorage.removeItem("refresh-token");
    show("Nenhum usuario autenticado.");
});

if (accessToken) {
    show("Token encontrado no navegador. Clique em Buscar /api/users/me.");
}
