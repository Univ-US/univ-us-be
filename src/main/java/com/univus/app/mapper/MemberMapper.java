package com.univus.app.mapper;

import com.univus.app.domain.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

  MemberDto findByMemberId(@Param("memberId") Long memberId);

  // 아이디 중복 검사
  int existsByMemberId(@Param("memberId") Long memberId);

  // 닉네임 중복 검사
  int existsByCommunityNickname(@Param("communityNickname") String communityNickname);

  int insertMember(MemberDto member);

  int insertMemberDetail(MemberDto member);

}
