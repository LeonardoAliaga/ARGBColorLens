package io.github.leonardoaliaga.argbcolorlens

import com.intellij.DynamicBundle

/** Localized strings follow the IDE language, with English as fallback. */
internal object ArgbMessages {
    private val bundle = DynamicBundle(ArgbMessages::class.java, "messages.ArgbColorLensBundle")
    fun message(key: String, vararg params: Any): String = bundle.getMessage(key, *params)
}
