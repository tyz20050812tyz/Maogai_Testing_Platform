package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.Flashcard;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);
    private static final String FLASHCARDS_PATH = "data/flashcards.json";

    private List<Flashcard> cards;

    public FlashcardService() {
        loadCards();
    }

    private void loadCards() {
        try {
            cards = JsonUtil.readList(FLASHCARDS_PATH,
                    TypeToken.getParameterized(List.class, Flashcard.class).getType());
            if (cards == null) {
                cards = new ArrayList<>();
            }
            log.info("加载速记卡片 {} 张", cards.size());
        } catch (Exception e) {
            cards = new ArrayList<>();
            log.warn("速记卡片不存在，初始化为空列表");
        }
    }

    public List<Flashcard> list(Integer chapter) {
        return cards.stream()
                .filter(card -> chapter == null || card.getChapter() == chapter)
                .collect(Collectors.toList());
    }
}
