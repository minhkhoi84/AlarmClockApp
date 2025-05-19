# ⏰ Alarm Clock App with Jetpack Compose

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Platform](https://img.shields.io/badge/platform-android-blue)

## Overview

A modern, feature-rich alarm clock application built entirely with [Jetpack Compose](https://developer.android.com/jetpack/compose) for Android. This project delivers a beautiful, extensible, and user-friendly experience for alarms, timers, stopwatches, and sleep tracking features.

## Screenshots

![App Screenshot](https://user-images.githubusercontent.com/2553497/212576539-355770d2-bed4-4fd8-ad55-739252af5b22.png)

## Installation Guide

1. **System Requirements:**
   - Android Studio Flamingo or higher
   - Android SDK 33+
   - Kotlin 1.7.20+
   - Gradle 7.4+
   - Android device/emulator running API 26+

2. **Clone the repository:**
   ```bash
   git clone https://github.com/minhkhoi/Alarm-Clock-App-Tutorial.git
   ```

3. **Open with Android Studio** and build the project

4. **Run on your device or emulator**

## Features & Usage

- **Alarm Management:**
  - Add new alarms: Tap the plus icon, set your desired time, and save
  - Edit/Delete existing alarms: Tap on any alarm to modify or remove it
  
- **Sleep Mode:**
  - Navigate to the Bedtime tab to set and manage your sleep schedule
  - Create notes and reminders for bedtime routines

- **Time Tools:**
  - Stopwatch: Track elapsed time with precision
  - Timer: Set countdown timers for various activities

- **World Clock:**
  - View time across different time zones
  - Quickly check local times in cities around the world

## Contributing

We welcome all contributions! Please follow these steps to contribute:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to your branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

For major changes, please open an issue first to discuss what you would like to change.

## Troubleshooting

- **Build Failures:** Verify your Android Studio, SDK, and Gradle versions match the requirements
- **Alarm Issues:** Ensure the app has proper notification and background permissions
- **Device Compatibility:** Some features may require specific hardware support
- **Additional Help:** Please create an issue on GitHub or contact support

## Dependencies

- androidx.core:core-ktx:1.9.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.5.1
- androidx.activity:activity-compose:1.6.1
- androidx.compose.* (Material3, UI, Tooling, Test)

## Support & Contact

- YouTube: [@HaniTech](https://www.youtube.com/@HaniTech)
- Email: minhkhoi@gmail.com
- GitHub Issues: For bug reports and feature requests

## Acknowledgements

- [Minh Khoi](https://github.com/minhkhoi) - Project Author & Developer
- Jetpack Compose Community for guidance and resources

## Technical References

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material 3 Design System](https://m3.material.io/)

## License

This project is provided for educational purposes. Please contact the author for commercial use permissions.

## Version History

- v1.0: Initial Release
  - Core alarm functionality
  - Timer, stopwatch, and sleep tracking features
  - Material Design implementation

## Known Issues

- Alarm reliability may be affected on devices with aggressive battery optimization settings
- Some UI elements may appear differently across various screen sizes and densities

## Project Structure

```
app/
└── src/
    └── main/
        ├── java/io/github/minhkhoi/alarmclock/
        │   ├── MainActivity.kt
        │   ├── AlarmReceiver.kt
        │   ├── BootReceiver.kt
        │   ├── AlarmClockApplication.kt
        │   └── ui/
        │       ├── AlarmScreen.kt
        │       ├── BedtimeScreen.kt
        │       ├── StopwatchScreen.kt 
        │       ├── TimerScreen.kt 
        │       └── theme/ 
        └── res/