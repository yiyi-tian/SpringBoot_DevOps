/**
 * Full API catalog ? all requests go through TopBiz :8080 (BFF).
 * backendPort is the external entry port (always 8080 via TopBiz).
 */

export const DOMAINS = [
  { id: 'account', label: '账号', port: '8080', icon: '◎' },
  { id: 'auth', label: '主页', port: '8080', icon: '◉' },
  { id: 'message', label: '消息', port: '8080', icon: '✉' },
  { id: 'log', label: '日志', port: '8080', icon: '▤', admin: true },
  { id: 'admin', label: '管理', port: '8080', icon: '⚙', admin: true },
];

/** @typedef {'done'|'partial'|'501'|'planned'} ApiStatus */
/** @typedef {'anon'|'authc'|'admin'} AuthLevel */

/**
 * @typedef {Object} Endpoint
 * @property {string} id
 * @property {string} domain
 * @property {string} [group]
 * @property {string} method
 * @property {string} path
 * @property {string} title
 * @property {string} [description]
 * @property {string} backendPort
 * @property {ApiStatus} status
 * @property {AuthLevel} auth
 * @property {Object} [bodyExample]
 * @property {Object} [queryExample]
 * @property {string[]} [pathParams]
 */

/** @type {Endpoint[]} */
export const ENDPOINTS = [
  // ? Auth ?
  {
    id: 'auth-profile-get',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/profile',
    title: '我的信息',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
  },
  {
    id: 'auth-deregister',
    domain: 'auth',
    method: 'POST',
    path: '/api/v1/deregister',
    title: '注销账号',
    backendPort: '8080',
    status: 'planned',
    auth: 'authc',
    bodyExample: {},
  },
  {
    id: 'auth-permissions',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/permissions',
    title: '查看当前权限',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
  },
  {
    id: 'auth-perm-catalog',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/permissions/catalog',
    title: '权限目录',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    queryExample: {
      page: 1,
      size: 10,
      keyword: ''
    },
  },
  {
    id: 'auth-perm-applications',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/permissions/applications',
    title: '我的权限申请',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
  },
  {
    id: 'auth-groups',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/groups',
    title: '查看当前分组',
    backendPort: '8080',
    status: 'planned',
    auth: 'authc',
  },
  {
    id: 'auth-password',
    domain: 'auth',
    method: 'PUT',
    path: '/api/v1/password',
    title: '修改密码',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      oldPassword: 'demo123456',
      newPassword: 'newpass123456'
    },
  },
  {
    id: 'auth-profile',
    domain: 'auth',
    method: 'PATCH',
    path: '/api/v1/profile',
    title: '修改资料',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      displayName: 'Demo User',
      sex: 1
    },
  },
  {
    id: 'auth-bind',
    domain: 'auth',
    method: 'POST',
    path: '/api/v1/account/bind',
    title: '绑定账号',
    backendPort: '8080',
    status: 'planned',
    auth: 'authc',
    bodyExample: {
      credential: 'newemail@example.com',
      password: 'demo123456'
    },
  },
  {
    id: 'auth-perm-apply',
    domain: 'auth',
    method: 'POST',
    path: '/api/v1/permissions/apply',
    title: '申请权限',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      permId: 1
    },
  },
  {
    id: 'auth-sessions',
    domain: 'auth',
    method: 'GET',
    path: '/api/v1/sessions',
    title: '活跃会话列表',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
  },
  {
    id: 'auth-device-delete',
    domain: 'auth',
    method: 'DELETE',
    path: '/api/v1/devices/{deviceId}',
    title: '踢出指定设备',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    pathParams: ['deviceId'],
  },
  {
    id: 'auth-session-delete',
    domain: 'auth',
    method: 'DELETE',
    path: '/api/v1/sessions/{sessionId}',
    title: '结束指定会话',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {},
    pathParams: ['sessionId'],
  },
  {
    id: 'auth-sessions-others',
    domain: 'auth',
    method: 'DELETE',
    path: '/api/v1/sessions',
    title: '登出其他会话',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    queryExample: {
      scope: 'others'
    },
  },

  // ? Message ?
  {
    id: 'msg-send-instant',
    domain: 'message',
    method: 'POST',
    path: '/api/v1/send/instant',
    title: '即时发送',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      templateId: 1,
      channel: 'EMAIL',
      testTo: 'recipient@example.com',
      variables: {
        username: 'Demo'
      }
    },
  },
  {
    id: 'msg-send-scheduled',
    domain: 'message',
    method: 'POST',
    path: '/api/v1/send/scheduled',
    title: '定时发送',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
    bodyExample: {
      templateId: 1,
      channel: 'EMAIL',
      scheduledAt: '2026-07-01T10:00:00'
    },
  },
  {
    id: 'msg-records',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/sending-records',
    title: '发送记录',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    queryExample: {
      page: 1,
      size: 10
    },
  },
  {
    id: 'msg-record-delete',
    domain: 'message',
    method: 'DELETE',
    path: '/api/v1/sending-records/{id}',
    title: '删除发送记录',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    pathParams: ['id'],
  },
  {
    id: 'msg-template-create',
    domain: 'message',
    method: 'POST',
    path: '/api/v1/templates',
    title: '创建模板',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      name: 'demo_template',
      channel: 'EMAIL',
      subject: 'Hello {{username}}',
      content: 'Welcome, {{username}}!'
    },
  },
  {
    id: 'msg-template-list',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/templates',
    title: '模板列表',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    queryExample: {
      page: 1,
      size: 10
    },
  },
  {
    id: 'msg-var-schema',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/variables/schema',
    title: '变量 Schema',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
  },
  {
    id: 'msg-var-create',
    domain: 'message',
    method: 'POST',
    path: '/api/v1/variables',
    title: '创建变量',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
    bodyExample: {
      name: 'username',
      type: 'STRING'
    },
  },
  {
    id: 'msg-var-get',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/variables/{variableId}',
    title: '查询变量',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
    pathParams: ['variableId'],
  },
  {
    id: 'msg-carrier-list',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/msg/carriers',
    title: '载体列表',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    queryExample: {
      channelType: 'EMAIL'
    },
  },
  {
    id: 'msg-carrier-get',
    domain: 'message',
    method: 'GET',
    path: '/api/v1/msg/carriers/{id}',
    title: '载体详情',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    pathParams: ['id'],
  },
  {
    id: 'msg-carrier-test',
    domain: 'message',
    method: 'POST',
    path: '/api/v1/msg/carriers/{id}/test',
    title: '载体连通测试',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      testTo: 'test@example.com'
    },
    pathParams: ['id'],
  },

  // ? Log ?
  {
    id: 'log-audit',
    domain: 'log',
    method: 'GET',
    path: '/api/v1/log',
    title: '审计日志列表',
    description: 'MySQL audit_log',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    queryExample: {
      page: 1,
      size: 20
    },
  },
  {
    id: 'log-ops-get',
    domain: 'log',
    method: 'GET',
    path: '/api/v1/log/ops/query',
    title: '运维日志查询 (GET)',
    description: 'ClickHouse 访问日志',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    queryExample: {
      service_name: 'topbiz',
      time_range: '24h',
      page: 1,
      size: 20
    },
  },
  {
    id: 'log-ops-post',
    domain: 'log',
    method: 'POST',
    path: '/api/v1/log/ops/query',
    title: '运维日志复杂查询 (POST)',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    bodyExample: {
      time_range: '24h',
      filters: {
        service_names: ['topbiz'],
        has_error: true,
        http_status_min: 400
      },
      sort: { field: 'cost_ms', order: 'desc' },
      page: 1,
      size: 20
    },
  },
  {
    id: 'log-metrics',
    domain: 'log',
    method: 'GET',
    path: '/api/v1/log/metrics',
    title: '日志指标',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    queryExample: {
      metric: 'qps',
      time_range: '1h',
      source: 'raw'
    },
  },
  {
    id: 'log-export',
    domain: 'log',
    method: 'POST',
    path: '/api/v1/log/ops/export',
    title: '导出日志',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    bodyExample: {
      format: 'json',
      filters: []
    },
  },
  {
    id: 'log-metrics-config-get',
    domain: 'log',
    method: 'GET',
    path: '/api/v1/log/metrics/config',
    title: '读取指标阈值',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
  },
  {
    id: 'log-metrics-config-put',
    domain: 'log',
    method: 'PUT',
    path: '/api/v1/log/metrics/config',
    title: '更新指标阈值',
    backendPort: '8080',
    status: 'done',
    auth: 'admin',
    bodyExample: {
      errorRateThreshold: 0.05,
      p99ThresholdMs: 500
    },
  },

  // ? Admin: message-resource ?
  {
    id: 'msg-template-status',
    domain: 'admin',
    group: 'message-resource',
    method: 'PUT',
    path: '/api/v1/templates/{id}/status',
    title: '更新模板状态',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      status: 'ACTIVE'
    },
    pathParams: ['id'],
  },
  {
    id: 'msg-var-update',
    domain: 'admin',
    group: 'message-resource',
    method: 'PUT',
    path: '/api/v1/variables/{variableId}',
    title: '修改变量',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
    bodyExample: {
      name: 'username'
    },
    pathParams: ['variableId'],
  },
  {
    id: 'msg-var-delete',
    domain: 'admin',
    group: 'message-resource',
    method: 'DELETE',
    path: '/api/v1/variables/{variableId}',
    title: '删除变量',
    backendPort: '8080',
    status: '501',
    auth: 'authc',
    pathParams: ['variableId'],
  },
  {
    id: 'msg-carrier-create',
    domain: 'admin',
    group: 'message-resource',
    method: 'POST',
    path: '/api/v1/msg/carriers',
    title: '新增载体',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      channelType: 'EMAIL',
      name: 'QQ SMTP',
      config: {
        host: 'smtp.qq.com',
        port: 465
      }
    },
  },
  {
    id: 'msg-carrier-update',
    domain: 'admin',
    group: 'message-resource',
    method: 'PUT',
    path: '/api/v1/msg/carriers/{id}',
    title: '修改载体',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    bodyExample: {
      name: 'Updated Carrier'
    },
    pathParams: ['id'],
  },
  {
    id: 'msg-carrier-delete',
    domain: 'admin',
    group: 'message-resource',
    method: 'DELETE',
    path: '/api/v1/msg/carriers/{id}',
    title: '删除载体',
    backendPort: '8080',
    status: 'done',
    auth: 'authc',
    pathParams: ['id'],
  },

  // ? Admin: RBAC ?
  {
    id: 'admin-user-create',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/users',
    title: '新增用户',
    backendPort: '8080',
    status: 'partial',
    auth: 'admin',
    bodyExample: {
      credentialType: 'USERNAME',
      credential: 'new_user',
      password: 'pass123456'
    },
  },
  {
    id: 'admin-user-delete',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/users',
    title: '删除用户',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      userId: 2
    },
  },
  {
    id: 'admin-user-update',
    domain: 'admin',
    method: 'PATCH',
    path: '/api/v1/admin/users',
    title: '修改用户',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      userId: 2,
      nickname: 'Updated'
    },
  },
  {
    id: 'admin-user-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/users',
    title: '查询用户',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      keyword: '',
      page: 1,
      size: 10
    },
  },
  {
    id: 'admin-group-create',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/groups',
    title: '创建分组',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      name: 'developers',
      description: 'Dev team'
    },
  },
  {
    id: 'admin-group-delete',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/groups',
    title: '删除分组',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      groupId: 1
    },
  },
  {
    id: 'admin-group-update',
    domain: 'admin',
    method: 'PATCH',
    path: '/api/v1/admin/groups',
    title: '修改分组',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      groupId: 1,
      name: 'updated_group'
    },
  },
  {
    id: 'admin-group-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/groups',
    title: '查询分组',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      page: 1,
      size: 10
    },
  },
  {
    id: 'admin-group-user-add',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/group-users',
    title: '添加用户到组',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      userId: 2,
      groupId: 1
    },
  },
  {
    id: 'admin-group-user-remove',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/group-users',
    title: '从组移除用户',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      userId: 2,
      groupId: 1
    },
  },
  {
    id: 'admin-group-user-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/group-users',
    title: '查询组成员',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      groupId: 1
    },
  },
  {
    id: 'admin-perm-create',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/permissions',
    title: '创建权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      code: 'READ_LOG',
      name: 'Read logs'
    },
  },
  {
    id: 'admin-perm-delete',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/permissions',
    title: '删除权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      permId: 1
    },
  },
  {
    id: 'admin-perm-update',
    domain: 'admin',
    method: 'PATCH',
    path: '/api/v1/admin/permissions',
    title: '修改权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      permId: 1,
      name: 'Updated perm'
    },
  },
  {
    id: 'admin-perm-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/permissions',
    title: '查询权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      page: 1,
      size: 10
    },
  },
  {
    id: 'admin-gp-create',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/group-permissions',
    title: '创建分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      groupId: 1,
      permId: 1
    },
  },
  {
    id: 'admin-gp-delete',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/group-permissions',
    title: '删除分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      id: 1
    },
  },
  {
    id: 'admin-gp-update',
    domain: 'admin',
    method: 'PATCH',
    path: '/api/v1/admin/group-permissions',
    title: '修改分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      id: 1
    },
  },
  {
    id: 'admin-gp-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/group-permissions',
    title: '查询分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      groupId: 1
    },
  },
  {
    id: 'admin-up-create',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/user-permissions',
    title: '创建用户权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      userId: 2,
      permId: 1
    },
  },
  {
    id: 'admin-up-delete',
    domain: 'admin',
    method: 'DELETE',
    path: '/api/v1/admin/user-permissions',
    title: '删除用户权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      id: 1
    },
  },
  {
    id: 'admin-up-update',
    domain: 'admin',
    method: 'PATCH',
    path: '/api/v1/admin/user-permissions',
    title: '修改用户权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      id: 1
    },
  },
  {
    id: 'admin-up-search',
    domain: 'admin',
    method: 'GET',
    path: '/api/v1/admin/user-permissions',
    title: '查询用户权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    queryExample: {
      userId: 2
    },
  },
  {
    id: 'admin-up-approve',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/user-permissions/{id}/approve',
    title: '审批用户权限申请',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {},
    pathParams: ['id'],
  },
  {
    id: 'admin-up-reject',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/user-permissions/{id}/reject',
    title: '拒绝用户权限申请',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {},
    pathParams: ['id'],
  },
  {
    id: 'admin-gp-apply',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/groups/{groupId}/permissions/apply',
    title: '分组申请权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {
      permId: 1
    },
    pathParams: ['groupId'],
  },
  {
    id: 'admin-gp-approve',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/group-permissions/{id}/approve',
    title: '审批分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {},
    pathParams: ['id'],
  },
  {
    id: 'admin-gp-reject',
    domain: 'admin',
    method: 'POST',
    path: '/api/v1/admin/group-permissions/{id}/reject',
    title: '拒绝分组权限',
    backendPort: '8080',
    status: 'planned',
    auth: 'admin',
    bodyExample: {},
    pathParams: ['id'],
  },
];

export function getEndpointsByDomain(domainId) {
  return ENDPOINTS.filter((e) => e.domain === domainId);
}

export function getEndpointsByGroup(domainId, group) {
  return ENDPOINTS.filter((e) => e.domain === domainId && e.group === group);
}

export function resolvePath(path, pathValues = {}) {
  return path.replace(/\{(\w+)\}/g, (_, key) => {
    const val = pathValues[key];
    if (val === undefined || val === '') return `{${key}}`;
    return encodeURIComponent(String(val));
  });
}

export function buildQueryString(params = {}) {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null && String(v).trim() !== ''
  );
  if (entries.length === 0) return '';
  return '?' + new URLSearchParams(entries.map(([k, v]) => [k, String(v)])).toString();
}
