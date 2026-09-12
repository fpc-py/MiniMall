package com.mall.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlUtilsTest {

    @Test
    @DisplayName("LIKE 通配符被转义，用户输入不会退化成全表匹配")
    void escapeLikeEscapesWildcards() {
        assertThat(SqlUtils.escapeLike("100%")).isEqualTo("100\\%");
        assertThat(SqlUtils.escapeLike("a_b")).isEqualTo("a\\_b");
        assertThat(SqlUtils.escapeLike("a\\b")).isEqualTo("a\\\\b");
        assertThat(SqlUtils.escapeLike("iPhone 15")).isEqualTo("iPhone 15");
        assertThat(SqlUtils.escapeLike(null)).isNull();
        assertThat(SqlUtils.escapeLike("")).isEmpty();
    }

    @Test
    @DisplayName("关键字规范化：去空白、限长、空串归一为 null")
    void normalizeKeyword() {
        assertThat(SqlUtils.normalizeKeyword("  手机  ")).isEqualTo("手机");
        assertThat(SqlUtils.normalizeKeyword("   ")).isNull();
        assertThat(SqlUtils.normalizeKeyword(null)).isNull();
        assertThat(SqlUtils.normalizeKeyword("a".repeat(120))).hasSize(SqlUtils.MAX_KEYWORD_LENGTH);
        assertThat(SqlUtils.normalizeKeyword("%")).isEqualTo("\\%");
    }

    @Test
    @DisplayName("转义后不含裸露的通配符，杜绝 LIKE 注入")
    void escapedKeywordHasNoRawWildcards() {
        String escaped = SqlUtils.normalizeKeyword("a%b_c\\d");
        assertThat(escaped).isEqualTo("a\\%b\\_c\\\\d");
        assertThat(escaped.replace("\\%", "").replace("\\_", "").replace("\\\\", ""))
                .doesNotContain("%")
                .doesNotContain("_");
    }
}
