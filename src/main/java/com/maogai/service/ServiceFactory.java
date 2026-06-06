package com.maogai.service;

/**
 * 服务工厂 - 管理所有服务单例
 */
public class ServiceFactory {

    private static final QuestionService questionService = new QuestionService();
    private static final OutlineService outlineService = new OutlineService();
    private static final WrongBookService wrongBookService = new WrongBookService(questionService);
    private static final AIService aiService = new AIService();
    private static final ImportService importService = new ImportService(aiService, questionService);
    private static final AnswerStatsService answerStatsService = new AnswerStatsService(questionService, outlineService);
    private static final FlashcardService flashcardService = new FlashcardService();
    private static final UserService userService = new UserService();
    private static final UserAccountService userAccountService = new UserAccountService();
    private static final SmsService smsService = new SmsService();

    public static QuestionService getQuestionService() { return questionService; }
    public static OutlineService getOutlineService() { return outlineService; }
    public static WrongBookService getWrongBookService() { return wrongBookService; }
    public static AIService getAIService() { return aiService; }
    public static ImportService getImportService() { return importService; }
    public static AnswerStatsService getAnswerStatsService() { return answerStatsService; }
    public static FlashcardService getFlashcardService() { return flashcardService; }
    public static UserService getUserService() { return userService; }
    public static UserAccountService getUserAccountService() { return userAccountService; }
    public static SmsService getSmsService() { return smsService; }
}
