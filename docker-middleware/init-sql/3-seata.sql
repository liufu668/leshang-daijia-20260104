-- 文件路径: docker-middleware/init-sql/3-seata.sql
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE seata;

-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE IF NOT EXISTS global_table (
    xid varchar(128) NOT NULL COMMENT 'transaction id',
    transaction_id bigint COMMENT 'transaction id',
    status tinyint NOT NULL COMMENT 'transaction status',
    application_id varchar(32) COMMENT 'application id',
    transaction_service_group varchar(32) COMMENT 'transaction service group',
    transaction_name varchar(128) COMMENT 'transaction name',
    timeout int COMMENT 'timeout',
    begin_time bigint COMMENT 'begin time',
    application_data varchar(2000) COMMENT 'application data',
    gmt_create datetime COMMENT 'create time',
    gmt_modified datetime COMMENT 'update time',
    PRIMARY KEY (xid),
    KEY idx_status_gmt_modified (status, gmt_modified),
    KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='global table';

-- the table to store BranchSession data
CREATE TABLE IF NOT EXISTS branch_table (
    branch_id bigint NOT NULL COMMENT 'branch id',
    xid varchar(128) NOT NULL COMMENT 'transaction id',
    transaction_id bigint COMMENT 'transaction id',
    resource_group_id varchar(32) COMMENT 'resource group id',
    resource_id varchar(256) COMMENT 'resource id',
    branch_type varchar(8) COMMENT 'branch type',
    status tinyint COMMENT 'status',
    client_id varchar(64) COMMENT 'client id',
    application_data varchar(2000) COMMENT 'application data',
    gmt_create datetime(6) COMMENT 'create time',
    gmt_modified datetime(6) COMMENT 'update time',
    PRIMARY KEY (branch_id),
    KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='branch table';

-- the table to store lock data
CREATE TABLE IF NOT EXISTS lock_table (
    row_key varchar(128) NOT NULL COMMENT 'row key',
    xid varchar(128) COMMENT 'transaction id',
    transaction_id bigint COMMENT 'transaction id',
    branch_id bigint NOT NULL COMMENT 'branch id',
    resource_id varchar(256) COMMENT 'resource id',
    table_name varchar(32) COMMENT 'table name',
    pk varchar(36) COMMENT 'primary key',
    status tinyint NOT NULL DEFAULT '0' COMMENT 'status 0:normal 1:rollback 2:committing',
    gmt_create datetime COMMENT 'create time',
    gmt_modified datetime COMMENT 'update time',
    PRIMARY KEY (row_key),
    KEY idx_status (status),
    KEY idx_branch_id (branch_id),
    KEY idx_xid_and_branch_id (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='lock table';

-- the table to store distributed_lock
CREATE TABLE IF NOT EXISTS distributed_lock (
    lock_key char(20) NOT NULL COMMENT 'lock key',
    lock_value varchar(20) NOT NULL COMMENT 'lock value',
    expire bigint COMMENT 'expire time',
    PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='distributed_lock';