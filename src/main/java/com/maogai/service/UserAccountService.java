package com.maogai.service;

import com.google.gson.reflect.TypeToken;
import com.maogai.model.UserAccount;
import com.maogai.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    public synchronized UserAccount register(String username, String phone, String password) {
        String normalizedPhone = normalizePhone(phone);
        String displayName = UserService.normalizeDisplayName(username);
        String userKey = UserService.toStorageKey(displayName);

        if (UserService.GUEST_USER.equals(displayName)) {
            throw new IllegalArgumentException("请设置用户名");
        }
        if (!isValidPhone(normalizedPhone)) {
            throw new IllegalArgumentException("请输入正确的手机号");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (findByPhone(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("该手机号已注册，请直接登录");
        }
        if (findByUsername(displayName).isPresent()) {
            throw new IllegalArgumentException("该用户名已存在，请换一个");
        }

        long now = System.currentTimeMillis();
        int newId = users.isEmpty() ? 1 : users.stream().mapToInt(UserAccount::getId).max().orElse(0) + 1;
        UserAccount account = new UserAccount(newId, displayName, normalizedPhone, userKey, hashPassword(password), now, now);
        users.add(account);
        saveUsers();
        return account;
    }

    public synchronized UserAccount authenticate(String loginName, String password) {
        if (!isValidPassword(password)) {
            return null;
        }
        UserAccount account = findByLoginName(loginName).orElse(null);
        if (account == null) {
            return null;
        }
        if (account.getPasswordHash() == null || account.getPasswordHash().trim().isEmpty()) {
            return null;
        }
        if (!account.getPasswordHash().equals(hashPassword(password))) {
            return null;
        }
        account.setLastLoginAt(System.currentTimeMillis());
        saveUsers();
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

    public synchronized Optional<UserAccount> findByUsername(String username) {
        String displayName = UserService.normalizeDisplayName(username);
        String userKey = UserService.toStorageKey(displayName);
        return users.stream()
                .filter(u -> displayName.equalsIgnoreCase(u.getUsername())
                        || userKey.equalsIgnoreCase(u.getUserKey()))
                .findFirst();
    }

    public String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D+", "");
    }

    public boolean isValidPhone(String phone) {
        return normalizePhone(phone).matches("1[3-9]\\d{9}");
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 64;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("maogai-review:" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("密码处理失败", e);
        }
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
