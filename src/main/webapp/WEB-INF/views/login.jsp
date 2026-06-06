<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>
<%
    Object authMessage = session.getAttribute("authMessage");
    if (authMessage != null) {
        pageContext.setAttribute("authMessage", String.valueOf(authMessage));
        session.removeAttribute("authMessage");
    }
    String loginRedirect = request.getParameter("redirect");
    String contextPath = request.getContextPath();
    boolean safeRedirect = loginRedirect != null && !loginRedirect.trim().isEmpty()
            && ((contextPath == null || contextPath.isEmpty())
                ? (loginRedirect.startsWith("/") && !loginRedirect.startsWith("//"))
                : (loginRedirect.equals(contextPath) || loginRedirect.startsWith(contextPath + "/")));
    if (!safeRedirect) {
        loginRedirect = request.getContextPath() + "/index";
    }
    pageContext.setAttribute("loginRedirect", loginRedirect);
%>

<div class="card login-card">
    <h2 class="card-title">用户登录与注册</h2>
    <p class="section-subtitle">请先注册或登录，进入后才能访问首页、题库、统计和 AI 功能。</p>

    <c:if test="${not empty authMessage}">
        <div class="import-result error"><c:out value="${authMessage}"/></div>
    </c:if>

    <div class="login-grid">
        <form method="post" action="${pageContext.request.contextPath}/auth/register" class="login-form">
            <input type="hidden" name="redirect" value="${loginRedirect}">
            <h3>手机号注册</h3>
            <label>
                昵称
                <input type="text" name="username" placeholder="例如：佟雨泽" maxlength="40">
            </label>
            <label>
                手机号
                <input type="tel" id="register-phone" name="phone" placeholder="请输入 11 位手机号" required maxlength="11">
            </label>
            <label>
                验证码
                <div class="sms-code-row">
                    <input type="text" name="code" placeholder="6 位验证码" required maxlength="6">
                    <button class="btn btn-outline" type="button" id="send-code-btn" onclick="sendRegisterCode()">发送验证码</button>
                </div>
            </label>
            <button class="btn btn-primary" type="submit">注册并进入</button>
            <div class="empty-state login-note" id="sms-message"></div>
        </form>

        <form method="post" action="${pageContext.request.contextPath}/auth/login" class="login-form">
            <input type="hidden" name="redirect" value="${loginRedirect}">
            <h3>已注册用户登录</h3>
            <label>
                用户名或手机号
                <input type="text" name="username" placeholder="请输入已注册手机号或昵称" required maxlength="40">
            </label>
            <button class="btn btn-outline" type="submit">进入学习档案</button>
            <div class="empty-state login-note">
                当前用户：<c:out value="${currentUser}"/>
            </div>
        </form>
    </div>
</div>

<script>
function sendRegisterCode() {
    var phoneInput = document.getElementById('register-phone');
    var btn = document.getElementById('send-code-btn');
    var msg = document.getElementById('sms-message');
    var phone = phoneInput ? phoneInput.value.trim() : '';
    if (!/^1[3-9]\d{9}$/.test(phone)) {
        msg.textContent = '请输入正确的手机号';
        return;
    }

    btn.disabled = true;
    msg.textContent = '正在发送验证码...';
    fetch('${pageContext.request.contextPath}/auth/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: 'phone=' + encodeURIComponent(phone)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        msg.textContent = data.message || (data.success ? '验证码已发送' : '发送失败');
        if (!data.success) {
            btn.disabled = false;
        } else {
            startSmsCountdown(btn);
        }
    })
    .catch(function() {
        msg.textContent = '发送失败，请稍后重试';
        btn.disabled = false;
    });
}

function startSmsCountdown(btn) {
    var left = 60;
    btn.textContent = left + '秒后重发';
    var timer = setInterval(function() {
        left--;
        if (left <= 0) {
            clearInterval(timer);
            btn.disabled = false;
            btn.textContent = '发送验证码';
        } else {
            btn.textContent = left + '秒后重发';
        }
    }, 1000);
}
</script>

<%@ include file="footer.jsp" %>
