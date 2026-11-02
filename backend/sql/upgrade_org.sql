-- 已有 cp_oa 数据库的增量升级脚本（执行一次）
USE cp_oa;

ALTER TABLE sys_user
    ADD COLUMN feishu_open_id  VARCHAR(64) DEFAULT NULL COMMENT '飞书 open_id' AFTER phone,
    ADD COLUMN feishu_union_id VARCHAR(64) DEFAULT NULL COMMENT '飞书 union_id' AFTER feishu_open_id,
    ADD COLUMN employee_no     VARCHAR(64) DEFAULT NULL COMMENT '工号' AFTER feishu_union_id,
    ADD UNIQUE KEY uk_feishu_open_id (feishu_open_id);

CREATE TABLE IF NOT EXISTS sys_department (
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

CREATE TABLE IF NOT EXISTS sys_user_department (
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
