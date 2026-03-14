-- ====================================================================================================
-- Seer Fitness Edu - 定时任务模块建表脚本
-- sys_job: 任务定义（平台级，无 tenant_id）
-- sys_job_log: 执行历史（只追加，无 delete_flag / tenant_id）
-- ====================================================================================================

-- ----------------------------
-- sys_job (任务定义表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job;
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

COMMENT ON TABLE  public.sys_job                  IS '定时任务定义表';
COMMENT ON COLUMN public.sys_job.job_name         IS '任务名称';
COMMENT ON COLUMN public.sys_job.job_group        IS '任务分组';
COMMENT ON COLUMN public.sys_job.handler_name     IS 'Spring Bean 名称';
COMMENT ON COLUMN public.sys_job.cron_expression  IS 'Cron 表达式';
COMMENT ON COLUMN public.sys_job.job_params       IS '任务参数（JSON）';
COMMENT ON COLUMN public.sys_job.status           IS '0=停用 1=启用';

-- ----------------------------
-- sys_job_log (执行历史表)
-- ----------------------------
DROP TABLE IF EXISTS public.sys_job_log;
CREATE TABLE public.sys_job_log (
  id            BIGSERIAL    PRIMARY KEY,
  job_id        BIGINT       NOT NULL,
  job_name      VARCHAR(100) NOT NULL,
  handler_name  VARCHAR(100) NOT NULL,
  trigger_type  SMALLINT     NOT NULL DEFAULT 0,
  start_time    TIMESTAMP    NOT NULL,
  end_time      TIMESTAMP,
  duration_ms   BIGINT,
  status        SMALLINT     NOT NULL DEFAULT 0,
  error_msg     TEXT,
  operator_id   BIGINT
);

COMMENT ON TABLE  public.sys_job_log               IS '定时任务执行历史表';
COMMENT ON COLUMN public.sys_job_log.trigger_type  IS '0=定时触发 1=手动触发';
COMMENT ON COLUMN public.sys_job_log.status        IS '0=失败 1=成功';
COMMENT ON COLUMN public.sys_job_log.operator_id   IS '手动触发时的操作人ID';

-- 索引
CREATE INDEX idx_sys_job_log_job_id    ON public.sys_job_log(job_id);
CREATE INDEX idx_sys_job_log_start_time ON public.sys_job_log(start_time DESC);
