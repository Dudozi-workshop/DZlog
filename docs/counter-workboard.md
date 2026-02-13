# Counter / Scope 작업 보드

이 문서는 카운터/스트림 관련 작업의 진행 상태를 한 곳에서 관리하기 위한 체크리스트입니다.

## 현재 목표
- 카운터가 의도한 scope(저장경로/파일명 ON/OFF 조합)에서만 반응하도록 안정화
- 화면별(촬영/표 상세설정/홈) 미리보기 의미를 일치시켜 혼란 최소화
- 회귀 테스트/에뮬 QC 루틴을 고정해 재발 방지

## 완료된 작업
- [x] wildcard(`*`) scope에서 MediaStore 스캔/파싱 가능하도록 보강
- [x] 같은 scope에서 카운터 역행 방지(stable stream next)
- [x] manual override clear 조건을 실제 정책 판단(`shouldClearPreserveManualSeed`) 기반으로 제한
- [x] DATE/TIME 셀이 counter stream prefix를 흔들지 않도록 제외
- [x] 파일명 delimiter가 하드코딩(`_`)되지 않도록 `fnDelim` SSOT 반영
- [x] ON/OFF 모드 전환 시, 현재 ON인 축(path/filename)만 새 스트림 판정하도록 스냅샷 기반 로직 적용
- [x] counter padding 변경 시 파일명 suffix에도 동일 자릿수가 반영되도록 SSOT 파이프라인 보강

## 이번 턴 반영
- [x] 전체설정 화면에서 저장경로/파일명 상단 미리보기(Status card) 제거
- [x] 홈 화면 저장경로/파일명 카드를 SSOT 기반 계산(CaptureCounterPolicy + CaptureNamingPolicy)으로 재연결
- [x] 4조합(Path/Filename ON/OFF) 회귀 고정표 문서 추가
- [x] 진행 현황 문서화 시작(이 문서 추가)


## 최근 완결 처리(브랜치 정리)
- [x] settings Quick Controls Reset 진입점 제거
- [x] settings dead 계산 코드 제거
- [x] settings 토스트 중복 조건 공통화

## 남은 작업
- [x] OFF/OFF 정책을 완전 전역(전체 최대값 기반)으로 확정
- [x] (결정) 설정 화면 scope 동작 요약 문구는 현 단계에서 미적용(추후 UX 개선 배치로 이관)
- [ ] 네트워크 가능한 환경에서 단위 테스트 실행/결과 아카이브

## 에뮬 QC 핵심 시나리오
1. 같은 값 유지 + 모드 토글만 변경 시 카운터가 불필요 초기화되지 않아야 함
2. Filename ON일 때만 파일명(prefix) 변경에 반응해야 함
3. Path ON일 때만 저장경로 변경에 반응해야 함
4. OFF/OFF에서는 전역 흐름처럼 최대값 기반으로 이어지는지 확인

## 이번 턴 확인 결과
- [x] OFF/OFF 정책 방향 확정: **완전 전역**
- [x] 4조합 에뮬 체크리스트 실행 준비 상태 재점검 (시나리오/판정 기준 문서화 완료)
- [x] 이전 부산물 정리: 문자열 scopeKey 비교 유틸 제거(스냅샷 판정으로 단일화)
- [x] 다음 단계용 패키지 정리 초안 문서 추가(`docs/package-reorg-plan.md`)
- [x] 패키지 정리 1단계: scope 판정 모델/함수를 `domain/counter/policy`로 분리 이관
- [x] 패키지 정리 2단계: seed 정책(`CounterSeedPolicy`)을 `domain/counter/policy`로 이관
- [x] 패키지 정리 3단계(부분): table 정책 코디네이터를 `feature/table/policy`로 이관
- [x] 패키지 정리 3단계(부분): capture 안정화 정책(`stabilizeStreamNextCounter`)을 `feature/capture/policy`로 이관
- [x] 패키지 정리 3단계(부분): settings 진입점(`SettingsRootScreen`)을 `feature/settings/ui`로 이관
- [x] 패키지 정리 3단계(부분): settings 공용 컴포넌트(`SegmentedControl`)를 `feature/settings/components`로 이관
- [x] 패키지 정리 3단계(부분): settings 라우팅 래퍼(`SettingsScreen`)를 `feature/settings/ui`로 이관
- [x] 패키지 정리 3단계 마감: feature 경계 1차 이관(table/capture/settings) 완료
- [x] 패키지 정리 4단계(부분): capture 권한 체크(`hasCameraPermission`)를 `feature/capture/permission`으로 분리
- [x] 앨범 삭제 UX 정리: 앱 내부 삭제 확인 팝업 제거(시스템 삭제 요청만 사용)
- [x] 패키지 정리 4단계(부분): capture 저장소 생성 로직(`createCaptureRepository`)을 `feature/capture/io`로 분리
- [x] 이전 기획 부산물 정리: 문자열 scopeKey 상태(`lastScopeKey`, `currentScopeKey`, `previousScopeKey`) 완전 제거
- [x] 이전 기획 부산물 정리: 카메라 typealias 브릿지(`CameraPreviewTypeAliases`) 및 table 디버그 scope 로그 제거
- [x] 앨범 삭제 정책 중복 제거: `createDeleteRequest` 호출을 `feature/log/policy/launchMediaDeleteRequest`로 통합
- [x] scope 스냅샷 생성 경로 단일화: `buildCounterScopeSnapshot` 도입 후 camera/table 호출점 공통화
- [x] 마이그레이션 디버그 로그 정리: camera/table counter sync 로그 출력 제거
- [x] table persistence 분리: 템플릿 저장/워터마크 설정 write를 `feature/table/policy/TableSettingsPolicy`로 이관
- [x] settings 권한 판정 분리: 저장소 읽기 권한 체크를 `feature/settings/policy/isStorageReadGranted`로 이관
- [x] settings 이벤트 분기 정리: `SettingsAction`/`applySettingsAction`으로 콜백 분기 공통화
- [x] table 워터마크 이벤트 분기 정리: `TableWatermarkAction`/`applyTableWatermarkAction`으로 콜백 분기 공통화 (TableSettingsPolicy)
- [x] table 카운터 인라인 편집 분기 정리: 충돌 판정/숫자 파싱을 `TableCounterEditPolicy`로 분리

- [x] table 인라인 편집/다이얼로그 상태 전환 분기 분리: `TableCounterConflictDialogPolicy` 도입
- [x] 에뮬 실측 결과 1차 기록
  - Pixel 6a / Android 14 에뮬: 4조합(Path/Filename ON/OFF) PASS
  - same scope 유지/Path 변경 반응/Filename 변경 반응/OFF-OFF 전역 증가 PASS
- [x] 에뮬 실측 결과 기록(기기별 PASS/FAIL) 1차 누적 완료
- [ ] 로컬 단위 테스트 실행은 네트워크 프록시 제한(Gradle 403) 해소 후 재시도

## 메모
- 테스트 명령은 환경 프록시 제약으로 현재 실행 실패 가능(Gradle 배포본 다운로드 403).
- 네트워크 가능한 개발/CI 환경에서 동일 명령 재실행 필요.
