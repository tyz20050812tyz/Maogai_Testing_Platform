package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Question;
import com.maogai.model.WrongRecord;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrongBookService {

    private static final Logger log = LoggerFactory.getLogger(WrongBookService.class);
    private static final String LEGACY_WRONG_PATH = "data/wrong_book.json";
    private static final String USER_WRONG_PATH_PATTERN = "data/users/%s/wrong_book.json";

    private final Map<String, List<WrongRecord>> recordsByUser = new HashMap<>();
    private final QuestionService questionService;

    public WrongBookService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public List<Map<String, Object>> getWrongList() {
        return getWrongList(UserService.GUEST_USER);
    }

    public synchronized List<Map<String, Object>> getWrongList(String userKey) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongRecord record : recordsFor(userKey)) {
            String bank = questionService.normalizeBank(record.getBank());
            String examBank = normalizeRecordExamBank(bank, record.getExamBank());
            Question question = questionService.getById(bank, examBank, record.getQuestionId());
            if (question != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", record.getId());
                map.put("questionId", record.getQuestionId());
                map.put("bank", bank);
                map.put("examBank", examBank);
                map.put("timestamp", record.getTimestamp());
                map.put("userAnswer", record.getUserAnswer());
                map.put("aiExplanation", record.getAiExplanation());
                map.put("question", question);
                result.add(map);
            }
        }
        return result;
    }

    public synchronized WrongRecord addWrong(int questionId) {
        return addWrong(UserService.GUEST_USER, QuestionService.BANK_CHAPTER, null, questionId, null, null);
    }

    public synchronized WrongRecord addWrong(String bank, int questionId) {
        return addWrong(UserService.GUEST_USER, bank, null, questionId, null, null);
    }

    public synchronized WrongRecord addWrong(String bank, String examBank, int questionId) {
        return addWrong(UserService.GUEST_USER, bank, examBank, questionId, null, null);
    }

    public synchronized WrongRecord addWrong(String userKey, String bank, String examBank, int questionId) {
        return addWrong(userKey, bank, examBank, questionId, null, null);
    }

    public synchronized WrongRecord addWrong(String userKey, String bank, String examBank, int questionId,
                                             String userAnswer) {
        return addWrong(userKey, bank, examBank, questionId, userAnswer, null);
    }

    public synchronized WrongRecord addWrong(String userKey, String bank, String examBank, int questionId,
                                             String userAnswer, String aiExplanation) {
        List<WrongRecord> records = recordsFor(userKey);
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        boolean exists = records.stream()
                .anyMatch(r -> r.getQuestionId() == questionId
                        && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                        && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())));
        if (exists) {
            records.stream()
                    .filter(r -> r.getQuestionId() == questionId
                            && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                            && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())))
                    .findFirst()
                    .ifPresent(r -> {
                        r.setTimestamp(System.currentTimeMillis());
                        if (userAnswer != null) r.setUserAnswer(userAnswer);
                        if (aiExplanation != null) r.setAiExplanation(aiExplanation);
                    });
        } else {
            int newId = records.isEmpty() ? 1 :
                    records.stream().mapToInt(WrongRecord::getId).max().orElse(0) + 1;
            records.add(new WrongRecord(newId, questionId, normalizedBank, normalizedExamBank,
                    System.currentTimeMillis(), userAnswer, aiExplanation));
        }
        saveRecords(userKey);
        return records.stream()
                .filter(r -> r.getQuestionId() == questionId
                        && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                        && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())))
                .findFirst()
                .orElse(null);
    }

    public synchronized void updateAIExplanation(String userKey, String bank, String examBank, int questionId,
                                                  String aiExplanation) {
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        recordsFor(userKey).stream()
                .filter(r -> r.getQuestionId() == questionId
                        && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                        && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())))
                .findFirst()
                .ifPresent(r -> {
                    r.setAiExplanation(aiExplanation);
                    saveRecords(userKey);
                });
    }

    public synchronized boolean removeWrong(int questionId) {
        return removeWrong(UserService.GUEST_USER, QuestionService.BANK_CHAPTER, null, questionId);
    }

    public synchronized boolean removeWrong(String bank, int questionId) {
        return removeWrong(UserService.GUEST_USER, bank, null, questionId);
    }

    public synchronized boolean removeWrong(String bank, String examBank, int questionId) {
        return removeWrong(UserService.GUEST_USER, bank, examBank, questionId);
    }

    public synchronized boolean removeWrong(String userKey, String bank, String examBank, int questionId) {
        List<WrongRecord> records = recordsFor(userKey);
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        boolean removed = records.removeIf(r -> r.getQuestionId() == questionId
                && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())));
        if (removed) {
            saveRecords(userKey);
        }
        return removed;
    }

    public boolean isInWrongBook(int questionId) {
        return isInWrongBook(UserService.GUEST_USER, QuestionService.BANK_CHAPTER, null, questionId);
    }

    public boolean isInWrongBook(String bank, int questionId) {
        return isInWrongBook(UserService.GUEST_USER, bank, null, questionId);
    }

    public boolean isInWrongBook(String bank, String examBank, int questionId) {
        return isInWrongBook(UserService.GUEST_USER, bank, examBank, questionId);
    }

    public synchronized boolean isInWrongBook(String userKey, String bank, String examBank, int questionId) {
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        return recordsFor(userKey).stream().anyMatch(r -> r.getQuestionId() == questionId
                && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())));
    }

    public int getCount() {
        return getCount(UserService.GUEST_USER);
    }

    public synchronized int getCount(String userKey) {
        return recordsFor(userKey).size();
    }

    public synchronized void clear() {
        clear(UserService.GUEST_USER);
    }

    public synchronized void clear(String userKey) {
        recordsFor(userKey).clear();
        saveRecords(userKey);
    }

    private List<WrongRecord> recordsFor(String userKey) {
        String key = UserService.toStorageKey(userKey);
        return recordsByUser.computeIfAbsent(key, this::loadRecords);
    }

    private List<WrongRecord> loadRecords(String userKey) {
        try {
            return normalizeRecords(JsonUtil.readList(pathForUser(userKey),
                    TypeToken.getParameterized(List.class, WrongRecord.class).getType()));
        } catch (Exception e) {
            if (UserService.GUEST_USER.equals(userKey)) {
                try {
                    return normalizeRecords(JsonUtil.readList(LEGACY_WRONG_PATH,
                            TypeToken.getParameterized(List.class, WrongRecord.class).getType()));
                } catch (Exception ignored) {
                    // No legacy data either.
                }
            }
            return new ArrayList<>();
        }
    }

    private List<WrongRecord> normalizeRecords(List<WrongRecord> records) {
        if (records == null) {
            records = new ArrayList<>();
        }
        for (WrongRecord record : records) {
            if (record.getBank() == null || record.getBank().trim().isEmpty()) {
                record.setBank(QuestionService.BANK_CHAPTER);
            }
            if (QuestionService.BANK_EXAM.equals(questionService.normalizeBank(record.getBank()))
                    && (record.getExamBank() == null || record.getExamBank().trim().isEmpty())) {
                record.setExamBank(questionService.normalizeExamBank(null));
            }
        }
        return records;
    }

    private void saveRecords(String userKey) {
        String key = UserService.toStorageKey(userKey);
        String path = pathForUser(key);
        List<WrongRecord> records = recordsFor(key);
        try {
            URL resource = WrongBookService.class.getClassLoader().getResource("");
            if (resource != null) {
                File classRoot = new File(resource.toURI());
                JsonUtil.writeList(new File(classRoot, path).getPath(), records);
            }

            String srcPath = System.getProperty("user.dir") +
                    File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + path.replace("/", File.separator);
            JsonUtil.writeList(srcPath, records);
        } catch (Exception e) {
            log.error("Failed to save wrong book for user {}", key, e);
        }
    }

    private String pathForUser(String userKey) {
        return String.format(USER_WRONG_PATH_PATTERN, UserService.toStorageKey(userKey));
    }

    private String normalizeRecordExamBank(String bank, String examBank) {
        if (!QuestionService.BANK_EXAM.equals(questionService.normalizeBank(bank))) {
            return "";
        }
        return questionService.normalizeExamBank(examBank);
    }
}
