package com.xss.common.nova.model;

import com.xss.common.nova.util.BaseJsonUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

@Data
public class DirInfo {
    private String relativePath;
    private Map<String, DirInfo> childDirs = new HashMap<>(0);
    private List<File> files = new ArrayList<>(0);

    public DirInfo() {
    }

    public DirInfo(String relativePath) {
        this.relativePath = relativePath;
    }

    /**
     * @param path 多层path以File.separator分隔
     */
    public DirInfo createPath(String path) {
        path = StringUtils.isNoneBlank(path) ? path.replaceFirst("^" + File.separator, "") : path;
        path = StringUtils.isNoneBlank(path) ? path.replaceFirst(File.separator + "$", "") : path;
        if (StringUtils.isBlank(path)) {
            return this;
        }

        DirInfo dirInfo;
        int index = path.indexOf(File.separator);
        if (index == -1) {
            if (!childDirs.containsKey(path)) {
                this.getChildDirs().put(path, new DirInfo(relativePath + File.separator + path));
            }
            dirInfo = this.getChildDirs().get(path);
        } else {
            String childPath = path.substring(0, index);
            if (!childDirs.containsKey(childPath)) {
                this.getChildDirs().put(childPath, new DirInfo(relativePath + File.separator + childPath));
            }
            dirInfo = this.getChildDirs().get(childPath).createPath(path.substring(index + File.separator.length()));
        }
        return dirInfo;
    }

    public void createFile(String path, File file) {
        if (StringUtils.isBlank(path) || path.equals(File.separator)) {
            this.getFiles().add(file);
        } else {
            DirInfo dirInfo = this.createPath(path);
            dirInfo.getFiles().add(file);
        }
    }

    public Set<String> getDirs(String path) {
        DirInfo dirInfo = this.getDirInfo(path);
        if (dirInfo != null) {
            return dirInfo.getChildDirs().keySet();
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public List<File> getFiles(String path) {
        DirInfo dirInfo = this.getDirInfo(path);
        if (dirInfo != null) {
            return dirInfo.getFiles();
        } else {
            return Collections.emptyList();
        }
    }

    public String toString() {
        return BaseJsonUtil.writeValue(this);
    }

    private DirInfo getDirInfo(String path) {
        path = StringUtils.isNoneBlank(path) ? path.replaceFirst("^" + File.separator, "") : path;
        path = StringUtils.isNoneBlank(path) ? path.replaceFirst(File.separator + "$", "") : path;
        DirInfo dirInfo = this;
        if (StringUtils.isNotBlank(path)) {
            String fileSep = (File.separator.equals("\\") ? "\\\\" : File.separator);
            for (String crtPath : path.split(fileSep)) {
                dirInfo = dirInfo.getChildDirs().get(crtPath);
            }
        }
        return dirInfo;
    }
}
