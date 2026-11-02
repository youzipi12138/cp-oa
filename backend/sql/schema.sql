-- CP OA 初始化脚本（MySQL 8+）
CREATE DATABASE IF NOT EXISTS cp_oa
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE cp_oa;

DROP TABLE IF EXISTS sys_user_department;
DROP TABLE IF EXISTS sys_department;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)  NOT NULL COMMENT '登录名',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    real_name       VARCHAR(50)  NOT NULL COMMENT '姓名',
    email           VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    feishu_open_id  VARCHAR(64)           DEFAULT NULL COMMENT '飞书 open_id',
    feishu_union_id VARCHAR(64)           DEFAULT NULL COMMENT '飞书 union_id',
    employee_no     VARCHAR(64)           DEFAULT NULL COMMENT '工号',
    role            VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：admin / user',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_feishu_open_id (feishu_open_id),
    KEY idx_username (username),
    KEY idx_real_name (real_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '系统用户';

CREATE TABLE sys_department (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    feishu_department_id VARCHAR(64)  NOT NULL COMMENT '飞书 open_department_id',
    parent_feishu_id     VARCHAR(64)           DEFAULT NULL COMMENT '飞书父部门 ID，根部门为 0',
    parent_id            BIGINT                DEFAULT NULL COMMENT '本地父部门 ID',
    name                 VARCHAR(100) NOT NULL COMMENT '部门名称',
    leader_open_id       VARCHAR(64)           DEFAULT NULL COMMENT '部门负责人 open_id',
    order_num            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    member_count         INT          NOT NULL DEFAULT 0 COMMENT '部门成员数',
    status               TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用/删除',
    deleted              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_feishu_department_id (feishu_department_id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '部门（同步自飞书）';

CREATE TABLE sys_user_department (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT   NOT NULL COMMENT 'sys_user.id',
    department_id BIGINT   NOT NULL COMMENT 'sys_department.id',
    is_primary    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否主部门',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_department (user_id, department_id),
    KEY idx_department_id (department_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '用户-部门关系';

-- 演示账号：admin / 123456（管理员），zhangsan / 123456（普通用户）
INSERT INTO sys_user (username, password, real_name, email, phone, role, status, deleted)
VALUES ('admin',
        '$2y$10$ip0hnwFVJ8nUx9.74xhAvOXmiLUEIYEac54Q6uuaSj1/mq4X9fyXW',
        '系统管理员',
        'admin@cp-oa.local',
        '13800000000',
        'admin',
        1,
        0),
       ('zhangsan',
        '$2y$10$ip0hnwFVJ8nUx9.74xhAvOXmiLUEIYEac54Q6uuaSj1/mq4X9fyXW',
        '张三',
        'zhangsan@cp-oa.local',
        '13900000000',
        'user',
        1,
        0);
