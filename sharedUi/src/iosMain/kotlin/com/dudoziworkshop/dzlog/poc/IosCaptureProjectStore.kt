package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.project.IosCaptureProjectRegistry
import com.dudoziworkshop.dzlog.data.project.IosCaptureProjectRegistryCodec
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

/** One versioned project-registry value next to, but separate from, the iOS editor workspace. */
internal class IosCaptureProjectStore(private val editorKey: String) {
    private val key = "$editorKey.capture-projects.v1"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    fun load(): IosCaptureProjectRegistry = defaults.stringForKey(key)
        ?.let(IosCaptureProjectRegistryCodec::decode)
        ?: IosCaptureProjectRegistry()

    fun save(value: IosCaptureProjectRegistry): Boolean = runCatching {
        val encoded = IosCaptureProjectRegistryCodec.encode(value)
        check(IosCaptureProjectRegistryCodec.decode(encoded) == value)
        defaults.setObject(encoded, forKey = key)
        defaults.synchronize()
        check(load() == value)
        true
    }.getOrDefault(false)

    fun create(title: String, templateId: String?): IosCaptureProjectRegistry? = update { current ->
        current.create(
            projectId = NSUUID().UUIDString.lowercase(),
            title = title,
            timestamp = nowMillis(),
            templateId = templateId,
        )
    }

    fun select(projectId: String?): IosCaptureProjectRegistry? = update { it.select(projectId) }

    fun rememberTemplate(templateId: String): IosCaptureProjectRegistry? = update {
        it.rememberTemplate(templateId)
    }

    fun complete(projectId: String): IosCaptureProjectRegistry? = update {
        it.complete(projectId, nowMillis())
    }

    fun reopen(projectId: String): IosCaptureProjectRegistry? = update { it.reopen(projectId) }

    fun rename(projectId: String, title: String): IosCaptureProjectRegistry? = update {
        it.rename(projectId, title)
    }

    fun delete(projectId: String): IosCaptureProjectRegistry? = update { it.delete(projectId) }

    fun appendCapture(captureId: String): IosCaptureProjectRegistry? = update {
        it.appendCapture(captureId)
    }

    fun removeCapture(captureId: String): IosCaptureProjectRegistry? = update {
        it.removeCapture(captureId)
    }

    fun resetTestFixture() {
        check(editorKey in setOf(IOS_EDITOR_TEST_KEY, IOS_NAVIGATION_TEST_KEY, IOS_PRODUCTION_STARTUP_TEST_KEY))
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }

    private fun update(
        transform: (IosCaptureProjectRegistry) -> IosCaptureProjectRegistry,
    ): IosCaptureProjectRegistry? = runCatching {
        val current = load()
        val next = transform(current)
        if (next != current) check(save(next))
        next
    }.getOrNull()

    private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000.0).toLong()
}
