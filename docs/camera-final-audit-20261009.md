# 카메라 최종 코드 감사 · 2026-10-09

## 범위와 기준

- 저장소: Dudozi-workshop/DZlog
- 브랜치: feat/table-editor-v2-ux-mock
- 최초 GitHub HEAD 재조회 결과: 0e26e5f0dc7a2523adb10d2a71736a879b77fef3 (인계 기준과 일치).
- 카메라만 감사·수정. feat/gallery-v2, feat/table-detail-v2 병합 없음.
- 기존 실기기 통합 QA 15/15는 6a479e2 기준 사용자 확인 이력이다. 이후 UI 패치 및 이번 자동 검증의 실기기 PASS를 뜻하지 않는다.

## 검토 파일과 판정

| 파일/경로 | 검토 내용 및 판정 |
| --- | --- |
| ui/camera/CameraScreen.kt, state/CameraDerivedState.kt, CameraUiState.kt | 화면 연결, 계산 입력, 촬영 가능 조건. 이전 조회 조건의 카운터를 새 파일명/경로에 재사용할 수 있는 구간 보완. |
| domain/preview/PreviewPipeline.kt, domain/capturepolicy/CaptureContext.kt, CaptureNamingPolicy.kt, domain/naming/NamePathBuilders.kt | 파일명·경로·번호·문구의 계산 기준. 저장 요청은 finalCapturePreview 값을 그대로 사용. 별도 엔진 재작성 필요 없음. |
| ui/camera/effects/CameraNowTickEffect.kt, domain/preview/TemplateTickUnit.kt, PreviewTick.kt | 날짜만 있는 표에서 DAY 갱신은 독립 FORMAT TIME 슬롯과 한국 자정 갱신에 부적합. 화면 복귀 즉시 갱신도 누락. 카메라 한정 분 경계 갱신으로 보완. |
| feature/counter/camera/CameraCounterSyncEffect.kt, CameraCounterSync.kt, feature/counter/core/CounterRequestResolver.kt, CounterFacade.kt, CounterSyncDecider.kt | read/commit 스트림 및 Undo 하향 재조회. 번호 조회를 완료한 CounterRequest를 유지하고 현재 조건과 일치할 때만 최종 촬영 preview 허용. 기존 증가·수동 번호·Undo 계산 정책 보존. |
| domain/counter/CounterManager.kt, CounterStore.kt, domain/capturepolicy/CaptureCounterPolicy.kt, data/counter/CounterParsingUtils.kt | MediaStore max+1 및 Room 보조기록 확인. 기존 정책 유지. |
| ui/camera/controls/CameraTriggerCapture.kt, CaptureClickHandler.kt, CameraBottomControls.kt | 셔터/음량키/보조 셔터 공용 실행 경로, 저장 성공 후 진행 상태 반영, 중복 촬영 gate. Undo 삭제와 촬영의 동시 실행 차단. |
| ui/camera/effects/CameraUndoDeleteController.kt, feature/capture/policy/UndoCapturePolicy.kt, UndoDeletionOutcomePolicy.kt | Android 삭제 확인·부분 실패·세션 stack 복원과 삭제 완료 이벤트 확인. 기존 정책 유지. |
| data/repository/DzlogRepositoryImpl.kt, CaptureMediaSaveCoordinator.kt, data/mediastore/MediaStoreSaverImpl.kt | ORIGINAL_ONLY/WATERMARK_ONLY/BOTH의 물리 저장, 부분 저장 보상, 이름 충돌 처리. 안내 경로와 현재 저장 정책 일치. 엔진 변경 없음. |
| ui/camera/presenter/CameraPreviewAreaArgsPresenter.kt, preview/CameraPreviewArea.kt, WatermarkPreviewOverlay.kt, watermark/WatermarkRenderUseCase.kt, WatermarkRendererImpl.kt | 날짜·번호·문구의 동일 resolver 사용, 표 비율·좌표 계산 연결. 관련 기존 회귀 테스트 재사용. |
| ui/camera/controls/CameraTopBar.kt, CameraNextCaptureInfoPopup.kt, ui/common/CounterAwareFileNameText.kt | 긴 파일명에서 suffix 폭 예약, 확장자 숨김, 팝업 back/외부 탭/저장설정 진입. 숫자만 색 강조하고 조회 전 날짜·수동 숫자 오인 방지. 공용 컴포넌트의 기본(non-exact) 호출 동작 유지. |
| ui/camera/effects/CameraLatestImageEffect.kt, ui/log/RecentCaptureNavigation.kt | 최근 사진 조회 여러 coroutine의 늦은 결과가 새 폴더/모드 결과를 덮을 수 있음. 단일 LaunchedEffect로 취소/갱신을 관리하고 조회 중 이전 썸네일 해제. 앨범 UI 변경 없음. |

## 패치 구성

각 커밋은 1~3파일로 유지한다.

1. 시간 갱신: CameraNowTickEffect + 순수 cameraNowTickUnit + 테스트 (3파일).
2. 카운터 조회 조건 보관: CameraUiState + CameraCounterSyncEffect (2파일).
3. 현재 조건 일치 검사: CameraDerivedState + CameraScreen + 회귀 테스트 (3파일).
4. 촬영/Undo 동시 실행 차단: CameraScreen + CameraTriggerCapture + CameraBottomControls (3파일).
5. 자동번호 강조 정확성: CameraScreen + CameraTopBar + CounterAwareFileNameText (3파일).
6. 최근 사진 조회 경쟁 제거: CameraLatestImageEffect (1파일).
7. 날짜·순환문구 연결 회귀 테스트 보완 및 본 감사 기록 (2파일).

촬영·저장 엔진, 사진 화질·해상도, 표 합성, 파일명 규칙, 번호 계산, 갤러리·표 상세 UI는 변경하지 않는다.

## 자동 검증과 한계

- 신규 시간 정책 테스트 4개: 날짜만 있는 표, 한국 자정, 빈 표의 FORMAT 슬롯, 초 옵션 호환.
- 카메라 파생 상태 테스트 10개: 일치/대기/폴더/파일명/3개 모드/스코프 옵션/시간 경계/번호 전진·Undo 값/문구·경로 연결/날짜 경계.
- 관련 기존 naming/counter/phrase/render/save/Undo 테스트는 전체 :app:testDebugUnitTest에서 함께 실행.
- 시간 패치 d7d40d3: Actions 37934418358의 unit-test 및 debug-apk SUCCESS 확인.
- 조회 조건 보관의 중간 CI는 후속 연결 커밋에 의해 취소됨. 취소를 실패 또는 성공으로 기록하지 않는다.
- 후속 연결 커밋 및 최종 패치의 테스트·APK 완료 결과와 최종 HEAD 일치는 Notion 개발 이력/현재 구현 상태에 기록한다.
- 로컬 Gradle wrapper 실행은 배포 파일 다운로드 네트워크 제한으로 시작하지 못했다. 로컬 테스트 PASS로 기록하지 않는다. Android 빌드·테스트는 GitHub Actions JDK21/SDK34 환경으로 검증한다.
- 실제 Android 화면 복귀, back/IME, 음량키, 터치, APK 설치 및 시각 배치는 이번에 실기기에서 수행하지 않았다.

## 수정하지 않은 위험·개선 후보

- P2: 사용자 저장경로 마지막 항목을 original로 직접 지정하면 기존 엔진은 원본 하위 폴더와 동일하게 취급한다. BOTH의 두 출력이 같은 폴더에서 이름 충돌 보정될 수 있다. 예약 폴더명/별도 출력 경로의 정책 결정이 필요하며 이번에 기존 저장 엔진을 임의로 바꾸지 않는다.
- P2: BOTH에서 각 폴더의 기존 충돌 상황이 다르면 원본/합성 파일의 충돌 보정 이름이 달라질 수 있다. 두 출력은 동일 세션 URI 묶음으로 Undo 되며 기본 촬영명은 동일하다. 공통 이름 예약은 저장 엔진의 후속 정책 검토 대상.
- P2: 많은 파일에서 카운터 MediaStore 조회의 비용과 화면 반응성은 기기 데이터량으로 별도 측정할 필요가 있다. 이번 정확성 패치에 성능 재설계를 섞지 않는다.
- P3: CameraScreen은 약 600줄이지만 prefs/counter/preview/controls/Undo/controller 책임은 이미 별도 파일에 분리되어 있다. 현재 결함 해결을 위해 추가 분할은 필요하지 않다.
- P3: CameraPrefsState의 saveMode/photoQualityMode mirror, CameraPrefsEffect의 초기/RESUMED 중복 읽기, CameraPreviewArea의 사용하지 않는 remember(plan) Boolean 및 legacy naming wrappers는 정리 후보. 현재 동작 오류의 근거가 없어 제거하지 않는다.

## 최소 실기기 확인

기존 15항목을 반복하지 않는다. 최종 자동 검증 성공 후 다음만 묶어서 확인한다.

1. 날짜 셀과 파일명 시간 항목을 사용하는 카메라를 1분 이상 백그라운드에 두고 복귀: 파일명/표/정보 경로의 현재값, 촬영 1회 및 다음 번호, Undo 후 값 확인.
2. 긴 파일명으로 상단 끝 번호 보존·숫자만 색 강조 확인. 팝업에서 외부 탭/뒤로가기 및 저장설정→카메라 복귀 확인.
3. 폴더/저장 방식 변경 후 최근 사진의 대상 앨범 및 조회 완료 전 촬영 차단 확인. 저장 또는 Undo 처리 중 다른 촬영/Undo 입력이 겹치지 않는지 확인.

카메라 코드·자동검증 마감과 실기기 최종 승인, 다른 브랜치 통합/앱 전체 출시 판정은 구분한다.
