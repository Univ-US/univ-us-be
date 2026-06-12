## 1. 개요
- 학교(UNIV_ID)별로 도서관(READING_ROOMS)과 회의실(MEETING_ROOMS) 데이터가 분리되도록 백엔드 조회 쿼리 로직을 개선했습니다.

## 2. 작업 사항
- **SpaceReservationController**: `getRoomAvailability`, `getReadingRoomAvailability`, `getReadingSeatAvailability` API에 `@AuthenticationPrincipal memberId` 파라미터 추가
- **ReservationService**: 조회 시 `memberId`를 받아 하위 Mapper로 전달하도록 메서드 시그니처 수정 및 권한 검증 추가
- **ReservationMapper & XML**: 시설물 및 열람실 조회 시 `WHERE` 절에 `AND UNIV_ID = (SELECT UNIV_ID FROM MEMBER WHERE MEMBER_ID = #{memberId})` 서브쿼리 필터 추가

## 3. 관련 이슈 / 특이사항
- 부모 테이블(`READING_ROOMS`, `MEETING_ROOMS`)에 `UNIV_ID`가 격리되어 있으므로, 하위 자식(좌석, 채팅, 예약)들은 별도의 UNIV_ID 검증 없이 자동으로 격리됩니다.
- DB에 `READING_ROOMS`, `MEETING_ROOMS` 테이블에 `UNIV_ID` 컬럼이 반드시 추가되어 있어야 정상 동작합니다.
