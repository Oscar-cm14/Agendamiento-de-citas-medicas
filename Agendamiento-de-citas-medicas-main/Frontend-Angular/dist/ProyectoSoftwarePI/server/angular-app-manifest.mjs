
export default {
  bootstrap: () => import('./main.server.mjs').then(m => m.default),
  inlineCriticalCss: true,
  baseHref: '/',
  locale: undefined,
  routes: [
  {
    "renderMode": 2,
    "route": "/"
  },
  {
    "renderMode": 2,
    "route": "/login"
  },
  {
    "renderMode": 2,
    "route": "/registro"
  },
  {
    "renderMode": 2,
    "route": "/agendar"
  },
  {
    "renderMode": 2,
    "route": "/admin"
  },
  {
    "renderMode": 2,
    "route": "/agendador"
  },
  {
    "renderMode": 2,
    "redirectTo": "/",
    "route": "/**"
  }
],
  entryPointToBrowserMapping: undefined,
  assets: {
    'index.csr.html': {size: 1051, hash: '9e2d7270a13505101000a5f64f4b2f1938548d18093324acf5d5fc852ba76233', text: () => import('./assets-chunks/index_csr_html.mjs').then(m => m.default)},
    'index.server.html': {size: 1369, hash: 'c66578ffee8775b640f808894f5d5eb619914697f18fab951357a8455cba4f3a', text: () => import('./assets-chunks/index_server_html.mjs').then(m => m.default)},
    'login/index.html': {size: 2248, hash: '16f105cfeaa961d8a26537455fd260ccb661443eb15a52d666dd1e4092c7bd5f', text: () => import('./assets-chunks/login_index_html.mjs').then(m => m.default)},
    'agendador/index.html': {size: 2433, hash: 'ed50674ed0f451df241a706c479ac65382671a266814dc2a7ab4dcd8ee986096', text: () => import('./assets-chunks/agendador_index_html.mjs').then(m => m.default)},
    'index.html': {size: 10179, hash: '2d7bd90cb9def0b840de336cde09cf12d51041e89656870bf5a68df4c09ffcc2', text: () => import('./assets-chunks/index_html.mjs').then(m => m.default)},
    'agendar/index.html': {size: 2197, hash: 'b84930f67fe191d9fd45884a7c0dbfd342e7dd4b0db5c8e9c0ef219c934a7a58', text: () => import('./assets-chunks/agendar_index_html.mjs').then(m => m.default)},
    'registro/index.html': {size: 3347, hash: '5f3a3a42a20642bc40752178cf51768b18a7d782ef0e97d2d1f06dfc2c7116d6', text: () => import('./assets-chunks/registro_index_html.mjs').then(m => m.default)},
    'admin/index.html': {size: 4244, hash: 'a7408abe133296f141db06eb1c0eb478a850b3193561a69d2387706b4550006c', text: () => import('./assets-chunks/admin_index_html.mjs').then(m => m.default)},
    'styles-75EU3E3Q.css': {size: 1402, hash: 'Xp8fEUJsJRk', text: () => import('./assets-chunks/styles-75EU3E3Q_css.mjs').then(m => m.default)}
  },
};
