const API_BASE = "../webresources/generic";

async function solicitarAPI(ruta, opciones) {
    const respuesta = await fetch(`${API_BASE}/${ruta}`, opciones);
    if (!respuesta.ok) {
        throw new Error(`El servidor respondió con ${respuesta.status}`);
    }
    return respuesta.json();
}

function escaparHTML(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, caracter => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;"
    }[caracter]));
}
