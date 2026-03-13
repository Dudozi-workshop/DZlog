# Counter SSOT 후속 정리 메모

## A. 카메라/테이블 스코프 동기화
- 조치 완료: `TableEditorScreen`도 `AppSettingsStore`의
  - `includePathInCounterScope`
  - `includeFilenameInCounterScope`
  값을 읽어 `buildScopedCounter(...)`로 정책 키를 계산하도록 통일.
- 기대 효과: 카메라와 테이블에서 동일 설정일 때 동일 scope key를 사용.

## B. 미사용 이전 설계 경로 정리
- 조치 완료: `CaptureNamingPolicy.buildForCapture(...)` 제거.
- 현재 촬영 네이밍 엔트리포인트는 `buildForCaptureWithCounter(...)` 단일 경로.

## D. 잔존 코드 점검 결과
아래는 아직 남아 있는 후속 정리 대상:
1. `CounterManager` legacy 마이그레이션 분기
   - 정리 완료: `legacyKey`, `loadMigratedLegacyKeys`, `markLegacyMigrated` 제거.
   - 카운터 조회는 현행 stream 키 + MediaStore 재스캔 기반으로 단순화.
2. 설정 화면 placeholder
   - 정리 완료: `CaptureSettingsScreen` 분리 화면 제거, 전체설정에서 카메라 설정 직접 조작으로 통합.
3. Coordinator/API 정리
   - `TableCounterSeedPolicy` scoped 전용 정리 완료.
   - `CaptureCounterPolicy` key 오버로드 private 축소까지 완료.

## 우선순위 제안
1. 실행 검증(컴파일/기기 회귀) 완료
2. 도메인 테스트 추가(스트림 전환/수동 override/reset)
3. 로그 포맷 표준화 및 문서 최종 마감


## 1단계 완료 (포맷 하드코딩 통합)
- `NamingFormatDefaults` 추가로 다음 기본값을 단일 소스로 관리:
  - 파일명 구분자(`FILE_NAME_DELIMITER`)
  - 기본 날짜/시간 포맷(`DATE_FORMAT_DEFAULT`, `TIME_FORMAT_CAPTURE_DEFAULT`)
  - 프리뷰/렌더 전용 시간 포맷
- `CameraScreen`, `TableEditorScreen`, `HomeScreen`, `SettingsRootScreen`, `TableRender`에서 하드코딩 문자열 제거.


## 2단계 완료 (촬영 후 counter 역파싱 정책화)
- `CaptureClickHandler`의 로컬 역파싱 함수를 제거하고 `CaptureNamingPolicy.parseUsedCounterFromDisplayName(...)`로 통합.
- 구분자(`fnDelim`)와 자리수(`counterDigits`)를 입력 받아 정책 기반으로 committed counter를 판별하도록 정리.


## 3단계 완료 (Coordinator API 슬림화)
- `TableCounterSeedPolicy`의 중복 오버로드를 제거하고 scoped stream 중심 API만 유지.
- 테이블 호출부는 이미 scoped stream만 사용 중이라 동작 변화 없이 유지보수 경로만 단순화.


## 설정 통합 완료 (CaptureSettings placeholder 제거)
- "촬영설정으로 이동" 버튼/분리 라우트를 제거하고, 전체설정(`SettingsRootScreen`)에서 즉시 조작하도록 정리.
- 사용자 입장에서 설정 동선이 1단계 단축되고 빈 placeholder 화면 진입이 사라짐.


## CaptureCounterPolicy 내부 오버로드 정리
- `CaptureStreamKey` 기반 오버로드는 내부(private) 구현으로 축소.
- 외부 카운터 read/commit 호출 표면은 `CaptureScopedCounterStream` 중심으로 유지.
