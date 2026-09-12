package com.mall.common.util;

/**
 * SQL 相关工具：只做参数值转义，绝不参与 SQL 拼接。
 */
public final class SqlUtils {

    /** 关键字最大长度，防止超长入参拖垮 LIKE 查询 */
    public static final int MAX_KEYWORD_LENGTH = 50;

    private SqlUtils() {
    }

    /**
     * 转义 MySQL LIKE 通配符（% _ \），避免用户输入被当作通配符。
     * 转义后的值仍以 JDBC 占位符方式绑定，不存在 SQL 注入风险。
     */
    public static String escapeLike(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** 规范化搜索关键字：去空白 + 截断 + LIKE 转义；空串返回 null 便于条件构造器判空。 */
    public static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            trimmed = trimmed.substring(0, MAX_KEYWORD_LENGTH);
        }
        return escapeLike(trimmed);
    }
}
