# 패키지 정리 계획 (기능/권한 기준)

## 목적
- 현재 카운터/네이밍/화면 상태 코드가 화면 단위와 도메인 단위에 혼재되어 있어 변경 범위를 빠르게 파악하기 어려움.
- 다음 단계에서 기능 단위(촬영, 테이블, 설정) + 권한 단위(저장소/카메라)로 패키지를 정리해 유지보수성을 높인다.

## 1차 정리 대상(우선순위)
1. Counter Scope/Seed 정책
   - 대상: `domain/counter`, `ui/camera`, `ui/table`
   - 방향: 정책 계산(`scope 판정`, `seed 결정`)은 `domain/counter/policy`로 수렴
2. Naming Preview/Build 경로
   - 대상: `domain/naming`, `domain/capturepolicy`, `ui/home`, `ui/table`, `ui/camera`
   - 방향: preview/실촬영 모두 `CaptureNamingPolicy` 단일 경로 사용
3. 권한 연계 영역
   - 대상: 카메라/저장소 접근점
   - 방향: `feature/capture/permission`(권한 상태), `feature/capture/io`(저장 접근)로 분리

## 제안 패키지 스케치
- `feature/capture/...`
  - `ui` : CameraScreen, preview 컴포넌트
  - `policy` : 촬영 시점 counter/naming orchestration
  - `permission` : 카메라/저장소 권한 상태 및 요청
- `feature/table/...`
  - `ui` : TableEditor, 섹션 컴포넌트
  - `policy` : 테이블 seed sync coordinator
- `feature/settings/...`
  - `ui` : SettingsRoot, 토글/패딩 설정 UI
- `domain/counter/policy/...`
  - `CounterScopeSnapshot`, scope 판정, seed 결정
- `domain/naming/policy/...`
  - 파일명/경로 생성 정책

## 점진 이관 원칙
- 한 번에 이동하지 않고, "호출점 1개 + 테스트 1세트" 단위로 이동.
- public API 시그니처 유지 후 내부 위임으로 먼저 정리.
- 패키지 이동마다 회귀 테스트/에뮬 체크리스트 업데이트.

## 완료 기준
- 홈/촬영/표 상세설정에서 preview 생성 경로가 동일 정책 모듈을 통과.
- scope 변경 판정 로직이 단일 패키지(중복 없음).
- 권한 코드가 feature 하위에서 일관되게 관리.

## 진행 현황
- [x] 1단계 착수: `CounterScopeSnapshot`, `isNewCounterScope`를 `domain/counter/policy`로 이관
- [x] 2단계: seed 결정 정책(`CounterSeedPolicy`)도 `domain/counter/policy`로 패키지 정렬
- [x] 3단계 완료: capture/table/settings feature 경계 기준 1차 호출점 이관 완료
  - [x] table 호출점 1차 이관: `TableCounterPolicyCoordinator` -> `feature/table/policy`
  - [x] capture 호출점 1차 이관: `stabilizeStreamNextCounter` -> `feature/capture/policy`
  - [x] settings 호출점 1차 이관: `SettingsRootScreen` -> `feature/settings/ui`
  - [x] settings 컴포넌트 이관: `SegmentedControl` -> `feature/settings/components`
  - [x] settings 화면 라우팅 이관: `SettingsScreen` wrapper -> `feature/settings/ui`


## 최근 완결 처리(커밋 단위)
- [x] `448e5c4` settings 카운터 동기화 policy 추출
- [x] `b22d6b1` 전체설정 Reset 버튼 제거로 policy 진입점 제거(상기 추출 커밋은 의도적으로 되돌림)
- [x] `da6ace7` settings 미리보기 dead 계산 코드 제거
- [x] `17e8382` settings 토스트 처리 공통화(`showSettingsToast`)

## 남은 정리
- [~] feature 경계 2차 진행중: capture/table/settings 내부 세부 모듈(권한/IO/정책) 추가 분리
  - [x] capture 권한 모듈 1차 분리: `hasCameraPermission` -> `feature/capture/permission`
  - [x] capture 저장 IO 모듈 1차 분리: `createCaptureRepository` -> `feature/capture/io`
  - [x] 레거시 상태 제거: 문자열 scopeKey 기반 임시 상태(`lastScopeKey` 등) 제거
  - [x] table counter 충돌 복구 분기 공통화(`restoreCounterCellToAutoNext`)로 호출 중복 정리
  - [x] 전체설정 리셋 버튼 제거: Quick Controls에서 수동 카운터 동기화 진입점 정리
  - [x] settings 미리보기 주변 dead 계산 코드 제거(rows/cols ratio 임시값)
  - [x] settings 토스트 처리 공통화(`showSettingsToast`)로 콜백 중복 조건 정리
  - [x] settings 저장 mutation 분리: `SettingsMutationPolicy`로 AppSettingsStore write 경로 분리
  - [~] table/settings 세부 모듈 추가 분리 (진행중)
    - [x] table 저장 persistence write 분리: `saveTableTemplate` + watermark pref write를 `feature/table/policy/TableSettingsPolicy`로 이관
    - [x] settings 저장소 읽기 권한 판정 분리: `isStorageReadGranted`를 `feature/settings/policy`로 이관
    - [~] settings/table 잔여 UI 이벤트 분기 추가 분리 (진행중)
      - [x] settings 이벤트 디스패처 분리: `SettingsAction` + `applySettingsAction` 도입
      - [x] table 워터마크 이벤트 디스패처 분리: `TableWatermarkAction` + `applyTableWatermarkAction` 도입(통합 파일: `TableSettingsPolicy`)
      - [x] table policy 통합: persistence/action policy를 `TableSettingsPolicy` 단일 파일로 통합
      - [~] table 기타 UI 이벤트 분기 추가 분리 (진행중)
        - [x] table counter 인라인 편집 충돌 판정 분리: `evaluateCounterEditConflict`/`parseNonNegativeInt` 도입
        - [x] table 인라인 편집/다이얼로그 상태 전환 분기 추가 분리
- [ ] 네트워크 가능한 환경에서 테스트 실행 후 구조 정리 완료 판정
