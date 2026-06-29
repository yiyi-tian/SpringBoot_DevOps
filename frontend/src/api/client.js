/**
 * HTTP client — all requests proxied to TopBiz :8080 via Vite.
 */

/** @type {{ userId: number|null, isAdmin: boolean, roles: string[], lastTraceId: string|null }} */
export const session = {
  userId: null,
  isAdmin: false,
  roles: [],
  lastTraceId: null,
};

/** @type {{ status: number|null, durationMs: number|null, body: unknown, error: string|null, url: string|null }} */
export const lastResponse = {
  status: null,
  durationMs: null,
  body: null,
  error: null,
  url: null,
};

const listeners = new Set();

export function subscribe(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function notify() {
  listeners.forEach((fn) => fn());
}

/**
 * @param {string} method
 * @param {string} path
 * @param {{ body?: object, query?: object, pathParams?: object }} [opts]
 */
export async function apiRequest(method, path, opts = {}) {
  const { body, query = {}, pathParams = {} } = opts;

  let url = path.replace(/\{(\w+)\}/g, (_, key) => {
    const val = pathParams[key];
    if (val === undefined || val === '') throw new Error(`Missing path param: ${key}`);
    return encodeURIComponent(String(val));
  });

  const qs = Object.entries(query).filter(
    ([, v]) => v !== undefined && v !== null && String(v).trim() !== ''
  );
  if (qs.length > 0) {
    url += '?' + new URLSearchParams(qs.map(([k, v]) => [k, String(v)])).toString();
  }

  const init = {
    method: method.toUpperCase(),
    credentials: 'include',
    headers: { Accept: 'application/json' },
  };

  if (body !== undefined && !['GET', 'HEAD'].includes(init.method)) {
    init.headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }

  const start = performance.now();
  let res;
  let parsed;

  try {
    res = await fetch(url, init);
    const text = await res.text();
    try {
      parsed = text ? JSON.parse(text) : null;
    } catch {
      parsed = text;
    }

    const traceId =
      res.headers.get('X-Trace-Id') ||
      res.headers.get('x-trace-id') ||
      res.headers.get('trace-id');
    if (traceId) session.lastTraceId = traceId;

    lastResponse.status = res.status;
    lastResponse.durationMs = Math.round(performance.now() - start);
    lastResponse.body = parsed;
    lastResponse.error = null;
    lastResponse.url = url;

    updateSessionFromResponse(parsed, method, path);
    notify();

    return { ok: res.ok, status: res.status, data: parsed, headers: res.headers };
  } catch (err) {
    lastResponse.status = null;
    lastResponse.durationMs = Math.round(performance.now() - start);
    lastResponse.body = null;
    lastResponse.error = err.message || String(err);
    lastResponse.url = url;
    notify();
    throw err;
  }
}

function updateSessionFromResponse(data, method, path) {
  if (!data || typeof data !== 'object') return;

  if (path.includes('/login') && method.toUpperCase() === 'POST' && data.code === 0) {
    const uid = data.data?.userId;
    if (uid != null) session.userId = Number(uid);
  }

  if (path.includes('/register') && method.toUpperCase() === 'POST' && data.code === 0) {
    const uid = data.data?.userId;
    if (uid != null) session.userId = Number(uid);
  }

  if (path.includes('/logout') && method.toUpperCase() === 'POST' && data.code === 0) {
    session.userId = null;
    session.isAdmin = false;
    session.roles = [];
  }

  if (path.includes('/deregister') && method.toUpperCase() === 'POST' && data.code === 0) {
    session.userId = null;
    session.isAdmin = false;
    session.roles = [];
  }

  if (path.endsWith('/profile') && method.toUpperCase() === 'GET' && data.code === 0) {
    const uid = data.data?.userId ?? data.data?.id;
    if (uid != null) session.userId = Number(uid);
  }

  if (path === '/api/v1/permissions' && method.toUpperCase() === 'GET' && data.code === 0) {
    const d = data.data || {};
    const roles = Array.isArray(d.roles) ? d.roles : [];
    const permissions = Array.isArray(d.permissions) ? d.permissions : [];
    session.roles = roles;
    session.isAdmin =
      d.is_admin === true ||
      d.isAdmin === true ||
      d.admin === true ||
      roles.includes('admin') ||
      roles.some((r) => String(r).toLowerCase() === 'admin') ||
      permissions.includes('admin');
  }
}

export function clearSession() {
  session.userId = null;
  session.isAdmin = false;
  session.roles = [];
  notify();
}

export function reportError(message) {
  lastResponse.error = message;
  lastResponse.status = null;
  lastResponse.body = null;
  lastResponse.url = null;
  notify();
}

export async function refreshSession() {
  try {
    await Promise.all([
      apiRequest('GET', '/api/v1/profile'),
      apiRequest('GET', '/api/v1/permissions'),
    ]);
  } catch {
    /* not logged in */
  }
}
