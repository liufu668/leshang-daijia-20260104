package com.daijia.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户登录记录
 */
@Data
@Entity
@Table(name = "customer_login_log")
public class CustomerLoginLog {

    /**
     * 访问ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 客户ID
     */
    private String customerId;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 登录状态
     */
    private Integer status;

    /**
     * 提示信息
     */
    private String msg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 0:不可用
     * 1:可用
     */
    private Integer isDeleted;
}
