import { apiRequest, refreshSession, clearSession } from '../api/client.js';
import { getOrCreateDeviceId } from '../api/device-id.js';

const EMAIL_RE =
  /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;

const COUNTDOWN_SEC = 60;

function withDeviceId(body) {
  return { ...body, deviceId: getOrCreateDeviceId() };
}

function passwordFieldHtml(id, label, autocomplete = 'new-password') {
  return `
    <div class="field">
      <label class="label" for="${id}">${label}</label>
      <div class="auth-pwd-row">
        <input class="input" id="${id}" type="password" autocomplete="${autocomplete}" minlength="6" placeholder="至少 6 位" />
        <button type="button" class="auth-pwd-toggle" data-target="${id}" aria-label="显示或隐藏密码">👁</button>
      </div>
    </div>
  `;
}

/**
 * @param {HTMLElement} container
 */
export function renderAuthPortal(container) {
  container.innerHTML = `
    <div class="auth-portal-wrap">
      <div class="glass glass-glow auth-portal">
        <h1 class="auth-portal-title" id="auth-portal-title">欢迎</h1>
        <p class="auth-portal-sub" id="auth-portal-sub">邮箱登录或注册 DevOps 平台</p>

        <p class="auth-back-login-wrap auth-hidden" id="auth-back-login-wrap">
          <button type="button" class="auth-back-login" id="auth-back-login">← 返回登录</button>
        </p>

        <div class="auth-tabs" id="auth-tabs" role="tablist">
          <button type="button" class="auth-tab active" data-intent="login">登录</button>
          <button type="button" class="auth-tab" data-intent="register">注册</button>
        </div>

        <div class="auth-mode-switch" id="auth-mode-switch">
          <button type="button" class="auth-mode-btn active" data-mode="password">邮箱 + 密码</button>
          <button type="button" class="auth-mode-btn" data-mode="code">邮箱 + 验证码</button>
        </div>

        <div class="auth-steps auth-hidden" id="auth-steps" aria-hidden="true">
          <span class="auth-step-dot active" data-step="email">1</span>
          <span class="auth-step-line"></span>
          <span class="auth-step-dot" data-step="code">2</span>
          <span class="auth-step-line"></span>
          <span class="auth-step-dot" data-step="setPassword">3</span>
        </div>
        <p class="auth-step-label auth-hidden" id="auth-step-label"></p>

        <form class="auth-form" id="auth-form" novalidate>
          <div id="auth-panel-content"></div>
          <div class="auth-actions" id="auth-actions"></div>
        </form>

        <p class="auth-hint" id="auth-hint"></p>
        <div class="auth-status" id="auth-status" role="status"></div>
      </div>
    </div>
  `;

  bindAuthPortal(container);
}

function bindAuthPortal(container) {
  /** @type {'login'|'register'|'reset'} */
  let intent = 'login';
  let mode = 'password';
  /** @type {'email'|'code'|'setPassword'} */
  let wizardStep = 'email';
  let countdownTimer = null;
  let pendingEmail = '';
  let pendingCode = '';

  const tabs = container.querySelectorAll('.auth-tab');
  const tabsWrap = container.querySelector('#auth-tabs');
  const modeSwitch = container.querySelector('#auth-mode-switch');
  const modeBtns = container.querySelectorAll('.auth-mode-btn');
  const stepsEl = container.querySelector('#auth-steps');
  const stepLabelEl = container.querySelector('#auth-step-label');
  const panelContent = container.querySelector('#auth-panel-content');
  const actionsEl = container.querySelector('#auth-actions');
  const form = container.querySelector('#auth-form');
  const hintEl = container.querySelector('#auth-hint');
  const statusEl = container.querySelector('#auth-status');
  const titleEl = container.querySelector('#auth-portal-title');
  const subEl = container.querySelector('#auth-portal-sub');
  const backLoginWrap = container.querySelector('#auth-back-login-wrap');
  const backLoginBtn = container.querySelector('#auth-back-login');

  function showStatus(message, type = 'info') {
    statusEl.textContent = message;
    statusEl.className = `auth-status visible ${type}`;
  }

  function clearStatus() {
    statusEl.className = 'auth-status';
    statusEl.textContent = '';
  }

  function exitReset() {
    intent = 'login';
    mode = 'password';
    wizardStep = 'email';
    pendingEmail = '';
    pendingCode = '';
    tabs.forEach((t) => t.classList.toggle('active', t.dataset.intent === 'login'));
    renderPanel();
  }

  function bindPasswordToggles(root) {
    root.querySelectorAll('.auth-pwd-toggle').forEach((btn) => {
      btn.addEventListener('click', () => {
        const input = root.querySelector(`#${btn.dataset.target}`);
        if (!input) return;
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        btn.textContent = show ? '🙈' : '👁';
      });
    });
  }

  function getEmailFromForm() {
    const el = panelContent.querySelector('#auth-email');
    return el ? el.value.trim().toLowerCase() : pendingEmail;
  }

  function validateEmail(email) {
    if (!email) {
      showStatus('请输入邮箱', 'err');
      return false;
    }
    if (!EMAIL_RE.test(email)) {
      showStatus('邮箱格式无效', 'err');
      return false;
    }
    return true;
  }

  function validatePasswordPair(pwdEl, confirmEl, requireConfirm) {
    const password = pwdEl?.value ?? '';
    const confirm = confirmEl?.value ?? '';
    if (!password || password.length < 6) {
      showStatus('密码长度不能少于 6 位', 'err');
      return null;
    }
    if (requireConfirm && password !== confirm) {
      showStatus('两次输入的密码不一致', 'err');
      return null;
    }
    return password;
  }

  function updateChrome() {
    const isReset = intent === 'reset';
    tabsWrap.classList.toggle('auth-hidden', isReset);
    modeSwitch.classList.toggle('auth-hidden', isReset);
    backLoginWrap.classList.toggle('auth-hidden', !isReset);
    titleEl.textContent = isReset ? '重置密码' : '欢迎';
    subEl.textContent = isReset
      ? '通过邮箱验证码设置新密码'
      : '邮箱登录或注册 DevOps 平台';
  }

  function updateStepIndicator() {
    const showSteps =
      (intent === 'register' && mode === 'code') || intent === 'reset';
    stepsEl.classList.toggle('auth-hidden', !showSteps);
    stepsEl.setAttribute('aria-hidden', showSteps ? 'false' : 'true');
    stepLabelEl.classList.toggle('auth-hidden', !showSteps);

    if (!showSteps) return;

    const labels =
      intent === 'reset'
        ? {
            email: '步骤 1：输入邮箱并获取验证码',
            code: '步骤 2：填写邮箱验证码',
            setPassword: '步骤 3：设置新密码',
          }
        : {
            email: '步骤 1：输入邮箱并获取验证码',
            code: '步骤 2：填写邮箱验证码',
            setPassword: '步骤 3：设置登录密码',
          };
    stepLabelEl.textContent = labels[wizardStep] ?? '';

    stepsEl.querySelectorAll('.auth-step-dot').forEach((dot) => {
      const step = dot.dataset.step;
      const order = ['email', 'code', 'setPassword'];
      const current = order.indexOf(wizardStep);
      const idx = order.indexOf(step);
      dot.classList.toggle('active', idx === current);
      dot.classList.toggle('done', idx < current);
    });
  }

  function renderPanel() {
    clearStatus();
    updateChrome();

    const isReg = intent === 'register';
    const isReset = intent === 'reset';
    const isPwd = mode === 'password';
    const isCodeWizard = (isReg && !isPwd) || isReset;

    if (isCodeWizard && wizardStep === 'email') {
      panelContent.innerHTML = `
        <div class="field">
          <label class="label" for="auth-email">邮箱</label>
          <input class="input" id="auth-email" type="email" placeholder="you@example.com" autocomplete="email" value="${pendingEmail}" />
        </div>
      `;
      actionsEl.innerHTML = `
        <button type="button" class="btn btn-ghost" id="auth-send-code">获取验证码</button>
      `;
    } else if (isCodeWizard && wizardStep === 'code') {
      panelContent.innerHTML = `
        <div class="field">
          <label class="label">邮箱</label>
          <input class="input" type="email" value="${pendingEmail}" disabled />
        </div>
        <div class="field">
          <label class="label" for="auth-code">验证码</label>
          <input class="input" id="auth-code" type="text" inputmode="numeric" maxlength="8" autocomplete="one-time-code" placeholder="6 位验证码" value="${pendingCode}" />
        </div>
      `;
      actionsEl.innerHTML = `
        <button type="button" class="btn btn-ghost" id="auth-back">上一步</button>
        <button type="button" class="btn" id="auth-next">下一步</button>
      `;
    } else if (isCodeWizard && wizardStep === 'setPassword') {
      const pwdLabel = isReset ? '新密码' : '设置密码';
      const submitLabel = isReset ? '重置密码' : '完成注册';
      panelContent.innerHTML = `
        <div class="field">
          <label class="label">邮箱</label>
          <input class="input" type="email" value="${pendingEmail}" disabled />
        </div>
        ${passwordFieldHtml('auth-password', pwdLabel, 'new-password')}
        ${passwordFieldHtml('auth-password-confirm', '确认密码', 'new-password')}
      `;
      actionsEl.innerHTML = `
        <button type="button" class="btn btn-ghost" id="auth-back">上一步</button>
        <button type="submit" class="btn" id="auth-submit">${submitLabel}</button>
      `;
      bindPasswordToggles(panelContent);
    } else if (isReg && isPwd) {
      panelContent.innerHTML = `
        <div class="field">
          <label class="label" for="auth-email">邮箱</label>
          <input class="input" id="auth-email" type="email" placeholder="you@example.com" autocomplete="email" />
        </div>
        ${passwordFieldHtml('auth-password', '密码', 'new-password')}
        ${passwordFieldHtml('auth-password-confirm', '确认密码', 'new-password')}
      `;
      actionsEl.innerHTML = `<button type="submit" class="btn auth-submit" id="auth-submit">注册</button>`;
      bindPasswordToggles(panelContent);
    } else if (!isReg && isPwd) {
      panelContent.innerHTML = `
        <div class="field">
          <label class="label" for="auth-email">邮箱</label>
          <input class="input" id="auth-email" type="email" placeholder="you@example.com" autocomplete="email" />
        </div>
        ${passwordFieldHtml('auth-password', '密码', 'current-password')}
        <p class="auth-forgot-wrap">
          <button type="button" class="auth-forgot-link" id="auth-forgot">忘记密码？</button>
        </p>
      `;
      actionsEl.innerHTML = `<button type="submit" class="btn auth-submit" id="auth-submit">登录</button>`;
      bindPasswordToggles(panelContent);
    } else {
      panelContent.innerHTML = `
        <div class="field">
          <label class="label" for="auth-email">邮箱</label>
          <input class="input" id="auth-email" type="email" placeholder="you@example.com" autocomplete="email" />
        </div>
        <div class="field">
          <label class="label" for="auth-code">验证码</label>
          <div class="auth-code-row">
            <input class="input" id="auth-code" type="text" inputmode="numeric" maxlength="8" autocomplete="one-time-code" placeholder="6 位验证码" />
            <button type="button" class="btn btn-ghost" id="auth-send-code">获取验证码</button>
          </div>
        </div>
      `;
      actionsEl.innerHTML = `<button type="submit" class="btn auth-submit" id="auth-submit">登录</button>`;
    }

    bindPanelActions();
    updateStepIndicator();
    updateHint();
  }

  function updateHint() {
    if (intent === 'reset') {
      if (wizardStep === 'email') {
        hintEl.textContent = '输入注册邮箱，获取验证码后重置密码。';
      } else if (wizardStep === 'code') {
        hintEl.textContent = '请填写邮件中的验证码。';
      } else {
        hintEl.textContent = '设置新密码后请使用新密码登录。';
      }
    } else if (intent === 'register' && mode === 'password') {
      hintEl.textContent = '校验邮箱格式后创建账号；请设置并确认密码。';
    } else if (intent === 'register' && mode === 'code') {
      hintEl.textContent = '获取验证码后，验证通过再设置登录密码。';
    } else if (intent === 'login' && mode === 'password') {
      hintEl.textContent = '使用邮箱与密码登录。';
    } else {
      hintEl.textContent = '获取验证码后可直接登录，无需设置密码。';
    }
  }

  function startCountdown(btn) {
    let countdown = COUNTDOWN_SEC;
    btn.disabled = true;
    btn.textContent = `${countdown}s 后重试`;
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(() => {
      countdown -= 1;
      if (countdown <= 0) {
        clearInterval(countdownTimer);
        countdownTimer = null;
        btn.disabled = false;
        btn.textContent = '获取验证码';
      } else {
        btn.textContent = `${countdown}s 后重试`;
      }
    }, 1000);
  }

  function bindPanelActions() {
    panelContent.querySelector('#auth-forgot')?.addEventListener('click', () => {
      intent = 'reset';
      wizardStep = 'email';
      pendingEmail = '';
      pendingCode = '';
      renderPanel();
    });

    const sendBtn =
      actionsEl.querySelector('#auth-send-code') || panelContent.querySelector('#auth-send-code');
    if (sendBtn) {
      sendBtn.addEventListener('click', async () => {
        clearStatus();
        const email = getEmailFromForm();
        if (!validateEmail(email)) return;

        let path;
        if (intent === 'reset') {
          path = '/api/v1/password/reset/email_code';
        } else if (intent === 'register') {
          path = '/api/v1/register';
        } else {
          path = '/api/v1/login';
        }

        sendBtn.disabled = true;
        try {
          const res = await apiRequest('POST', path, { body: { email } });
          if (res.data?.code === 0) {
            pendingEmail = email;
            showStatus('验证码已发送，请查收邮件', 'ok');
            startCountdown(sendBtn);
            if (
              (intent === 'register' && mode === 'code' && wizardStep === 'email') ||
              (intent === 'reset' && wizardStep === 'email')
            ) {
              wizardStep = 'code';
              renderPanel();
            }
          } else {
            showStatus(res.data?.message || '发送失败', 'err');
            sendBtn.disabled = false;
          }
        } catch (e) {
          showStatus(e.message || '网络错误', 'err');
          sendBtn.disabled = false;
        }
      });
    }

    actionsEl.querySelector('#auth-back')?.addEventListener('click', () => {
      clearStatus();
      if (wizardStep === 'setPassword') wizardStep = 'code';
      else if (wizardStep === 'code') wizardStep = 'email';
      renderPanel();
    });

    actionsEl.querySelector('#auth-next')?.addEventListener('click', () => {
      clearStatus();
      const code = panelContent.querySelector('#auth-code')?.value.trim();
      if (!code) {
        showStatus('请输入验证码', 'err');
        return;
      }
      pendingCode = code;
      wizardStep = 'setPassword';
      renderPanel();
    });
  }

  backLoginBtn.addEventListener('click', exitReset);

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      intent = tab.dataset.intent;
      wizardStep = 'email';
      pendingEmail = '';
      pendingCode = '';
      tabs.forEach((t) => t.classList.toggle('active', t === tab));
      renderPanel();
    });
  });

  modeBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      mode = btn.dataset.mode;
      wizardStep = 'email';
      pendingEmail = '';
      pendingCode = '';
      modeBtns.forEach((b) => b.classList.toggle('active', b === btn));
      renderPanel();
    });
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearStatus();

    const submitBtn = actionsEl.querySelector('#auth-submit') || form.querySelector('#auth-submit');
    if (submitBtn) submitBtn.disabled = true;

    try {
      if (intent === 'reset' && wizardStep === 'setPassword') {
        const newPassword = validatePasswordPair(
          panelContent.querySelector('#auth-password'),
          panelContent.querySelector('#auth-password-confirm'),
          true
        );
        if (!newPassword) return;

        const res = await apiRequest('POST', '/api/v1/password/reset', {
          body: { email: pendingEmail, code: pendingCode, newPassword },
        });
        if (res.data?.code === 0) {
          clearSession();
          showStatus('密码已重置，请使用新密码登录', 'ok');
          setTimeout(exitReset, 1500);
        } else {
          showStatus(res.data?.message || '重置失败', 'err');
        }
        return;
      }

      if (intent === 'register' && mode === 'code' && wizardStep === 'setPassword') {
        const password = validatePasswordPair(
          panelContent.querySelector('#auth-password'),
          panelContent.querySelector('#auth-password-confirm'),
          true
        );
        if (!password) return;

        const res = await apiRequest('POST', '/api/v1/register', {
          body: withDeviceId({ email: pendingEmail, code: pendingCode, password }),
        });
        if (res.data?.code === 0) {
          showStatus(`注册并登录成功，userId=${res.data.data?.userId ?? '—'}`, 'ok');
          await refreshSession();
        } else {
          showStatus(res.data?.message || '注册失败', 'err');
        }
        return;
      }

      const email = getEmailFromForm();
      if (!validateEmail(email)) return;

      if (intent === 'register' && mode === 'password') {
        const password = validatePasswordPair(
          panelContent.querySelector('#auth-password'),
          panelContent.querySelector('#auth-password-confirm'),
          true
        );
        if (!password) return;

        const res = await apiRequest('POST', '/api/v1/register', { body: withDeviceId({ email, password }) });
        if (res.data?.code === 0) {
          showStatus(`注册并登录成功，userId=${res.data.data?.userId ?? '—'}`, 'ok');
          await refreshSession();
        } else {
          showStatus(res.data?.message || '注册失败', 'err');
        }
        return;
      }

      if (intent === 'login' && mode === 'password') {
        const password = validatePasswordPair(
          panelContent.querySelector('#auth-password'),
          null,
          false
        );
        if (!password) return;

        const res = await apiRequest('POST', '/api/v1/login', { body: withDeviceId({ email, password }) });
        if (res.data?.code === 0) {
          showStatus('登录成功', 'ok');
          await refreshSession();
        } else {
          showStatus(res.data?.message || '登录失败', 'err');
        }
        return;
      }

      if (intent === 'login' && mode === 'code') {
        const code = panelContent.querySelector('#auth-code')?.value.trim();
        if (!code) {
          showStatus('请输入验证码', 'err');
          return;
        }
        const res = await apiRequest('POST', '/api/v1/login', { body: withDeviceId({ email, code }) });
        if (res.data?.code === 0) {
          showStatus('登录成功', 'ok');
          await refreshSession();
        } else {
          showStatus(res.data?.message || '登录失败', 'err');
        }
      }
    } catch (err) {
      showStatus(err.message || '网络错误', 'err');
    } finally {
      const btn = actionsEl.querySelector('#auth-submit');
      if (btn) btn.disabled = false;
    }
  });

  renderPanel();
}
