package com.maogai.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    /**
     * 读取类路径下的资源文件内容
     */
    public static String readResource(String path) throws IOException {
        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("资源文件不存在: " + path);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 读取文件内容
     */
    public static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /**
     * 写入文件
     */
    public static void writeFile(String path, String content) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent());
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取上传文件存储目录
     */
    public static String getUploadDir() {
        String userDir = System.getProperty("user.dir");
        return userDir + File.separator + "upload";
    }

    /**
     * 获取数据文件存储目录
     */
    public static String getDataDir() {
        String classesPath = FileUtil.class.getClassLoader().getResource("").getPath();
        return classesPath + "data" + File.separator;
    }

    /**
     * 获取docx文档目录
     */
    public static String getDocxDir() {
        return getDataDir() + "docs" + File.separator;
    }
}
