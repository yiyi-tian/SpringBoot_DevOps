import { session, lastResponse, subscribe, apiRequest, reportError } from '../api/client.js';

const COLLAPSE_KEY = 'devops.response.collapsed';

let els = {};

export function initShell(root) {
  root.innerHTML = `
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>
    <div class="shell">
      <header class="header">
        <div class="header-start">
          <button type="button" class="btn btn-sm btn-ghost header-logout" id="header-logout" hidden>登出</button>
          <div class="header-brand">
            <div class="header-logo">◈</div>
            <div>
              <div class="header-title">DevOps Console</div>
              <div class="header-sub">SpringBoot DevOps · API 演示控制台</div>
            </div>
          </div>
        </div>
        <div class="header-meta">
          <div class="meta-item">
            <span class="meta-label">Session</span>
            <span class="meta-value" id="meta-session">未登录</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Role</span>
            <span class="meta-value" id="meta-role">—</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Trace</span>
            <span class="meta-value" id="meta-trace">—</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Latency</span>
            <span class="meta-value" id="meta-latency">—</span>
          </div>
        </div>
      </header>
      <nav class="sidebar" id="sidebar"></nav>
      <div class="content-row" id="content-row">
        <main class="main" id="main"></main>
        <div class="right-rail" id="right-rail">
          <div class="api-cards-rail" id="api-cards-rail" hidden>
            <div class="api-cards-rail-title">API 调试</div>
            <div id="api-cards-mount"></div>
          </div>
          <aside class="response-panel" id="response-panel">
            <div class="response-inner">
              <div class="response-header">
                <span class="response-title">Response</span>
                <div class="response-meta">
                  <span id="resp-status">—</span>
                  <span id="resp-url" class="resp-url"></span>
                </div>
              </div>
              <div class="response-body" id="response-body">
                <div class="response-empty">发送请求后，响应将显示在此处</div>
              </div>
            </div>
          </aside>
          <button type="button" class="response-toggle" id="response-toggle" aria-label="折叠响应面板" title="折叠/展开响应">◀</button>
        </div>
      </div>
    </div>
  `;

  els = {
    sidebar: root.querySelector('#sidebar'),
    main: root.querySelector('#main'),
    contentRow: root.querySelector('#content-row'),
    rightRail: root.querySelector('#right-rail'),
    apiCardsRail: root.querySelector('#api-cards-rail'),
    apiCardsMount: root.querySelector('#api-cards-mount'),
    responsePanel: root.querySelector('#response-panel'),
    responseToggle: root.querySelector('#response-toggle'),
    metaSession: root.querySelector('#meta-session'),
    metaRole: root.querySelector('#meta-role'),
    metaTrace: root.querySelector('#meta-trace'),
    metaLatency: root.querySelector('#meta-latency'),
    headerLogout: root.querySelector('#header-logout'),
    respStatus: root.querySelector('#resp-status'),
    respUrl: root.querySelector('#resp-url'),
    responseBody: root.querySelector('#response-body'),
  };

  const collapsed = localStorage.getItem(COLLAPSE_KEY) === '1';
  setResponseCollapsed(collapsed, false);

  els.responseToggle.addEventListener('click', () => {
    const next = !els.responsePanel.classList.contains('collapsed');
    setResponseCollapsed(next, true);
  });

  els.headerLogout?.addEventListener('click', () => performLogout());

  subscribe(updateHeader);
  subscribe(updateResponse);
  updateHeader();
  updateResponse();
}

export function getMainEl() {
  return els.main;
}

export function getSidebarEl() {
  return els.sidebar;
}

export function getApiCardsMountEl() {
  return els.apiCardsMount;
}

/** @param {boolean} enabled — split main overview + right API cards / response (auth & message) */
export function setSplitLayout(enabled) {
  if (!els.contentRow) return;
  els.contentRow.classList.toggle('content-row--home', enabled);
  if (els.apiCardsRail) {
    els.apiCardsRail.hidden = !enabled;
  }
  if (els.apiCardsMount) {
    els.apiCardsMount.innerHTML = '';
  }
}

/** @deprecated use setSplitLayout */
export function setHomeLayout(enabled) {
  setSplitLayout(enabled);
}

/** @param {boolean} visible */
export function setResponsePanelVisible(visible) {
  if (!els.contentRow) return;
  els.contentRow.classList.toggle('response-hidden', !visible);
}

export function expandResponsePanel() {
  setResponseCollapsed(false, true);
}

export async function performLogout() {
  const btn = els.headerLogout;
  if (btn?.dataset.busy === '1') return;

  if (btn) {
    btn.dataset.busy = '1';
    btn.disabled = true;
    btn.textContent = '登出中…';
  }

  try {
    await apiRequest('POST', '/api/v1/logout', { body: {} });
    expandResponsePanel();
  } catch (err) {
    reportError(err.message || String(err));
    expandResponsePanel();
  } finally {
    if (btn) {
      delete btn.dataset.busy;
      btn.textContent = '登出';
      updateHeader();
    }
  }
}

function setResponseCollapsed(collapsed, persist) {
  if (!els.responsePanel || !els.responseToggle) return;

  els.responsePanel.classList.toggle('collapsed', collapsed);
  els.responseToggle.textContent = collapsed ? '◀' : '▶';
  els.responseToggle.title = collapsed ? '展开响应面板' : '折叠响应面板';

  if (persist) {
    localStorage.setItem(COLLAPSE_KEY, collapsed ? '1' : '0');
  }
}

function updateHeader() {
  if (!els.metaSession) return;

  if (session.userId != null) {
    els.metaSession.textContent = `userId=${session.userId}`;
    els.metaSession.classList.add('active');
  } else {
    els.metaSession.textContent = '未登录';
    els.metaSession.classList.remove('active');
  }

  if (els.headerLogout) {
    const loggedIn = session.userId != null;
    els.headerLogout.hidden = !loggedIn;
    if (!els.headerLogout.dataset.busy) {
      els.headerLogout.disabled = !loggedIn;
    }
  }

  if (session.isAdmin) {
    els.metaRole.textContent = 'admin';
    els.metaRole.classList.add('admin');
  } else if (session.userId != null && session.roles.length > 0) {
    els.metaRole.textContent = session.roles.join(' · ');
    els.metaRole.classList.remove('admin');
  } else if (session.userId != null) {
    els.metaRole.textContent = 'user';
    els.metaRole.classList.remove('admin');
  } else {
    els.metaRole.textContent = '—';
    els.metaRole.classList.remove('admin');
  }

  els.metaTrace.textContent = session.lastTraceId || '—';
  els.metaLatency.textContent =
    lastResponse.durationMs != null ? `${lastResponse.durationMs} ms` : '—';
}

function syntaxHighlight(json) {
  if (typeof json !== 'string') {
    json = JSON.stringify(json, null, 2);
  }
  return json
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(
      /("(\\u[\dA-Fa-f]{4}|\\[^u]|[^"\\])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
      (match) => {
        let cls = 'num';
        if (/^"/.test(match)) {
          cls = /:$/.test(match) ? 'key' : 'str';
        } else if (/true|false/.test(match)) {
          cls = 'bool';
        } else if (/null/.test(match)) {
          cls = 'null';
        }
        return `<span class="${cls}">${match}</span>`;
      }
    );
}

function updateResponse() {
  if (!els.responseBody) return;

  if (lastResponse.error) {
    els.respStatus.textContent = 'ERROR';
    els.respStatus.className = 'status-err';
    els.respUrl.textContent = lastResponse.url || '';
    els.responseBody.innerHTML = `<div class="json-view"><span class="str">${lastResponse.error}</span></div>`;
    return;
  }

  if (lastResponse.status == null && lastResponse.body == null) {
    els.respStatus.textContent = '—';
    els.respStatus.className = '';
    els.respUrl.textContent = '';
    els.responseBody.innerHTML =
      '<div class="response-empty">发送请求后，响应将显示在此处</div>';
    return;
  }

  const ok = lastResponse.status >= 200 && lastResponse.status < 300;
  els.respStatus.textContent = String(lastResponse.status ?? '—');
  els.respStatus.className = ok ? 'status-ok' : 'status-err';
  els.respUrl.textContent = lastResponse.url || '';

  const body = lastResponse.body;
  if (typeof body === 'string') {
    els.responseBody.innerHTML = `<div class="json-view">${body}</div>`;
  } else {
    els.responseBody.innerHTML = `<div class="json-view">${syntaxHighlight(body)}</div>`;
  }
}

export function renderNav(domains, activeId, onSelect) {
  const sidebar = getSidebarEl();
  if (!sidebar) return;

  sidebar.innerHTML =
    domains
      .map(
        (d) => `
      <button class="nav-item${d.id === activeId ? ' active' : ''}" data-id="${d.id}">
        <span class="nav-icon">${d.icon}</span>
        <span>${d.label}</span>
        <span class="nav-port">:${d.port}${d.admin ? ' · admin' : ''}</span>
      </button>
    `
      )
      .join('') +
    `
    <div class="sidebar-hint">
      <strong>提示</strong><br/>
      「账号」页登录/注册/重置密码；「主页」「消息」「日志」中间概览、右侧调试 API。<br/>
      消息写操作在「管理」域；日志域需 admin 角色。<br/>
      请求经 TopBiz :8080 代理。
    </div>
  `;

  sidebar.querySelectorAll('.nav-item').forEach((btn) => {
    btn.addEventListener('click', () => onSelect(btn.dataset.id));
  });
}
