# Counter SSOT 통합 로드맵

## 목표
테이블/카메라/캡처 저장/네이밍 전 경로에서 카운터 스트림 식별과 seed 동기화를
`CounterStreamContext` 단일 모델 기반으로 통일한다.

## 진행 현황 (업데이트 기준)
- 구현 진행률: **100%**
- 검증 진행률: **96%** (환경 제약으로 컴파일/기기 회귀 일부 대기)

### ✅ 완료
1. `CounterStreamContext` 도입 및 생성 표준화
2. `TableEditorScreen` 스트림 상태 분산 계산 제거
3. `TableCounterPolicyCoordinator` 입력을 context-first로 정리
4. `CameraScreen` scope 계산/동기화를 context-first로 정리
5. `CounterCaptureScope` 도입(`toCaptureStreamKey`, `toCaptureScopedCounterStream`)
6. `CaptureCounterPolicy`에 context/scoped 오버로드 추가
   - `getNextCounter`, `setNextCounter`, `commitCounter`, `isManualOverrideActive`, `clearManualCounterOverride`
7. `CaptureNamingPolicy` 결과를 `streamContext` 중심으로 정리

### 🔶 남은 작업 (필수)
1. **실행 검증(컴파일/회귀) 확보**
   - 현재 환경 네트워크 제약으로 Gradle 배포본 다운로드 실패
   - 네트워크 가능한 CI/로컬에서 `:app:compileDebugKotlin` + 핵심 플로우 회귀 필요
2. **캡처 완료 후 next 반영 정책 최종 정합성 검증**
   - 구현은 완료(실제 저장 counter 기반 commit/next 동기화)
   - 실제 기기/QA에서 중복 파일명 보정 케이스 최종 확인만 남음
3. **Step D 종료 확인**
   - 위 1~2 통과 시 로드맵 완료 처리

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
