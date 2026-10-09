package org.futo.inputmethod.latin.uix.settings.pages

import androidx.compose.ui.res.stringResource
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.SEND_KEY_CODES_RATHER_THAN_TEXT
import org.futo.inputmethod.latin.uix.SHOW_LOAD_LAYOUT_FROM_FILE
import org.futo.inputmethod.latin.uix.CASE_FORM_SUGGESTIONS
import org.futo.inputmethod.latin.uix.RECAPITALIZE_TOUCHED_WORD
import org.futo.inputmethod.latin.uix.KEY_LABEL_ANCHOR
import org.futo.inputmethod.latin.uix.KEY_LABEL_SCALE
import org.futo.inputmethod.latin.uix.SPACE_SWIPE_VERTICAL
import org.futo.inputmethod.latin.uix.STICKY_MODIFIER_KEYS
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.userSettingNavigationItem
import org.futo.inputmethod.latin.uix.VOICE_INPUT_ACTION_BUTTONS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_CLICK_GESTURES
import org.futo.inputmethod.latin.uix.VOICE_INPUT_MIC_KEY_BLUE
import org.futo.inputmethod.latin.uix.VOICE_INPUT_MIC_KEY_TOGGLE
import org.futo.inputmethod.latin.uix.VOICE_INPUT_NOISE_GATE
import org.futo.inputmethod.latin.uix.VOICE_INPUT_UNDO_KEY
import org.futo.inputmethod.latin.uix.VOICE_INPUT_TRIM_TRAILING_SILENCE
import org.futo.inputmethod.latin.uix.VOICE_INPUT_FILTER_MADE_UP_TEXT
import org.futo.inputmethod.latin.uix.VOICE_INPUT_FILTER_STOCK_PHRASES
import org.futo.inputmethod.latin.uix.VOICE_INPUT_INLINE_PARTIAL_RESULT
import org.futo.inputmethod.latin.uix.VOICE_INPUT_OVER_KEYBOARD
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SEGMENTED_RESULTS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_NO_CIRCLE_OVER_KEYS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_HIDE_KEYBOARD_BUTTON
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SWITCH_MODE_BUTTONS
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.SettingSlider
import org.futo.inputmethod.latin.uix.settings.SettingToggleDataStore
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import org.futo.inputmethod.latin.uix.settings.userSettingDecorationOnly
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore
import kotlin.math.roundToInt

/**
 * Every feature flag of this build on one page. These are the same toggles, with the same text,
 * as on the Typing and Voice input pages; they write the same stored settings, so changing one
 * here changes it there too. With every flag off the app behaves like the Play Store build.
 */
val TpdiFeaturesMenu = UserSettingsMenu(
    title = R.string.tpdi_features_title,
    navPath = "tpdiFeatures", registerNavPath = true,
    settings = listOf(
        userSettingDecorationOnly {
            ScreenTitle(stringResource(R.string.tpdi_features_title))
        },

        userSettingToggleDataStore(
            title = R.string.keyboard_settings_send_key_codes_rather_than_text,
            subtitle = R.string.keyboard_settings_send_key_codes_rather_than_text_subtitle,
            setting = SEND_KEY_CODES_RATHER_THAN_TEXT
        ),
        userSettingToggleDataStore(
            title = R.string.keyboard_settings_sticky_modifier_keys,
            subtitle = R.string.keyboard_settings_sticky_modifier_keys_subtitle,
            setting = STICKY_MODIFIER_KEYS
        ),
        userSettingDecorationOnly {
            SettingSlider(
                title = stringResource(R.string.keyboard_settings_key_label_size),
                subtitle = stringResource(R.string.keyboard_settings_key_label_size_subtitle),
                setting = KEY_LABEL_SCALE,
                range = 0.5f .. 1.5f,
                transform = { (it * 20f).roundToInt() / 20f },
                indicator = { "${(it * 100f).roundToInt()}%" }
            )
        },
        userSettingDecorationOnly {
            SettingSlider(
                title = stringResource(R.string.keyboard_settings_key_label_position),
                subtitle = stringResource(R.string.keyboard_settings_key_label_position_subtitle),
                setting = KEY_LABEL_ANCHOR,
                range = 0.0f .. 1.0f,
                transform = { (it * 20f).roundToInt() / 20f },
                indicator = { "${(it * 100f).roundToInt()}%" }
            )
        },
        userSettingToggleDataStore(
            title = R.string.keyboard_settings_space_swipe_vertical,
            subtitle = R.string.keyboard_settings_space_swipe_vertical_subtitle,
            setting = SPACE_SWIPE_VERTICAL
        ),
        userSettingToggleDataStore(
            title = R.string.keyboard_settings_case_form_suggestions,
            subtitle = R.string.keyboard_settings_case_form_suggestions_subtitle,
            setting = CASE_FORM_SUGGESTIONS
        ),
        userSettingToggleDataStore(
            title = R.string.keyboard_settings_recapitalize_touched_word,
            subtitle = R.string.keyboard_settings_recapitalize_touched_word_subtitle,
            setting = RECAPITALIZE_TOUCHED_WORD
        ),
        userSettingToggleDataStore(
            title = R.string.keyboard_settings_show_load_layout_from_file,
            subtitle = R.string.keyboard_settings_show_load_layout_from_file_subtitle,
            setting = SHOW_LOAD_LAYOUT_FROM_FILE
        ),
        userSettingNavigationItem(
            title = R.string.key_hints_title,
            subtitle = R.string.key_hints_subtitle,
            style = NavigationItemStyle.Misc,
            navigateTo = "keyhints"
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_inline_partial_result,
            subtitle = R.string.voice_input_settings_inline_partial_result_subtitle,
            setting = VOICE_INPUT_INLINE_PARTIAL_RESULT
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_segmented_results,
            subtitle = R.string.voice_input_settings_segmented_results_subtitle,
            setting = VOICE_INPUT_SEGMENTED_RESULTS
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_filter_made_up_text,
            subtitle = R.string.voice_input_settings_filter_made_up_text_subtitle,
            setting = VOICE_INPUT_FILTER_MADE_UP_TEXT
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_filter_stock_phrases,
            subtitle = R.string.voice_input_settings_filter_stock_phrases_subtitle,
            setting = VOICE_INPUT_FILTER_STOCK_PHRASES
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_noise_gate,
            subtitle = R.string.voice_input_settings_noise_gate_subtitle,
            setting = VOICE_INPUT_NOISE_GATE
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_trim_trailing_silence,
            subtitle = R.string.voice_input_settings_trim_trailing_silence_subtitle,
            setting = VOICE_INPUT_TRIM_TRAILING_SILENCE
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_undo_key,
            subtitle = R.string.voice_input_settings_undo_key_subtitle,
            setting = VOICE_INPUT_UNDO_KEY
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_mic_key_toggle,
            subtitle = R.string.voice_input_settings_mic_key_toggle_subtitle,
            setting = VOICE_INPUT_MIC_KEY_TOGGLE
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_mic_key_blue,
            subtitle = R.string.voice_input_settings_mic_key_blue_subtitle,
            setting = VOICE_INPUT_MIC_KEY_BLUE
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_click_gestures,
            subtitle = R.string.voice_input_settings_click_gestures_subtitle,
            setting = VOICE_INPUT_CLICK_GESTURES
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_action_buttons,
            subtitle = R.string.voice_input_settings_action_buttons_subtitle,
            setting = VOICE_INPUT_ACTION_BUTTONS
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_over_keyboard,
            subtitle = R.string.voice_input_settings_over_keyboard_subtitle,
            setting = VOICE_INPUT_OVER_KEYBOARD
        ),
        UserSetting(
            name = R.string.voice_input_settings_no_circle_over_keys,
            subtitle = R.string.voice_input_settings_no_circle_over_keys_subtitle,
            component = {
                val overKeyboard = useDataStoreValue(VOICE_INPUT_OVER_KEYBOARD)
                val base = stringResource(R.string.voice_input_settings_no_circle_over_keys_subtitle)
                SettingToggleDataStore(
                    title = stringResource(R.string.voice_input_settings_no_circle_over_keys),
                    setting = VOICE_INPUT_NO_CIRCLE_OVER_KEYS,
                    subtitle = if (overKeyboard) base else base + "\n" +
                            stringResource(R.string.voice_input_settings_needs_over_keyboard_note),
                    disabled = !overKeyboard
                )
            }
        ),
        UserSetting(
            name = R.string.voice_input_settings_hide_keyboard_button,
            subtitle = R.string.voice_input_settings_hide_keyboard_button_subtitle,
            component = {
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
        ),
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_switch_mode_buttons,
            subtitle = R.string.voice_input_settings_switch_mode_buttons_subtitle,
            setting = VOICE_INPUT_SWITCH_MODE_BUTTONS
        ),
    )
)
