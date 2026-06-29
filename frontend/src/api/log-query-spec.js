/**
 * Ops log query field metadata — aligned with log-service OpsQueryBuilder + docs/API.md §5.3–5.4
 */

export const SERVICE_NAMES = ['topbiz', 'user', 'message', 'log'];

export const TIME_RANGES = [
  { id: '1h', label: '1 小时' },
  { id: '24h', label: '24 小时' },
  { id: '7d', label: '7 天' },
  { id: '30d', label: '30 天' },
];

export const TIME_MODES = [
  { id: 'preset', label: '快捷范围' },
  { id: 'custom', label: '自定义时间' },
];

export const METRIC_SOURCES = [
  { id: 'raw', label: '原始日志 (raw)' },
  { id: 'aggregate', label: '预聚合 (aggregate)' },
];

/** Metrics supported with source=aggregate (docs/API.md §5.4) */
export const AGGREGATE_METRICS = new Set([
  'pv',
  'qps',
  'api_calls',
  'error_rate',
  'p95',
  'p99',
  'success_rate',
]);

export const RANK_METRICS = new Set(['slowest_api', 'ip_request_topn', 'ip_error_topn']);

export const AUDIT_OPERATIONS = [
  { id: '', label: '全部操作' },
  { id: 'USER_REGISTER', label: 'USER_REGISTER · 注册' },
  { id: 'USER_LOGIN', label: 'USER_LOGIN · 登录' },
  { id: 'USER_LOGOUT', label: 'USER_LOGOUT · 登出' },
  { id: 'USER_DEREGISTER', label: 'USER_DEREGISTER · 注销' },
  { id: 'USER_CHANGE_PASSWORD', label: 'USER_CHANGE_PASSWORD · 改密' },
  { id: 'USER_UPDATE_PROFILE', label: 'USER_UPDATE_PROFILE · 改资料' },
  { id: 'USER_RESET_PASSWORD', label: 'USER_RESET_PASSWORD · 重置密码' },
  { id: 'USER_BIND_CREDENTIAL', label: 'USER_BIND_CREDENTIAL · 绑定凭证' },
  { id: 'USER_PERMISSION_APPLY', label: 'USER_PERMISSION_APPLY · 申请权限' },
  { id: 'ADMIN_USER_CREATE', label: 'ADMIN_USER_CREATE · 创建用户' },
  { id: 'ADMIN_USER_DELETE', label: 'ADMIN_USER_DELETE · 删除用户' },
  { id: 'ADMIN_USER_UPDATE', label: 'ADMIN_USER_UPDATE · 更新用户' },
  { id: 'SESSION_TERMINATED', label: 'SESSION_TERMINATED · 终止会话' },
];

export const SORT_FIELDS = [
  { id: 'timestamp', label: '时间' },
  { id: 'cost_ms', label: '耗时' },
  { id: 'http_status', label: 'HTTP 状态' },
];

export const SORT_ORDERS = [
  { id: 'desc', label: '降序' },
  { id: 'asc', label: '升序' },
];

/** @typedef {'service'|'boolean'|'exact'|'range'|'prefix'|'keyword'} FilterKind */

/**
 * @typedef {Object} FilterDef
 * @property {string} key
 * @property {string} label
 * @property {FilterKind} kind
 * @property {string} [hint]
 * @property {'number'|'text'} [inputType]
 * @property {string} [rangeOp] — gte | lte
 */

/** @type {{ title: string, filters: FilterDef[] }[]} */
export const FILTER_GROUPS = [
  {
    title: '服务',
    filters: SERVICE_NAMES.map((s) => ({
      key: `service:${s}`,
      label: s,
      kind: 'service',
      serviceValue: s,
    })),
  },
  {
    title: '错误与慢请求',
    filters: [
      {
        key: 'has_error',
        label: '仅错误请求',
        kind: 'boolean',
        hint: 'http_status≥400 或 biz_code≠0',
      },
      {
        key: 'slow_only',
        label: '仅慢请求',
        kind: 'boolean',
        hint: '耗时超过阈值（默认 3000ms）',
      },
    ],
  },
  {
    title: 'HTTP 状态码',
    filters: [
      { key: 'http_status', label: '状态码 =', kind: 'exact', inputType: 'number' },
      { key: 'http_status_min', label: '状态码 ≥', kind: 'range', inputType: 'number', rangeOp: 'gte' },
      { key: 'http_status_max', label: '状态码 ≤', kind: 'range', inputType: 'number', rangeOp: 'lte' },
    ],
  },
  {
    title: '性能',
    filters: [
      { key: 'cost_ms_min', label: '耗时 ≥ ms', kind: 'range', inputType: 'number', rangeOp: 'gte' },
      { key: 'cost_ms_max', label: '耗时 ≤ ms', kind: 'range', inputType: 'number', rangeOp: 'lte' },
    ],
  },
  {
    title: '链路与 URI',
    filters: [
      { key: 'trace_id', label: 'Trace ID', kind: 'exact', inputType: 'text' },
      {
        key: 'uri',
        label: 'URI 精确 (=)',
        kind: 'exact',
        inputType: 'text',
        hint: '完整 URI 精确匹配，非模糊',
      },
      {
        key: 'api',
        label: 'URI 前缀 (LIKE x%)',
        kind: 'prefix',
        inputType: 'text',
        hint: '前缀匹配，如 /api/v1/log',
      },
      { key: 'method', label: 'HTTP 方法', kind: 'exact', inputType: 'text' },
    ],
  },
  {
    title: '其他',
    filters: [
      { key: 'level', label: '日志级别', kind: 'exact', inputType: 'text' },
      { key: 'client_ip', label: '客户端 IP', kind: 'exact', inputType: 'text' },
      { key: 'biz_code', label: '业务 code', kind: 'exact', inputType: 'text' },
      { key: 'keyword', label: '关键词（模糊）', kind: 'keyword', inputType: 'text', hint: 'LIKE 匹配 uri / 请求体 / 响应体' },
    ],
  },
];

export const METRIC_GROUPS = [
  {
    title: '请求量',
    metrics: [
      {
        id: 'qps',
        label: 'QPS',
        desc: '所选时间范围内平均每秒请求数',
        unit: '次/秒',
        kind: 'number',
      },
      {
        id: 'pv',
        label: 'PV',
        desc: '所选时间范围内的总请求数（Page View）',
        unit: '次',
        kind: 'integer',
      },
      {
        id: 'api_calls',
        label: 'API 调用数',
        desc: '所选时间范围内的 API 调用总次数',
        unit: '次',
        kind: 'integer',
      },
      {
        id: 'slow_count',
        label: '慢请求数',
        desc: '耗时超过阈值（默认 3000ms）的请求数量',
        unit: '次',
        kind: 'integer',
      },
    ],
  },
  {
    title: '错误',
    metrics: [
      {
        id: 'error_rate',
        label: '错误率',
        desc: 'HTTP≥400 或 biz_code≠0 的请求占比',
        unit: '%',
        kind: 'percent',
      },
      {
        id: 'error_count',
        label: '错误数',
        desc: 'HTTP≥400 或 biz_code≠0 的请求数量',
        unit: '次',
        kind: 'integer',
      },
      {
        id: 'http_5xx',
        label: '5xx 数',
        desc: 'HTTP 状态码 5xx 的请求数量',
        unit: '次',
        kind: 'integer',
      },
      {
        id: 'http_4xx',
        label: '4xx 数',
        desc: 'HTTP 状态码 4xx 的请求数量',
        unit: '次',
        kind: 'integer',
      },
    ],
  },
  {
    title: '性能',
    metrics: [
      {
        id: 'avg',
        label: '平均耗时',
        desc: '所有请求的平均响应时间',
        unit: 'ms',
        kind: 'duration',
      },
      {
        id: 'p95',
        label: 'P95',
        desc: '95% 的请求耗时低于此值',
        unit: 'ms',
        kind: 'duration',
      },
      {
        id: 'p99',
        label: 'P99',
        desc: '99% 的请求耗时低于此值',
        unit: 'ms',
        kind: 'duration',
      },
      {
        id: 'max',
        label: '最大耗时',
        desc: '所选时间范围内最慢单次请求耗时',
        unit: 'ms',
        kind: 'duration',
      },
    ],
  },
  {
    title: '稳定性',
    metrics: [
      {
        id: 'success_rate',
        label: '成功率',
        desc: 'HTTP<400 且 biz_code 正常的请求占比',
        unit: '%',
        kind: 'percent',
      },
    ],
  },
  {
    title: '排行',
    metrics: [
      {
        id: 'slowest_api',
        label: '最慢 API',
        desc: '按 URI 统计的最大耗时排行，用于定位慢接口',
        kind: 'rank_uri',
      },
      {
        id: 'ip_request_topn',
        label: 'IP 请求 TopN',
        desc: '请求次数最多的客户端 IP 排行',
        kind: 'rank_ip',
      },
      {
        id: 'ip_error_topn',
        label: 'IP 错误 TopN',
        desc: '错误请求最多的客户端 IP 排行',
        kind: 'rank_ip',
      },
    ],
  },
];

/** @typedef {'number'|'integer'|'percent'|'duration'|'rank_uri'|'rank_ip'} MetricKind */

/**
 * @typedef {Object} MetricDef
 * @property {string} id
 * @property {string} label
 * @property {string} desc
 * @property {string} [unit]
 * @property {MetricKind} kind
 */

/** @returns {MetricDef|undefined} */
export function getMetricDef(id) {
  for (const g of METRIC_GROUPS) {
    const m = g.metrics.find((x) => x.id === id);
    if (m) return m;
  }
  return undefined;
}

/**
 * @param {MetricDef|undefined} def
 * @param {unknown} value
 * @returns {{ display: string, isRank: boolean, rows: object[]|null }}
 */
export function formatMetricValue(def, value) {
  if (!def) {
    return { display: String(value ?? '—'), isRank: false, rows: null };
  }

  if (def.kind === 'rank_uri' || def.kind === 'rank_ip') {
    const rows = Array.isArray(value) ? value : [];
    return { display: '', isRank: true, rows };
  }

  const n = Number(value);
  if (Number.isNaN(n)) {
    return { display: String(value ?? '—'), isRank: false, rows: null };
  }

  if (def.kind === 'percent') {
    return { display: `${(n * 100).toFixed(2)}%`, isRank: false, rows: null };
  }
  if (def.kind === 'duration') {
    return { display: `${Math.round(n)} ms`, isRank: false, rows: null };
  }
  if (def.kind === 'integer') {
    return { display: `${Math.round(n).toLocaleString()} ${def.unit || ''}`.trim(), isRank: false, rows: null };
  }
  if (def.kind === 'number') {
    const formatted = n >= 100 ? Math.round(n).toLocaleString() : n.toFixed(2);
    return { display: `${formatted} ${def.unit || ''}`.trim(), isRank: false, rows: null };
  }

  return { display: String(value), isRank: false, rows: null };
}

/** @param {string} timeRangeId */
export function getTimeRangeLabel(timeRangeId) {
  return TIME_RANGES.find((t) => t.id === timeRangeId)?.label ?? timeRangeId;
}

/**
 * @param {string} datetimeLocal — value from input[type=datetime-local]
 * @returns {string|undefined} ISO-8601 for backend
 */
export function datetimeLocalToIso(datetimeLocal) {
  if (!datetimeLocal || !String(datetimeLocal).trim()) return undefined;
  const d = new Date(datetimeLocal);
  if (Number.isNaN(d.getTime())) return undefined;
  return d.toISOString();
}

/**
 * @param {{ timeMode?: string, timeRange?: string, startTime?: string, endTime?: string }} state
 * @returns {Record<string, string>}
 */
export function buildTimeQueryParams(state) {
  if (state.timeMode === 'custom') {
    const start = datetimeLocalToIso(state.startTime);
    const end = datetimeLocalToIso(state.endTime);
    const params = {};
    if (start) params.start_time = start;
    if (end) params.end_time = end;
    return params;
  }
  return { time_range: state.timeRange || '24h' };
}

/**
 * @param {ReturnType<typeof createDefaultMetricState>} state
 */
export function buildMetricQueryParams(state) {
  const query = {
    metric: state.selectedMetric,
    ...buildTimeQueryParams(state),
  };

  if (RANK_METRICS.has(state.selectedMetric)) {
    query.source = 'raw';
    query.top_n = state.topN || 10;
  } else {
    query.source = state.source || 'raw';
    if (state.selectedMetric === 'qps' && state.interval) {
      query.interval = state.interval;
    }
  }

  if (state.serviceName && String(state.serviceName).trim()) {
    query.service_name = String(state.serviceName).trim();
  }
  if (state.apiPrefix && String(state.apiPrefix).trim()) {
    query.api = String(state.apiPrefix).trim();
  }

  return query;
}

/**
 * @param {ReturnType<typeof createDefaultAuditFilterState>} state
 * @param {{ page: number, size: number }} paging
 */
export function buildAuditQueryParams(state, paging) {
  const query = { page: paging.page, size: paging.size, ...buildTimeQueryParams(state) };
  if (state.operation) query.operation = state.operation;
  if (state.userId && String(state.userId).trim()) {
    query.userId = Number(state.userId);
  }
  return query;
}

/** @returns {import('./log-query-spec.js').MetricPanelState} */
export function createDefaultMetricState() {
  return {
    timeMode: 'preset',
    timeRange: '24h',
    startTime: '',
    endTime: '',
    source: 'raw',
    serviceName: '',
    apiPrefix: '',
    interval: 60,
    selectedMetric: 'qps',
    topN: 10,
  };
}

/** @returns {import('./log-query-spec.js').AuditFilterState} */
export function createDefaultAuditFilterState() {
  return {
    timeMode: 'preset',
    timeRange: '24h',
    startTime: '',
    endTime: '',
    operation: '',
    userId: '',
  };
}

/**
 * Pick recommended source when time range is long.
 * @param {string} metricId
 * @param {string} timeRangeId
 * @param {string} currentSource
 */
export function suggestMetricSource(metricId, timeRangeId, currentSource) {
  if (RANK_METRICS.has(metricId)) return 'raw';
  if ((timeRangeId === '7d' || timeRangeId === '30d') && AGGREGATE_METRICS.has(metricId)) {
    return 'aggregate';
  }
  return currentSource || 'raw';
}

/** @returns {import('./log-query-spec.js').OpsQueryState} */
export function createDefaultOpsState() {
  return {
    timeMode: 'preset',
    timeRange: '24h',
    startTime: '',
    endTime: '',
    sortField: 'timestamp',
    sortOrder: 'desc',
    page: 1,
    size: 20,
    services: [],
    has_error: false,
    slow_only: false,
    trace_id: '',
    uri: '',
    api: '',
    method: '',
    level: '',
    client_ip: '',
    biz_code: '',
    http_status: '',
    http_status_min: '',
    http_status_max: '',
    cost_ms_min: '',
    cost_ms_max: '',
    keyword: '',
    /** @type {Set<string>} fields with input panel expanded */
    activeFields: new Set(),
  };
}

/**
 * @param {ReturnType<typeof createDefaultOpsState>} state
 */
export function buildOpsQueryBody(state) {
  const filters = {};

  if (state.services.length > 0) {
    filters.service_names = [...state.services];
  }
  if (state.has_error) filters.has_error = true;
  if (state.slow_only) filters.slow_only = true;

  for (const key of [
    'trace_id',
    'uri',
    'method',
    'level',
    'client_ip',
    'biz_code',
    'http_status',
    'api',
    'keyword',
  ]) {
    const val = state[key];
    if (val !== undefined && val !== null && String(val).trim() !== '') {
      filters[key] = key === 'http_status' ? Number(val) : String(val).trim();
    }
  }

  for (const key of ['http_status_min', 'http_status_max', 'cost_ms_min', 'cost_ms_max']) {
    const val = state[key];
    if (val !== undefined && val !== null && String(val).trim() !== '') {
      filters[key] = Number(val);
    }
  }

  const body = {
    ...buildTimeQueryParams(state),
    filters,
    sort: { field: state.sortField, order: state.sortOrder },
    page: state.page,
    size: state.size,
  };

  return body;
}

/**
 * Human-readable summary tags for active filters.
 * @param {ReturnType<typeof createDefaultOpsState>} state
 */
export function summarizeActiveFilters(state) {
  const tags = [];

  if (state.timeMode === 'custom' && state.startTime && state.endTime) {
    tags.push({ key: 'time:custom', label: `时间: ${state.startTime} ~ ${state.endTime}` });
  } else if (state.timeRange) {
    tags.push({ key: 'time:preset', label: `时间: ${getTimeRangeLabel(state.timeRange)}` });
  }

  for (const s of state.services) {
    tags.push({ key: `service:${s}`, label: `服务: ${s}` });
  }
  if (state.has_error) tags.push({ key: 'has_error', label: '仅错误' });
  if (state.slow_only) tags.push({ key: 'slow_only', label: '仅慢请求' });

  const exactLabels = {
    trace_id: 'Trace',
    uri: 'URI',
    api: 'URI 前缀',
    method: 'Method',
    level: 'Level',
    client_ip: 'IP',
    biz_code: 'Biz',
    http_status: 'Status',
    keyword: '关键词',
  };
  for (const [k, lbl] of Object.entries(exactLabels)) {
    if (state[k] && String(state[k]).trim()) {
      tags.push({ key: k, label: `${lbl}: ${state[k]}` });
    }
  }

  if (state.http_status_min !== '') tags.push({ key: 'http_status_min', label: `状态 ≥ ${state.http_status_min}` });
  if (state.http_status_max !== '') tags.push({ key: 'http_status_max', label: `状态 ≤ ${state.http_status_max}` });
  if (state.cost_ms_min !== '') tags.push({ key: 'cost_ms_min', label: `耗时 ≥ ${state.cost_ms_min}ms` });
  if (state.cost_ms_max !== '') tags.push({ key: 'cost_ms_max', label: `耗时 ≤ ${state.cost_ms_max}ms` });

  return tags;
}

/**
 * @param {ReturnType<typeof createDefaultOpsState>} state
 * @param {string} tagKey
 */
export function clearFilterTag(state, tagKey) {
  if (tagKey.startsWith('service:')) {
    const s = tagKey.slice(8);
    state.services = state.services.filter((x) => x !== s);
    return;
  }
  if (tagKey === 'has_error') state.has_error = false;
  else if (tagKey === 'slow_only') state.slow_only = false;
  else if (tagKey === 'time:custom') {
    state.timeMode = 'preset';
    state.startTime = '';
    state.endTime = '';
  } else if (tagKey === 'time:preset') {
    state.timeRange = '24h';
  } else if (Object.prototype.hasOwnProperty.call(state, tagKey)) {
    state[tagKey] = '';
    state.activeFields.delete(tagKey);
  }
}
