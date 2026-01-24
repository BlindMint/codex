I want to modify some of the settings for my speed reader.
## main reader

- In my current app, I have settings called "OSD" that actually refer to the forward/back arrows and the play/pause button in the middle of the screen. This feels inaccurate (OSD should represent the entire on-screen display including top and bottom items), let's rename the forwarsd/back/play/pause button object "Playback controls"
- I want to slightly increase the size of the "Playback controls" give it a static position vertically. It should be ~20% height from the bottom of the screen on mobile phones, and ~25% from the bottom on tablets. Increase the size of the Play/pause button (keep the forward/back buttons)
## speed reader settings menu

### general tab

- Fix auto-hide
- The toggle setting that is is just called "OSD" should be called "Playback controls" - this should still toggle whether playback controls in the middle of the screen are visible or not. Users should have enough intuitive actions such as tapping to play/pause and edge tapping to forward/back to hide the Playback controls if they desire. Make sure that when Playback controls is toggled off (hidden) these items stay hidden even when "Auto-hide OSD" is enabled (playback should stay hidden even after playing/pausing and other OSD items fade back in)
- Remove the settings under the General tab for "OSD height" (we are setting a static height) and "OSD separation" - use the current default value that is used for OSD separation.


### focus tab

- I would like to adjust the default Focal point position based on screen size 38% is working well for most large phones, but 45% looks better on tablets.
- Add a new setting called "Focus indicators" under the current "Focal point position" slider. This new "Focus indicators" setting is going to merge and set some more generic defaults for Horizontal bars and Vertical indicators - users will be able to choose between a few set default configurations rather than configuring the existing settings for these individually. After we combine these settings into "Focus indicators", we will remove the original settings. This new "Focus indicators" setting should be a segmented button with 3 options: "Off", "Lines", and "Arrows". "Lines" should be default. When set to off, the horizontal bars and vertical indicators are hidden (this setting shouldn't directly affect other speed reader UI items). I want to combine horizontal bars and vertical lines. Vertical lines should still always point to the accent character (when "Center word" is disabled). Horizontal bars and Vertical indicators should use the same, default thickness: use the value that is currently the 3rd option in the Horizontal bars thickness slider (this shows as 3px in the UI, this might need to be converted to a corresponding "dp" or "sp" value to properly scale per material 3 guidelines). Vertical indicators length should be set to use the value that is currently the 2nd option on the Vertical indicators length slider. Use the current default for Horizontal bar length. The "Arrows" option under "Focus indicators" should show the "empty arrows" that are currently used under "Vertical indicator type", but we need to move these further away from the word by default (in code, they should have a higher "distance from word" value than the "Lines", I think this distance should be something around ~65dp.
- Rename the "Horizontal bars color" section to "Focus indicators colors", and place it below the new "Focus indicators" setting and above the Accent character setting.

- When this update is complete, I should still have two tabs under my speed reader settings: General and Focus. Their contents are described below:

### general tab post update

- Words per minute slider
- Manual sentence pause
- Auto hide OSD
- Playback controls (previously "OSD")
- Font size
- Color preset
- Background
- Speed reading custom font

### Focus tab post update

- Center word
- Focal point position
- Focus Indicators
	- has 3 options: Off, Lines, Arrows - Lines is default
- Focus indicators colors
- Accent character
- Accent color
