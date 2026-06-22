# univ-us-be (백엔드)

> Univ-US 프로젝트의 REST API 서버. 프론트엔드(`univ-us-fe`)와 분리된 구조이며 응답은 전부 JSON.

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 빌드 | Java 21 · Gradle (Wrapper) |
| 프레임워크 | Spring Boot 4.0.6 (`spring-boot-starter-webmvc`) |
| 보안 | Spring Security (STATELESS) · JWT (jjwt 0.12.6) · OAuth2 Client |
| 영속성 | MyBatis 4.0.1 (XML Mapper) · Oracle (ojdbc11) |
| 캐시 / 락 / 세션 | Redis (Lettuce) · Redisson 3.27.0 — 분산 락 · refresh 토큰 저장 |
| 실시간 | WebSocket (STOMP + SockJS) |
| 외부 연동 | Groq LLM · PostgreSQL(pgvector) RAG (WebClient/WebFlux) · Octomo(옥토모) SMS 본인인증 — 아이디·비밀번호 찾기 |
| API 문서 | springdoc-openapi 3.0.3 — Swagger UI (`/swagger-ui.html`, OpenAPI 스펙 `/v3/api-docs`) |
| 기타 | Bean Validation · Apache POI (xlsx) · Lombok |

## 로컬 실행

```bash
./gradlew build      # 컴파일 + 테스트 + jar
./gradlew bootRun    # 실행 (기본 포트 9090)
```

> `src/main/resources/application.yml`은 `.gitignore` 대상. Oracle/Redis 접속정보·`jwt.secret`·`file.upload-root`·외부 API 키를 로컬에서 작성해야 부팅된다(시크릿 커밋 금지). Redisson이 부팅 시 Redis에 즉시 연결하므로 **로컬 Redis 기동 필요**.

## 브랜치 전략

- 기본 브랜치 **`dev`**. 흐름 `feat/* → dev → main`. **dev / main 직접 push 금지**(룰셋 차단 — PR로만 병합).
- dev로 향하는 모든 PR이 CI 검증 후 자동 병합된다.

```bash
git switch -c feat/<작업> origin/dev
git push -u origin feat/<작업>
gh pr create --base dev --fill
```

## CI/CD

| 워크플로 | 역할 | 트리거 |
|------|------|--------|
| `ci-cd-dev.yml` | dev PR `./gradlew build -x test`(컴파일/빌드만, 테스트 스킵) → 통과 시 자동 병합 | dev로 향하는 PR |
| `ci-cd-main.yml` | dev → main 병합 (PR 생성·머지 방식) | 수동 `workflow_dispatch` (cron 정지) |
| `deploy.yml` | build → GHCR push → `helm upgrade` (K8s 롤링 배포) | `push:main` + 수동 |

- 배포 대상 = VM 단일노드 K8s (kubeadm + MetalLB + Traefik + Helm). Traefik이 `/api`·`/ws-univus`·`/uploads`를 BE 파드(:9090)로 라우팅.
- 이미지 태그 = **커밋 SHA**(불변 → 롤백 용이). 빌드(GHCR push)는 클라우드 러너, `helm upgrade`는 VM self-hosted 러너에서 실행.
- 시크릿·환경설정은 K8s Secret/ConfigMap으로 주입(`SPRING_PROFILES_ACTIVE=prod` → `application-prod.yml`의 `${...}` placeholder를 env가 채움).
- 검증 잡 이름 `test`는 dev/main 룰셋의 필수 status check와 일치 → **변경 금지**. 알림 = Discord (`DISCORD_WEBHOOK`).
- ⚠️ CI는 컴파일/빌드만 검증한다(의미 있는 테스트 미작성). **빌드 성공이 동작을 보장하지 않음.**

## 서버 배포

```
1) dev → main 병합
   Actions → "CI-CD Main (Scheduled Dev to Main)" → Run workflow
   (dev 검증 후 dev→main PR 생성·머지)

2) 배포 실행
   Actions → "Deploy BE (build → GHCR → helm)" → Run workflow → Branch: main
```

- `push:main`이 `deploy.yml`을 트리거하지만, **봇(`GITHUB_TOKEN`) 머지는 재귀 방지로 자동 트리거되지 않는다** → 배포는 Deploy 워크플로를 **수동 실행**한다. (사람이 dev→main PR을 직접 머지하면 자동 트리거됨)
- 배포 확인 (VM SSH 후):
  ```bash
  kubectl get pods -n univus           # 파드 Running 확인
  helm history univ-us-be -n univus    # 릴리스 리비전 이력
  ```

## 롤백

런타임 롤백(helm)과 코드 롤백(git revert)은 독립이다. 이미지 태그가 커밋 SHA, helm이 릴리스 리비전을 추적하므로 언제든 이전 상태로 복구 가능.

**서버 롤백** (VM, 즉시 복구 — 장애 1순위)
```bash
helm history univ-us-be -n univus              # 리비전 확인 (REVISION/STATUS)
helm rollback univ-us-be <리비전> -n univus     # 이전 리비전(이미지SHA+차트)으로 롤링

# 또는 특정 정상 커밋 SHA 이미지로 재배포
helm upgrade univ-us-be ./charts/univ-us-be --set image.tag=<good-SHA> -n univus
```

**main 브랜치 롤백** (코드 영구 수정 = fix-forward)
```bash
git switch -c fix/revert-<설명> dev
git revert <잘못된커밋SHA>          # 머지 커밋이면 -m 1
git push -u origin fix/revert-<설명>
gh pr create --base dev --fill     # dev 머지 → dev→main → 재배포
```

- 공유 브랜치(`dev`/`main`)에서 `git reset --hard`·force push **금지**(룰셋도 차단). 되돌리기는 **revert**로 커밋을 쌓는다.
- 실전 순서: ① `helm rollback`으로 서버 즉시 복구 → ② `git revert`로 코드 정리 → ③ 재배포.

## 디렉토리 구조

```
src/main/java/com/univus/app/
├── config/       # Security(JWT)·CORS·Redis·WebSocket·WebMvc
├── security/     # JWT 필터·토큰 프로바이더
├── common/       # 파일 저장·페이지네이션·공통 유틸
├── exception/    # 전역 예외 핸들러
├── member/       # 인증/회원 (signup·login·refresh, JWT)
├── community/    # 게시판(post)·중고거래(market)
├── lms/          # 교수·학생 LMS (프로필·수강생·채점·과제·출결·공지·채팅·캘린더)
├── reservation/  # 공간·좌석 예약 (Redisson 분산 락)
├── subscription/ # 구독 결제 (PortOne)
├── ai/           # 챗봇 (Groq + pgvector RAG)
├── commoncode/ · admin/ · serviceadmin/ · inquiry/ · weather/ · cmypage/
└── ...
src/main/resources/mybatis/mapper/   # *.xml (MyBatis SQL)
```
