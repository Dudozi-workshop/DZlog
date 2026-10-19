# M1-B B5 · integrated regression and .dzlog compatibility contract

검토 기준: B4 `858f44b90d6dab8114fd64533f27416e014e084c`.
B5는 테스트·합성 fixture·분석 문서만 추가한다. 앱 및 shared 운영 코드, 사진,
DB/설정, 기존 백업 형식은 변경하지 않는다. PR은 Draft·미병합으로 유지한다.

## 1. 목적과 검증 범위

B2 번호, B3 촬영 날짜/시간, B4 이름/논리 경로가 기존 Android orchestration에서
같이 작동하는지 확인한다. 기존 전체 회귀 검사를 재사용하고 중복 개별 테스트를
추가하지 않는다. 빠져 있던 백업→복원→촬영 계획 연결에만 테스트 2개를 추가한다.

- 고정 schema-1 합성 fixture: 여섯 셀 타입, ID/slot 참조, 5칸 슬롯과 null,
  행/열 비율, 전체 스타일, 날짜/시간 옵션, 셀별 순환 진행/연속 번호 정책.
- 원본 payload 문자열의 공백/줄바꿈이 checksum 대상임을 검증한다.
  JSON 값이 같아도 payload를 다시 직렬화하고 checksum을 그대로 두면 거절한다.
- 지원 모델 전체의 export→decode 동등성(기존 COUNTER seed=1 정책 반영).
- 기존 buildScope→buildCapturePreview를 호출하여 세 저장 방식의 여섯 셀 결과,
  파일명/경로/번호 patch/순환 진행 patch/원본 불변을 함께 확인한다.
- fixture는 Python 표준 JSON/hashlib로 독립 구성한 **합성 입력**이다.
  사용자 기기에서 내보낸 실제 백업 또는 Android encoder의 golden output이 아니다.
- iOS 백업 import/export 실행, 사용자 실제 백업, 실제 사진 저장, APK/기기 검수는
  이 테스트로 검증하지 않는다.

## 2. 현재 wire contract

코드 근거: `data/backup/LocalTemplateBackupCodec.kt`,
`LocalTemplateBackupValidator.kt`, `LocalTemplateBackupMerge.kt`,
`data/template/TableTemplateCatalog.kt`, `TableTemplateStateUtils.kt`.

| 영역 | 기존 계약 | iOS 공통화 시 보존 기준 |
| --- | --- | --- |
| 파일 | UTF-8 JSON, 확장자 `.dzlog`, 전체 2 MiB 이하 | 문자 수 대신 UTF-8 바이트 수로 제한 |
| 식별 | format=`dzlog-template-backup`, schemaVersion 정수 1 | 알 수 없는/새 버전 거절 정책 보존 |
| 메타 | createdAtMillis 비음수 정수, activeTemplateId nullable/존재 ID | Long 정밀도·null/빈 값 및 참조 판정 보존 |
| payload | 템플릿 배열을 담는 **문자열** | root의 배열 객체로 바꾸지 않음 |
| 체크섬 | payload 원문 UTF-8의 SHA-256, 소문자 hex 64자 | parse/re-serialize 전에 검증. root 전체가 대상은 아님 |
| 목록 | ID, 이름, modifiedAt, lastUsedAt, template, style | 배열 순서·지원 필드·참조 보존 |
| 표 | rows/cols, 모든 backing cells, 선택적 row/col weights | 병합 셀에 덮인 backing cell도 유효 |
| 셀 | 좌표/종류/타입/rawText/ID/span, typed counter seed, 날짜·시간 옵션, quickEditable, 스코프 옵션 | TEXT/NUMBER typed value는 rawText로 복원, 기타 자동 타입은 Auto |
| 순환문구 | sets/items/defaultEvery/counterProgressMode, phraseSetId/everyOverride/index/completedShots | 문자열 enum과 셀별 진행 보존, 기본값/정규화도 기존과 맞춤 |
| 저장 규칙 | 파일명/경로 각 최대 5칸, null 포함, CELL/MANUAL/FORMAT 및 참조/형식 | 기존 3칸은 5칸으로 null padding, 값과 순서 보존 |
| 스타일 | bgStyle/gridEnabled/textColorMode/manualTextColor/valueScale/textAlign/bgAlpha | 현재 validation 범위·parser coercion 보존 |
| 촬영 번호 | export에서 COUNTER typed seed만 1로 바꿈 | 원본 모델은 불변. 기존 rawText는 그대로임 |
| 제외 정보 | 사진, 갤러리 위치, 계정, 촬영 카운터 stream 저장소, 전체 앱 설정 | 전체 기기 백업으로 확대 해석하지 않음 |

단순한 byte-identical round-trip은 현재 앱의 계약이 아니다. JSON key 순서/공백은
재출력 시 달라질 수 있다. trim, blank/null 처리, enum fallback, slot padding,
seed 초기화 등 기존 정규화가 있다. 지원하지 않는 새 필드는 현 parser에서
유지되지 않는다. 따라서 미래/임의 필드까지 무손실이라고 주장하지 않는다.

## 3. 검증 및 복원 동작 경계

- 현재 validator는 archive 목록 1..500, 표 1..100행/열, cells 1..10,000,
  앵커 좌표 완전성, ID 중복·병합 범위, 가중치·참조·스타일 범위를 검사한다.
  모든 optional enum/slot kind를 엄격하게 검사하는 것은 아니다.
- schemaVersion/좌표/span/style 등은 JSON 숫자의 **정수 타입**도 검사한다.
  `1`과 `1.0`을 새 JSON 구현이 동일하게 취급해 호환성을 바꾸지 않도록 차등 검사한다.
- SHA-256은 손상 검출용이며 서명/인증은 아니다. payload와 digest를 함께 다시
  작성한 입력은 구조 validation도 통과해야 하며 무결성 검증만으로 신뢰하지 않는다.
- decode는 보존된 템플릿 ID를 읽는다. 실제 import merge는 새 템플릿 ID를 만들고
  기존 목록에 추가한다. 기존 템플릿/활성 선택을 덮어쓰지 않는다.
- 이름 충돌은 `(가져옴)`, `(가져옴 2)` 순서로 해소한다. 빈 목록에서만 archive의
  활성 참조를 새 ID로 매핑한다. 셀/문구 참조의 의미는 그대로 유지한다.
- Android repository는 SAF I/O와 DataStore 1회 transaction을 담당한다.
  현재 목록을 transaction 안에서 다시 읽고 전체 직렬화 성공 후 적용한다.
  이 정책은 iOS 저장 adapter에서도 보존해야 한다.

## 4. 재사용 설계와 남은 의존성

| 부분 | 현재 위치/의존 | 다음 작은 패치 |
| --- | --- | --- |
| 표·셀·스타일, 번호·날짜·이름 규칙 | shared commonMain + JVM/iOS actual | 기존 구현 재사용 |
| SavedTableTemplate 및 catalog JSON | Android, org.json/UUID/clock | 모델부터 공유하고 시간/ID 주입; 기존 JSON fixture로 동등성 검증 |
| 표 JSON parser/serializer | Android, org.json + UUID fallback | 같은 key/default/null/정규화·숫자 타입을 보존하는 adapter부터 검증 |
| archive checksum/validator/merge | Android, org.json/MessageDigest/UUID | 공통 정책과 JSON/SHA/ID adapter 분리; 기존 JVM 구현을 oracle로 유지 |
| backup I/O·영속화 | Android SAF/DataStore | iOS Files·영속화 adapter 별도, 정책 재사용 |
| TableResolver/ResolvePlan/PreviewPipeline/CaptureNamingPolicy | Android에 orchestration 잔존 | 모델과 순수 orchestration을 UI PoC에 필요한 범위부터 공통화 |

B5에서는 kotlinx.serialization 등 새 JSON 의존을 추가하거나 기존 codec을
교체하지 않는다. serializer를 고르기 전에 위의 기존 허용/거절·정규화 계약을
고정해야 한다. iOS codec이 생기면 같은 fixture를 Android/iOS에서 읽고 서로
내보낸 archive를 교차 복원하며 기존 JVM parser와 차등 검사한다.

## 5. 완료 판정

B5의 완료 범위는 통합 회귀 자동 검사와 codec 분석이다. B4의 shared 코드/테스트는
바이트 단위로 변경하지 않으므로 iOS 공통 41개 증빙은 B4 run
[39298930491](https://github.com/Dudozi-workshop/DZlog/actions/runs/39298930491)을
재사용한다. 이를 B5 HEAD에서 새로 실행한 iOS 검사로 표시하지 않는다.
B5 HEAD Android/shared 전체 CI 결과는 PR과 기존 Notion 기획 문서에 기록한다.

이 단계만으로 M1 전체 완료, iOS backup 구현, 공통 UI, 앱/실기기 검증을
완료 처리하지 않는다. 다음은 기존 업무 대장의 codec/순수 orchestration 공유
잔여 작업을 작은 패치로 처리하고 M2 UI PoC의 진입 조건을 확인하는 것이다.
병합·출시·배포는 사용자 승인 전 수행하지 않는다.
