package com.devicemind.core.persistence.dao.timescale;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.vo.DeviceDataVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 设备时序数据 DAO（TimescaleDB）— 自定义 SQL
 */
@Mapper
public interface DeviceDataDao {

    IPage<DeviceDataVO> selectDataPage(Page<DeviceDataVO> page,
                                       @Param("deviceId") String deviceId,
                                       @Param("attrName") String attrName,
                                       @Param("start") Long start,
                                       @Param("end") Long end);

    List<Map<String, Object>> selectLatestDataTimes(@Param("deviceIds") List<String> deviceIds);
}
