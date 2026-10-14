package com.dudoziworkshop.dzlog.feature.help.model

/** Read-only approved tutorial structure. No actions or modifications to user media. */
internal object DzlogTutorialCatalog {
    const val VERSION = "1.0"

    val modules = listOf(
        TutorialModule("camera", "카메라 촬영", listOf(
            step("layout", "촬영 화면 구성", "다음 파일명, 카메라, 표와 하단 촬영 버튼들을 확인해요."),
            step("quick", "빠른 수정", "텍스트·숫자 셀을 입력하고 앞뒤 셀과 파일명·저장경로 연결을 확인해요."),
            step("tools", "촬영 도구", "줌, 초점, 플래시, 격자, 표 표시와 보조 셔터를 구분해요."),
            step("result", "촬영 후 확인", "최근 사진, 앨범, 직전 촬영 삭제와 다음 파일명을 알아봐요."),
        )),
        TutorialModule("table-gesture", "표 조작", listOf(
            step("select", "표 선택", "표를 터치하면 상세·크기·회전·잠금 도구가 나타나요."),
            step("move", "표 이동", "표를 드래그해 촬영 영역 안에서 위치를 이동해요."),
            step("size", "표 크기", "슬라이더와 핸들로 비율을 유지하며 표를 조절해요."),
            step("rotation", "회전과 잠금", "0도/90도 회전, 배치 잠금과 셀 값 수정을 구분해요."),
        )),
        TutorialModule("table-detail", "표 상세 편집", listOf(
            step("sections", "편집 화면과 양식", "내용·구조·스타일·저장설정 탭과 양식 교체 시 저장 선택을 알아봐요."),
            step("cells", "셀 종류와 내용", "직접 입력과 자동 생성되는 값을 구분해요.", listOf(
                scene("text", "텍스트", "문자를 직접 입력해요."),
                scene("number", "숫자", "측정값을 직접 입력해요."),
                scene("counter", "자동번호", "촬영 성공을 기준으로 번호가 진행돼요."),
                scene("date", "날짜", "촬영 날짜가 자동 표시돼요."),
                scene("time", "시간", "촬영 시각이 자동 표시돼요."),
                scene("rotation", "순환문구", "문구 목록과 변경 주기에 따라 표시돼요."),
            )),
            step("structure", "표 구조", "행·열, 병합과 해제, 크기 변경 및 삭제 영향을 확인해요."),
            step("style", "표 스타일", "배경·투명도·문자 크기·정렬·테두리를 비교해요."),
            step("save", "실행 취소와 저장", "편집 취소·다시 실행과 저장/저장 안 함/취소를 구분해요."),
        )),
        TutorialModule("saving", "저장 및 자동번호", listOf(
            step("overview", "저장설정 개요", "파일명·저장 위치·자동번호 설정은 서로 달라요."),
            step("filename", "파일명 구성", "요소와 출처·순서·완성된 파일명을 확인해요.", listOf(
                scene("parts", "구성요소", "번호 접미부와 .jpg는 요소 개수에서 제외돼요."),
                scene("sources", "출처 추가", "셀·직접입력·날짜·시간 중 선택해요."),
                scene("edit", "값과 유형", "가운데 값과 오른쪽 출처 변경을 구분해요."),
                scene("order", "순서 변경", "왼쪽 손잡이로 요소 순서를 바꿔요."),
                scene("remove", "요소 삭제", "왼쪽 밀기로 삭제해요. 기존 사진 이름은 바뀌지 않아요."),
                scene("preview", "결과 미리보기", "요소·번호·확장자를 구분해요."),
            )),
            step("folder", "저장 위치", "Pictures/DZlog 아래에 하위 폴더가 만들어져요."),
            step("counter", "자동번호", "다음 번호·자릿수·동기화·초기화·범위를 알아봐요.", listOf(
                scene("next", "다음 번호", "-/+와 직접 입력으로 다음 번호를 조절해요."),
                scene("digits", "자릿수", "1을 1, 01, 001, 0001로 표시해요."),
                scene("sync", "번호 동기화", "저장된 사진 이력에서 다음 번호를 찾아요."),
                scene("reset", "번호 초기화", "기존 사진은 변경하지 않고 다음 번호를 1로 바꿔요."),
                scene("scope", "분리 범위", "저장 위치·파일명 구성별 번호 범위를 비교해요."),
            )),
            step("advanced", "고급 옵션과 확인", "저장 방식과 파일명·경로·번호 결과를 확인해요."),
        )),
        TutorialModule("gallery", "갤러리·폴더 관리", listOf(
            step("home", "갤러리 첫 화면", "전체·폴더·즐겨찾기와 최근 촬영을 확인해요."),
            step("browse", "폴더 탐색", "현재 폴더의 사진과 하위 폴더·원본사진을 구분해요."),
            step("viewer", "사진 보기", "뷰어·즐겨찾기·공유·정보·사진 메뉴를 알아봐요."),
            step("folders", "폴더 관리", "폴더 조작 전 영향과 결과를 확인해요.", listOf(
                scene("new", "새 폴더", "현재 위치에서 하위 폴더를 만들어요."),
                scene("rename", "이름 변경", "변경에 따른 저장경로 영향을 확인해요."),
                scene("move", "폴더 이동", "하위 폴더와 원본 사진도 함께 이동해요."),
                scene("delete", "폴더 삭제", "개수를 최종 확인해요. 되돌릴 수 없어요."),
                scene("path", "저장 규칙", "폴더 이동이 촬영 저장 규칙을 자동 변경하지 않아요."),
            )),
            step("photos", "사진 관리", "사진 다중 선택과 이동·공유·삭제를 구분해요.", listOf(
                scene("select", "다중 선택", "길게 눌러 사진을 선택해요."),
                scene("actions", "선택 도구", "이동·공유·삭제 메뉴를 알아봐요."),
                scene("move", "사진 이동", "필요하면 연결 원본 포함을 선택해요."),
                scene("delete", "사진 삭제", "원본 포함 여부와 수량을 확인해요. 되돌릴 수 없어요."),
                scene("result", "결과 확인", "이동·삭제 결과, 일부 실패와 재시도를 확인해요."),
            )),
        )),
    )

    fun module(id: String): TutorialModule? = modules.firstOrNull { it.id == id }

    private fun step(id: String, title: String, instruction: String, scenes: List<TutorialScene> = emptyList()) =
        TutorialStep(id, title, instruction, scenes)

    private fun scene(id: String, title: String, instruction: String) =
        TutorialScene(id, title, instruction)
}

internal data class TutorialModule(
    val id: String,
    val title: String,
    val steps: List<TutorialStep>,
)

internal data class TutorialStep(
    val id: String,
    val title: String,
    val instruction: String,
    val scenes: List<TutorialScene> = emptyList(),
)

internal data class TutorialScene(
    val id: String,
    val title: String,
    val instruction: String,
)
