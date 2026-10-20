package com.dudoziworkshop.dzlog.feature.help.model

/** Navigation-only grouping. Existing tutorial content remains the source of truth. */
internal data class TutorialArea(val id: String, val title: String, val sessions: List<TutorialSession>)
internal data class TutorialSession(
    val id: String,
    val title: String,
    val steps: List<TutorialStep>,
    val pageOrder: List<String> = emptyList(),
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
            custom("settings", "상단 정보·촬영 설정",
                Triple("filename", "다음 촬영 정보", "위쪽 파일명을 누르면 다음 사진의 이름·저장경로·촬영번호를 확인해요. 저장설정에서 구성과 번호를 바꿀 수 있어요."),
                Triple("ratio", "촬영 비율", "위쪽 설정 버튼에서 촬영 비율을 바꿔요. 비율에 따라 사진에 담기는 영역이 달라져요."),
                Triple("quality", "저장 방식", "원본·표 합성·둘 다 중 사진에 저장할 형식을 선택해요."),
                Triple("capture-confirmation", "촬영 후 확인 방식", "안 함·잠깐·유지 중 촬영 후 사진을 확인하는 방식을 선택해요."),
                Triple("feedback", "촬영 소리와 진동", "원하는 촬영 효과음을 선택해 미리 듣고, 진동 강도는 OFF·약함·보통·강함 중 선택해요."),
                Triple("controls", "음량 버튼", "휴대전화 음량 버튼을 배율 조절 또는 촬영에 사용하도록 설정할 수 있어요.")),
            custom("tools", "촬영 화면·도구",
                Triple("menu", "촬영도구 메뉴", "촬영도구 버튼을 열어 메뉴 구성을 먼저 살펴봐요. 각 기능은 다음을 눌러 하나씩 안내해요."),
                Triple("focus", "초점", "화면을 터치해 원하는 위치에 초점을 맞춰요. 도구의 초점 메뉴에서 자동·수동을 바꾸고 수동 조절바도 살펴봐요."),
                Triple("flash", "플래시", "촬영도구를 열고 플래시 설정을 확인해요. 조명 상태를 선택할 수 있어요."),
                Triple("grid", "격자", "촬영도구의 격자를 켜거나 꺼요. 수평과 구도만 돕고 사진에는 저장되지 않아요."),
                Triple("table", "표 표시", "촬영도구에서 표를 숨기거나 다시 표시해요."),
                Triple("assist", "보조 셔터", "촬영도구에서 보조 셔터를 확인해요. 원하는 위치로 옮겨 촬영할 수 있어요."),
                Triple("zoom", "배율 조절", "도구 설명을 마친 뒤 화면의 배율 버튼·눈금·핀치를 사용해 확대·축소하는 방법을 살펴봐요.")),
            custom("capture", "하단 버튼·촬영 확인",
                Triple("layout", "카메라 화면", "위쪽에서 저장 정보를 확인하고, 아래 버튼으로 촬영과 도구를 사용해요."),
                Triple("saving", "촬영과 저장", "가운데 버튼으로 촬영해요. 저장 중 표시를 누르면 진행 상태를 확인할 수 있어요."),
                Triple("preview", "촬영 후 확인", "촬영한 사진을 잠깐 보거나, 화면에 유지하며 확인할 수 있어요."),
                Triple("recent", "최근 촬영", "썸네일을 누르면 이번 촬영의 사진들을 확인해요. 촬영한 사진이 없으면 앨범으로 이동해요."),
                Triple("undo", "촬영 취소", "Undo로 직전 촬영을 삭제하고 번호·순환문구 진행을 되돌려요. 현재와 다른 저장경로의 사진이면 경고가 나타나요.")),
            custom("table", "표와 빠른 수정",
                Triple("select", "표 선택", "표를 누르면 상세 편집·크기·회전·잠금 도구가 나타나요."),
                Triple("size", "이동과 크기", "표를 끌어 위치를 옮겨요. 크기 도구의 조절바나 모서리를 끌어 크기를 바꿔요."),
                Triple("lock", "회전과 잠금", "회전 버튼으로 표 방향을 바꿔요. 배치를 잠그면 이동·크기·회전이 고정돼요."),
                Triple("details", "상세 편집", "상세 버튼에서 표의 내용·구조·스타일을 편집할 수 있어요."),
                Triple("quick", "빠른 수정 열기", "빠른 수정 버튼으로 창을 열고 위에서 아래로 기능을 살펴봐요."),
                Triple("template", "템플릿 변경", "빠른 수정창 상단의 템플릿 이름을 누르면 다른 표 양식으로 바꿀 수 있어요."),
                Triple("quick-filename", "파일명 확인", "현재 파일명을 확인하고 저장 규칙을 살펴봐요."),
                Triple("quick-path", "저장경로 확인", "사진이 저장될 폴더를 확인해요."),
                Triple("quick-text", "텍스트 입력", "표에서 텍스트 셀을 선택하고 문구를 수정하는 방법을 살펴봐요."),
                Triple("quick-number", "숫자 입력", "숫자 셀을 선택하고 값을 수정하는 방법을 살펴봐요."),
                Triple("quick-date", "날짜와 시간", "날짜·시간 셀의 표시 형식을 확인해요. 자동으로 표시되는 시각은 직접 촬영하지 않아요."),
                Triple("quick-phrase", "순환문구", "순환문구 셀의 세트를 선택하고 촬영마다 달라지는 문구를 확인해요.")),
        )),
        TutorialArea("gallery", "앨범", listOf(
            from("gallery", "overview", "앨범 화면과 탐색", "home", "browse"),
            from("gallery", "folders", "폴더와 하위 폴더 관리", "folders"),
            from("gallery", "photos", "사진 선택·공유·삭제", "photos"),
            from("gallery", "viewer", "사진 보기와 원본·결과", "viewer"),
        )),
        TutorialArea("table-detail", "표 편집", listOf(
            from("table-detail", "overview", "표 상세 화면 구성", "sections"),
            from("table-detail", "cells", "셀 내용과 입력 유형", "cells"),
            from("table-detail", "structure", "행·열과 병합 구조", "structure"),
            from("table-detail", "style", "스타일과 배치", "style"),
            from("table-detail", "save", "저장과 종료", "save").copy(
                pageOrder = listOf("save/template", "save/template-confirm", "save/undo", "save/redo", "save", "save/decision")
            ),
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
            custom("environment", "앱 환경",
                Triple("intro", "전체 설정 둘러보기", "DZlog의 촬영 환경과 앱 사용에 필요한 설정을 관리하는 화면이에요. 각 메뉴에서 어떤 기능을 설정할 수 있는지 하나씩 살펴볼게요."),
                Triple("permission", "권한 관리", "카메라와 사진 접근 권한을 확인하고 관리할 수 있어요. 사진 저장에 필요한 권한이나 폴더 연결 상태도 여기에서 확인해요."),
                Triple("quality", "사진 품질", "사진 저장 품질을 속도 우선, 균형, 화질 우선 중에서 선택할 수 있어요. 화질이 높을수록 저장 시간이 길어질 수 있어요."),
                Triple("assist", "플로팅 보조 셔터", "촬영 화면에 표시되는 보조 셔터 버튼을 켜거나 끌 수 있어요. 원하는 위치에서 편리하게 촬영할 때 사용하는 기능이에요.")),
            custom("capture", "촬영 설정",
                Triple("feedback", "촬영 효과음 및 진동", "촬영할 때 사용할 효과음과 음량, 진동 강도를 설정할 수 있어요. 효과음은 저장하기 전에 미리 들어볼 수도 있어요."),
                Triple("warning", "저장경로 변경 시 삭제 경고", "촬영 중 저장 위치가 변경된 경우, 실행 취소로 다른 폴더의 사진을 삭제하기 전에 확인 메시지를 표시하는 기능이에요.")),
            custom("data", "데이터 관리",
                Triple("export", "작업 환경 내보내기", "현재 사용 중인 표 양식과 저장 규칙을 백업 파일로 저장할 수 있어요. 촬영한 사진 자체를 백업하는 기능은 아니에요."),
                Triple("import", "작업 환경 가져오기", "이전에 백업한 표 양식과 저장 규칙을 다시 가져올 수 있어요. 다른 기기에서 작업 환경을 준비할 때도 활용할 수 있어요.")),
            custom("support", "정보 및 지원",
                Triple("help", "사용법 및 도움말", "처음 사용법을 다시 보거나 카메라, 표 편집, 갤러리 등 기능별 튜토리얼을 선택해서 살펴볼 수 있어요."),
                Triple("credits", "도움 주신 분들·버전 정보", "DZlog 제작에 도움을 주신 분들과 화면 아래의 앱 버전 정보를 확인할 수 있어요.")),
        )),
    )

    fun area(id: String) = areas.firstOrNull { it.id == id }
}

/** Stable page IDs keep saved positions independent of display wording. */
internal data class TutorialGuidePage(val id: String, val parentId: String, val title: String, val instruction: String)
internal fun TutorialSession.guidePages(): List<TutorialGuidePage> {
    val pages = steps.flatMap { step ->
        listOf(TutorialGuidePage(step.id, step.id, step.title, step.instruction)) +
            step.scenes.map { TutorialGuidePage("${step.id}/${it.id}", step.id, it.title, it.instruction) }
    }
    if (pageOrder.isEmpty()) return pages
    val order = pageOrder.withIndex().associate { it.value to it.index }
    return pages.sortedBy { order[it.id] ?: Int.MAX_VALUE }
}
internal data class TutorialLocation(val areaId: String, val sessionId: String, val pageId: String = "")
internal fun tutorialPageIndex(pages: List<TutorialGuidePage>, savedId: String): Int =
    pages.indexOfFirst { it.id == savedId }.coerceAtLeast(0)
