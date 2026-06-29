import './styles/glass.css';
import './styles/layout.css';
import './styles/auth-portal.css';

import { DOMAINS, getEndpointsByDomain } from './api/endpoints.js';
import { refreshSession } from './api/client.js';
import {
  initShell,
  getMainEl,
  getApiCardsMountEl,
  renderNav,
  setResponsePanelVisible,
  setSplitLayout,
} from './ui/shell.js';
import { renderEndpointList } from './ui/forms.js';
import { renderAuthPortal } from './ui/auth-portal.js';
import { renderHomeOverview } from './ui/home-overview.js';
import { renderMessageOverview } from './ui/message-overview.js';
import { renderLogOverview } from './ui/log-overview.js';
import { renderAdminOverview } from './ui/admin-overview.js';

const DOMAIN_META = {
  auth: {
    title: '主页',
    desc: '会话、权限与账号管理 — 经 TopBiz :8080',
  },
  message: {
    title: '消息域',
    desc: '中间浏览模板/变量/载体，右侧调试发送与查询 API — 经 TopBiz :8080',
  },
  log: {
    title: '日志域',
    desc: '中间可视化构建运维日志查询，右侧调试 API — 经 TopBiz :8080，需 admin',
    admin: true,
  },
  admin: {
    title: '管理域',
    desc: '消息资源管理 + 用户/组/权限 RBAC — 经 TopBiz :8080',
    admin: true,
  },
};

const SPLIT_LAYOUT_DOMAINS = new Set(['auth', 'message', 'log', 'admin']);

const OVERVIEW_RENDERERS = {
  auth: (el) => renderHomeOverview(el),
  message: (el) => renderMessageOverview(el),
  log: (el) => renderLogOverview(el),
  admin: (el) => renderAdminOverview(el),
};

const OVERVIEW_MOUNT_IDS = {
  auth: 'home-overview',
  message: 'message-overview',
  log: 'log-overview',
  admin: 'admin-overview',
};

function renderPanelHeader(meta, endpointCount) {
  return `
    <div class="panel-header">
      <div class="panel-header-text">
        <h1 class="panel-title">${meta.title}</h1>
        <p class="panel-desc">${meta.desc}</p>
      </div>
      <span class="panel-badge">${endpointCount} 个接口</span>
    </div>
  `;
}

function renderPanel(domainId) {
  const main = getMainEl();
  if (!main) return;

  renderNav(DOMAINS, domainId, renderPanel);
  setResponsePanelVisible(domainId !== 'account');

  if (domainId === 'account') {
    setSplitLayout(false);
    renderAuthPortal(main);
    return;
  }

  const meta = DOMAIN_META[domainId];
  const endpoints = getEndpointsByDomain(domainId);

  if (SPLIT_LAYOUT_DOMAINS.has(domainId)) {
    setSplitLayout(true);
    const mountId = OVERVIEW_MOUNT_IDS[domainId];
    main.innerHTML = `${renderPanelHeader(meta, endpoints.length)}<div id="${mountId}"></div>`;
    OVERVIEW_RENDERERS[domainId]?.(main.querySelector(`#${mountId}`));

    const cardsMount = getApiCardsMountEl();
    if (cardsMount) {
      renderEndpointList(cardsMount, endpoints);
    }
    return;
  }

  setSplitLayout(false);
  main.innerHTML = `${renderPanelHeader(meta, endpoints.length)}<div id="endpoint-container"></div>`;

  const container = main.querySelector('#endpoint-container');

  if (meta.admin) {
    const banner = document.createElement('div');
    banner.className = 'admin-banner';
    banner.innerHTML =
      '<strong>需要 admin 权限</strong> — 请使用首个注册用户 (userId=1) 或 identifier 为 admin 的账号登录。403 表示当前会话无 admin 角色。';
    container.before(banner);
  }

  renderEndpointList(container, endpoints);
}

function init() {
  const root = document.getElementById('app');
  initShell(root);
  renderPanel('account');
  refreshSession();
}

init();
