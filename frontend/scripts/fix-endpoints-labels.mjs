/**
 * Restore Chinese labels in endpoints.js (UTF-8 safe).
 * Run: node scripts/fix-endpoints-labels.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const file = path.join(__dirname, '../src/api/endpoints.js');
let s = fs.readFileSync(file, 'utf8');

const domainBlock = `export const DOMAINS = [
  { id: 'account', label: '账号', port: '8080', icon: '◎' },
  { id: 'auth', label: '主页', port: '8080', icon: '◉' },
  { id: 'message', label: '消息', port: '8080', icon: '✉' },
  { id: 'log', label: '日志', port: '8080', icon: '▤', admin: true },
  { id: 'admin', label: '管理', port: '8080', icon: '⚙', admin: true },
];`;

s = s.replace(/export const DOMAINS = \[[\s\S]*?\];/, domainBlock);

s = s.replace(
  /\/\*\*\n \* Full API catalog[\s\S]*?\n \*\/\n\n/,
  `/**\n * Full API catalog — all requests go through TopBiz :8080 (BFF).\n * backendPort is the external entry port (always 8080 via TopBiz).\n */\n\n`
);

const comments = [
  ['// ?? Auth ??', '// — Auth —'],
  ['// ?? Message ??', '// — Message —'],
  ['// ?? Log ??', '// — Log —'],
  ['// ?? Admin: message-resource ??', '// — Admin: message-resource —'],
  ['// ?? Admin: RBAC ??', '// — Admin: RBAC —'],
];
for (const [from, to] of comments) {
  s = s.replace(from, to);
}

/** @type {Record<string, string>} */
const titles = {
  'auth-profile-get': '我的信息',
  'auth-deregister': '注销账号',
  'auth-permissions': '查看当前权限',
  'auth-groups': '查看当前分组',
  'auth-password': '修改密码',
  'auth-profile': '修改资料',
  'auth-bind': '绑定账号',
  'auth-perm-apply': '申请权限',
  'auth-perm-catalog': '权限目录',
  'auth-perm-applications': '我的权限申请',
  'auth-sessions': '活跃会话列表',
  'auth-device-delete': '踢出指定设备',
  'auth-session-delete': '结束指定会话',
  'auth-sessions-others': '登出其他会话',
  'msg-send-instant': '即时发送',
  'msg-send-scheduled': '定时发送',
  'msg-records': '发送记录',
  'msg-record-delete': '删除发送记录',
  'msg-template-create': '创建模板',
  'msg-template-list': '模板列表',
  'msg-var-schema': '变量 Schema',
  'msg-var-create': '创建变量',
  'msg-var-get': '查询变量',
  'msg-carrier-list': '载体列表',
  'msg-carrier-get': '载体详情',
  'msg-carrier-test': '载体连通测试',
  'log-audit': '审计日志列表',
  'log-ops-get': '运维日志查询 (GET)',
  'log-ops-post': '运维日志复杂查询 (POST)',
  'log-metrics': '日志指标',
  'log-export': '导出日志',
  'log-metrics-config-get': '读取指标阈值',
  'log-metrics-config-put': '更新指标阈值',
  'msg-template-status': '更新模板状态',
  'msg-var-update': '修改变量',
  'msg-var-delete': '删除变量',
  'msg-carrier-create': '新增载体',
  'msg-carrier-update': '修改载体',
  'msg-carrier-delete': '删除载体',
  'admin-user-create': '新增用户',
  'admin-user-delete': '删除用户',
  'admin-user-update': '修改用户',
  'admin-user-search': '查询用户',
  'admin-group-create': '创建分组',
  'admin-group-delete': '删除分组',
  'admin-group-update': '修改分组',
  'admin-group-search': '查询分组',
  'admin-group-user-add': '添加用户到组',
  'admin-group-user-remove': '从组移除用户',
  'admin-group-user-search': '查询组成员',
  'admin-perm-create': '创建权限',
  'admin-perm-delete': '删除权限',
  'admin-perm-update': '修改权限',
  'admin-perm-search': '查询权限',
  'admin-gp-create': '创建分组权限',
  'admin-gp-delete': '删除分组权限',
  'admin-gp-update': '修改分组权限',
  'admin-gp-search': '查询分组权限',
  'admin-up-create': '创建用户权限',
  'admin-up-delete': '删除用户权限',
  'admin-up-update': '修改用户权限',
  'admin-up-search': '查询用户权限',
  'admin-up-approve': '审批用户权限申请',
  'admin-up-reject': '拒绝用户权限申请',
  'admin-gp-apply': '分组申请权限',
  'admin-gp-approve': '审批分组权限',
  'admin-gp-reject': '拒绝分组权限',
};

for (const [id, title] of Object.entries(titles)) {
  const re = new RegExp(
    `(id: '${id}',[\\s\\S]*?title: )'[^']*'`,
    'm'
  );
  s = s.replace(re, `$1'${title}'`);
}

s = s.replace(
  /(id: 'log-audit',[\s\S]*?description: )'[^']*'/,
  "$1'MySQL audit_log'"
);
s = s.replace(
  /(id: 'log-ops-get',[\s\S]*?description: )'[^']*'/,
  "$1'ClickHouse 访问日志'"
);

fs.writeFileSync(file, s, 'utf8');

const out = fs.readFileSync(file, 'utf8');
const corrupt = /label: '\?\?'/.test(out);
const ok = out.includes('账号') && out.includes('运维日志复杂查询');
console.log(JSON.stringify({ corrupt, ok, bytes: Buffer.byteLength(out) }));
