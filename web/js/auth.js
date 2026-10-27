function obtenerUsuarioSesion() {
    const datos = sessionStorage.getItem("usuarioSesion");
    if (!datos) {
        window.location.href = "../index.html";
        return null;
    }
    return JSON.parse(datos);
}

function inicializarSesion() {
    const usuario = obtenerUsuarioSesion();
    if (!usuario) return;
    document.querySelectorAll("[data-nombre-usuario]").forEach(elemento => {
        elemento.textContent = usuario.nombreCompleto || usuario.nombreUsuario;
    });
    document.querySelectorAll("[data-rol-usuario]").forEach(elemento => {
        elemento.textContent = usuario.rol || "Usuario";
    });
    document.querySelectorAll("[data-cerrar-sesion]").forEach(elemento => {
        elemento.addEventListener("click", async () => {
            elemento.disabled = true;
            try {
                await fetch("../webresources/auth/google/logout", {
                    method: "GET",
                    cache: "no-store",
                    credentials: "same-origin"
                });
            } catch (error) {
                console.error("No se pudo cerrar la sesión del servidor.", error);
            }
            sessionStorage.removeItem("usuarioSesion");
            window.location.replace("../index.html");
        });
    });
}
