package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.UserAccount;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);
    private static final String USERS_PATH = "data/users.json";

    private List<UserAccount> users;

    public UserAccountService() {
        loadUsers();
    }

    public synchronized UserAccount registerOrUpdate(String username, String phone) {
        String normalizedPhone = normalizePhone(phone);
        String displayName = UserService.normalizeDisplayName(
                username == null || username.trim().isEmpty() ? normalizedPhone : username);
        String userKey = UserService.toStorageKey(displayName);
        long now = System.currentTimeMillis();

        Optional<UserAccount> existing = findByPhone(normalizedPhone);
        if (existing.isPresent()) {
            UserAccount account = existing.get();
            account.setUsername(displayName);
            account.setUserKey(userKey);
            account.setLastLoginAt(now);
            saveUsers();
            return account;
        }

        int newId = users.isEmpty() ? 1 : users.stream().mapToInt(UserAccount::getId).max().orElse(0) + 1;
        UserAccount account = new UserAccount(newId, displayName, normalizedPhone, userKey, now, now);
        users.add(account);
        saveUsers();
        return account;
    }

    public synchronized UserAccount touchLogin(String loginName) {
        UserAccount account = findByLoginName(loginName).orElse(null);
        if (account != null) {
            account.setLastLoginAt(System.currentTimeMillis());
            saveUsers();
        }
        return account;
    }

    public synchronized Optional<UserAccount> findByLoginName(String loginName) {
        String value = loginName == null ? "" : loginName.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        String phone = normalizePhone(value);
        return users.stream()
                .filter(u -> value.equalsIgnoreCase(u.getUsername())
                        || value.equalsIgnoreCase(u.getUserKey())
                        || phone.equals(u.getPhone()))
                .findFirst();
    }

    public synchronized Optional<UserAccount> findByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return users.stream()
                .filter(u -> normalizedPhone.equals(u.getPhone()))
                .findFirst();
    }

    public String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D+", "");
    }

    public boolean isValidPhone(String phone) {
        return normalizePhone(phone).matches("1[3-9]\\d{9}");
    }

    private void loadUsers() {
        try {
            users = JsonUtil.readList(USERS_PATH,
                    TypeToken.getParameterized(List.class, UserAccount.class).getType());
            if (users == null) {
                users = new ArrayList<>();
            }
        } catch (Exception e) {
            users = new ArrayList<>();
        }
    }

    private void saveUsers() {
        try {
            URL resource = UserAccountService.class.getClassLoader().getResource("");
            if (resource != null) {
                File classRoot = new File(resource.toURI());
                JsonUtil.writeList(new File(classRoot, USERS_PATH).getPath(), users);
            }

            String srcPath = System.getProperty("user.dir") +
                    File.separator + "src" + File.separator + "main" +
                    File.separator + "resources" + File.separator + USERS_PATH.replace("/", File.separator);
            JsonUtil.writeList(srcPath, users);
        } catch (Exception e) {
            log.error("Failed to save users", e);
        }
    }
}
