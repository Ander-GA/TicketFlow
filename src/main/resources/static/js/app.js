/* Contenido frontend generado por Claude (Anthropic) para el proyecto TicketFlow. */

const $ = s => document.querySelector(s);
const h = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const ROLES = { ADMIN: 'Administrador', ORGANIZADOR: 'Organizador', COMPRADOR: 'Comprador', VALIDADOR: 'Validador' };
const render = html => { $('#app').innerHTML = html; };

function toast(msg, err) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = err ? 'err' : '';
  t.hidden = false;
  clearTimeout(t._t);
  t._t = setTimeout(() => (t.hidden = true), 3500);
}

function errorHtml(e) {
  const err = e instanceof ApiError ? e : new ApiError(0, 'Ha ocurrido un error inesperado.', {});
  const det = Object.values(err.errores || {});
  return `<div class="aviso" role="alert"><strong>${h(err.mensaje)}</strong>` +
    (det.length ? `<ul>${det.map(d => `<li>${h(d)}</li>`).join('')}</ul>` : '') + '</div>';
}

/* Conecta un formulario con una acción asíncrona y pinta los errores del backend. */
function formulario(selector, accion) {
  const f = $(selector), salida = f.querySelector('.salida'), btn = f.querySelector('button');
  f.onsubmit = async ev => {
    ev.preventDefault();
    salida.innerHTML = '';
    btn.disabled = true;
    try { await accion(Object.fromEntries(new FormData(f)), f); }
    catch (e) { salida.innerHTML = errorHtml(e); }
    finally { btn.disabled = false; }
  };
}

const campo = (id, texto, extra = '') => `<label for="${id}">${texto}</label><input id="${id}" name="${id}" ${extra}>`;

/* ============ VISTAS ============ */
const views = {
  async eventos() {
    let lista = [], aviso = '';
    try {
      const r = await Api.eventos();
      lista = Array.isArray(r) ? r : (r.content || []);
    } catch (e) {
      aviso = e.status === 404 ? 'El catálogo de eventos todavía no está disponible en el servidor.' : e.mensaje;
    }
    render(`<h1>Próximos eventos</h1><p class="lead">Consulta los eventos publicados. Para reservar y comprar necesitas una cuenta.</p>` +
      (lista.length ? lista.map(e => {
        const d = new Date(e.fechaInicio), ok = !isNaN(d);
        return `<div class="stub"><div class="date"><span class="day">${ok ? d.getDate() : '–'}</span><span class="mon">${ok ? d.toLocaleDateString('es-ES', { month: 'short' }) : ''}</span></div>
          <div class="body"><h3>${h(e.nombre)}</h3><p>${h(e.lugar)}</p></div><div class="side"></div></div>`;
      }).join('') : `<div class="empty">${h(aviso || 'No hay eventos publicados por ahora.')}</div>`));
  },

  acceso() {
    render(`<h1>Iniciar sesión</h1>
      <form id="f" class="panel estrecho">
        ${campo('email', 'Correo electrónico', 'type="email" required autocomplete="email"')}
        ${campo('password', 'Contraseña', 'type="password" required autocomplete="current-password"')}
        <button>Iniciar sesión</button><div class="salida"></div>
      </form>
      <p>¿Todavía no tienes cuenta? <a href="#registro">Crear cuenta</a></p>`);
    formulario('#f', async d => {
      try {
        Api.guardar(await Api.login(d.email, d.password));
      } catch (e) {
        if (e.status === 401 || e.status === 403) throw new ApiError(401, 'Correo o contraseña incorrectos.', {});
        throw e;
      }
      toast('Sesión iniciada');
      location.hash = '#eventos';
    });
  },

  registro() {
    render(`<h1>Crear cuenta</h1>
      <form id="f" class="panel estrecho">
        ${campo('nombre', 'Nombre', 'required maxlength="100" autocomplete="given-name"')}
        ${campo('apellidos', 'Apellidos', 'required maxlength="150" autocomplete="family-name"')}
        ${campo('email', 'Correo electrónico', 'type="email" required maxlength="150" autocomplete="email"')}
        ${campo('password', 'Contraseña (mínimo 8 caracteres)', 'type="password" required minlength="8" maxlength="72" autocomplete="new-password"')}
        ${campo('telefono', 'Teléfono (opcional)', 'type="tel" maxlength="20" autocomplete="tel"')}
        <button>Crear cuenta</button><div class="salida"></div>
      </form>
      <p>¿Ya tienes cuenta? <a href="#acceso">Iniciar sesión</a></p>`);
    formulario('#f', async d => {
      await Api.registro({ ...d, telefono: d.telefono || null });
      toast('Cuenta creada');
      location.hash = '#acceso';
    });
  },

  async cuenta() {
    if (!Api.sesion) { location.hash = '#acceso'; return; }
    const u = await Api.perfil();
    render(`<h1>Mi cuenta</h1>
      <p class="lead">${h(u.email)} <span class="tag">${h(ROLES[u.rol] || u.rol)}</span></p>
      <form id="fp" class="panel estrecho"><h2>Datos personales</h2>
        ${campo('nombre', 'Nombre', `required maxlength="100" value="${h(u.nombre)}"`)}
        ${campo('apellidos', 'Apellidos', `required maxlength="150" value="${h(u.apellidos)}"`)}
        ${campo('telefono', 'Teléfono', `type="tel" maxlength="20" value="${h(u.telefono)}"`)}
        <button>Guardar cambios</button><div class="salida"></div>
      </form>
      <form id="fc" class="panel estrecho"><h2>Cambiar contraseña</h2>
        ${campo('passwordActual', 'Contraseña actual', 'type="password" required autocomplete="current-password"')}
        ${campo('passwordNueva', 'Nueva contraseña', 'type="password" required minlength="8" maxlength="72" autocomplete="new-password"')}
        <button>Cambiar contraseña</button><div class="salida"></div>
      </form>`);
    formulario('#fp', async d => {
      const r = await Api.actualizarPerfil({ ...d, telefono: d.telefono || null });
      Api.guardar({ ...Api.sesion, usuario: r });
      renderNav('cuenta');
      toast('Cambios guardados');
    });
    formulario('#fc', async (d, f) => {
      await Api.cambiarPassword(d.passwordActual, d.passwordNueva);
      f.reset();
      toast('Contraseña actualizada');
    });
  }
};

/* ============ NAVEGACIÓN ============ */
function renderNav(actual) {
  const a = (k, t) => `<a href="#${k}" ${actual === k ? 'aria-current="page"' : ''}>${t}</a>`;
  const s = Api.sesion;
  $('#nav').innerHTML = a('eventos', 'Eventos') + (s
    ? a('cuenta', `Mi cuenta (${h(s.usuario.nombre)})`) + '<a href="#" id="salir">Cerrar sesión</a>'
    : a('acceso', 'Iniciar sesión') + a('registro', 'Crear cuenta'));
  const o = $('#salir');
  if (o) o.onclick = e => { e.preventDefault(); Api.cerrar(); toast('Sesión cerrada'); location.hash = '#eventos'; ruta(); };
}

async function ruta() {
  const nombre = location.hash.slice(1) || 'eventos';
  renderNav(nombre);
  render('<p class="lead">Cargando…</p>');
  try { await (views[nombre] || views.eventos)(); }
  catch (e) { render(`<div class="empty">${h(e.mensaje || e.message)}</div>`); }
}

addEventListener('hashchange', ruta);
ruta();