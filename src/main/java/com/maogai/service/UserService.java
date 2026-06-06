package com.maogai.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

public class UserService {

    public static final String SESSION_USER = "currentUser";
    public static final String SESSION_USER_KEY = "currentUserKey";
    public static final String GUEST_USER = "guest";

    public String login(HttpServletRequest req, String username) {
        String displayName = normalizeDisplayName(username);
        String userKey = toStorageKey(displayName);
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER, displayName);
        session.setAttribute(SESSION_USER_KEY, userKey);
        return displayName;
    }

    public void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_USER);
            session.removeAttribute(SESSION_USER_KEY);
        }
    }

    public String currentDisplayName(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object value = session == null ? null : session.getAttribute(SESSION_USER);
        return value == null ? GUEST_USER : String.valueOf(value);
    }

    public String currentUserKey(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object value = session == null ? null : session.getAttribute(SESSION_USER_KEY);
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            return String.valueOf(value);
        }
        return toStorageKey(currentDisplayName(req));
    }

    public boolean isLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && session.getAttribute(SESSION_USER_KEY) != null;
    }

    public static String normalizeDisplayName(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            return GUEST_USER;
        }
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        return name;
    }

    public static String toStorageKey(String username) {
        String name = normalizeDisplayName(username).toLowerCase(Locale.ROOT);
        String safe = name.replaceAll("[^\\p{IsHan}\\p{L}\\p{N}_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return safe.isEmpty() ? GUEST_USER : safe;
    }
}
