# Fixes

- When opening a book, going to search, and typing, the typing behavior keeps getting stuck at the first letter, typing additional characters causes some visual jumping but additional letters don't appear in the search field

- Update Segmented Buttons: The "Original" reading option seems to be implemented properly, but the button itself in the settings menu is causing the button to overflow the edge of the app. Reference latest screenshots under ~/Pictures/screenshots/. Can you please evaluate buttons, sliders, and menu formats, etc. throughout my app and provide some update recommendations that align with Material 3 guidelines? I want a clean, minimal, yet usable interface that is consistent throughout my app. I think Segmented buttons may still be the proper solution for many options, so perhaps it is a matter of scaling font or similar to suit devices of varying resolutions and similar.

- I can successfully open books from Files or other apps, but doing so creates duplicates if the app was already imported via browse. We need to add some checks here, such as if an app is already in the library and the user opens from a file manager, the file should open in the app but use the version already added and should open to the current progress for that book.

## Next

- I don't see the Dictionary implementation from my original to-do.md. In an earlier session, you asked if this should be put-off, and I think I suggested either creating a separate branch or simply holding off on the implementation entirely until the rest of the features were added. Once we fix the above, let's work on this dictionary implementation including the extra setup screen when the app is opened for the first time. Was this checklist or structure stored somewhere that we can now reference?
