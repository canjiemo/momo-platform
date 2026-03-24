-- ====================================================================================================
-- V1 - 初始建表脚本
-- 所有表在 public schema，通过 tenant_id 区分租户数据
-- tenant_id = NULL 表示平台级数据（平台菜单/角色）
-- tenant_id = 具体值 表示租户数据
-- ====================================================================================================
CREATE EXTENSION IF NOT EXISTS vector;
-- ----------------------------
-- sys_tenant (租户主表，平台级)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_tenant (
  id             BIGSERIAL    PRIMARY KEY,
  tenant_code    VARCHAR(50)  NOT NULL UNIQUE,
  tenant_name    VARCHAR(100) NOT NULL,
  real_name      VARCHAR(50),
  contact_phone  VARCHAR(20),
  contact_email  VARCHAR(100),
  address        VARCHAR(200),
  description    TEXT,
  status         SMALLINT     DEFAULT 0,
  activated_at   TIMESTAMP,
  expired_at     TIMESTAMP,
  max_users      INT          DEFAULT 1000,
  delete_flag    SMALLINT     DEFAULT 0,
  created_by     BIGINT,
  create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_by     BIGINT,
  update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_tenant_code   ON public.sys_tenant(tenant_code);
CREATE INDEX IF NOT EXISTS idx_sys_tenant_status ON public.sys_tenant(status, delete_flag);

COMMENT ON TABLE  public.sys_tenant IS '租户表（学校），平台级数据，不参与多租户隔离';
COMMENT ON COLUMN public.sys_tenant.id            IS '租户唯一标识';
COMMENT ON COLUMN public.sys_tenant.tenant_code   IS '租户编码，全局唯一，字母开头，创建后不可修改，用于登录账号前缀';
COMMENT ON COLUMN public.sys_tenant.tenant_name   IS '租户显示名称，通常为学校全称';
COMMENT ON COLUMN public.sys_tenant.real_name     IS '学校联系人真实姓名';
COMMENT ON COLUMN public.sys_tenant.contact_phone IS '学校联系电话';
COMMENT ON COLUMN public.sys_tenant.contact_email IS '学校联系邮箱';
COMMENT ON COLUMN public.sys_tenant.address       IS '学校地址';
COMMENT ON COLUMN public.sys_tenant.description   IS '租户备注描述';
COMMENT ON COLUMN public.sys_tenant.status        IS '租户状态：0=待激活 1=正常 2=已禁用 3=已过期';
COMMENT ON COLUMN public.sys_tenant.activated_at  IS '租户激活时间';
COMMENT ON COLUMN public.sys_tenant.expired_at    IS '租户到期时间，超过此时间后状态自动变为已过期';
COMMENT ON COLUMN public.sys_tenant.max_users     IS '最大用户数量限制';
COMMENT ON COLUMN public.sys_tenant.delete_flag   IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_tenant.created_by    IS '创建人用户ID';
COMMENT ON COLUMN public.sys_tenant.create_time   IS '记录创建时间';
COMMENT ON COLUMN public.sys_tenant.updated_by    IS '最后修改人用户ID';
COMMENT ON COLUMN public.sys_tenant.update_time   IS '记录最后修改时间';

-- ----------------------------
-- sys_user (用户表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_user (
  id          BIGSERIAL    PRIMARY KEY,
  tenant_id   BIGINT,
  username    VARCHAR(50)  NOT NULL,
  password    VARCHAR(255) NOT NULL,
  real_name   VARCHAR(50),
  org_id      BIGINT,
  admin_flag  SMALLINT     DEFAULT 0,
  user_type   SMALLINT     DEFAULT 0,
  status      SMALLINT     DEFAULT 1,
  delete_flag SMALLINT     DEFAULT 0,
  created_by  BIGINT,
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_by  BIGINT,
  update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, username)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_tenant   ON public.sys_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON public.sys_user(username);
CREATE INDEX IF NOT EXISTS idx_sys_user_status   ON public.sys_user(status, delete_flag);

COMMENT ON TABLE  public.sys_user IS '系统用户表，存储平台管理员和各租户下的所有用户';
COMMENT ON COLUMN public.sys_user.id          IS '用户唯一标识';
COMMENT ON COLUMN public.sys_user.tenant_id   IS '所属租户ID，NULL 表示平台级管理员';
COMMENT ON COLUMN public.sys_user.username    IS '登录账号，同一租户内唯一';
COMMENT ON COLUMN public.sys_user.password    IS '登录密码，BCrypt 加密存储';
COMMENT ON COLUMN public.sys_user.real_name   IS '用户真实姓名，用于展示';
COMMENT ON COLUMN public.sys_user.org_id      IS '所属组织架构ID，关联 sys_organization.id';
COMMENT ON COLUMN public.sys_user.admin_flag  IS '超级管理员标识：1=是 0=否，超管自动绕过所有权限检查';
COMMENT ON COLUMN public.sys_user.user_type   IS '用户业务类型：0=运维人员 1=教师 2=学生';
COMMENT ON COLUMN public.sys_user.status      IS '账号状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_user.delete_flag IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_user.created_by  IS '创建人用户ID';
COMMENT ON COLUMN public.sys_user.create_time IS '账号创建时间';
COMMENT ON COLUMN public.sys_user.updated_by  IS '最后修改人用户ID';
COMMENT ON COLUMN public.sys_user.update_time IS '账号最后修改时间';

-- ----------------------------
-- sys_role (角色表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_role (
  id          BIGSERIAL    PRIMARY KEY,
  tenant_id   BIGINT,
  role_name   VARCHAR(50)  NOT NULL,
  role_code   VARCHAR(50)  NOT NULL,
  description VARCHAR(255),
  status      SMALLINT     DEFAULT 1,
  delete_flag SMALLINT     DEFAULT 0,
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_role_tenant ON public.sys_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_status ON public.sys_role(status, delete_flag);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_role_code ON public.sys_role(COALESCE(tenant_id, -1), role_code) WHERE delete_flag = 0;

COMMENT ON TABLE  public.sys_role IS '角色表，tenant_id=NULL 为平台角色模板，租户创建时会复制模板';
COMMENT ON COLUMN public.sys_role.id          IS '角色唯一标识';
COMMENT ON COLUMN public.sys_role.tenant_id   IS '所属租户ID，NULL 表示平台角色模板';
COMMENT ON COLUMN public.sys_role.role_name   IS '角色显示名称，如"教师"、"班主任"';
COMMENT ON COLUMN public.sys_role.role_code   IS '角色编码，同租户内唯一，用于权限判断';
COMMENT ON COLUMN public.sys_role.description IS '角色用途描述';
COMMENT ON COLUMN public.sys_role.status      IS '角色状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_role.delete_flag IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_role.create_time IS '记录创建时间';
COMMENT ON COLUMN public.sys_role.update_time IS '记录最后修改时间';

-- ----------------------------
-- sys_menu (菜单表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_menu (
  id          BIGSERIAL    PRIMARY KEY,
  tenant_id   BIGINT,
  menu_name   VARCHAR(50)  NOT NULL,
  path        VARCHAR(255),
  parent_id   BIGINT       DEFAULT 0,
  type        SMALLINT     DEFAULT 0,
  permission  VARCHAR(100),
  icon        VARCHAR(100),
  sort_order  INT          DEFAULT 0,
  status      SMALLINT     DEFAULT 1,
  delete_flag SMALLINT     DEFAULT 0,
  create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_menu_tenant        ON public.sys_menu(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id     ON public.sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_permission    ON public.sys_menu(permission);
CREATE INDEX IF NOT EXISTS idx_sys_menu_status_delete ON public.sys_menu(status, delete_flag);

COMMENT ON TABLE  public.sys_menu IS '菜单权限表，tenant_id=NULL 为平台/模板菜单，构成导航树和权限体系';
COMMENT ON COLUMN public.sys_menu.id          IS '菜单唯一标识';
COMMENT ON COLUMN public.sys_menu.tenant_id   IS '所属租户ID，NULL 表示平台级菜单或租户模板菜单';
COMMENT ON COLUMN public.sys_menu.menu_name   IS '菜单显示名称';
COMMENT ON COLUMN public.sys_menu.path        IS '前端路由路径，按钮类型为 NULL';
COMMENT ON COLUMN public.sys_menu.parent_id   IS '父节点ID，0 表示顶级节点';
COMMENT ON COLUMN public.sys_menu.type        IS '节点类型：0=目录 1=菜单 2=按钮（权限点）';
COMMENT ON COLUMN public.sys_menu.permission  IS '权限标识符，如 user:create，用于接口和按钮鉴权';
COMMENT ON COLUMN public.sys_menu.icon        IS '前端图标名称（Ant Design Icon 组件名）';
COMMENT ON COLUMN public.sys_menu.sort_order  IS '同级节点排序顺序，数字越小越靠前';
COMMENT ON COLUMN public.sys_menu.status      IS '菜单状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_menu.delete_flag IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_menu.create_time IS '记录创建时间';
COMMENT ON COLUMN public.sys_menu.update_time IS '记录最后修改时间';

-- ----------------------------
-- sys_role_menu (角色菜单关联表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_role_menu (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT,
  role_id     BIGINT    NOT NULL,
  menu_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_role_menu_tenant ON public.sys_role_menu(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_role   ON public.sys_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu   ON public.sys_role_menu(menu_id);

COMMENT ON TABLE  public.sys_role_menu IS '角色与菜单的关联表，确定角色拥有哪些菜单和权限点';
COMMENT ON COLUMN public.sys_role_menu.id          IS '关联记录唯一标识';
COMMENT ON COLUMN public.sys_role_menu.tenant_id   IS '所属租户ID，NULL 表示平台角色模板的菜单关联';
COMMENT ON COLUMN public.sys_role_menu.role_id     IS '角色ID，关联 sys_role.id';
COMMENT ON COLUMN public.sys_role_menu.menu_id     IS '菜单ID，关联 sys_menu.id';
COMMENT ON COLUMN public.sys_role_menu.create_time IS '关联记录创建时间';

-- ----------------------------
-- sys_user_role (用户角色关联表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_user_role (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT,
  user_id     BIGINT    NOT NULL,
  role_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_user_role_tenant ON public.sys_user_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user   ON public.sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role   ON public.sys_user_role(role_id);

COMMENT ON TABLE  public.sys_user_role IS '用户与角色的关联表，一个用户可拥有多个角色';
COMMENT ON COLUMN public.sys_user_role.id          IS '关联记录唯一标识';
COMMENT ON COLUMN public.sys_user_role.tenant_id   IS '所属租户ID';
COMMENT ON COLUMN public.sys_user_role.user_id     IS '用户ID，关联 sys_user.id';
COMMENT ON COLUMN public.sys_user_role.role_id     IS '角色ID，关联 sys_role.id';
COMMENT ON COLUMN public.sys_user_role.create_time IS '关联记录创建时间';

-- ----------------------------
-- sys_tenant_role (租户-平台角色映射表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_tenant_role (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT    NOT NULL,
  role_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_tenant_role_tenant ON public.sys_tenant_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_tenant_role_role   ON public.sys_tenant_role(role_id);

COMMENT ON TABLE  public.sys_tenant_role IS '租户与平台角色模板的映射表，决定租户可使用哪些平台预置角色';
COMMENT ON COLUMN public.sys_tenant_role.id          IS '映射记录唯一标识';
COMMENT ON COLUMN public.sys_tenant_role.tenant_id   IS '租户ID，关联 sys_tenant.id';
COMMENT ON COLUMN public.sys_tenant_role.role_id     IS '平台角色模板ID，关联 sys_role.id（tenant_id=NULL 的角色）';
COMMENT ON COLUMN public.sys_tenant_role.create_time IS '映射记录创建时间';

-- ----------------------------
-- sys_organization (组织架构表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_organization (
  id            BIGSERIAL    PRIMARY KEY,
  tenant_id     BIGINT,
  org_code      VARCHAR(50),
  org_name      VARCHAR(100) NOT NULL,
  parent_id     BIGINT       DEFAULT 0,
  sort_order    INT          DEFAULT 0,
  leader_id     BIGINT,
  contact_phone VARCHAR(20),
  email         VARCHAR(100),
  address       VARCHAR(200),
  description   TEXT,
  status        SMALLINT     DEFAULT 1,
  delete_flag   SMALLINT     DEFAULT 0,
  created_by    BIGINT,
  create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_by    BIGINT,
  update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_org_tenant ON public.sys_organization(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_org_parent ON public.sys_organization(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_org_status ON public.sys_organization(status, delete_flag);

COMMENT ON TABLE  public.sys_organization IS '组织架构表，支持树形结构，用于学校的部门/班级层级管理';

-- ----------------------------
-- sys_operation_log (操作日志表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_operation_log (
  id               BIGSERIAL    PRIMARY KEY,
  tenant_id        BIGINT,
  user_id          BIGINT,
  username         VARCHAR(50),
  real_name        VARCHAR(50),
  operation_type   VARCHAR(20)  NOT NULL,
  module_name      VARCHAR(50)  NOT NULL,
  business_id      VARCHAR(100),
  business_name    VARCHAR(200),
  operation_desc   TEXT,
  request_method   VARCHAR(10),
  request_url      VARCHAR(500),
  request_params   JSONB,
  response_data    JSONB,
  ip_address       VARCHAR(50),
  user_agent       TEXT,
  operation_result SMALLINT     DEFAULT 1,
  error_message    TEXT,
  execution_time   INT          DEFAULT 0,
  create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_op_log_tenant ON public.sys_operation_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_op_log_user   ON public.sys_operation_log(user_id);
CREATE INDEX IF NOT EXISTS idx_op_log_type   ON public.sys_operation_log(operation_type);
CREATE INDEX IF NOT EXISTS idx_op_log_module ON public.sys_operation_log(module_name);
CREATE INDEX IF NOT EXISTS idx_op_log_time   ON public.sys_operation_log(create_time);

COMMENT ON TABLE  public.sys_operation_log IS '系统操作日志表，记录所有用户的关键操作行为，用于审计追溯';

-- ----------------------------
-- sys_dict_type (数据字典类型表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_dict_type (
  id               BIGSERIAL    PRIMARY KEY,
  tenant_id        BIGINT,
  dict_name        VARCHAR(100) NOT NULL,
  dict_type        VARCHAR(100) NOT NULL,
  dict_description VARCHAR(500),
  status           SMALLINT     DEFAULT 1,
  sort_order       INT          DEFAULT 0,
  remark           VARCHAR(500),
  create_by        VARCHAR(64),
  create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_by        VARCHAR(64),
  update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  delete_flag      SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dict_type_tenant ON public.sys_dict_type(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dict_type_type   ON public.sys_dict_type(dict_type);
CREATE INDEX IF NOT EXISTS idx_dict_type_status ON public.sys_dict_type(status, delete_flag);

COMMENT ON TABLE  public.sys_dict_type IS '数据字典类型表，定义字典分组，每个类型下有多条字典数据';

-- ----------------------------
-- sys_dict_data (数据字典数据表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_dict_data (
  id               BIGSERIAL    PRIMARY KEY,
  tenant_id        BIGINT,
  dict_type        VARCHAR(100) NOT NULL,
  dict_label       VARCHAR(100) NOT NULL,
  dict_value       VARCHAR(100) NOT NULL,
  dict_description VARCHAR(500),
  css_class        VARCHAR(100),
  list_class       VARCHAR(100),
  is_default       SMALLINT     DEFAULT 0,
  status           SMALLINT     DEFAULT 1,
  sort_order       INT          DEFAULT 0,
  remark           VARCHAR(500),
  create_by        VARCHAR(64),
  create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_by        VARCHAR(64),
  update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  delete_flag      SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dict_data_tenant ON public.sys_dict_data(tenant_id);
CREATE INDEX IF NOT EXISTS idx_dict_data_type   ON public.sys_dict_data(dict_type);
CREATE INDEX IF NOT EXISTS idx_dict_data_status ON public.sys_dict_data(status, delete_flag);

COMMENT ON TABLE  public.sys_dict_data IS '数据字典数据表，存储每个字典类型下的键值对选项';

-- ----------------------------
-- sys_job (定时任务定义表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_job (
  id              BIGSERIAL     PRIMARY KEY,
  job_name        VARCHAR(100)  NOT NULL,
  job_group       VARCHAR(50)   NOT NULL DEFAULT 'DEFAULT',
  handler_name    VARCHAR(100)  NOT NULL,
  cron_expression VARCHAR(50)   NOT NULL,
  job_params      TEXT,
  status          SMALLINT      NOT NULL DEFAULT 0,
  remark          VARCHAR(255),
  delete_flag     SMALLINT      NOT NULL DEFAULT 0,
  created_by      BIGINT,
  create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by      BIGINT,
  update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  public.sys_job IS '定时任务定义表，配置需要定时执行的后台任务';

-- ----------------------------
-- sys_job_log (定时任务执行历史表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_job_log (
  id           BIGSERIAL    PRIMARY KEY,
  job_id       BIGINT       NOT NULL,
  job_name     VARCHAR(100) NOT NULL,
  handler_name VARCHAR(100) NOT NULL,
  trigger_type SMALLINT     NOT NULL DEFAULT 0,
  start_time   TIMESTAMP    NOT NULL,
  end_time     TIMESTAMP,
  duration_ms  BIGINT,
  status       SMALLINT     NOT NULL DEFAULT 0,
  error_msg    TEXT,
  operator_id  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_sys_job_log_job_id     ON public.sys_job_log(job_id);
CREATE INDEX IF NOT EXISTS idx_sys_job_log_start_time ON public.sys_job_log(start_time DESC);

COMMENT ON TABLE  public.sys_job_log IS '定时任务执行历史表，记录每次任务运行的结果，仅追加不删除';

-- ----------------------------
-- sys_config (系统配置表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_config (
  id           BIGSERIAL    PRIMARY KEY,
  config_key   VARCHAR(100) NOT NULL,
  config_value TEXT,
  config_name  VARCHAR(200) NOT NULL,
  config_type  SMALLINT     NOT NULL DEFAULT 1,
  remark       VARCHAR(500),
  tenant_id    BIGINT,
  create_by    VARCHAR(50),
  create_time  TIMESTAMP,
  update_by    VARCHAR(50),
  update_time  TIMESTAMP,
  delete_flag  SMALLINT     NOT NULL DEFAULT 0,
  CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

COMMENT ON TABLE  public.sys_config IS '系统配置表，存储可动态调整的系统参数，启动时全量预热到 Redis';

-- ----------------------------
-- sys_file_config (文件存储配置表，平台级)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_file_config (
  id           BIGSERIAL    PRIMARY KEY,
  config_name  VARCHAR(100) NOT NULL,
  storage_type VARCHAR(20)  NOT NULL,
  is_active    SMALLINT     NOT NULL DEFAULT 0,
  config       JSONB        NOT NULL,
  remark       VARCHAR(500),
  create_by    VARCHAR(50),
  create_time  TIMESTAMP,
  update_by    VARCHAR(50),
  update_time  TIMESTAMP,
  delete_flag  SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_file_config_type   ON public.sys_file_config(storage_type);
CREATE INDEX IF NOT EXISTS idx_file_config_active ON public.sys_file_config(is_active) WHERE delete_flag = 0;

COMMENT ON TABLE  public.sys_file_config IS '文件存储配置表，平台级，支持本地/MinIO 等多种存储方式热切换';

-- ----------------------------
-- sys_file (文件记录表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS public.sys_file (
  id           BIGSERIAL     PRIMARY KEY,
  file_name    VARCHAR(255)  NOT NULL,
  file_key     VARCHAR(500)  NOT NULL,
  file_url     VARCHAR(1000),
  file_size    BIGINT,
  content_type VARCHAR(100),
  storage_type VARCHAR(20),
  biz_type     VARCHAR(50),
  tenant_id    BIGINT,
  create_by    VARCHAR(50),
  create_time  TIMESTAMP,
  delete_flag  SMALLINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sys_file_tenant ON public.sys_file(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_file_biz    ON public.sys_file(biz_type);

COMMENT ON TABLE  public.sys_file IS '文件上传记录表，记录所有上传文件的元信息';
