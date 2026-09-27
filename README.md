# Alphabet Launcher

A custom Android launcher-style interface built with **Kotlin** and **Jetpack Compose**.

The app provides a vertical A–Z alphabet navigation bar. Dragging a finger over the alphabet bends the bar toward the finger and displays applications installed on the device that begin with the selected letter.

## Features

- Live clock and date on the home screen
- Displays installed launcher applications
- Favourite application area on the home screen
- Vertical A–Z alphabet navigation
- Custom finger-drag interaction
- Alphabet bends toward the user's finger
- Selected letter is highlighted with a circular bubble
- Applications are filtered by the selected letter
- Displays `No apps` when no application matches
- Tapping an application launches it
- Smooth return animation after releasing the alphabet
- Installed application list is loaded once and cached
- Android 11+ package visibility support

## Technology

- Kotlin
- Jetpack Compose
- Android SDK
- Android PackageManager
- Kotlin Coroutines

## How It Works

### Installed applications

The application queries Android's `PackageManager` for activities that contain:

- `ACTION_MAIN`
- `CATEGORY_LAUNCHER`

The resulting application information is converted into a local `LauncherApp` data model containing:

- Application name
- Package name
- Application icon

The application list is loaded when the launcher starts rather than querying `PackageManager` during every drag event.

### Alphabet interaction

The A–Z alphabet is drawn using a Compose `Canvas`.

During a drag:

1. The finger position is tracked.
2. The vertical position is converted into an alphabet index.
3. The selected letter is calculated.
4. Applications beginning with that letter are displayed.
5. Each alphabet character receives a distance-based horizontal offset.
6. The offset creates the curved/bending effect around the finger.

The bending effect is calculated using custom mathematical functions rather than an external animation library.

### Release animation

When the user releases the alphabet, the current horizontal displacement is captured and animated back to its resting position using a Compose spring animation.

## Android Package Visibility

The launcher declares the required launcher intent in the manifest so that Android 11+ can discover launchable applications.

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
