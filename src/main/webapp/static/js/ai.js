/**
 * AI 对话和题库解析交互逻辑
 */
var AI = {
    ctx: '',
    _parsedQuestions: [],

    init: function(ctx) {
        this.ctx = ctx || '';
    },

    escapeHtml: function(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    sendQuestion: function() {
        var input = document.getElementById('ai-input');
        if (!input) return;

        var question = input.value.trim();
        if (!question) return;

        var container = document.getElementById('chat-messages');
        this.appendMessage(container, 'user', question);
        input.value = '';
        input.focus();

        var self = this;
        var loadingMsg = this.appendMessage(container, 'ai', '思考中', 'loading');

        fetch(this.ctx + '/api/ai/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question: question })
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (loadingMsg) loadingMsg.remove();

            if (data.success) {
                self.appendMessage(container, 'ai', data.answer || '我暂时没有生成回答。');
            } else {
                self.appendMessage(container, 'ai', '抱歉，回答失败：' + (data.message || '未知错误'));
            }
        })
        .catch(function() {
            if (loadingMsg) loadingMsg.remove();
            self.appendMessage(container, 'ai', '网络错误，请稍后重试。');
        });
    },

    appendMessage: function(container, role, text, className) {
        if (!container) return null;

        var div = document.createElement('div');
        div.className = 'chat-message ' + role + (className ? ' ' + className : '');
        div.innerHTML = '<div class="chat-bubble">' + this.escapeHtml(text).replace(/\n/g, '<br>') + '</div>';
        container.appendChild(div);
        container.scrollTop = container.scrollHeight;
        return div;
    },

    handleKeydown: function(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendQuestion();
        }
    },

    parseText: function() {
        var textarea = document.getElementById('import-text-input');
        var chapterSelect = document.getElementById('import-chapter');
        var bankSelect = document.getElementById('import-bank');
        if (!textarea) return;

        var text = textarea.value.trim();
        if (!text) {
            alert('请输入题库文本');
            return;
        }

        var chapter = chapterSelect ? parseInt(chapterSelect.value, 10) : 1;
        var bank = bankSelect ? bankSelect.value : 'chapter';
        var resultDiv = document.getElementById('import-result');
        if (resultDiv) resultDiv.innerHTML = '<div class="loading-state">正在解析题库...</div>';

        var self = this;
        fetch(this.ctx + '/api/ai/parse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text, chapter: chapter, bank: bank })
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            self.showParseResult(data, resultDiv);
        })
        .catch(function(err) {
            if (resultDiv) resultDiv.innerHTML = '<div class="import-result error">解析失败：' + self.escapeHtml(err) + '</div>';
        });
    },

    showParseResult: function(data, container) {
        if (!container) return;
        this._lastResultContainer = container;

        if (!data || !data.success) {
            container.innerHTML = '<div class="import-result error">' + this.escapeHtml((data && data.message) || '解析失败') + '</div>';
            return;
        }

        this._parsedQuestions = data.questions || [];

        var html = '<div class="import-result success">';
        html += '<p>识别模式：' + (data.mode === 'image' ? '图片识别' : '文本解析') + '</p>';
        html += '<p>识别出 <strong>' + this.escapeHtml(data.count || this._parsedQuestions.length) + '</strong> 道题目</p>';

        if (this._parsedQuestions.length) {
            html += '<div style="margin-top:12px">';
            for (var i = 0; i < Math.min(this._parsedQuestions.length, 5); i++) {
                var q = this._parsedQuestions[i];
                var type = q.type === 'single' ? '[单选]' : q.type === 'multiple' ? '[多选]' : '[判断]';
                html += '<div class="import-preview-question">';
                html += '<strong>' + type + '</strong> ' + this.escapeHtml(q.question);
                html += ' <span style="color:#667085">答案：' + this.escapeHtml(q.answer) + '</span>';
                html += '</div>';
            }
            if (this._parsedQuestions.length > 5) {
                html += '<p style="color:#667085;font-size:0.86rem;">还有 ' + (this._parsedQuestions.length - 5) + ' 道题未展示</p>';
            }
            html += '</div>';
        }

        if (data.rawText) {
            html += '<details style="margin-top:12px">';
            html += '<summary style="cursor:pointer;color:#667085">查看原始识别文本</summary>';
            html += '<pre style="background:#f8fafc;padding:10px;border-radius:8px;margin-top:8px;white-space:pre-wrap;font-size:0.86rem;">' + this.escapeHtml(data.rawText) + '</pre>';
            html += '</details>';
        }

        html += '<button class="btn btn-success btn-small" style="margin-top:12px" onclick="AI.importToBank()">确认导入题库</button>';
        html += '</div>';
        container.innerHTML = html;
    },

    importToBank: function() {
        if (!this._parsedQuestions || this._parsedQuestions.length === 0) {
            alert('没有可导入的题目');
            return;
        }

        var textarea = document.getElementById('import-text-input');
        var chapterSelect = document.getElementById('import-chapter');
        var bankSelect = document.getElementById('import-bank');
        var text = textarea && textarea.value.trim() ? textarea.value.trim() : JSON.stringify(this._parsedQuestions);
        var chapter = chapterSelect ? parseInt(chapterSelect.value, 10) : 1;
        var bank = bankSelect ? bankSelect.value : 'chapter';
        var resultDiv = this._lastResultContainer || document.getElementById('import-result');
        if (resultDiv) resultDiv.innerHTML = '<div class="loading-state">正在导入题库...</div>';

        var self = this;
        fetch(this.ctx + '/api/import/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text, chapter: chapter, bank: bank })
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (!resultDiv) return;
            if (data.success) {
                resultDiv.innerHTML = '<div class="import-result success">' +
                    '<p>成功导入 <strong>' + self.escapeHtml(data.count) + '</strong> 道题目到题库。</p>' +
                    '<button class="btn btn-primary btn-small" onclick="location.reload()">继续导入</button>' +
                    '</div>';
            } else {
                resultDiv.innerHTML = '<div class="import-result error">导入失败：' + self.escapeHtml(data.message || '未知错误') + '</div>';
            }
        })
        .catch(function(err) {
            if (resultDiv) resultDiv.innerHTML = '<div class="import-result error">导入失败：' + self.escapeHtml(err) + '</div>';
        });
    },

    uploadImage: function(input) {
        var file = input.files[0];
        if (!file) return;

        if (!file.type || !file.type.startsWith('image/')) {
            alert('请选择图片文件');
            return;
        }

        var resultDiv = document.getElementById('import-result');
        if (resultDiv) resultDiv.innerHTML = '<div class="loading-state">正在识别图片...</div>';

        var reader = new FileReader();
        var self = this;
        reader.onload = function(e) {
            var base64 = e.target.result.split(',')[1];
            var chapterSelect = document.getElementById('import-chapter');
            var bankSelect = document.getElementById('import-bank');
            var chapter = chapterSelect ? parseInt(chapterSelect.value, 10) : 1;
            var bank = bankSelect ? bankSelect.value : 'chapter';

            fetch(self.ctx + '/api/ai/parse', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ image: base64, chapter: chapter, bank: bank })
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                self.showParseResult(data, resultDiv);
            })
            .catch(function(err) {
                if (resultDiv) resultDiv.innerHTML = '<div class="import-result error">识别失败：' + self.escapeHtml(err) + '</div>';
            });
        };
        reader.readAsDataURL(file);
    },

    switchTab: function(tabName) {
        document.querySelectorAll('.import-tab').forEach(function(tab) {
            tab.classList.remove('active');
        });
        document.querySelectorAll('.import-tab-panel').forEach(function(panel) {
            panel.style.display = 'none';
        });

        var activeTab = document.querySelector('.import-tab[data-tab="' + tabName + '"]');
        var activePanel = document.getElementById('tab-' + tabName);
        if (activeTab) activeTab.classList.add('active');
        if (activePanel) activePanel.style.display = 'block';
    }
};
