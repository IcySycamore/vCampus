-- =====================================================================
-- vCampus 演示数据：可登录的师生账号 + 一整套能点开看数据的业务行
--   与 vCampus-data.sql 的区别：那个是「测试/演示都需要的最小引用数据」（学院、教室、课程），
--   由 TestSchemaSetup 在测试库里自动加载；本脚本只给开发库用，故意不进测试库——
--   里面的账号会占用测试对这些表的行数断言。
--
--   用法（开发库，先执行 sql/vCampus.sql 建表）：
--     mysql -uroot -p --default-character-set=utf8mb4 vCampus < sql/vCampus-demo.sql
--
--   可登录账号（口令都是 123456，哈希 = sha256(demo1234567890ab + 123456)）：
--     stu01 / 演示学生A（学生）  stu02 / 演示学生B（学生）  tea01 / 演示教师（教师）
--   管理员账号不在这里：它由服务端启动时从 data/admins.tsv 导入（admin / admin123）。
--
--   幂等：先按固定 uuid/主键删掉本脚本自己的行，再插入，可以反复执行。
-- =====================================================================

SET NAMES utf8mb4;

DELETE FROM tblScore WHERE uUuid IN
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a2');
DELETE FROM tblCourseSelection WHERE uUuid IN
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a2');
DELETE FROM tblShopItem WHERE siShopId = 'DEMO-SHOP';
DELETE FROM tblShop WHERE shopId = 'DEMO-SHOP';
DELETE FROM tblLibraryAccount WHERE uUuid IN
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a2');
DELETE FROM tblBankAccount WHERE baUuid = '00000000-0000-0000-0000-0000000000b1';
DELETE FROM tblBook WHERE bIsbn IN ('9787111213826', '9787115428028', '9787121350030');
DELETE FROM tblTeacherField WHERE tcUuid = '00000000-0000-0000-0000-0000000000a3';
DELETE FROM tblTeacher WHERE tcUuid = '00000000-0000-0000-0000-0000000000a3';
DELETE FROM tblStudentProfile WHERE uUuid IN
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a2',
 '00000000-0000-0000-0000-0000000000a3');
DELETE FROM tblUserCredential WHERE ucUsername IN ('stu01', 'stu02', 'tea01');

-- 账号：ucHash = sha256('demo1234567890ab' + '123456')
INSERT INTO tblUserCredential (ucUsername, ucUuid, ucSalt, ucHash, ucRole, ucEnabled, ucName) VALUES
('stu01', '00000000-0000-0000-0000-0000000000a1', 'demo1234567890ab',
 '677d1c122430d3cdca7442a2f854aae9e06d0dd425dc57551d054fc94d7b9a66', '学生', 1, '演示学生A'),
('stu02', '00000000-0000-0000-0000-0000000000a2', 'demo1234567890ab',
 '677d1c122430d3cdca7442a2f854aae9e06d0dd425dc57551d054fc94d7b9a66', '学生', 1, '演示学生B'),
('tea01', '00000000-0000-0000-0000-0000000000a3', 'demo1234567890ab',
 '677d1c122430d3cdca7442a2f854aae9e06d0dd425dc57551d054fc94d7b9a66', '教师', 1, '演示教师');

-- 学籍档案（含教师，spCategory = TEACHER）
INSERT INTO tblStudentProfile (uUuid, spCategory, spStudentNo, spJoinYear, spStatus, spField, spDeleted)
VALUES
('00000000-0000-0000-0000-0000000000a1', 'STUDENT', '20260001', 2026, 'ENROLLED', '软件工程', 0),
('00000000-0000-0000-0000-0000000000a2', 'STUDENT', '20260002', 2026, 'ENROLLED', '软件工程', 0),
('00000000-0000-0000-0000-0000000000a3', 'TEACHER', NULL, 2026, 'ENROLLED', '人工智能', 0);

-- 教师档案：挂到参考数据里的计算机学院（学院是教师档案的非空外键）
INSERT INTO tblTeacher (tcUuid, tcCollegeUuid, tcResearchGroup) VALUES
('00000000-0000-0000-0000-0000000000a3', '00000000-0000-0000-0000-000000000c01', '人工智能实验室');

INSERT INTO tblTeacherField (tcUuid, tfField) VALUES
('00000000-0000-0000-0000-0000000000a3', '人工智能');

-- 选课：stu01 选了 CS101、CS102（课程来自 sql/vCampus-data.sql）
INSERT INTO tblCourseSelection (uUuid, coUuid, csSemester) VALUES
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000c21', '2026-2027-1'),
('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000c22', '2026-2027-1');

-- 成绩：课程按编号 + uuid 双写（表上有指向课程的 uuid 外键）
INSERT INTO tblScore (uUuid, scCourseCode, coUuid, scSemester, scScore, scSavedAt) VALUES
('00000000-0000-0000-0000-0000000000a1', 'CS101', '00000000-0000-0000-0000-000000000c21',
 '2026-2027-1', 92.5, NOW()),
('00000000-0000-0000-0000-0000000000a1', 'CS102', '00000000-0000-0000-0000-000000000c22',
 '2026-2027-1', 85.0, NOW());

-- 图书馆：三个学生各一张读者证 + 三本馆藏
INSERT INTO tblLibraryAccount (uUuid, laStatus, laBorrowLimit, laDeleted) VALUES
('00000000-0000-0000-0000-0000000000a1', 'NORMAL', 5, 0),
('00000000-0000-0000-0000-0000000000a2', 'NORMAL', 5, 0);

INSERT INTO tblBook (bIsbn, bTitle, bAuthor, bCategory, bTotal, bAvailable, bWithdrawn) VALUES
('9787111213826', 'Java 编程思想', 'Bruce Eckel', '计算机', 5, 5, 0),
('9787115428028', '算法导论', 'Cormen', '计算机', 3, 3, 0),
('9787121350030', '计算机网络', '谢希仁', '计算机', 4, 4, 0);

-- 银行：stu01 一个账户，余额 1000.00
INSERT INTO tblBankAccount (baUuid, baId, uUuid, baBalance, baState)
VALUES ('00000000-0000-0000-0000-0000000000b1', 'A-0000000000000001',
 '00000000-0000-0000-0000-0000000000a1', 1000.00, '正常');

-- 商店：店主用演示教师账号（表上有指向账户的外键）
INSERT INTO tblShop (shopId, shopName, shopDescription, shopOwnerUuid, shopEnabled) VALUES
('DEMO-SHOP', '校园便利店', '演示用商店，商品由本脚本写入', '00000000-0000-0000-0000-0000000000a3', 1);

INSERT INTO tblShopItem (siUuid, siId, siName, siPrice, siStock, siDesc, siShopId) VALUES
('00000000-0000-0000-0000-0000000000d1', 'DEMO001', '矿泉水', 2.00, 100, '550ml', 'DEMO-SHOP'),
('00000000-0000-0000-0000-0000000000d2', 'DEMO002', '笔记本', 8.50, 50, 'A5 横线', 'DEMO-SHOP'),
('00000000-0000-0000-0000-0000000000d3', 'DEMO003', '中性笔', 3.00, 80, '0.5mm 黑', 'DEMO-SHOP');
