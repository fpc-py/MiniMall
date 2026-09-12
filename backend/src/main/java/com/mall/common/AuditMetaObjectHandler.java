package com.mall.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.mall.common.security.LoginUser;
import com.mall.common.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 审计字段自动填充：create_by / update_by 取当前登录人用户名。
 * 未登录场景（定时任务、系统初始化）写入 system。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String SYSTEM_OPERATOR = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createBy", String.class, currentOperator());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateBy", String.class, currentOperator());
    }

    private String currentOperator() {
        LoginUser loginUser = SecurityUtils.currentUserOptional().orElse(null);
        if (loginUser == null) {
            return SYSTEM_OPERATOR;
        }
        return loginUser.username() == null ? String.valueOf(loginUser.userId()) : loginUser.username();
    }
}
