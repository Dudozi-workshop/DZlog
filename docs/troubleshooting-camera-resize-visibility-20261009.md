# DZlog 트러블슈팅: 카메라 표 크기 핸들 Kotlin 가시성 컴파일 실패

- 발생일: 2026-10-09 (KST)
- 브랜치: `feat/table-editor-v2-ux-mock`
- 오류 도입·재현 커밋: `d9a99f811b956788d173d3bcfc1b040612042572`
- 수정 커밋: `90991001ef2fd7273264603528be8d60b27f4931`
- 오류 확인 CI: [Run 37877747124](https://github.com/Dudozi-workshop/DZlog/actions/runs/37877747124)
- 수정 검증 CI: [Run 37878224947](https://github.com/Dudozi-workshop/DZlog/actions/runs/37878224947) — 컴파일 성공, 별도 단위 테스트 4건 실패 후 실행 취소됨.
- 문서 보강 후 최신 CI: [Run 37878534039](https://github.com/Dudozi-workshop/DZlog/actions/runs/37878534039) — 결과 별도 확인.
- 분류: Kotlin 소스 컴파일 / API visibility / Compose Composable
- 영향: `:app:compileDebugKotlin` 실패 → 테스트·APK 검증 차단. 앱 배포나 저장 데이터 손상 여부는 이 오류로부터 추정하지 않는다.

## 1. 현상과 확정된 근거

기능 패치에서 표 크기 조절 핸들의 위치를 자동 전환하기 위해 `internal enum class ResizeHandleCorner`가 추가됐다. `WatermarkPreviewOverlay.kt`의 공개 Composable 함수가 해당 enum을 매개변수로 받으면서 Kotlin 컴파일 오류가 발생했다.

CI unit-test job의 실제 로그:

```text
WatermarkPreviewOverlay.kt:42:5 Function 'public' exposes its 'internal' parameter type 'ResizeHandleCorner'.
> Task :app:compileDebugKotlin FAILED
```

이 오류는 정적 접근 제한 위반이다. 카메라 센서, 이미지 해상도, 갤러리, 저장 규칙, 서명 키 문제와 혼동하지 않는다. debug-apk job 역시 결과를 별도로 확인해야 한다.

## 2. 원인 분석

1. 표 핸들 위치 정책을 `internal` 타입으로 캡슐화했다.
2. 호출부 `WatermarkPreviewOverlay`는 선언에서 접근 제한자를 생략해 기본값 `public`이 됐다.
3. Kotlin은 공개 함수가 그보다 접근성이 낮은 타입을 외부 API로 노출하는 것을 허용하지 않는다.
4. 관련 파일을 연속 커밋하면서 컴파일 완료 전에 다음 패치를 진행했다. 기존 [DZLOG_UPDATE_QA_GATE.md](DZLOG_UPDATE_QA_GATE.md)의 Gate 1과 Gate 2를 충족하지 못한 절차상 누락도 재발 가능 요인이다.

## 3. 조치

`app/src/main/java/com/dudoziworkshop/dzlog/ui/camera/preview/WatermarkPreviewOverlay.kt`:

```diff
 @Composable
-fun WatermarkPreviewOverlay(
+internal fun WatermarkPreviewOverlay(
```

`ResizeHandleCorner` 자체를 `public`으로 승격하지 않는다. Preview 전용 UI API는 해당 패키지/모듈 내부에서만 사용하는 것이 목적에 맞다. 변경 범위는 함수 접근 제한자에 한정하며, 크기 조절 상태·위치 계산·저장 로직은 수정하지 않았다.

## 4. 확인 및 상태 구분

- **확정:** 실패한 CI의 컴파일 에러 메시지와 해당 원인 확인.
- **확정:** 함수 가시성을 `internal`로 수정하는 커밋 생성.
- **컴파일 확인:** 수정 후 CI 로그에서 `:app:compileDebugKotlin` 단계가 통과함.
- **후속 실패:** 같은 실행의 `:app:testDebugUnitTest`에서 `CameraResizeHandlePolicyTest` 4건 실패. 따라서 CI 전체 성공은 아님.
- **미확정:** 최신 HEAD 기준 unit-test/debug-apk 동시 성공 및 설치·촬영 동작. 실행 취소를 PASS로 기록하지 않는다.
- **별도 실기기 QA:** 왼쪽 위 표 → 오른쪽 아래 핸들, 오른쪽 아래 표 → 왼쪽 위 핸들, 손가락 드래그 중 핸들 위치 고정, 슬라이더↔핸들 동기화, 촬영 전후 좌표 일치.

## 4.1. 후속 검증에서 발견된 별도 문제 (같은 날짜)

`90991001`에서 **기존 접근 제한 컴파일 오류는 해소**됐으나, [CI 37878224947](https://github.com/Dudozi-workshop/DZlog/actions/runs/37878224947)의 unit-test 로그에 다음 문제가 남았다.

```text
:app:compileDebugKotlin   # success
CameraResizeHandlePolicyTest > topLeftTablePointsToBottomRightHandle FAILED
CameraResizeHandlePolicyTest > topRightTablePointsToBottomLeftHandle FAILED
CameraResizeHandlePolicyTest > bottomLeftTablePointsToTopRightHandle FAILED
CameraResizeHandlePolicyTest > bottomRightTablePointsToTopLeftHandle FAILED
258 tests completed, 4 failed
> Task :app:testDebugUnitTest FAILED
```

- 네 테스트는 모두 `android.graphics.RectF`를 직접 생성하는 로컬 JVM 단위 테스트이며, 로그에는 테스트 위치의 `java.lang.RuntimeException`이 나타난다.
- **가능성 높은 원인:** Android SDK의 그래픽 클래스가 Android 런타임/적절한 mock 환경 없이 로컬 JVM에서 호출됨. 단, 보고서의 전체 예외 메시지 확인 전까지 확정 원인으로 선언하지 않는다.
- **권장 조치:** 좌표 선택 정책을 Float 좌표를 받는 순수 Kotlin 함수로 분리해 JVM에서 검증하고, `RectF` 어댑터는 별도 Android 기반 테스트 또는 적절한 프레임워크 환경으로 확인한다.
- **추가 재발 방지:** JVM `src/test`에서 Android 프레임워크 객체를 직접 생성할 때는 mockability를 검토한다. 최초 단위 테스트 실패부터 수정 완료까지 CI는 `PASS`로 표기하지 않는다.
- **상태:** 후속 테스트 오류 미해결/별도 패치·CI 재검증 필요. 본 문서화 작업은 소스 또는 테스트 코드를 바꾸지 않는다.

## 5. 재발 방지 실행 규칙

### Gate 1 — 파일 변경 즉시 (새 파일 포함)
- [ ] `fun`, `class`, `enum class`, `data class`의 기본 `public`을 확인한다.
- [ ] 함수 매개변수·반환형·프로퍼티 타입이 공개 API보다 더 제한적으로 선언되지 않았는지 점검한다.
- [ ] 모듈 내부에서만 쓰는 Composable / policy / DTO는 가능한 `internal`로 명시한다.
- [ ] 공개 API가 필요한 경우 타입 공개 여부와 모듈 간 사용 필요성을 함께 검토한다.
- [ ] 새 파일 참조, import, 콜백 이름, 호출부, data/state 흐름을 점검한다.
- [ ] 관련 코드 1~3파일 단위 커밋 후 인접 사용처에 누락이 없는지 검토한다.

### Gate 2 — CI 검증
- [ ] 최신 브랜치 HEAD의 `:app:compileDebugKotlin` 성공.
- [ ] 같은 HEAD의 unit-test job 성공.
- [ ] 같은 HEAD의 debug-apk job 성공 및 실제 APK artifact 생성 확인.
- [ ] 실패 시 해당 **run의 정확한 job log**와 첫 Kotlin `e: ...` 오류를 확인한다.
- [ ] `cancelled`, `pending`, `in_progress`는 `success`로 표기하지 않는다.
- [ ] 동일 기능 내 누적 패치는 Gate 1 검토 후 진행하되, 최종 묶음은 Gate 2 및 실기기 확인 전까지 '검증 대기'로 표기한다.

## 6. 추후 운영

- 기능이 정상 동작했다는 실기기 피드백과 GitHub Actions 코드 검증은 별도 게이트로 관리한다.
- 저장 규칙·카운터 기능과의 충돌은 변경 파일과 인접 사용처를 기준으로 확인한다.
- CI 실패 원인이 서명 키·의존성·컴파일 중 어디에 있는지 실제 로그로 판정하고, 추측을 원인으로 기록하지 않는다.
- 신규 문제는 이 문서와 동일하게 **현상 → 근거 → 원인 → 조치 → 검증 → 재발 방지** 순으로 기록한다.
