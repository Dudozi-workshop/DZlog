# Counter / Scope 작업 보드

이 문서는 카운터/스트림 관련 작업의 진행 상태를 한 곳에서 관리하기 위한 체크리스트입니다.

## 현재 목표
- 카운터가 의도한 scope(저장경로/파일명 ON/OFF 조합)에서만 반응하도록 안정화
- 화면별(촬영/표 상세설정/홈) 미리보기 의미를 일치시켜 혼란 최소화
- 회귀 테스트/에뮬 QC 루틴을 고정해 재발 방지

## 완료된 작업
- [x] wildcard(`*`) scope에서 MediaStore 스캔/파싱 가능하도록 보강
- [x] 같은 scope에서 카운터 역행 방지(stable stream next)
- [x] manual override clear 조건을 실제 정책 판단(`shouldClearPreserveManualSeed`) 기반으로 제한
- [x] DATE/TIME 셀이 counter stream prefix를 흔들지 않도록 제외
- [x] 파일명 delimiter가 하드코딩(`_`)되지 않도록 `fnDelim` SSOT 반영
- [x] ON/OFF 모드 전환 시, 현재 ON인 축(path/filename)만 새 스트림 판정하도록 스냅샷 기반 로직 적용

## 이번 턴 반영
- [x] 전체설정 화면에서 저장경로/파일명 상단 미리보기(Status card) 제거
- [x] 홈 화면 저장경로/파일명 카드를 SSOT 기반 계산(CaptureCounterPolicy + CaptureNamingPolicy)으로 재연결
- [x] 진행 현황 문서화 시작(이 문서 추가)

## 남은 작업
- [ ] 4조합(Path/Filename ON/OFF) 에뮬 회귀표 작성 및 고정
- [ ] OFF/OFF 정책(완전 전역 vs 템플릿 전역) 최종 UX 확정
- [ ] 필요 시 설정 화면에 scope 동작 요약 문구(ON인 항목 변화에만 반응) 추가
- [ ] 네트워크 가능한 환경에서 단위 테스트 실행/결과 아카이브

## 에뮬 QC 핵심 시나리오
1. 같은 값 유지 + 모드 토글만 변경 시 카운터가 불필요 초기화되지 않아야 함
2. Filename ON일 때만 파일명(prefix) 변경에 반응해야 함
3. Path ON일 때만 저장경로 변경에 반응해야 함
4. OFF/OFF에서는 전역 흐름처럼 최대값 기반으로 이어지는지 확인

## 메모
- 테스트 명령은 환경 프록시 제약으로 현재 실행 실패 가능(Gradle 배포본 다운로드 403).
- 네트워크 가능한 개발/CI 환경에서 동일 명령 재실행 필요.
