import { session, subscribe, apiRequest } from '../api/client.js';
import { prefillEndpointCard } from './forms.js';

const TAB_IDS = ['templates', 'variables', 'carriers'];

/**
 * @param {HTMLElement} container
 */
export function renderMessageOverview(container) {
  container.innerHTML = `
    <div class="domain-overview message-overview" id="message-overview-root">
      <div class="home-overview-loading">加载中…</div>
    </div>
  `;

  const root = container.querySelector('#message-overview-root');
  let lastUserId = session.userId;
  let activeTab = 'templates';

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
      <div class="overview-tab-panel" id="msg-tab-panel"></div>
    `;

    el.querySelectorAll('.overview-tab').forEach((btn) => {
      btn.addEventListener('click', () => {
        renderShell(el, btn.dataset.tab);
      });
    });

    const panel = el.querySelector('#msg-tab-panel');
    if (tab === 'templates') loadTemplates(panel);
    else if (tab === 'variables') loadVariables(panel);
    else loadCarriers(panel);
  }
}

function tabLabel(id) {
  if (id === 'templates') return '模板';
  if (id === 'variables') return '变量';
  return '载体';
}

function renderLoggedOut(root) {
  root.innerHTML = `
    <div class="home-overview-empty glass">
      <p class="home-overview-empty-title">尚未登录</p>
      <p class="home-overview-empty-desc">请先在侧栏「账号」页登录，再浏览消息模板、变量与载体。</p>
    </div>
  `;
}

async function loadTemplates(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <div class="msg-toolbar">
        <input type="search" class="input msg-search" id="tpl-keyword" placeholder="搜索模板名称…" />
        <select class="input msg-select" id="tpl-channel">
          <option value="">全部渠道</option>
          <option value="EMAIL">EMAIL</option>
          <option value="IN_APP">IN_APP</option>
          <option value="TENCENT_SMS">TENCENT_SMS</option>
        </select>
        <select class="input msg-select" id="tpl-status">
          <option value="">全部状态</option>
          <option value="DRAFT">DRAFT</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
        <button type="button" class="btn btn-sm" id="tpl-search-btn">查询</button>
        <button type="button" class="btn btn-sm btn-ghost" id="tpl-refresh-btn">刷新</button>
      </div>
      <div id="tpl-list-wrap"><div class="home-overview-loading">加载模板…</div></div>
      <div id="tpl-detail-wrap" hidden></div>
    </section>
  `;

  let page = 1;
  const size = 10;

  const runSearch = () => {
    page = 1;
    fetchList();
  };

  panel.querySelector('#tpl-search-btn')?.addEventListener('click', runSearch);
  panel.querySelector('#tpl-refresh-btn')?.addEventListener('click', () => fetchList());
  panel.querySelector('#tpl-keyword')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') runSearch();
  });

  async function fetchList() {
    const wrap = panel.querySelector('#tpl-list-wrap');
    wrap.innerHTML = '<div class="home-overview-loading">加载模板…</div>';
    panel.querySelector('#tpl-detail-wrap').hidden = true;

    const keyword = panel.querySelector('#tpl-keyword')?.value.trim() || '';
    const channelType = panel.querySelector('#tpl-channel')?.value || '';
    const status = panel.querySelector('#tpl-status')?.value || '';

    const query = { page, size };
    if (keyword) query.keyword = keyword;
    if (channelType) query.channelType = channelType;
    if (status) query.status = status;

    try {
      const res = await apiRequest('GET', '/api/v1/templates', { query });
      const data = res.data?.code === 0 ? res.data.data : null;
      if (!data) {
        wrap.innerHTML = '<p class="home-card-err">无法加载模板列表</p>';
        return;
      }

      const list = data.list || [];
      const total = data.total ?? 0;
      const totalPages = Math.max(1, Math.ceil(total / size));

      if (list.length === 0) {
        wrap.innerHTML = '<p class="home-muted">暂无模板，可在右侧「创建模板」接口新建。</p>';
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
                <th>状态</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${list
                .map(
                  (t) => `
                <tr data-tpl='${escAttr(JSON.stringify(t))}'>
                  <td>${esc(t.templateId ?? t.id)}</td>
                  <td>${esc(t.name)}</td>
                  <td><span class="home-tag">${esc(t.channelType)}</span></td>
                  <td>${esc(t.status)}</td>
                  <td><button type="button" class="btn btn-sm btn-ghost tpl-detail-btn">详情</button></td>
                </tr>`
                )
                .join('')}
            </tbody>
          </table>
        </div>
        <div class="msg-pager">
          <span class="home-muted">共 ${total} 条 · 第 ${page}/${totalPages} 页</span>
          <div class="msg-pager-btns">
            <button type="button" class="btn btn-sm btn-ghost" id="tpl-prev" ${page <= 1 ? 'disabled' : ''}>上一页</button>
            <button type="button" class="btn btn-sm btn-ghost" id="tpl-next" ${page >= totalPages ? 'disabled' : ''}>下一页</button>
          </div>
        </div>
      `;

      wrap.querySelector('#tpl-prev')?.addEventListener('click', () => {
        if (page > 1) {
          page -= 1;
          fetchList();
        }
      });
      wrap.querySelector('#tpl-next')?.addEventListener('click', () => {
        if (page < totalPages) {
          page += 1;
          fetchList();
        }
      });

      wrap.querySelectorAll('.tpl-detail-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
          const row = btn.closest('tr');
          const tpl = JSON.parse(row.dataset.tpl);
          showTemplateDetail(panel, tpl);
        });
      });
    } catch {
      wrap.innerHTML = '<p class="home-card-err">加载失败，请确认已登录且服务可用。</p>';
    }
  }

  fetchList();
}

function showTemplateDetail(panel, tpl) {
  const detailWrap = panel.querySelector('#tpl-detail-wrap');
  const vars = extractTemplateVars(tpl.content || '');
  const tplId = tpl.templateId ?? tpl.id;

  detailWrap.hidden = false;
  detailWrap.innerHTML = `
    <div class="msg-detail-panel glass">
      <div class="msg-detail-header">
        <h3 class="home-card-title">模板详情 · ${esc(tpl.name)}</h3>
        <button type="button" class="btn btn-sm btn-ghost" id="tpl-detail-close">关闭</button>
      </div>
      <dl class="home-dl msg-detail-dl">
        <div><dt>ID</dt><dd>${esc(tplId)}</dd></div>
        <div><dt>渠道</dt><dd>${esc(tpl.channelType)}</dd></div>
        <div><dt>状态</dt><dd>${esc(tpl.status)}</dd></div>
      </dl>
      <div class="msg-detail-block">
        <span class="home-creds-label">内容</span>
        <pre class="msg-code-block">${esc(tpl.content || '—')}</pre>
      </div>
      <div class="msg-detail-block">
        <span class="home-creds-label">关联变量（${vars.length}）</span>
        ${
          vars.length
            ? `<div class="msg-var-tags">${vars.map((v) => `<span class="home-tag">\${${esc(v)}}</span>`).join('')}</div>`
            : '<p class="home-muted">内容中未检测到 ${varName} 占位符</p>'
        }
      </div>
      <p class="home-muted">可在右侧「API 调试」使用「即时发送」测试此模板。</p>
      <button type="button" class="btn btn-sm" id="tpl-prefill-send">预填即时发送</button>
    </div>
  `;

  detailWrap.querySelector('#tpl-detail-close')?.addEventListener('click', () => {
    detailWrap.hidden = true;
  });

  detailWrap.querySelector('#tpl-prefill-send')?.addEventListener('click', () => {
    const channel = tpl.channelType || 'EMAIL';
    prefillEndpointCard('msg-send-instant', {
      templateId: tplId,
      channel,
      variables: Object.fromEntries(vars.map((v) => [v, ''])),
    });
  });
}

async function loadVariables(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">变量定义规则</h2>
      <div id="var-schema-wrap"><div class="home-overview-loading">加载 Schema…</div></div>
      <hr class="msg-divider" />
      <h2 class="home-card-title">按 ID 查询变量</h2>
      <p class="home-muted">变量 CRUD 部分接口返回 501，查询仍可在右侧 Response 查看。</p>
      <div class="msg-toolbar">
        <input type="text" class="input msg-search" id="var-id-input" placeholder="variableId" />
        <button type="button" class="btn btn-sm" id="var-query-btn">查询</button>
      </div>
      <div id="var-query-result"></div>
    </section>
  `;

  const schemaWrap = panel.querySelector('#var-schema-wrap');

  try {
    const res = await apiRequest('GET', '/api/v1/variables/schema');
    const data = res.data?.code === 0 ? res.data.data : null;
    if (!data) {
      schemaWrap.innerHTML = '<p class="home-card-err">无法加载变量 Schema</p>';
    } else {
      const rules = data.rules || [];
      schemaWrap.innerHTML = `
        <p class="home-muted">占位符格式：<code>${esc(data.placeholderFormat || '${varName}')}</code></p>
        <ul class="msg-schema-list">
          ${rules
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
    }
  } catch {
    schemaWrap.innerHTML = '<p class="home-card-err">加载 Schema 失败</p>';
  }

  panel.querySelector('#var-query-btn')?.addEventListener('click', async () => {
    const variableId = panel.querySelector('#var-id-input')?.value.trim();
    const resultEl = panel.querySelector('#var-query-result');
    if (!variableId) {
      resultEl.innerHTML = '<p class="home-card-err">请输入 variableId</p>';
      return;
    }
    resultEl.innerHTML = '<div class="home-overview-loading">查询中…</div>';
    try {
      await apiRequest('GET', '/api/v1/variables/{variableId}', { pathParams: { variableId } });
      resultEl.innerHTML = '<p class="home-muted">请查看右侧 Response 面板。</p>';
    } catch (err) {
      resultEl.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  });
}

async function loadCarriers(panel) {
  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <div class="msg-toolbar">
        <select class="input msg-select" id="carrier-channel">
          <option value="">全部渠道</option>
          <option value="EMAIL">EMAIL</option>
          <option value="IN_APP">IN_APP</option>
          <option value="TENCENT_SMS">TENCENT_SMS</option>
        </select>
        <button type="button" class="btn btn-sm" id="carrier-load-btn">加载列表</button>
      </div>
      <div id="carrier-list-wrap"><div class="home-overview-loading">加载载体…</div></div>
      <div id="carrier-detail-wrap" hidden></div>
    </section>
  `;

  panel.querySelector('#carrier-load-btn')?.addEventListener('click', () => fetchCarriers(panel));
  fetchCarriers(panel);
}

async function fetchCarriers(panel) {
  const wrap = panel.querySelector('#carrier-list-wrap');
  wrap.innerHTML = '<div class="home-overview-loading">加载载体…</div>';
  panel.querySelector('#carrier-detail-wrap').hidden = true;

  const channelType = panel.querySelector('#carrier-channel')?.value || '';
  const query = channelType ? { channelType } : {};

  try {
    const res = await apiRequest('GET', '/api/v1/msg/carriers', { query });
    const data = res.data?.code === 0 ? res.data.data : null;
    const list = Array.isArray(data?.list) ? data.list : Array.isArray(data) ? data : data ? [data] : [];

    if (list.length === 0) {
      wrap.innerHTML = '<p class="home-muted">暂无载体记录。</p>';
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
              <th>Provider</th>
              <th>启用</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            ${list
              .map(
                (c) => `
              <tr>
                <td>${esc(c.carrierId ?? c.id)}</td>
                <td>${esc(c.name)}</td>
                <td><span class="home-tag">${esc(c.channelType)}</span></td>
                <td>${esc(c.provider || '—')}</td>
                <td>${c.enabled ? '是' : '否'}</td>
                <td><button type="button" class="btn btn-sm btn-ghost carrier-detail-btn" data-id="${esc(c.carrierId ?? c.id)}">详情</button></td>
              </tr>`
              )
              .join('')}
          </tbody>
        </table>
      </div>
    `;

    wrap.querySelectorAll('.carrier-detail-btn').forEach((btn) => {
      btn.addEventListener('click', () => showCarrierDetail(panel, btn.dataset.id));
    });
  } catch {
    wrap.innerHTML = '<p class="home-card-err">加载载体失败</p>';
  }
}

async function showCarrierDetail(panel, id) {
  const detailWrap = panel.querySelector('#carrier-detail-wrap');
  detailWrap.hidden = false;
  detailWrap.innerHTML = '<div class="home-overview-loading">加载详情…</div>';

  try {
    const res = await apiRequest('GET', '/api/v1/msg/carriers/{id}', { pathParams: { id } });
    const c = res.data?.code === 0 ? res.data.data : null;
    if (!c) {
      detailWrap.innerHTML = '<p class="home-card-err">无法加载载体详情</p>';
      return;
    }

    detailWrap.innerHTML = `
      <div class="msg-detail-panel glass">
        <div class="msg-detail-header">
          <h3 class="home-card-title">载体详情 · ${esc(c.name)}</h3>
          <button type="button" class="btn btn-sm btn-ghost" id="carrier-detail-close">关闭</button>
        </div>
        <dl class="home-dl msg-detail-dl">
          <div><dt>ID</dt><dd>${esc(c.carrierId ?? c.id)}</dd></div>
          <div><dt>渠道</dt><dd>${esc(c.channelType)}</dd></div>
          <div><dt>Provider</dt><dd>${esc(c.provider || '—')}</dd></div>
          <div><dt>启用</dt><dd>${c.enabled ? '是' : '否'}</dd></div>
        </dl>
        ${
          c.configJson
            ? `<div class="msg-detail-block"><span class="home-creds-label">配置（已脱敏）</span><pre class="msg-code-block">${esc(typeof c.configJson === 'string' ? c.configJson : JSON.stringify(c.configJson, null, 2))}</pre></div>`
            : ''
        }
        <p class="home-muted">修改/删除载体请至「管理」域。</p>
      </div>
    `;

    detailWrap.querySelector('#carrier-detail-close')?.addEventListener('click', () => {
      detailWrap.hidden = true;
    });
  } catch {
    detailWrap.innerHTML = '<p class="home-card-err">加载详情失败</p>';
  }
}

/** @param {string} content */
function extractTemplateVars(content) {
  const re = /\$\{(\w+)\}/g;
  const vars = new Set();
  let m;
  while ((m = re.exec(content)) !== null) {
    vars.add(m[1]);
  }
  return [...vars];
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function escAttr(str) {
  return String(str).replace(/&/g, '&amp;').replace(/'/g, '&#39;');
}
