package com.univus.app.member.mapper;

import com.univus.app.member.dto.LoginSessionDto;
import com.univus.app.member.dto.LoginLogDto;
import com.univus.app.member.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

  MemberDto findByLoginIdAndUnivId(@Param("loginId") String loginId, @Param("univId") Long univId);

  // 관리자/게스트 로그인처럼 학교 선택 없이 loginId만으로 회원을 조회할 때 사용합니다.
  MemberDto findByLoginId(@Param("loginId") String loginId);

  MemberDto findByMemberId(@Param("memberId") Long memberId);

  // 아이디 중복 검사
  int existsByLoginId(@Param("loginId") String loginId);

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

  // 활성 로그인 세션 조회
  LoginSessionDto findActiveLoginSessionByRefreshToken(@Param("refreshToken") String refreshToken);

  // 로그아웃 시 refresh token 폐기
  int revokeLoginSession(@Param("refreshToken") String refreshToken);

}
