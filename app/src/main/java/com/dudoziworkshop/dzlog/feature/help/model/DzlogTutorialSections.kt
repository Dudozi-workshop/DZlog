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
            from("camera", "basics", "화면 구성과 촬영", "layout"),
            custom("zoom-focus", "배율과 초점",
                Triple("zoom", "배율 조절", "배율 칩이나 확대 기능을 사용하면 화면에 보이는 대상을 확대할 수 있어요."),
                Triple("pinch", "핀치 줌", "카메라 미리보기에서 두 손가락을 벌리면 확대, 오므리면 축소돼요. 표 크기 조절과는 달라요."),
                Triple("focus", "초점", "화면을 터치하는 초점 조절과 수동 초점 도구를 상황에 맞게 사용할 수 있어요.")),
            from("camera", "tools", "촬영 보조도구", "tools"),
            from("table-gesture", "table", "표 표시와 조작", "select", "move", "size", "rotation"),
            from("camera", "quick", "빠른 수정", "quick"),
            from("camera", "results", "촬영 결과와 실행 취소", "result"),
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
