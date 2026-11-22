/* Contenido frontend generado por Claude (Anthropic) para el proyecto TicketFlow.
   Única capa que conoce el backend: si cambia una ruta o un DTO, solo se toca este archivo. */

class ApiError extends Error {
  constructor(status, mensaje, errores = {}) {
    super(mensaje);
    this.status = status;
    this.mensaje = mensaje;
    this.errores = errores;
    this.detalle = errores.general || mensaje; // el backend pone el motivo concreto en errores.general
  }
}

const Api = (() => {
  const BASE = '/api/v1';
  let sesion = JSON.parse(sessionStorage.getItem('tf') || 'null'); // { accessToken, tokenType, usuario }

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (sesion) headers.Authorization = `${sesion.tokenType || 'Bearer'} ${sesion.accessToken}`;
    let res;
    try {
      res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    } catch {
      throw new ApiError(0, 'No se pudo conectar con el servidor.');
    }
    if (res.status === 204) return null;
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      if (res.status === 401 && sesion && !path.startsWith('/auth')) { cerrar(); location.hash = '#acceso'; }
      const msg = res.status === 403 ? 'No tienes permisos para realizar esta acción.'
        : data.mensaje || 'No se ha podido completar la operación.';
      throw new ApiError(res.status, msg, data.errores);
    }
    return data;
  }

  function guardar(auth) { sesion = auth; sessionStorage.setItem('tf', JSON.stringify(auth)); }
  function cerrar() { sesion = null; sessionStorage.removeItem('tf'); }

  return {
    get sesion() { return sesion; },
    guardar, cerrar,
    // Auth y usuarios
    login: (email, password) => request('POST', '/auth/login', { email, password }),
    registro: d => request('POST', '/usuarios/registro', d),
    perfil: () => request('GET', '/usuarios/perfil'),
    actualizarPerfil: d => request('PUT', '/usuarios/perfil', d),
    cambiarPassword: (passwordActual, passwordNueva) => request('PUT', '/usuarios/perfil/password', { passwordActual, passwordNueva }),
    // Eventos
    eventos: (f = {}) => {
      const q = new URLSearchParams(Object.entries(f).filter(([, v]) => v)).toString();
      return request('GET', '/eventos' + (q ? '?' + q : ''));
    },
    evento: id => request('GET', `/eventos/${id}`),
    misEventos: () => request('GET', '/eventos/mis-eventos'),
    crearEvento: d => request('POST', '/eventos', d),
    actualizarEvento: (id, d) => request('PUT', `/eventos/${id}`, d),
    cambiarEstado: (id, accion) => request('PATCH', `/eventos/${id}/${accion}`) // publicar | retirar | cancelar | finalizar
  };
})();