package com.devicemind.core.persistence.dao.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.vo.DeviceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备 DAO — 自定义 SQL（CRUD 由 MyBatis-Plus BaseMapper 提供）
 */
@Mapper
public interface DeviceDao {

    IPage<DeviceVO> selectDevicePage(Page<DeviceVO> page,
                                     @Param("deviceId") String deviceId,
                                     @Param("productId") Long productId,
                                     @Param("status") String status);

    long countByProductId(@Param("productId") Long productId);
}
