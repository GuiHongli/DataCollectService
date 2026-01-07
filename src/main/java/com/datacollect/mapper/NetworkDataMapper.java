 package com.datacollect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.dto.NetworkDataGroupDTO;
import com.datacollect.entity.NetworkData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 网络侧数据Mapper
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface NetworkDataMapper extends BaseMapper<NetworkData> {
    
    /**
     * 分页查询网络侧数据聚合（按GPSI+日期+子应用ID分组）
     * 从start_time字段提取日期（格式：YYYY-MM-DD HH:mm:ss -> YYYY-MM-DD）
     * 使用LEFT函数提取前10个字符作为日期部分
     * 
     * @param page 分页对象
     * @param gpsi GPSI筛选条件（可选）
     * @param date 日期筛选条件（可选，格式：YYYY-MM-DD）
     * @param subAppId 子应用ID筛选条件（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT " +
            "gpsi, " +
            "LEFT(start_time, 10) as date, " +
            "sub_app_id as subAppId, " +
            "COUNT(*) as count " +
            "FROM network_data " +
            "WHERE deleted = 0 " +
            "AND gpsi IS NOT NULL " +
            "AND start_time IS NOT NULL " +
            "AND LENGTH(TRIM(start_time)) >= 10 " +
            "AND LEFT(start_time, 10) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' " +
            "AND sub_app_id IS NOT NULL " +
            "AND sub_app_id != '' " +
            "<if test='gpsi != null and gpsi != \"\"'>" +
            "AND gpsi = #{gpsi} " +
            "</if>" +
            "<if test='date != null and date != \"\"'>" +
            "AND LEFT(start_time, 10) = #{date} " +
            "</if>" +
            "<if test='subAppId != null and subAppId != \"\"'>" +
            "AND sub_app_id = #{subAppId} " +
            "</if>" +
            "GROUP BY gpsi, LEFT(start_time, 10), sub_app_id " +
            "ORDER BY LEFT(start_time, 10) DESC, gpsi ASC, sub_app_id ASC" +
            "</script>")
    Page<NetworkDataGroupDTO> selectGroupedNetworkDataPage(
            Page<NetworkDataGroupDTO> page,
            @org.apache.ibatis.annotations.Param("gpsi") String gpsi,
            @org.apache.ibatis.annotations.Param("date") String date,
            @org.apache.ibatis.annotations.Param("subAppId") String subAppId);
}





