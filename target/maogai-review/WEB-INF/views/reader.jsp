<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="reader-toolbar">
    <button class="btn btn-outline btn-small" onclick="clearChapterHighlights()">清除本章高亮</button>
</div>

<div class="highlight-popover" id="highlight-popover">
    <button type="button" class="btn btn-primary btn-small" onmousedown="event.preventDefault()" onclick="highlightSavedSelection()">高亮</button>
</div>

<div class="card" id="reader-container">
    <p class="loading-state">正在加载章节内容...</p>
</div>

<div style="text-align:center;margin-top:16px;">
    <a href="${pageContext.request.contextPath}/page/outline" class="btn btn-outline">返回章节列表</a>
    <a href="${pageContext.request.contextPath}/page/quiz?bank=chapter" class="btn btn-primary" style="margin-left:8px;">做本章练习题</a>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');

var params = new URLSearchParams(window.location.search);
var chapterId = params.get('id');
var currentUserKey = '${currentUserKey}';
var highlightKey = chapterId ? 'chapter-highlight-' + currentUserKey + '-' + chapterId : '';
var originalChapterHtml = '';
var savedHighlightRange = null;

function getReaderContainer() {
    return document.getElementById('reader-container');
}

function getHighlightPopover() {
    return document.getElementById('highlight-popover');
}

function saveChapterHighlights() {
    if (!highlightKey) return;
    localStorage.setItem(highlightKey, getReaderContainer().innerHTML);
}

function restoreChapterHighlights(html) {
    var saved = highlightKey ? localStorage.getItem(highlightKey) : '';
    originalChapterHtml = html;
    getReaderContainer().innerHTML = saved || html;
}

function hideHighlightPopover() {
    var popover = getHighlightPopover();
    popover.style.display = 'none';
    savedHighlightRange = null;
}

function showHighlightPopoverFromSelection() {
    var container = getReaderContainer();
    var selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
        hideHighlightPopover();
        return;
    }

    var range = selection.getRangeAt(0);
    if (!container.contains(range.commonAncestorContainer)) {
        hideHighlightPopover();
        return;
    }

    var rect = range.getBoundingClientRect();
    if (!rect || (rect.width === 0 && rect.height === 0)) {
        hideHighlightPopover();
        return;
    }

    savedHighlightRange = range.cloneRange();
    var popover = getHighlightPopover();
    popover.style.left = (window.scrollX + rect.left + rect.width / 2) + 'px';
    popover.style.top = (window.scrollY + rect.top - 46) + 'px';
    popover.style.display = 'block';
}

function highlightSavedSelection() {
    if (!savedHighlightRange) return;

    var container = getReaderContainer();
    if (!container.contains(savedHighlightRange.commonAncestorContainer)) {
        hideHighlightPopover();
        return;
    }

    var mark = document.createElement('span');
    mark.className = 'study-highlight';

    try {
        var fragment = savedHighlightRange.extractContents();
        mark.appendChild(fragment);
        savedHighlightRange.insertNode(mark);
        window.getSelection().removeAllRanges();
        saveChapterHighlights();
    } catch (e) {
        alert('这段内容跨越较复杂，建议少选一点再高亮。');
    } finally {
        hideHighlightPopover();
    }
}

function clearChapterHighlights() {
    if (!highlightKey) return;
    localStorage.removeItem(highlightKey);
    getReaderContainer().innerHTML = originalChapterHtml;
    hideHighlightPopover();
}

document.addEventListener('mouseup', function(event) {
    if (getHighlightPopover().contains(event.target)) return;
    setTimeout(showHighlightPopoverFromSelection, 0);
});

document.addEventListener('keyup', function() {
    setTimeout(showHighlightPopoverFromSelection, 0);
});

document.addEventListener('mousedown', function(event) {
    var popover = getHighlightPopover();
    if (!popover.contains(event.target) && !getReaderContainer().contains(event.target)) {
        hideHighlightPopover();
    }
});

if (chapterId) {
    Quiz.getJson(Quiz.ctx + '/api/outline/reader?id=' + chapterId)
        .then(function(data) {
            if (data.content) {
                restoreChapterHighlights(data.content);
            } else {
                getReaderContainer().innerHTML =
                    '<p class="empty-state">' + (data.message || '章节内容加载失败') + '</p>';
            }
        })
        .catch(function() {
            getReaderContainer().innerHTML =
                '<p class="empty-state">加载失败，请检查章节内容是否存在</p>';
        });

    var quizLink = document.querySelector('a[href*="page/quiz"]');
    if (quizLink) {
        quizLink.href = Quiz.ctx + '/page/quiz?bank=chapter&chapter=' + chapterId;
    }
} else {
    getReaderContainer().innerHTML = '<p class="empty-state">请选择章节</p>';
}
</script>

<%@ include file="footer.jsp" %>
