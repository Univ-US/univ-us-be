# univ-us-be (백엔드)

> Univ-US 프로젝트의 백엔드 REST API 서버. 프론트엔드(`univ-us-fe`, React)와 분리된 구조이며, 모든 응답은 JSON입니다.

## 🧰 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java **21** |
| 프레임워크 | Spring Boot **4.0.6** |
| 빌드 | Gradle (Wrapper) |
| 데이터베이스 | **Oracle** (ojdbc11) |
| 영속성 | **MyBatis** (mybatis-spring-boot-starter 4.0.1, XML Mapper) |
| 보안 | Spring Security + OAuth2 Client *(JWT 인증 도입 예정)* |
| 실시간 | WebSocket (STOMP) |
| 기타 | Lombok, Spring Boot DevTools |

## 🚀 시작하기

```bash
# 빌드 (컴파일 + 테스트 + jar 패키징)
./gradlew build        # Windows: gradlew.bat build

# 실행
./gradlew bootRun

# 테스트만 실행
./gradlew test
```

> ⚠️ **사전 준비**
> `src/main/resources/application.yml` 은 **.gitignore 처리**되어 레포에 포함되지 않습니다.
> 로컬에서 **Oracle 접속정보**와 **`file.upload-root`** 등을 직접 작성해야 정상 구동됩니다.
> DB 접속정보·시크릿은 **절대 커밋하지 마세요.**

> 🛠️ **`gradlew` 실행권한 (이미 조치 완료, macOS/Linux)**
> **변경 내역**: `gradlew`를 git에 **실행 가능한 모드(`100755`)로 기록**하도록 수정했습니다.
> (`chore/cicd-docs-cleanup`에서 `git update-index --chmod=+x gradlew` 적용 → 모드 `100644` → `100755`)
> - **왜 필요했나**: 기존엔 `gradlew`가 `100644`(비실행)로 저장돼 macOS/Linux에서 클론 시 `-rw-r--r--`가 되어 `zsh: permission denied: ./gradlew` 발생. 원인은 이 파일을 **최초 커밋한 환경이 Windows**여서 — Windows엔 유닉스 실행비트(`x`) 개념이 없어 git이 `100644`로 기록했기 때문.
> - **효과**: 이제 새로 클론해도 `./gradlew`가 바로 실행됨. **윈도우 팀원 영향 없음**(Git `core.fileMode=false`로 실행비트 무시 + 윈도우는 `gradlew.bat` 사용 + content 변화 0).
> - 혹시 다시 `permission denied`가 나면(예: 윈도우에서 gradlew 수정·재커밋으로 644 회귀): `chmod +x gradlew` (임시) 또는 `git update-index --chmod=+x gradlew` (영구) 재적용.

## 🌿 브랜치 전략 & 작업 규칙

- 기본 브랜치: **`dev`**
- 흐름: **`main ← dev ← feat/*`**
- 모든 작업은 **dev에서 새 브랜치를 따서 → dev로 PR**. **dev / main 직접 커밋 금지.**

> dev로 오는 **모든 브랜치 PR**이 `ci-cd-dev.yml`로 자동 검증·병합됩니다.
> (프론트엔드와 동일하게 `head_ref` 필터를 두지 않음)

**작업 흐름 명령 예시**

```bash
git switch -c feat/your-work        # dev에서 분기
# ...작업...
git push -u origin feat/your-work
gh pr create --base dev --fill      # dev로 PR (CI 통과 시 자동 병합)
gh pr view --web                    # PR 상태 확인
```

## 🔄 CI/CD 파이프라인

GitHub Actions 워크플로우 2개로 구성됩니다.

### `ci-cd-dev.yml` — `* → dev`
- **트리거**: 어떤 브랜치든 **dev로 PR**(생성/커밋추가/재오픈) 될 때
- **동작**: `./gradlew build`(컴파일 + JUnit) → **통과 시 자동 병합**(`gh pr merge`), 실패 시 거부
- 동시 PR은 `concurrency`로 한 번에 하나씩 직렬화
- **Discord 알림** (`[dev]` 태그, PR번호·브랜치 포함):
  - ❌ 테스트 실패 / ✅ 자동 병합 성공 / ⚠️ 테스트는 통과했으나 병합 실패

### `ci-cd-main.yml` — `dev → main` (정기)
- **트리거**: ⛔ **현재 스케줄(cron) 정지** — CD 안정화 전 임시 조치로 `schedule` 주석 처리. **수동(`workflow_dispatch`)만 동작**
  - 복구: `ci-cd-main.yml`의 `schedule` 2줄 주석 해제 후 **dev에 반영**(스케줄은 기본 브랜치 버전 기준으로 동작)
- **동작**: dev 빌드·테스트 → 통과 시 **dev→main 병합** (실제 변경이 있을 때만)
- **Discord 알림** (`[main]` 태그):
  - ✅ 정기 병합 성공(실제 병합 시만) / ❌ 검사·병합 실패

### 인증 / 권한
- `GITHUB_TOKEN` + 워크플로우 `permissions: contents: write, pull-requests: write` → **PAT 불필요**
- Repo Secret: `DISCORD_WEBHOOK`

### 🔒 브랜치 보호 (dev Ruleset)

> ✅ **적용 완료** — FE(`univ-us-fe`)와 **동일 ruleset**으로 `dev` 보호 **활성(Active)**. dev 직접 push·삭제·force-push 차단, `test` 통과 + PR 후에만 병합, **승인 0**으로 봇 자동병합 유지.

- Settings → Rules → Rulesets, Target=**`dev`**, Enforcement=**Active**
- **Require a pull request before merging** + **Required approvals = `0`**
  - ⚠️ 봇 자동병합 유지를 위해 **0 필수** (1 이상이면 `GITHUB_TOKEN` 자동병합이 차단됨)
- **Require status checks to pass** → 검증 잡 **`test`** 지정
- **Restrict deletions** / **Block force pushes**
- ⚠️ **`main`은 지금 보호하지 않음** — CD 구축 시 "직접 push 금지 + PR/CI 필수"로 함께 설계
  (지금 보호를 걸면 수동 `workflow_dispatch`의 main 직접 push와 충돌)

## ⚠️ 현재 CI/CD의 한계와 개선 과제

> **핵심:** `./gradlew build`는 **컴파일 + 작성된 JUnit 테스트까지만** 검증합니다.
> **테스트가 부족하면 런타임·로직 버그는 못 잡습니다.** (빌드 성공 ≠ 정상 동작)

| 항목 | 현재 |
|------|------|
| 컴파일·의존성 오류 | ✅ 잡음 |
| 문법은 맞지만 **로직 오류** | ❌ 못 잡음 |
| 단위 동작 | 테스트 있으면 ✅ / 없으면 ❌ |
| 통합·API 동작 | 테스트 없으면 ❌ |
| 보안(의존성 취약점 등) | ❌ 못 잡음 |

**개선 과제 (적은 노력으로 큰 효과):**
1. **JUnit 단위 테스트** 커버리지 확대
2. **통합 테스트(`@SpringBootTest`)** / **API 테스트(`MockMvc` 또는 REST Assured)**
3. **브랜치 보호 규칙**(필수 상태체크 통과 후 병합) 추가
4. **정적 분석 / 커버리지**(Checkstyle · SpotBugs · JaCoCo) 도입
5. **보안 점검**(OWASP Dependency-Check 또는 Dependabot)
6. **액션 버전 업**(`actions/checkout`, `actions/setup-java` 최신화)

## 📁 디렉토리 구조

```
src/main/java/com/univus/app/
├── common/      # 파일 업로드·다운로드·ZIP, 페이지네이션, 요청 유틸 (구현됨)
├── config/      # Security(STATELESS), CORS(localhost:3000), WebSocket(STOMP), PasswordEncoder
├── controller/  # REST 컨트롤러 (예: GET /api/test)
├── domain/dto/  # 데이터 전송 객체(DTO)
├── exception/   # 전역 예외 처리 (400/404/500 JSON 응답)
├── mapper/      # MyBatis Mapper 인터페이스 (+ resources/mybatis/mapper/*.xml)
├── security/    # 인증/인가 (CustomUserDetails 등 — 대부분 구현 예정)
└── service/     # 비즈니스 로직
```
