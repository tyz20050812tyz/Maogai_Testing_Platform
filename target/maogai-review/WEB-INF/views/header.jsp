<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    String currentUser = session.getAttribute("currentUser") == null
            ? "guest"
            : String.valueOf(session.getAttribute("currentUser"));
    String currentUserKey = session.getAttribute("currentUserKey") == null
            ? "guest"
            : String.valueOf(session.getAttribute("currentUserKey"));
    pageContext.setAttribute("currentUser", currentUser);
    pageContext.setAttribute("currentUserKey", currentUserKey);
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>毛概复习助手</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260606-authwall">
    <script src="${pageContext.request.contextPath}/static/js/quiz.js?v=20260606-authwall"></script>
    <script src="${pageContext.request.contextPath}/static/js/ai.js?v=20260606-authwall"></script>
</head>
<body>
<header class="main-header">
    <div class="header-container">
        <a href="${pageContext.request.contextPath}/index" class="logo">
            毛概复习助手
        </a>
        <nav class="main-nav" aria-label="主导航">
            <c:if test="${currentUserKey != 'guest'}">
            <a data-path="/page/outline" href="${pageContext.request.contextPath}/page/outline">章节大纲</a>
            <a data-path="/page/quiz" href="${pageContext.request.contextPath}/page/quiz">题库练习</a>
            <a data-path="/page/random" href="${pageContext.request.contextPath}/page/random">随机测试</a>
            <a data-path="/page/wrong" href="${pageContext.request.contextPath}/page/wrong">错题本</a>
            <a data-path="/page/flashcards" href="${pageContext.request.contextPath}/page/flashcards">速记</a>
            <a data-path="/page/ai" href="${pageContext.request.contextPath}/page/ai">AI答疑</a>
            <a data-path="/page/import" href="${pageContext.request.contextPath}/page/import">题目导入</a>
            <a data-path="/page/stats" href="${pageContext.request.contextPath}/page/stats">统计</a>
            </c:if>
            <span class="user-badge">当前：<c:out value="${currentUser}"/></span>
            <c:choose>
                <c:when test="${currentUserKey == 'guest'}">
                    <a data-path="/page/login" href="${pageContext.request.contextPath}/page/login">登录</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/auth/logout">退出</a>
                </c:otherwise>
            </c:choose>
        </nav>
    </div>
</header>
<main class="main-content">
<script>
(function() {
    var path = window.location.pathname;
    document.querySelectorAll('.main-nav a').forEach(function(link) {
        if (path.indexOf(link.getAttribute('data-path')) !== -1) {
            link.classList.add('active');
        }
    });
})();
</script>
