package com.dudoziworkshop.dzlog.feature.help.model

/** Navigation-only grouping. Existing tutorial content remains the source of truth. */
internal data class TutorialArea(val id: String, val title: String, val sessions: List<TutorialSession>)
internal data class TutorialSession(
    val id: String,
    val title: String,
    val steps: List<TutorialStep>,
)

internal object DzlogTutorialSections {
    private fun from(module: String, id: String, title: String, vararg stepIds: String): TutorialSession {
        val source = requireNotNull(DzlogTutorialCatalog.module(module))
        val selected = stepIds.map { stepId -> source.steps.first { it.id == stepId } }
        return TutorialSession(id, title, selected)
    }
    private fun custom(id: String, title: String, vararg pages: Triple<String, String, String>) =
        TutorialSession(id, title, pages.map { (stepId, heading, body) ->
            TutorialStep(stepId, heading, body)
        })

    val areas: List<TutorialArea> = listOf(
        TutorialArea("home", "홈", listOf(
            custom("overview", "홈 화면과 주요 메뉴",
                Triple("main", "홈 화면", "홈에서는 최근 촬영을 확인하고 카메라와 앨범으로 이동할 수 있어요.")),
            custom("recent", "최근 촬영과 빠른 이동",
                Triple("recent", "최근 촬영", "최근 사진을 선택하면 저장된 사진과 촬영 결과를 확인할 수 있어요.")),
            custom("start", "홈에서 작업 시작하기",
                Triple("start", "촬영과 앨범", "새 기록은 촬영에서 시작하고, 저장된 사진은 앨범에서 관리해요.")),
        )),
        TutorialArea("camera", "카메라", listOf(
            custom("basics", "화면 구성",
                Triple("layout", "카메라 화면 둘러보기", "상단에는 다음 파일명과 촬영 정보, 중앙에는 미리보기와 표, 하단에는 촬영·도구·빠른 수정·최근 촬영·Undo가 있어요."),
                Triple("top", "상단 파일명과 정보", "상단의 다음 파일명 영역에서 촬영 전에 저장될 이름을 확인할 수 있어요. 촬영 정보는 별도의 안내로 확인해요."),
                Triple("bottom", "하단 버튼", "아래쪽의 최근 촬영, 도구, 촬영, 빠른 수정, 실행 취소 버튼을 순서대로 살펴봐요.")),
            custom("capture", "촬영과 저장",
                Triple("shutter", "촬영 버튼", "가운데 셔터 버튼으로 사진을 촬영해요. 튜토리얼에서는 실제 촬영할 필요가 없어요."),
                Triple("saving", "저장 진행 상태", "저장 중 표시가 사라졌는지 확인하세요. 오류가 표시되면 저장 결과와 안내를 확인해요."),
                Triple("preview", "촬영 후 미리보기", "촬영 후 사진을 확인하는 동작은 미리보기 설정에 따라 달라질 수 있어요.")),
            custom("zoom-focus", "배율과 초점",
                Triple("zoom", "배율 칩", "화면에 표시되는 배율을 확인하고 배율 조절 기능을 펼쳐 확대 정도를 선택할 수 있어요."),
                Triple("pinch", "핀치 줌", "카메라 미리보기에서 두 손가락을 벌리면 확대, 오므리면 축소돼요. 표의 크기 조절과는 별개예요."),
                Triple("focus", "초점 조절", "화면의 원하는 부분을 터치해 초점을 맞추거나, 초점 도구에서 모드를 선택하고 수동으로 조절할 수 있어요.")),
            custom("tools", "촬영 보조도구",
                Triple("menu", "도구 메뉴", "하단 도구 버튼에서 촬영을 돕는 설정을 골라 사용할 수 있어요."),
                Triple("flash", "플래시", "어두운 환경에서 조명 모드를 확인하고 변경할 수 있어요. 사용할 수 있는 모드는 기기에 따라 달라질 수 있어요."),
                Triple("grid", "격자", "격자 표시를 켜면 화면의 수평과 구도를 맞추기 편해요. 사진에 격자가 함께 저장되는 기능은 아니에요."),
                Triple("table", "표 표시", "미리보기에서 표 표시 여부를 조정할 수 있어요. 실제 저장 결과는 촬영·저장 설정을 함께 확인하세요."),
                Triple("assist", "보조 셔터", "보조 셔터를 사용하면 화면의 기본 셔터 이외의 위치에서 촬영 버튼을 이용할 수 있어요.")),
            custom("table", "표와 빠른 수정",
                Triple("select", "표 선택과 배치", "미리보기의 표를 선택하면 이동, 크기, 회전, 잠금 등 배치 도구를 사용할 수 있어요."),
                Triple("quick", "빠른 수정", "촬영 전에 표의 입력값을 빠르게 확인하고 수정하는 기능이에요. 튜토리얼에서는 입력하지 않아요."),
                Triple("template", "양식 확인", "빠른 수정에서 현재 사용하는 양식을 확인할 수 있어요. 양식을 바꿀 때는 변경 내용을 확인하세요.")),
            custom("results", "촬영 결과와 Undo",
                Triple("recent", "최근 촬영", "하단의 최근 촬영 영역에서 방금 촬영한 결과로 이동하거나 앨범을 열 수 있어요."),
                Triple("result", "빠른 결과", "현재 촬영 작업에서 만들어진 사진들을 빠르게 확인하는 화면이에요."),
                Triple("undo", "Undo · 직전 촬영 취소", "Undo는 직전 촬영을 취소하는 기능이에요. 사진 삭제와 번호·문구 되돌림에 영향을 줄 수 있으니 실제 사용할 때 확인하세요."),
                Triple("warning", "저장경로 변경 경고", "저장경로 변경으로 직전 촬영 취소에 영향이 생기면 경고가 표시될 수 있어요. 관련 알림은 설정에서 조절할 수 있어요.")),
            custom("settings", "카메라 설정",
                Triple("entry", "설정 열기", "카메라 상단의 설정 버튼으로 촬영 관련 옵션에 들어갈 수 있어요."),
                Triple("quality", "화질과 저장 방식", "화질과 원본·결과 저장 방식에 따라 저장되는 사진이 달라질 수 있어요."),
                Triple("feedback", "촬영 소리와 진동", "촬영 효과음과 진동 관련 옵션을 확인하고 원하는 사용 환경에 맞게 조절할 수 있어요."),
                Triple("controls", "촬영 조작 옵션", "음량 버튼과 미리보기 동작처럼 촬영 조작에 영향을 주는 설정을 확인해요.")),
            custom("naming", "파일명과 저장 정보",
                Triple("filename", "다음 파일명", "촬영 전에 상단에 표시되는 파일명으로 다음 사진 이름을 미리 확인할 수 있어요."),
                Triple("path", "저장 위치", "저장설정에서 지정한 위치로 사진이 저장돼요. 저장 폴더와 파일명은 서로 다른 설정이에요."),
                Triple("counter", "자동번호", "촬영 번호는 설정한 번호 규칙과 성공한 촬영 결과를 바탕으로 진행돼요. 자세한 변경 방법은 저장설정 구역에서 알아봐요.")),
        )),
        TutorialArea("gallery", "앨범", listOf(
            from("gallery", "overview", "앨범 화면과 탐색", "home", "browse"),
            from("gallery", "viewer", "사진 보기와 원본·결과", "viewer"),
            from("gallery", "photos", "사진 선택·공유·삭제", "photos"),
            from("gallery", "folders", "폴더와 하위 폴더 관리", "folders"),
        )),
        TutorialArea("table-detail", "표 편집", listOf(
            from("table-detail", "overview", "표 상세 화면 구성", "sections"),
            from("table-detail", "cells", "셀 내용과 입력 유형", "cells"),
            from("table-detail", "structure", "행·열과 병합 구조", "structure"),
            from("table-detail", "style", "스타일과 배치", "style"),
            from("table-detail", "save", "저장·취소와 실행 취소", "save"),
        )),
        TutorialArea("saving", "저장설정", listOf(
            from("saving", "filename", "파일명 구성", "overview", "filename"),
            from("saving", "folder", "저장 위치와 방식", "folder", "advanced"),
            from("saving", "counter", "자동번호와 동기화", "counter"),
            custom("rotation", "순환문구와 자동값",
                Triple("rotation", "순환문구", "문구 목록과 변경 주기에 따라 촬영할 때 표시되는 값이 달라질 수 있어요."),
                Triple("automatic", "자동값", "날짜·시간·자동번호는 설정한 규칙에 따라 생성돼요.")),
        )),
        TutorialArea("settings", "앱 설정", listOf(
            custom("capture", "촬영과 미리보기",
                Triple("preview", "촬영 옵션", "설정에서 촬영 후 미리보기 방식 등 카메라 옵션을 확인할 수 있어요.")),
            custom("feedback", "소리와 조작 설정",
                Triple("feedback", "촬영 알림", "촬영 소리와 진동 등 사용 환경에 맞게 조작 설정을 확인할 수 있어요.")),
            custom("permission", "권한과 폴더 연결",
                Triple("permission", "권한", "사진 저장과 카메라 사용에 필요한 권한과 폴더 연결 상태를 확인할 수 있어요.")),
            custom("data", "데이터 관리와 도움말",
                Triple("help", "도움말", "궁금한 기능은 사용법 및 도움말에서 해당 세션을 선택해 다시 볼 수 있어요.")),
        )),
    )

    fun area(id: String) = areas.firstOrNull { it.id == id }
}
