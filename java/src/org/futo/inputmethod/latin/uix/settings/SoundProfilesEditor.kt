package org.futo.inputmethod.latin.uix.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.SOUND_PROFILES_CUSTOM
import org.futo.inputmethod.latin.uix.SOUND_PROFILE_ACTIVE
import org.futo.inputmethod.latin.uix.SoundProfiles

/** Rename, delete and add your own sound profiles. The built-in ones can't be changed. */
@Composable
fun SoundProfilesEditor() {
    val context = LocalContext.current
    val custom = useDataStore(SOUND_PROFILES_CUSTOM)
    val active = useDataStore(SOUND_PROFILE_ACTIVE)
    val profiles = SoundProfiles.parseCustom(custom.value)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(stringResource(R.string.sound_profiles_editor_title), style = MaterialTheme.typography.titleMedium)
        if (profiles.isEmpty()) {
            Text(stringResource(R.string.sound_profiles_editor_none), style = MaterialTheme.typography.bodyMedium)
        }
        profiles.forEachIndexed { index, profile ->
            var text by remember(index) { mutableStateOf(profile.name) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newName ->
                        text = newName
                        if (newName.isNotBlank()) {
                            custom.setValue(SoundProfiles.toJson(profiles.mapIndexed { i, p ->
                                if (i == index) p.copy(name = newName) else p
                            }))
                            if (active.value == profile.name) active.setValue(newName)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    custom.setValue(SoundProfiles.toJson(profiles.filterIndexed { i, _ -> i != index }))
                    if (active.value == profile.name) active.setValue("")
                }) { Text(stringResource(R.string.sound_profiles_editor_delete)) }
            }
        }
        TextButton(onClick = {
            val saved = SoundProfiles.snapshot(context, "My profile ${profiles.size + 1}")
            custom.setValue(SoundProfiles.toJson(profiles + saved))
            active.setValue(saved.name)
        }) { Text(stringResource(R.string.sound_profiles_save_current)) }
    }
}
