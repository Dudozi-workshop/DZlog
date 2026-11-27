# iOS M3-A 클라우드 실기기 QA 준비

2026-11-07 KST. 직접 사용할 Mac·iPhone과 유료 Apple Developer 계정이 없는 경우의 대체 경로다.

## 준비 결과

- CI가 실제 iPhone용 `arm64-apple-ios` 앱과 KMP 프레임워크를 빌드한다.
- 기존 unsigned 진단 ZIP은 그대로 보존한다.
- 클라우드 실기기 서비스가 다시 서명할 수 있도록 `Payload/DZlogUiPoc.app` 구조의 `dzlog-ios-device-CLOUD-RESIGN.ipa`를 추가 생성한다.
- 이 IPA에는 Apple 인증서·계정·프로비저닝 정보가 없으며 일반 iPhone에 직접 설치할 수 없다.

## 권장 경로

1. AWS Device Farm의 public iOS device 또는 BrowserStack App Live/App Automate 계정을 준비한다.
2. 최신 iPhone device workflow artifact에서 `dzlog-ios-device-CLOUD-RESIGN.ipa`를 내려받아 업로드한다.
3. 서비스의 자동 재서명을 사용해 실제 iPhone 원격 세션을 시작한다.
4. 아래 QA를 수행하고 화면 녹화·스크린샷·기기 로그를 보존한다.

## 실기기 QA

- 카메라·사진 권한 최초 허용, 거부, 설정 복귀와 권한 철회
- 실제 카메라 촬영과 표 합성, 원본/결과/BOTH의 1/1/2개 저장
- 초점, 줌, 플래시, 세로·가로 회전과 표 위치·크기
- 저장 도중 앱 종료 후 재실행·복구, 중복 사진·번호 증가 여부
- 갤러리 폴더·목록·썸네일·원본/결과 전환과 외부 삭제 반영
- 빠른 입력 취소·완료, 한글 조합, 숫자 형식, 카메라 복귀

## 한계

- 클라우드 기기의 카메라는 장비 거치 상태 때문에 실제 사용 환경과 영상 품질이 다를 수 있다.
- 개인 iCloud 계정 로그인은 하지 않는다. iCloud 전용 자산 검증은 별도 실제 사용자 기기가 필요하다.
- 자동 재서명 과정에서 iCloud·푸시·Apple Pay 같은 entitlement는 제거될 수 있다. DZlog M3-A 검증 범위에는 해당 entitlement가 없다.
