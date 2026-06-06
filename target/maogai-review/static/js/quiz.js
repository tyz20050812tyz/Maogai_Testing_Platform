/**
 * 答题交互逻辑
 */
var Quiz = {
    ctx: '',
    bank: 'exam',
    examBank: '',
    _randomQuestions: [],
    _randomIndex: 0,
    _randomScore: 0,
    _randomAnswered: 0,
    _randomBank: 'exam',
    _randomExamBank: '',

    init: function(ctx) {
        this.ctx = ctx || '';
    },

    ready: function(callback) {
        if (typeof callback !== 'function') return;
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', callback, { once: true });
        } else {
            callback();
        }
    },

    getJson: function(url, retries) {
        retries = retries == null ? 1 : retries;
        function request() {
            return fetch(url, {
                cache: 'no-store',
                headers: { 'Accept': 'application/json' }
            }).then(function(r) {
                if (!r.ok) {
                    throw new Error('HTTP ' + r.status);
                }
                return r.json();
            });
        }

        return request().catch(function(err) {
            if (retries <= 0) {
                throw err;
            }
            return new Promise(function(resolve) {
                setTimeout(resolve, 350);
            }).then(function() {
                return Quiz.getJson(url, retries - 1);
            });
        });
    },

    setBank: function(bank) {
        this.bank = bank === 'chapter' ? 'chapter' : 'exam';
    },

    setExamBank: function(examBank) {
        this.examBank = examBank || this.examBank || '';
    },

    loadChapterOptions: function(selectIds, selectedValue, includeAll, callback) {
        var ids = Array.isArray(selectIds) ? selectIds : [selectIds];
        var self = this;

        this.getJson(this.ctx + '/api/outline/list')
            .then(function(chapters) {
                ids.forEach(function(id) {
                    var select = document.getElementById(id);
                    if (!select) return;

                    var html = includeAll ? '<option value="">全部章节</option>' : '';
                    if (Array.isArray(chapters)) {
                        chapters.forEach(function(chapter) {
                            html += '<option value="' + chapter.id + '">' + self.escapeHtml(chapter.title) + '</option>';
                        });
                    }
                    select.innerHTML = html;
                    if (selectedValue != null) {
                        select.value = selectedValue;
                    }
                });
                if (typeof callback === 'function') {
                    callback(chapters);
                }
            })
            .catch(function() {
                ids.forEach(function(id) {
                    var select = document.getElementById(id);
                    if (select && !select.options.length) {
                        select.innerHTML = includeAll ? '<option value="">全部章节</option>' : '<option value="1">导论</option>';
                    }
                });
                if (typeof callback === 'function') {
                    callback([]);
                }
            });
    },

    loadExamBankOptions: function(selectIds, selectedValue, callback) {
        var ids = Array.isArray(selectIds) ? selectIds : [selectIds];
        var self = this;

        this.getJson(this.ctx + '/api/quiz/exam-banks')
            .then(function(banks) {
                var firstId = '';
                ids.forEach(function(id) {
                    var select = document.getElementById(id);
                    if (!select) return;

                    var html = '';
                    if (Array.isArray(banks)) {
                        banks.forEach(function(bank, index) {
                            if (index === 0) firstId = bank.id || '';
                            var label = (bank.name || bank.id || '考试题库') + '（' + (bank.count || 0) + '题）';
                            html += '<option value="' + self.escapeHtml(bank.id) + '">' + self.escapeHtml(label) + '</option>';
                        });
                    }
                    select.innerHTML = html || '<option value="">暂无考试题库文件</option>';
                    select.value = selectedValue || firstId || select.value;
                });

                self.examBank = selectedValue || firstId || self.examBank || '';
                if (typeof callback === 'function') {
                    callback(banks || []);
                }
            })
            .catch(function() {
                ids.forEach(function(id) {
                    var select = document.getElementById(id);
                    if (select && !select.options.length) {
                        select.innerHTML = '<option value="">题库文件加载失败</option>';
                    }
                });
                if (typeof callback === 'function') {
                    callback([]);
                }
            });
    },

    escapeHtml: function(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    typeLabel: function(type) {
        if (type === 'single') return '单选题';
        if (type === 'multiple') return '多选题';
        return '判断题';
    },

    bankParams: function(bank, examBank) {
        bank = bank || this.bank || 'exam';
        examBank = examBank || this.examBank || '';
        var params = ['bank=' + encodeURIComponent(bank)];
        if (bank === 'exam' && examBank) {
            params.push('examBank=' + encodeURIComponent(examBank));
        }
        return params;
    },

    requestBody: function(bank, examBank, data) {
        var body = data || {};
        body.bank = bank || this.bank || 'exam';
        if (body.bank === 'exam') {
            body.examBank = examBank || this.examBank || '';
        }
        return body;
    },

    setLoading: function(container, text) {
        if (container) {
            container.innerHTML = '<div class="loading-state">' + this.escapeHtml(text || '正在加载...') + '</div>';
        }
    },

    loadQuestions: function(chapter, type, containerId, bank, examBank) {
        bank = bank || this.bank || 'exam';
        examBank = examBank || this.examBank || '';
        var container = document.getElementById(containerId);
        this.setLoading(container, '正在加载题目...');

        var params = this.bankParams(bank, examBank);
        if (chapter) params.push('chapter=' + encodeURIComponent(chapter));
        if (type) params.push('type=' + encodeURIComponent(type));

        var self = this;
        this.getJson(this.ctx + '/api/quiz/list?' + params.join('&'))
            .then(function(data) {
                if (Array.isArray(data)) {
                    self.renderQuestionList(data, containerId);
                } else if (container) {
                    container.innerHTML = '<div class="empty-state">暂时没有匹配的题目</div>';
                }
            })
            .catch(function(err) {
                console.error('加载题目失败:', err);
                if (container) container.innerHTML = '<div class="empty-state">题目加载失败，请稍后重试</div>';
            });
    },

    renderQuestionList: function(questions, containerId) {
        var container = document.getElementById(containerId);
        if (!container) return;

        if (!questions.length) {
            container.innerHTML = '<div class="empty-state">当前筛选条件下还没有题目</div>';
            return;
        }

        var html = '';
        var self = this;
        questions.forEach(function(q, index) {
            html += self.renderQuestionCard(q, {
                indexText: '第 ' + (index + 1) + ' 题',
                name: 'q-' + q.id,
                actions: true,
                bank: q.bank || self.bank,
                examBank: q.examBank || self.examBank
            });
        });
        container.innerHTML = html;
    },

    renderQuestionCard: function(q, options) {
        options = options || {};
        var bank = options.bank || q.bank || this.bank || 'exam';
        var examBank = options.examBank || q.examBank || this.examBank || '';
        var html = '<div class="question-card" id="q-' + q.id + '" data-type="' + this.escapeHtml(q.type) + '" data-bank="' + this.escapeHtml(bank) + '" data-exam-bank="' + this.escapeHtml(examBank) + '">';
        html += '  <div class="question-header">';
        html += '    <span class="question-type type-' + this.escapeHtml(q.type) + '">' + this.typeLabel(q.type) + '</span>';
        html += '    <span class="quiz-counter">' + this.escapeHtml(options.indexText || '') + '</span>';
        html += '  </div>';
        html += '  <div class="question-text">' + this.escapeHtml(q.question) + '</div>';
        html += '  <div class="options-list">';

        if (q.options && q.options.length) {
            for (var i = 0; i < q.options.length; i++) {
                var value = String.fromCharCode(65 + i);
                var inputType = q.type === 'multiple' ? 'checkbox' : 'radio';
                html += '    <label class="option-item">';
                html += '      <input type="' + inputType + '" name="' + this.escapeHtml(options.name || ('q-' + q.id)) + '" value="' + value + '" onchange="Quiz.onOptionChange(this)">';
                html += '      <span>' + this.escapeHtml(q.options[i]) + '</span>';
                html += '    </label>';
            }
        }

        html += '  </div>';

        if (options.actions) {
            html += '  <div class="question-actions">';
            html += '    <button class="btn btn-primary btn-small" onclick="Quiz.checkAnswer(' + q.id + ')">提交答案</button>';
            html += '    <button class="btn btn-outline btn-small" data-wrong="' + (q.inWrongBook ? 'true' : 'false') + '" onclick="Quiz.toggleWrong(' + q.id + ', this)">';
            html += q.inWrongBook ? '移出错题本' : '加入错题本';
            html += '    </button>';
            html += '  </div>';
        }

        html += '  <div id="result-' + q.id + '"></div>';
        html += '</div>';
        return html;
    },

    onOptionChange: function(input) {
        var card = input.closest('.question-card');
        if (!card) return;

        var groupName = input.getAttribute('name');
        var inputs = card.querySelectorAll('input[name="' + groupName + '"]');
        inputs.forEach(function(item) {
            var option = item.closest('.option-item');
            if (option) option.classList.toggle('selected', item.checked);
        });
    },

    getAnswerFromCard: function(card, name) {
        var checked = card.querySelectorAll('input[name="' + name + '"]:checked');
        return Array.from(checked).map(function(input) {
            return input.value;
        }).sort().join('');
    },

    checkAnswer: function(questionId) {
        var card = document.getElementById('q-' + questionId);
        if (!card) return;
        var bank = card.getAttribute('data-bank') || this.bank || 'exam';
        var examBank = card.getAttribute('data-exam-bank') || this.examBank || '';

        var answer = this.getAnswerFromCard(card, 'q-' + questionId);
        if (!answer) {
            alert('请先选择答案');
            return;
        }

        var self = this;
        fetch(this.ctx + '/api/quiz/check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.requestBody(bank, examBank, { questionId: questionId, answer: answer }))
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            self.showResult(questionId, data);
        })
        .catch(function() {
            var resultDiv = document.getElementById('result-' + questionId);
            if (resultDiv) resultDiv.innerHTML = '<div class="answer-result wrong-result">提交失败，请稍后重试</div>';
        });
    },

    showResult: function(questionId, data) {
        var resultDiv = document.getElementById('result-' + questionId);
        var card = document.getElementById('q-' + questionId);
        if (!resultDiv || !card) return;

        var options = card.querySelectorAll('.option-item');
        var userAnswer = data.userAnswer || '';
        var correctAnswer = data.correctAnswer || '';

        options.forEach(function(opt) {
            var input = opt.querySelector('input');
            if (!input) return;
            input.disabled = true;
            if (correctAnswer.indexOf(input.value) !== -1) {
                opt.classList.add('correct');
            }
            if (!data.correct && userAnswer.indexOf(input.value) !== -1 && correctAnswer.indexOf(input.value) === -1) {
                opt.classList.add('wrong');
            }
        });

        var html = '<div class="answer-result ' + (data.correct ? 'correct-result' : 'wrong-result') + '">';
        html += data.correct ? '回答正确' : '回答错误';
        html += '，正确答案：' + this.escapeHtml(correctAnswer);
        html += '</div>';

        if (data.explanation) {
            html += '<div class="explanation-box">' + this.escapeHtml(data.explanation) + '</div>';
        }
        if (!data.correct) {
            var bank = card.getAttribute('data-bank') || this.bank || 'exam';
            var examBank = card.getAttribute('data-exam-bank') || this.examBank || '';
            html += '<div id="ai-explain-' + questionId + '"></div>';
        }

        resultDiv.innerHTML = html;
        if (!data.correct) {
            this.explainWrongAnswer(questionId, bank, examBank, userAnswer, 'ai-explain-' + questionId);
        }
    },

    explainWrongAnswer: function(questionId, bank, examBank, answer, containerId) {
        var container = document.getElementById(containerId);
        if (!container) return;
        container.innerHTML = '<div class="loading-state">AI正在分析错因...</div>';
        var self = this;
        fetch(this.ctx + '/api/quiz/explain', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.requestBody(bank, examBank, { questionId: questionId, answer: answer }))
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                var explanationHtml = '<div class="explanation-box ai-explanation">' + self.escapeHtml(data.explanation || '').replace(/\n/g, '<br>') + '</div>';
                container.innerHTML = explanationHtml;
                // 保存AI解析到错题本
                fetch(self.ctx + '/api/wrong/explain', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(self.requestBody(bank, examBank, {
                        questionId: questionId,
                        aiExplanation: data.explanation || ''
                    }))
                }).catch(function() { /* 静默失败，不影响主流程 */ });
            } else {
                container.innerHTML = '<div class="empty-state">' + self.escapeHtml(data.message || 'AI解释失败') + '</div>';
            }
        })
        .catch(function() {
            container.innerHTML = '<div class="empty-state">AI解释失败，请稍后重试</div>';
        });
    },

    toggleWrong: function(questionId, btn) {
        if (!btn) return;
        var isInWrong = btn.getAttribute('data-wrong') === 'true';
        var card = btn.closest('.question-card');
        var bank = card ? (card.getAttribute('data-bank') || this.bank || 'exam') : (this.bank || 'exam');
        var examBank = card ? (card.getAttribute('data-exam-bank') || this.examBank || '') : (this.examBank || '');
        var url = this.ctx + '/api/quiz/' + (isInWrong ? 'remove-wrong' : 'wrong');

        btn.disabled = true;
        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.requestBody(bank, examBank, { questionId: questionId }))
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                btn.setAttribute('data-wrong', isInWrong ? 'false' : 'true');
                btn.textContent = isInWrong ? '加入错题本' : '移出错题本';
            } else {
                alert(data.message || '操作失败');
            }
        })
        .catch(function() {
            alert('操作失败，请稍后重试');
        })
        .finally(function() {
            btn.disabled = false;
        });
    },

    randomQuiz: function(chapter, count, containerId, bank, examBank) {
        bank = bank || this.bank || 'exam';
        examBank = examBank || this.examBank || '';
        this._randomBank = bank;
        this._randomExamBank = examBank;
        var container = document.getElementById(containerId);
        this.setLoading(container, '正在抽取题目...');

        var params = this.bankParams(bank, examBank);
        if (count) params.push('count=' + encodeURIComponent(count));
        if (chapter && chapter > 0) params.push('chapter=' + encodeURIComponent(chapter));

        var self = this;
        this.getJson(this.ctx + '/api/quiz/random?' + params.join('&'))
            .then(function(data) {
                if (Array.isArray(data) && data.length) {
                    self.renderRandomQuiz(data, containerId);
                } else if (container) {
                    container.innerHTML = '<div class="empty-state">当前题库文件里暂时没有可抽取的题目</div>';
                }
            })
            .catch(function() {
                if (container) container.innerHTML = '<div class="empty-state">随机测验加载失败，请稍后重试</div>';
            });
    },

    renderRandomQuiz: function(questions, containerId) {
        this._randomQuestions = questions;
        this._randomIndex = 0;
        this._randomScore = 0;
        this._randomAnswered = 0;
        this.renderSingleRandomQuestion(document.getElementById(containerId));
    },

    renderSingleRandomQuestion: function(container) {
        if (!container) return;

        var q = this._randomQuestions[this._randomIndex];
        if (!q) {
            this.showRandomScore(container);
            return;
        }

        var total = this._randomQuestions.length;
        var idx = this._randomIndex + 1;
        var bank = q.bank || this._randomBank || this.bank || 'exam';
        var examBank = q.examBank || this._randomExamBank || this.examBank || '';
        var html = '<div class="question-card" id="random-card" data-type="' + this.escapeHtml(q.type) + '" data-bank="' + this.escapeHtml(bank) + '" data-exam-bank="' + this.escapeHtml(examBank) + '">';
        html += '  <div class="question-header">';
        html += '    <span class="question-type type-' + this.escapeHtml(q.type) + '">' + this.typeLabel(q.type) + '</span>';
        html += '    <span class="quiz-counter">' + idx + ' / ' + total + '</span>';
        html += '  </div>';
        html += '  <div class="question-text">' + this.escapeHtml(q.question) + '</div>';
        html += '  <div class="options-list">';
        if (q.options && q.options.length) {
            for (var i = 0; i < q.options.length; i++) {
                var value = String.fromCharCode(65 + i);
                var inputType = q.type === 'multiple' ? 'checkbox' : 'radio';
                html += '    <label class="option-item">';
                html += '      <input type="' + inputType + '" name="random-q" value="' + value + '" onchange="Quiz.onOptionChange(this)">';
                html += '      <span>' + this.escapeHtml(q.options[i]) + '</span>';
                html += '    </label>';
            }
        }
        html += '  </div>';
        html += '  <div class="question-actions"><button class="btn btn-primary btn-small" onclick="Quiz.checkRandomAnswer()">提交并继续</button></div>';
        html += '  <div id="random-result"></div>';
        html += '</div>';

        html += '<div class="quiz-nav">';
        html += '<span class="quiz-counter">已答 ' + this._randomAnswered + ' 题，正确 ' + this._randomScore + ' 题</span>';
        html += '<div class="progress-bar" style="width:180px"><div class="progress-fill" style="width:' + Math.round((this._randomAnswered / total) * 100) + '%"></div></div>';
        html += '</div>';

        container.innerHTML = html;
    },

    checkRandomAnswer: function() {
        var q = this._randomQuestions[this._randomIndex];
        var card = document.querySelector('.question-card');
        if (!q || !card) return;
        var bank = card.getAttribute('data-bank') || this._randomBank || this.bank || 'exam';
        var examBank = card.getAttribute('data-exam-bank') || this._randomExamBank || this.examBank || '';

        var answer = this.getAnswerFromCard(card, 'random-q');
        if (!answer) {
            alert('请先选择答案');
            return;
        }

        var self = this;
        fetch(this.ctx + '/api/quiz/check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.requestBody(bank, examBank, { questionId: q.id, answer: answer }))
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.correct) self._randomScore++;
            self._randomAnswered++;

            var resultDiv = document.getElementById('random-result');
            if (resultDiv) {
                var html = '<div class="answer-result ' + (data.correct ? 'correct-result' : 'wrong-result') + '">';
                html += data.correct ? '正确' : '错误，正确答案：' + self.escapeHtml(data.correctAnswer);
                html += '</div>';
                if (data.explanation) {
                    html += '<div class="explanation-box">' + self.escapeHtml(data.explanation) + '</div>';
                }
                if (!data.correct) {
                    html += '<div id="random-ai-explain"></div>';
                }
                resultDiv.innerHTML = html;
                if (!data.correct) {
                    self.explainWrongAnswer(q.id, bank, examBank, answer, 'random-ai-explain');
                }
            }

            card.querySelectorAll('input').forEach(function(input) {
                input.disabled = true;
            });

            setTimeout(function() {
                self._randomIndex++;
                self.renderSingleRandomQuestion(card.parentElement);
            }, data.correct ? 1200 : 6500);
        });
    },

    loadMasteryMap: function(containerId, suggestionId) {
        var container = document.getElementById(containerId);
        if (!container) return;
        this.setLoading(container, '正在计算掌握度...');
        this.getJson(this.ctx + '/api/quiz/mastery')
            .then(function(data) {
                var chapters = data.chapters || [];
                if (!chapters.length) {
                    container.innerHTML = '<div class="empty-state">暂无章节掌握度数据</div>';
                    return;
                }
                var html = '<div class="mastery-grid">';
                chapters.forEach(function(item) {
                    var level = item.level || 'empty';
                    var label = level === 'green' ? '稳' : level === 'yellow' ? '补' : level === 'red' ? '弱' : '未练';
                    html += '<a class="mastery-item mastery-' + level + '" href="' + Quiz.ctx + '/page/quiz?bank=chapter&chapter=' + item.chapter + '">';
                    html += '<strong>' + Quiz.escapeHtml(item.title) + '</strong>';
                    html += '<span>' + label + ' · 正确率 ' + (item.attempts ? item.accuracy + '%' : '暂无') + '</span>';
                    html += '<small>已答 ' + (item.attempts || 0) + ' / 题库 ' + (item.questionTotal || 0) + '</small>';
                    html += '</a>';
                });
                html += '</div>';
                container.innerHTML = html;

                if (suggestionId) {
                    var suggestionEl = document.getElementById(suggestionId);
                    var suggestion = data.suggestion;
                    if (suggestionEl && suggestion) {
                        suggestionEl.innerHTML = '今天建议复习：<a href="' + Quiz.ctx + '/page/quiz?bank=chapter&chapter=' + suggestion.chapter + '">' + Quiz.escapeHtml(suggestion.title) + '</a>';
                    }
                }
            })
            .catch(function() {
                container.innerHTML = '<div class="empty-state">掌握度加载失败，请稍后重试</div>';
            });
    },

    showRandomScore: function(container) {
        var total = this._randomQuestions.length || 1;
        var score = this._randomScore;
        var percent = Math.round(score / total * 100);
        var msg = percent >= 90 ? '非常优秀' : percent >= 70 ? '表现良好' : percent >= 60 ? '继续巩固' : '需要加强复习';

        container.innerHTML =
            '<div class="random-score">' +
            '<div class="score-number">' + score + ' / ' + total + '</div>' +
            '<div class="score-detail">正确率 ' + percent + '% - ' + msg + '</div>' +
            '<button class="btn btn-primary" style="margin-top:16px" onclick="location.reload()">再来一次</button>' +
            '</div>';
    },

    loadWrongBook: function(containerId) {
        var self = this;
        var container = document.getElementById(containerId);
        this.setLoading(container, '正在加载错题...');
        if (!container) return;

        this.getJson(this.ctx + '/api/wrong/list')
            .then(function(data) {
                if (data.success && data.data && data.data.length) {
                    self.renderWrongBook(data.data, container);
                } else {
                    container.innerHTML = '<div class="empty-state">暂无错题记录，答错的题会自动收录到这里</div>';
                }
            })
            .catch(function() {
                container.innerHTML = '<div class="empty-state">错题加载失败，请稍后重试</div>';
            });
    },

    renderWrongBook: function(records, container) {
        var html = '';
        var self = this;
        records.forEach(function(record, index) {
            var q = record.question || {};
            var bank = record.bank || q.bank || 'exam';
            var examBank = record.examBank || q.examBank || '';
            var bankLabel = bank === 'chapter' ? '章节题库' : '考试题库';
            var answer = q.answer || '';

            html += '<div class="wrong-book-card">';

            // 题头：题型 + 序号 + 来源
            html += '  <div class="question-header">';
            html += '    <span class="question-type type-' + self.escapeHtml(q.type) + '">' + self.typeLabel(q.type) + '</span>';
            html += '    <span class="quiz-counter">第 ' + (index + 1) + ' 题</span>';
            html += '    <span class="wrong-bank-tag">' + self.escapeHtml(bankLabel) + (examBank ? ' · ' + self.escapeHtml(examBank) : '') + '</span>';
            html += '  </div>';

            // 题目正文
            html += '  <div class="question-text">' + self.escapeHtml(q.question || '题目已不存在') + '</div>';

            // 选项列表（只读展示，高亮正确答案）
            if (q.options && q.options.length) {
                html += '  <div class="options-list">';
                for (var i = 0; i < q.options.length; i++) {
                    var letter = String.fromCharCode(65 + i);
                    var isCorrect = answer.indexOf(letter) !== -1;
                    html += '    <div class="option-item ' + (isCorrect ? 'correct' : '') + '">';
                    html += '      <span class="option-letter">' + letter + '</span>';
                    html += '      <span>' + self.escapeHtml(q.options[i]) + '</span>';
                    if (isCorrect) {
                        html += '      <span class="correct-badge">✓ 正确答案</span>';
                    }
                    html += '    </div>';
                }
                html += '  </div>';
            }

            // 正确答案 & 错选答案 & 解析
            var userAnswer = record.userAnswer || '';
            var aiExplanation = record.aiExplanation || '';
            html += '  <div class="wrong-answer-section">';
            html += '    <strong>正确答案：</strong>' + self.escapeHtml(answer || '-');
            if (userAnswer) {
                html += '    <span class="wrong-user-answer">你选了：<em>' + self.escapeHtml(userAnswer) + '</em></span>';
            }
            if (q.explanation) {
                html += '    <div class="explanation-box">' + self.escapeHtml(q.explanation) + '</div>';
            }
            html += '  </div>';

            // AI 错因解析
            if (aiExplanation) {
                html += '  <div class="wrong-ai-section">';
                html += '    <div class="wrong-ai-label">🤖 AI 错因分析</div>';
                html += '    <div class="explanation-box ai-explanation">' + self.escapeHtml(aiExplanation).replace(/\n/g, '<br>') + '</div>';
                html += '  </div>';
            }

            // 操作按钮
            html += '  <div class="wrong-item-actions">';
            html += '    <button class="btn btn-outline btn-small" onclick="Quiz.removeWrong(' + q.id + ', \'' + self.escapeHtml(bank) + '\', \'' + self.escapeHtml(examBank) + '\')">移出错题本</button>';
            html += '  </div>';

            html += '</div>';
        });
        container.innerHTML = html;
    },

    removeWrong: function(questionId, bank, examBank) {
        bank = bank || this.bank || 'exam';
        examBank = examBank || this.examBank || '';
        var self = this;
        fetch(this.ctx + '/api/wrong/remove', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.requestBody(bank, examBank, { questionId: questionId }))
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                self.loadWrongBook('wrong-list-container');
            } else {
                alert(data.message || '移除失败');
            }
        });
    }
};
