-- =====================================================================
-- vCampus 建库脚本 · 扩展补丁（为「全模块落库」补齐缺失的表与字段）
--
-- 定位：与 sql/vCampus.sql 配合使用，**原脚本保持不动**。
-- 执行顺序：先执行 sql/vCampus.sql 建库建表，再执行本脚本。
--
-- 幂等性说明：
--   · 新建表统一用 CREATE TABLE IF NOT EXISTS，可反复执行；
--   · 补列用 ALTER TABLE ... ADD COLUMN，若该列已存在会报 1060
--     （Duplicate column name），属预期，可忽略继续。
--
-- 主键约定（与 sql/vCampus.sql 一致，务必遵守）：
--   · 主体表一律用 uuid 做主键：tblUser.uUuid、tblStudent.stUuid、tblCourse.coUuid、
--     tblBankAccount.baUuid、tblBook.bIsbn；关系表用业务键做复合主键；
--   · **不要新增 AUTO_INCREMENT 主键**；外键、查询、协议一律走 uuid 列。
--   · 唯一的自增主键是给定表 tblBorrow.rId（既有协议按记录号还书，保留）。
--   · 实体里的 Long m_id 只是内存实现的内部序号：落库时用一列 AUTO_INCREMENT
--     辅助序号回填，它不参与任何对外寻址。
-- =====================================================================

USE vCampus;

-- =====================================================================
-- 一、既有表补列
-- =====================================================================

-- 1. 用户凭证：账号启用状态与姓名快照（UserRepository.Credential 需要）
ALTER TABLE tblUserCredential
  ADD COLUMN ucEnabled TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '账号是否启用' AFTER ucRole,
  ADD COLUMN ucName    VARCHAR(50) NULL COMMENT '姓名快照；与登录名同宽，避免用登录名兜底时超长' AFTER ucEnabled;

-- 2. 学籍：新建贴合代码模型（StudentProfile）的档案表。
--    原 tblStudent 是设计稿的产物、字段与实体已不对应，保留不动，业务侧改用本表。
--    姓名不落库：实体里的 realName 由服务端按 uUuid 联查用户模块填充。
--
--    主键遵循全库约定（tblUser.uUuid / tblStudent.stUuid / tblBankAccount.baUuid 同风格）：
--    一用户一档，uUuid 天然唯一，直接做主键。spId 只是回填实体 Long m_id 的序号，不对外寻址。
CREATE TABLE IF NOT EXISTS tblStudentProfile (
  spId        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 m_id，不对外寻址',
  uUuid       CHAR(36)    NOT NULL COMMENT '所属用户账户 UUID（主键）',
  spCategory  VARCHAR(16) NOT NULL DEFAULT 'STUDENT' COMMENT '人员类别枚举名：STUDENT/TEACHER',
  spStudentNo VARCHAR(16) NULL COMMENT '学号（纯展示，可为空）',
  spJoinYear  INT         NOT NULL DEFAULT 0 COMMENT '入学/入职年份',
  spStatus    VARCHAR(16) NOT NULL DEFAULT 'ENROLLED' COMMENT '在校状态枚举名',
  spField     VARCHAR(40) NULL COMMENT '专业/研究方向',
  spDeleted   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除标记',
  PRIMARY KEY (uUuid),
  UNIQUE KEY ukStudentProfileSeq (spId)
) COMMENT='学籍档案表（对应 StudentProfile）';

-- 3. 馆藏：下架标记（Book.withdrawn 需要）
ALTER TABLE tblBook
  ADD COLUMN bWithdrawn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已下架' AFTER bAvailable;

-- 4. 借阅：用户标识改存 uuid，并补续借次数与罚金（BorrowRecord 需要）
ALTER TABLE tblBorrow
  MODIFY COLUMN uId VARCHAR(64) NOT NULL COMMENT '用户标识（与用户 UUID 一致）',
  ADD COLUMN rRenewalCount      INT           NOT NULL DEFAULT 0 AFTER rReturnedAt,
  ADD COLUMN rFineAmount        DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER rRenewalCount,
  ADD COLUMN rFinePaid          TINYINT(1)    NOT NULL DEFAULT 0 AFTER rFineAmount,
  ADD COLUMN rFineTransactionId VARCHAR(32)   NULL COMMENT '缴罚金对应的银行流水号' AFTER rFinePaid,
  ADD COLUMN rActiveKey VARCHAR(80) GENERATED ALWAYS AS (
      IF(rReturnedAt IS NULL, CONCAT(uId, '|', bIsbn), NULL)) VIRTUAL
      COMMENT '未归还去重键：已归还置空，故历史记录可重复',
  ADD UNIQUE KEY ukBorrowActive (rActiveKey);

-- 5. 银行账户：银行密码与冻结（#60 的能力需要落库）
ALTER TABLE tblBankAccount
  ADD COLUMN baPwdSalt   VARCHAR(64)  NULL COMMENT '银行密码盐' AFTER baState,
  ADD COLUMN baPwdHash   VARCHAR(64)  NULL COMMENT '银行密码哈希 sha256(salt + 密码)' AFTER baPwdSalt,
  ADD COLUMN baPwdSetAt  DATETIME     NULL COMMENT '银行密码最近设置时间' AFTER baPwdHash,
  ADD COLUMN baFrozenAt  DATETIME     NULL COMMENT '挂失时间；非空表示已挂失' AFTER baPwdSetAt;

-- 6. 课程：补授课教师、容量、已选人数（Course 需要）
--    不另加自增主键：tblCourse 已有 coUuid 做主键（全库约定），再加一套等于造出双主键。
ALTER TABLE tblCourse
  ADD COLUMN coTeacherUuid CHAR(36) NULL COMMENT '授课教师用户 UUID' AFTER coName,
  ADD COLUMN coCapacity    INT      NOT NULL DEFAULT 0 COMMENT '容量' AFTER coCredit,
  ADD COLUMN coEnrolled    INT      NOT NULL DEFAULT 0 COMMENT '已选人数' AFTER coCapacity;

-- =====================================================================
-- 二、新增业务表
-- =====================================================================

-- 选课关系表（学生 ↔ 课程）
--   关系表用业务键做复合主键，不再加代主键：同一学生对同一课程只能选一次。
CREATE TABLE IF NOT EXISTS tblCourseSelection (
  uUuid         CHAR(36)    NOT NULL COMMENT '学生用户 UUID',
  coUuid        CHAR(36)    NOT NULL COMMENT '课程 UUID',
  csSemester    VARCHAR(20) NOT NULL DEFAULT '' COMMENT '学期',
  csCreatedAt   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  PRIMARY KEY (uUuid, coUuid),
  KEY idxCourseSelectionCourse (coUuid),
  CONSTRAINT fkCourseSelectionUser FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid),
  CONSTRAINT fkCourseSelectionCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='选课关系表';

-- 成绩表
--   同样用业务键做复合主键：一个学生的一门课一个学期只有一条成绩。
--   scSemester 进主键因此不能为空，未指定学期时存空串。
CREATE TABLE IF NOT EXISTS tblScore (
  uUuid     CHAR(36)    NOT NULL COMMENT '学生用户 UUID',
  coUuid    CHAR(36)    NOT NULL COMMENT '课程 UUID',
  scSemester VARCHAR(20) NOT NULL DEFAULT '' COMMENT '学期',
  scScore   DECIMAL(5,1) NULL COMMENT '总评成绩；空表示未录入',
  scSavedAt DATETIME    NULL COMMENT '最近录入时间',
  PRIMARY KEY (uUuid, coUuid, scSemester),
  KEY idxScoreCourse (coUuid),
  CONSTRAINT fkScoreUser FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid),
  CONSTRAINT fkScoreCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='成绩表';

-- 图书馆读者账户表（LibraryAccount 需要）
--   主键用 uUuid（一用户一账户，与 tblBankAccount.uUuid 同风格）。
--   不再冗余存一个独立账户 UUID：它先前只是自增主键的陪衬，值还等于 uUuid。
CREATE TABLE IF NOT EXISTS tblLibraryAccount (
  laId         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 m_id，不对外寻址',
  uUuid        CHAR(36)    NOT NULL COMMENT '用户 UUID（主键，一用户一账户）',
  laStatus     VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '账户状态：NORMAL/FROZEN/CLOSED',
  laBorrowLimit INT        NOT NULL DEFAULT 5 COMMENT '同时借阅上限',
  laCreatedAt  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
  laUpdatedAt  DATETIME    NULL COMMENT '更新时间',
  laDeleted    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否软删除',
  PRIMARY KEY (uUuid),
  UNIQUE KEY ukLibraryAccountSeq (laId)
) COMMENT='图书馆读者账户表';

-- 图书预约表（BookReservation 需要）
--   主键用 rvUuid；rvId 退为回填实体 Long m_id 的序号列，不再对外寻址。
CREATE TABLE IF NOT EXISTS tblReservation (
  rvUuid      CHAR(36)    NOT NULL COMMENT '预约记录 UUID（主键）',
  rvId        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 m_id',
  uUuid       CHAR(36)    NOT NULL COMMENT '预约用户 UUID',
  bIsbn       VARCHAR(20) NOT NULL COMMENT '图书 ISBN',
  bTitle      VARCHAR(120) NOT NULL COMMENT '书名快照',
  rvRequestedAt DATETIME  NOT NULL COMMENT '预约时间',
  rvReadyAt   DATETIME    NULL COMMENT '可借时间',
  rvExpiresAt DATETIME    NULL COMMENT '保留截止时间',
  rvStatus    VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT '状态：WAITING/READY/FULFILLED/CANCELLED/EXPIRED',
  rvActiveKey VARCHAR(80) GENERATED ALWAYS AS (IF(rvStatus IN ('WAITING', 'READY'),
              CONCAT(uUuid, '|', bIsbn), NULL)) VIRTUAL
              COMMENT '有效预约去重键：仅等待/到馆参与唯一性，完成或取消置空',
  PRIMARY KEY (rvUuid),
  UNIQUE KEY ukReservationSeq (rvId),
  UNIQUE KEY ukReservationActive (rvActiveKey),
  KEY idxReservationUser (uUuid),
  KEY idxReservationBook (bIsbn),
  CONSTRAINT fkReservationBook FOREIGN KEY (bIsbn) REFERENCES tblBook (bIsbn)
) COMMENT='图书预约表';

-- 教室表（选课排课）
CREATE TABLE IF NOT EXISTS tblClassroom (
  crUuid     CHAR(36)    NOT NULL COMMENT '教室 UUID',
  crLocation VARCHAR(80) NOT NULL COMMENT '上课地点',
  crCapacity INT         NOT NULL DEFAULT 0 COMMENT '容纳人数',
  PRIMARY KEY (crUuid),
  UNIQUE KEY ukClassroomLocation (crLocation)
) COMMENT='教室表';

-- 开课班次表（选课排课：一门课的具体班次、教师、教室、学期）
CREATE TABLE IF NOT EXISTS tblCourseSection (
  csecUuid       CHAR(36)    NOT NULL COMMENT '班次 UUID',
  coUuid         CHAR(36)    NOT NULL COMMENT '课程 UUID',
  csecTeacherUuid CHAR(36)   NULL COMMENT '任课教师 UUID',
  csecClassroomUuid CHAR(36) NULL COMMENT '教室 UUID',
  csecSemester   VARCHAR(20) NULL COMMENT '学期',
  csecCapacity   INT         NOT NULL DEFAULT 0 COMMENT '班次容量',
  csecEnrolled   INT         NOT NULL DEFAULT 0 COMMENT '班次已选人数',
  PRIMARY KEY (csecUuid),
  KEY idxCourseSectionCourse (coUuid),
  CONSTRAINT fkCourseSectionCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='开课班次表';

-- 时间槽表（选课：上课时间与教师偏好时间）
--   主键用 tsUuid；时间槽是值对象集合，归属方由 (tsOwnerType, tsOwnerUuid) 索引定位。
CREATE TABLE IF NOT EXISTS tblTimeslot (
  tsUuid      CHAR(36)    NOT NULL COMMENT '时间槽 UUID（主键）',
  tsOwnerType VARCHAR(16) NOT NULL COMMENT '归属类型：SECTION/TEACHER_PREFERENCE/STUDENT_AVAILABLE',
  tsOwnerUuid CHAR(36)    NOT NULL COMMENT '归属对象 UUID',
  tsDayOfWeek TINYINT     NOT NULL COMMENT '星期 1-7',
  tsStartSlot TINYINT     NOT NULL COMMENT '起始节次',
  tsEndSlot   TINYINT     NOT NULL COMMENT '结束节次',
  PRIMARY KEY (tsUuid),
  KEY idxTimeslotOwner (tsOwnerType, tsOwnerUuid)
) COMMENT='时间槽表';

-- 学籍修改申请单（审核流程的流水；对外寻址用 smrUuid）
--   smrId 只是回填实体 requestId（Long）的辅助序号；目标档案沿用实体的 m_profile_id，
--   故这里存 tblStudentProfile 的 spId 序号而非 uuid。
CREATE TABLE IF NOT EXISTS tblStudentModifyRequest (
  smrId        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 requestId，不对外寻址',
  smrUuid      CHAR(36)     NOT NULL COMMENT '申请单 UUID（主键）',
  uUuid        CHAR(36)     NOT NULL COMMENT '申请人账户 UUID',
  smProfileSeq BIGINT       NULL COMMENT '目标学籍档案序号（StudentProfile.m_id）',
  smChanges    VARCHAR(500) NOT NULL COMMENT '变更内容：字段=新值，多项以 ; 分隔',
  smReason     VARCHAR(200) NULL COMMENT '申请理由',
  smStatus     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态枚举名：PENDING/APPROVED/REJECTED',
  smComment    VARCHAR(200) NULL COMMENT '审核意见',
  smAppliedAt  DATETIME     NOT NULL COMMENT '提交时间',
  smAuditedBy  CHAR(36)     NULL COMMENT '审核人账户 UUID；未审核为 NULL',
  smAuditedAt  DATETIME     NULL COMMENT '审核时间；未审核为 NULL',
  PRIMARY KEY (smrUuid),
  UNIQUE KEY ukStudentModifySeq (smrId),
  KEY idxStudentModifyApplicant (uUuid, smAppliedAt),
  KEY idxStudentModifyStatus (smStatus, smAppliedAt),
  KEY idxStudentModifyProfile (smProfileSeq),
  CONSTRAINT fkStudentModifyApplicant FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid)
) COMMENT='学籍修改申请单';

-- =====================================================================
-- 三、商店初始数据（让商店有货可卖）
-- =====================================================================

INSERT INTO tblShop (shopId, shopName, shopDescription, shopOwnerUuid, shopEnabled)
SELECT 'SHOP-CAMPUS-01', '校园便利店', '文具、饮品与校园周边',
       '00000000-0000-0000-0000-000000000003', 1
WHERE NOT EXISTS (SELECT 1 FROM tblShop WHERE shopId = 'SHOP-CAMPUS-01');

INSERT INTO tblShopItem (siUuid, siId, siName, siPrice, siStock, siDesc, siShopId)
SELECT * FROM (
  SELECT UUID(), 'ITM-001', '中性笔（黑）',        2.50, 200, '0.5mm 考试专用', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-002', '中性笔（蓝）',        2.50, 180, '0.5mm 日常记录', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-003', 'A4 笔记本',           9.90, 120, '80 页线圈本', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-004', '牛皮纸档案袋',         3.00, 150, '加厚，可装 200 页', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-005', '订书机',             15.00,  40, '含 1000 枚订书钉', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-006', '计算器',             35.00,  30, '考试可用型号', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-007', 'U 盘 64G',           49.00,  25, 'USB 3.0', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-008', '校园文化衫',          59.00,  50, '棉质，校徽印花', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-009', '帆布袋',             29.00,  60, '加厚帆布，印校徽', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-010', '保温杯',             69.00,  35, '316 不锈钢 500ml', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-011', '雨伞',               39.00,  45, '八骨加固', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-012', '矿泉水 550ml',        1.50, 500, '整箱可议', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-013', '无糖气泡水',          4.50, 300, '青柠味', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-014', '速溶咖啡（10 条）',   19.90, 100, '三合一', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-015', '袋泡茶（20 包）',     15.90,  80, '茉莉花茶', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-016', '巧克力',              12.00, 120, '可可含量 70%', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-017', '面包',                 8.00,  90, '当日生产', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-018', '打印纸 A4（500 张）', 25.00,  70, '70g 复印纸', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-019', '无线鼠标',            69.00,  28, '静音款', 'SHOP-CAMPUS-01' UNION ALL
  SELECT UUID(), 'ITM-020', '鼠标垫',              15.00,  60, '加厚防滑', 'SHOP-CAMPUS-01'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM tblShopItem WHERE siId = 'ITM-001');
