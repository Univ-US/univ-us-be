package com.univus.app.member.mapper;

import com.univus.app.member.dto.LoginSessionDto;
import com.univus.app.member.dto.LoginLogDto;
import com.univus.app.member.dto.MemberDto;
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

  // 로그인 성공 시 MEMBER.LOGTIME_AT = SYSTIMESTAMP
  int updateLogtimeAt(@Param("memberId") Long memberId);

  // 성공/실패/로그아웃 이력 저장
  int insertLoginLog(LoginLogDto loginLog);

  // 로그인 성공 시 refresh token 저장
  int insertLoginSession(LoginSessionDto loginSession);

  // 로그아웃 시 refresh token 폐기
  int revokeLoginSession(@Param("refreshToken") String refreshToken);

}
