# M1-D · shared capture planning

기준: 검증된 M1-C `b5986988bb55c892dbd9689fd0f7352258dfb1ef`.

- TableResolver의 여섯 셀 해석을 TableValueResolver로 이동한다. B2 번호 정책과
  B3 날짜/시간 정책을 그대로 호출하고, 기존 Android TableResolver(Date/Locale)는
  shared JVM의 얇은 facade로 유지한다.
- ResolvePlan/TablePatch/applyPatch, CaptureContext/CaptureNamingPolicy,
  PreviewPipeline과 순환문구 filename 정책을 commonMain으로 이동한다.
- 공통 입력의 촬영 시각은 CaptureTime(epoch/locale/timezone)이다. Android의
  기존 Date 생성 호출은 JVM factory overload로 유지하며 명시적 TableResolver를
  넘기는 카메라 호출도 JVM overload로 연결한다. CameraPreviewArea는 Date형
  filename helper extension import 한 줄만 추가한다.
- 번호 조회 전 scope 계획과 동기화 후 final 계획, 저장 성공 후 적용할 patch를
  분리하는 기존 정책을 유지한다. common plan 계산은 영속화/사진 저장을 하지 않는다.
- 템플릿 편집→표/파일명/폴더 반영 기존 테스트3개를 commonTest로 이동한다.
  새 native/JVM 통합1개는 고정 schema1 백업→복원→6종 셀→촬영 계획→반환 patch
  적용을 ko-KR/en-US·세 저장 방식으로 검증한다. Android B5 Date API 통합2개 및
  기존 단위 테스트는 그대로 재사용한다.

플랫폼 경계:

- MediaStore 실제 파일 조회/max+1 집계·DataStore·Room·카메라·Bitmap·실제 저장·Undo
  영속화/UI 화면은 기존 Android 구현을 유지한다.
- iOS Files import/export, 카메라/저장/권한·앱 UI·실기기는 아직 후속이다.
- 입력 생성 시 locale/timezone을 한 번 snapshot하므로 공통 계산 전체가 같은 촬영
  시각 문맥을 사용한다. JVM 개별 TableResolver 호출의 명시적 Locale도 유지한다.
- M1 공통 코어의 순수 계획/codec 자동 검증과 전체 앱 동등성/출시 완료는 구분한다.

최종 HEAD의 전체 Android/shared 및 iOS compile/simulator 증빙은 PR/기존 Notion에서
기록한다. Draft·미병합 유지, 기존 PR 및 Android 통합/출시 기준 보호.
