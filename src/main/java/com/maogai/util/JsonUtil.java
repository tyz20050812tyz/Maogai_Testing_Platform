package com.maogai.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static <T> List<T> readList(String filePath, Type type) {
        try (InputStream is = getInputStream(filePath);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("读取JSON文件失败: " + filePath, e);
        }
    }

    public static <T> void writeList(String filePath, List<T> list) {
        String json = gson.toJson(list);
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(json);
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("写入JSON文件失败: " + filePath, e);
        }
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    private static InputStream getInputStream(String path) throws IOException {
        // 优先从类路径加载
        InputStream is = JsonUtil.class.getClassLoader().getResourceAsStream(path);
        if (is != null) return is;
        // 从文件系统加载
        File file = new File(path);
        if (file.exists()) return new FileInputStream(file);
        throw new FileNotFoundException("文件不存在: " + path);
    }
}
