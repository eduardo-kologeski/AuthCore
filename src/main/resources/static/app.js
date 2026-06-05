const output = document.querySelector("#output");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const tabs = document.querySelectorAll(".tab");
const meButton = document.querySelector("#me-button");
const logoutButton = document.querySelector("#logout-button");

let token = localStorage.getItem("jwt-token");

function show(data) {
    output.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

function setSession(data) {
    token = data.token;
    localStorage.setItem("jwt-token", token);
    show(data);
}

async function request(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(path, { ...options, headers });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || "Erro na requisicao.");
    }
    return data;
}

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
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

logoutButton.addEventListener("click", () => {
    token = null;
    localStorage.removeItem("jwt-token");
    show("Nenhum usuario autenticado.");
});

if (token) {
    show("Token encontrado no navegador. Clique em Buscar /api/users/me.");
}
