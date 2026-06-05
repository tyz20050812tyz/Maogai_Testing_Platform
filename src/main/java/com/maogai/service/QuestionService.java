package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Question;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final String LEGACY_EXAM_QUESTIONS_PATH = "data/exam_questions.json";
    private static final String EXAM_BANKS_MANIFEST_PATH = "data/exam_banks.json";
    private static final String DEFAULT_EXAM_BANK_ID = "exam-default";

    private List<Question> chapterQuestions;
    private final List<Map<String, Object>> examBanks = new ArrayList<>();
    private final Map<String, List<Question>> examQuestionBanks = new LinkedHashMap<>();
    private final Random random = new Random();

    public QuestionService() {
        loadQuestions();
    }

    private void loadQuestions() {
        chapterQuestions = readQuestionFile(CHAPTER_QUESTIONS_PATH);
        loadExamBanks();

        int examTotal = examQuestionBanks.values().stream().mapToInt(List::size).sum();
        log.info("加载章节题库 {} 道，考试题库 {} 个文件共 {} 道",
                chapterQuestions.size(), examBanks.size(), examTotal);
    }

    private void loadExamBanks() {
        examBanks.clear();
        examQuestionBanks.clear();

        List<Map<String, Object>> manifest = readExamBankManifest();
        if (manifest.isEmpty()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("id", DEFAULT_EXAM_BANK_ID);
            fallback.put("name", "考试题库");
            fallback.put("source", "exam_questions.json");
            fallback.put("path", LEGACY_EXAM_QUESTIONS_PATH);
            manifest.add(fallback);
        }

        for (Map<String, Object> item : manifest) {
            String id = stringValue(item.get("id"));
            String path = stringValue(item.get("path"));
            if (id.isEmpty() || path.isEmpty()) {
                continue;
            }
            Map<String, Object> bank = new HashMap<>();
            bank.put("id", id);
            bank.put("name", stringValue(item.get("name")).isEmpty() ? id : stringValue(item.get("name")));
            bank.put("source", stringValue(item.get("source")));
            bank.put("path", path);

            List<Question> questions = readQuestionFile(path);
            bank.put("count", questions.size());
            examBanks.add(bank);
            examQuestionBanks.put(id, questions);
        }

        if (examBanks.isEmpty()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("id", DEFAULT_EXAM_BANK_ID);
            fallback.put("name", "考试题库");
            fallback.put("source", "exam_questions.json");
            fallback.put("path", LEGACY_EXAM_QUESTIONS_PATH);
            List<Question> questions = readQuestionFile(LEGACY_EXAM_QUESTIONS_PATH);
            fallback.put("count", questions.size());
            examBanks.add(fallback);
            examQuestionBanks.put(DEFAULT_EXAM_BANK_ID, questions);
        }
    }

    private List<Map<String, Object>> readExamBankManifest() {
        try {
            List<Map<String, Object>> list = JsonUtil.readList(EXAM_BANKS_MANIFEST_PATH,
                    TypeToken.getParameterized(List.class, Map.class).getType());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("加载考试题库清单失败: {}", EXAM_BANKS_MANIFEST_PATH);
            return new ArrayList<>();
        }
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public String normalizeBank(String bank) {
        return BANK_CHAPTER.equals(bank) ? BANK_CHAPTER : BANK_EXAM;
    }

    public String normalizeExamBank(String examBank) {
        if (examBanks.isEmpty()) {
            return DEFAULT_EXAM_BANK_ID;
        }
        String candidate = stringValue(examBank);
        if (!candidate.isEmpty() && examQuestionBanks.containsKey(candidate)) {
            return candidate;
        }
        return stringValue(examBanks.get(0).get("id"));
    }

    public List<Map<String, Object>> getExamBanks() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> bank : examBanks) {
            result.add(new HashMap<>(bank));
        }
        return result;
    }

    private List<Question> listForBank(String bank) {
        return listForBank(bank, null);
    }

    private List<Question> listForBank(String bank, String examBank) {
        if (BANK_CHAPTER.equals(normalizeBank(bank))) {
            return chapterQuestions;
        }
        return examQuestionBanks.getOrDefault(normalizeExamBank(examBank), new ArrayList<>());
    }

    private String pathForBank(String bank) {
        return pathForBank(bank, null);
    }

    private String pathForBank(String bank, String examBank) {
        if (BANK_CHAPTER.equals(normalizeBank(bank))) {
            return CHAPTER_QUESTIONS_PATH;
        }
        String normalizedExamBank = normalizeExamBank(examBank);
        for (Map<String, Object> item : examBanks) {
            if (normalizedExamBank.equals(stringValue(item.get("id")))) {
                return stringValue(item.get("path"));
            }
        }
        return LEGACY_EXAM_QUESTIONS_PATH;
    }

    public List<Question> getAllQuestions() {
        return getAllQuestions(BANK_EXAM);
    }

    public List<Question> getAllQuestions(String bank) {
        if (BANK_EXAM.equals(normalizeBank(bank))) {
            List<Question> all = new ArrayList<>();
            for (List<Question> questions : examQuestionBanks.values()) {
                all.addAll(questions);
            }
            return all;
        }
        return new ArrayList<>(listForBank(bank));
    }

    public List<Question> getAllQuestions(String bank, String examBank) {
        return new ArrayList<>(listForBank(bank, examBank));
    }

    public List<Question> getByChapter(int chapterId) {
        return getByChapter(BANK_CHAPTER, chapterId);
    }

    public List<Question> getByChapter(String bank, int chapterId) {
        return getByChapter(bank, null, chapterId);
    }

    public List<Question> getByChapter(String bank, String examBank, int chapterId) {
        return listForBank(bank, examBank).stream()
                .filter(q -> q.getChapter() == chapterId)
                .collect(Collectors.toList());
    }

    public List<Question> filter(Integer chapter, String type) {
        return filter(BANK_EXAM, chapter, type);
    }

    public List<Question> filter(String bank, Integer chapter, String type) {
        return filter(bank, null, chapter, type);
    }

    public List<Question> filter(String bank, String examBank, Integer chapter, String type) {
        return listForBank(bank, examBank).stream()
                .filter(q -> chapter == null || q.getChapter() == chapter)
                .filter(q -> type == null || type.isEmpty() || q.getType().equals(type))
                .collect(Collectors.toList());
    }

    public List<Question> randomPick(int count) {
        return randomPick(BANK_EXAM, count);
    }

    public List<Question> randomPick(String bank, int count) {
        return randomPick(bank, null, count);
    }

    public List<Question> randomPick(String bank, String examBank, int count) {
        return randomPickFrom(listForBank(bank, examBank), count);
    }

    public List<Question> randomPickByChapter(int chapterId, int count) {
        return randomPickByChapter(BANK_CHAPTER, chapterId, count);
    }

    public List<Question> randomPickByChapter(String bank, int chapterId, int count) {
        return randomPickByChapter(bank, null, chapterId, count);
    }

    public List<Question> randomPickByChapter(String bank, String examBank, int chapterId, int count) {
        return randomPickFrom(getByChapter(bank, examBank, chapterId), count);
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
        return getById(bank, null, id);
    }

    public Question getById(String bank, String examBank, int id) {
        return listForBank(bank, examBank).stream()
                .filter(q -> q.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Map<String, Object> getStats() {
        return getStats(BANK_EXAM);
    }

    public Map<String, Object> getStats(String bank) {
        if (BANK_EXAM.equals(normalizeBank(bank))) {
            return getStatsForList(bank, getAllQuestions(BANK_EXAM));
        }
        return getStatsForList(bank, listForBank(bank));
    }

    public Map<String, Object> getStats(String bank, String examBank) {
        Map<String, Object> stats = getStatsForList(bank, listForBank(bank, examBank));
        if (BANK_EXAM.equals(normalizeBank(bank))) {
            stats.put("examBank", normalizeExamBank(examBank));
        }
        return stats;
    }

    private Map<String, Object> getStatsForList(String bank, List<Question> list) {
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
        return addQuestion(bank, null, question);
    }

    public synchronized Question addQuestion(String bank, String examBank, Question question) {
        List<Question> list = listForBank(bank, examBank);
        int newId = list.isEmpty() ? 1 : list.stream().mapToInt(Question::getId).max().orElse(0) + 1;
        question.setId(newId);
        list.add(question);
        saveQuestions(bank, examBank);
        return question;
    }

    public synchronized int addQuestions(List<Question> newQuestions) {
        return addQuestions(BANK_CHAPTER, newQuestions);
    }

    public synchronized int addQuestions(String bank, List<Question> newQuestions) {
        return addQuestions(bank, null, newQuestions);
    }

    public synchronized int addQuestions(String bank, String examBank, List<Question> newQuestions) {
        List<Question> list = listForBank(bank, examBank);
        int startId = list.isEmpty() ? 1 : list.stream().mapToInt(Question::getId).max().orElse(0) + 1;
        int count = 0;
        for (Question q : newQuestions) {
            q.setId(startId + count);
            list.add(q);
            count++;
        }
        saveQuestions(bank, examBank);
        log.info("向{}题库批量添加 {} 道题目", normalizeBank(bank), count);
        return count;
    }

    private void saveQuestions(String bank) {
        saveQuestions(bank, null);
    }

    private void saveQuestions(String bank, String examBank) {
        String normalizedBank = normalizeBank(bank);
        String path = pathForBank(normalizedBank, examBank);
        List<Question> list = listForBank(normalizedBank, examBank);
        try {
            URL resource = QuestionService.class.getClassLoader().getResource("");
            if (resource != null) {
                File classRoot = new File(resource.toURI());
                JsonUtil.writeList(new File(classRoot, path).getPath(), list);
            }

            String srcPath = System.getProperty("user.dir") +
                    File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + path.replace("/", File.separator);
            JsonUtil.writeList(srcPath, list);
        } catch (Exception e) {
            log.error("保存题库失败: {}", path, e);
        }
    }

    public int getNextId(int currentId) {
        return getNextId(BANK_EXAM, currentId);
    }

    public int getNextId(String bank, int currentId) {
        return getNextId(bank, null, currentId);
    }

    public int getNextId(String bank, String examBank, int currentId) {
        List<Question> list = listForBank(bank, examBank);
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
        return getPrevId(bank, null, currentId);
    }

    public int getPrevId(String bank, String examBank, int currentId) {
        List<Question> list = listForBank(bank, examBank);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == currentId && i > 0) {
                return list.get(i - 1).getId();
            }
        }
        return -1;
    }
}
