# DZlog 카메라 실기기 QA 전 통합 체크리스트 (2026-10-09)

브랜치: `feat/table-editor-v2-ux-mock`

## Phase 2-B. 모서리 핸들 및 테스트

- [x] 표에서 촬영 영역 중앙에 가장 가까운 모서리 선택
- [x] 24dp 핸들 및 십자화살표, 드래그 중 모서리 고정
- [x] 핸들/슬라이더 같은 표 크기 상태 사용, 슬라이더 패널 88% 불투명도
- [x] `CameraResizeHandlePolicyTest`의 Android `RectF` 의존 제거: 좌표 정책 순수 Kotlin overload 추가
- [ ] *최종 HEAD* CI 단위 테스트 + APK 작업 success 확인
- [ ] 실기기: 3:4·9:16 및 0/90도 핸들 조작, 촬영 결과 비교

## Phase 2-C. 표 잠금 / 잠금 해제

- [x] 새 DataStore 키 `KEY_WM_TABLE_LOCKED` (기본 false)
- [x] `CameraSettingsWriter.setWmTableLocked` 및 `CameraPrefsEffect` 복원
- [x] 선택 도구바에서 잠금/해제 전환
- [x] 잠금 중 크기 슬라이더와 회전 비활성
- [x] 잠금 중 일반 표 이동 드래그와 핸들 리사이즈 차단; 잠긴 표에서 선택 테두리는 유지하고 편집 핸들 숨김
- [x] 상세 편집 및 촬영은 계속 사용 가능
- [ ] 실기기: 잠금→앱 재시작→해제 복원, 터치 이동 제한, 상세·촬영 확인

## Phase 3. 값 변경·저장 연결성 (코드 감사)

- [x] `CameraQuickValueSheet`는 TEXT/NUMBER만 편집하며 적용 시 `onTemplateChange` 호출
- [x] COUNTER/ROTATING_TEXT/DATE/TIME은 빠른 값 변경 대상에서 제외
- [x] 취소 시 draft 미적용, 변경 없으면 적용 버튼 비활성
- [x] 파일명·경로는 기존 템플릿 기반 저장 파이프라인 이용; 추가 카운터 증가 로직 없음
- [ ] 실기기: 값 변경 전/후 미리보기·저장 파일명·저장 경로 및 촬영 후 카운터 확인

## Phase 4. 카메라 UI 중첩/조작 (코드 감사)

- [x] 표 선택 시 상세·크기·회전·잠금 도구바 제공
- [x] 확대 슬라이더는 카메라 화면 위의 반투명 패널이며 셔터 위치 변경 경로 없음
- [x] 선택 해제 및 도구 메뉴 이동 시 오버레이 dismiss 경로 확인
- [ ] 실기기: 셔터, 최근 사진, 도구, 빠른 값, 보조 셔터 간 가림/터치 충돌
- [ ] 실기기: 키보드/바텀시트, 시스템 뒤로가기, 밝은 피사체에서 슬라이더 가독성

## Phase 5. 정적·자동·실기기 QA

- [ ] 코드 검사: Kotlin 타입 접근 범위, 새 콜백 call-site, 중복 매개변수
- [ ] 최종 HEAD에서 `:app:compileDebugKotlin` 통과
- [ ] 최종 HEAD에서 `:app:testDebugUnitTest` 통과
- [ ] 최종 HEAD에서 `:app:assembleDebug` 및 고정 키 APK 검증 통과
- [ ] APK artifact / SHA / versionCode 확인 후 테스트 사용자에게 제공
- [ ] 3:4·9:16, 0°·90°: 표시된 표 ↔ 촬영 저장 이미지 좌표·크기 일치
- [ ] 저장 모드 BOTH, 카운터/순환문구, 갤러리, 설정 영속성 회귀 확인
- [ ] 기존 앱 위 APK 업데이트 설치 및 데이터 유지 확인

## Phase C3-C5. 카메라 상호작용 및 촬영/Undo 통합 검수 (2026-10-09)

- [x] 촬영 설정의 중복 격자 버튼 제거, 저장/확인 명칭 간소화
- [x] 하단 버튼 패널 진입/선택 해제 및 직전 촬영 삭제 접근성 명칭 정리
- [x] 촬영 중 재입력 사운드·진동 중복 방지 가드
- [x] 결과 미리보기 뒤로가기 우선 처리 및 중복 전체 화면 터치 차단 레이어 제거
- [x] Undo 삭제 작업 진행 상태(권한 창·IO 작업 포함)와 버튼 비활성화 연결
- [x] Undo 빠른 연속 입력의 이중 세션 pop 방지: 컨트롤러에서 busy 확인 후 최신 촬영 확보
- [x] Android 10 복구 가능 삭제 권한 요청 승인 후 실제 ContentResolver.delete 재시도
- [x] Android 10 이하 다중 저장 파일 개별 삭제, 실패 파일만 Undo 스택에 복구
- [x] 삭제 결과를 순수 Kotlin `UndoDeletionOutcomePolicy`로 분리하고 성공/부분 실패/예외/빈 입력 테스트 추가
- [ ] 최종 HEAD CI 두 잡 모두 성공 확인 후 기록
- [ ] 기기에서 Undo 전체 성공/취소/부분 실패/연속 탭/삭제 승인 케이스 확인
- [ ] 기기에서 짧은 미리보기 자동 종료, 고정 미리보기 뒤로가기 종료 확인

### 코드 감사 중 확인된 별도 검증 포인트

- [ ] 저장 방식 즉시 변경 후 바로 촬영: `ui.prefs.saveMode`와 `appSettings.saveMode` 간 비동기 동기화 시점 확인. 특히 파일명·카운터 스코프 변경 전의 셔터 입력 회귀 검사
- [ ] 촬영 도중 설정 변경, 캡처 완료 후 카운터 readback 동기화 확인
- [ ] 시스템의 삭제 요청 결과가 앱 Activity 재생성/복귀 후에도 복구되는지 실기기 확인
- [ ] 실제 미디어가 일부만 삭제됐을 때 실패 파일 재시도 및 카운터 동기화 확인

실기기에서만 판정 가능한 항목은 코드 반영만으로 완료 처리하지 않는다.

## 운영 주의

- `pending`, `in_progress`, `cancelled` CI는 PASS가 아니다. 기능 코드 반영과 최종 QA 완료를 분리한다.
- 다른 팀의 갤러리·표 편집기 변경은 별도 검증 대상으로 유지한다.
- 수동 QA 전에 기록된 각 항목은 실제 실기기 테스트 완료를 의미하지 않는다.
- 오류 발생 시 [트러블슈팅 문서](./troubleshooting-camera-resize-visibility-20261009.md) 및 [QA Gate](./DZLOG_UPDATE_QA_GATE.md)에 로그·원인·수정 커밋 기록.
