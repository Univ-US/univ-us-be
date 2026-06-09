package com.univus.app.commoncode.mapper;

import com.univus.app.commoncode.dto.CommonCodeResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommonCodeMapper {

    /* 그룹코드(GRP_CODE)별 활성 공통코드 목록 (CODE_ORDER 정렬) */
    List<CommonCodeResponseDto> selectCodesByGroupCode(@Param("groupCode") String groupCode);
}
