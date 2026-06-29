import { session, subscribe, apiRequest } from '../api/client.js';
import { prefillEndpointCard } from './forms.js';

const MAIN_TAB_IDS = ['message', 'rbac'];

const MSG_SUB_TABS = [
  { id: 'templates', label: '模板' },
  { id: 'variables', label: '变量' },
  { id: 'carriers', label: '载体' },
];

const RBAC_SUB_TABS = [
  { id: 'users', label: '用户' },
  { id: 'groups', label: '分组' },
  { id: 'group-users', label: '组成员' },
  { id: 'permissions', label: '权限' },
  { id: 'group-permissions', label: '分组权限' },
  { id: 'user-permissions', label: '用户权限' },
];

const TPL_STATUSES = ['DRAFT', 'ACTIVE', 'DISABLED'];

/**
 * @param {HTMLElement} container
 */
export function renderAdminOverview(container) {
  container.innerHTML = `
    <div class="domain-overview admin-overview" id="admin-overview-root">
      <div class="home-overview-loading">加载中…</div>
    </div>
  `;

  const root = container.querySelector('#admin-overview-root');
  let lastAuth = authKey();
  let activeMainTab = 'message';

  subscribe(() => {
    const key = authKey();
    if (key === lastAuth) return;
    lastAuth = key;
    if (session.userId != null) {
      renderShell(root, activeMainTab);
    } else {
      renderLoggedOut(root);
    }
  });

  if (session.userId != null) {
    renderShell(root, activeMainTab);
  } else {
    renderLoggedOut(root);
  }

  function renderShell(el, mainTab) {
    activeMainTab = mainTab;
    el.innerHTML = `
      <div class="overview-tabs" role="tablist">
        ${MAIN_TAB_IDS.map(
          (id) => `
          <button type="button" class="overview-tab${mainTab === id ? ' active' : ''}" data-main-tab="${id}" role="tab">
            ${mainTabLabel(id)}
          </button>`
        ).join('')}
      </div>
      <div class="overview-tab-panel" id="admin-main-panel"></div>
    `;

    el.querySelectorAll('[data-main-tab]').forEach((btn) => {
      btn.addEventListener('click', () => renderShell(el, btn.dataset.mainTab));
    });

    const panel = el.querySelector('#admin-main-panel');
    if (mainTab === 'message') {
      renderMessageResourceTab(panel);
    } else {
      renderRbacTab(panel);
    }
  }
}

function authKey() {
  return `${session.userId ?? 'none'}:${session.isAdmin}`;
}

function mainTabLabel(id) {
  return id === 'message' ? '消息资源' : '用户与权限';
}

function renderLoggedOut(root) {
  root.innerHTML = `
    <div class="home-overview-empty glass">
      <p class="home-overview-empty-title">尚未登录</p>
      <p class="home-overview-empty-desc">请先在侧栏「账号」页登录，再使用管理域维护消息资源与 RBAC。</p>
    </div>
  `;
}

/** @param {HTMLElement} panel */
function renderMessageResourceTab(panel) {
  let activeSub = 'templates';

  function draw() {
    panel.innerHTML = `
      <p class="home-muted admin-section-hint">以下操作仅需登录（authc），写操作会同步预填右侧 API 调试卡片。</p>
      <div class="overview-tabs overview-tabs--nested" role="tablist">
        ${MSG_SUB_TABS.map(
          (t) => `
          <button type="button" class="overview-tab${activeSub === t.id ? ' active' : ''}" data-msg-sub="${t.id}" role="tab">
            ${t.label}
          </button>`
        ).join('')}
      </div>
      <div id="admin-msg-sub-panel"></div>
    `;

    panel.querySelectorAll('[data-msg-sub]').forEach((btn) => {
      btn.addEventListener('click', () => {
        activeSub = btn.dataset.msgSub;
        draw();
      });
    });

    const subPanel = panel.querySelector('#admin-msg-sub-panel');
    if (activeSub === 'templates') loadAdminTemplates(subPanel);
    else if (activeSub === 'variables') loadAdminVariables(subPanel);
    else loadAdminCarriers(subPanel);
  }

  draw();
}

/** @param {HTMLElement} panel */
function renderRbacTab(panel) {
  if (!session.isAdmin) {
    panel.innerHTML = `
      <div class="admin-banner admin-banner--inline admin-rbac-banner">
        <strong>需要 admin 权限</strong> — RBAC 接口需 admin 角色；请使用 userId=1 或 admin 账号。
      </div>
      <div class="home-overview-empty glass">
        <p class="home-overview-empty-title">需要 admin 权限</p>
        <p class="home-overview-empty-desc">当前账号无 admin 角色，无法查询用户与权限数据。</p>
      </div>
    `;
    return;
  }

  let activeSub = 'users';

  function draw() {
    panel.innerHTML = `
      <div class="admin-banner admin-banner--inline admin-rbac-banner">
        <strong>需要 admin 权限</strong> — 路径 /api/v1/admin/**；403 表示当前会话无权限。
      </div>
      <div class="overview-tabs overview-tabs--nested" role="tablist">
        ${RBAC_SUB_TABS.map(
          (t) => `
          <button type="button" class="overview-tab${activeSub === t.id ? ' active' : ''}" data-rbac-sub="${t.id}" role="tab">
            ${t.label}
          </button>`
        ).join('')}
      </div>
      <div id="admin-rbac-sub-panel"></div>
    `;

    panel.querySelectorAll('[data-rbac-sub]').forEach((btn) => {
      btn.addEventListener('click', () => {
        activeSub = btn.dataset.rbacSub;
        draw();
      });
    });

    const subPanel = panel.querySelector('#admin-rbac-sub-panel');
    switch (activeSub) {
      case 'users':
        loadAdminUsers(subPanel);
        break;
      case 'groups':
        loadAdminGroups(subPanel);
        break;
      case 'group-users':
        loadAdminGroupUsers(subPanel);
        break;
      case 'permissions':
        loadAdminPermissions(subPanel);
        break;
      case 'group-permissions':
        loadAdminGroupPermissions(subPanel);
        break;
      default:
        loadAdminUserPermissions(subPanel);
    }
  }

  draw();
}

/** @param {HTMLElement} panel */
function loadAdminTemplates(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <div class="msg-toolbar">
        <input type="search" class="input msg-search" id="adm-tpl-keyword" placeholder="搜索模板名称…" />
        <select class="input msg-select" id="adm-tpl-channel">
          <option value="">全部渠道</option>
          <option value="EMAIL">EMAIL</option>
          <option value="IN_APP">IN_APP</option>
          <option value="TENCENT_SMS">TENCENT_SMS</option>
        </select>
        <select class="input msg-select" id="adm-tpl-status-filter">
          <option value="">全部状态</option>
          ${TPL_STATUSES.map((s) => `<option value="${s}">${s}</option>`).join('')}
        </select>
        <button type="button" class="btn btn-sm" id="adm-tpl-search">查询</button>
      </div>
      <div id="adm-tpl-list"><div class="home-overview-loading">加载模板…</div></div>
    </section>
  `;

  let page = 1;
  const size = 10;

  const fetchList = async () => {
    const wrap = panel.querySelector('#adm-tpl-list');
    wrap.innerHTML = '<div class="home-overview-loading">加载模板…</div>';

    const keyword = panel.querySelector('#adm-tpl-keyword')?.value.trim() || '';
    const channelType = panel.querySelector('#adm-tpl-channel')?.value || '';
    const status = panel.querySelector('#adm-tpl-status-filter')?.value || '';
    const query = { page, size };
    if (keyword) query.keyword = keyword;
    if (channelType) query.channelType = channelType;
    if (status) query.status = status;

    try {
      const res = await apiRequest('GET', '/api/v1/templates', { query });
      const data = res.data?.code === 0 ? res.data.data : null;
      if (!data) {
        wrap.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '无法加载模板')}</p>`;
        return;
      }

      const list = data.list || [];
      const total = Number(data.total ?? 0);
      const totalPages = Math.max(1, Math.ceil(total / size));

      if (!list.length) {
        wrap.innerHTML = '<p class="home-muted">暂无模板。</p>';
        return;
      }

      wrap.innerHTML = `
        <div class="msg-table-wrap">
          <table class="msg-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>渠道</th>
                <th>当前状态</th>
                <th>更新状态</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${list
                .map((t) => {
                  const id = t.templateId ?? t.id;
                  return `
                <tr>
                  <td>${esc(id)}</td>
                  <td>${esc(t.name)}</td>
                  <td><span class="home-tag">${esc(t.channelType)}</span></td>
                  <td>${esc(t.status)}</td>
                  <td>
                    <select class="input input-sm adm-tpl-status-select" data-id="${escAttr(String(id))}">
                      ${TPL_STATUSES.map(
                        (s) => `<option value="${s}"${t.status === s ? ' selected' : ''}>${s}</option>`
                      ).join('')}
                    </select>
                  </td>
                  <td class="msg-table-actions">
                    <button type="button" class="btn btn-sm adm-tpl-save" data-id="${escAttr(String(id))}">更新</button>
                  </td>
                </tr>`;
                })
                .join('')}
            </tbody>
          </table>
        </div>
        ${renderSimplePager({ page, totalPages, total, idPrefix: 'adm-tpl' })}
        <div id="adm-tpl-msg" class="home-muted"></div>
      `;

      bindSimplePager(wrap, 'adm-tpl', page, totalPages, (p) => {
        page = p;
        fetchList();
      });

      wrap.querySelectorAll('.adm-tpl-save').forEach((btn) => {
        btn.addEventListener('click', async () => {
          const id = btn.dataset.id;
          const row = btn.closest('tr');
          const status = row?.querySelector('.adm-tpl-status-select')?.value;
          if (!status) return;

          const msgEl = panel.querySelector('#adm-tpl-msg');
          btn.disabled = true;
          prefillAdminEndpoint('msg-template-status', { pathParams: { id }, body: { status } });

          try {
            const res = await apiRequest('PUT', '/api/v1/templates/{id}/status', {
              pathParams: { id },
              body: { status },
            });
            if (res.data?.code === 0) {
              if (msgEl) msgEl.textContent = `模板 ${id} 状态已更新为 ${status}`;
              fetchList();
            } else if (msgEl) {
              msgEl.innerHTML = `<span class="home-card-err">${esc(res.data?.message || '更新失败')}</span>`;
            }
          } catch (err) {
            if (msgEl) msgEl.innerHTML = `<span class="home-card-err">${esc(err.message || String(err))}</span>`;
          } finally {
            btn.disabled = false;
          }
        });
      });
    } catch (err) {
      wrap.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  };

  panel.querySelector('#adm-tpl-search')?.addEventListener('click', () => {
    page = 1;
    fetchList();
  });
  panel.querySelector('#adm-tpl-keyword')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      page = 1;
      fetchList();
    }
  });

  fetchList();
}

/** @param {HTMLElement} panel */
function loadAdminVariables(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">变量 Schema</h2>
      <div id="adm-var-schema"><div class="home-overview-loading">加载…</div></div>
      <hr class="msg-divider" />
      <h2 class="home-card-title">按 ID 查询 / 维护</h2>
      <p class="home-muted">修改变量/删除接口可能返回 501；可通过右侧 API 调试发送。</p>
      <div class="msg-toolbar">
        <input type="text" class="input msg-search" id="adm-var-id" placeholder="variableId" />
        <button type="button" class="btn btn-sm" id="adm-var-query">查询</button>
      </div>
      <div id="adm-var-result"></div>
    </section>
  `;

  const schemaWrap = panel.querySelector('#adm-var-schema');
  apiRequest('GET', '/api/v1/variables/schema')
    .then((res) => {
      const data = res.data?.code === 0 ? res.data.data : null;
      if (!data) {
        schemaWrap.innerHTML = '<p class="home-card-err">无法加载 Schema</p>';
        return;
      }
      schemaWrap.innerHTML = `
        <p class="home-muted">占位符：<code>${esc(data.placeholderFormat || '${varName}')}</code></p>
        <ul class="msg-schema-list">
          ${(data.rules || [])
            .map(
              (r) => `
            <li class="glass msg-schema-item">
              <strong>${esc(r.name)}</strong>
              ${r.required ? '<span class="home-tag">必填</span>' : ''}
              <span class="home-muted">${esc(r.description || '')}</span>
            </li>`
            )
            .join('')}
        </ul>
      `;
    })
    .catch(() => {
      schemaWrap.innerHTML = '<p class="home-card-err">加载 Schema 失败</p>';
    });

  panel.querySelector('#adm-var-query')?.addEventListener('click', async () => {
    const variableId = panel.querySelector('#adm-var-id')?.value.trim();
    const resultEl = panel.querySelector('#adm-var-result');
    if (!variableId) {
      resultEl.innerHTML = '<p class="home-card-err">请输入 variableId</p>';
      return;
    }

    resultEl.innerHTML = '<div class="home-overview-loading">查询中…</div>';
    try {
      const res = await apiRequest('GET', '/api/v1/variables/{variableId}', { pathParams: { variableId } });
      const row = res.data?.code === 0 ? res.data.data : null;
      if (!row) {
        resultEl.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '未找到')}</p>`;
        return;
      }

      resultEl.innerHTML = `
        <div class="msg-detail-panel glass">
          <dl class="home-dl msg-detail-dl">
            <div><dt>ID</dt><dd>${esc(row.variableId ?? row.id ?? variableId)}</dd></div>
            <div><dt>名称</dt><dd>${esc(row.name ?? '—')}</dd></div>
          </dl>
          <div class="home-action-row">
            <button type="button" class="btn btn-sm btn-ghost" id="adm-var-edit">预填修改</button>
            <button type="button" class="btn btn-sm btn-ghost" id="adm-var-del">预填删除</button>
          </div>
        </div>
      `;

      resultEl.querySelector('#adm-var-edit')?.addEventListener('click', () => {
        prefillAdminEndpoint('msg-var-update', {
          pathParams: { variableId },
          body: { name: row.name ?? '' },
        });
      });
      resultEl.querySelector('#adm-var-del')?.addEventListener('click', () => {
        prefillAdminEndpoint('msg-var-delete', { pathParams: { variableId } });
      });
    } catch (err) {
      resultEl.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  });
}

/** @param {HTMLElement} panel */
function loadAdminCarriers(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">新增载体</h2>
      <div class="log-field-inputs admin-carrier-form">
        <div class="log-inline-field">
          <label class="label">渠道</label>
          <select class="input" id="adm-carrier-channel">
            <option value="EMAIL">EMAIL</option>
            <option value="IN_APP">IN_APP</option>
            <option value="TENCENT_SMS">TENCENT_SMS</option>
          </select>
        </div>
        <div class="log-inline-field">
          <label class="label">名称</label>
          <input class="input" type="text" id="adm-carrier-name" placeholder="QQ SMTP" />
        </div>
        <div class="log-inline-field">
          <label class="label">配置 JSON</label>
          <input class="input" type="text" id="adm-carrier-config" placeholder='{"host":"smtp.qq.com"}' />
        </div>
      </div>
      <div class="home-action-row">
        <button type="button" class="btn btn-sm" id="adm-carrier-create">创建</button>
        <button type="button" class="btn btn-sm btn-ghost" id="adm-carrier-prefill">预填右侧 API</button>
      </div>
      <p id="adm-carrier-msg" class="home-muted"></p>
      <hr class="msg-divider" />
      <div class="msg-toolbar">
        <select class="input msg-select" id="adm-carrier-filter">
          <option value="">全部渠道</option>
          <option value="EMAIL">EMAIL</option>
          <option value="IN_APP">IN_APP</option>
          <option value="TENCENT_SMS">TENCENT_SMS</option>
        </select>
        <button type="button" class="btn btn-sm" id="adm-carrier-refresh">刷新列表</button>
      </div>
      <div id="adm-carrier-list"><div class="home-overview-loading">加载载体…</div></div>
    </section>
  `;

  const buildCreateBody = () => {
    const channelType = panel.querySelector('#adm-carrier-channel')?.value || 'EMAIL';
    const name = panel.querySelector('#adm-carrier-name')?.value.trim() || '';
    let config = {};
    const raw = panel.querySelector('#adm-carrier-config')?.value.trim();
    if (raw) {
      try {
        config = JSON.parse(raw);
      } catch {
        config = { host: raw };
      }
    }
    return { channelType, name, config };
  };

  panel.querySelector('#adm-carrier-prefill')?.addEventListener('click', () => {
    prefillEndpointCard('msg-carrier-create', buildCreateBody());
  });

  panel.querySelector('#adm-carrier-create')?.addEventListener('click', async () => {
    const body = buildCreateBody();
    const msgEl = panel.querySelector('#adm-carrier-msg');
    if (!body.name) {
      if (msgEl) msgEl.innerHTML = '<span class="home-card-err">请填写名称</span>';
      return;
    }
    prefillEndpointCard('msg-carrier-create', body);
    try {
      const res = await apiRequest('POST', '/api/v1/msg/carriers', { body });
      if (res.data?.code === 0) {
        if (msgEl) msgEl.textContent = '载体创建成功';
        fetchCarriers();
      } else if (msgEl) {
        msgEl.innerHTML = `<span class="home-card-err">${esc(res.data?.message || '创建失败')}</span>`;
      }
    } catch (err) {
      if (msgEl) msgEl.innerHTML = `<span class="home-card-err">${esc(err.message || String(err))}</span>`;
    }
  });

  panel.querySelector('#adm-carrier-refresh')?.addEventListener('click', fetchCarriers);

  async function fetchCarriers() {
    const wrap = panel.querySelector('#adm-carrier-list');
    wrap.innerHTML = '<div class="home-overview-loading">加载载体…</div>';

    const channelType = panel.querySelector('#adm-carrier-filter')?.value || '';
    const query = channelType ? { channelType } : {};

    try {
      const res = await apiRequest('GET', '/api/v1/msg/carriers', { query });
      const data = res.data?.code === 0 ? res.data.data : null;
      const list = Array.isArray(data?.list) ? data.list : Array.isArray(data) ? data : data ? [data] : [];

      if (!list.length) {
        wrap.innerHTML = '<p class="home-muted">暂无载体。</p>';
        return;
      }

      wrap.innerHTML = `
        <div class="msg-table-wrap">
          <table class="msg-table">
            <thead>
              <tr><th>ID</th><th>名称</th><th>渠道</th><th>启用</th><th></th></tr>
            </thead>
            <tbody>
              ${list
                .map((c) => {
                  const id = String(c.carrierId ?? c.id);
                  return `
                <tr>
                  <td>${esc(id)}</td>
                  <td>${esc(c.name)}</td>
                  <td><span class="home-tag">${esc(c.channelType)}</span></td>
                  <td>${c.enabled ? '是' : '否'}</td>
                  <td class="msg-table-actions">
                    <button type="button" class="btn btn-sm btn-ghost adm-carrier-edit" data-id="${escAttr(id)}" data-name="${escAttr(c.name || '')}">修改</button>
                    <button type="button" class="btn btn-sm btn-ghost adm-carrier-del" data-id="${escAttr(id)}">删除</button>
                  </td>
                </tr>`;
                })
                .join('')}
            </tbody>
          </table>
        </div>
      `;

      wrap.querySelectorAll('.adm-carrier-edit').forEach((btn) => {
        btn.addEventListener('click', () => {
          prefillAdminEndpoint('msg-carrier-update', {
            pathParams: { id: btn.dataset.id },
            body: { name: btn.dataset.name },
          });
        });
      });
      wrap.querySelectorAll('.adm-carrier-del').forEach((btn) => {
        btn.addEventListener('click', () => {
          prefillAdminEndpoint('msg-carrier-delete', { pathParams: { id: btn.dataset.id } });
        });
      });
    } catch (err) {
      wrap.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  }

  fetchCarriers();
}

/** @param {HTMLElement} panel */
function loadAdminUsers(panel) {
  renderSearchPanel(panel, {
    title: '用户列表',
    keywordPlaceholder: '搜索 displayName…',
    statusFilter: true,
    onSearch: async ({ keyword, status, page, size }) => {
      const query = { page, size };
      if (keyword) query.keyword = keyword;
      if (status) query.status = status;
      prefillEndpointCard('admin-user-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/users', { query });
      return res.data;
    },
    columns: [
      { key: 'userId', label: '用户 ID' },
      { key: 'displayName', label: '显示名' },
      { key: 'status', label: '状态' },
      { key: 'createdAt', label: '注册时间', fmt: formatDateTime },
    ],
    rowActions: (row) => [
      {
        label: '调试修改',
        onClick: () =>
          prefillAdminEndpoint('admin-user-update', {
            body: { userId: row.userId, nickname: row.displayName },
          }),
      },
      {
        label: '调试删除',
        onClick: () =>
          prefillAdminEndpoint('admin-user-delete', {
            body: { userId: row.userId },
          }),
      },
    ],
  });
}

/** @param {HTMLElement} panel */
function loadAdminGroups(panel) {
  renderSearchPanel(panel, {
    title: '分组列表',
    keywordPlaceholder: '搜索分组名…',
    onSearch: async ({ keyword, page, size }) => {
      const query = { page, size };
      if (keyword) query.keyword = keyword;
      prefillEndpointCard('admin-group-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/groups', { query });
      return res.data;
    },
    columns: [
      { key: 'groupId', label: '分组 ID' },
      { key: 'name', label: '名称' },
      { key: 'description', label: '描述' },
      { key: 'isAdmin', label: 'Admin 组', fmt: (v) => (v ? '是' : '否') },
    ],
    rowActions: (row) => [
      {
        label: '调试修改',
        onClick: () =>
          prefillAdminEndpoint('admin-group-update', {
            body: { groupId: row.groupId, name: row.name },
          }),
      },
      {
        label: '调试删除',
        onClick: () =>
          prefillAdminEndpoint('admin-group-delete', {
            body: { groupId: row.groupId },
          }),
      },
    ],
  });
}

/** @param {HTMLElement} panel */
function loadAdminGroupUsers(panel) {
  renderSearchPanel(panel, {
    title: '分组成员',
    requiredFilter: { key: 'groupId', label: 'groupId', placeholder: '分组 ID（必填）' },
    onSearch: async ({ filters, page, size }) => {
      const query = { page, size, groupId: filters.groupId };
      prefillEndpointCard('admin-group-user-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/group-users', { query });
      return res.data;
    },
    columns: [
      { key: 'userId', label: '用户 ID' },
      { key: 'displayName', label: '显示名' },
      { key: 'status', label: '状态' },
    ],
    toolbarExtra: `
      <button type="button" class="btn btn-sm btn-ghost" id="adm-gu-add-prefill">预填添加成员</button>
    `,
    onMount: (panelEl) => {
      panelEl.querySelector('#adm-gu-add-prefill')?.addEventListener('click', () => {
        const groupId = panelEl.querySelector('[data-filter="groupId"]')?.value.trim();
        prefillAdminEndpoint('admin-group-user-add', {
          body: { groupId: groupId ? Number(groupId) : '', userId: '' },
        });
      });
    },
    rowActions: (row, panelEl) => {
      const groupId = panelEl.querySelector('[data-filter="groupId"]')?.value.trim();
      return [
        {
          label: '移除',
          onClick: () =>
            prefillAdminEndpoint('admin-group-user-remove', {
              body: { groupId: groupId ? Number(groupId) : '', userId: row.userId },
            }),
        },
      ];
    },
  });
}

/** @param {HTMLElement} panel */
function loadAdminPermissions(panel) {
  renderSearchPanel(panel, {
    title: '权限列表',
    keywordPlaceholder: '搜索 permCode / 名称…',
    onSearch: async ({ keyword, page, size }) => {
      const query = { page, size };
      if (keyword) query.keyword = keyword;
      prefillEndpointCard('admin-perm-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/permissions', { query });
      return res.data;
    },
    columns: [
      { key: 'permId', label: 'ID' },
      { key: 'permCode', label: 'Code', fmt: (v) => `<code class="perm-badge">${esc(v)}</code>` },
      { key: 'permName', label: '名称' },
    ],
    rowActions: (row) => [
      {
        label: '调试修改',
        onClick: () =>
          prefillAdminEndpoint('admin-perm-update', {
            body: { permId: row.permId, permName: row.permName },
          }),
      },
      {
        label: '调试删除',
        onClick: () =>
          prefillAdminEndpoint('admin-perm-delete', {
            body: { permId: row.permId },
          }),
      },
    ],
  });
}

/** @param {HTMLElement} panel */
function loadAdminGroupPermissions(panel) {
  renderSearchPanel(panel, {
    title: '分组权限',
    requiredFilter: { key: 'groupId', label: 'groupId', placeholder: '分组 ID（必填）' },
    paginated: false,
    onSearch: async ({ filters }) => {
      const query = { groupId: filters.groupId };
      prefillEndpointCard('admin-gp-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/group-permissions', { query });
      return res.data;
    },
    columns: [
      { key: 'id', label: '关联 ID' },
      { key: 'groupId', label: '分组 ID' },
      { key: 'permCode', label: 'Code', fmt: (v) => `<code class="perm-badge">${esc(v)}</code>` },
      { key: 'permName', label: '名称' },
    ],
    rowActions: (row) => [
      {
        label: '调试删除',
        onClick: () =>
          prefillAdminEndpoint('admin-gp-delete', {
            body: { id: row.id },
          }),
      },
      {
        label: '审批',
        onClick: () =>
          prefillAdminEndpoint('admin-gp-approve', {
            pathParams: { id: row.id },
          }),
      },
    ],
  });
}

/** @param {HTMLElement} panel */
function loadAdminUserPermissions(panel) {
  renderSearchPanel(panel, {
    title: '用户权限',
    requiredFilter: { key: 'userId', label: 'userId', placeholder: '用户 ID（必填）' },
    statusFilter: true,
    paginated: false,
    onSearch: async ({ filters, status }) => {
      const query = { userId: filters.userId };
      if (status) query.status = status;
      prefillEndpointCard('admin-up-search', query);
      const res = await apiRequest('GET', '/api/v1/admin/user-permissions', { query });
      return res.data;
    },
    columns: [
      { key: 'id', label: '关联 ID' },
      { key: 'permCode', label: 'Code', fmt: (v) => `<code class="perm-badge">${esc(v)}</code>` },
      { key: 'permName', label: '名称' },
      { key: 'status', label: '状态' },
    ],
    rowActions: (row) => {
      const actions = [
        {
          label: '调试删除',
          onClick: () =>
            prefillAdminEndpoint('admin-up-delete', {
              body: { id: row.id },
            }),
        },
      ];
      if (row.status === 'PENDING') {
        actions.push(
          {
            label: '批准',
            onClick: () =>
              prefillAdminEndpoint('admin-up-approve', {
                pathParams: { id: row.id },
              }),
          },
          {
            label: '拒绝',
            onClick: () =>
              prefillAdminEndpoint('admin-up-reject', {
                pathParams: { id: row.id },
              }),
          }
        );
      }
      return actions;
    },
  });
}

/**
 * @param {HTMLElement} panel
 * @param {{
 *   title: string,
 *   keywordPlaceholder?: string,
 *   statusFilter?: boolean,
 *   requiredFilter?: { key: string, label: string, placeholder: string },
 *   paginated?: boolean,
 *   toolbarExtra?: string,
 *   onMount?: (panel: HTMLElement) => void,
 *   onSearch: (opts: { keyword?: string, status?: string, page: number, size: number, filters: Record<string, string> }) => Promise<{ code?: number, message?: string, data?: { list?: unknown[], total?: number } } | null>,
 *   columns: { key: string, label: string, fmt?: (v: unknown, row: Record<string, unknown>) => string }[],
 *   rowActions?: (row: Record<string, unknown>, panel: HTMLElement) => { label: string, onClick: () => void }[],
 * }} spec
 */
function renderSearchPanel(panel, spec) {
  const paginated = spec.paginated !== false;
  let page = 1;
  const size = 10;

  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">${esc(spec.title)}</h2>
      <div class="msg-toolbar">
        ${
          spec.requiredFilter
            ? `<input class="input msg-search" type="number" min="1" data-filter="${escAttr(spec.requiredFilter.key)}" placeholder="${escAttr(spec.requiredFilter.placeholder)}" />`
            : ''
        }
        ${
          spec.keywordPlaceholder
            ? `<input class="input msg-search" type="search" id="adm-search-keyword" placeholder="${escAttr(spec.keywordPlaceholder)}" />`
            : ''
        }
        ${
          spec.statusFilter
            ? `
          <select class="input msg-select" id="adm-search-status">
            <option value="">全部状态</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="PENDING">PENDING</option>
            <option value="REJECTED">REJECTED</option>
          </select>`
            : ''
        }
        <button type="button" class="btn btn-sm" id="adm-search-btn">查询</button>
        ${spec.toolbarExtra || ''}
      </div>
      <div id="adm-search-wrap"><p class="home-muted">点击查询加载数据。</p></div>
    </section>
  `;

  spec.onMount?.(panel);

  const fetchData = async () => {
    const wrap = panel.querySelector('#adm-search-wrap');
    wrap.innerHTML = '<div class="home-overview-loading">查询中…</div>';

    const filters = {};
    panel.querySelectorAll('[data-filter]').forEach((el) => {
      filters[el.dataset.filter] = el.value.trim();
    });

    if (spec.requiredFilter && !filters[spec.requiredFilter.key]) {
      wrap.innerHTML = `<p class="home-card-err">请填写 ${esc(spec.requiredFilter.label)}</p>`;
      return;
    }

    const keyword = panel.querySelector('#adm-search-keyword')?.value.trim() || '';
    const status = panel.querySelector('#adm-search-status')?.value || '';

    try {
      const payload = await spec.onSearch({ keyword, status, page, size, filters });
      if (!payload || payload.code !== 0) {
        wrap.innerHTML = `<p class="home-card-err">${esc(payload?.message || '查询失败')}</p>`;
        return;
      }

      const list = /** @type {Record<string, unknown>[]} */ (payload.data?.list || []);
      const total = Number(payload.data?.total ?? list.length);
      const totalPages = paginated ? Math.max(1, Math.ceil(total / size)) : 1;

      if (!list.length) {
        wrap.innerHTML = '<p class="home-muted">无匹配记录。</p>';
        return;
      }

      wrap.innerHTML = `
        <div class="msg-table-wrap">
          <table class="msg-table">
            <thead>
              <tr>
                ${spec.columns.map((c) => `<th>${esc(c.label)}</th>`).join('')}
                ${spec.rowActions ? '<th></th>' : ''}
              </tr>
            </thead>
            <tbody>
              ${list
                .map((row) => {
                  const cells = spec.columns
                    .map((c) => {
                      const raw = row[c.key];
                      const html = c.fmt ? c.fmt(raw, row) : esc(raw);
                      return `<td>${html}</td>`;
                    })
                    .join('');
                  const actions = spec.rowActions?.(row, panel) || [];
                  const actionHtml = actions.length
                    ? `<td class="msg-table-actions">${actions
                        .map(
                          (a, i) =>
                            `<button type="button" class="btn btn-sm btn-ghost adm-row-action" data-row="${escAttr(String(row.id ?? row.userId ?? row.groupId ?? i))}" data-action="${i}">${esc(a.label)}</button>`
                        )
                        .join(' ')}</td>`
                    : '';
                  return `<tr data-row-key="${escAttr(JSON.stringify(row))}">${cells}${actionHtml}</tr>`;
                })
                .join('')}
            </tbody>
          </table>
        </div>
        ${paginated ? renderSimplePager({ page, totalPages, total, idPrefix: 'adm-search' }) : `<p class="home-muted">共 ${list.length} 条</p>`}
      `;

      if (paginated) {
        bindSimplePager(wrap, 'adm-search', page, totalPages, (p) => {
          page = p;
          fetchData();
        });
      }

      wrap.querySelectorAll('tr[data-row-key]').forEach((tr) => {
        const row = JSON.parse(tr.dataset.rowKey);
        const actions = spec.rowActions?.(row, panel) || [];
        tr.querySelectorAll('.adm-row-action').forEach((btn) => {
          const idx = Number(btn.dataset.action);
          btn.addEventListener('click', () => actions[idx]?.onClick());
        });
      });
    } catch (err) {
      wrap.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  };

  panel.querySelector('#adm-search-btn')?.addEventListener('click', () => {
    page = 1;
    fetchData();
  });
  panel.querySelector('#adm-search-keyword')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      page = 1;
      fetchData();
    }
  });

  if (!spec.requiredFilter) {
    fetchData();
  }
}

/**
 * @param {string} endpointId
 * @param {{ body?: object, pathParams?: Record<string, string|number> }} [opts]
 */
function prefillAdminEndpoint(endpointId, opts = {}) {
  if (opts.body) prefillEndpointCard(endpointId, opts.body);
  else prefillEndpointCard(endpointId, {});

  const card = document.querySelector(`.endpoint-card[data-id="${endpointId}"]`);
  if (!card) return;
  card.classList.add('open');

  if (opts.pathParams) {
    for (const [k, v] of Object.entries(opts.pathParams)) {
      const input = card.querySelector(`.path-param[data-param="${k}"]`);
      if (input) input.value = String(v);
    }
  }
}

/** @param {{ page: number, totalPages: number, total: number, idPrefix: string }} opts */
function renderSimplePager(opts) {
  const { page, totalPages, total, idPrefix } = opts;
  return `
    <div class="msg-pager">
      <span class="home-muted">共 ${total.toLocaleString()} 条 · 第 ${page}/${totalPages} 页</span>
      <div class="msg-pager-btns">
        <button type="button" class="btn btn-sm btn-ghost" id="${idPrefix}-prev" ${page <= 1 ? 'disabled' : ''}>上一页</button>
        <button type="button" class="btn btn-sm btn-ghost" id="${idPrefix}-next" ${page >= totalPages ? 'disabled' : ''}>下一页</button>
      </div>
    </div>`;
}

/** @param {HTMLElement} root @param {string} idPrefix @param {number} page @param {number} totalPages @param {(p: number) => void} onChange */
function bindSimplePager(root, idPrefix, page, totalPages, onChange) {
  root.querySelector(`#${idPrefix}-prev`)?.addEventListener('click', () => {
    if (page > 1) onChange(page - 1);
  });
  root.querySelector(`#${idPrefix}-next`)?.addEventListener('click', () => {
    if (page < totalPages) onChange(page + 1);
  });
}

function formatDateTime(v) {
  if (v == null || v === '') return '—';
  try {
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return String(v);
    return d.toLocaleString();
  } catch {
    return String(v);
  }
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escAttr(str) {
  return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
}
