package io.github.leonardoaliaga.argbcolorlens

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-wide preferences. Immutable snapshots keep hover rendering and
 * Settings apply/reset independent of each other.
 */
@Service(Service.Level.APP)
@State(name = "ArgbColorLensSettings", storages = [Storage("ArgbColorLens.xml")])
class ArgbHighlightSettings : PersistentStateComponent<ArgbHighlightSettings.Options> {
    data class Options(
        var enabled: Boolean = true,
        var useSwatchColor: Boolean = false,
        var customColorArgb: Int = DEFAULT_COLOR
    )

    @Volatile
    private var options = Options()

    override fun getState(): Options = options

    override fun loadState(state: Options) {
        options = state.copy()
    }

    fun snapshot(): Options = options.copy()

    fun update(newOptions: Options) {
        options = newOptions.copy()
    }

    companion object {
        // Similar blue accent to the original hover highlight, before settings existed.
        const val DEFAULT_COLOR: Int = -0xCA7545 // 0xFF358ABB
        fun getInstance(): ArgbHighlightSettings =
            ApplicationManager.getApplication().getService(ArgbHighlightSettings::class.java)
    }
}
