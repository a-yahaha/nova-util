package com.xss.common.nova.model;

import com.xss.common.nova.annotation.PrimaryKey;
import com.xss.common.nova.annotation.UniqueIndex;
import lombok.Data;

import java.util.Date;

@Data
public class UserEntity {
    @PrimaryKey
    private String id;
    @UniqueIndex
    private String name;
    private Integer age;
    private Date lastModifyTime;
}
