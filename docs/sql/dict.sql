-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
                               id BIGINT PRIMARY KEY,
                               dict_name VARCHAR(100) NOT NULL,
                               dict_type VARCHAR(100) NOT NULL,
                               dict_description VARCHAR(500),
                               status SMALLINT DEFAULT 1,
                               sort_order INT DEFAULT 0,
                               remark VARCHAR(500),
                               create_by VARCHAR(64),
                               create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               update_by VARCHAR(64),
                               update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               delete_flag SMALLINT DEFAULT 0
);

-- 注释
COMMENT ON TABLE sys_dict_type IS '数据字典类型表';
COMMENT ON COLUMN sys_dict_type.id IS '主键ID';
COMMENT ON COLUMN sys_dict_type.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict_type.dict_type IS '字典类型(唯一标识)';
COMMENT ON COLUMN sys_dict_type.dict_description IS '字典描述';
COMMENT ON COLUMN sys_dict_type.status IS '状态(1:启用 0:禁用)';
COMMENT ON COLUMN sys_dict_type.sort_order IS '排序';
COMMENT ON COLUMN sys_dict_type.remark IS '备注';
COMMENT ON COLUMN sys_dict_type.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_type.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_type.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_type.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_type.delete_flag IS '删除标记(0:正常 1:已删除)';

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
                               id BIGINT PRIMARY KEY,
                               dict_type VARCHAR(100) NOT NULL,
                               dict_label VARCHAR(100) NOT NULL,
                               dict_value VARCHAR(100) NOT NULL,
                               dict_description VARCHAR(500),
                               css_class VARCHAR(100),
                               list_class VARCHAR(100),
                               is_default SMALLINT DEFAULT 0,
                               status SMALLINT DEFAULT 1,
                               sort_order INT DEFAULT 0,
                               remark VARCHAR(500),
                               create_by VARCHAR(64),
                               create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               update_by VARCHAR(64),
                               update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               delete_flag SMALLINT DEFAULT 0
);

-- 注释
COMMENT ON TABLE sys_dict_data IS '数据字典数据表';
COMMENT ON COLUMN sys_dict_data.id IS '主键ID';
COMMENT ON COLUMN sys_dict_data.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_data.dict_label IS '字典标签(显示值)';
COMMENT ON COLUMN sys_dict_data.dict_value IS '字典值(实际值)';
COMMENT ON COLUMN sys_dict_data.dict_description IS '字典项描述';
COMMENT ON COLUMN sys_dict_data.css_class IS '样式属性(CSS类名)';
COMMENT ON COLUMN sys_dict_data.list_class IS '表格样式';
COMMENT ON COLUMN sys_dict_data.is_default IS '是否默认(1:是 0:否)';
COMMENT ON COLUMN sys_dict_data.status IS '状态(1:启用 0:禁用)';
COMMENT ON COLUMN sys_dict_data.sort_order IS '排序';
COMMENT ON COLUMN sys_dict_data.remark IS '备注';
COMMENT ON COLUMN sys_dict_data.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_data.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_data.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_data.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_data.delete_flag IS '删除标记(0:正常 1:已删除)';
