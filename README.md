# Mobile Activity Tracking Application
## Overview <br>
This Android mobile application is part of a comprehensive system designed to detect and monitor human physical activities in real-time. The system consists of a measurement wristband and this mobile app, working together to identify and display activities such as standing, walking, running, or unknown movements.

## Features
- **User Authentication**: Secure registration and login through Supabase, enabling user management on the server side.
- **Device Connectivity**: Connect seamlessly with a dedicated measurement wristband via Bluetooth Low Energy (BLE).
- **Real-Time Activity Detection**: Receive live data from the wristband to detect the current physical activity.
- **Data Visualization**: Display measurement results through intuitive charts for better understanding.
- **Data Filtering**: Filter results by hours, weeks, or months to analyze activity patterns over time.
- **Offline Functionality**: Operate without an internet connection, with automatic synchronization to Supabase when reconnected.
- **Session Management**: Save session keys locally to prevent the need for repeated logins.

## Technologies Used

- **Programming Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Networking**: Retrofit
- **Local Storage**: Data Store (for session keys), Room (for offline data storage)
- **Backend**: Supabase (with PostgreSQL)
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Asynchronous Programming**: Coroutines
- **Unit Testing**: JUnit4
- **Communication Protocol**: Bluetooth Low Energy (BLE)

## Architecture and Design

The application is built with clean code principles and emphasizes modularity and scalability:

- **MVVM Pattern**: Ensures a clear separation of concerns and promotes maintainable code.
- **Hilt Dependency Injection**: Facilitates modularization and easier management of dependencies.
- **Kotlin Coroutines**: Enables asynchronous programming for smooth and responsive user experiences.
- **Room Database**: Provides local data storage for offline functionality.
- **Data Store**: Secures session key storage to enhance user experience.

## Data Management

- **Offline Capability**: All data is stored locally using Room, allowing the app to function without an internet connection.
- **Synchronization**: Data synchronizes with the Supabase backend once an internet connection is re-established.
- **Session Persistence**: Session keys are stored using Data Store, eliminating the need for users to log in every time they use the app.


