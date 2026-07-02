import { apiRequest, reportError } from '../api/client.js';
import { resolvePath } from '../api/endpoints.js';
import { expandResponsePanel } from './shell.js';

/**
 * Render endpoint cards for a domain.
 * @param {HTMLElement} container
 * @param {import('../api/endpoints.js').Endpoint[]} endpoints
 */
export function renderEndpointList(container, endpoints) {
  if (endpoints.length === 0) {
    container.innerHTML = '<p class="endpoint-count">暂无接口</p>';
    return;
  }

  container.innerHTML = `
    <div class="endpoint-list">
      ${endpoints.map((ep) => renderEndpointCard(ep)).join('')}
    </div>
  `;

  bindEndpointCards(container);
}

/**
 * @param {HTMLElement} container
 * @param {{ title: string, endpoints: import('../api/endpoints.js').Endpoint[], hint?: string, adminBanner?: boolean }[]} sections
 */
export function renderEndpointSections(container, sections) {
  container.innerHTML = sections
    .map((section) => {
      const hint = section.hint
        ? `<p class="endpoint-section-hint">${section.hint}</p>`
        : '';
      const banner = section.adminBanner
        ? `<div class="admin-banner admin-banner--inline"><strong>需要 admin 权限</strong> — RBAC 接口需 admin 角色；403 表示当前会话无权限。</div>`
        : '';
      return `
        <div class="endpoint-section">
          <h2 class="endpoint-section-title">${section.title}</h2>
          ${hint}
          ${banner}
          <div class="endpoint-list">
            ${section.endpoints.map((ep) => renderEndpointCard(ep)).join('')}
          </div>
        </div>
      `;
    })
    .join('');

  bindEndpointCards(container);
}

function bindEndpointCards(container) {
  container.querySelectorAll('.endpoint-header').forEach((header) => {
    header.addEventListener('click', () => {
      header.closest('.endpoint-card')?.classList.toggle('open');
    });
  });

  container.querySelectorAll('[data-send]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const card = btn.closest('.endpoint-card');
      if (card) sendFromCard(card);
    });
  });
}

function renderEndpointCard(ep) {
  const statusClass = `badge-${ep.status}`;
  const authBadge =
    ep.auth === 'admin'
      ? '<span class="badge badge-auth-admin">admin</span>'
      : ep.auth === 'anon'
        ? '<span class="badge badge-auth-anon">anon</span>'
        : '';

  const pathFields = (ep.pathParams || [])
    .map(
      (p) => `
    <div class="field">
      <label class="label">Path: ${p}</label>
      <input class="input path-param" data-param="${p}" placeholder="${p}" />
    </div>
  `
    )
    .join('');

  const showBody = !['GET', 'HEAD'].includes(ep.method);
  const bodyDefault =
    ep.bodyExample !== undefined
      ? JSON.stringify(ep.bodyExample, null, 2)
      : showBody
        ? '{}'
        : '';

  const showQuery = ep.queryExample !== undefined || ep.method === 'GET' || ep.method === 'DELETE';
  const queryDefault =
    ep.queryExample !== undefined ? JSON.stringify(ep.queryExample, null, 2) : '{}';

  return `
    <div class="endpoint-card glass" data-id="${ep.id}"
         data-method="${ep.method}" data-path="${ep.path}">
      <div class="endpoint-header">
        <span class="chevron">▶</span>
        <span class="method method-${ep.method}">${ep.method}</span>
        <span class="endpoint-title">${ep.title}</span>
        <span class="endpoint-path">${ep.path}</span>
      </div>
      <div class="endpoint-body">
        <div class="endpoint-meta">
          <span class="badge ${statusClass}">${ep.status}</span>
          ${authBadge}
          <span class="badge badge-planned">→ :8080</span>
        </div>
        ${ep.description ? `<p class="panel-desc">${ep.description}</p>` : ''}
        ${pathFields}
        ${
          showQuery
            ? `
          <div class="field">
            <label class="label">Query Params (JSON)</label>
            <textarea class="textarea query-json">${escapeHtml(queryDefault)}</textarea>
          </div>
        `
            : ''
        }
        ${
          showBody
            ? `
          <div class="field">
            <label class="label">Request Body (JSON)</label>
            <textarea class="textarea body-json">${escapeHtml(bodyDefault)}</textarea>
          </div>
        `
            : ''
        }
        <div class="endpoint-actions">
          <button class="btn btn-sm btn-ghost format-btn" type="button">格式化 JSON</button>
          <button class="btn btn-sm" data-send type="button">发送请求</button>
        </div>
      </div>
    </div>
  `;
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

/**
 * Open an API card in the right rail and prefill JSON body.
 * @param {string} endpointId
 * @param {object} bodyPartial
 */
export function prefillEndpointCard(endpointId, bodyPartial) {
  const card = document.querySelector(`.endpoint-card[data-id="${endpointId}"]`);
  if (!card) return;

  card.classList.add('open');
  const bodyEl = card.querySelector('.body-json');
  if (bodyEl) {
    let current = {};
    try {
      if (bodyEl.value.trim()) current = JSON.parse(bodyEl.value);
    } catch {
      /* ignore */
    }
    bodyEl.value = JSON.stringify({ ...current, ...bodyPartial }, null, 2);
  }

  card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function sendFromCard(card) {
  const method = card.dataset.method;
  const pathTemplate = card.dataset.path;
  const btn = card.querySelector('[data-send]');
  if (btn) {
    btn.disabled = true;
    btn.textContent = '请求中…';
  }

  try {
    const pathParams = {};
    card.querySelectorAll('.path-param').forEach((input) => {
      pathParams[input.dataset.param] = input.value.trim();
    });

    const path = resolvePath(pathTemplate, pathParams);
    if (path.includes('{')) {
      throw new Error('请填写所有路径参数');
    }

    let query = {};
    const queryEl = card.querySelector('.query-json');
    if (queryEl && queryEl.value.trim()) {
      query = JSON.parse(queryEl.value);
    }

    let body;
    const bodyEl = card.querySelector('.body-json');
    if (bodyEl && bodyEl.value.trim() && !['GET', 'HEAD'].includes(method)) {
      body = JSON.parse(bodyEl.value);
    }

    await apiRequest(method, path, { body, query, pathParams });

    expandResponsePanel();
    card.closest('.endpoint-list')?.querySelectorAll('.endpoint-card.last-sent').forEach((c) => {
      c.classList.remove('last-sent');
    });
    card.classList.add('last-sent');
  } catch (err) {
    reportError(err.message || String(err));
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = '发送请求';
    }
  }
}

// Format buttons — delegated after render
document.addEventListener('click', (e) => {
  if (!e.target.classList.contains('format-btn')) return;
  e.stopPropagation();
  const card = e.target.closest('.endpoint-card');
  if (!card) return;
  card.querySelectorAll('.body-json, .query-json').forEach((ta) => {
    if (!ta.value.trim()) return;
    try {
      ta.value = JSON.stringify(JSON.parse(ta.value), null, 2);
    } catch {
      /* ignore */
    }
  });
});

export function renderAdminBanner(container) {
  const banner = document.createElement('div');
  banner.className = 'admin-banner';
  banner.innerHTML =
    '<strong>需要 admin 权限</strong> — 请使用首个注册用户 (userId=1) 或 identifier 为 admin 的账号登录。403 表示当前会话无 admin 角色。';
  const anchor = container.querySelector('#endpoint-container');
  if (anchor) {
    container.insertBefore(banner, anchor);
  } else {
    container.prepend(banner);
  }
}
