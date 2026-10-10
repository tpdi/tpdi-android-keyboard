package org.futo.inputmethod.latin.uix

import android.content.Context
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.actions.VoiceInputAction
import org.futo.inputmethod.latin.uix.actions.keyCode
import org.futo.inputmethod.latin.uix.actions.keyCodeAlt
import org.json.JSONArray
import org.json.JSONObject

/** A named set of values for the voice-input toggles in [SoundProfiles.toggles]. */
data class SoundProfile(
    val name: String,
    val values: Map<String, Boolean>,
    val builtIn: Boolean = false
)

/**
 * Sound profiles: voice-input toggles grouped by the room you are in, picked by long-pressing
 * the microphone key. A toggle a profile doesn't mention goes back to its default (off, except
 * auto-stop), so picking a profile always gives the same result.
 */
object SoundProfiles {
    @Volatile
    private var enabled = false

    /** Set by the keyboard service: shows the profile menu. */
    @Volatile
    var showMenu: (() -> Unit)? = null

    @JvmStatic
    fun onSettingChanged(value: Boolean) {
        enabled = value
    }

    /** True for the keyboard's microphone key (the voice input action key, or the system shortcut). */
    @JvmStatic
    fun isMicKey(code: Int): Boolean =
        code == Constants.CODE_SHORTCUT || code == VoiceInputAction.keyCode || code == VoiceInputAction.keyCodeAlt

    /** True if a press on [code] should start the long-press timer: the mic key, while profiles are on. */
    @JvmStatic
    fun wantsLongPress(code: Int): Boolean = enabled && showMenu != null && isMicKey(code)

    /** Called on a long press of a key; true if it was the microphone key and the press was used. */
    @JvmStatic
    fun onMicLongPress(code: Int): Boolean {
        val show = showMenu
        if (!enabled || show == null || !isMicKey(code)) return false
        show()
        return true
    }

    /** The toggles a profile controls. */
    val toggles: List<SettingsKey<Boolean>> = listOf(
        USE_VAD_AUTOSTOP,
        VOICE_INPUT_NOISE_GATE,
        VOICE_INPUT_MY_VOICE_ONLY,
        VOICE_INPUT_TRIM_TRAILING_SILENCE,
        VOICE_INPUT_FILTER_MADE_UP_TEXT,
        VOICE_INPUT_FILTER_STOCK_PHRASES,
        VOICE_INPUT_CLICK_GESTURES
    )

    private fun nameOf(key: SettingsKey<Boolean>) = key.key.name

    /** Ready-made profiles. "Outside / noisy" is a guess and has not been tried outdoors. */
    val builtIn: List<SoundProfile> = listOf(
        SoundProfile("Quiet room", emptyMap(), builtIn = true),
        SoundProfile(
            "TV on",
            mapOf(
                nameOf(VOICE_INPUT_MY_VOICE_ONLY) to true,
                nameOf(VOICE_INPUT_TRIM_TRAILING_SILENCE) to true,
                nameOf(VOICE_INPUT_FILTER_MADE_UP_TEXT) to true
            ),
            builtIn = true
        ),
        SoundProfile(
            "Outside / noisy",
            mapOf(
                nameOf(USE_VAD_AUTOSTOP) to false,
                nameOf(VOICE_INPUT_NOISE_GATE) to true,
                nameOf(VOICE_INPUT_TRIM_TRAILING_SILENCE) to true
            ),
            builtIn = true
        )
    )

    fun parseCustom(json: String): List<SoundProfile> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val v = o.optJSONObject("values") ?: JSONObject()
            SoundProfile(o.getString("name"), v.keys().asSequence().associateWith { v.getBoolean(it) })
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun toJson(profiles: List<SoundProfile>): String = JSONArray().apply {
        profiles.forEach { p ->
            put(JSONObject().put("name", p.name).put("values", JSONObject(p.values)))
        }
    }.toString()

    fun all(context: Context): List<SoundProfile> =
        builtIn + parseCustom(context.getSetting(SOUND_PROFILES_CUSTOM))

    /** The current value of every controlled toggle, as a new profile called [name]. */
    fun snapshot(context: Context, name: String): SoundProfile =
        SoundProfile(name, toggles.associate { nameOf(it) to context.getSetting(it) })

    /** How full the water on the microphone key is: empty for no masking, higher for more. */
    fun waterLevel(profile: SoundProfile?): Float = when {
        profile == null -> 0f
        profile.values[nameOf(VOICE_INPUT_NOISE_GATE)] == true -> 0.5f
        profile.values[nameOf(VOICE_INPUT_MY_VOICE_ONLY)] == true -> 1f / 3f
        else -> 0f
    }

    fun apply(context: Context, profile: SoundProfile) {
        toggles.forEach { key ->
            context.setSettingBlocking(key.key, profile.values[nameOf(key)] ?: key.default)
        }
        context.setSettingBlocking(SOUND_PROFILE_ACTIVE.key, profile.name)
    }
}
