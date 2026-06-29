import { session, subscribe, apiRequest } from '../api/client.js';
import { getOrCreateDeviceId } from '../api/device-id.js';
import { prefillEndpointCard } from './forms.js';
import { performLogout } from './shell.js';

const TAB_IDS = ['profile', 'security', 'devices', 'permissions'];

/**
 * @param {HTMLElement} container
 */
export function renderHomeOverview(container) {
  container.innerHTML = `
    <div class="domain-overview home-overview" id="home-overview-root">
      <div class="home-overview-loading">加载中…</div>
    </div>
  `;

  const root = container.querySelector('#home-overview-root');
  let lastUserId = session.userId;
  let activeTab = 'profile';

  subscribe(() => {
    const uid = session.userId;
    if (uid === lastUserId) return;
    lastUserId = uid;
    if (uid != null) {
      renderShell(root, activeTab);
    } else {
      renderLoggedOut(root);
    }
  });

  if (session.userId != null) {
    renderShell(root, activeTab);
  } else {
    renderLoggedOut(root);
  }

  function renderShell(el, tab) {
    activeTab = tab;
    el.innerHTML = `
      <div class="overview-tabs" role="tablist">
        ${TAB_IDS.map(
          (id) => `
          <button type="button" class="overview-tab${tab === id ? ' active' : ''}" data-tab="${id}" role="tab">
            ${tabLabel(id)}
          </button>`
        ).join('')}
      </div>
      <div class="overview-tab-panel" id="home-tab-panel"></div>
    `;

    el.querySelectorAll('.overview-tab').forEach((btn) => {
      btn.addEventListener('click', () => renderShell(el, btn.dataset.tab));
    });

    const panel = el.querySelector('#home-tab-panel');
    if (tab === 'profile') loadProfileTab(panel);
    else if (tab === 'security') loadSecurityTab(panel);
    else if (tab === 'devices') loadDevicesSessionsTab(panel);
    else loadPermissionsTab(panel);
  }
}

function tabLabel(id) {
  if (id === 'profile') return '我的信息';
  if (id === 'security') return '账号安全';
  if (id === 'devices') return '设备与会话';
  return '权限';
}

function renderLoggedOut(root) {
  root.innerHTML = `
    <div class="home-overview-empty glass">
      <p class="home-overview-empty-title">尚未登录</p>
      <p class="home-overview-empty-desc">请先在侧栏「账号」页登录或注册，再使用主页查看信息与调试 API。</p>
    </div>
  `;
}

function renderSubTabs(panel, tabs, activeId, onSelect) {
  panel.innerHTML = `
    <div class="overview-tabs overview-tabs--nested" role="tablist">
      ${tabs
        .map(
          (t) => `
        <button type="button" class="overview-tab${t.id === activeId ? ' active' : ''}" data-subtab="${t.id}" role="tab">
          ${t.label}
        </button>`
        )
        .join('')}
    </div>
    <div id="home-sub-panel"></div>
  `;
  panel.querySelectorAll('[data-subtab]').forEach((btn) => {
    btn.addEventListener('click', () => onSelect(btn.dataset.subtab));
  });
  return panel.querySelector('#home-sub-panel');
}

async function loadProfileTab(panel) {
  panel.innerHTML = '<div class="home-overview-loading">加载中…</div>';
  try {
    const res = await apiRequest('GET', '/api/v1/profile');
    const profile = res.data?.code === 0 ? res.data.data : null;

    panel.innerHTML = `
      <section class="home-card glass msg-panel">
        <h2 class="home-card-title">基本信息</h2>
        ${
          profile
            ? `
          <dl class="home-dl">
            <div><dt>用户 ID</dt><dd>${esc(profile.userId)}</dd></div>
            <div><dt>状态</dt><dd>${esc(profile.status || '—')}</dd></div>
            <div><dt>最后登录</dt><dd>${formatDateTime(profile.lastLoginAt)}</dd></div>
            <div><dt>登录 IP</dt><dd>${esc(profile.lastLoginIp || '—')}</dd></div>
            <div><dt>注册时间</dt><dd>${formatDateTime(profile.createdAt)}</dd></div>
          </dl>
          ${
            Array.isArray(profile.credentials) && profile.credentials.length
              ? `<div class="home-creds">
                  <span class="home-creds-label">绑定凭证</span>
                  <ul class="home-cred-list">
                    ${profile.credentials
                      .map(
                        (c) =>
                          `<li><span class="home-cred-type">${esc(c.identityType)}</span> ${esc(c.identifier)}</li>`
                      )
                      .join('')}
                  </ul>
                </div>`
              : ''
          }
        `
            : '<p class="home-card-err">无法加载用户信息</p>'
        }
      </section>
      <section class="home-card glass msg-panel home-form-card">
        <h2 class="home-card-title">修改资料</h2>
        <form id="home-profile-form" class="home-form">
          <div class="field">
            <label class="label" for="home-display-name">显示名</label>
            <input class="input" id="home-display-name" type="text" maxlength="64"
              value="${escAttr(profile?.displayName || '')}" />
          </div>
          <div class="field">
            <label class="label" for="home-sex">性别</label>
            <select class="input" id="home-sex">
              <option value="0"${profile?.sex === 0 || profile?.sex == null ? ' selected' : ''}>未设置</option>
              <option value="1"${profile?.sex === 1 ? ' selected' : ''}>男</option>
              <option value="2"${profile?.sex === 2 ? ' selected' : ''}>女</option>
            </select>
          </div>
          <div class="home-action-row">
            <button type="submit" class="btn btn-sm">保存资料</button>
            <button type="button" class="btn btn-sm btn-ghost" id="home-profile-debug">右侧调试</button>
          </div>
          <p class="home-form-msg home-hidden" id="home-profile-msg"></p>
        </form>
      </section>
    `;

    panel.querySelector('#home-profile-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const msgEl = panel.querySelector('#home-profile-msg');
      const displayName = panel.querySelector('#home-display-name')?.value.trim();
      const sex = Number(panel.querySelector('#home-sex')?.value ?? 0);
      try {
        const r = await apiRequest('PATCH', '/api/v1/profile', { body: { displayName, sex } });
        showFormMsg(msgEl, r.data?.code === 0 ? '保存成功' : r.data?.message || '保存失败', r.data?.code === 0);
      } catch (err) {
        showFormMsg(msgEl, err.message || '请求失败', false);
      }
    });

    panel.querySelector('#home-profile-debug')?.addEventListener('click', () => {
      prefillEndpointCard('auth-profile', {
        displayName: panel.querySelector('#home-display-name')?.value.trim(),
        sex: Number(panel.querySelector('#home-sex')?.value ?? 0),
      });
    });
  } catch {
    panel.innerHTML = '<p class="home-card-err">加载失败</p>';
  }
}

function loadSecurityTab(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel home-form-card">
      <h2 class="home-card-title">修改密码</h2>
      <p class="home-muted">修改成功后当前会话仍有效；若需更高安全性可手动登出后重新登录。</p>
      <form id="home-password-form" class="home-form">
        <div class="field">
          <label class="label" for="home-old-pwd">旧密码</label>
          <input class="input" id="home-old-pwd" type="password" autocomplete="current-password" />
        </div>
        <div class="field">
          <label class="label" for="home-new-pwd">新密码</label>
          <input class="input" id="home-new-pwd" type="password" autocomplete="new-password" minlength="6" />
        </div>
        <div class="home-action-row">
          <button type="submit" class="btn btn-sm">修改密码</button>
          <button type="button" class="btn btn-sm btn-ghost" id="home-pwd-debug">右侧调试</button>
        </div>
        <p class="home-form-msg home-hidden" id="home-pwd-msg"></p>
      </form>
    </section>
    <section class="home-card glass msg-panel home-card-actions">
      <h2 class="home-card-title">快捷操作</h2>
      <div class="home-action-row">
        <button type="button" class="btn btn-sm btn-ghost" id="home-logout-btn">登出当前会话</button>
      </div>
    </section>
  `;

  panel.querySelector('#home-password-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const msgEl = panel.querySelector('#home-pwd-msg');
    const oldPassword = panel.querySelector('#home-old-pwd')?.value ?? '';
    const newPassword = panel.querySelector('#home-new-pwd')?.value ?? '';
    if (newPassword.length < 6) {
      showFormMsg(msgEl, '新密码至少 6 位', false);
      return;
    }
    try {
      const r = await apiRequest('PUT', '/api/v1/password', { body: { oldPassword, newPassword } });
      showFormMsg(msgEl, r.data?.code === 0 ? '密码已修改' : r.data?.message || '修改失败', r.data?.code === 0);
      if (r.data?.code === 0) {
        panel.querySelector('#home-old-pwd').value = '';
        panel.querySelector('#home-new-pwd').value = '';
      }
    } catch (err) {
      showFormMsg(msgEl, err.message || '请求失败', false);
    }
  });

  panel.querySelector('#home-pwd-debug')?.addEventListener('click', () => {
    prefillEndpointCard('auth-password', {
      oldPassword: 'demo123456',
      newPassword: 'newpass123456',
    });
  });

  panel.querySelector('#home-logout-btn')?.addEventListener('click', () => performLogout());
}

async function loadDevicesSessionsTab(panel) {
  let subTab = 'devices';

  async function render(sub) {
    subTab = sub;
    panel.innerHTML = '';
    const subPanel = renderSubTabs(
      panel,
      [
        { id: 'devices', label: '我的设备' },
        { id: 'sessions', label: '活跃会话' },
      ],
      subTab,
      render
    );

    subPanel.innerHTML = '<div class="home-overview-loading">加载中…</div>';
    const currentDeviceId = getOrCreateDeviceId();

    try {
      const res = await apiRequest('GET', '/api/v1/sessions');
      const data = res.data?.code === 0 ? res.data.data : null;
      const sessions = data?.sessions || [];

      if (subTab === 'devices') {
        renderDevicesView(subPanel, sessions, currentDeviceId, () => render('devices'));
      } else {
        renderSessionsView(subPanel, sessions, () => render('sessions'));
      }
    } catch {
      subPanel.innerHTML = '<p class="home-card-err">加载失败</p>';
    }
  }

  await render(subTab);
}

function renderDevicesView(subPanel, sessions, currentDeviceId, reload) {
  subPanel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">我的设备（${sessions.length}）</h2>
      <p class="home-muted home-device-hint">
        设备号标识物理/浏览器端；同一设备重复登录会更新会话，不会新增设备行。
      </p>
      <p class="home-muted">本机设备号：
        <code class="home-inline-code" title="${escAttr(currentDeviceId)}">${esc(shortId(currentDeviceId))}</code>
        <button type="button" class="btn btn-sm btn-ghost" id="home-copy-device">复制</button>
      </p>
      <div id="home-device-list"></div>
    </section>
  `;

  subPanel.querySelector('#home-copy-device')?.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(currentDeviceId);
      alert('设备号已复制');
    } catch {
      alert(currentDeviceId);
    }
  });

  const listEl = subPanel.querySelector('#home-device-list');
  if (!sessions.length) {
    listEl.innerHTML = '<p class="home-muted">暂无注册设备</p>';
    return;
  }

  listEl.innerHTML = `
    <div class="msg-table-wrap">
      <table class="msg-table">
        <thead>
          <tr>
            <th>设备号</th>
            <th>类型</th>
            <th>最近 IP</th>
            <th>最近登录</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${sessions
            .map((s) => {
              const isCurrent = s.isCurrent === true;
              const deviceId = s.deviceId || '—';
              return `
            <tr class="${isCurrent ? 'home-device-current' : ''}">
              <td><code class="home-inline-code" title="${escAttr(deviceId)}">${esc(shortId(deviceId))}</code></td>
              <td>${esc(s.deviceType || '—')}${isCurrent ? ' <span class="home-tag">本机</span>' : ''}</td>
              <td>${esc(s.clientIp || '—')}</td>
              <td>${formatDateTime(s.loginAt)}</td>
              <td class="msg-table-actions">
                ${
                  isCurrent
                    ? '<span class="home-muted">—</span>'
                    : `<button type="button" class="btn btn-sm btn-ghost home-kick-device" data-device-id="${escAttr(deviceId)}">移除设备</button>`
                }
              </td>
            </tr>`;
            })
            .join('')}
        </tbody>
      </table>
    </div>
  `;

  bindKickDevice(listEl, reload);
}

function renderSessionsView(subPanel, sessions, reload) {
  subPanel.innerHTML = `
    <section class="home-card glass msg-panel">
      <div class="home-card-head-row">
        <h2 class="home-card-title">活跃会话（${sessions.length}）</h2>
        <button type="button" class="btn btn-sm btn-ghost" id="home-kick-others" ${
          sessions.length <= 1 ? 'disabled' : ''
        }>登出其他会话</button>
      </div>
      <p class="home-muted home-device-hint">
        会话对应一次 Shiro 登录态；结束会话会立即失效对应 Cookie/Redis，与「移除设备」作用范围不同。
      </p>
      <div id="home-session-list"></div>
    </section>
  `;

  const listEl = subPanel.querySelector('#home-session-list');
  if (!sessions.length) {
    listEl.innerHTML = '<p class="home-muted">暂无活跃会话</p>';
    return;
  }

  listEl.innerHTML = `
    <div class="msg-table-wrap">
      <table class="msg-table">
        <thead>
          <tr>
            <th>会话 ID</th>
            <th>关联设备</th>
            <th>最后活跃</th>
            <th>状态</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${sessions
            .map((s) => {
              const isCurrent = s.isCurrent === true;
              const sessionId = s.sessionId || '—';
              const deviceId = s.deviceId || '—';
              return `
            <tr class="${isCurrent ? 'home-device-current' : ''}">
              <td><code class="home-inline-code" title="${escAttr(sessionId)}">${esc(shortId(sessionId))}</code></td>
              <td><code class="home-inline-code" title="${escAttr(deviceId)}">${esc(shortId(deviceId))}</code></td>
              <td>${formatDateTime(s.lastActiveAt || s.loginAt)}</td>
              <td>${isCurrent ? '<span class="home-tag">当前会话</span>' : '<span class="home-muted">其他</span>'}</td>
              <td class="msg-table-actions">
                ${
                  isCurrent
                    ? '<span class="home-muted">—</span>'
                    : `<button type="button" class="btn btn-sm btn-ghost home-kick-session" data-session-id="${escAttr(sessionId)}">结束会话</button>`
                }
              </td>
            </tr>`;
            })
            .join('')}
        </tbody>
      </table>
    </div>
  `;

  subPanel.querySelector('#home-kick-others')?.addEventListener('click', async () => {
    if (!confirm('确定登出除当前会话外的所有其他会话？')) return;
    try {
      const r = await apiRequest('DELETE', '/api/v1/sessions', { query: { scope: 'others' } });
      if (r.data?.code === 0) reload();
      else alert(r.data?.message || '操作失败');
    } catch (e) {
      alert(e.message || '请求失败');
    }
  });

  listEl.querySelectorAll('.home-kick-session').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const sessionId = btn.dataset.sessionId;
      if (!sessionId || !confirm(`结束会话 ${shortId(sessionId)}？`)) return;
      try {
        const r = await apiRequest('DELETE', `/api/v1/sessions/${encodeURIComponent(sessionId)}`);
        if (r.data?.code === 0) reload();
        else alert(r.data?.message || '操作失败');
      } catch (e) {
        alert(e.message || '请求失败');
      }
    });
  });
}

function bindKickDevice(listEl, reload) {
  listEl.querySelectorAll('.home-kick-device').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const deviceId = btn.dataset.deviceId;
      if (!deviceId || !confirm(`移除设备 ${shortId(deviceId)}？该设备上的会话将一并失效。`)) return;
      try {
        const r = await apiRequest('DELETE', `/api/v1/devices/${encodeURIComponent(deviceId)}`);
        if (r.data?.code === 0) reload();
        else alert(r.data?.message || '操作失败');
      } catch (e) {
        alert(e.message || '请求失败');
      }
    });
  });
}

async function loadPermissionsTab(panel) {
  panel.innerHTML = '<div class="home-overview-loading">加载权限…</div>';

  let ownedSet = new Set();
  let roles = [];
  let catalogPage = 1;
  const catalogSize = 10;
  let catalogKeyword = '';
  let catalogFilter = 'applyable';
  let permSubTab = 'mine';

  async function refreshMyPerms() {
    const permRes = await apiRequest('GET', '/api/v1/permissions');
    const permData = permRes.data?.code === 0 ? permRes.data.data : null;
    roles = permData?.roles || [];
    ownedSet = new Set(permData?.permissions || []);
  }

  function renderPermShell() {
    panel.innerHTML = '';
    const subPanel = renderSubTabs(
      panel,
      [
        { id: 'mine', label: '我的权限' },
        { id: 'catalog', label: '权限目录' },
        { id: 'applications', label: '我的申请' },
      ],
      permSubTab,
      (id) => {
        permSubTab = id;
        renderPermShell();
      }
    );

    if (permSubTab === 'mine') {
      renderMineTab(subPanel);
    } else if (permSubTab === 'catalog') {
      renderCatalogTab(subPanel);
    } else {
      renderApplicationsTab(subPanel);
    }
  }

  function renderMineTab(subPanel) {
    subPanel.innerHTML = `
      <section class="home-card glass msg-panel">
        <h2 class="home-card-title">我的权限</h2>
        ${
          roles.length
            ? `<p class="home-roles">角色：${roles.map((r) => `<span class="home-tag">${esc(r)}</span>`).join('')}</p>`
            : '<p class="home-muted">暂无角色</p>'
        }
        ${
          ownedSet.size
            ? `<ul class="home-perm-list">${[...ownedSet].map((p) => `<li><code class="perm-badge">${esc(p)}</code></li>`).join('')}</ul>`
            : '<p class="home-muted">暂无权限记录</p>'
        }
      </section>
    `;
  }

  function renderCatalogTab(subPanel) {
    subPanel.innerHTML = `
      <section class="home-card glass msg-panel">
        <h2 class="home-card-title">权限目录</h2>
        <div class="msg-toolbar">
          <input class="input msg-search" id="home-perm-keyword" type="search" placeholder="搜索 permCode / 名称"
            value="${escAttr(catalogKeyword)}" />
          <select class="input msg-select" id="home-perm-filter">
            <option value="applyable"${catalogFilter === 'applyable' ? ' selected' : ''}>仅可申请</option>
            <option value="all"${catalogFilter === 'all' ? ' selected' : ''}>全部</option>
          </select>
          <button type="button" class="btn btn-sm" id="home-perm-search">查询</button>
        </div>
        <div id="home-perm-catalog-wrap"><div class="home-overview-loading">加载目录…</div></div>
      </section>
    `;

    subPanel.querySelector('#home-perm-search')?.addEventListener('click', () => {
      catalogKeyword = subPanel.querySelector('#home-perm-keyword')?.value.trim() || '';
      catalogFilter = subPanel.querySelector('#home-perm-filter')?.value || 'applyable';
      catalogPage = 1;
      loadCatalog(subPanel);
    });
    subPanel.querySelector('#home-perm-keyword')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') subPanel.querySelector('#home-perm-search')?.click();
    });

    loadCatalog(subPanel);
  }

  async function loadCatalog(subPanel) {
    const wrap = subPanel.querySelector('#home-perm-catalog-wrap');
    if (!wrap) return;
    wrap.innerHTML = '<div class="home-overview-loading">加载目录…</div>';

    try {
      const q = { page: catalogPage, size: catalogSize };
      if (catalogKeyword) q.keyword = catalogKeyword;
      const res = await apiRequest('GET', '/api/v1/permissions/catalog', { query: q });
      if (res.data?.code !== 0) {
        wrap.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '加载失败')}</p>`;
        return;
      }
      let list = res.data.data?.list || [];
      if (catalogFilter === 'applyable') {
        list = list.filter((p) => !ownedSet.has(p.permCode));
      }
      const total = Number(res.data.data?.total ?? list.length);
      const totalPages = Math.max(1, Math.ceil(total / catalogSize));

      if (!list.length) {
        wrap.innerHTML =
          catalogFilter === 'applyable'
            ? '<p class="home-muted">暂无可申请权限（可切换「全部」查看已拥有项）</p>'
            : '<p class="home-muted">无匹配权限</p>';
        return;
      }

      wrap.innerHTML = `
        <div class="msg-table-wrap">
          <table class="msg-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Code</th>
                <th>名称</th>
                <th>状态</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${list
                .map((p) => {
                  const owned = ownedSet.has(p.permCode);
                  return `
                <tr>
                  <td>${esc(p.permId)}</td>
                  <td><code class="perm-badge">${esc(p.permCode)}</code></td>
                  <td>${esc(p.permName || '—')}</td>
                  <td>${owned ? '<span class="home-tag">已拥有</span>' : '<span class="perm-status-apply">可申请</span>'}</td>
                  <td class="msg-table-actions">
                    ${
                      owned
                        ? ''
                        : `<button type="button" class="btn btn-sm home-apply-perm" data-perm-id="${escAttr(String(p.permId))}">申请</button>`
                    }
                  </td>
                </tr>`;
                })
                .join('')}
            </tbody>
          </table>
        </div>
        <div class="overview-pager">
          <button type="button" class="btn btn-sm btn-ghost" id="home-perm-prev" ${
            catalogPage <= 1 ? 'disabled' : ''
          }>上一页</button>
          <span class="overview-pager-info">${catalogPage} / ${totalPages}（共 ${total} 条）</span>
          <button type="button" class="btn btn-sm btn-ghost" id="home-perm-next" ${
            catalogPage >= totalPages ? 'disabled' : ''
          }>下一页</button>
        </div>
      `;

      wrap.querySelector('#home-perm-prev')?.addEventListener('click', () => {
        if (catalogPage > 1) {
          catalogPage--;
          loadCatalog(subPanel);
        }
      });
      wrap.querySelector('#home-perm-next')?.addEventListener('click', () => {
        if (catalogPage < totalPages) {
          catalogPage++;
          loadCatalog(subPanel);
        }
      });

      wrap.querySelectorAll('.home-apply-perm').forEach((btn) => {
        btn.addEventListener('click', async () => {
          const permId = Number(btn.dataset.permId);
          if (!permId) return;
          btn.disabled = true;
          try {
            const r = await apiRequest('POST', '/api/v1/permissions/apply', { body: { permId } });
            if (r.data?.code === 0) {
              alert('申请已提交，等待管理员审批');
              prefillEndpointCard('auth-perm-apply', { permId });
              permSubTab = 'applications';
              await refreshMyPerms();
              renderPermShell();
            } else {
              alert(r.data?.message || '申请失败');
            }
          } catch (e) {
            alert(e.message || '请求失败');
          } finally {
            btn.disabled = false;
          }
        });
      });
    } catch (e) {
      wrap.innerHTML = `<p class="home-card-err">${esc(e.message || '加载失败')}</p>`;
    }
  }

  async function renderApplicationsTab(subPanel) {
    subPanel.innerHTML = '<div class="home-overview-loading">加载申请记录…</div>';
    try {
      const res = await apiRequest('GET', '/api/v1/permissions/applications');
      const list = res.data?.code === 0 ? res.data.data?.list || [] : null;

      if (list === null) {
        subPanel.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '加载失败')}</p>`;
        return;
      }

      if (!list.length) {
        subPanel.innerHTML = `
          <section class="home-card glass msg-panel">
            <h2 class="home-card-title">我的申请</h2>
            <p class="home-muted">暂无进行中的申请。可在「权限目录」中提交新申请。</p>
          </section>
        `;
        return;
      }

      subPanel.innerHTML = `
        <section class="home-card glass msg-panel">
          <h2 class="home-card-title">我的申请（${list.length}）</h2>
          <div class="msg-table-wrap">
            <table class="msg-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>名称</th>
                  <th>状态</th>
                  <th>申请时间</th>
                </tr>
              </thead>
              <tbody>
                ${list
                  .map(
                    (a) => `
                  <tr>
                    <td><code class="perm-badge">${esc(a.permCode)}</code></td>
                    <td>${esc(a.permName || '—')}</td>
                    <td>${formatAppStatus(a.status)}</td>
                    <td>${formatDateTime(a.createdAt)}</td>
                  </tr>`
                  )
                  .join('')}
              </tbody>
            </table>
          </div>
        </section>
      `;
    } catch (e) {
      subPanel.innerHTML = `<p class="home-card-err">${esc(e.message || '加载失败')}</p>`;
    }
  }

  try {
    await refreshMyPerms();
    renderPermShell();
  } catch {
    panel.innerHTML = '<p class="home-card-err">加载权限失败</p>';
  }
}

function formatAppStatus(status) {
  if (status === 'PENDING') return '<span class="home-tag perm-pending">审批中</span>';
  if (status === 'REJECTED') return '<span class="home-tag perm-rejected">已驳回</span>';
  return esc(status || '—');
}

function formatDateTime(raw) {
  if (!raw) return '—';
  const s = String(raw);
  const m = s.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})/);
  if (m) return `${m[1]} ${m[2]}`;
  return s.length > 19 ? s.slice(0, 19).replace('T', ' ') : s;
}

function showFormMsg(el, text, ok) {
  if (!el) return;
  el.textContent = text;
  el.classList.remove('home-hidden', 'home-form-ok', 'home-form-err');
  el.classList.add(ok ? 'home-form-ok' : 'home-form-err');
}

function shortId(id) {
  const s = String(id ?? '');
  if (s.length <= 12) return s;
  return `${s.slice(0, 8)}…${s.slice(-4)}`;
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escAttr(str) {
  return esc(str).replace(/"/g, '&quot;');
}
