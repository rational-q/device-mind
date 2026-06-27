package com.devicemind.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.devicemind.common.utils.ContextUtils;
import com.devicemind.common.utils.DateUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 插入时填充 createdDate / createdBy，更新时填充 updatedDate / updatedBy。
 * 当前用户 ID 通过 {@link ContextUtils} 获取，未登录时默认 0L。
 * <p>
 * 由 {@link DataSourceConfig} 注册到各 SqlSessionFactory 的 GlobalConfig。
 */
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long personId = ContextUtils.getCurrentUserId().orElse(-1L);
        Date now = DateUtils.getNow();
        strictInsertFill(metaObject, "createdDate", Date.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, personId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long personId = ContextUtils.getCurrentUserId().orElse(-1L);
        Date now = DateUtils.getNow();
        strictUpdateFill(metaObject, "updatedDate", Date.class, now);
        strictUpdateFill(metaObject, "updatedBy", Long.class, personId);
    }
}
