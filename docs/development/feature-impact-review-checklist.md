# DZlog 기능 변경 시 연계 영향 점검

**적용 원칙:** 하나의 입력 유형·편집 옵션·저장 계약을 변경하면, 호출 화면만 수정하지 않고 생성 → 편집 → 미리보기 → 촬영 → 저장 → 재개 → 내보내기/복원까지 영향을 검토한다. 결과는 PR에 경로·테스트·미검증 사항을 적는다.

## 공통 점검표 (새 기능/수정 PR마다 수행)

- [ ] **데이터 모델:** enum/타입, 기본값, 허용 범위, null 의미, 안정 ID, 기존 값과의 호환성
- [ ] **표 상세설정:** 유형 선택, 생성, 편집, 삭제, 순서 변경, 셀 병합/복제, 기본값과 개별값
- [ ] **촬영 중 빠른 수정:** 같은 값의 노출·수정, 설정 우선순위, 선택 변경 후 현재 촬영 미리보기
- [ ] **촬영 파이프라인:** 촬영 전 해석값, 성공 후 진행, 실패/대기/취소/되돌리기, 앱 재시작 후 복구
- [ ] **파일명·경로·카운터:** 표 값과 실제 저장명·경로의 일치, 번호/범위 구분, 중복 파일 충돌
- [ ] **저장 구조:** JSON 왕복, DataStore/카탈로그, 구형 데이터 로딩, 가져오기/내보내기/백업 검증
- [ ] **연결 화면:** 홈, 갤러리, 최근 촬영, 템플릿 전환, 프로젝트 별 설정, 설정 미리보기
- [ ] **안내·튜토리얼:** 신규·기존 사용자의 설명, 튜토 데이터/가상 시연, 접근성 문구
- [ ] **화면 대응:** 작은 화면·큰 글씨·가로모드·IME, 제스처/3버튼 내비게이션, 탭 가능한 영역
- [ ] **플랫폼:** Android 변경이 iOS/KMP·웹 계약에 미치는 영향 (독립 작업 브랜치는 자동 병합하지 않음)
- [ ] **검증:** 경계값·회귀 테스트, 기존 자료 0/1/다량 상태, QA APK/실기기, 변경 전후 스크린샷

### 검토 결과 기록 형식

| 변경 계약 | 관련 위치/파일 | 검토 결과 | 테스트/실기기 증거 | 후속 담당 |
|---|---|---|---|---|
| 예: 신규 셀 타입 X | 모델/표편집/카메라/저장/백업 등 | 반영/영향 없음/미검증 | 테스트 또는 PR 링크 | 이슈·PR |

"영향 없음"이라도 해당 경로를 확인한 이유를 간단히 남긴다. 다른 브랜치의 미완료 기능은 자동 수정하거나 병합하지 않는다.

## PR #108 순환문구 변경 주기 연계 매핑

| 책임 | 구현/검토 위치 | 상태 |
|---|---|---|
| 세트 모델, 문구별 주기와 순서 보존 | `domain/model/RotatingPhraseSet.kt`, `domain/phrase/RotatingPhraseSetItemPolicy.kt` | 변경 |
| 문구 추가·이름 수정·개별/다중 기간 지정 | `ui/table/rotating/RotatingPhraseSetEditDialog.kt`, `RotatingPhraseSetListPanel.kt`, `RotatingPhraseItemEditDialog.kt`, `RotatingPhrasePeriodDialog.kt` | 변경, 실기기 확인 필요 |
| 기본 주기, 번호 방식, CSV 하단 시트 | `ui/table/rotating/RotatingPhraseSetFooter.kt`, `RotatingPhraseCsvActions.kt` | 변경, 실기기 확인 필요 |
| 표 편집 시 진행 초기화 정책 | `ui/table/mock/MockSaveRulesDomainAdapter.kt` | 검토·회귀 테스트 |
| 촬영 중 빠른 수정, 셀별 고정 우선 | `feature/table/policy/QuickAutoCellEditPolicy.kt`, `ui/camera/controls/CameraQuickAutoValueEditor.kt` | 검토·회귀 테스트 |
| 촬영 중 문구 선택/완료 장수 | `domain/phrase/CellPhraseProgress.kt`, `PhraseResolver.kt` | 검토·회귀 테스트 |
| 촬영 표시→파일명·경로·번호 | `domain/table/TableResolver.kt`, `domain/naming/NamePathBuilders.kt`, `domain/counter/CounterManager.kt` | 간접 경로 확인, 실기기 검수 필요 |
| 데이터 직렬화·복원 | `data/template/TableTemplateStateUtils.kt`, `data/repository/CaptureJobSnapshotCodec.kt` | 검토·회귀 테스트 |
| 전체 템플릿 백업 | `data/backup/LocalTemplateBackupCodec.kt`, `LocalTemplateBackupValidator.kt` | 신규 누락 발견 후 보완·회귀 테스트 |
| 2열/3열 CSV 왕복 | `domain/phrase/RotatingPhraseCsvCodec.kt` | 검토·회귀 테스트 |
| 번호 정책·촬영 취소/되돌리기 | `domain/counter/RotatingCounterStreamIdentity.kt`, `feature/capture/policy/CapturePhraseUndoHistory.kt` | 회귀 테스트 범위 추가 |
| 튜토리얼 문구 및 iOS 대응 | `feature/help/**`, iOS 전용 PR/브랜치 | **추후 별도 호환성 확인 필요** |

**안전한 마감 기준:** PR #108 집중 QA 통과 + 별도 설치형 APK + 제스처/3버튼 하단 터치, 키패드 입력, 문구 연속 추가, 200개 이상 검색·선택, 복원/재촬영 검수. 병합은 기존 독립 PR과 분리한다.
