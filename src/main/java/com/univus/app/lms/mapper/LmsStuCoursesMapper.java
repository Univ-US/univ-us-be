package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuCoursesDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuCoursesMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    List<LmsStuCoursesDto.CourseFlatRow> selectStudentCourseRows(@Param("lmsPrfId") Long lmsPrfId);
}
