package com.xss.common.nova.util;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.xss.common.nova.model.DirInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

@Slf4j
public class BaseFileUtils {
    public static DirInfo getDirInfo(String dir, String fileType) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(dir), "搜索路径不能为空");
        DirInfo dirInfo = new DirInfo("");

        String rootPath = URLDecoder.decode(BaseFileUtils.class.getClassLoader().getResource(dir).getPath(), "UTF-8");
        if (rootPath.contains("!")) { //从jar包中的指定目录下搜索
            JarFile jar = new JarFile(rootPath.substring(rootPath.indexOf(":" + File.separator) + 1, rootPath.indexOf("!")));
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String path = entries.nextElement().getName();
                if (path.contains(dir)) {
                    String relativePath = path.substring(path.indexOf(dir) + dir.length());
                    if (relativePath.endsWith(fileType)) { //文件
                        int index = relativePath.lastIndexOf(File.separator);
                        String relativeDir = relativePath.substring(0, index);
                        dirInfo.createFile(relativeDir, nameToFile(dir + relativePath));
                    } else { //目录
                        dirInfo.createPath(relativePath);
                    }
                }
            }
            jar.close();
        } else {
            handleDir(dirInfo, new File(rootPath), fileType);
        }

        return dirInfo;
    }

    public static String fileToString(String file) {
        return fileToString(file, Charsets.UTF_8);
    }

    public static String fileToString(String file, Charset charset) {
        try {
            return fileToString(nameToFile(file), charset);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String fileToString(File file) {
        return fileToString(file, Charsets.UTF_8);
    }

    public static String fileToString(File file, Charset charset) {
        try {
            if (!file.isFile()) { //如果file为jar包中的文件时
                String separator = "!" + File.separator;
                int index = file.getPath().lastIndexOf(separator);
                index = (index == -1) ? 0 : index + separator.length();
                String path = file.getPath().substring(index);
                List<String> lines = IOUtils.readLines(BaseFileUtils.class.getClassLoader().getResourceAsStream(path), charset);
                return StringUtils.join(lines, System.lineSeparator());
            }
            return Files.toString(file, charset);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void writeToFile(String path, String fileName, String content, boolean append) throws IOException {
        File dir = new File(path);
        FileUtils.forceMkdir(dir);
        File file = new File(dir, fileName);
        if (file.exists() && !append) {
            FileUtils.forceDelete(file);
        }
        FileUtils.touch(file);
        FileUtils.writeByteArrayToFile(file, content.getBytes(Charsets.UTF_8));
    }

    public static boolean writeToFileSave(String path, String fileName, String content, boolean append) {
        try {
            writeToFile(path, fileName, content, append);
            return true;
        } catch (IOException e) {
            log.error("写入文件{}/{}异常:{}", path, fileName, ExceptionUtils.getStackTrace(e));
            return false;
        }
    }

    private static File nameToFile(String name) throws Exception {
        URL url = BaseFileUtils.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new RuntimeException("无法找到" + name + "对应的文件");
        }
        String rootPath = url.getPath();
        return new File(URLDecoder.decode(rootPath, "UTF-8"));
    }

    private static void handleDir(DirInfo dirInfo, File file, String fileType) {
        if (file != null && file.isDirectory() && dirInfo != null && dirInfo.getFiles() != null) {
            dirInfo.getFiles().addAll(Arrays.asList(file.listFiles(fileFilter(fileType))));
            Stream.of(file.listFiles(dirFilter())).forEach(innerDir ->
                    handleDir(dirInfo.createPath(innerDir.getName()), innerDir, fileType));
        }
    }

    private static FilenameFilter fileFilter(String fileType) {
        return (File dir, String name) -> name.contains(fileType);
    }

    private static FilenameFilter dirFilter() {
        return (File dir, String name) -> {
            File file = new File(dir, name);
            return file.isDirectory();
        };
    }
}
