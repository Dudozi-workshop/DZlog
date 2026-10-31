package com.dudoziworkshop.dzlog.feature.help.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DzlogTutorialCatalogTest {
    @Test fun approvedStructureIsFiveModulesAndTwentyThreeSteps() {
        val modules = DzlogTutorialCatalog.modules
        assertEquals(listOf("camera", "table-gesture", "table-detail", "saving", "gallery"), modules.map { it.id })
        assertEquals(listOf(4, 4, 5, 5, 5), modules.map { it.steps.size })
        assertEquals(23, modules.sumOf { it.steps.size })
    }

    @Test fun everyModuleStepAndSceneIsUniqueAndReadable() {
        val modules = DzlogTutorialCatalog.modules
        assertEquals(modules.size, modules.map { it.id }.toSet().size)
        modules.forEach { module ->
            assertFalse(module.title.isBlank())
            assertEquals(module.steps.size, module.steps.map { it.id }.toSet().size)
            module.steps.forEach { step ->
                assertFalse(step.title.isBlank())
                assertFalse(step.instruction.isBlank())
                assertEquals(step.scenes.size, step.scenes.map { it.id }.toSet().size)
                step.scenes.forEach { scene ->
                    assertFalse(scene.title.isBlank())
                    assertFalse(scene.instruction.isBlank())
                }
            }
        }
    }

    @Test fun approvedDetailedScenesAndModuleLookupRemainAvailable() {
        assertEquals(7, DzlogTutorialCatalog.module("table-detail")!!.steps[1].scenes.size)
        val saving = DzlogTutorialCatalog.module("saving")!!
        val fileNameScenes = saving.steps.first { it.id == "filename" }.scenes.map { it.id }
        assertTrue(fileNameScenes.containsAll(listOf("parts", "sources", "edit", "order", "remove", "preview", "copy")))
        val counterScenes = saving.steps.first { it.id == "counter" }.scenes.map { it.id }
        assertTrue(counterScenes.containsAll(listOf("next", "input", "digits", "sync", "reset", "scope")))
        assertEquals(5, DzlogTutorialCatalog.module("gallery")!!.steps.first { it.id == "folders" }.scenes.size)
        assertTrue(DzlogTutorialCatalog.module("gallery")!!.steps.first { it.id == "folders" }.scenes.any { it.id == "destination" })
        assertEquals(6, DzlogTutorialCatalog.module("gallery")!!.steps.first { it.id == "photos" }.scenes.size)
        assertTrue(DzlogTutorialCatalog.module("gallery")!!.steps.first { it.id == "photos" }.scenes.any { it.id == "destination" })
        assertTrue(DzlogTutorialCatalog.module("gallery")!!.steps.first { it.id == "photos" }.scenes.any { it.id == "original-choice" })
        assertNull(DzlogTutorialCatalog.module("unknown"))
        assertTrue(DzlogTutorialCatalog.VERSION.isNotBlank())
    }
}
