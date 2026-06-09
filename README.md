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
- **동작**: `./gradlew build -x test`(⚠️ **테스트 스킵 — 컴파일/빌드만 검증**) → **통과 시 자동 병합**(`gh pr merge`), 실패 시 거부
  - 테스트를 건너뛰는 이유: 현재 의미 있는 테스트가 없고(스모크 `contextLoads`만 있어 기능 추가 시 자주 깨짐), 학습 단계에선 파이프라인에 집중. **실제 단위/슬라이스 테스트가 생기면 `-x test`를 빼서 되살릴 것.**
  - 검증 잡 이름은 **`test` 그대로 유지** (dev/main 룰셋의 필수 status check 이름과 일치시켜야 하므로 변경 금지)
- 동시 PR은 `concurrency`로 한 번에 하나씩 직렬화
- **Discord 알림** (`[dev]` 태그, PR번호·브랜치 포함):
  - ❌ 빌드 실패 / ✅ 자동 병합 성공 / ⚠️ 빌드는 통과했으나 병합 실패

### `ci-cd-main.yml` — `dev → main` (정기)
- **트리거**: ⛔ **현재 스케줄(cron) 정지** — CD 안정화 전 임시 조치로 `schedule` 주석 처리. **수동(`workflow_dispatch`)만 동작**
  - 복구: `ci-cd-main.yml`의 `schedule` 2줄 주석 해제 후 **dev에 반영**(스케줄은 기본 브랜치 버전 기준으로 동작)
- **동작**: dev 빌드·테스트 → 통과 시 **dev→main 병합** (🔁 직접 push가 아니라 **PR 생성→머지** 방식 — main 보호와 호환)
  - 봇 PR 생성을 위해 org+repo의 **"Allow GitHub Actions to create and approve pull requests"** 활성 필요
- **Discord 알림** (`[main]` 태그):
  - ✅ 정기 병합 성공(실제 병합 시만) / ❌ 검사·병합 실패

### `deploy.yml` — CD (배포) 🚀

`main`에 반영되면(또는 수동 실행) **빌드 → 이미지 → K8s 배포**가 자동으로 굴러갑니다.
(배포 대상 = 서버 K8s 클러스터: kubeadm + MetalLB + Traefik + Helm)

**전체 흐름**
```
main push / 수동 실행 (workflow_dispatch)
   │ ⟶ 트리거
[GitHub Actions: deploy.yml]
   ├─ ① build 잡 (GitHub 클라우드 러너, ubuntu-latest)
   │     setup-java 21 → ./gradlew build → docker build(Dockerfile)
   │     → GHCR push: ghcr.io/univ-us/univ-us-be:<커밋SHA>
   │
   └─ ② deploy 잡 (서버 VM의 self-hosted 러너)
         helm upgrade --install univ-us-be ./charts/univ-us-be --set image.tag=<SHA>
                ▼
         [Kubernetes]  GHCR에서 이미지 pull
            → 새 파드 생성 → Ready 확인 → 옛 파드 종료   (롤링 업데이트 = 무중단)
                ▼
         [Traefik]  /api → univ-us-be Service(9090) → 파드
```

**누가 무슨 역할? (도구 정리)**

| 도구 | 역할 | 비유 |
|------|------|------|
| GitHub Actions | 빌드·배포를 자동 실행하는 파이프라인 | 작업 지시서 |
| Docker 이미지 | 앱 + 실행환경을 한 덩어리로 포장 | 택배 상자 |
| GHCR | 그 이미지를 보관하는 레지스트리(창고) | 택배 보관소 |
| Kubernetes(K8s) | 이미지를 컨테이너로 굴리는 오케스트레이터 | 물류센터(지휘자) |
| kubectl | K8s에 직접 명령하는 저수준 CLI | 지휘자에게 거는 전화 |
| Helm | K8s 매니페스트를 "차트"로 묶어 한 번에 배포 (kubectl 위 고수준) | 포장·배달 매니저 |
| self-hosted 러너 | 서버 VM에서 helm 명령을 실제 실행 | 현장 작업자 |
| Traefik | 외부 요청을 경로(`/api`)로 갈라 보내는 게이트웨이 | 안내 데스크 |

> 관계 한 줄: **Helm**이 `charts/univ-us-be/`를 렌더해 **K8s**에 적용 → K8s가 **GHCR**에서 이미지를 받아 파드로 실행. (kubectl=직접 명령, Helm=그 위 패키지 매니저)

**무중단 배포(롤링 업데이트)**: 새 SHA 배포 시 K8s가 → ① **새 파드 먼저** 띄워 Ready(헬스체크) 확인 → ② 그 다음 **옛 파드 종료**. 끊김 없음. 새 파드가 고장나면 옛 파드가 계속 떠 있어 **안전**.

**산출물 / 설정 규칙**
- 산출물: `Dockerfile` · `.dockerignore` · Helm 차트 `charts/univ-us-be/` · `application-prod.yml`(운영 프로파일, **비밀 없는 placeholder**)
- 이미지 태그 = **커밋 SHA**(불변 → 롤백 용이)
- 비밀·환경설정은 **K8s Secret/ConfigMap**으로 주입(코드/이미지엔 없음). 운영은 `SPRING_PROFILES_ACTIVE=prod`로 `application-prod.yml`을 로드하고, K8s env가 `${...}` placeholder를 채움.
- 헬스 프로브 = `tcpSocket:9090` (현재 `/api/test`가 인증 필요라 httpGet은 부적합)

**⚠️ 트리거 현실 (중요)**
- 트리거 = `workflow_dispatch`(수동) + `push: main`.
- 단, `ci-cd-main.yml`이 `GITHUB_TOKEN`으로 dev→main을 머지하면 **그 `main` push는 다른 워크플로를 트리거하지 않음**(GitHub 재귀 방지). → **현재 배포는 수동(`workflow_dispatch`)으로 실행.** 완전 자동화하려면 **PAT 또는 `repository_dispatch`** 필요(향후).
- `pull_request` 트리거 **금지** (public 레포 + self-hosted 러너 = fork PR RCE 방지).

> 첫 배포 전 **클러스터 1회 세팅**(네임스페이스 · `ghcr-cred` imagePullSecret · ConfigMap/Secret/PVC · local-path StorageClass · IngressRoute)은 **서버 인프라 측 담당**.

### 🔙 롤백 (배포 되돌리기)

> **핵심: 런타임 롤백(helm) ≠ 코드 롤백(git revert)** — 둘은 독립이다. `main` 병합은 항상 **앞으로(배포)** 가는 동작이고, 되돌리기는 helm·revert가 담당한다. 이미지 태그가 **커밋 SHA(불변)** 이고 helm이 **릴리스 리비전**을 추적하므로 언제든 이전 상태로 복구 가능.

**① `helm rollback` — 서버 즉시 복구 (가장 빠름, git 무관 · 서버 VM에서)**
```bash
helm history univ-us-be -n univus                # 리비전 이력 (REVISION / STATUS / DESCRIPTION)
helm rollback univ-us-be <이전리비전> -n univus    # 그 리비전(이미지SHA+차트)으로 즉시 롤링
```
- 재빌드·GitHub Actions를 안 거쳐 **1분 내 복구** → **장애 긴급복구 1순위**.
- ⚠️ 차트(IngressRoute 등)까지 그 리비전 상태로 **통째** 되돌아감(부분 롤백 아님).
- git 히스토리엔 잘못된 코드가 그대로 남으므로, 안정화 후 **③으로 정리**.

**② 특정 good 이미지(커밋 SHA)로 재배포 (서버 VM에서)**
```bash
helm upgrade univ-us-be ./charts/univ-us-be --set image.tag=<good-sha> -n univus
```
- 이미지가 커밋 SHA 불변 태그라 과거 이미지가 **GHCR에 그대로** → 원하는 버전을 콕 집어 소환.

**③ `git revert` — 코드 영구 수정 (fix-forward)**
```bash
git switch -c fix/revert-bad dev
git revert <잘못된커밋SHA>     # 그 변경을 취소하는 새 커밋 (머지커밋이면 -m 1)
# → dev로 PR → 병합 → (main) → 재배포
```
- ⚠️ 공유 브랜치(`dev`/`main`)에서 **`git reset --hard`(히스토리 삭제) 금지** — 협업이 깨진다. 공유 브랜치는 **revert**(되돌리는 커밋을 쌓는 방식)가 정석.
- 커밋 "일부만" 잘못이면 revert 대신 **그 부분만 고치는 새 커밋**(fix-forward)이 깔끔.

**🚑 실전 순서 (장애 발생 시)**
1. **`helm rollback`** 으로 서버부터 즉시 복구 (사용자 끊김 방지 — 시간 버는 단계)
2. 차분히 **`git revert`** 또는 수정 커밋 → dev PR → 병합
3. **재배포**(정상 코드) → `helm history`에 새 정상 리비전이 쌓임

> FE(`univ-us-fe`)도 동일: `helm rollback univ-us-fe -n univus`.

### 인증 / 권한
- `GITHUB_TOKEN` + 워크플로우 `permissions: contents: write, pull-requests: write` → **PAT 불필요** (CD의 GHCR push는 `packages: write`)
- Repo Secret: `DISCORD_WEBHOOK`

### 🔒 브랜치 보호 (dev Ruleset)

> ✅ **적용 완료** — FE(`univ-us-fe`)와 **동일 ruleset**으로 `dev` 보호 **활성(Active)**. dev 직접 push·삭제·force-push 차단, `test` 통과 + PR 후에만 병합, **승인 0**으로 봇 자동병합 유지.

- Settings → Rules → Rulesets, Target=**`dev`**, Enforcement=**Active**
- **Require a pull request before merging** + **Required approvals = `0`**
  - ⚠️ 봇 자동병합 유지를 위해 **0 필수** (1 이상이면 `GITHUB_TOKEN` 자동병합이 차단됨)
- **Require status checks to pass** → 검증 잡 **`test`** 지정
- **Restrict deletions** / **Block force pushes**

### 🔒 브랜치 보호 (main Ruleset) — 배경·과정 기록

> ✅ **적용 완료** — `main` 직접 push **차단**(PR 필수), 봇은 **PR 머지**로 통과. dev와 동일 정책(승인 0, force/삭제 차단), 대상만 `main`.

**왜 했나 (배경)**
- `main`은 향후 **CD의 배포 스위치**(`main` push/workflow_run → 실배포 예정). 그래서 **실수·통제 안 된 직접 push**를 반드시 막아야 함.
- 그런데 단순히 "직접 push 금지"만 걸면, 기존 `ci-cd-main.yml`이 **직접 push(`git push origin main`)로 병합**하던 탓에 **봇 병합까지 같이 막히는** 문제 발생.
- → 워크플로를 **PR 머지 방식으로 전환**한 뒤 main을 보호하는 **지속 가능형(방법 2)** 으로 진행.

**어떻게 했나 (과정)**
1. **org + repo Actions 설정**: *Settings → Actions → General →* **"Allow GitHub Actions to create and approve pull requests"** 활성화 → 봇(`GITHUB_TOKEN`)이 dev→main **PR 생성** 가능 (기본은 차단돼 있음)
2. **`ci-cd-main.yml` 전환**: `git push origin main`(직접) → **`gh pr create` + `gh pr merge`**(PR 머지), `permissions`에 `pull-requests: write` 추가
3. **main Ruleset 생성**(`gh api` 또는 UI): **Require PR**(승인 0) + **Block force pushes** + **Restrict deletions**, **bypass 없음**
   - ※ repo 룰셋엔 **GitHub Actions 봇을 bypass에 추가 불가**(API `422`: *"Actor GitHub Actions integration must be part of the ruleset source or owner organization"*) → bypass 없이, 봇은 **"PR 머지"** 로 규칙을 통과
   - ※ **status check(`test`)는 main에 미설정** — dev→main PR엔 `test`가 트리거되지 않아(`ci-cd-dev.yml`이 `branches: [dev]`만 감시) **deadlock 방지**
4. **검증**: 사람 강제 push(`git push --force origin HEAD:main`) → **`GH013` 거부**(*Cannot force-push* / *Changes must be made through a pull request*) 확인. 봇 PR 머지 경로는 정상.

**설정 요약**

| 항목 | 값 |
|------|-----|
| Target / Enforcement | `main` / Active |
| Require PR | ✅ (Required approvals **0**) |
| Block force pushes / Restrict deletions | ✅ / ✅ |
| Required status check | ❌ (의도적 미설정 — deadlock 방지) |
| Bypass | 없음 |

**효과**: 사람 직접·강제 push 차단(PR로만), 봇 dev→main은 PR 머지로 유지 → dev·main **동일 거버넌스**.

> 🔭 **CD 트리거 헤드업** *(CD 구현 완료 → 위 `deploy.yml` 절 참고)*: `GITHUB_TOKEN`이 머지한 `main` push는 **다른 워크플로를 미트리거**(재귀 방지) → CD는 현재 **수동 `workflow_dispatch`** 로 실행, 완전 자동화는 **PAT 또는 `repository_dispatch`** 필요(향후 과제).

## ⚠️ 현재 CI/CD의 한계와 개선 과제

> **핵심:** 현재 `ci-cd-dev.yml`은 **`./gradlew build -x test`** 로 돌아 **컴파일/빌드만** 검증합니다(⚠️ **테스트 스킵**).
> **테스트(로직·동작)는 CI에서 전혀 검증하지 않습니다.** (빌드 성공 ≠ 정상 동작)
> → 실제 단위/슬라이스 테스트가 생기면 `-x test`를 빼서 다시 게이트로 활성화할 것.

| 항목 | 현재 |
|------|------|
| 컴파일·의존성 오류 | ✅ 잡음 |
| 문법은 맞지만 **로직 오류** | ❌ 못 잡음 |
| 단위/통합/API 동작 | ❌ (CI에서 `-x test`로 **테스트 스킵 중**) |
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
