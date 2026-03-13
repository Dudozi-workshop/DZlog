# Counter SSOT 통합 로드맵

## 목표
테이블/카메라/캡처 저장/네이밍 전 경로에서 카운터 스트림 식별과 seed 동기화를
카운터 read/commit 경로는 `CaptureScopedCounterStream` 기준으로 통일하고,
네이밍/스코프 계산 경계는 `CounterScope` + `scanPrefix` 기준으로 정리한다.

## 진행 현황 (업데이트 기준)
- 구현 진행률: **100%**
- 검증 진행률: **99%** (환경 제약으로 컴파일/기기 회귀 일부 대기)

### ✅ 완료
1. `CaptureScopedCounterStream` 도입 및 read/commit 경로 생성 표준화
2. `TableEditorScreen` 스트림 상태 분산 계산 제거
3. `TableCounterSeedPolicy` 입력을 context-first로 정리
4. `CameraScreen` scope 계산/동기화를 context-first로 정리
5. `CounterCaptureScope` 도입(`buildStreamKey`, `buildScopedCounter`)
6. `CaptureCounterPolicy`에 context/scoped 오버로드 추가
   - `resolveNext`, `setNext`, `commit`, `hasManualOverride`, `clearManualOverride`
7. `CaptureNamingPolicy` 결과를 `counterScope + scanPrefix` 중심으로 정리
8. A단계 반영: `TableEditorScreen` 스코프 계산을 AppSettings(`includePathInCounterScope`, `includeFilenameInCounterScope`) 기반 scoped stream으로 통일
9. B단계 반영: 미사용 이전 설계 경로 `CaptureNamingPolicy.buildForCapture(...)` 제거
10. D단계 점검: 잔존 항목을 `docs/counter-ssot-debt.md`로 분리 정리
11. 1단계 반영: 파일명/카운터 포맷 기본값(`fnDelim`, `dateFormat`, `timeFormat`) 하드코딩을 `NamingFormatDefaults` 단일 소스로 통합
12. 2단계 반영: 촬영 완료 후 committed counter 역파싱을 `CaptureNamingPolicy.parseUsedCounterFromDisplayName(...)` 정책 함수로 통일
13. 3단계 반영: `TableCounterSeedPolicy` API를 scoped stream 중심으로 슬림화(중복 `CounterScope`/context 계열 오버로드 제거)
14. 설정 통합 반영: `CaptureSettingsScreen` 분리 진입 제거, 전체설정에서 카메라 설정 즉시 조작으로 통일
15. 정책 API 정리: `CaptureCounterPolicy` 내부 `CaptureStreamKey` 오버로드를 private으로 축소해 외부 표면 단순화
16. 레거시 마이그레이션 정리: `CounterManager`의 legacy marker 기반 이전 분기 제거, stream 키 + MediaStore 재스캔 기반으로 단순화

### 🔶 남은 작업 (필수)
1. **실행 검증(컴파일/회귀) 확보**
   - 현재 환경 네트워크 제약으로 Gradle 배포본 다운로드 실패
   - 네트워크 가능한 CI/로컬에서 `:app:compileDebugKotlin` + 핵심 플로우 회귀 필요
2. **캡처 완료 후 next 반영 정책 최종 정합성 검증**
   - 구현은 완료(실제 저장 counter 기반 commit/next 동기화)
   - 수동으로 큰 값 지정 시 촬영 전 메뉴얼 유지 + 촬영 후 오토 복귀 정책 반영 완료
   - 실제 기기/QA에서 중복 파일명 보정 케이스 최종 확인만 남음
3. **파일 삭제 이후 stale 인덱스 동기화 검증**
   - 구현 반영: DB 인덱스와 MediaStore 스캔값이 다르면 현재 파일 기준으로 스트림 인덱스 재동기화
   - 기기에서 삭제/복원 후 next counter 및 오토/메뉴얼 라벨 일치 여부 확인 필요
4. **표 상세설정 진입 시 stale seed 자동 동기화 검증**
   - 구현 반영: 수동 모드가 아니면 상세설정/파일명 미리보기 카운터를 streamNext(실제값)로 강제 동기화
5. **Step D 종료 확인**
   - 위 1~4 통과 시 로드맵 완료 처리

### 🔹 남은 작업 (부수)
1. 로그 키/포맷 표준화(디버그 가독성)
2. 도메인 단위 테스트 추가(스트림 전환, manual override, reset)
3. 문서화 보강(운영/QA 체크리스트와 연결)

## 단계 정의
- Step A (완료): 모델 통합
- Step B (완료): 호출부 인자 축소
- Step C (완료): 정책 API 대칭화
- Step D (진행중): 실행 검증/회귀 안정화

## 완료 조건 (Definition of Done)
1. 컴파일 검증 통과
2. 테이블/카메라에서 스트림 전환, 수동 seed, reset, 연속 촬영 시 카운터 일관성 확인
3. 회귀 테스트(또는 최소 스모크) 문서화 완료
