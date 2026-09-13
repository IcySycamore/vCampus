package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;

/** 图书馆协议参数校验；在业务调用前拒绝错误类型、缺失值和非法格式。 */
final class LibraryRequestValidator {
    private LibraryRequestValidator() {
    }

    static String[] search(Object data) throws LibraryException {
        if (!(data instanceof String[])) {
            throw badRequest("搜索参数必须是字符串数组：[关键词, 检索范围]");
        }
        String[] filters = (String[]) data;
        if (filters.length < 1 || filters.length > 2) {
            throw badRequest("搜索参数需包含关键词和可选的检索范围，最多两项");
        }
        String keyword = filters[0] == null ? "" : filters[0].trim();
        if (keyword.length() > 200) {
            throw badRequest("搜索关键词不能超过 200 个字符");
        }
        String field = filters.length == 1 || filters[1] == null ? "all" : filters[1].trim();
        if (field.length() == 0) {
            field = "all";
        }
        if (!"all".equals(field) && !"title".equals(field)
                && !"author".equals(field) && !"category".equals(field)) {
            throw badRequest("检索范围仅支持 all（全部）、title（书名）、author（作者）、category（分类）");
        }
        return new String[] {keyword, field};
    }

    static String isbn(Object data) throws LibraryException {
        if (!(data instanceof String)) {
            throw badRequest("ISBN 必须是字符串，请选择有效图书");
        }
        String isbn = ((String) data).trim();
        if (isbn.length() == 0) {
            throw badRequest("ISBN 不能为空，请选择要借阅的图书");
        }
        String compact = isbn.replace("-", "");
        if (isbn.length() > 25 || !isbn.matches("[0-9Xx]+(-[0-9Xx]+)*")
                || !compact.matches("[0-9]{9}[0-9Xx]|(978|979)[0-9]{10}")) {
            throw badRequest("ISBN 格式不正确：应为 10 位或以 978/979 开头的 13 位，可含连字符");
        }
        // 保留连字符与字母大小写，避免改变现有馆藏的字符串主键；不校验 ISBN 校验位。
        return isbn;
    }

    static long recordId(Object data) throws LibraryException {
        if (data == null) {
            throw badRequest("借阅记录号不能为空，请选择要归还的记录");
        }
        if (!(data instanceof Long) && !(data instanceof Integer)
                && !(data instanceof Short) && !(data instanceof Byte)) {
            throw badRequest("借阅记录号必须是 64 位范围内的整数，不能使用字符串或小数");
        }
        long id = ((Number) data).longValue();
        if (id <= 0) {
            throw badRequest("借阅记录号必须大于 0");
        }
        return id;
    }

    private static LibraryException badRequest(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
