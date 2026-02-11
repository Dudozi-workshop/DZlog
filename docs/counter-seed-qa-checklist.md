# Counter Seed QA Checklist

## 목적
`includePathInCounterScope` / `includeFilenameInCounterScope` 2개 토글 조합에 따라
카운터 범위(scope)가 의도대로 분리되는지 검증한다.

## 공통 준비
1. 설정에서 카운터 자릿수(예: 3)를 고정.
2. TableEditor에서 COUNTER seed를 눈에 띄는 값(예: 50)으로 수동 변경해둔다.
3. Logcat에서 `DZlogCounter` 필터를 켠다.

## 시나리오 A: 저장경로 ON + 파일명 ON
1. 설정에서 두 토글 모두 ON.
2. 저장경로 변경 → scopeKey가 바뀌고 seed가 해당 범위의 stream next로 이동하는지 확인.
3. 파일명(카운터 prefix) 변경 → 동일하게 scopeKey 변경/seed 재동기화 확인.

## 시나리오 B: 저장경로 ON + 파일명 OFF
1. 저장경로 ON, 파일명 OFF.
2. 파일명만 변경 → scopeKey가 바뀌지 않고 기존 흐름이 유지되는지 확인.
3. 저장경로 변경 → scopeKey 변경, seed 재동기화 확인.

## 시나리오 C: 저장경로 OFF + 파일명 ON
1. 저장경로 OFF, 파일명 ON.
2. 저장경로만 변경 → scopeKey가 바뀌지 않는지 확인.
3. 파일명(카운터 prefix) 변경 → scopeKey 변경, seed 재동기화 확인.

## 시나리오 D: 저장경로 OFF + 파일명 OFF
1. 두 토글 모두 OFF.
2. 저장경로/파일명 변경 반복해도 scopeKey가 `global` 유지되는지 확인.
3. 카운터가 하나의 공용 흐름으로 이어지는지 확인.

## 로그 확인 키
- `scopeKey`
- `includePathInCounterScope`
- `includeFilenameInCounterScope`
- `isNewStream`
- `shouldResyncForScopeChange`
- `desiredSeed`
