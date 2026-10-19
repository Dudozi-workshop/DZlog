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
            custom("overview", "촬영 준비",
                Triple("main", "현재 템플릿", "템플릿을 선택해 촬영에 사용할 표 양식을 바꿔요."),
                Triple("templates", "템플릿 선택", "저장된 템플릿 중 사용할 양식을 선택해요."),
                Triple("manage", "템플릿 메뉴", "⋯ 버튼을 누르면 템플릿 관리 메뉴가 열려요."),
                Triple("more", "템플릿 관리", "표를 편집하거나 이름을 바꾸고, 복제·삭제할 수 있어요."),
                Triple("counter", "다음 촬영번호", "다음 사진에 사용할 번호예요. 촬영할 때마다 올라가요."),
                Triple("start", "촬영 시작", "선택한 표 양식으로 촬영 화면을 열어요.")),
            custom("recent", "사진 확인",
                Triple("recent", "최근 촬영", "사진을 누르면 해당 사진이 저장된 폴더로 이동해요."),
                Triple("album", "앨범 보기", "전체 사진과 폴더를 확인해요.")),
            custom("start", "편집과 설정",
                Triple("table-settings", "표 편집", "표의 내용·구조·스타일을 편집해요."),
                Triple("save-info", "저장 정보", "파일명·저장경로·촬영번호를 확인하고 설정할 수 있어요."),
                Triple("settings", "앱 설정", "촬영 환경·권한·백업 등을 설정해요.")),
        )),
        TutorialArea("camera", "카메라", listOf(
            custom("basics", "화면 구성",
                Triple("layout", "카메라 화면 둘러보기", "상단에는 다음 파일명과 촬영 정보, 중앙에는 미리보기와 표, 하단에는 촬영·도구·빠른 수정·최근 촬영·Undo가 있어요."),
                Triple("top", "상단 파일명과 정보", "상단의 다음 파일명 영역에서 촬영 전에 저장될 이름을 확인할 수 있어요. 촬영 정보는 별도의 안내로 확인해요."),
                Triple("bottom", "하단 버튼", "아래쪽의 최근 촬영, 도구, 촬영, 빠른 수정, 실행 취소 버튼을 순서대로 살펴봐요.")),
            custom("capture", "촬영과 저장",
                Triple("shutter", "촬영 버튼", "가운데 셔터 버튼으로 사진을 촬영해요. 튜토리얼에서는 실제 촬영할 필요가 없어요."),
                Triple("saving", "저장 진행 상태", "저장 중 표시를 누르면 진행 상태를 확인할 수 있어요. 지금은 표시 예시이며 실제 저장은 하지 않아요. 완료되면 표시가 사라져요."),
                Triple("preview", "촬영 후 미리보기", "안 함·잠깐·유지의 차이를 순서대로 보여드려요. 저장된 사진이 있으면 표시 예시로 사용하고, 없으면 표시 위치를 안내해요.")),
            custom("zoom-focus", "배율과 초점",
                Triple("zoom", "배율 칩", "현재 배율 버튼을 누르면 눈금이 펼쳐져요. 버튼이나 눈금을 좌우로 끌면 배율과 양옆의 빠른 배율 버튼이 함께 바뀌어요."),
                Triple("pinch", "핀치 줌", "카메라 미리보기에서 두 손가락을 벌리면 확대, 오므리면 축소돼요. 표의 크기 조절과는 별개예요."),
                Triple("focus", "초점 조절", "화면에서 선명하게 보고 싶은 부분을 터치하면 그 위치에 초점을 맞출 수 있어요.")),
            custom("tools", "촬영 보조도구",
                Triple("menu", "도구 메뉴", "하단 도구 버튼에서 촬영을 돕는 설정을 골라 사용할 수 있어요."),
                Triple("manual-focus", "초점 도구", "초점 도구에서 자동·수동 모드를 선택하고 초점을 조절해요."),
                Triple("flash", "플래시", "어두운 환경에서 조명 모드를 확인하고 변경할 수 있어요. 사용할 수 있는 모드는 기기에 따라 달라질 수 있어요."),
                Triple("grid", "격자", "격자 표시를 켜면 화면의 수평과 구도를 맞추기 편해요. 사진에 격자가 함께 저장되는 기능은 아니에요."),
                Triple("table", "표 표시", "미리보기에서 표 표시 여부를 조정할 수 있어요. 실제 저장 결과는 촬영·저장 설정을 함께 확인하세요."),
                Triple("assist", "보조 셔터", "보조 셔터를 사용하면 화면의 기본 셔터 이외의 위치에서 촬영 버튼을 이용할 수 있어요.")),
            custom("table", "표와 빠른 수정",
                Triple("select", "표 선택과 배치", "미리보기의 표를 선택하면 이동, 크기, 회전, 잠금 등 배치 도구를 사용할 수 있어요."),
                Triple("move", "표 이동", "표를 선택한 뒤 끌어 촬영 영역 안에서 위치를 옮겨요. 사진 바깥으로 나가지 않도록 이동 범위가 제한돼요."),
                Triple("size", "표 크기", "표의 크기 도구와 모서리 조절점으로 크기를 바꿔요. 카메라 배율과는 별도로 조절돼요."),
                Triple("rotation", "표 회전", "회전 버튼으로 표 방향을 90도씩 바꿔요. 화면에서 달라지는 표 방향을 확인해 보세요."),
                Triple("lock", "배치 잠금과 해제", "잠그면 표의 이동·크기·회전을 막아요. 같은 버튼으로 잠금을 해제하면 다시 조절할 수 있어요."),
                Triple("details", "상세 편집", "상세 버튼을 누르면 표 편집 화면을 열어 내용·구조·스타일을 바꿀 수 있어요."),
                Triple("quick", "빠른 수정", "실제 빠른 수정 창에서 기능을 살펴봐요. 여기서는 저장하지 않는 안내용 예시 값을 보여드려요."),
                Triple("quick-text", "문자 입력과 반영", "셀을 선택해 글자를 바꾸면 표와 연결된 파일명에도 바로 반영돼요. 예시 값이 바뀌는 모습을 확인해 보세요."),
                Triple("quick-number", "숫자 입력", "숫자 셀도 같은 창에서 바꿔요. 셀 이동 버튼으로 다른 항목을 이어서 수정할 수 있어요."),
                Triple("quick-date", "날짜 형식", "날짜 셀에서는 표시 형식을 고를 수 있어요. 바꾼 형식이 표에 반영돼요."),
                Triple("quick-time", "촬영 시각", "시간 셀은 촬영 시각을 자동으로 표시해요. 촬영할 때의 시간이 사용돼요."),
                Triple("quick-phrase", "순환문구 세트", "등록한 문구 세트를 선택해 사용할 문구를 바꿔요. 촬영에 따른 문구 진행은 기존 설정을 따라요."),
                Triple("quick-filename", "파일명 전체 확인", "파일명 한 줄을 누르면 전체 이름을 펼쳐 확인할 수 있어요. 저장 설정으로 이동하는 버튼과는 달라요."),
                Triple("quick-path", "저장경로 전체 확인", "저장경로 한 줄을 누르면 사진이 저장될 전체 경로를 확인할 수 있어요."),
                Triple("template", "템플릿 변경", "템플릿 이름을 누르면 선택 목록이 열려요. 다른 양식을 선택하면 표 구성이 바뀌어요. 지금은 안내용 예시로 보여드려요.")),
            custom("results", "촬영 결과와 Undo",
                Triple("recent", "최근 촬영", "하단의 최근 촬영 영역에서 방금 촬영한 결과로 이동하거나 앨범을 열 수 있어요."),
                Triple("result", "빠른 결과", "현재 촬영 작업의 사진들을 모아 보는 실제 빠른결과 화면이에요. 안내에서는 촬영하지 않으므로 빈 상태를 보여드려요."),
                Triple("undo", "Undo · 직전 촬영 취소", "Undo는 직전 촬영을 취소하는 기능이에요. 사진 삭제와 번호·문구 되돌림에 영향을 줄 수 있으니 실제 사용할 때 확인하세요."),
                Triple("warning", "저장경로 변경 경고", "현재 저장경로와 다른 경로의 사진을 Undo로 삭제하려 할 때 경고가 표시돼요. 삭제 여부를 확인하고 진행하거나 취소해요.")),
            custom("settings", "카메라 설정",
                Triple("entry", "설정 열기", "카메라 상단의 설정 버튼으로 촬영 관련 옵션에 들어갈 수 있어요."),
                Triple("ratio", "촬영 비율", "비율을 선택해 사진에 담기는 화면의 가로·세로 비율을 바꿔요."),
                Triple("quality", "저장 양식", "원본·표 합성·둘 다 중 저장할 사진의 양식을 선택해요."),
                Triple("capture-confirmation", "촬영 확인", "안함·잠깐·유지 중 촬영한 사진을 확인하는 방식을 선택해요."),
                Triple("feedback", "촬영 소리와 진동", "촬영 효과음과 진동 관련 옵션을 확인하고 원하는 사용 환경에 맞게 조절할 수 있어요."),
                Triple("controls", "촬영 조작 옵션", "음량키를 안 함·배율·촬영 중 선택할 수 있어요. 지금은 선택 표시만 바뀌며 음량키로 촬영되지는 않아요.")),
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
                Triple("rotation", "순환문구", "저장하지 않는 예시 표에서 순환문구의 목록·세트·변경 주기를 살펴봐요. 저장 규칙에서는 이 셀을 연결해 촬영마다 달라지는 값을 사용할 수 있어요."),
                Triple("automatic", "자동값", "날짜·시간·자동번호는 설정한 규칙에 따라 생성돼요.")),
        )),
        TutorialArea("settings", "앱 설정", listOf(
            custom("capture", "촬영과 미리보기",
                Triple("preview", "사진 품질", "사진 품질에서 속도 우선·균형·화질 우선을 선택해요. 촬영 비율과 촬영 후 확인 방식은 카메라 상단 설정에서 정해요."),
                Triple("warning", "Undo 삭제 경고", "저장경로가 다른 사진을 Undo할 때의 삭제 경고를 켜거나 끌 수 있어요. 경고를 꺼도 Undo의 삭제 동작은 유지돼요.")),
            custom("feedback", "소리와 조작 설정",
                Triple("feedback", "보조 셔터", "플로팅 보조 셔터를 켜거나 끌 수 있어요. 촬영 소리·진동·음량키 동작은 카메라 상단의 촬영 설정에서 확인해요.")),
            custom("permission", "권한과 폴더 연결",
                Triple("permission", "권한", "사진 저장과 카메라 사용에 필요한 권한과 폴더 연결 상태를 확인할 수 있어요."),
                Triple("permission-screen", "권한 관리 화면", "실제 권한 관리 화면에서 카메라·사진 접근 권한과 폴더 연결 상태를 확인해요. 안내에서는 권한 요청이나 폴더 연결을 실행하지 않아요.")),
            custom("data", "데이터 관리와 도움말",
                Triple("backup", "작업 환경 백업", "작업 환경 내보내기로 표 양식과 저장 규칙을 백업해요. 가져오기는 백업의 양식을 추가하는 기능이며 사진 파일 백업과는 달라요."),
                Triple("export-screen", "작업 환경 내보내기", "실제 내보내기 화면이에요. 표 템플릿과 저장 규칙을 백업 파일로 보관할 수 있어요. 안내에서는 파일을 만들지 않아요."),
                Triple("import", "작업 환경 가져오기", "백업 파일의 템플릿을 현재 작업 환경에 추가해요. 기존 사진 파일을 복원하는 기능은 아니에요."),
                Triple("import-screen", "작업 환경 가져오기 화면", "실제 가져오기 화면이에요. 파일을 선택한 뒤 추가할 양식을 확인하고 진행해요. 안내에서는 파일을 선택하거나 가져오지 않아요."),
                Triple("help", "도움말", "사용법 및 도움말에서 원하는 구역·세션으로 이동해 다시 볼 수 있어요. 보는 중인 위치와 완료 여부도 기억해요.")),
        )),
    ).map { area ->
        if (area.id != "camera") area else {
            fun group(id: String, title: String, vararg sources: String): TutorialSession =
                TutorialSession(id, title, sources.flatMap { source -> area.sessions.first { it.id == source }.steps })
            area.copy(sessions = listOf(
                group("capture", "촬영과 확인", "basics", "capture", "results"),
                group("tools", "초점과 촬영도구", "zoom-focus", "tools"),
                group("table", "표와 빠른 수정", "table"),
                group("settings", "촬영·저장설정", "settings", "naming"),
            ))
        }
    }

    fun area(id: String) = areas.firstOrNull { it.id == id }
}

/** Stable page IDs keep saved positions independent of display wording. */
internal data class TutorialGuidePage(val id: String, val parentId: String, val title: String, val instruction: String)
internal fun TutorialSession.guidePages(): List<TutorialGuidePage> = steps.flatMap { step ->
    listOf(TutorialGuidePage(step.id, step.id, step.title, step.instruction)) +
        step.scenes.map { TutorialGuidePage("${step.id}/${it.id}", step.id, it.title, it.instruction) }
}
internal data class TutorialLocation(val areaId: String, val sessionId: String, val pageId: String = "")
internal fun tutorialPageIndex(pages: List<TutorialGuidePage>, savedId: String): Int =
    pages.indexOfFirst { it.id == savedId }.coerceAtLeast(0)
