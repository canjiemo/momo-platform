INSERT INTO sys_config (config_key, config_value, config_name, config_type, remark, create_by, create_time, update_by, update_time, delete_flag) VALUES

-- 验证码配置
('security.captcha.enabled',       'true',        '验证码开关',           1, '是否启用图形验证码', 'system', NOW(), 'system', NOW(), 0),
('security.captcha.expire-seconds','300',          '验证码过期时间(秒)',    1, '验证码Redis缓存过期秒数', 'system', NOW(), 'system', NOW(), 0),
('security.captcha.length',        '4',            '验证码位数',           1, '验证码字符个数（4-8）', 'system', NOW(), 'system', NOW(), 0),
('security.captcha.type',          'DIGIT',        '验证码类型',           1, 'DIGIT=纯数字 CHAR=字母 MIXED=混合', 'system', NOW(), 'system', NOW(), 0),

-- 密码策略
('security.password.initial-password',        'Aa123456!', '用户初始密码',             1, '管理员重置/初始化密码时使用的默认值', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.min-length',       '8',         '密码最小长度',             1, '用户密码最少字符数', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.max-length',       '15',        '密码最大长度',             1, '用户密码最多字符数', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-lowercase','true',      '密码需含小写字母',         1, 'true=必须包含 a-z', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-uppercase','true',      '密码需含大写字母',         1, 'true=必须包含 A-Z', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-digit',    'true',      '密码需含数字',             1, 'true=必须包含 0-9', 'system', NOW(), 'system', NOW(), 0),
('security.password.policy.require-special',  'true',      '密码需含特殊字符',         1, 'true=必须包含特殊字符', 'system', NOW(), 'system', NOW(), 0),

-- 账户锁定
('security.account-lock.enabled',                  'true',         '账户锁定开关',           1, '登录失败超限后是否锁定账户', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.attempts.max-fail-count',  '5',            '最大登录失败次数',       1, '超过此次数账户被锁定', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.attempts.auto-reset-hours','24',           '失败次数自动重置(小时)', 1, 'N小时内无失败则重置计数', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.lock-time.base-minutes',   '30',           '基础锁定时长(分钟)',     1, '渐进式锁定的初始锁定分钟数', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.enabled',          'true',         'IP锁定开关',             1, '同一IP失败过多时锁定', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.max-attempts',     '20',           'IP最大失败次数',         1, '超过后锁定该IP', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.lock-minutes',     '60',           'IP锁定时长(分钟)',       1, 'IP被锁定的持续时间', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.ip-lock.record-hours',     '2',            'IP失败记录保留时长(小时)', 1, 'N小时内IP失败次数会被累计', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.reset.on-success',         'true',         '登录成功重置失败记录',   1, 'true=登录成功后清除该用户的失败计数', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.whitelist.users',          'admin,system', '账户白名单',             1, '不受锁定限制的用户名，英文逗号分隔', 'system', NOW(), 'system', NOW(), 0),
('security.account-lock.whitelist.ips',            '127.0.0.1',    'IP白名单',               1, '不受锁定限制的IP，英文逗号分隔', 'system', NOW(), 'system', NOW(), 0),

-- 定时任务
('scheduler.pool-size', '5', '定时任务线程池大小', 1, '修改后重启生效', 'system', NOW(), 'system', NOW(), 0),

-- 文件上传限制
('file.upload.image-max-mb', '10',  '图片上传大小限制(MB)', 1, '图片类文件最大允许上传大小', 'system', NOW(), 'system', NOW(), 0),
('file.upload.video-max-mb', '500', '视频上传大小限制(MB)', 1, '视频类文件最大允许上传大小', 'system', NOW(), 'system', NOW(), 0),
('file.upload.other-max-mb', '100', '其他文件上传大小限制(MB)', 1, '非图片非视频文件最大允许上传大小', 'system', NOW(), 'system', NOW(), 0);
