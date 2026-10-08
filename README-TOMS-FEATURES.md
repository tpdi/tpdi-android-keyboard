# Tom's Futo Keyboard: added features

Every feature below is behind its own setting, off by default (unless noted), so with all of them off the keyboard behaves like the Play Store build. Each one is on its own page in the settings (Typing, Voice input or Clipboard manager), and every flag is duplicated on Settings > Tom's Features in the combined build.

- Dictate over the keyboard: keep the keyboard visible while dictating, with a Listening bar, Undo and a stop microphone (#25).
- Hide-keyboard button: a chevron on the dictation bar collapses the keys and brings them back (#35).
- Mode-switch buttons: switch between dictating over the keyboard and the full voice window without ending the session (#36).
- Volume circle over the keys: choose whether the volume circle is drawn over the keys (#34).
- Undo history: remembers each dictated segment so it can be undone (#23).
- Undo and Enter buttons beside the voice bubble (#24).
- Undo key undoes dictation: the keyboard's Undo key removes the last dictated segment while dictating (#60).
- Microphone key toggles: pressing the voice input key again stops and transcribes the recording (#58).
- Click gestures: two sharp clicks near the microphone press Enter (#22).
- Drop made-up text: discard repeated loops and text invented from silence (#21).
- Drop stock phrases: discard results that are only phrases like "Thanks for watching" (#30).
- Automatic noise gate: learn the background level and turn audio near it down (#50).
- Trim trailing silence: cut the quiet end off audio before it is transcribed (#54).
- Inline partial result: show words recognized so far inside the voice bubble (#20).
- Send key codes rather than text: Tab, Enter, Escape, Home, End, Page Up/Down, Forward Delete, Insert and F1 to F12 as real key events (#16, merged).
- Sticky Ctrl and Alt keys: tap to latch; the next key is sent with real Ctrl or Alt meta state (#40).
- Load layout from file: show the Load from file button in the custom layout editor (#19).
- Key hint size: slider to scale the small hint characters on keys (#44).
- Key hint brightness: slider from 0% to 100% to dim the hint characters (#56).
- Fourth alternate page: a layout key to switch to a fourth page (#39).
- Dismiss-keyboard key: a layout action that hides the keyboard, long press opens Settings (#46).
- Show sensitive quick clips: show the quick-copy chip text even when the source app marks it sensitive (#52, merged).
- Tom's Features page: one settings page listing every flag (#28).
- KASROZ layout: an arrow-key layout with symbols on long press, loadable from file (kb repo, PR #10).
