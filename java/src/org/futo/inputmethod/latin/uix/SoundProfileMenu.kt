package org.futo.inputmethod.latin.uix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.LatinIME

/** The menu shown by a long press on the microphone key: pick a profile, or listen and choose. */
object SoundProfileMenu {
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun show(ime: LatinIME, text: String, options: List<DialogRequestItem>) {
        ime.uixManager.activeDialogRequest.value = ActiveDialogRequest(text, options) {}
        ime.uixManager.activeDialogRequestDismissed.value = false
    }

    fun open(ime: LatinIME) {
        val active = ime.getSetting(SOUND_PROFILE_ACTIVE)
        val items = mutableListOf(
            DialogRequestItem("Listen and choose") { listenAndChoose(ime) }
        )
        SoundProfiles.all(ime).forEach { profile ->
            items.add(DialogRequestItem(if (profile.name == active) "✓ ${profile.name}" else profile.name) {
                SoundProfiles.apply(ime, profile)
            })
        }
        items.add(DialogRequestItem("Save current as new") {
            val custom = SoundProfiles.parseCustom(ime.getSetting(SOUND_PROFILES_CUSTOM))
            val saved = SoundProfiles.snapshot(ime, "My profile ${custom.size + 1}")
            scope.launch {
                ime.setSetting(SOUND_PROFILES_CUSTOM, SoundProfiles.toJson(custom + saved))
                ime.setSetting(SOUND_PROFILE_ACTIVE, saved.name)
            }
        })
        show(ime, "Sound profile", items)
    }

    private fun listenAndChoose(ime: LatinIME) {
        scope.launch {
            val room = SoundCalibration.listen()
            if (room == null) {
                show(ime, "Could not listen to the room (microphone busy?)", listOf(DialogRequestItem("OK") {}))
                return@launch
            }
            val name = SoundCalibration.choose(room)
            SoundProfiles.all(ime).firstOrNull { it.name == name }?.let { SoundProfiles.apply(ime, it) }
            show(
                ime,
                "Chose \"$name\" (room %.0f dB, spread %.0f dB)".format(room.medianDb, room.spreadDb),
                listOf(DialogRequestItem("OK") {})
            )
        }
    }
}
