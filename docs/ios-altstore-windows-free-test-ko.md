# DZlog iPhone 무료 테스트 설치

## 적용 범위

이 문서는 Apple Developer Program 회비 없이 Windows PC에서 DZlog 테스트판을
iPhone에 설치하는 절차를 설명한다. 실제 앱 검사는 iPhone에서 진행하며,
Windows는 IPA 재서명과 7일 갱신에 사용한다.

## 내려받을 파일

GitHub Actions의 `DZlog M3-A iPhone Device Compile` 실행 결과에서
`dzlog-iphone-altstore-free-test` 아티팩트를 내려받는다. 압축을 풀고 아래 파일을
사용한다.

- `DZlog-iPhone-free-test-AltStore.ipa`
- `README-ALTSTORE-WINDOWS-KO.txt`
- `ALTSTORE-BUILD-INFO.txt`
- `sha256.txt`

IPA는 iPhone Safari에서 바로 열어 설치할 수 없다. AltStore Classic이 사용자의
Apple 계정으로 기기용 서명을 적용해야 한다.

## Windows 준비

1. Apple 웹사이트에서 Windows용 iTunes와 iCloud를 설치한다.
2. AltStore 공식 안내에서 AltServer for Windows를 설치한다.
3. iPhone을 케이블로 연결하고 iPhone과 Windows 양쪽에서 신뢰를 허용한다.
4. iTunes에서 Wi-Fi 동기화를 켠다.
5. AltServer를 관리자 권한으로 실행한다.
6. 작업 표시줄의 AltServer에서 `Install AltStore`를 선택하고 iPhone을 고른다.
7. 본인의 Apple 계정으로 인증한다. 계정 암호와 인증 코드는 공유하지 않는다.

## iPhone 설치

1. iOS 16 이상에서는 `설정 > 개인정보 보호 및 보안 > 개발자 모드`를 켠다.
2. 필요한 경우 `설정 > 일반 > VPN 및 기기 관리`에서 본인의 개발자 앱을 신뢰한다.
3. iPhone에서 AltStore를 연다.
4. `My Apps`의 `+`를 누르고 `DZlog-iPhone-free-test-AltStore.ipa`를 선택한다.
5. 설치가 끝나면 DZlog를 열고 카메라 및 사진 권한을 허용한다.

## 7일 갱신

무료 Apple 계정으로 설치한 앱과 프로비저닝 프로파일은 7일 후 만료된다. 만료
전에 iPhone과 AltServer를 같은 Wi-Fi에 두거나 케이블로 연결한 뒤 AltStore에서
`Refresh All`을 실행한다.

## 첫 실기기 검수 순서

1. 첫 실행과 권한 안내
2. 홈 화면 및 설정 이동
3. 카메라 미리보기, 초점, 밝기, 확대, 플래시
4. 표 오버레이와 문구 렌더링
5. 촬영, 원본 및 결과 저장
6. 갤러리 목록, 필터, 상세 보기, 확대 및 좌우 이동
7. 앱 종료 후 재실행과 저장 상태 복원

검수 중 문제가 발생하면 화면 녹화와 함께 발생 화면, 직전 조작, 반복 여부를
기록한다. 앱이 실행되지 않으면 먼저 AltStore의 남은 서명 기간과 개발자 모드를
확인한다.
