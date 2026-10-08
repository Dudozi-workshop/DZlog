# 테스트 APK 업데이트 설치

GitHub Actions의 매번 새로 생성되는 기본 debug 키로 서명하면 기존 APK에 덮어쓰는 업데이트가 거부될 수 있다. CI는 이제 GitHub repository secret에 보관된 동일한 테스트 키를 사용한다. applicationId는 `com.dudoziworkshop.dzlog`로 유지한다. Release 서명과 버전 설정에는 적용하지 않는다.

## 최초 1회 설정

저장소 관리자 계정으로 다음을 실행한다. JDK의 keytool, Python 3, 로그인된 GitHub CLI가 필요하다. 기존 설치 APK를 만든 키를 보유하고 있다면 새 키를 만들지 않고 그 키를 사용한다. 아래 설정은 alias `androiddebugkey`, store/key password `android`인 개발 테스트 키용이다.

```bash
keytool -genkeypair -keystore dzlog-test.jks -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug, OU=DZlog Test Only, O=Dudozi Workshop, C=KR'
python3 -c "import base64,pathlib; pathlib.Path('dzlog-test.jks.b64').write_bytes(base64.b64encode(pathlib.Path('dzlog-test.jks').read_bytes()))"
gh secret set DZLOG_DEBUG_KEYSTORE_BASE64 --repo Dudozi-workshop/DZlog < dzlog-test.jks.b64
```

또는 GitHub → repository Settings → Secrets and variables → Actions → New repository secret에서 이름 `DZLOG_DEBUG_KEYSTORE_BASE64`, 값은 위 base64 파일 내용으로 등록한다. 키 원본은 저장소 밖에 안전하게 백업하고 git에 추가하지 않는다. 이 키는 테스트용으로만 사용하며 정식 배포키와 분리한다. Secret 등록 후 실패한 Android CI 실행을 다시 실행한다.

Secret이 없거나 키가 잘못되면 APK job을 명시적으로 실패시킨다. 새로운 임시 키를 만들어 호환되지 않는 APK를 배포하지 않는다. Unit-test job은 이 Secret과 독립적으로 실행된다.

## 빌드 및 자동 검증

CI는 먼저 versionCode `1000000 + 2 * GITHUB_RUN_NUMBER`로 APK를 만들고, 다음 번호로 다시 만든다. 두 APK에 대해 apksigner 검증, 고정키의 인증서 SHA-256 일치, 동일 applicationId, 각 versionCode를 확인한다. 두 번째 APK를 `DZlog-v2-test-apk` 아티팩트로 배포한다. CI 재실행은 같은 번호를 사용한다. 워크플로를 새로 만들어 run number가 초기화되는 경우 기존 설치 버전보다 큰 번호 체계를 유지해야 한다.

로컬에서 CI APK 위에 업데이트하려면 같은 키를 사용하고 설치된 versionCode 이상을 지정한다.

```bash
export DZLOG_DEBUG_KEYSTORE=/absolute/path/to/dzlog-test.jks
./gradlew :app:assembleDebug -PdzlogDebugVersionCode=1000001
```

예시 번호는 실제 설치된 APK 번호에 맞춰 증가시킨다. 환경변수가 없는 로컬 빌드는 일반 Android debug 키를 사용하므로 CI APK와 업데이트 호환되지 않을 수 있다.

## 실기기 확인 및 기존 설치 전환

기존 설치본이 사라진 임시 CI 키로 서명됐다면 그 키 없이 기존 설치본을 업데이트할 수 없다. 고정키로 전환하는 이번 한 번은 삭제·재설치가 필요할 수 있다. 삭제하면 앱 내부 설정과 템플릿 등이 지워질 수 있으므로 필요한 내용을 먼저 보관한다. 기존 키를 보유한 경우 해당 키를 등록하여 재설치를 피할 수 있다.

첫 고정키 APK 설치 후 다음 CI APK를 삭제 없이 설치하여 설정, 템플릿, 번호 진행 상태가 유지되는지 확인한다. 자동 검증은 APK 업데이트의 서명·패키지·버전 조건을 검사하며, 실제 Android 기기의 설치 및 데이터 유지 검증을 대신하지 않는다. 이 변경은 촬영 해상도, JPEG 품질, 표 합성, 저장 경로에 영향을 주지 않는다.
