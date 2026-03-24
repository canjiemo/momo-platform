-- ====================================================================================================
-- momo-platform - 统一建表脚本
-- 所有表在 public schema，通过 tenant_id 区分租户数据
-- tenant_id = NULL 表示平台级数据（平台菜单/角色）
-- tenant_id = 具体值 表示租户数据
-- ====================================================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ----------------------------
-- sys_tenant (租户主表，平台级)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant CASCADE;
CREATE TABLE public.sys_tenant (
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

CREATE INDEX idx_sys_tenant_code   ON public.sys_tenant(tenant_code);
CREATE INDEX idx_sys_tenant_status ON public.sys_tenant(status, delete_flag);

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
DROP TABLE IF EXISTS public.sys_user CASCADE;
CREATE TABLE public.sys_user (
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

CREATE INDEX idx_sys_user_tenant   ON public.sys_user(tenant_id);
CREATE INDEX idx_sys_user_username ON public.sys_user(username);
CREATE INDEX idx_sys_user_status   ON public.sys_user(status, delete_flag);

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
DROP TABLE IF EXISTS public.sys_role CASCADE;
CREATE TABLE public.sys_role (
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

CREATE INDEX idx_sys_role_tenant ON public.sys_role(tenant_id);
CREATE INDEX idx_sys_role_status ON public.sys_role(status, delete_flag);
CREATE UNIQUE INDEX idx_sys_role_code ON public.sys_role(COALESCE(tenant_id, -1), role_code) WHERE delete_flag = 0;

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
DROP TABLE IF EXISTS public.sys_menu CASCADE;
CREATE TABLE public.sys_menu (
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

CREATE INDEX idx_sys_menu_tenant      ON public.sys_menu(tenant_id);
CREATE INDEX idx_sys_menu_parent_id   ON public.sys_menu(parent_id);
CREATE INDEX idx_sys_menu_permission  ON public.sys_menu(permission);
CREATE INDEX idx_sys_menu_status_delete ON public.sys_menu(status, delete_flag);

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
DROP TABLE IF EXISTS public.sys_role_menu CASCADE;
CREATE TABLE public.sys_role_menu (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT,
  role_id     BIGINT    NOT NULL,
  menu_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_role_menu_tenant ON public.sys_role_menu(tenant_id);
CREATE INDEX idx_sys_role_menu_role   ON public.sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu   ON public.sys_role_menu(menu_id);

COMMENT ON TABLE  public.sys_role_menu IS '角色与菜单的关联表，确定角色拥有哪些菜单和权限点';
COMMENT ON COLUMN public.sys_role_menu.id          IS '关联记录唯一标识';
COMMENT ON COLUMN public.sys_role_menu.tenant_id   IS '所属租户ID，NULL 表示平台角色模板的菜单关联';
COMMENT ON COLUMN public.sys_role_menu.role_id     IS '角色ID，关联 sys_role.id';
COMMENT ON COLUMN public.sys_role_menu.menu_id     IS '菜单ID，关联 sys_menu.id';
COMMENT ON COLUMN public.sys_role_menu.create_time IS '关联记录创建时间';

-- ----------------------------
-- sys_user_role (用户角色关联表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_user_role CASCADE;
CREATE TABLE public.sys_user_role (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT,
  user_id     BIGINT    NOT NULL,
  role_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_user_role_tenant ON public.sys_user_role(tenant_id);
CREATE INDEX idx_sys_user_role_user   ON public.sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role   ON public.sys_user_role(role_id);

COMMENT ON TABLE  public.sys_user_role IS '用户与角色的关联表，一个用户可拥有多个角色';
COMMENT ON COLUMN public.sys_user_role.id          IS '关联记录唯一标识';
COMMENT ON COLUMN public.sys_user_role.tenant_id   IS '所属租户ID';
COMMENT ON COLUMN public.sys_user_role.user_id     IS '用户ID，关联 sys_user.id';
COMMENT ON COLUMN public.sys_user_role.role_id     IS '角色ID，关联 sys_role.id';
COMMENT ON COLUMN public.sys_user_role.create_time IS '关联记录创建时间';

-- ----------------------------
-- sys_tenant_role (租户-平台角色映射表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_tenant_role CASCADE;
CREATE TABLE public.sys_tenant_role (
  id          BIGSERIAL PRIMARY KEY,
  tenant_id   BIGINT    NOT NULL,
  role_id     BIGINT    NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(tenant_id, role_id)
);

CREATE INDEX idx_sys_tenant_role_tenant ON public.sys_tenant_role(tenant_id);
CREATE INDEX idx_sys_tenant_role_role   ON public.sys_tenant_role(role_id);

COMMENT ON TABLE  public.sys_tenant_role IS '租户与平台角色模板的映射表，决定租户可使用哪些平台预置角色';
COMMENT ON COLUMN public.sys_tenant_role.id          IS '映射记录唯一标识';
COMMENT ON COLUMN public.sys_tenant_role.tenant_id   IS '租户ID，关联 sys_tenant.id';
COMMENT ON COLUMN public.sys_tenant_role.role_id     IS '平台角色模板ID，关联 sys_role.id（tenant_id=NULL 的角色）';
COMMENT ON COLUMN public.sys_tenant_role.create_time IS '映射记录创建时间';

-- ----------------------------
-- sys_organization (组织架构表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_organization CASCADE;
CREATE TABLE public.sys_organization (
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

CREATE INDEX idx_sys_org_tenant ON public.sys_organization(tenant_id);
CREATE INDEX idx_sys_org_parent ON public.sys_organization(parent_id);
CREATE INDEX idx_sys_org_status ON public.sys_organization(status, delete_flag);

COMMENT ON TABLE  public.sys_organization IS '组织架构表，支持树形结构，用于学校的部门/班级层级管理';
COMMENT ON COLUMN public.sys_organization.id            IS '组织节点唯一标识';
COMMENT ON COLUMN public.sys_organization.tenant_id     IS '所属租户ID';
COMMENT ON COLUMN public.sys_organization.org_code      IS '组织编码，租户内唯一';
COMMENT ON COLUMN public.sys_organization.org_name      IS '组织名称，如"高一（3）班"、"体育教研组"';
COMMENT ON COLUMN public.sys_organization.parent_id     IS '父节点ID，0 表示顶级组织';
COMMENT ON COLUMN public.sys_organization.sort_order    IS '同级节点排序顺序';
COMMENT ON COLUMN public.sys_organization.leader_id     IS '负责人用户ID，关联 sys_user.id';
COMMENT ON COLUMN public.sys_organization.contact_phone IS '组织联系电话';
COMMENT ON COLUMN public.sys_organization.email         IS '组织联系邮箱';
COMMENT ON COLUMN public.sys_organization.address       IS '组织地址';
COMMENT ON COLUMN public.sys_organization.description   IS '组织描述备注';
COMMENT ON COLUMN public.sys_organization.status        IS '组织状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_organization.delete_flag   IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_organization.created_by    IS '创建人用户ID';
COMMENT ON COLUMN public.sys_organization.create_time   IS '记录创建时间';
COMMENT ON COLUMN public.sys_organization.updated_by    IS '最后修改人用户ID';
COMMENT ON COLUMN public.sys_organization.update_time   IS '记录最后修改时间';

-- ----------------------------
-- sys_operation_log (操作日志表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_operation_log CASCADE;
CREATE TABLE public.sys_operation_log (
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

CREATE INDEX idx_op_log_tenant ON public.sys_operation_log(tenant_id);
CREATE INDEX idx_op_log_user   ON public.sys_operation_log(user_id);
CREATE INDEX idx_op_log_type   ON public.sys_operation_log(operation_type);
CREATE INDEX idx_op_log_module ON public.sys_operation_log(module_name);
CREATE INDEX idx_op_log_time   ON public.sys_operation_log(create_time);

COMMENT ON TABLE  public.sys_operation_log IS '系统操作日志表，记录所有用户的关键操作行为，用于审计追溯';
COMMENT ON COLUMN public.sys_operation_log.id               IS '日志唯一标识';
COMMENT ON COLUMN public.sys_operation_log.tenant_id        IS '操作人所属租户ID，NULL 表示平台管理员操作';
COMMENT ON COLUMN public.sys_operation_log.user_id          IS '操作人用户ID';
COMMENT ON COLUMN public.sys_operation_log.username         IS '操作人登录账号（冗余存储，防止用户删除后日志丢失信息）';
COMMENT ON COLUMN public.sys_operation_log.real_name        IS '操作人真实姓名（冗余存储）';
COMMENT ON COLUMN public.sys_operation_log.operation_type   IS '操作类型：CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT 等';
COMMENT ON COLUMN public.sys_operation_log.module_name      IS '操作所属业务模块名称，如"用户管理"、"角色管理"';
COMMENT ON COLUMN public.sys_operation_log.business_id      IS '被操作的业务对象ID';
COMMENT ON COLUMN public.sys_operation_log.business_name    IS '被操作的业务对象名称，便于快速识别';
COMMENT ON COLUMN public.sys_operation_log.operation_desc   IS '操作详细描述';
COMMENT ON COLUMN public.sys_operation_log.request_method   IS 'HTTP 请求方法：GET/POST/PUT/DELETE';
COMMENT ON COLUMN public.sys_operation_log.request_url      IS '请求接口 URL';
COMMENT ON COLUMN public.sys_operation_log.request_params   IS '请求参数 JSON，敏感字段（密码等）已脱敏';
COMMENT ON COLUMN public.sys_operation_log.response_data    IS '响应数据 JSON（部分操作记录）';
COMMENT ON COLUMN public.sys_operation_log.ip_address       IS '操作人客户端 IP 地址';
COMMENT ON COLUMN public.sys_operation_log.user_agent       IS '操作人浏览器 User-Agent';
COMMENT ON COLUMN public.sys_operation_log.operation_result IS '操作结果：1=成功 0=失败';
COMMENT ON COLUMN public.sys_operation_log.error_message    IS '操作失败时的错误信息';
COMMENT ON COLUMN public.sys_operation_log.execution_time   IS '接口执行耗时（毫秒）';
COMMENT ON COLUMN public.sys_operation_log.create_time      IS '操作发生时间';

-- ----------------------------
-- sys_dict_type (数据字典类型表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_dict_type CASCADE;
CREATE TABLE public.sys_dict_type (
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

CREATE INDEX idx_dict_type_tenant ON public.sys_dict_type(tenant_id);
CREATE INDEX idx_dict_type_type   ON public.sys_dict_type(dict_type);
CREATE INDEX idx_dict_type_status ON public.sys_dict_type(status, delete_flag);

COMMENT ON TABLE  public.sys_dict_type IS '数据字典类型表，定义字典分组，每个类型下有多条字典数据';
COMMENT ON COLUMN public.sys_dict_type.id               IS '字典类型唯一标识';
COMMENT ON COLUMN public.sys_dict_type.tenant_id        IS '所属租户ID，NULL 表示平台公共字典';
COMMENT ON COLUMN public.sys_dict_type.dict_name        IS '字典类型显示名称，如"用户状态"';
COMMENT ON COLUMN public.sys_dict_type.dict_type        IS '字典类型编码，唯一标识，如 user_status，用于代码中查询';
COMMENT ON COLUMN public.sys_dict_type.dict_description IS '字典类型用途描述';
COMMENT ON COLUMN public.sys_dict_type.status           IS '字典类型状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_dict_type.sort_order       IS '排序顺序';
COMMENT ON COLUMN public.sys_dict_type.remark           IS '备注信息';
COMMENT ON COLUMN public.sys_dict_type.create_by        IS '创建人账号';
COMMENT ON COLUMN public.sys_dict_type.create_time      IS '记录创建时间';
COMMENT ON COLUMN public.sys_dict_type.update_by        IS '最后修改人账号';
COMMENT ON COLUMN public.sys_dict_type.update_time      IS '记录最后修改时间';
COMMENT ON COLUMN public.sys_dict_type.delete_flag      IS '逻辑删除标志：0=正常 1=已删除';

-- ----------------------------
-- sys_dict_data (数据字典数据表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_dict_data CASCADE;
CREATE TABLE public.sys_dict_data (
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

CREATE INDEX idx_dict_data_tenant ON public.sys_dict_data(tenant_id);
CREATE INDEX idx_dict_data_type   ON public.sys_dict_data(dict_type);
CREATE INDEX idx_dict_data_status ON public.sys_dict_data(status, delete_flag);

COMMENT ON TABLE  public.sys_dict_data IS '数据字典数据表，存储每个字典类型下的键值对选项';
COMMENT ON COLUMN public.sys_dict_data.id               IS '字典数据唯一标识';
COMMENT ON COLUMN public.sys_dict_data.tenant_id        IS '所属租户ID，NULL 表示平台公共字典数据';
COMMENT ON COLUMN public.sys_dict_data.dict_type        IS '所属字典类型编码，关联 sys_dict_type.dict_type';
COMMENT ON COLUMN public.sys_dict_data.dict_label       IS '字典选项显示标签，如"启用"、"禁用"';
COMMENT ON COLUMN public.sys_dict_data.dict_value       IS '字典选项存储值，如"1"、"0"，代码中存此值';
COMMENT ON COLUMN public.sys_dict_data.dict_description IS '该选项的含义描述';
COMMENT ON COLUMN public.sys_dict_data.css_class        IS '前端样式类名（用于标签颜色等）';
COMMENT ON COLUMN public.sys_dict_data.list_class       IS '列表展示样式类名';
COMMENT ON COLUMN public.sys_dict_data.is_default       IS '是否为默认选项：1=是 0=否';
COMMENT ON COLUMN public.sys_dict_data.status           IS '字典数据状态：1=启用 0=禁用';
COMMENT ON COLUMN public.sys_dict_data.sort_order       IS '同类型下选项排序顺序';
COMMENT ON COLUMN public.sys_dict_data.remark           IS '备注信息';
COMMENT ON COLUMN public.sys_dict_data.create_by        IS '创建人账号';
COMMENT ON COLUMN public.sys_dict_data.create_time      IS '记录创建时间';
COMMENT ON COLUMN public.sys_dict_data.update_by        IS '最后修改人账号';
COMMENT ON COLUMN public.sys_dict_data.update_time      IS '记录最后修改时间';
COMMENT ON COLUMN public.sys_dict_data.delete_flag      IS '逻辑删除标志：0=正常 1=已删除';

-- ----------------------------
-- sys_job (定时任务定义表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job CASCADE;
CREATE TABLE public.sys_job (
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
COMMENT ON COLUMN public.sys_job.id              IS '任务唯一标识';
COMMENT ON COLUMN public.sys_job.job_name        IS '任务名称，便于识别任务用途';
COMMENT ON COLUMN public.sys_job.job_group       IS '任务分组，默认 DEFAULT，用于任务归类管理';
COMMENT ON COLUMN public.sys_job.handler_name    IS '执行器 Spring Bean 名称，对应实现了任务接口的 Bean';
COMMENT ON COLUMN public.sys_job.cron_expression IS 'Cron 表达式，定义任务执行周期，如 0 0 2 * * ?';
COMMENT ON COLUMN public.sys_job.job_params      IS '任务执行参数（JSON 字符串），传递给执行器';
COMMENT ON COLUMN public.sys_job.status          IS '任务状态：0=停用 1=启用';
COMMENT ON COLUMN public.sys_job.remark          IS '任务备注说明';
COMMENT ON COLUMN public.sys_job.delete_flag     IS '逻辑删除标志：0=正常 1=已删除';
COMMENT ON COLUMN public.sys_job.created_by      IS '创建人用户ID';
COMMENT ON COLUMN public.sys_job.create_time     IS '记录创建时间';
COMMENT ON COLUMN public.sys_job.updated_by      IS '最后修改人用户ID';
COMMENT ON COLUMN public.sys_job.update_time     IS '记录最后修改时间';

-- ----------------------------
-- sys_job_log (定时任务执行历史表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job_log CASCADE;
CREATE TABLE public.sys_job_log (
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

CREATE INDEX idx_sys_job_log_job_id     ON public.sys_job_log(job_id);
CREATE INDEX idx_sys_job_log_start_time ON public.sys_job_log(start_time DESC);

COMMENT ON TABLE  public.sys_job_log IS '定时任务执行历史表，记录每次任务运行的结果，仅追加不删除';
COMMENT ON COLUMN public.sys_job_log.id           IS '执行记录唯一标识';
COMMENT ON COLUMN public.sys_job_log.job_id       IS '任务ID，关联 sys_job.id';
COMMENT ON COLUMN public.sys_job_log.job_name     IS '任务名称（冗余存储）';
COMMENT ON COLUMN public.sys_job_log.handler_name IS '执行器 Bean 名称（冗余存储）';
COMMENT ON COLUMN public.sys_job_log.trigger_type IS '触发方式：0=定时自动触发 1=管理员手动触发';
COMMENT ON COLUMN public.sys_job_log.start_time   IS '任务开始执行时间';
COMMENT ON COLUMN public.sys_job_log.end_time     IS '任务执行结束时间';
COMMENT ON COLUMN public.sys_job_log.duration_ms  IS '任务执行耗时（毫秒）';
COMMENT ON COLUMN public.sys_job_log.status       IS '执行结果：0=失败 1=成功';
COMMENT ON COLUMN public.sys_job_log.error_msg    IS '执行失败时的错误信息和堆栈';
COMMENT ON COLUMN public.sys_job_log.operator_id  IS '手动触发时的操作人用户ID';

-- ----------------------------
-- sys_config (系统配置表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_config CASCADE;
CREATE TABLE public.sys_config (
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
COMMENT ON COLUMN public.sys_config.id           IS '配置项唯一标识';
COMMENT ON COLUMN public.sys_config.config_key   IS '配置项键名，全局唯一，如 security.captcha.enabled';
COMMENT ON COLUMN public.sys_config.config_value IS '配置项值，统一以字符串存储';
COMMENT ON COLUMN public.sys_config.config_name  IS '配置项中文名称，便于管理界面展示';
COMMENT ON COLUMN public.sys_config.config_type  IS '配置类型：1=系统内置（不可删除）2=用户自定义';
COMMENT ON COLUMN public.sys_config.remark       IS '配置项用途说明';
COMMENT ON COLUMN public.sys_config.tenant_id    IS '所属租户ID，NULL 表示平台全局配置';
COMMENT ON COLUMN public.sys_config.create_by    IS '创建人账号';
COMMENT ON COLUMN public.sys_config.create_time  IS '记录创建时间';
COMMENT ON COLUMN public.sys_config.update_by    IS '最后修改人账号';
COMMENT ON COLUMN public.sys_config.update_time  IS '记录最后修改时间';
COMMENT ON COLUMN public.sys_config.delete_flag  IS '逻辑删除标志：0=正常 1=已删除';

-- ----------------------------
-- sys_file_config (文件存储配置表，平台级)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_file_config CASCADE;
CREATE TABLE public.sys_file_config (
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

CREATE INDEX idx_file_config_type   ON public.sys_file_config(storage_type);
CREATE INDEX idx_file_config_active ON public.sys_file_config(is_active) WHERE delete_flag = 0;

COMMENT ON TABLE  public.sys_file_config IS '文件存储配置表，平台级，支持本地/MinIO 等多种存储方式热切换';
COMMENT ON COLUMN public.sys_file_config.id           IS '配置唯一标识';
COMMENT ON COLUMN public.sys_file_config.config_name  IS '配置名称，便于区分多套配置';
COMMENT ON COLUMN public.sys_file_config.storage_type IS '存储类型：local=本地磁盘 minio=MinIO 对象存储';
COMMENT ON COLUMN public.sys_file_config.is_active    IS '是否为当前激活配置：1=激活 0=未激活，同一时刻只有一条激活';
COMMENT ON COLUMN public.sys_file_config.config       IS '存储配置 JSON，结构随 storage_type 变化，含地址/密钥等';
COMMENT ON COLUMN public.sys_file_config.remark       IS '配置备注说明';
COMMENT ON COLUMN public.sys_file_config.create_by    IS '创建人账号';
COMMENT ON COLUMN public.sys_file_config.create_time  IS '记录创建时间';
COMMENT ON COLUMN public.sys_file_config.update_by    IS '最后修改人账号';
COMMENT ON COLUMN public.sys_file_config.update_time  IS '记录最后修改时间';
COMMENT ON COLUMN public.sys_file_config.delete_flag  IS '逻辑删除标志：0=正常 1=已删除';

-- ----------------------------
-- sys_file (文件记录表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_file CASCADE;
CREATE TABLE public.sys_file (
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

CREATE INDEX idx_sys_file_tenant ON public.sys_file(tenant_id);
CREATE INDEX idx_sys_file_biz    ON public.sys_file(biz_type);

COMMENT ON TABLE  public.sys_file IS '文件上传记录表，记录所有上传文件的元信息，实际文件存储在 sys_file_config 指定的存储系统中';
COMMENT ON COLUMN public.sys_file.id           IS '文件记录唯一标识';
COMMENT ON COLUMN public.sys_file.file_name    IS '原始文件名（用户上传时的文件名）';
COMMENT ON COLUMN public.sys_file.file_key     IS '文件在存储系统中的唯一路径/Key，用于访问和删除';
COMMENT ON COLUMN public.sys_file.file_url     IS '文件访问 URL（公开 URL 或预签名 URL）';
COMMENT ON COLUMN public.sys_file.file_size    IS '文件大小（字节）';
COMMENT ON COLUMN public.sys_file.content_type IS '文件 MIME 类型，如 image/jpeg、video/mp4';
COMMENT ON COLUMN public.sys_file.storage_type IS '实际存储类型：local/minio，对应上传时激活的存储配置';
COMMENT ON COLUMN public.sys_file.biz_type     IS '业务归属类型，标识文件用途，如 avatar（头像）、course（课程素材）';
COMMENT ON COLUMN public.sys_file.tenant_id    IS '上传人所属租户ID';
COMMENT ON COLUMN public.sys_file.create_by    IS '上传人账号';
COMMENT ON COLUMN public.sys_file.create_time  IS '文件上传时间';
COMMENT ON COLUMN public.sys_file.delete_flag  IS '逻辑删除标志：0=正常 1=已删除（文件本体需另行从存储系统删除）';

-- AI 智能查询模块表结构见 003_ai_schema.sql
