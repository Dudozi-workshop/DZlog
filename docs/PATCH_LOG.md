# DZlog 개발 패치 이력

> **목적:** 버전별 출시 공지가 아닌, 실제 병합된 소규모 개발 변경사항을 PR 단위로 추적합니다.  
> **대상:** Android `integration/dzlog-20261010` (iOS는 해당 통합 브랜치 변경이 확인된 시점부터 기록)  
> **주의:** 통합 브랜치에 병합됐다는 사실은 Google Play 사용자에게 배포됐다는 뜻이 아닙니다.  
> **시간:** 한국 표준시(KST, UTC+9) / 최신 병합부터 표시

## 기록 기준

1. **PR 1건 = 패치 기록 1건.** PR이 실제 통합 브랜치에 병합된 후 기록합니다. Draft·미병합 PR은 이력에 포함하지 않습니다.
2. **변경 전 → 변경 후를 짧게 기록.** 기능 추가, 수정, 오류 해결 등 사용자에게 보이는 차이 위주로 1~2문장만 작성합니다.
3. **병합일·PR 링크·병합 커밋을 남깁니다.** 상세 구현 및 테스트 근거는 PR에서 확인합니다.
4. **버전 번호와 분리합니다.** 배포 시에는 해당 버전에 포함된 패치들만 골라 Google Play 업데이트 설명을 별도로 만듭니다.
5. **새 패치 병합 후 이 문서에 한 줄을 누적합니다.** 기존 기록은 삭제·덮어쓰기하지 않고 최신 항목을 맨 위에 추가합니다. 이는 작업 절차이며 자동 생성 시스템은 아닙니다.

## 2026년 11월

| 병합일 (KST) | PR | 영역 | 변경 전 → 변경 후 | 병합 커밋 |
| --- | --- | --- | --- | --- |
| 11.13 19:00 | [#113](https://github.com/Dudozi-workshop/DZlog/pull/113) | 표 편집·순환문구 | 분산된 설정·큰 선택창 → 컴팩트 통합 UI·하단 셀 이동 유지. 셀 종류를 바꾼 뒤 돌아와도 유형별 입력·문구 상태를 복원하도록 개선 | [`83bfaf0`](https://github.com/Dudozi-workshop/DZlog/commit/83bfaf0032e4d6cae5d32b1cdf319425006e18f7) |
| 11.13 10:39 | [#108](https://github.com/Dudozi-workshop/DZlog/pull/108) | 순환문구 | 세트 기본 주기 중심 → 문구별 주기·셀별 고정 주기 지원, CSV에 주기 열 추가 및 이전 형식 호환 | [`a8f66f3`](https://github.com/Dudozi-workshop/DZlog/commit/a8f66f3122bfca5465589293e84f601dc074eca9) |
| 11.13 10:39 | [#111](https://github.com/Dudozi-workshop/DZlog/pull/111) | 카메라·촬영 정보 | 촬영 정보 팝업 중심 → 프로젝트·템플릿 목록으로 직접 이동하고 기존 화면으로 복귀. 순환문구 편집 화면 구성도 정리 (일부 #108 내용 통합) | [`1e405ec`](https://github.com/Dudozi-workshop/DZlog/commit/1e405ec959c23c8ef0c45760317e729b39ace239) |
| 11.13 09:46 | [#112](https://github.com/Dudozi-workshop/DZlog/pull/112) | 카메라 | 초점·밝기 조절 접근이 제한적 → 터치 초점·AF/AE 잠금·노출 보정과 작은 링 UI 지원 | [`041e78a`](https://github.com/Dudozi-workshop/DZlog/commit/041e78afecfd789315d6eb47d337fb52f36997b7) |
| 11.08 18:52 | [#107](https://github.com/Dudozi-workshop/DZlog/pull/107) | 빠른 수정 | 선택 셀의 역할 식별이 어려움 → 유형 아이콘을 왼쪽, 파일명·저장경로 연동 아이콘을 오른쪽에 표시 | [`9030ae6`](https://github.com/Dudozi-workshop/DZlog/commit/9030ae6897649b290b385c475d51560fe09a6707) |
| 11.07 19:40 | [#105](https://github.com/Dudozi-workshop/DZlog/pull/105) | 프로젝트·갤러리 | 일반 촬영 흐름 중심 → 프로젝트별 이어서 촬영, 사진 구분·가상 앨범 및 이동·삭제 후 연결 유지 | [`979e97a`](https://github.com/Dudozi-workshop/DZlog/commit/979e97ae4c4af3c12b0ddee985ae2386016d0074) |
| 11.07 18:21 | [#100](https://github.com/Dudozi-workshop/DZlog/pull/100) | 표 편집 | 표 V2·카메라 렌더링 간 차이 및 구 V1 잔존 → 표현 규칙 통일, 사용하지 않는 V1 코드 정리 | [`dd29a80`](https://github.com/Dudozi-workshop/DZlog/commit/dd29a80b021a29a740b16b0e3ea363f435b12486) |
| 11.07 18:03 | [#104](https://github.com/Dudozi-workshop/DZlog/pull/104) | 순환문구 | 문구 목록의 개수·검색 확인이 불편 → 총 개수 표기와 검색 필터 추가 | [`9ef6092`](https://github.com/Dudozi-workshop/DZlog/commit/9ef60929e31bb0c98c33eb17f55d93e6fd5eee1a) |
| 11.07 17:11 | [#103](https://github.com/Dudozi-workshop/DZlog/pull/103) | 순환문구 | 앱 내부에서만 문구 세트 관리 → CSV 파일 가져오기·내보내기와 교체 미리보기 지원 | [`699a2bc`](https://github.com/Dudozi-workshop/DZlog/commit/699a2bc7429ce0cdaab62516e489729b878468f9) |
| 11.07 16:59 | [#102](https://github.com/Dudozi-workshop/DZlog/pull/102) | 카메라 빠른 수정 | 순환문구 조정 항목 부족 → 세트·문구·변경 주기·완료 장수 조정 제공 | [`6e3910a`](https://github.com/Dudozi-workshop/DZlog/commit/6e3910a26679776e25047f2bc707ea4c4faee59e) |
| 11.07 16:19 | [#101](https://github.com/Dudozi-workshop/DZlog/pull/101) | 카메라 빠른 수정 | 일부 텍스트·숫자 셀 수정 제한 → 기존 잠금 플래그와 무관하게 수정 가능 (자동번호 보호 유지) | [`f6e5ef9`](https://github.com/Dudozi-workshop/DZlog/commit/f6e5ef99679e47226762bb0a80699220e18e552d) |
| 11.01 15:10 | [#99](https://github.com/Dudozi-workshop/DZlog/pull/99) | 배포·의존성 | 사용하지 않는 광고 SDK 포함 → 광고 SDK 및 관련 권한·초기화 제거, 출시 코드 `versionCode 4` 준비 | [`86b38a7`](https://github.com/Dudozi-workshop/DZlog/commit/86b38a7e6901e8ad3c2efd6e8502786c4512848e) |

### 검증 참고

- **#113:** Android Kotlin 컴파일·전체 단위 테스트 및 병합 후 통합 CI 성공 ([CI](https://github.com/Dudozi-workshop/DZlog/actions/runs/43405771220)). 화면의 최종 실기기 QA는 별도 확인 대상입니다.
- **#105:** 프로젝트 촬영의 기존 주요 경로 실기기 QA 6/6 완료. PR 병합 시점 신규 UI의 별도 실기기 최종 QA 여부는 PR 본문을 확인해야 합니다.
- **#108 · #111:** 기능이 일부 중복 포함되어 있어 이후 배포 설명을 작성할 때 같은 기능을 중복 집계하지 않습니다.
- 그 외 패치의 상세 빌드·기기 검증 범위는 각 PR 본문과 GitHub Actions 실행 기록을 기준으로 확인하며, 병합 자체를 실기기 QA 완료로 간주하지 않습니다.

## 다음 기록 템플릿

새 PR을 병합한 다음 아래 한 줄을 해당 월의 맨 위에 추가합니다.

```text
| MM.DD HH:mm | [#PR](PR_URL) | 작업 영역 | 변경 전 상태 → 변경 후 상태 | [병합 SHA](COMMIT_URL) |
```

**작업 흐름:** 작은 PR 구현 → 변경 전·후 및 검증 사항을 PR에 작성 → CI/QA 확인 → 통합 브랜치 병합 → 이 문서에 한 줄 추가. 정식 버전명·배포 안내는 별도 출시 결정이 있을 때만 갱신합니다.
