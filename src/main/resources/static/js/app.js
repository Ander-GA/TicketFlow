/* Contenido frontend generado por Claude (Anthropic) para el proyecto TicketFlow. */

const $ = s => document.querySelector(s);
const h = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const render = html => { $('#app').innerHTML = html; };
const rol = () => Api.sesion?.usuario.rol;

const ROLES = { ADMIN: 'Administrador', ORGANIZADOR: 'Organizador', COMPRADOR: 'Comprador', VALIDADOR: 'Validador' };
const ESTADOS = { BORRADOR: ['Borrador', ''], PUBLICADO: ['Publicado', 'ok'], CANCELADO: ['Cancelado', 'bad'], FINALIZADO: ['Finalizado', ''] };
const MENSAJES = { publicar: 'Evento publicado', retirar: 'Evento retirado de la venta', cancelar: 'Evento cancelado', finalizar: 'Evento finalizado' };

const fecha = (iso, o) => iso ? new Date(iso).toLocaleString('es-ES', o) : '';
const dia = i => fecha(i, { day: 'numeric' });
const mes = i => fecha(i, { month: 'short' }).replace('.', '');
const largo = i => fecha(i, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });

function toast(msg, err) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = err ? 'err' : '';
  t.hidden = false;
  clearTimeout(t._t);
  t._t = setTimeout(() => (t.hidden = true), 3500);
}

function errorHtml(e) {
  const err = e instanceof ApiError ? e : new ApiError(0, 'Ha ocurrido un error inesperado.');
  const det = Object.entries(err.errores || {});
  if (det.length === 1 && det[0][0] === 'general') return `<div class="aviso" role="alert"><strong>${h(err.detalle)}</strong></div>`;
  return `<div class="aviso" role="alert"><strong>${h(err.mensaje)}</strong>` +
    (det.length ? `<ul>${det.map(([, d]) => `<li>${h(d)}</li>`).join('')}</ul>` : '') + '</div>';
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

const campo = (id, texto, attrs = '', val = '') =>
  `<div class="campo"><label for="${id}">${texto}</label><input id="${id}" name="${id}" value="${h(val)}" ${attrs}></div>`;
const area = (id, texto, attrs = '', val = '') =>
  `<div class="campo"><label for="${id}">${texto}</label><textarea id="${id}" name="${id}" ${attrs}>${h(val)}</textarea></div>`;

const tarjeta = e => `<a class="tk" href="#evento/${e.id}">
  <div class="foto">${e.imagen ? `<img src="${h(e.imagen)}" alt="" loading="lazy" onerror="this.remove()">` : ''}</div>
  <div class="corte"></div>
  <div class="info"><div class="fecha"><b>${dia(e.fechaInicio)}</b><span>${mes(e.fechaInicio)}</span></div>
  <div><h3>${h(e.titulo)}</h3><p>${h(e.ubicacion)}</p></div></div></a>`;

/* ============ VISTAS ============ */
let filtros = { titulo: '', ubicacion: '' };

async function listar() {
  const c = $('#lista');
  c.innerHTML = '<p class="lead">Cargando…</p>';
  try {
    const l = await Api.eventos(filtros);
    c.innerHTML = l.length ? `<div class="grid">${l.map(tarjeta).join('')}</div>`
      : `<div class="empty">No hay eventos que coincidan con tu búsqueda.</div>`;
  } catch (e) { c.innerHTML = `<div class="empty">${h(e.detalle)}</div>`; }
}

const views = {
  async eventos() {
    render(`<h1>Encuentra tu próximo evento</h1>
      <p class="lead">Busca por nombre o por lugar. Para organizar eventos necesitas una cuenta de organizador.</p>
      <form id="fb" class="buscador" role="search">
        ${campo('titulo', 'Evento', 'type="search" placeholder="Concierto, obra, festival…"', filtros.titulo)}
        ${campo('ubicacion', 'Lugar', 'type="search" placeholder="Bilbao, Donostia…"', filtros.ubicacion)}
        <button>Buscar eventos</button>
      </form><div id="lista"></div>`);
    $('#fb').onsubmit = ev => { ev.preventDefault(); filtros = Object.fromEntries(new FormData(ev.target)); listar(); };
    await listar();
  },

  async evento(id) {
    const e = await Api.evento(id);
    const mapa = 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(`${e.direccion} ${e.ubicacion}`);
    render(`<a href="#eventos">Volver a eventos</a>
      <div class="detalle"><div>
        <div class="portada">${e.imagen ? `<img src="${h(e.imagen)}" alt="" onerror="this.remove()">` : ''}</div>
        <h1>${h(e.titulo)}</h1><p class="texto">${h(e.descripcion)}</p></div>
      <aside class="entrada" aria-label="Datos del evento"><dl style="margin:0">
        <dt>Cuándo</dt><dd>${h(largo(e.fechaInicio))}${e.fechaFin ? `<small>Hasta ${h(largo(e.fechaFin))}</small>` : ''}</dd>
        <dt>Dónde</dt><dd>${h(e.ubicacion)}<small>${h(e.direccion)}</small></dd>
        <dt>Organiza</dt><dd>${h(e.organizadorNombre)}</dd></dl>
        <div class="corte"></div>
        <a href="${mapa}" target="_blank" rel="noopener" style="color:var(--ink);font-weight:700">Ver en el mapa</a>
        <small style="margin-top:.8rem">La venta de entradas estará disponible próximamente.</small></aside></div>`);
  },

  acceso() {
    render(`<h1>Iniciar sesión</h1><form id="f" class="panel estrecho">
      ${campo('email', 'Correo electrónico', 'type="email" required autocomplete="email"')}
      ${campo('password', 'Contraseña', 'type="password" required autocomplete="current-password"')}
      <button>Iniciar sesión</button><div class="salida"></div></form>
      <p>¿Todavía no tienes cuenta? <a href="#registro">Crear cuenta</a></p>`);
    formulario('#f', async d => {
      Api.guardar(await Api.login(d.email, d.password));
      toast('Sesión iniciada');
      location.hash = rol() === 'ORGANIZADOR' ? '#panel' : '#eventos';
    });
  },

  registro() {
    render(`<h1>Crear cuenta</h1><form id="f" class="panel estrecho">
      ${campo('nombre', 'Nombre', 'required maxlength="100" autocomplete="given-name"')}
      ${campo('apellidos', 'Apellidos', 'required maxlength="150" autocomplete="family-name"')}
      ${campo('email', 'Correo electrónico', 'type="email" required maxlength="150" autocomplete="email"')}
      ${campo('password', 'Contraseña (mínimo 8 caracteres)', 'type="password" required minlength="8" maxlength="72" autocomplete="new-password"')}
      ${campo('telefono', 'Teléfono (opcional)', 'type="tel" maxlength="20" autocomplete="tel"')}
      <button>Crear cuenta</button><div class="salida"></div></form>
      <p>¿Ya tienes cuenta? <a href="#acceso">Iniciar sesión</a></p>`);
    formulario('#f', async d => {
      await Api.registro({ ...d, telefono: d.telefono || null });
      toast('Cuenta creada. Ya puedes iniciar sesión');
      location.hash = '#acceso';
    });
  },

  async cuenta() {
    if (!Api.sesion) { location.hash = '#acceso'; return; }
    const u = await Api.perfil();
    render(`<h1>Mi cuenta</h1><p class="lead">${h(u.email)} <span class="tag">${h(ROLES[u.rol] || u.rol)}</span></p>
      <form id="fp" class="panel estrecho"><h2>Datos personales</h2>
        ${campo('nombre', 'Nombre', 'required maxlength="100"', u.nombre)}
        ${campo('apellidos', 'Apellidos', 'required maxlength="150"', u.apellidos)}
        ${campo('telefono', 'Teléfono', 'type="tel" maxlength="20"', u.telefono)}
        <button>Guardar cambios</button><div class="salida"></div></form>
      <form id="fc" class="panel estrecho"><h2>Cambiar contraseña</h2>
        ${campo('passwordActual', 'Contraseña actual', 'type="password" required autocomplete="current-password"')}
        ${campo('passwordNueva', 'Nueva contraseña', 'type="password" required minlength="8" maxlength="72" autocomplete="new-password"')}
        <button>Cambiar contraseña</button><div class="salida"></div></form>`);
    formulario('#fp', async d => {
      Api.guardar({ ...Api.sesion, usuario: await Api.actualizarPerfil({ ...d, telefono: d.telefono || null }) });
      renderNav('cuenta');
      toast('Cambios guardados');
    });
    formulario('#fc', async (d, f) => {
      await Api.cambiarPassword(d.passwordActual, d.passwordNueva);
      f.reset();
      toast('Contraseña actualizada');
    });
  },

  /* Panel del organizador: #panel, #panel/nuevo, #panel/editar/ID */
  async panel(a, b) {
    if (!Api.sesion) { location.hash = '#acceso'; return; }
    if (rol() !== 'ORGANIZADOR') { render(`<h1>Mis eventos</h1><div class="empty">Esta sección es solo para cuentas de organizador.</div>`); return; }
    if (a === 'nuevo' || a === 'editar') return formEvento(b);
    return listaPanel();
  }
};

async function listaPanel() {
  const l = await Api.misEventos();
  const fila = e => {
    const [txt, cls] = ESTADOS[e.estado] || [e.estado, ''];
    const editable = e.estado === 'BORRADOR' || e.estado === 'PUBLICADO';
    const acc = { BORRADOR: ['publicar', 'cancelar'], PUBLICADO: ['retirar', 'finalizar', 'cancelar'] }[e.estado] || [];
    return `<div class="fila"><div class="grow"><strong>${h(e.titulo)}</strong><br><small>${h(largo(e.fechaInicio))}</small><br><small>${h(e.ubicacion)}</small></div>
      <span class="tag ${cls}">${txt}</span><div class="acc">
      ${e.estado === 'PUBLICADO' ? `<a class="btn sec sm" href="#evento/${e.id}">Ver</a>` : ''}
      ${editable ? `<a class="btn sec sm" href="#panel/editar/${e.id}">Editar</a>` : ''}
      ${acc.map(x => `<button class="btn sec sm ${x === 'cancelar' ? 'peligro' : ''}" data-ac="${x}" data-id="${e.id}">${x[0].toUpperCase() + x.slice(1)}</button>`).join('')}
      </div></div>`;
  };
  render(`<div class="cab"><div><h1>Mis eventos</h1><p class="lead">Los eventos nuevos se crean como borrador. Publícalos para que aparezcan en el catálogo.</p></div>
    <a class="btn" href="#panel/nuevo">Nuevo evento</a></div>` +
    (l.length ? l.map(fila).join('') : '<div class="empty">Todavía no has creado ningún evento.</div>'));
  document.querySelectorAll('[data-ac]').forEach(btn => btn.onclick = async () => {
    const { ac, id } = btn.dataset;
    if (ac === 'cancelar' && !confirm('¿Cancelar este evento? Esta acción no se puede deshacer.')) return;
    try { await Api.cambiarEstado(id, ac); toast(MENSAJES[ac]); listaPanel(); }
    catch (e) { toast(e.detalle, true); }
  });
}

async function formEvento(id) {
  let e = {};
  if (id) {
    e = (await Api.misEventos()).find(x => x.id == id);
    if (!e) throw new ApiError(404, '', { general: 'No se ha encontrado el evento.' });
  }
  render(`<a href="#panel">Volver a mis eventos</a><h1 style="margin-top:1rem">${id ? 'Editar evento' : 'Nuevo evento'}</h1>
    <form id="f" class="panel">
      ${campo('titulo', 'Título', 'required maxlength="150"', e.titulo)}
      ${area('descripcion', 'Descripción', 'required', e.descripcion)}
      <div class="cols">
        ${campo('fechaInicio', 'Inicio', 'type="datetime-local" required', (e.fechaInicio || '').slice(0, 16))}
        ${campo('fechaFin', 'Fin (opcional)', 'type="datetime-local"', (e.fechaFin || '').slice(0, 16))}
      </div><div class="cols">
        ${campo('ubicacion', 'Recinto o ciudad', 'required maxlength="255"', e.ubicacion)}
        ${campo('direccion', 'Dirección', 'required maxlength="255"', e.direccion)}
      </div>
      ${campo('imagen', 'Imagen (URL, opcional)', 'type="url" maxlength="500"', e.imagen)}
      <button>${id ? 'Guardar cambios' : 'Crear borrador'}</button><div class="salida"></div></form>`);
  formulario('#f', async d => {
    const body = { ...d, fechaFin: d.fechaFin || null, imagen: d.imagen || null };
    if (id) await Api.actualizarEvento(id, body); else await Api.crearEvento(body);
    toast(id ? 'Cambios guardados' : 'Evento creado como borrador');
    location.hash = '#panel';
  });
}

/* ============ NAVEGACIÓN ============ */
function renderNav(actual) {
  const c = actual === 'evento' ? 'eventos' : actual;
  const a = (k, t) => `<a href="#${k}" ${c === k ? 'aria-current="page"' : ''}>${t}</a>`;
  $('#nav').innerHTML = a('eventos', 'Eventos') + (Api.sesion
    ? (rol() === 'ORGANIZADOR' ? a('panel', 'Mis eventos') : '') + a('cuenta', 'Mi cuenta') + '<a href="#" id="salir">Cerrar sesión</a>'
    : a('acceso', 'Iniciar sesión') + a('registro', 'Crear cuenta'));
  const o = $('#salir');
  if (o) o.onclick = e => { e.preventDefault(); Api.cerrar(); toast('Sesión cerrada'); location.hash = '#eventos'; ruta(); };
}

async function ruta() {
  const [n, a, b] = (location.hash.slice(1) || 'eventos').split('/');
  const nombre = views[n] ? n : 'eventos';
  renderNav(nombre);
  render('<p class="lead">Cargando…</p>');
  try { await views[nombre](a, b); }
  catch (e) { render(`<div class="empty">${h(e.detalle || e.message)}</div><p><a href="#eventos">Volver a eventos</a></p>`); }
  scrollTo(0, 0);
}

addEventListener('hashchange', ruta);
ruta();