package com.xss.common.nova.util;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class BaseZipUtil {
    public static String zip(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        return new String(zip(content.getBytes(Charsets.UTF_8)), Charsets.ISO_8859_1);
    }

    public static String unzip(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        return new String(unzip(content.getBytes(Charsets.ISO_8859_1)), Charsets.UTF_8);
    }

    public static byte[] zip(byte[] content) {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(1024);
        GZIPOutputStream output = null;
        try {
            output = new GZIPOutputStream(byteOutput);
            output.write(content);
        } catch (Throwable e) {
            throw new RuntimeException("gzip压缩异常", e);
        } finally {
            IOUtils.closeQuietly(output);
        }
        return byteOutput.toByteArray();
    }

    public static byte[] unzip(byte[] content) {
        GZIPInputStream in = null;
        try {
            in = new GZIPInputStream(new ByteArrayInputStream(content));
            return IOUtils.toByteArray(in);
        } catch (Throwable e) {
            throw new RuntimeException("unzip解压缩异常", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
