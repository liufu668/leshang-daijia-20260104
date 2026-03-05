

CREATE DATABASE IF NOT EXISTS `daijia_customer`;
USE `daijia_customer`;

-- 删除原表（如果存在的话）
DROP TABLE IF EXISTS `customer_login_log`;
DROP TABLE IF EXISTS `customer_info`;

-- 客户表
CREATE TABLE `customer_info` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `wx_open_id` varchar(200) NOT NULL DEFAULT '' COMMENT '微信openId',
                                 `nickname` varchar(200) DEFAULT '' COMMENT '客户昵称',
                                 `gender` char(1) NOT NULL DEFAULT '1' COMMENT '性别',
                                 `avatar_url` varchar(200) DEFAULT NULL COMMENT '头像',
                                 `phone` char(11) DEFAULT NULL COMMENT '电话',
                                 `status` tinyint(3) DEFAULT '1' COMMENT '1有效，2禁用',
                                 `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:未删除 1:已删除）',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE KEY `uni_open_id` (`wx_open_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 客户登录记录表
CREATE TABLE `customer_login_log` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `customer_id` bigint(20) NOT NULL COMMENT '客户id',
                                      `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
                                      `status` tinyint(1) DEFAULT '0' COMMENT '登录状态(0成功,1失败)',
                                      `msg` varchar(255) DEFAULT '' COMMENT '提示信息',
                                      `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:未删除 1:已删除）',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_customer_id` (`customer_id`),
                                      CONSTRAINT `fk_customer_login_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户登录记录';


#
# Database "daijia_driver"
#

CREATE DATABASE IF NOT EXISTS `daijia_driver`;
USE `daijia_driver`;

#
# Structure for table "driver_account"
#

CREATE TABLE `driver_account` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                  `driver_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '司机id',
                                  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账户总金额',
                                  `lock_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '锁定金额',
                                  `available_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用金额',
                                  `total_income_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总收入',
                                  `total_pay_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总支出',
                                  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE KEY `uni_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机账户';

#
# Data for table "driver_account"
#


#
# Structure for table "driver_account_detail"
#

CREATE TABLE `driver_account_detail` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                         `driver_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '司机id',
                                         `content` varchar(100) NOT NULL DEFAULT '' COMMENT '交易内容',
                                         `trade_type` varchar(10) NOT NULL DEFAULT '' COMMENT '交易类型：1-奖励 2-补贴 3-提现',
                                         `amount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT '金额',
                                         `trade_no` varchar(50) DEFAULT NULL COMMENT '交易编号',
                                         `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         `is_deleted` varchar(2) NOT NULL DEFAULT '0',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机账户明细';

#
# Data for table "driver_account_detail"
#


#
# Structure for table "driver_face_recognition"
#

CREATE TABLE `driver_face_recognition` (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `driver_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '司机id',
                                           `face_date` date DEFAULT NULL COMMENT '识别日期',
                                           `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                                           PRIMARY KEY (`id`) USING BTREE,
                                           KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='司机人脸识别记录表';

#
# Data for table "driver_face_recognition"
#

#
# Structure for table "driver_info"
#

-- 如果表已存在，先删除（生产环境慎用，建议用 ALTER 修改）
-- DROP TABLE IF EXISTS `driver_info`;

CREATE TABLE `driver_info` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `wx_open_id` varchar(200) NOT NULL DEFAULT '' COMMENT '微信openId',
                               `nickname` varchar(200) NOT NULL COMMENT '昵称',
                               `avatar_url` varchar(200) DEFAULT NULL COMMENT '头像',
                               `phone` char(11) DEFAULT NULL COMMENT '电话',
                               `name` varchar(20) DEFAULT NULL COMMENT '姓名',
                               `gender` char(1) NOT NULL DEFAULT '1' COMMENT '性别 1:男 2：女',
                               `birthday` date DEFAULT NULL COMMENT '生日',
                               `idcard_no` varchar(18) DEFAULT NULL COMMENT '身份证号码',
                               `idcard_address` varchar(200) DEFAULT NULL COMMENT '身份证地址',
                               `idcard_expire` date DEFAULT NULL COMMENT '身份证有效期',
                               `idcard_front_url` varchar(200) DEFAULT NULL COMMENT '身份证正面',
                               `idcard_back_url` varchar(200) DEFAULT NULL COMMENT '身份证背面',
                               `idcard_hand_url` varchar(200) DEFAULT NULL COMMENT '手持身份证',
                               `driver_license_clazz` varchar(20) DEFAULT NULL COMMENT '准驾车型',
                               `driver_license_no` varchar(100) DEFAULT NULL COMMENT '驾驶证证件号',
                               `driver_license_expire` date DEFAULT NULL COMMENT '驾驶证有效期',
                               `driver_license_issue_date` date DEFAULT NULL COMMENT '驾驶证初次领证日期',
                               `driver_license_front_url` varchar(200) DEFAULT NULL COMMENT '驾驶证正面',
                               `driver_license_back_url` varchar(200) DEFAULT NULL COMMENT '行驶证副页正面',
                               `driver_license_hand_url` varchar(200) DEFAULT NULL COMMENT '手持驾驶证',
                               `contact_name` varchar(20) DEFAULT NULL COMMENT '紧急联系人',
                               `contact_phone` char(11) DEFAULT NULL COMMENT '紧急联系人电话',
                               `contact_relationship` varchar(20) DEFAULT NULL COMMENT '紧急联系人关系',
                               `face_model_id` varchar(100) DEFAULT NULL COMMENT '腾讯云人脸模型id',
                               `job_no` varchar(50) DEFAULT NULL COMMENT '司机工号',
                               `score` decimal(10,2) NOT NULL DEFAULT '9.00' COMMENT '评分',
                               `order_count` int(11) NOT NULL DEFAULT '0' COMMENT '订单量统计',
                               `auth_status` tinyint(3) NOT NULL DEFAULT '0' COMMENT '认证状态 0:未认证  1：审核中 2：认证通过 -1：认证未通过',
                               `status` tinyint(3) NOT NULL DEFAULT '1' COMMENT '状态，1正常，2禁用',
                               `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                               PRIMARY KEY (`id`) USING BTREE,
                               KEY `idx_wx_open_id` (`wx_open_id`),
                               KEY `idx_phone` (`phone`),
                               KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='司机表';
#
# Data for table "driver_info"
#

#
# Structure for table "driver_login_log"
#

-- 如果表已存在，先删除（生产环境慎用，建议用 ALTER 修改）
-- DROP TABLE IF EXISTS `driver_login_log`;

CREATE TABLE `driver_login_log` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
                                    `driver_id` bigint(20) NOT NULL COMMENT '司机id',
                                    `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
                                    `status` tinyint(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
                                    `msg` varchar(255) DEFAULT '' COMMENT '提示信息',
                                    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                    `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_driver_id` (`driver_id`),
                                    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='司机登录记录';
#
# Data for table "driver_login_log"
#


#
# Structure for table "driver_set"
#

CREATE TABLE `driver_set` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `driver_id` bigint(20) NOT NULL COMMENT '司机ID',
                              `service_status` tinyint(3) NOT NULL DEFAULT '0' COMMENT '服务状态 1：开始接单 0：未接单',
                              `order_distance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单里程设置',
                              `accept_distance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '接单里程设置',
                              `is_auto_accept` tinyint(3) NOT NULL DEFAULT '0' COMMENT '是否自动接单',
                              `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE KEY `uni_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='司机设置表';

#
# Data for table "driver_set"
#

#
# Structure for table "undo_log"
#

CREATE TABLE `undo_log` (
                            `id` bigint(20) NOT NULL AUTO_INCREMENT,
                            `branch_id` bigint(20) NOT NULL,
                            `xid` varchar(100) NOT NULL,
                            `context` varchar(128) NOT NULL,
                            `rollback_info` longblob NOT NULL,
                            `log_status` int(11) NOT NULL,
                            `log_created` datetime NOT NULL,
                            `log_modified` datetime NOT NULL,
                            `ext` varchar(100) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#
# Data for table "undo_log"
#

-- 新建数据库 daijia_order
-- DROP DATABASE `daijia_order`;
CREATE DATABASE IF NOT EXISTS `daijia_order`;
USE `daijia_order`;

CREATE TABLE `order_info` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `customer_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '客户ID',
                              `order_no` varchar(50) NOT NULL DEFAULT '' COMMENT '订单号',
                              `start_location` varchar(200) NOT NULL DEFAULT '' COMMENT '起始地点',
                              `start_point_longitude` decimal(10,7) NOT NULL DEFAULT '0.0000000' COMMENT '起始地点经度',
                              `start_point_latitude` decimal(10,7) NOT NULL DEFAULT '0.0000000' COMMENT '起始地点纬度',  -- 修正
                              `end_location` varchar(200) NOT NULL DEFAULT '' COMMENT '结束地点',
                              `end_point_longitude` decimal(10,7) NOT NULL DEFAULT '0.0000000' COMMENT '结束地点经度',
                              `end_point_latitude` decimal(10,7) NOT NULL DEFAULT '0.0000000' COMMENT '结束地点纬度',    -- 修正
                              `expect_distance` decimal(10,2) DEFAULT NULL COMMENT '预估里程',
                              `real_distance` decimal(10,2) DEFAULT NULL COMMENT '实际里程',
                              `expect_amount` decimal(10,2) DEFAULT NULL COMMENT '预估订单金额',
                              `real_amount` decimal(10,2) DEFAULT NULL COMMENT '实际订单金额',
                              `favour_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '顾客好处费',
                              `driver_id` bigint(20) DEFAULT NULL COMMENT '司机ID',
                              `accept_time` datetime DEFAULT NULL COMMENT '司机接单时间',
                              `arrive_time` datetime DEFAULT NULL COMMENT '司机到达时间',
                              `start_service_time` datetime DEFAULT NULL COMMENT '开始服务时间',
                              `end_service_time` datetime DEFAULT NULL COMMENT '结束服务时间',
                              `pay_time` datetime DEFAULT NULL COMMENT '微信付款时间',
                              `cancel_rule_id` bigint(20) DEFAULT NULL COMMENT '订单取消规则ID',
                              `car_license` varchar(20) NOT NULL DEFAULT '' COMMENT '车牌号',
                              `car_type` varchar(20) NOT NULL DEFAULT '' COMMENT '车型',
                              `car_front_url` varchar(200) DEFAULT NULL COMMENT '司机到达拍照：车前照',
                              `car_back_url` varchar(200) DEFAULT NULL COMMENT '司机到达拍照：车后照',
                              `transaction_id` varchar(50) DEFAULT NULL COMMENT '微信支付订单号',
                              `job_id` bigint(20) DEFAULT NULL,
                              `status` tinyint(3) NOT NULL DEFAULT '1' COMMENT '订单状态：1等待接单，2已接单，3司机已到达，4开始代驾，5结束代驾，6未付款，7已付款，8订单已结束，9顾客撤单，10司机撤单，11事故关闭，12其他',
                              `remark` varchar(200) DEFAULT NULL COMMENT '订单备注信息',
                              `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE KEY `uni_order_no` (`order_no`),
                              KEY `idx_customer_id` (`customer_id`),
                              KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='订单表';


CREATE TABLE `order_status_log` (
                                    `id` bigint(11) NOT NULL AUTO_INCREMENT,
                                    `order_id` bigint(11) DEFAULT NULL,
                                    `order_status` varchar(11) DEFAULT NULL,
                                    `operate_time` datetime DEFAULT NULL,
                                    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单状态日志记录表';

CREATE TABLE `xxl_job_log` (
                               `id` bigint(11) NOT NULL AUTO_INCREMENT,
                               `job_id` bigint(11) NOT NULL DEFAULT '0' COMMENT '任务id',
                               `status` int(11) NOT NULL DEFAULT '1' COMMENT '任务状态    0：失败    1：成功',
                               `error` text COMMENT '失败信息',
                               `times` int(11) NOT NULL DEFAULT '0' COMMENT '耗时(单位：毫秒)',
                               `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                               PRIMARY KEY (`id`),
                               KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 新建数据库 daijia_rule
CREATE DATABASE IF NOT EXISTS `daijia_rule`;
USE `daijia_rule`;

CREATE TABLE `fee_rule` (
                            `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `name` varchar(200) NOT NULL COMMENT '规则名称',
                            `rule` text NOT NULL COMMENT '规则代码',
                            `status` tinyint(3) NOT NULL DEFAULT '1' COMMENT '状态代码，1有效，2关闭',
                            `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                            `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                            PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='代驾费用规则表';


-- 将规则内容插入表中的rule字段，方便运营人员经常修改
INSERT INTO `fee_rule` (`name`, `rule`, `status`, `create_time`, `update_time`, `is_deleted`)
VALUES (
           '代驾费用计算规则',
           '//package对应的不一定是真正的目录，可以任意写com.abc，同一个包下的drl文件可以相互访问
       package  com.daijia

       import com.daijia.model.form.rules.FeeRuleRequest;
       import java.math.BigDecimal;
       import java.math.RoundingMode;

       global com.daijia.model.vo.rules.FeeRuleResponse feeRuleResponse;

       /**
       1.起步价
           00:00:00-06:59:59  19元(含3公里)
           07:00:00-23:59:59  19元(含5公里)
       */
       rule "起步价 00:00:00-07:00:00  19元(含3公里)"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               $rule:FeeRuleRequest(startTime >= "00:00:00" && startTime < "07:00:00")
           then
               //基础里程 3公里
               feeRuleResponse.setBaseDistance(new BigDecimal("3.0"));
               //收费19元 础里程费
               feeRuleResponse.setBaseDistanceFee(new BigDecimal("19.0"));
               //超出里程  超出基础里程的里程
               feeRuleResponse.setExceedDistance(new BigDecimal("0.0"));
               feeRuleResponse.setExceedDistancePrice(new BigDecimal("4.0"));
               System.out.println("00:00:00-07:00:00 " + feeRuleResponse.getBaseDistance() + "公里，起步价:" + feeRuleResponse.getBaseDistanceFee() + "元");
       end

       rule "起步价 07:00:00-23:59:59  19元(含5公里)"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(startTime >= "07:00:00" && startTime <= "23:59:59")
           then
               feeRuleResponse.setBaseDistance(new BigDecimal("5.0"));
               feeRuleResponse.setBaseDistanceFee(new BigDecimal("19.0"));

               //5公里内里程费为0
               feeRuleResponse.setExceedDistance(new BigDecimal("0.0"));
               feeRuleResponse.setExceedDistancePrice(new BigDecimal("3.0"));
               System.out.println("07:00:00-23:59:59 " + feeRuleResponse.getBaseDistance() + "公里，起步价:" + feeRuleResponse.getBaseDistanceFee() + "元");
       end

       /**
       2.里程费
           超出起步里程后开始计算
           00:00:00-06:59:59   4元/1公里
           07:00:00-23:59:59   3元/1公里
       */
       rule "里程费 00:00:00-06:59:59 4元/1公里"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(startTime >= "00:00:00"
                   && startTime <= "06:59:59"
                   && distance > 3.0)
           then
               BigDecimal exceedDistance = $rule.getDistance().subtract(new BigDecimal("3.0"));
               feeRuleResponse.setExceedDistance(exceedDistance);
               feeRuleResponse.setExceedDistancePrice(new BigDecimal("4.0"));
               System.out.println("里程费，超出里程:" + feeRuleResponse.getExceedDistance() + "公里，单价：" + feeRuleResponse.getExceedDistancePrice());
       end
       rule "里程费 07:00:00-23:59:59 3元/1公里"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(startTime >= "07:00:00"
                   && startTime <= "23:59:59"
                   && distance > 5.0)
           then
               BigDecimal exceedDistance = $rule.getDistance().subtract(new BigDecimal("5.0"));
               feeRuleResponse.setExceedDistance(exceedDistance);
               feeRuleResponse.setExceedDistancePrice(new BigDecimal("3.0"));
               System.out.println("里程费，超出里程:" + feeRuleResponse.getExceedDistance() + "公里，单价：" + feeRuleResponse.getExceedDistancePrice());
       end

       /**
       3.等候费
           等候10分钟后  1元/1分钟
       */
       rule "等候费 等候10分钟后 1元/1分钟"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(waitMinute > 10)
           then
               Integer exceedWaitMinute = $rule.getWaitMinute() - 10;
               feeRuleResponse.setBaseWaitMinute(10);
               feeRuleResponse.setExceedWaitMinute(exceedWaitMinute);
               feeRuleResponse.setExceedWaitMinutePrice(new BigDecimal("1.0"));
               System.out.println("等候费，超出分钟:" + feeRuleResponse.getExceedWaitMinute() + "分钟，单价：" + feeRuleResponse.getExceedWaitMinutePrice());
       end
       rule "无等候费"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(waitMinute <= 10)
           then
               feeRuleResponse.setBaseWaitMinute(10);
               feeRuleResponse.setExceedWaitMinute(0);
               feeRuleResponse.setExceedWaitMinutePrice(new BigDecimal("1.0"));
               System.out.println("等候费：无");
       end

       /**
       4.远途费
           订单行程超出12公里后每公里1元
       */
       rule "远途费 订单行程超出12公里后每公里1元"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(distance > 12.0)
           then
               BigDecimal exceedLongDistance = $rule.getDistance().subtract(new BigDecimal("12.0"));
               feeRuleResponse.setBaseLongDistance(new BigDecimal("12.0"));
               feeRuleResponse.setExceedLongDistance(exceedLongDistance);
               feeRuleResponse.setExceedLongDistancePrice(new BigDecimal("1.0"));
               System.out.println("远途费，超出公里:" + feeRuleResponse.getExceedLongDistance() + "公里，单价：" + feeRuleResponse.getExceedLongDistancePrice());
       end
       rule "无远途费"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(distance <= 12.0)
           then
               feeRuleResponse.setBaseLongDistance(new BigDecimal("12.0"));
               feeRuleResponse.setExceedLongDistance(new BigDecimal("0"));
               feeRuleResponse.setExceedLongDistancePrice(new BigDecimal("0"));
               System.out.println("远途费：无");
       end

       /**
       5.计算总金额
           订单总金额 = 基础里程费 + 超出基础里程的费 + 等候费 + 远程费
       */
       rule "计算总金额"
           salience 10          //指定优先级，数值越大优先级越高，不指定的情况下由上到下执行
           no-loop true         //防止陷入死循环
           when
               /*规则条件，到工作内存中查找FeeRuleRequest对象
               里面出来的结果只能是ture或者false
               $rule是绑定变量名，可以任意命名，官方推荐$符号，定义了绑定变量名，可以在then部分操作fact对象*/
               $rule:FeeRuleRequest(distance > 0.0)
           then
               //订单总金额 = 基础里程费 + 超出基础里程的费 + 等候费 + 远程费
               BigDecimal distanceFee = feeRuleResponse.getBaseDistanceFee().add(feeRuleResponse.getExceedDistance().multiply(feeRuleResponse.getExceedDistancePrice()));
               BigDecimal waitFee = new BigDecimal(feeRuleResponse.getExceedWaitMinute()).multiply(feeRuleResponse.getExceedWaitMinutePrice());
               BigDecimal longDistanceFee = feeRuleResponse.getExceedLongDistance().multiply(feeRuleResponse.getExceedLongDistancePrice());

               BigDecimal totalAmount = distanceFee.add(waitFee).add(longDistanceFee);
               feeRuleResponse.setDistanceFee(distanceFee);
               feeRuleResponse.setWaitFee(waitFee);
               feeRuleResponse.setLongDistanceFee(longDistanceFee);
               feeRuleResponse.setTotalAmount(totalAmount);
               System.out.println("计算总金额:" + feeRuleResponse.getTotalAmount() + "元");
       end',
           1,
           CURRENT_TIMESTAMP,
           NULL,
           0
       );

CREATE DATABASE IF NOT EXISTS `daijia_dispatch`;
USE `daijia_dispatch`;

#
# Structure for table "order_job"
#

CREATE TABLE `order_job` (
                             `id` bigint(11) NOT NULL AUTO_INCREMENT,
                             `order_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '订单id',
                             `job_id` bigint(11) NOT NULL DEFAULT '0' COMMENT '任务id',
                             `parameter` text COMMENT '参数',
                             `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uni_order_id` (`order_id`),
                             UNIQUE KEY `uni_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单与任务的关联表';


CREATE TABLE `order_status_log` (
                                    `id` bigint(11) NOT NULL AUTO_INCREMENT,
                                    `order_id` bigint(11) DEFAULT NULL,
                                    `order_status` varchar(11) DEFAULT NULL,
                                    `operate_time` datetime DEFAULT NULL,
                                    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    `is_deleted` tinyint(3) NOT NULL DEFAULT '0',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单状态日志记录表';

#
# Structure for table "xxl_job_log"
#

CREATE TABLE `xxl_job_log` (
                               `id` bigint(11) NOT NULL AUTO_INCREMENT,
                               `job_id` bigint(11) NOT NULL DEFAULT '0' COMMENT '任务id',
                               `status` int(11) NOT NULL DEFAULT '1' COMMENT '任务状态    0：失败    1：成功',
                               `error` text COMMENT '失败信息',
                               `times` int(11) NOT NULL DEFAULT '0' COMMENT '耗时(单位：毫秒)',
                               `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `is_deleted` tinyint(3) NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
                               PRIMARY KEY (`id`),
                               KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

#
# Data for table "xxl_job_log"
#