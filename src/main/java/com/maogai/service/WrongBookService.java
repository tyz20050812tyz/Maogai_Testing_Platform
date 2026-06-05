package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Question;
import com.maogai.model.WrongRecord;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrongBookService {

    private static final Logger log = LoggerFactory.getLogger(WrongBookService.class);
    private static final String WRONG_PATH = "data/wrong_book.json";

    private List<WrongRecord> records;
    private final QuestionService questionService;

    public WrongBookService(QuestionService questionService) {
        this.questionService = questionService;
        loadRecords();
    }

    private void loadRecords() {
        try {
            records = JsonUtil.readList(WRONG_PATH,
                    TypeToken.getParameterized(List.class, WrongRecord.class).getType());
            if (records == null) records = new ArrayList<>();
            for (WrongRecord record : records) {
                if (record.getBank() == null || record.getBank().trim().isEmpty()) {
                    record.setBank(QuestionService.BANK_CHAPTER);
                }
                if (QuestionService.BANK_EXAM.equals(questionService.normalizeBank(record.getBank()))
                        && (record.getExamBank() == null || record.getExamBank().trim().isEmpty())) {
                    record.setExamBank(questionService.normalizeExamBank(null));
                }
            }
            log.info("加载错题记录 {} 条", records.size());
        } catch (Exception e) {
            log.warn("错题记录不存在，初始化为空列表");
            records = new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getWrongList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongRecord record : records) {
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
                map.put("question", question);
                result.add(map);
            }
        }
        return result;
    }

    public synchronized WrongRecord addWrong(int questionId) {
        return addWrong(QuestionService.BANK_CHAPTER, questionId);
    }

    public synchronized WrongRecord addWrong(String bank, int questionId) {
        return addWrong(bank, null, questionId);
    }

    public synchronized WrongRecord addWrong(String bank, String examBank, int questionId) {
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
                    .ifPresent(r -> r.setTimestamp(System.currentTimeMillis()));
        } else {
            int newId = records.isEmpty() ? 1 :
                    records.stream().mapToInt(WrongRecord::getId).max().orElse(0) + 1;
            records.add(new WrongRecord(newId, questionId, normalizedBank, normalizedExamBank, System.currentTimeMillis()));
        }
        saveRecords();
        return records.stream()
                .filter(r -> r.getQuestionId() == questionId
                        && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                        && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())))
                .findFirst()
                .orElse(null);
    }

    public synchronized boolean removeWrong(int questionId) {
        return removeWrong(QuestionService.BANK_CHAPTER, questionId);
    }

    public synchronized boolean removeWrong(String bank, int questionId) {
        return removeWrong(bank, null, questionId);
    }

    public synchronized boolean removeWrong(String bank, String examBank, int questionId) {
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        boolean removed = records.removeIf(r -> r.getQuestionId() == questionId
                && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())));
        if (removed) saveRecords();
        return removed;
    }

    public boolean isInWrongBook(int questionId) {
        return isInWrongBook(QuestionService.BANK_CHAPTER, questionId);
    }

    public boolean isInWrongBook(String bank, int questionId) {
        return isInWrongBook(bank, null, questionId);
    }

    public boolean isInWrongBook(String bank, String examBank, int questionId) {
        String normalizedBank = questionService.normalizeBank(bank);
        String normalizedExamBank = normalizeRecordExamBank(normalizedBank, examBank);
        return records.stream().anyMatch(r -> r.getQuestionId() == questionId
                && normalizedBank.equals(questionService.normalizeBank(r.getBank()))
                && normalizedExamBank.equals(normalizeRecordExamBank(normalizedBank, r.getExamBank())));
    }

    public int getCount() {
        return records.size();
    }

    public synchronized void clear() {
        records.clear();
        saveRecords();
    }

    private void saveRecords() {
        try {
            String classPath = WrongBookService.class.getClassLoader()
                    .getResource("").getPath();
            JsonUtil.writeList(classPath + WRONG_PATH, records);
        } catch (Exception e) {
            log.error("保存错题记录失败", e);
        }
    }

    private String normalizeRecordExamBank(String bank, String examBank) {
        if (!QuestionService.BANK_EXAM.equals(questionService.normalizeBank(bank))) {
            return "";
        }
        return questionService.normalizeExamBank(examBank);
    }
}
