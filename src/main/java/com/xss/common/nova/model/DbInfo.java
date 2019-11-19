package com.xss.common.nova.model;

import lombok.Data;

@Data
public class DbInfo {
    private String url;
    private String user;
    private String pass;

    public DbInfo(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }
}
