package com.xss.common.nova.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

@Slf4j
public class BaseProcessUtils {
    public static String run(String... commands) {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        Process process;
        try {
            process = processBuilder.redirectErrorStream(true).start();
        } catch (Exception e) {
            throw new RuntimeException("启动进程" + Arrays.asList(commands) + "异常", e);
        }

        StringBuilder buf = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("从process中读取数据异常", e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        int errorCode;
        try {
            errorCode = process.waitFor();
        } catch (Throwable e) {
            log.error("获取进程结果异常");
            throw new RuntimeException(e);
        }

        if (errorCode == 0) {
            return buf.toString();
        } else {
            throw new RuntimeException(String.format("process执行异常.错误码[%s],异常信息:%s", errorCode, buf.toString()));
        }
    }
}
