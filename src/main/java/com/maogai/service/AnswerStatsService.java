package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.AnswerRecord;
import com.maogai.model.Question;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnswerStatsService {

    private static final Logger log = LoggerFactory.getLogger(AnswerStatsService.class);
    private static final String LEGACY_ANSWER_RECORDS_PATH = "data/answer_records.json";
    private static final String USER_ANSWER_RECORDS_PATH_PATTERN = "data/users/%s/answer_records.json";

    private final QuestionService questionService;
    private final OutlineService outlineService;
    private final Map<String, List<AnswerRecord>> recordsByUser = new HashMap<>();

    public AnswerStatsService(QuestionService questionService, OutlineService outlineService) {
        this.questionService = questionService;
        this.outlineService = outlineService;
    }

    public void record(String bank, String examBank, Question question, String userAnswer, boolean correct) {
        record(UserService.GUEST_USER, bank, examBank, question, userAnswer, correct);
    }

    public synchronized void record(String userKey, String bank, String examBank, Question question, String userAnswer, boolean correct) {
        List<AnswerRecord> records = recordsFor(userKey);
        int newId = records.isEmpty() ? 1 : records.stream().mapToInt(AnswerRecord::getId).max().orElse(0) + 1;
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = QuestionService.BANK_EXAM.equals(normalizedBank)
                ? questionService.normalizeExamBank(examBank)
                : "";
        records.add(new AnswerRecord(
                newId,
                question.getId(),
                question.getChapter(),
                normalizedBank,
                normalizedExamBank,
                question.getType(),
                userAnswer,
                question.getAnswer(),
                correct,
                System.currentTimeMillis()
        ));
        saveRecords(userKey);
    }

    public Map<String, Object> getChapterMastery() {
        return getChapterMastery(UserService.GUEST_USER);
    }

    public synchronized Map<String, Object> getChapterMastery(String userKey) {
        List<AnswerRecord> records = recordsFor(userKey);
        List<Map<String, Object>> chapters = outlineService.getChapterList();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Map<String, Object> chapter : chapters) {
            int chapterId = ((Number) chapter.get("id")).intValue();
            String title = String.valueOf(chapter.get("title"));
            long total = records.stream()
                    .filter(r -> r.getChapter() == chapterId)
                    .count();
            long correct = records.stream()
                    .filter(r -> r.getChapter() == chapterId && r.isCorrect())
                    .count();
            int questionTotal = questionService.getByChapter(QuestionService.BANK_CHAPTER, chapterId).size();
            int rate = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);

            Map<String, Object> item = new HashMap<>();
            item.put("chapter", chapterId);
            item.put("title", title);
            item.put("attempts", total);
            item.put("correct", correct);
            item.put("accuracy", rate);
            item.put("questionTotal", questionTotal);
            item.put("level", levelFor(total, rate));
            item.put("suggested", false);
            items.add(item);
        }

        Map<String, Object> suggestion = findSuggestion(items);
        if (suggestion != null) {
            suggestion.put("suggested", true);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("chapters", items);
        result.put("suggestion", suggestion);
        return result;
    }

    private List<AnswerRecord> recordsFor(String userKey) {
        String key = UserService.toStorageKey(userKey);
        return recordsByUser.computeIfAbsent(key, this::loadRecords);
    }

    private List<AnswerRecord> loadRecords(String userKey) {
        try {
            List<AnswerRecord> records = JsonUtil.readList(pathForUser(userKey),
                    TypeToken.getParameterized(List.class, AnswerRecord.class).getType());
            return records == null ? new ArrayList<>() : records;
        } catch (Exception e) {
            if (UserService.GUEST_USER.equals(userKey)) {
                try {
                    List<AnswerRecord> legacy = JsonUtil.readList(LEGACY_ANSWER_RECORDS_PATH,
                            TypeToken.getParameterized(List.class, AnswerRecord.class).getType());
                    return legacy == null ? new ArrayList<>() : legacy;
                } catch (Exception ignored) {
                    // No legacy data either.
                }
            }
            return new ArrayList<>();
        }
    }

    private String levelFor(long attempts, int rate) {
        if (attempts == 0) return "empty";
        if (rate < 60) return "red";
        if (rate < 80) return "yellow";
        return "green";
    }

    private Map<String, Object> findSuggestion(List<Map<String, Object>> items) {
        return items.stream()
                .filter(item -> ((Number) item.get("questionTotal")).intValue() > 0)
                .min(Comparator
                        .comparingInt((Map<String, Object> item) -> levelRank(String.valueOf(item.get("level"))))
                        .thenComparingInt(item -> ((Number) item.get("accuracy")).intValue())
                        .thenComparingLong(item -> ((Number) item.get("attempts")).longValue()))
                .orElse(null);
    }

    private int levelRank(String level) {
        if ("red".equals(level)) return 0;
        if ("empty".equals(level)) return 1;
        if ("yellow".equals(level)) return 2;
        return 3;
    }

    private void saveRecords(String userKey) {
        String key = UserService.toStorageKey(userKey);
        String path = pathForUser(key);
        List<AnswerRecord> records = recordsFor(key);
        try {
            URL resource = AnswerStatsService.class.getClassLoader().getResource("");
            if (resource != null) {
                File classRoot = new File(resource.toURI());
                JsonUtil.writeList(new File(classRoot, path).getPath(), records);
            }

            String srcPath = System.getProperty("user.dir") +
                    File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + path.replace("/", File.separator);
            JsonUtil.writeList(srcPath, records);
        } catch (Exception e) {
            log.error("Failed to save answer records for user {}", key, e);
        }
    }

    private String pathForUser(String userKey) {
        return String.format(USER_ANSWER_RECORDS_PATH_PATTERN, UserService.toStorageKey(userKey));
    }
}
