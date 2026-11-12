/* Contenido frontend generado por Claude (Anthropic) para el proyecto TicketFlow.
   Única capa que conoce el backend: si cambia una ruta o un DTO, solo se toca este archivo. */

class ApiError extends Error {
  constructor(status, mensaje, errores) {
    super(mensaje);
    this.status = status;
    this.mensaje = mensaje;
    this.errores = errores || {};
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
      throw new ApiError(0, 'No se pudo conectar con el servidor.', {});
    }
    if (res.status === 204) return null;

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      // Token caducado o inválido en una ruta protegida: cerramos la sesión local.
      if (res.status === 401 && sesion && !path.startsWith('/auth')) {
        cerrar();
        location.hash = '#acceso';
      }
      throw new ApiError(res.status, data.mensaje || 'No se ha podido completar la operación.', data.errores);
    }
    return data;
  }

  function guardar(auth) {
    sesion = auth;
    sessionStorage.setItem('tf', JSON.stringify(auth));
  }
  function cerrar() {
    sesion = null;
    sessionStorage.removeItem('tf');
  }

  return {
    get sesion() { return sesion; },
    guardar,
    cerrar,
    // Auth y usuarios (existen en el backend)
    login: (email, password) => request('POST', '/auth/login', { email, password }),
    registro: datos => request('POST', '/usuarios/registro', datos),
    perfil: () => request('GET', '/usuarios/perfil'),
    actualizarPerfil: datos => request('PUT', '/usuarios/perfil', datos),
    cambiarPassword: (passwordActual, passwordNueva) =>
      request('PUT', '/usuarios/perfil/password', { passwordActual, passwordNueva }),
    // Eventos (la ruta GET ya es pública en SecurityConfig; el controlador aún no existe)
    eventos: () => request('GET', '/eventos')
  };
})();