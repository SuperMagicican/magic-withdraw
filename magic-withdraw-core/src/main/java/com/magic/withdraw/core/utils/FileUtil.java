package com.magic.withdraw.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 文件工具类
 *
 * @author liguangyuan
 */
public class FileUtil {

    public static String readToStr(String fileName){
        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(fileName);
        ByteArrayOutputStream os = new ByteArrayOutputStream(2048);
        byte[] buffer = new byte[1024];

        String str;
        try {
            int length;
            while((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }

            str = os.toString(StandardCharsets.UTF_8);
        } catch (IOException var5) {
            throw new IllegalArgumentException("无效的密钥", var5);
        }
        return str;
    }


    public static String copyResourceToTempFile(String fileName) {
        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            throw new IllegalArgumentException("资源文件不存在: " + fileName);
        }

        try {
            String suffix = "";
            int idx = fileName.lastIndexOf(".");
            if (idx != -1) {
                suffix = fileName.substring(idx);
            }

            Path tempFile = Files.createTempFile("temp-", suffix);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toAbsolutePath().toString();

        } catch (IOException e) {
            throw new IllegalArgumentException("复制资源文件到临时文件失败: " + fileName, e);
        }
    }

}
