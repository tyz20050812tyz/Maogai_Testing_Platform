/**
 * 答题交互逻辑
 */
var Quiz = {
    ctx: '',
    bank: 'exam',
    _randomQuestions: [],
    _randomIndex: 0,
    _randomScore: 0,
    _randomAnswered: 0,

    init: function(ctx) {
        this.ctx = ctx || '';
    },

    setBank: function(bank) {
        this.bank = bank === 'chapter' ? 'chapter' : 'exam';
    },

    loadChapterOptions: function(selectIds, selectedValue, includeAll, callback) {
        var ids = Array.isArray(selectIds) ? selectIds : [selectIds];
        var self = this;

        fetch(this.ctx + '/api/outline/list')
            .then(function(r) { return r.json(); })
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

    setLoading: function(container, text) {
        if (container) {
            container.innerHTML = '<div class="loading-state">' + this.escapeHtml(text || '正在加载...') + '</div>';
        }
    },

    loadQuestions: function(chapter, type, containerId, bank) {
        bank = bank || this.bank || 'exam';
        var container = document.getElementById(containerId);
        this.setLoading(container, '正在加载题目...');

        var url = this.ctx + '/api/quiz/list';
        var params = ['bank=' + encodeURIComponent(bank)];
        if (chapter) params.push('chapter=' + encodeURIComponent(chapter));
        if (type) params.push('type=' + encodeURIComponent(type));
        if (params.length > 0) url += '?' + params.join('&');

        var self = this;
        fetch(url)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (Array.isArray(data)) {
                    self.renderQuestionList(data, containerId);
                } else {
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
                bank: q.bank || bank
            });
        });
        container.innerHTML = html;
    },

    renderQuestionCard: function(q, options) {
        options = options || {};
        var bank = options.bank || q.bank || this.bank || 'exam';
        var html = '<div class="question-card" id="q-' + q.id + '" data-type="' + this.escapeHtml(q.type) + '" data-bank="' + this.escapeHtml(bank) + '">';
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

        var answer = this.getAnswerFromCard(card, 'q-' + questionId);
        if (!answer) {
            alert('请先选择答案');
            return;
        }

        var self = this;
        fetch(this.ctx + '/api/quiz/check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bank: bank, questionId: questionId, answer: answer })
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

        resultDiv.innerHTML = html;
    },

    toggleWrong: function(questionId, btn) {
        if (!btn) return;
        var isInWrong = btn.getAttribute('data-wrong') === 'true';
        var card = btn.closest('.question-card');
        var bank = card ? (card.getAttribute('data-bank') || this.bank || 'exam') : (this.bank || 'exam');
        var url = this.ctx + '/api/quiz/' + (isInWrong ? 'remove-wrong' : 'wrong');

        btn.disabled = true;
        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bank: bank, questionId: questionId })
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

    randomQuiz: function(chapter, count, containerId, bank) {
        bank = bank || this.bank || 'exam';
        this._randomBank = bank;
        var container = document.getElementById(containerId);
        this.setLoading(container, '正在抽取题目...');

        var url = this.ctx + '/api/quiz/random';
        var params = ['bank=' + encodeURIComponent(bank)];
        if (count) params.push('count=' + encodeURIComponent(count));
        if (chapter && chapter > 0) params.push('chapter=' + encodeURIComponent(chapter));
        if (params.length > 0) url += '?' + params.join('&');

        var self = this;
        fetch(url)
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (Array.isArray(data) && data.length) {
                    self.renderRandomQuiz(data, containerId);
                } else {
                    container.innerHTML = '<div class="empty-state">题库中暂时没有可抽取的题目</div>';
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
        var html = '<div class="question-card" id="random-card" data-type="' + this.escapeHtml(q.type) + '" data-bank="' + this.escapeHtml(bank) + '">';
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

        var answer = this.getAnswerFromCard(card, 'random-q');
        if (!answer) {
            alert('请先选择答案');
            return;
        }

        var self = this;
        fetch(this.ctx + '/api/quiz/check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bank: bank, questionId: q.id, answer: answer })
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
                resultDiv.innerHTML = html;
            }

            card.querySelectorAll('input').forEach(function(input) {
                input.disabled = true;
            });

            setTimeout(function() {
                self._randomIndex++;
                self.renderSingleRandomQuestion(card.parentElement);
            }, 1200);
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

        fetch(this.ctx + '/api/wrong/list')
            .then(function(r) { return r.json(); })
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
        records.forEach(function(record) {
            var q = record.question || {};
            var bank = record.bank || q.bank || 'exam';
            html += '<div class="wrong-item">';
            html += '<div class="wrong-item-info">';
            html += '<div class="wrong-item-title">' + self.escapeHtml(q.question || '题目已不存在') + '</div>';
            html += '<div class="wrong-item-meta">' + (bank === 'chapter' ? '章节题库' : '考试题库') + ' · 正确答案：' + self.escapeHtml(q.answer || '-') + '</div>';
            html += '</div>';
            html += '<div class="wrong-item-actions">';
            html += '<button class="btn btn-outline btn-small" onclick="Quiz.removeWrong(' + q.id + ', \'' + bank + '\')">移除</button>';
            html += '</div>';
            html += '</div>';
        });
        container.innerHTML = html;
    },

    removeWrong: function(questionId, bank) {
        bank = bank || this.bank || 'exam';
        var self = this;
        fetch(this.ctx + '/api/wrong/remove', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bank: bank, questionId: questionId })
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
