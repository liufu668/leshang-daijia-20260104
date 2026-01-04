package com.example.daijia.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * // SQL → Java
 * VARCHAR(100)     → String
 * INT              → Integer
 * BIGINT           → Long
 * DATETIME         → LocalDateTime  // 推荐
 * DATETIME         → Date          // 传统
 * DECIMAL(10,2)    → BigDecimal
 * TINYINT          → Integer 或 Boolean
 * TEXT             → String
 */
@Data
@Entity
@Table(name = "customer_info")
@JsonIgnoreProperties(ignoreUnknown = true) // 忽视未知字段
public class Customer implements UserDetails{

    /**
     * 乘客ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 添加自增策略
    private Long id;

    /**
     * 微信openId
     */
    private String wxOpenId;

    /**
     * 客户昵称
     */
    private String nickname;

    /**
     * 性别
     */
    private String gender;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 电话
     */
    private String phone;

    /**
     * 1有效，2禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否已经删除
     */
    private Integer isDeleted;

    @JsonIgnore // 序列化时忽略
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return "";
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return nickname;
    }
}
