To properly display a high-resolution image as a full-screen splash screen in an Android app targeting SDK 35 (Android 15), you need to leverage the SplashScreen API (introduced in API 31/Android 12) while working around its limitations. The API's default behavior centers a small icon (masked to a circle, typically 192–288 dp) on a solid color background, which is why your 512x512 px icon asset appears zoomed-in and pixelated when stretched—it's not designed for full-screen use and gets scaled/masked aggressively.

Instead, make the system splash screen transparent (or minimal) so your custom full-screen image shows through via the activity's `windowBackground`. This avoids duplication and handles high-res scaling/centering. Note: Transparent backgrounds work on most devices but may render as white on some (e.g., certain OEM skins); test thoroughly. If issues arise, fall back to a matching solid color background with no icon.

### Step 1: Add the SplashScreen Compat Library
This ensures compatibility down to API 23. In your app module's `build.gradle`:
```
dependencies {
    implementation "androidx.core:core-splashscreen:1.0.1"
}
```
Sync and rebuild.

### Step 2: Prepare Your High-Res Image
- Place your original high-resolution PNG (e.g., at least 1440x3200 px for modern screens to avoid upscaling/pixelation) in `res/drawable-nodpi/` as `splash_image.png`. The `-nodpi` qualifier treats it as density-independent, displaying at native pixel size without automatic scaling.
- Create a drawable XML for centering without stretching (clips edges if image > screen size, like "centerCrop"; use "fitCenter" alternative below if preferred):
  In `res/drawable/splash_background.xml`:
  ```
  <?xml version="1.0" encoding="utf-8"?>
  <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
      <item>
          <bitmap android:src="@drawable/splash_image"
                  android:gravity="center" />
      </item>
  </layer-list>
  ```
  - This centers the image horizontally/vertically at its native size. If your image's aspect ratio matches common screens (e.g., 9:20), it fills nicely with minimal clipping. For "fitCenter" (scales down to fit without clipping, adding letterboxing if needed), wrap in a `<scale>` drawable instead:
    ```
    <?xml version="1.0" encoding="utf-8"?>
    <scale xmlns:android="http://schemas.android.com/apk/res/android"
           android:drawable="@drawable/splash_image"
           android:scaleGravity="center"
           android:scaleWidth="100%"
           android:scaleHeight="100%" />
    ```

### Step 3: Create a Transparent Splash Theme
In `res/values/themes.xml` (create if needed):
```
<style name="Theme.App.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@android:color/transparent</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/transparent_icon</item>
    <item name="windowSplashScreenAnimationDuration">1</item> <!-- Minimal duration -->
    <item name="postSplashScreenTheme">@style/Theme.App.Main</item> <!-- Your app's main theme -->
</style>
```
- Create a 1x1 transparent PNG (fully transparent pixel) in `res/drawable/transparent_icon.png` (use any image editor).
- In your main app theme (`Theme.App.Main`), add:
  ```
  <item name="android:windowBackground">@drawable/splash_background</item>
  ```

### Step 4: Update the Manifest
In `AndroidManifest.xml`, apply the splash theme to your launch activity:
```
<activity
    android:name=".MainActivity"
    android:theme="@style/Theme.App.Splash"
    ... >
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Step 5: Install the SplashScreen in Your Launch Activity
In your launch activity's `onCreate` (before `super.onCreate` and `setContentView`):
```
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Your init code...
    }
}
```
- If you need to keep the splash visible longer (e.g., for loading), add:
  ```
  val splashScreen = installSplashScreen()
  splashScreen.setKeepOnScreenCondition { /* Return true until ready (e.g., data loaded) */ !isReady }
  ```

### Step 6: Build and Test
- Rebuild and run the app.
- The system splash should be invisible, revealing your centered, high-res image immediately via `windowBackground`. No zooming/pixelation since it's not forced into the small icon mask—it's handled at native resolution.
- Test on emulators/physical devices with different resolutions (e.g., Pixel 8, Samsung S23) and densities (xxhdpi/xxxhdpi). If clipping occurs, adjust your image's dimensions/aspect or switch to the `<scale>` drawable.
- For dark mode, duplicate the theme in `res/values-night/themes.xml` with a dark-variant image.

### Additional Notes
- **Why this works**: The SplashScreen API overlays the system splash until your activity draws its first frame. Making it transparent lets the underlying `windowBackground` show through as the "splash."
- **Recent Changes (up to Android 15/SDK 35)**: No major updates to the SplashScreen API since API 33 (behavior control). The core elements (background color, icon masking) remain the same—full-screen raster images aren't natively supported as backgrounds, hence the workaround.
- **Alternatives if Transparent Fails**:
  - Set `windowSplashScreenBackground` to a dominant color from your image (use Android Studio's color picker) and omit the icon for a minimal overlay.
  - Use a vector drawable (SVG import in Android Studio) for your image if possible—vectors scale perfectly without pixelation.
  - For more control, move the image to an `ImageView` in your activity layout with `android:scaleType="centerCrop"` or `"fitCenter"`, but match the system splash closely to avoid flashing.
- Official docs confirm the API's focus on simple icon+color setups; custom full-screen is a community workaround.

This setup should give a crisp, centered full-screen splash without pixelation on high-res devices. If your image still looks off, share more details on its resolution/aspect.
