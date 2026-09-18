-- =====================================================================
-- vCampus 虚拟校园系统 · 建库脚本
--
-- 库名 vCampus；目标版本 MySQL 8.0（兼容 5.7）。
--
--   本地：  mysql -uroot -proot < sql/vCampus.sql
--   CI：    同上
--
-- 账户的唯一载体是 tblUserCredential(ucUuid)，各模块的用户外键一律指向它。
--
-- 命名与主键约定（全库统一，新增表照此办理）：
--   · 表名 tblXxx。
--   · 主体表用 uuid 做主键：tblUserCredential.ucUuid、tblStudentProfile.uUuid、
--     tblCourse.coUuid、tblBankAccount.baUuid、tblBook.bIsbn；关系表用业务键做复合主键。
--   · 不新增 AUTO_INCREMENT 主键；唯一的自增主键是 tblBorrow.rId（既有协议按记录号还书）。
--   · 实体里的 Long m_id 只是内存侧的内部序号：落库时用一列 AUTO_INCREMENT 辅助序号回填，
--     它不参与任何对外寻址。
--

--
-- 本脚本可从任意状态重跑（见下面的清表段）。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vCampus DEFAULT CHARACTER SET utf8mb4;
USE vCampus;

-- 声明本脚本内容按 utf8mb4 解读。
-- 必须写在脚本里而不是靠调用方加 --default-character-set：Docker 里的 mysql 客户端
-- 在没有 LANG 时会退回 latin1，导入的中文会被二次编码成乱码（实测过），
-- 而同一条命令在 CI 的 runner 上又是对的 —— 这种「看环境决定对错」的坑只能靠脚本自己堵。
SET NAMES utf8mb4;

-- 清表：关掉外键检查，让 DROP 的顺序不再重要。
-- 表之间的依赖会随模块演进而变，靠人工维护「先删子表再删父表」的顺序迟早漏一个。
SET FOREIGN_KEY_CHECKS = 0;

-- 全局序列
DROP TABLE IF EXISTS tblGlobalSequence;
-- 用户与认证
DROP TABLE IF EXISTS tblUserCredential;
-- 学籍
DROP TABLE IF EXISTS tblStudentModifyRequest;
DROP TABLE IF EXISTS tblStudentProfile;
-- 课程与选课
DROP TABLE IF EXISTS tblScore;
DROP TABLE IF EXISTS tblCourseSelection;
DROP TABLE IF EXISTS tblTimeslot;
DROP TABLE IF EXISTS tblCourseStudent;
DROP TABLE IF EXISTS tblCourseField;
DROP TABLE IF EXISTS tblClassroomTag;
DROP TABLE IF EXISTS tblClassroom;
DROP TABLE IF EXISTS tblBuilding;
DROP TABLE IF EXISTS tblTeacherField;
DROP TABLE IF EXISTS tblTeacher;
DROP TABLE IF EXISTS tblCollegeField;
DROP TABLE IF EXISTS tblCollege;
DROP TABLE IF EXISTS tblCourse;
-- 图书馆
DROP TABLE IF EXISTS tblReservation;
DROP TABLE IF EXISTS tblLibraryAccount;
DROP TABLE IF EXISTS tblBorrow;
DROP TABLE IF EXISTS tblBook;
-- 银行
DROP TABLE IF EXISTS tblBankTransaction;
DROP TABLE IF EXISTS tblBankAccount;
-- 商店
DROP TABLE IF EXISTS tblOrder;
DROP TABLE IF EXISTS tblShopItem;
DROP TABLE IF EXISTS tblShop;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 一、全局序列
--   服务端各模块按名字取号（edu.seu.vcampus.server.shop.SequenceDaoImpl）。
-- =====================================================================

CREATE TABLE tblGlobalSequence (
  gsName  VARCHAR(64) NOT NULL COMMENT '序列名',
  gsValue INT         NOT NULL COMMENT '当前值',
  PRIMARY KEY (gsName)
) COMMENT='全局序列表';

-- =====================================================================
-- 二、用户与认证（edu.seu.vcampus.server.user）
--   tblUserCredential 是账户的唯一载体：登录、鉴权、以及各模块的用户外键都认它。
-- =====================================================================

-- 用户凭证表：存储用户认证信息（用户名 → uuid + 盐 + 加盐哈希 + 角色）
CREATE TABLE tblUserCredential (
  ucUsername VARCHAR(50)  NOT NULL COMMENT '用户名（登录标识）',
  ucUuid     CHAR(36)     NOT NULL COMMENT '账户全局唯一标识；全库用户外键一律指它',
  ucSalt     VARCHAR(64)  NOT NULL COMMENT '加密盐',
  ucHash     VARCHAR(64)  NOT NULL COMMENT '加盐哈希 sha256(salt + password)',
  ucRole     VARCHAR(20)  NOT NULL COMMENT '角色：学生/教师/管理员',
  ucEnabled  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '账号是否启用',
  ucName     VARCHAR(50)  NULL COMMENT '姓名快照；与登录名同宽，避免用登录名兜底时超长',
  PRIMARY KEY (ucUsername),
  UNIQUE KEY ukUserCredentialUuid (ucUuid)
) COMMENT='用户凭证表（认证模块）';

-- =====================================================================
-- 三、学籍（edu.seu.vcampus.server.student）
-- =====================================================================

-- 学籍档案表（对应 StudentProfile）
--   一用户一档，uUuid 天然唯一，直接做主键；spId 只是回填实体 Long m_id 的辅助序号。
--   姓名不落库：实体里的 realName 由服务端按 uUuid 联查用户模块填充。
CREATE TABLE tblStudentProfile (
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

-- 学籍修改申请单（审核流程的流水；对外寻址用 smrUuid）
--   smrId 只是回填实体 requestId（Long）的辅助序号；目标档案沿用实体的 m_profile_id，
--   故这里存 tblStudentProfile 的 spId 序号而非 uuid。
CREATE TABLE tblStudentModifyRequest (
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
  CONSTRAINT fkStudentModifyApplicant FOREIGN KEY (uUuid) REFERENCES tblUserCredential (ucUuid)
) COMMENT='学籍修改申请单';

-- =====================================================================
-- 四、课程与选课（edu.seu.vcampus.server.course）
--
--   集合型字段（学院的研究方向/专业、教师的研究方向、课程的选课范围）一律拆成子表，
--   不塞进单列：它们要参与匹配运算，拼成逗号串就只能整串拉回来在内存里比，索引用不上。
--
--   没有「班次」表：实体虽然叫 CourseSection，但一条记录就是一节课的开课信息，
--   直接落在 tblCourse 上（含学期、教室、容量），它的上课时间在 tblTimeslot。
-- =====================================================================

-- 学院（College）
CREATE TABLE tblCollege (
  clgUuid        CHAR(36)     NOT NULL COMMENT '学院 UUID（主键）',
  clgName        VARCHAR(40)  NOT NULL COMMENT '学院名称',
  clgWebsite     VARCHAR(120) NULL COMMENT '学院官网',
  clgDescription VARCHAR(200) NULL COMMENT '学院介绍',
  PRIMARY KEY (clgUuid),
  UNIQUE KEY ukCollegeName (clgName)
) COMMENT='学院';

-- 学院的研究方向与专业（College.researchDirections / College.majors）
CREATE TABLE tblCollegeField (
  clgUuid CHAR(36)    NOT NULL COMMENT '学院 UUID',
  cfKind  VARCHAR(16) NOT NULL COMMENT 'DIRECTION=研究方向 / MAJOR=专业',
  cfField VARCHAR(40) NOT NULL COMMENT '领域名称（common.course.Field.name）',
  PRIMARY KEY (clgUuid, cfKind, cfField),
  CONSTRAINT fkCollegeFieldCollege FOREIGN KEY (clgUuid) REFERENCES tblCollege (clgUuid)
) COMMENT='学院研究方向与专业';

-- 教师（Teacher）：uuid 就是用户账户 uuid
CREATE TABLE tblTeacher (
  tcUuid          CHAR(36)    NOT NULL COMMENT '教师 UUID（= 用户账户 UUID，主键）',
  tcCollegeUuid   CHAR(36)    NOT NULL COMMENT '所属学院 UUID',
  tcResearchGroup VARCHAR(40) NULL COMMENT '研究组',
  PRIMARY KEY (tcUuid),
  KEY idxTeacherCollege (tcCollegeUuid),
  CONSTRAINT fkTeacherCollege FOREIGN KEY (tcCollegeUuid) REFERENCES tblCollege (clgUuid)
) COMMENT='教师';

-- 教师研究方向（Teacher.researchDirections）
CREATE TABLE tblTeacherField (
  tcUuid  CHAR(36)    NOT NULL COMMENT '教师 UUID',
  tfField VARCHAR(40) NOT NULL COMMENT '领域名称（common.course.Field.name）',
  PRIMARY KEY (tcUuid, tfField),
  CONSTRAINT fkTeacherFieldTeacher FOREIGN KEY (tcUuid) REFERENCES tblTeacher (tcUuid)
) COMMENT='教师研究方向';

-- 教室（Classroom）
CREATE TABLE tblBuilding (
  bdUuid        CHAR(36)    NOT NULL COMMENT '教学楼 UUID（主键）',
  bdName        VARCHAR(40) NOT NULL COMMENT '教学楼名称',
  bdCollegeUuid CHAR(36)    NULL COMMENT '所属学院 UUID',
  PRIMARY KEY (bdUuid),
  CONSTRAINT fkBuildingCollege FOREIGN KEY (bdCollegeUuid) REFERENCES tblCollege (clgUuid)
) COMMENT='教学楼';

CREATE TABLE tblClassroom (
  crUuid         CHAR(36)    NOT NULL COMMENT '教室 UUID（主键）',
  crLocation     VARCHAR(80) NOT NULL COMMENT '上课地点（教学楼名）',
  crName         VARCHAR(40) NULL COMMENT '教室号，如 101',
  crBuildingUuid CHAR(36)    NULL COMMENT '所属教学楼 UUID',
  crCapacity     INT         NOT NULL DEFAULT 0 COMMENT '容纳人数',
  crCollegeUuid  CHAR(36)    NULL COMMENT '所属学院 UUID',
  PRIMARY KEY (crUuid),
  UNIQUE KEY ukClassroomLocation (crLocation, crName),
  CONSTRAINT fkClassroomBuilding FOREIGN KEY (crBuildingUuid) REFERENCES tblBuilding (bdUuid)
) COMMENT='教室表';

-- 教室标签（Classroom.tags）
CREATE TABLE tblClassroomTag (
  crUuid    CHAR(36)    NOT NULL COMMENT '教室 UUID',
  ctagLabel VARCHAR(40) NOT NULL COMMENT '标签',
  PRIMARY KEY (crUuid, ctagLabel),
  CONSTRAINT fkClassroomTagClassroom FOREIGN KEY (crUuid) REFERENCES tblClassroom (crUuid)
) COMMENT='教室标签';

-- 课程（实体是 CourseSection，一条记录即一门开课）
CREATE TABLE tblCourse (
  coUuid            CHAR(36)     NOT NULL COMMENT '课程UUID',
  coId              VARCHAR(16)  NOT NULL COMMENT '课程业务ID',
  coCollegeUuid     CHAR(36)     NULL COMMENT '开课学院 UUID',
  coName            VARCHAR(80)  NOT NULL COMMENT '课程名称',
  coTeacherUuid     CHAR(36)     NULL COMMENT '授课教师用户 UUID',
  coCredit          DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '课程学分',
  coCapacity        INT          NOT NULL DEFAULT 0 COMMENT '容量',
  coEnrolled        INT          NOT NULL DEFAULT 0 COMMENT '已选人数',
  coSemester        VARCHAR(20)  NULL COMMENT '学期，如 2026-2027-1',
  coPreferredLocation VARCHAR(80) NULL COMMENT '偏好教学楼',
  coClassroomUuid   CHAR(36)     NULL COMMENT '分配教室 UUID',
  coState           VARCHAR(16)  NOT NULL DEFAULT '开放' COMMENT '课程状态',
  PRIMARY KEY (coUuid),
  UNIQUE KEY ukCourseId (coId)
) COMMENT='选课核心表';

-- 课程的选课范围与教师方向要求（CourseSection.eligibleMajors / requiredDirections）
CREATE TABLE tblCourseField (
  coUuid   CHAR(36)    NOT NULL COMMENT '课程 UUID',
  cfdKind  VARCHAR(24) NOT NULL COMMENT 'ELIGIBLE_MAJOR=可选专业 / REQUIRED_DIRECTION=教师方向要求',
  cfdField VARCHAR(40) NOT NULL COMMENT '领域名称（common.course.Field.name）',
  PRIMARY KEY (coUuid, cfdKind, cfdField),
  CONSTRAINT fkCourseFieldCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='课程可选专业与教师方向要求';

-- 时间槽（Timeslot）：上课时间，以及教师/教室/学生的可用与偏好时间
--   时间槽是值对象集合，归属方由 (tsOwnerType, tsOwnerUuid) 索引定位；
--   SECTION 类型的 tsOwnerUuid 放课程 UUID。
--   tsOwnerType 要装得下 STUDENT_PREFERENCE（18 字符），因此是 VARCHAR(24)。
CREATE TABLE tblTimeslot (
  tsUuid        CHAR(36)    NOT NULL COMMENT '时间槽 UUID（主键）',
  tsOwnerType   VARCHAR(24) NOT NULL COMMENT '归属类型：SECTION/TEACHER_AVAILABLE/TEACHER_PREFERENCE/STUDENT_AVAILABLE/STUDENT_PREFERENCE/CLASSROOM_AVAILABLE/CLASSROOM_PREFERENCE',
  tsOwnerUuid   CHAR(36)    NOT NULL COMMENT '归属对象 UUID',
  tsWeekday     TINYINT     NOT NULL COMMENT '星期（Timeslot.weekday）',
  tsStartMinute SMALLINT    NOT NULL COMMENT '起始分钟（Timeslot.startMinute，自 0 点起算）',
  tsEndMinute   SMALLINT    NOT NULL COMMENT '结束分钟（Timeslot.endMinute）',
  PRIMARY KEY (tsUuid),
  KEY idxTimeslotOwner (tsOwnerType, tsOwnerUuid)
) COMMENT='时间槽表（对应 Timeslot：星期 + 起止分钟）';

-- 选课模块的学生档案（Student）：存的是选课视角的「学院 + 专业」，
-- 与学籍档案 tblStudentProfile 不是一回事。
CREATE TABLE tblCourseStudent (
  uUuid          CHAR(36)    NOT NULL COMMENT '学生 UUID（= 用户账户 UUID，主键）',
  cstCollegeUuid CHAR(36)    NOT NULL COMMENT '所属学院 UUID',
  cstMajor       VARCHAR(40) NOT NULL COMMENT '专业（common.course.Field 的领域名称）',
  PRIMARY KEY (uUuid),
  KEY idxCourseStudentCollege (cstCollegeUuid),
  CONSTRAINT fkCourseStudentCollege FOREIGN KEY (cstCollegeUuid) REFERENCES tblCollege (clgUuid)
) COMMENT='选课模块学生档案';

-- 选课关系（学生 ↔ 课程）：关系表用业务键做复合主键，同一学生对同一课程只能选一次
CREATE TABLE tblCourseSelection (
  uUuid       CHAR(36)    NOT NULL COMMENT '学生用户 UUID',
  coUuid      CHAR(36)    NOT NULL COMMENT '课程 UUID',
  csSemester  VARCHAR(20) NOT NULL DEFAULT '' COMMENT '学期',
  csCreatedAt DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  PRIMARY KEY (uUuid, coUuid),
  KEY idxCourseSelectionCourse (coUuid),
  CONSTRAINT fkCourseSelectionUser FOREIGN KEY (uUuid) REFERENCES tblUserCredential (ucUuid),
  CONSTRAINT fkCourseSelectionCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='选课关系表';

-- 成绩表
--   同样用业务键做复合主键：一个学生的一门课一个学期只有一条成绩。
--   scSemester 进主键因此不能为空，未指定学期时存空串。
--   scId 是回填实体 m_id 的辅助序号；scCourseCode 是课程编号快照，
--   因为实体 Score 引用课程用的是「课程编号」而不是 coUuid。
CREATE TABLE tblScore (
  uUuid        CHAR(36)    NOT NULL COMMENT '学生用户 UUID',
  scId         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 m_id，不对外寻址',
  scCourseCode VARCHAR(16) NULL COMMENT '课程编号快照（实体按 code 而非 coUuid 引用课程）',
  coUuid       CHAR(36)    NOT NULL COMMENT '课程 UUID',
  scSemester   VARCHAR(20) NOT NULL DEFAULT '' COMMENT '学期',
  scScore      DECIMAL(5,1) NULL COMMENT '总评成绩；空表示未录入',
  scSavedAt    DATETIME    NULL COMMENT '最近录入时间',
  PRIMARY KEY (uUuid, coUuid, scSemester),
  UNIQUE KEY ukScoreSeq (scId),
  KEY idxScoreCourse (coUuid),
  CONSTRAINT fkScoreUser FOREIGN KEY (uUuid) REFERENCES tblUserCredential (ucUuid),
  CONSTRAINT fkScoreCourse FOREIGN KEY (coUuid) REFERENCES tblCourse (coUuid)
) COMMENT='成绩表';

-- =====================================================================
-- 五、图书馆（edu.seu.vcampus.server.library）
-- =====================================================================

-- 图书表：馆藏图书信息
CREATE TABLE tblBook (
  bIsbn      VARCHAR(20)  NOT NULL COMMENT '图书ISBN',
  bTitle     VARCHAR(120) NOT NULL COMMENT '书名',
  bAuthor    VARCHAR(80)  NULL COMMENT '作者',
  bCategory  VARCHAR(50)  NULL COMMENT '分类',
  bTotal     INT          NOT NULL DEFAULT 0 COMMENT '馆藏总数',
  bAvailable INT          NOT NULL DEFAULT 0 COMMENT '可借数量',
  bWithdrawn TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已下架',
  PRIMARY KEY (bIsbn),
  KEY idxBookTitle (bTitle),
  KEY idxBookAuthor (bAuthor),
  KEY idxBookCategory (bCategory)
) COMMENT='图书表';

-- 借阅记录表
--   uId 存的是**用户 UUID**，与全库「对外访问基于 uuid」一致。
--   rActiveKey 是生成列：已归还置空，因此同一个学生可以重复借同一本书；未归还时它等于
--   uId|bIsbn，配合唯一索引堵住「同一人同一本书同时存在两条在借记录」。
CREATE TABLE tblBorrow (
  rId          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '借阅记录ID',
  uId          VARCHAR(64)  NOT NULL COMMENT '用户标识（与用户 UUID 一致）',
  bIsbn        VARCHAR(20)  NOT NULL COMMENT '图书ISBN',
  bTitle       VARCHAR(120) NOT NULL COMMENT '书名',
  rBorrowedAt  DATETIME     NOT NULL COMMENT '借出时间',
  rDueAt       DATETIME     NOT NULL COMMENT '应还时间',
  rReturnedAt  DATETIME     NULL COMMENT '实际归还时间',
  rRenewalCount      INT           NOT NULL DEFAULT 0 COMMENT '续借次数',
  rFineAmount        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '罚金金额',
  rFinePaid          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '罚金是否已缴',
  rFineTransactionId VARCHAR(32)   NULL COMMENT '缴罚金对应的银行流水号',
  rActiveKey VARCHAR(80) GENERATED ALWAYS AS (
      IF(rReturnedAt IS NULL, CONCAT(uId, '|', bIsbn), NULL)) VIRTUAL
      COMMENT '未归还去重键：已归还置空，故历史记录可重复',
  PRIMARY KEY (rId),
  KEY idxBorrowUser (uId),
  KEY idxBorrowBook (bIsbn),
  KEY idxBorrowTime (rBorrowedAt),
  UNIQUE KEY ukBorrowActive (rActiveKey),
  CONSTRAINT fkBorrowBook FOREIGN KEY (bIsbn) REFERENCES tblBook (bIsbn)
) COMMENT='借阅记录表';

-- 图书馆读者账户表（LibraryAccount 需要）
--   主键用 uUuid（一用户一账户，与 tblBankAccount.uUuid 同风格）。
CREATE TABLE tblLibraryAccount (
  laId          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增序号；仅用于回填实体 m_id，不对外寻址',
  uUuid         CHAR(36)    NOT NULL COMMENT '用户 UUID（主键，一用户一账户）',
  laStatus      VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '账户状态：NORMAL/FROZEN/CLOSED',
  laBorrowLimit INT         NOT NULL DEFAULT 5 COMMENT '同时借阅上限',
  laCreatedAt   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
  laUpdatedAt   DATETIME    NULL COMMENT '更新时间',
  laDeleted     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否软删除',
  PRIMARY KEY (uUuid),
  UNIQUE KEY ukLibraryAccountSeq (laId)
) COMMENT='图书馆读者账户表';

-- 图书预约表（BookReservation 需要）
--   rvActiveKey 是生成列：只有「等待中/已到馆」参与唯一性，完成或取消后置空，
--   因此同一用户对同一本书可以再次预约。
CREATE TABLE tblReservation (
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

-- =====================================================================
-- 六、银行（edu.seu.vcampus.server.bank）
-- =====================================================================

-- 银行账户表
CREATE TABLE tblBankAccount (
  baUuid      CHAR(36)       NOT NULL COMMENT '银行账户UUID',
  baId        VARCHAR(20)    NOT NULL COMMENT '银行账户业务ID（A- + 16 位随机，见 BankService）',
  uUuid       CHAR(36)       NOT NULL COMMENT '用户UUID',
  baBalance   DECIMAL(12,2)  NOT NULL DEFAULT 0 COMMENT '账户余额',
  baState     VARCHAR(16)    NOT NULL DEFAULT '正常' COMMENT '账户状态',
  baPwdSalt   VARCHAR(64)    NULL COMMENT '银行密码盐（十六进制）',
  baPwdHash   VARCHAR(64)    NULL COMMENT '银行密码哈希 sha256(salt + 密码)（十六进制）',
  baPwdSetAt  DATETIME       NULL COMMENT '银行密码最近设置时间',
  baFrozenAt  DATETIME       NULL COMMENT '挂失时间；非空表示已挂失',
  baCreatedAt DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开户时间',
  baUpdatedAt DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (baUuid),
  UNIQUE KEY ukBankAccountId (baId),
  UNIQUE KEY ukBankAccountUser (uUuid),
  CONSTRAINT fkBankAccountUser FOREIGN KEY (uUuid) REFERENCES tblUserCredential (ucUuid)
) COMMENT='银行账户表';

-- 银行交易流水表
CREATE TABLE tblBankTransaction (
  btId             VARCHAR(32)    NOT NULL COMMENT '流水ID',
  btAccountId      VARCHAR(20)    NOT NULL COMMENT '关联账户ID',
  btType           VARCHAR(20)    NOT NULL COMMENT '交易类型：RECHARGE/CONSUMPTION/CASHBACK',
  btAmount         DECIMAL(12,2)  NOT NULL COMMENT '交易金额',
  btBalanceBefore  DECIMAL(12,2)  NOT NULL COMMENT '交易前余额',
  btBalanceAfter   DECIMAL(12,2)  NOT NULL COMMENT '交易后余额',
  btRelatedOrderId VARCHAR(32)    NULL COMMENT '关联订单ID',
  btDescription    VARCHAR(200)   NULL COMMENT '交易说明',
  btCreatedAt      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
  PRIMARY KEY (btId),
  KEY idxBankTransactionAccount (btAccountId),
  KEY idxBankTransactionType (btType),
  KEY idxBankTransactionTime (btCreatedAt),
  CONSTRAINT fkBankTransactionAccount FOREIGN KEY (btAccountId) REFERENCES tblBankAccount (baId)
) COMMENT='银行交易流水表';

-- =====================================================================
-- 七、商店（edu.seu.vcampus.server.shop）
-- =====================================================================

-- 商店表
CREATE TABLE tblShop (
  shopId          VARCHAR(50)  NOT NULL           COMMENT '商店ID',
  shopName        VARCHAR(100) NOT NULL           COMMENT '商店名称',
  shopDescription TEXT         NULL               COMMENT '商店描述',
  shopOwnerUuid   CHAR(36)     NOT NULL           COMMENT '店主账户 UUID',
  shopEnabled     BOOLEAN      NOT NULL DEFAULT 1 COMMENT '商店是否启用',
  shopCreatedAt   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  shopUpdatedAt   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (shopId),
  KEY idxShopOwner (shopOwnerUuid),
  CONSTRAINT fkShopOwner FOREIGN KEY (shopOwnerUuid) REFERENCES tblUserCredential (ucUuid)
) COMMENT='商店表';

-- 商品表
CREATE TABLE tblShopItem (
  siUuid   CHAR(36)      NOT NULL           COMMENT '商品UUID',
  siId     VARCHAR(16)   NOT NULL           COMMENT '商品业务ID',
  siName   VARCHAR(50)   NOT NULL           COMMENT '商品名称',
  siPrice  DECIMAL(10,2) NOT NULL           COMMENT '单价(元)',
  siStock  INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
  siDesc   VARCHAR(200)  NULL               COMMENT '商品描述',
  siShopId VARCHAR(50)   NULL               COMMENT '所属商店ID',
  PRIMARY KEY (siUuid),
  UNIQUE KEY ukShopItemId (siId),
  KEY idxShopItemShop (siShopId),
  CONSTRAINT fkShopItemShop FOREIGN KEY (siShopId) REFERENCES tblShop (shopId)
) COMMENT='商品表';

-- 订单表：总价由服务端按「单价 × 数量」计算后落库，客户端不参与金额计算
CREATE TABLE tblOrder (
  oId       VARCHAR(32)   NOT NULL                  COMMENT '订单ID',
  oUserUuid CHAR(36)      NOT NULL                  COMMENT '下单用户UUID',
  oItemId   VARCHAR(16)   NOT NULL                  COMMENT '商品ID',
  oShopId   VARCHAR(50)   NULL                      COMMENT '所属商店ID',
  oQuantity INT           NOT NULL                  COMMENT '购买数量',
  oTotal    DECIMAL(10,2) NOT NULL                  COMMENT '订单总价(元)',
  oTime     DATETIME      NOT NULL                  COMMENT '下单时间',
  oStatus   VARCHAR(10)   NOT NULL DEFAULT '待支付' COMMENT '状态：待支付/已支付/已取消',
  PRIMARY KEY (oId),
  KEY idxOrderUser (oUserUuid),
  KEY idxOrderShop (oShopId),
  CONSTRAINT fkOrderUser FOREIGN KEY (oUserUuid) REFERENCES tblUserCredential (ucUuid),
  CONSTRAINT fkOrderItem FOREIGN KEY (oItemId) REFERENCES tblShopItem (siId),
  CONSTRAINT fkOrderShop FOREIGN KEY (oShopId) REFERENCES tblShop (shopId)
) COMMENT='订单表';
