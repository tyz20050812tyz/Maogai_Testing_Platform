package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Question;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    public static final String BANK_CHAPTER = "chapter";
    public static final String BANK_EXAM = "exam";

    private static final String CHAPTER_QUESTIONS_PATH = "data/questions.json";
    private static final String EXAM_QUESTIONS_PATH = "data/exam_questions.json";

    private List<Question> chapterQuestions;
    private List<Question> examQuestions;
    private final Random random = new Random();

    public QuestionService() {
        loadQuestions();
    }

    private void loadQuestions() {
        chapterQuestions = readQuestionFile(CHAPTER_QUESTIONS_PATH);
        examQuestions = readQuestionFile(EXAM_QUESTIONS_PATH);

        log.info("加载章节题库 {} 道，考试题库 {} 道", chapterQuestions.size(), examQuestions.size());
    }

    private List<Question> readQuestionFile(String path) {
        try {
            List<Question> list = JsonUtil.readList(path,
                    TypeToken.getParameterized(List.class, Question.class).getType());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("加载题库失败: {}", path);
            return new ArrayList<>();
        }
    }

    public String normalizeBank(String bank) {
        return BANK_CHAPTER.equals(bank) ? BANK_CHAPTER : BANK_EXAM;
    }

    private List<Question> listForBank(String bank) {
        return BANK_CHAPTER.equals(normalizeBank(bank)) ? chapterQuestions : examQuestions;
    }

    private String pathForBank(String bank) {
        return BANK_CHAPTER.equals(normalizeBank(bank)) ? CHAPTER_QUESTIONS_PATH : EXAM_QUESTIONS_PATH;
    }

    public List<Question> getAllQuestions() {
        return getAllQuestions(BANK_EXAM);
    }

    public List<Question> getAllQuestions(String bank) {
        return new ArrayList<>(listForBank(bank));
    }

    public List<Question> getByChapter(int chapterId) {
        return getByChapter(BANK_CHAPTER, chapterId);
    }

    public List<Question> getByChapter(String bank, int chapterId) {
        return listForBank(bank).stream()
                .filter(q -> q.getChapter() == chapterId)
                .collect(Collectors.toList());
    }

    public List<Question> filter(Integer chapter, String type) {
        return filter(BANK_EXAM, chapter, type);
    }

    public List<Question> filter(String bank, Integer chapter, String type) {
        return listForBank(bank).stream()
                .filter(q -> chapter == null || q.getChapter() == chapter)
                .filter(q -> type == null || type.isEmpty() || q.getType().equals(type))
                .collect(Collectors.toList());
    }

    public List<Question> randomPick(int count) {
        return randomPick(BANK_EXAM, count);
    }

    public List<Question> randomPick(String bank, int count) {
        return randomPickFrom(listForBank(bank), count);
    }

    public List<Question> randomPickByChapter(int chapterId, int count) {
        return randomPickByChapter(BANK_CHAPTER, chapterId, count);
    }

    public List<Question> randomPickByChapter(String bank, int chapterId, int count) {
        return randomPickFrom(getByChapter(bank, chapterId), count);
    }

    private List<Question> randomPickFrom(List<Question> source, int count) {
        if (source.isEmpty()) {
            return new ArrayList<>();
        }
        if (count >= source.size()) {
            List<Question> list = new ArrayList<>(source);
            Collections.shuffle(list, random);
            return list;
        }

        Set<Integer> indices = new HashSet<>();
        List<Question> result = new ArrayList<>();
        while (result.size() < count) {
            int idx = random.nextInt(source.size());
            if (indices.add(idx)) {
                result.add(source.get(idx));
            }
        }
        return result;
    }

    public Question getById(int id) {
        return getById(BANK_EXAM, id);
    }

    public Question getById(String bank, int id) {
        return listForBank(bank).stream()
                .filter(q -> q.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Map<String, Object> getStats() {
        return getStats(BANK_EXAM);
    }

    public Map<String, Object> getStats(String bank) {
        List<Question> list = listForBank(bank);
        Map<String, Object> stats = new HashMap<>();
        stats.put("bank", normalizeBank(bank));
        stats.put("total", list.size());
        stats.put("singleCount", list.stream().filter(q -> "single".equals(q.getType())).count());
        stats.put("multipleCount", list.stream().filter(q -> "multiple".equals(q.getType())).count());
        stats.put("judgeCount", list.stream().filter(q -> "judge".equals(q.getType())).count());
        stats.put("chapterCount", list.stream()
                .collect(Collectors.groupingBy(Question::getChapter, Collectors.counting())));
        return stats;
    }

    public synchronized Question addQuestion(Question question) {
        return addQuestion(BANK_CHAPTER, question);
    }

    public synchronized Question addQuestion(String bank, Question question) {
        List<Question> list = listForBank(bank);
        int newId = list.isEmpty() ? 1 : list.stream().mapToInt(Question::getId).max().orElse(0) + 1;
        question.setId(newId);
        list.add(question);
        saveQuestions(bank);
        return question;
    }

    public synchronized int addQuestions(List<Question> newQuestions) {
        return addQuestions(BANK_CHAPTER, newQuestions);
    }

    public synchronized int addQuestions(String bank, List<Question> newQuestions) {
        List<Question> list = listForBank(bank);
        int startId = list.isEmpty() ? 1 : list.stream().mapToInt(Question::getId).max().orElse(0) + 1;
        int count = 0;
        for (Question q : newQuestions) {
            q.setId(startId + count);
            list.add(q);
            count++;
        }
        saveQuestions(bank);
        log.info("向{}题库批量添加 {} 道题目", normalizeBank(bank), count);
        return count;
    }

    private void saveQuestions(String bank) {
        String normalizedBank = normalizeBank(bank);
        String path = pathForBank(normalizedBank);
        List<Question> list = listForBank(normalizedBank);
        try {
            String classPath = QuestionService.class.getClassLoader().getResource("").getPath();
            JsonUtil.writeList(classPath + path, list);

            String srcPath = System.getProperty("user.dir") +
                    File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + path;
            JsonUtil.writeList(srcPath, list);
        } catch (Exception e) {
            log.error("保存题库失败: {}", path, e);
        }
    }

    public int getNextId(int currentId) {
        return getNextId(BANK_EXAM, currentId);
    }

    public int getNextId(String bank, int currentId) {
        List<Question> list = listForBank(bank);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == currentId && i + 1 < list.size()) {
                return list.get(i + 1).getId();
            }
        }
        return -1;
    }

    public int getPrevId(int currentId) {
        return getPrevId(BANK_EXAM, currentId);
    }

    public int getPrevId(String bank, int currentId) {
        List<Question> list = listForBank(bank);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == currentId && i > 0) {
                return list.get(i - 1).getId();
            }
        }
        return -1;
    }
}
