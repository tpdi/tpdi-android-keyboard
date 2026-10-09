/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.keyboard.internal;

import static org.futo.inputmethod.latin.common.Constants.CODE_ACTION_0;
import static org.futo.inputmethod.latin.common.Constants.CODE_UNSPECIFIED;

import org.futo.inputmethod.latin.common.Constants;
import org.futo.inputmethod.latin.uix.actions.ActionRegistry;

import java.util.HashMap;

public final class KeyboardCodesSet {
    public static final String PREFIX_CODE = "!code/";
    public static final String ACTION_CODE_PREFIX = "action_";

    private static final HashMap<String, Integer> sNameToIdMap = new HashMap<>();

    private KeyboardCodesSet() {
        // This utility class is not publicly instantiable.
    }

    public static int getCode(final String name) {
        if(name.startsWith(ACTION_CODE_PREFIX)) {
            int id = CODE_ACTION_0 + ActionRegistry.INSTANCE.parseAction(name);
            if(id >= CODE_UNSPECIFIED) throw new RuntimeException("Action ID too high!");
            return id;
        }
        Integer id = sNameToIdMap.get(name);
        if (id == null) throw new RuntimeException("Unknown key code: " + name);
        return DEFAULT[id];
    }

    private static final String[] ID_TO_NAME = {
        "key_tab",
        "key_enter",
        "key_space",
        "key_shift",
        "key_capslock",
        "key_switch_alpha_symbol",
        "key_output_text",
        "key_delete",
        "key_settings",
        "key_shortcut",
        "key_action_next",
        "key_action_previous",
        "key_shift_enter",
        "key_language_switch",
        "key_emoji",
        "key_alpha_from_emoji",
        "key_to_number_layout",
        "key_to_alt_0_layout",
        "key_to_alt_1_layout",
        "key_to_alt_2_layout",
        "key_to_alpha_0_layout",
        "key_to_alpha_1_layout",
        "key_to_alpha_2_layout",
        "key_to_alpha_3_layout",
        "key_unspecified",
        "key_to_alt_3_layout",
        "key_escape",
        "key_home",
        "key_end",
        "key_page_up",
        "key_page_down",
        "key_forward_delete",
        "key_insert",
        "key_f1",
        "key_f2",
        "key_f3",
        "key_f4",
        "key_f5",
        "key_f6",
        "key_f7",
        "key_f8",
        "key_f9",
        "key_f10",
        "key_f11",
        "key_f12",
    };

    private static final int[] DEFAULT = {
        Constants.CODE_TAB,
        Constants.CODE_ENTER,
        Constants.CODE_SPACE,
        Constants.CODE_SHIFT,
        Constants.CODE_CAPSLOCK,
        Constants.CODE_SWITCH_ALPHA_SYMBOL,
        Constants.CODE_OUTPUT_TEXT,
        Constants.CODE_DELETE,
        Constants.CODE_SETTINGS,
        Constants.CODE_SHORTCUT,
        Constants.CODE_ACTION_NEXT,
        Constants.CODE_ACTION_PREVIOUS,
        Constants.CODE_SHIFT_ENTER,
        Constants.CODE_LANGUAGE_SWITCH,
        Constants.CODE_EMOJI,
        Constants.CODE_ALPHA_FROM_EMOJI,
        Constants.CODE_TO_NUMBER_LAYOUT,
        Constants.CODE_TO_ALT_0_LAYOUT,
        Constants.CODE_TO_ALT_1_LAYOUT,
        Constants.CODE_TO_ALT_2_LAYOUT,
        Constants.CODE_TO_ALPHA_0_LAYOUT,
        Constants.CODE_TO_ALPHA_1_LAYOUT,
        Constants.CODE_TO_ALPHA_2_LAYOUT,
        Constants.CODE_TO_ALPHA_3_LAYOUT,
        Constants.CODE_UNSPECIFIED,
        Constants.CODE_TO_ALT_3_LAYOUT,
        Constants.CODE_ESCAPE,
        Constants.CODE_HOME,
        Constants.CODE_END,
        Constants.CODE_PAGE_UP,
        Constants.CODE_PAGE_DOWN,
        Constants.CODE_FORWARD_DELETE,
        Constants.CODE_INSERT,
        Constants.CODE_F1,
        Constants.CODE_F2,
        Constants.CODE_F3,
        Constants.CODE_F4,
        Constants.CODE_F5,
        Constants.CODE_F6,
        Constants.CODE_F7,
        Constants.CODE_F8,
        Constants.CODE_F9,
        Constants.CODE_F10,
        Constants.CODE_F11,
        Constants.CODE_F12,
    };

    static {
        for (int i = 0; i < ID_TO_NAME.length; i++) {
            sNameToIdMap.put(ID_TO_NAME[i], i);
        }
    }
}
