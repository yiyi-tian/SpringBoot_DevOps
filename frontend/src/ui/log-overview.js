import { session, subscribe, apiRequest } from '../api/client.js';
import { prefillEndpointCard } from './forms.js';
import {
  TIME_RANGES,
  TIME_MODES,
  SORT_FIELDS,
  SORT_ORDERS,
  FILTER_GROUPS,
  METRIC_GROUPS,
  METRIC_SOURCES,
  SERVICE_NAMES,
  AUDIT_OPERATIONS,
  AGGREGATE_METRICS,
  RANK_METRICS,
  createDefaultOpsState,
  createDefaultMetricState,
  createDefaultAuditFilterState,
  buildOpsQueryBody,
  buildMetricQueryParams,
  buildAuditQueryParams,
  summarizeActiveFilters,
  clearFilterTag,
  getMetricDef,
  formatMetricValue,
  getTimeRangeLabel,
  suggestMetricSource,
} from '../api/log-query-spec.js';

const TAB_IDS = ['ops', 'audit', 'metrics'];

/** @type {ReturnType<typeof createDefaultOpsState> | null} */
let opsState = null;

/** @type {ReturnType<typeof createDefaultMetricState> | null} */
let metricState = null;

/** @type {ReturnType<typeof createDefaultAuditFilterState> | null} */
let auditFilterState = null;

/**
 * @param {HTMLElement} container
 */
export function renderLogOverview(container) {
  container.innerHTML = `
    <div class="domain-overview log-overview" id="log-overview-root">
      <div class="home-overview-loading">加载中…</div>
    </div>
  `;

  const root = container.querySelector('#log-overview-root');
  let lastAuth = authKey();
  let activeTab = 'ops';

  if (!opsState) opsState = createDefaultOpsState();
  if (!metricState) metricState = createDefaultMetricState();
  if (!auditFilterState) auditFilterState = createDefaultAuditFilterState();

  subscribe(() => {
    const key = authKey();
    if (key === lastAuth) return;
    lastAuth = key;
    if (canAccessLog()) {
      renderShell(root, activeTab);
    } else {
      renderAccessDenied(root);
    }
  });

  if (canAccessLog()) {
    renderShell(root, activeTab);
  } else {
    renderAccessDenied(root);
  }

  function renderShell(el, tab) {
    activeTab = tab;
    el.innerHTML = `
      <div class="admin-banner admin-banner--inline log-overview-banner">
        <strong>需要 admin 权限</strong> — 日志域接口需 admin 角色；首个 userId=1 或 identifier=admin 账号可访问。
      </div>
      <div class="overview-tabs" role="tablist">
        ${TAB_IDS.map(
          (id) => `
          <button type="button" class="overview-tab${tab === id ? ' active' : ''}" data-tab="${id}" role="tab">
            ${tabLabel(id)}
          </button>`
        ).join('')}
      </div>
      <div class="overview-tab-panel" id="log-tab-panel"></div>
    `;

    el.querySelectorAll('.overview-tab').forEach((btn) => {
      btn.addEventListener('click', () => renderShell(el, btn.dataset.tab));
    });

    const panel = el.querySelector('#log-tab-panel');
    if (tab === 'ops') renderOpsPanel(panel);
    else if (tab === 'audit') renderAuditPanel(panel);
    else renderMetricsPanel(panel);
  }
}

function authKey() {
  return `${session.userId ?? 'none'}:${session.isAdmin}`;
}

function canAccessLog() {
  return session.userId != null && session.isAdmin;
}

function tabLabel(id) {
  if (id === 'ops') return '运维日志';
  if (id === 'audit') return '审计日志';
  return '日志指标';
}

function renderAccessDenied(root) {
  const msg =
    session.userId == null
      ? '请先在侧栏「账号」页登录。'
      : '当前账号无 admin 角色，无法查询日志。请使用首个注册用户 (userId=1) 或 admin 账号。';
  root.innerHTML = `
    <div class="home-overview-empty glass">
      <p class="home-overview-empty-title">${session.userId == null ? '尚未登录' : '需要 admin 权限'}</p>
      <p class="home-overview-empty-desc">${msg}</p>
    </div>
  `;
}

/** @param {HTMLElement} panel */
function renderOpsPanel(panel) {
  const state = opsState;
  const tags = summarizeActiveFilters(state);

  panel.innerHTML = `
    <section class="home-card glass msg-panel log-query-panel">
      <h2 class="home-card-title">时间范围</h2>
      ${renderTimeRangeHtml(state, 'ops')}

      <h2 class="home-card-title">排序</h2>
      <div class="log-chip-row">
        <span class="log-chip-label">字段</span>
        ${SORT_FIELDS.map(
          (f) => `
          <button type="button" class="log-filter-chip log-sort-field${state.sortField === f.id ? ' active' : ''}" data-field="${f.id}">
            ${f.label}
          </button>`
        ).join('')}
      </div>
      <div class="log-chip-row">
        <span class="log-chip-label">顺序</span>
        ${SORT_ORDERS.map(
          (o) => `
          <button type="button" class="log-filter-chip log-sort-order${state.sortOrder === o.id ? ' active' : ''}" data-order="${o.id}">
            ${o.label}
          </button>`
        ).join('')}
      </div>

      ${FILTER_GROUPS.map(
        (group) => `
        <h2 class="home-card-title">${group.title}</h2>
        <div class="log-chip-row" data-group="${group.title}">
          ${group.filters
            .map((f) => {
              if (f.kind === 'service') {
                const on = state.services.includes(f.serviceValue);
                return `<button type="button" class="log-filter-chip log-service-chip${on ? ' active' : ''}" data-service="${f.serviceValue}">${f.label}</button>`;
              }
              if (f.kind === 'boolean') {
                const on = state[f.key];
                return `<button type="button" class="log-filter-chip log-bool-chip${on ? ' active' : ''}" data-bool="${f.key}" title="${esc(f.hint || '')}">${f.label}</button>`;
              }
              const on = state.activeFields.has(f.key) || (state[f.key] !== undefined && String(state[f.key]).trim() !== '');
              return `<button type="button" class="log-filter-chip log-field-chip${on ? ' active' : ''}" data-field-key="${f.key}" title="${esc(f.hint || '')}">${f.label}</button>`;
            })
            .join('')}
        </div>
        <div class="log-field-inputs" data-group-inputs="${group.title}"></div>`
      ).join('')}

      ${
        tags.length
          ? `
        <h2 class="home-card-title">已选条件</h2>
        <div class="log-active-filters" id="log-active-tags">
          ${tags.map((t) => `<button type="button" class="home-tag log-tag-remove" data-tag="${t.key}">${esc(t.label)} ×</button>`).join('')}
        </div>`
          : ''
      }

      <div class="home-action-row">
        <button type="button" class="btn btn-sm" id="log-ops-search">查询</button>
        <button type="button" class="btn btn-sm btn-ghost" id="log-ops-reset">重置条件</button>
      </div>

      <div id="log-ops-results"></div>
    </section>
  `;

  renderFieldInputs(panel, state);
  bindTimeRangeControls(panel, state, 'ops', () => renderOpsPanel(panel));

  panel.querySelectorAll('.log-sort-field').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.sortField = btn.dataset.field;
      renderOpsPanel(panel);
    });
  });

  panel.querySelectorAll('.log-sort-order').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.sortOrder = btn.dataset.order;
      renderOpsPanel(panel);
    });
  });

  panel.querySelectorAll('.log-service-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      const s = btn.dataset.service;
      if (state.services.includes(s)) {
        state.services = state.services.filter((x) => x !== s);
      } else {
        state.services.push(s);
      }
      renderOpsPanel(panel);
    });
  });

  panel.querySelectorAll('.log-bool-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      const k = btn.dataset.bool;
      state[k] = !state[k];
      renderOpsPanel(panel);
    });
  });

  panel.querySelectorAll('.log-field-chip').forEach((btn) => {
    btn.addEventListener('click', () => {
      const k = btn.dataset.fieldKey;
      if (state.activeFields.has(k)) {
        state.activeFields.delete(k);
        state[k] = '';
      } else {
        state.activeFields.add(k);
      }
      renderOpsPanel(panel);
    });
  });

  panel.querySelectorAll('.log-tag-remove').forEach((btn) => {
    btn.addEventListener('click', () => {
      clearFilterTag(state, btn.dataset.tag);
      renderOpsPanel(panel);
    });
  });

  panel.querySelector('#log-ops-reset')?.addEventListener('click', () => {
    opsState = createDefaultOpsState();
    renderOpsPanel(panel);
  });

  panel.querySelector('#log-ops-search')?.addEventListener('click', () => runOpsSearch(panel));
}

/** @param {HTMLElement} panel @param {ReturnType<typeof createDefaultOpsState>} state */
function renderFieldInputs(panel, state) {
  for (const group of FILTER_GROUPS) {
    const wrap = panel.querySelector(`[data-group-inputs="${group.title}"]`);
    if (!wrap) continue;

    const inputs = group.filters
      .filter((f) => f.kind === 'exact' || f.kind === 'range' || f.kind === 'prefix' || f.kind === 'keyword')
      .filter((f) => state.activeFields.has(f.key))
      .map(
        (f) => `
        <div class="log-inline-field">
          <label class="label">${f.label}${f.hint ? ` <span class="home-muted">(${esc(f.hint)})</span>` : ''}</label>
          <input class="input log-field-input" type="${f.inputType === 'number' ? 'number' : 'text'}"
            data-field-key="${f.key}" value="${escAttr(String(state[f.key] ?? ''))}"
            placeholder="${f.kind === 'prefix' ? '/api/v1/...' : ''}" />
        </div>`
      )
      .join('');

    wrap.innerHTML = inputs;

    wrap.querySelectorAll('.log-field-input').forEach((input) => {
      input.addEventListener('input', () => {
        state[input.dataset.fieldKey] = input.value;
      });
    });
  }
}

/** @param {HTMLElement} panel */
async function runOpsSearch(panel) {
  const state = opsState;
  const body = buildOpsQueryBody(state);
  const resultsEl = panel.querySelector('#log-ops-results');
  resultsEl.innerHTML = '<div class="home-overview-loading">查询中…</div>';

  prefillEndpointCard('log-ops-post', body);

  try {
    const res = await apiRequest('POST', '/api/v1/log/ops/query', { body });
    const data = res.data?.code === 0 ? res.data.data : null;

    if (!data) {
      resultsEl.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '查询失败')}</p>`;
      return;
    }

    const list = data.list || [];
    const total = data.total ?? 0;
    const page = data.page ?? state.page;
    const size = data.size ?? state.size;
    const totalPages = Math.max(1, Math.ceil(total / size));
    state.page = page;

    if (list.length === 0) {
      resultsEl.innerHTML = '<p class="home-muted">无匹配记录，请调整筛选条件。</p>';
      return;
    }

    resultsEl.innerHTML = `
      <h2 class="home-card-title">查询结果</h2>
      <p class="home-muted">时间范围 ${formatTs(data.start_time)} — ${formatTs(data.end_time)} · 共 ${total} 条</p>
      <div class="msg-table-wrap">
        <table class="msg-table log-results-table">
          <thead>
            <tr>
              <th>Trace</th>
              <th>服务</th>
              <th>方法</th>
              <th>URI</th>
              <th>状态</th>
              <th>耗时</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            ${list
              .map(
                (row) => `
              <tr>
                <td class="log-cell-mono">${esc(short(row.trace_id))}</td>
                <td>${esc(row.service_name)}</td>
                <td>${esc(row.method)}</td>
                <td class="log-cell-uri">${esc(row.uri)}</td>
                <td>${esc(row.http_status)}</td>
                <td>${esc(row.cost_ms)}ms</td>
                <td>${esc(formatTs(row.timestamp))}</td>
              </tr>`
              )
              .join('')}
          </tbody>
        </table>
      </div>
      ${renderPagerHtml({ page, size, total: Number(total), totalPages, idPrefix: 'log-ops' })}
    `;

    bindPager(resultsEl, {
      idPrefix: 'log-ops',
      page,
      size,
      totalPages,
      onPageChange: (p) => {
        state.page = p;
        runOpsSearch(panel);
      },
      onSizeChange: (s) => {
        state.size = s;
        state.page = 1;
        runOpsSearch(panel);
      },
    });
  } catch (err) {
    resultsEl.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
  }
}

/** @param {HTMLElement} panel */
function renderAuditPanel(panel) {
  const filters = auditFilterState;

  panel.innerHTML = `
    <section class="home-card glass msg-panel">
      <h2 class="home-card-title">用户审计历史</h2>
      <p class="home-muted">MySQL audit_log — 谁注册/登录/管理操作</p>
      <div class="log-audit-filters glass">
        <div class="log-inline-field">
          <label class="label">操作类型</label>
          <select class="input" id="audit-operation">
            ${AUDIT_OPERATIONS.map(
              (o) => `<option value="${escAttr(o.id)}"${filters.operation === o.id ? ' selected' : ''}>${esc(o.label)}</option>`
            ).join('')}
          </select>
        </div>
        <div class="log-inline-field">
          <label class="label">用户 ID（可选）</label>
          <input class="input" type="number" id="audit-user-id" value="${escAttr(String(filters.userId ?? ''))}" placeholder="留空=当前登录用户" min="1" />
        </div>
      </div>
      <h2 class="home-card-title">时间范围</h2>
      ${renderTimeRangeHtml(filters, 'audit')}
      <div class="home-action-row">
        <button type="button" class="btn btn-sm" id="audit-search-btn">查询</button>
      </div>
      <div id="audit-list-wrap"><div class="home-overview-loading">加载中…</div></div>
    </section>
  `;

  bindTimeRangeControls(panel, filters, 'audit', () => {});

  let page = 1;
  let size = 20;

  panel.querySelector('#audit-operation')?.addEventListener('change', (e) => {
    filters.operation = e.target.value;
  });
  panel.querySelector('#audit-user-id')?.addEventListener('input', (e) => {
    filters.userId = e.target.value;
  });
  panel.querySelector('#audit-search-btn')?.addEventListener('click', () => {
    page = 1;
    fetchAudit();
  });

  fetchAudit();

  async function fetchAudit() {
    const wrap = panel.querySelector('#audit-list-wrap');
    wrap.innerHTML = '<div class="home-overview-loading">加载中…</div>';

    try {
      const query = buildAuditQueryParams(filters, { page, size });
      const res = await apiRequest('GET', '/api/v1/log', { query });
      const data = res.data?.code === 0 ? res.data.data : null;
      if (!data) {
        wrap.innerHTML = '<p class="home-card-err">无法加载审计日志</p>';
        return;
      }

      const list = data.list || [];
      page = Number(data.page ?? page) || 1;
      size = Number(data.size ?? size) || 20;
      const total = Number(data.total ?? 0);
      const totalPages = Math.max(1, Math.ceil(total / size));

      if (list.length === 0 && total === 0) {
        wrap.innerHTML = '<p class="home-muted">暂无审计记录。</p>';
        return;
      }

      wrap.innerHTML = `
        <div class="msg-table-wrap">
          <table class="msg-table">
            <thead>
              <tr><th>ID</th><th>用户</th><th>操作</th><th>成功</th><th>时间</th></tr>
            </thead>
            <tbody>
              ${list
                .map(
                  (r) => `
                <tr>
                  <td>${esc(r.log_id ?? r.logId ?? r.id ?? '—')}</td>
                  <td>${esc(r.userId ?? r.user_id ?? '—')}</td>
                  <td><code>${esc(r.operation ?? '—')}</code></td>
                  <td>${r.success === true || r.success === 1 ? '是' : '否'}</td>
                  <td>${esc(r.createdAt ?? r.created_at ?? '—')}</td>
                </tr>`
                )
                .join('')}
            </tbody>
          </table>
        </div>
        ${renderPagerHtml({ page, size, total, totalPages, idPrefix: 'audit' })}
      `;

      bindPager(wrap, {
        idPrefix: 'audit',
        page,
        size,
        totalPages,
        onPageChange: (p) => {
          page = p;
          fetchAudit();
        },
        onSizeChange: (s) => {
          size = s;
          page = 1;
          fetchAudit();
        },
      });
    } catch {
      wrap.innerHTML = '<p class="home-card-err">加载失败</p>';
    }
  }
}

/** @param {HTMLElement} panel */
function renderMetricsPanel(panel) {
  const state = metricState;
  draw();

  function draw() {
    const metricDef = getMetricDef(state.selectedMetric);
    const isRank = RANK_METRICS.has(state.selectedMetric);
    const isQps = state.selectedMetric === 'qps';
    const canAggregate = AGGREGATE_METRICS.has(state.selectedMetric);

    panel.innerHTML = `
      <section class="home-card glass msg-panel">
        <p class="log-metric-intro home-muted">
          基于 ClickHouse <strong>访问日志</strong>（非审计日志）的聚合统计。7 天/30 天长区间建议使用 <code>aggregate</code> 数据源。
        </p>

        <h2 class="home-card-title">时间范围</h2>
        ${renderTimeRangeHtml(state, 'metric')}

        <h2 class="home-card-title">数据源与筛选</h2>
        <div class="log-metric-filters">
          <div class="log-inline-field">
            <label class="label">数据源 source</label>
            <select class="input" id="metric-source" ${isRank ? 'disabled' : ''}>
              ${METRIC_SOURCES.map(
                (s) => `<option value="${s.id}"${state.source === s.id ? ' selected' : ''}>${esc(s.label)}</option>`
              ).join('')}
            </select>
            ${isRank ? '<p class="home-muted">排行类指标仅支持 raw</p>' : ''}
            ${!isRank && !canAggregate ? '<p class="home-muted">当前指标仅支持 raw</p>' : ''}
          </div>
          <div class="log-inline-field">
            <label class="label">服务 service_name</label>
            <select class="input" id="metric-service">
              <option value="">全部服务</option>
              ${SERVICE_NAMES.map(
                (s) => `<option value="${s}"${state.serviceName === s ? ' selected' : ''}>${s}</option>`
              ).join('')}
            </select>
          </div>
          <div class="log-inline-field">
            <label class="label">URI 前缀 api</label>
            <input class="input" type="text" id="metric-api" value="${escAttr(state.apiPrefix)}" placeholder="/api/v1/..." />
          </div>
          ${
            isQps
              ? `
          <div class="log-inline-field">
            <label class="label">QPS 时间桶 interval（秒）</label>
            <input class="input" type="number" id="metric-interval" value="${state.interval}" min="1" max="3600" />
          </div>`
              : ''
          }
        </div>

        ${METRIC_GROUPS.map(
          (g) => `
          <h2 class="home-card-title">${g.title}</h2>
          <div class="log-chip-row">
            ${g.metrics
              .map(
                (m) => `
              <button type="button" class="log-filter-chip log-metric-chip${state.selectedMetric === m.id ? ' active' : ''}" data-metric="${m.id}" title="${escAttr(m.desc)}">
                ${m.label}
              </button>`
              )
              .join('')}
          </div>`
        ).join('')}

        ${
          metricDef
            ? `
        <div class="log-metric-desc-card glass">
          <strong>${esc(metricDef.label)}</strong>
          <p class="home-muted">${esc(metricDef.desc)}</p>
          ${metricDef.unit ? `<span class="home-tag">${esc(metricDef.unit)}</span>` : ''}
        </div>`
            : ''
        }

        <div class="log-inline-field" id="metric-topn-wrap" ${isRank ? '' : 'hidden'}>
          <label class="label">Top N</label>
          <input class="input" type="number" id="metric-topn" value="${state.topN}" min="1" max="50" />
          <p class="home-muted">排行类指标返回前 N 条结果（1–50）</p>
        </div>

        <p class="log-metric-summary">
          即将查询：
          <strong>${state.timeMode === 'custom' ? '自定义时间' : `过去 ${getTimeRangeLabel(state.timeRange)}`}</strong>
          · source=<strong>${esc(isRank ? 'raw' : state.source)}</strong>
          · <strong>${esc(metricDef?.label ?? state.selectedMetric)}</strong>
          ${state.serviceName ? ` · 服务=${esc(state.serviceName)}` : ''}
          ${state.apiPrefix ? ` · api=${esc(state.apiPrefix)}` : ''}
        </p>

        <div class="home-action-row">
          <button type="button" class="btn btn-sm" id="metric-query-btn">查询指标</button>
        </div>
        <div id="metric-result"></div>
      </section>
    `;

    bindTimeRangeControls(panel, state, 'metric', () => draw());

    panel.querySelector('#metric-source')?.addEventListener('change', (e) => {
      state.source = e.target.value;
    });
    panel.querySelector('#metric-service')?.addEventListener('change', (e) => {
      state.serviceName = e.target.value;
    });
    panel.querySelector('#metric-api')?.addEventListener('input', (e) => {
      state.apiPrefix = e.target.value;
    });
    panel.querySelector('#metric-interval')?.addEventListener('input', (e) => {
      state.interval = Number(e.target.value) || 60;
    });

    panel.querySelectorAll('.log-metric-chip').forEach((btn) => {
      btn.addEventListener('click', () => {
        state.selectedMetric = btn.dataset.metric;
        state.source = suggestMetricSource(state.selectedMetric, state.timeRange, state.source);
        draw();
      });
    });

    panel.querySelector('#metric-topn')?.addEventListener('input', (e) => {
      state.topN = Number(e.target.value) || 10;
    });

    panel.querySelector('#metric-query-btn')?.addEventListener('click', () => runMetricQuery(panel));
  }

  async function runMetricQuery(panel) {
    const resultEl = panel.querySelector('#metric-result');
    resultEl.innerHTML = '<div class="home-overview-loading">查询中…</div>';

    const metricDef = getMetricDef(state.selectedMetric);
    const query = buildMetricQueryParams(state);

    try {
      const res = await apiRequest('GET', '/api/v1/log/metrics', { query });
      const data = res.data?.code === 0 ? res.data.data : null;
      if (!data) {
        resultEl.innerHTML = `<p class="home-card-err">${esc(res.data?.message || '查询失败')}</p>`;
        return;
      }

      const formatted = formatMetricValue(metricDef, data.value);
      let valueHtml;

      if (formatted.isRank && formatted.rows) {
        if (formatted.rows.length === 0) {
          valueHtml = '<p class="home-muted">该时间范围内无排行数据。</p>';
        } else if (state.selectedMetric === 'slowest_api') {
          valueHtml = `
            <div class="msg-table-wrap">
              <table class="msg-table log-metric-rank-table">
                <thead><tr><th>URI</th><th>最大耗时</th></tr></thead>
                <tbody>
                  ${formatted.rows
                    .map(
                      (row) => `
                    <tr>
                      <td class="log-cell-uri">${esc(row.uri ?? '—')}</td>
                      <td>${esc(Math.round(Number(row.max_cost_ms ?? row.maxCostMs ?? 0)))} ms</td>
                    </tr>`
                    )
                    .join('')}
                </tbody>
              </table>
            </div>`;
        } else {
          valueHtml = `
            <div class="msg-table-wrap">
              <table class="msg-table log-metric-rank-table">
                <thead><tr><th>客户端 IP</th><th>次数</th></tr></thead>
                <tbody>
                  ${formatted.rows
                    .map(
                      (row) => `
                    <tr>
                      <td>${esc(row.client_ip ?? row.clientIp ?? '—')}</td>
                      <td>${esc(Number(row.cnt ?? row.count ?? 0).toLocaleString())}</td>
                    </tr>`
                    )
                    .join('')}
                </tbody>
              </table>
            </div>`;
        }
      } else {
        valueHtml = `<p class="log-metric-value">${esc(formatted.display)}</p>`;
      }

      resultEl.innerHTML = `
        <div class="msg-detail-panel glass">
          <h3 class="home-card-title">${esc(metricDef?.label ?? data.metric)}</h3>
          ${valueHtml}
          <p class="home-muted">${formatTs(data.start_time)} — ${formatTs(data.end_time)} · source=${esc(data.source || 'raw')}</p>
        </div>
      `;
    } catch (err) {
      resultEl.innerHTML = `<p class="home-card-err">${esc(err.message || String(err))}</p>`;
    }
  }
}

/**
 * @param {{ page: number, size: number, total: number, totalPages: number, idPrefix: string }} opts
 */
function renderPagerHtml(opts) {
  const { page, size, total, totalPages, idPrefix } = opts;
  return `
    <div class="msg-pager">
      <span class="home-muted">共 ${total.toLocaleString()} 条 · 第 ${page}/${totalPages} 页</span>
      <div class="msg-pager-controls">
        <label class="msg-pager-size">
          <span class="home-muted">每页</span>
          <select class="input input-sm" id="${idPrefix}-size">
            ${[10, 20, 50]
              .map((n) => `<option value="${n}"${size === n ? ' selected' : ''}>${n}</option>`)
              .join('')}
          </select>
        </label>
        <div class="msg-pager-btns">
          <button type="button" class="btn btn-sm btn-ghost" id="${idPrefix}-prev" ${page <= 1 ? 'disabled' : ''}>上一页</button>
          <button type="button" class="btn btn-sm btn-ghost" id="${idPrefix}-next" ${page >= totalPages ? 'disabled' : ''}>下一页</button>
        </div>
        <div class="msg-pager-jump">
          <input class="input input-sm" type="number" id="${idPrefix}-page-input" min="1" max="${totalPages}" value="${page}" />
          <button type="button" class="btn btn-sm btn-ghost" id="${idPrefix}-jump">跳转</button>
        </div>
      </div>
    </div>`;
}

/**
 * @param {HTMLElement} root
 * @param {{ idPrefix: string, page: number, size: number, totalPages: number, onPageChange: (p: number) => void, onSizeChange: (s: number) => void }} opts
 */
function bindPager(root, opts) {
  const { idPrefix, page, totalPages, onPageChange, onSizeChange } = opts;

  root.querySelector(`#${idPrefix}-prev`)?.addEventListener('click', () => {
    if (page > 1) onPageChange(page - 1);
  });
  root.querySelector(`#${idPrefix}-next`)?.addEventListener('click', () => {
    if (page < totalPages) onPageChange(page + 1);
  });

  root.querySelector(`#${idPrefix}-size`)?.addEventListener('change', (e) => {
    onSizeChange(Number(e.target.value) || 20);
  });

  const jump = () => {
    const input = root.querySelector(`#${idPrefix}-page-input`);
    if (!input) return;
    let target = Number(input.value);
    if (Number.isNaN(target)) return;
    target = Math.max(1, Math.min(totalPages, Math.floor(target)));
    if (target !== page) onPageChange(target);
  };

  root.querySelector(`#${idPrefix}-jump`)?.addEventListener('click', jump);
  root.querySelector(`#${idPrefix}-page-input`)?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') jump();
  });
}

/**
 * @param {{ timeMode?: string, timeRange?: string, startTime?: string, endTime?: string }} state
 * @param {string} prefix — ops | audit | metric
 */
function renderTimeRangeHtml(state, prefix) {
  const isCustom = state.timeMode === 'custom';
  return `
    <div class="log-chip-row" id="${prefix}-time-mode">
      ${TIME_MODES.map(
        (m) => `
        <button type="button" class="log-filter-chip${state.timeMode === m.id ? ' active' : ''}" data-mode="${m.id}">
          ${m.label}
        </button>`
      ).join('')}
    </div>
    <div class="log-time-preset" id="${prefix}-time-preset" ${isCustom ? 'hidden' : ''}>
      <div class="log-chip-row">
        ${TIME_RANGES.map(
          (t) => `
          <button type="button" class="log-filter-chip${state.timeRange === t.id ? ' active' : ''}" data-time="${t.id}">
            ${t.label}
          </button>`
        ).join('')}
      </div>
    </div>
    <div class="log-time-custom log-field-inputs" id="${prefix}-time-custom" ${isCustom ? '' : 'hidden'}>
      <div class="log-inline-field">
        <label class="label">开始时间</label>
        <input class="input" type="datetime-local" id="${prefix}-start-time" value="${escAttr(state.startTime || '')}" />
      </div>
      <div class="log-inline-field">
        <label class="label">结束时间</label>
        <input class="input" type="datetime-local" id="${prefix}-end-time" value="${escAttr(state.endTime || '')}" />
      </div>
    </div>`;
}

/**
 * @param {HTMLElement} root
 * @param {{ timeMode?: string, timeRange?: string, startTime?: string, endTime?: string, selectedMetric?: string, source?: string }} state
 * @param {string} prefix
 * @param {() => void} onChange
 */
function bindTimeRangeControls(root, state, prefix, onChange) {
  const toggleCustomVisibility = () => {
    const preset = root.querySelector(`#${prefix}-time-preset`);
    const custom = root.querySelector(`#${prefix}-time-custom`);
    const isCustom = state.timeMode === 'custom';
    if (preset) preset.hidden = isCustom;
    if (custom) custom.hidden = !isCustom;
  };

  root.querySelectorAll(`#${prefix}-time-mode .log-filter-chip`).forEach((btn) => {
    btn.addEventListener('click', () => {
      state.timeMode = btn.dataset.mode;
      root.querySelectorAll(`#${prefix}-time-mode .log-filter-chip`).forEach((b) => {
        b.classList.toggle('active', b.dataset.mode === state.timeMode);
      });
      toggleCustomVisibility();
      onChange();
    });
  });

  root.querySelectorAll(`#${prefix}-time-preset .log-filter-chip`).forEach((btn) => {
    btn.addEventListener('click', () => {
      state.timeRange = btn.dataset.time;
      root.querySelectorAll(`#${prefix}-time-preset .log-filter-chip`).forEach((b) => {
        b.classList.toggle('active', b.dataset.time === state.timeRange);
      });
      if (prefix === 'metric' && state.selectedMetric) {
        state.source = suggestMetricSource(state.selectedMetric, state.timeRange, state.source);
      }
      onChange();
    });
  });

  root.querySelector(`#${prefix}-start-time`)?.addEventListener('input', (e) => {
    state.startTime = e.target.value;
  });
  root.querySelector(`#${prefix}-end-time`)?.addEventListener('input', (e) => {
    state.endTime = e.target.value;
  });
}

function formatTs(ms) {
  if (ms == null || ms === '') return '—';
  const n = Number(ms);
  if (Number.isNaN(n)) return String(ms);
  try {
    return new Date(n).toLocaleString();
  } catch {
    return String(ms);
  }
}

function short(s) {
  const t = String(s ?? '');
  return t.length > 12 ? t.slice(0, 12) + '…' : t;
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
