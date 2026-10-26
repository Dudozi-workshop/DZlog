# DZlog GitHub Actions 운영 기준 (2026-10-27)

## 기준과 범위
- Android 통합 브랜치: `integration/dzlog-20261010`.
- 이번 정리: 통합 브랜치의 워크플로우 18개 중 중복·완료된 기능별 QA 14개 제거, 재사용 가능한 4개 유지.
- 앱 소스, 테스트 코드, `.github/scripts/`, 실행 이력 및 다른 작업 브랜치의 파일은 변경하지 않는다.
- iOS M3-A PR #87 및 튜토리얼 관련 열린 PR과 그 브랜치의 워크플로우는 이번 대상에서 제외.

## 유지할 워크플로우
| 파일 | 역할 | 운영 |
| --- | --- | --- |
| `android-debug-apk.yml` | 통합 브랜치 단위 테스트·컴파일 및 업데이트 호환 APK 빌드 | 코드 push 시; 수동 실행에서는 `build_apk` 선택 |
| `dzlog-android16-smoke.yml` | Android 16 에뮬레이터 종합 스모크 | 수동 검증용 |
| `dzlog-postrelease-full-journey.yml` | 설치부터 촬영·앨범·백업까지 E2E 검증 | 수동 검증용 |
| `dzlog-v1-release-candidate.yml` | v1.0.0 출시 후보의 unsigned AAB 검증 | 릴리스 후보 브랜치 |

## 제거한 통합 브랜치의 워크플로우
- 단기 기능 QA: `dzlog-gallery-folder-focused-qa.yml`, `dzlog-quick-edit-window-qa.yml`, `dzlog-table-camera-sync-qa.yml`, `dzq009-quick-auto-qa.yml`, `dzq015-save-rule-copy-qa.yml`, `dzq018-focused-qa.yml`, `dzq020-focused-qa.yml`.
- 이전 온보딩·후속 통합 중복 QA: `dzlog-onboarding-first-camera.yml`, `dzlog-onboarding-qa.yml`, `dzlog-postrelease-integration-qa.yml`, `dzlog-postrelease-permission-smoke.yml`, `dzlog-postrelease-standalone-qa.yml`.
- 분기 전용 통합 QA: `dzlog-focused-tests.yml`, `tutorial-safe-qa.yml`. 이 파일들의 별도 iOS·튜토리얼 브랜치 사본은 유지된다.

## 운영 원칙
1. 단기 PR QA는 작업 브랜치에서 수행하고, 병합 뒤 필요 없는 전용 워크플로우는 통합 브랜치에서 제거한다.
2. 공통 회귀 테스트는 테스트 코드로 유지하고 반복 생성되는 기능별 YAML을 줄인다.
3. Android 기본 CI는 현재 통합 브랜치에만 자동 실행한다. 문서·워크플로우 정의만 수정하면 전체 앱 테스트를 반복 실행하지 않는다.
4. 설치·권한·촬영·갤러리·백업 같은 고비용 에뮬레이터 E2E는 필요 시 선택적으로 수행한다.
5. 워크플로우 정의 삭제는 테스트 코드나 과거 Action 실행 기록을 삭제하지 않는다.

## 별도 점검 필요
- 저장소 기본 브랜치는 `DZlog-260325`로 확인되었고 현재 Android 통합 브랜치와 다르다. GitHub의 `workflow_dispatch`가 화면에 표시되거나 정상 동작하려면 워크플로우가 기본 브랜치에 존재해야 한다. 기본 브랜치 변경 여부는 릴리스·기존 PR 기준 브랜치에 미치는 영향을 별도 검토한다.
- iOS와 튜토리얼용 열린 PR을 병합할 때 중복 검증 정의가 다시 도입되지 않도록 diff를 검토한다.
