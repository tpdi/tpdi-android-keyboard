package org.futo.inputmethod.latin.uix.settings.pages

import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ANIMATE_BUBBLE
import org.futo.inputmethod.latin.uix.VOICE_INPUT_INLINE_PARTIAL_RESULT
import org.futo.inputmethod.latin.uix.VOICE_INPUT_FILTER_MADE_UP_TEXT
import org.futo.inputmethod.latin.uix.VOICE_INPUT_FILTER_STOCK_PHRASES
import org.futo.inputmethod.latin.uix.VOICE_INPUT_CLICK_GESTURES
import org.futo.inputmethod.latin.uix.VOICE_INPUT_ACTION_BUTTONS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_HIDE_KEYBOARD_BUTTON
import org.futo.inputmethod.latin.uix.VOICE_INPUT_CIRCLE_OVER_KEYS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_OVER_KEYBOARD
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SWITCH_MODE_BUTTONS
import org.futo.inputmethod.latin.uix.AUDIO_FOCUS
import org.futo.inputmethod.latin.uix.CAN_EXPAND_SPACE
import org.futo.inputmethod.latin.uix.DISALLOW_SYMBOLS
import org.futo.inputmethod.latin.uix.ENABLE_SOUND
import org.futo.inputmethod.latin.uix.PREFER_BLUETOOTH
import org.futo.inputmethod.latin.uix.SYSTEM_VOICE_INPUT_PACKAGE
import org.futo.inputmethod.latin.uix.USE_PERSONAL_DICT
import org.futo.inputmethod.latin.uix.USE_SYSTEM_VOICE_INPUT
import org.futo.inputmethod.latin.uix.USE_VAD_AUTOSTOP
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SEGMENTED_RESULTS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SEGMENT_PAUSE_MS
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.SettingSlider
import org.futo.inputmethod.latin.uix.settings.Tip
import org.futo.inputmethod.latin.uix.settings.SettingToggleDataStore
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import org.futo.inputmethod.latin.uix.settings.userSettingNavigationItem
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore
import kotlin.math.roundToInt

private val visibilityCheckNotSystemVoiceInput = @Composable {
    useDataStoreValue(USE_SYSTEM_VOICE_INPUT) == false
}

private data class VoiceIMEInfo(
    val builtin: Boolean,
    val name: String,
    val packageName: String,
)

@Composable
fun usePackageReadableName(pkg: String): String? {
    val context = LocalContext.current
    return remember(pkg) {
        try {
            context.packageManager.getPackageInfo(pkg, 0)
        } catch(e: Exception) {
            null
        }?.applicationInfo?.let {
            context.packageManager.getApplicationLabel(it).toString()
        }
    }
}

val VoiceInputMenu = UserSettingsMenu(
    title = R.string.voice_input_settings_title,
    navPath = "voiceInput", registerNavPath = true,
    settings = listOf(
        UserSetting(
            name = R.string.voice_input_settings_backend_system,
            searchTagList = listOf(
                R.string.voice_input_settings_disable_builtin_voice_input,
                R.string.voice_input_settings_disable_builtin_voice_input_subtitle
            )
        ) {
            val useExternal = useDataStore(USE_SYSTEM_VOICE_INPUT)
            val externalPkg = useDataStore(SYSTEM_VOICE_INPUT_PACKAGE)

            val context = LocalContext.current
            val res = LocalResources.current
            val options = remember(externalPkg.value) {
                val imm = context.getSystemService<InputMethodManager>()!!
                buildList {
                    add(VoiceIMEInfo(true, "", ""))
                    addAll(imm.enabledInputMethodList.filter { im ->
                        im.packageName == externalPkg.value ||
                            (0 until im.subtypeCount).map { im.getSubtypeAt(it) }
                                .any { it.mode.lowercase() == "voice" }
                    }.map {
                        VoiceIMEInfo(false, it.loadLabel(context.packageManager)?.toString() ?: it.packageName, it.packageName)
                    })
                }
            }

            val currOption = remember(externalPkg.value) {
                if(externalPkg.value == "") options[0] else
                options.find { it.packageName == externalPkg.value }
            }


            DropDownPickerSettingItem(
                stringResource(R.string.voice_input_settings_backend_system),
                options,
                currOption,
                {
                    useExternal.setValue(!it.builtin)
                    externalPkg.setValue(it.packageName ?: "")
                },
                {
                    if(it.builtin) res.getString(R.string.voice_input_settings_backend_system_internal)
                    else it.name
                }
            )

            if(useExternal.value && externalPkg.value.isNotEmpty()) {
                val privacyWhitelist = listOf(
                    "org.futo.voiceinput",
                    "org.futo.voiceinput.dev",
                    "dev.notune.transcribe",
                    "dev.soupslurpr.transcribro"
                )

                if(!privacyWhitelist.contains(externalPkg.value))
                    Tip(stringResource(R.string.voice_input_settings_backend_system_external_warning,
                        currOption?.name ?: externalPkg.value))
            }
        },

        //if(!systemVoiceInput.value) {
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_indication_sounds,
            subtitle = R.string.voice_input_settings_indication_sounds_subtitle,
            setting = ENABLE_SOUND
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_filter_made_up_text,
            subtitle = R.string.voice_input_settings_filter_made_up_text_subtitle,
            setting = VOICE_INPUT_FILTER_MADE_UP_TEXT
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        /*
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_verbose_progress,
            subtitle = R.string.voice_input_settings_verbose_progress_subtitle,
            setting = VERBOSE_PROGRESS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),
         */

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_use_personal_dict,
            subtitle = R.string.voice_input_settings_use_personal_dict_subtitle,
            setting = USE_PERSONAL_DICT
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_use_bluetooth_mic,
            subtitle = R.string.voice_input_settings_use_bluetooth_mic_subtitle,
            setting = PREFER_BLUETOOTH
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_audio_focus,
            subtitle = R.string.voice_input_settings_audio_focus_subtitle,
            setting = AUDIO_FOCUS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_suppress_symbols,
            setting = DISALLOW_SYMBOLS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_long_form,
            subtitle = R.string.voice_input_settings_long_form_subtitle,
            setting = CAN_EXPAND_SPACE
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_autostop_vad,
            subtitle = R.string.voice_input_settings_autostop_vad_subtitle,
            setting = USE_VAD_AUTOSTOP
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_filter_stock_phrases,
            subtitle = R.string.voice_input_settings_filter_stock_phrases_subtitle,
            setting = VOICE_INPUT_FILTER_STOCK_PHRASES
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_action_buttons,
            subtitle = R.string.voice_input_settings_action_buttons_subtitle,
            setting = VOICE_INPUT_ACTION_BUTTONS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_segmented_results,
            subtitle = R.string.voice_input_settings_segmented_results_subtitle,
            setting = VOICE_INPUT_SEGMENTED_RESULTS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        UserSetting(
            name = R.string.voice_input_settings_circle_over_keys,
            subtitle = R.string.voice_input_settings_circle_over_keys_subtitle,
            component = {
                // Keeps its stored value but can't be changed while dictating over the keyboard
                // is off; the original subtitle stays and a note is added underneath.
                val overKeyboard = useDataStoreValue(VOICE_INPUT_OVER_KEYBOARD)
                val base = stringResource(R.string.voice_input_settings_circle_over_keys_subtitle)
                SettingToggleDataStore(
                    title = stringResource(R.string.voice_input_settings_circle_over_keys),
                    setting = VOICE_INPUT_CIRCLE_OVER_KEYS,
                    subtitle = if (overKeyboard) base else base + "\n" +
                            stringResource(R.string.voice_input_settings_needs_over_keyboard_note),
                    disabled = !overKeyboard
                )
            }
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_over_keyboard,
            subtitle = R.string.voice_input_settings_over_keyboard_subtitle,
            setting = VOICE_INPUT_OVER_KEYBOARD
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_switch_mode_buttons,
            subtitle = R.string.voice_input_settings_switch_mode_buttons_subtitle,
            setting = VOICE_INPUT_SWITCH_MODE_BUTTONS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        UserSetting(
            name = R.string.voice_input_settings_hide_keyboard_button,
            subtitle = R.string.voice_input_settings_hide_keyboard_button_subtitle,
            component = {
                // Keeps its stored value but can't be changed while dictating over the keyboard
                // is off; the original subtitle stays and a note is added underneath.
                val overKeyboard = useDataStoreValue(VOICE_INPUT_OVER_KEYBOARD)
                val base = stringResource(R.string.voice_input_settings_hide_keyboard_button_subtitle)
                SettingToggleDataStore(
                    title = stringResource(R.string.voice_input_settings_hide_keyboard_button),
                    setting = VOICE_INPUT_HIDE_KEYBOARD_BUTTON,
                    subtitle = if (overKeyboard) base else base + "\n" +
                            stringResource(R.string.voice_input_settings_hide_button_needs_over_keyboard_note),
                    disabled = !overKeyboard
                )
            }
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        UserSetting(
            name = R.string.voice_input_settings_segment_pause,
            subtitle = R.string.voice_input_settings_segment_pause_subtitle,
        ) {
            val resources = LocalResources.current
            SettingSlider(
                title = stringResource(R.string.voice_input_settings_segment_pause),
                subtitle = stringResource(R.string.voice_input_settings_segment_pause_subtitle),
                setting = VOICE_INPUT_SEGMENT_PAUSE_MS,
                range = 200.0f..1500.0f,
                hardRange = 100.0f..3000.0f,
                transform = { it.roundToInt() },
                indicator = { resources.getString(R.string.abbreviation_unit_milliseconds, "$it") },
                steps = 12
            )
        }.copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_click_gestures,
            subtitle = R.string.voice_input_settings_click_gestures_subtitle,
            setting = VOICE_INPUT_CLICK_GESTURES
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_animate_bubble,
            subtitle = R.string.voice_input_settings_animate_bubble_subtitle,
            setting = ANIMATE_BUBBLE
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_inline_partial_result,
            subtitle = R.string.voice_input_settings_inline_partial_result_subtitle,
            setting = VOICE_INPUT_INLINE_PARTIAL_RESULT
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingNavigationItem(
            title = R.string.voice_input_settings_change_models,
            subtitle = R.string.voice_input_settings_change_models_subtitle,
            style = NavigationItemStyle.Misc,
            navigateTo = "languages"
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),
        //}
    )
)