# GreenStreak

**GreenStreak** is a specialized Android Live Wallpaper that fetches and visualizes GitHub contribution graphs in real-time. It is designed for developers who want to stay motivated by seeing their "green squares" directly on their home screen, specifically optimized for the year 2026.

## 🚀 Project Overview

The app transforms a user's GitHub contribution history into a beautiful, minimalist heatmap wallpaper. It handles data fetching, periodic updates, and canvas rendering with a focus on aesthetics and performance.

### Key Features
- **Real-time Visualization**: Displays the 2026 GitHub contribution heatmap.
- **Dynamic Layout**: Custom rendering engine that avoids overlap with the system clock.
- **Motivational Quotes**: Randomized developer-focused quotes displayed on the wallpaper.
- **Automatic Updates**: Background sync using WorkManager to keep the heatmap fresh.
- **Dark/Light Mode**: Adapts colors based on system theme.

---

## 🏗 Architecture & Flow

The project follows a clean repository-based architecture:

### 1. Data Layer (`com.rahul.githubwallpaper.data`)
- **GitHubApi.kt**: Interface for GraphQL API calls to fetch contribution data.
- **GitHubRepository.kt**: The central hub for data operations. It fetches data from the API and caches it locally using **Android DataStore**.
- **Contribution Models**: Classes like `CachedContributionData` and `Week` define the structure of the data.

### 2. Wallpaper Service (`com.rahul.githubwallpaper.wallpaper`)
- **GitHubWallpaperService.kt**: Extends `WallpaperService`. It manages the `Engine` that listens for visibility changes and triggers redraws.
- **HeatmapRenderer.kt**: The core rendering engine. It uses Android's `Canvas` API to draw the layout:
    - **Header**: Displays "X contributions in 2026".
    - **Quote**: A centered motivational quote (with multi-line wrapping logic).
    - **Graph**: The heatmap grid, fixed at 36% height to prevent clock overlap.

### 3. Background Sync (`com.rahul.githubwallpaper.worker`)
- **UpdateWorker.kt**: Leverages **WorkManager** to periodically refresh GitHub data in the background without draining battery.

### 4. UI Layer (`com.rahul.githubwallpaper`)
- **MainActivity.kt**: Built with **Jetpack Compose**. Allows users to configure their GitHub username and trigger manual updates.

---

## 🛠 Technical Stack & Versions

| Component | Version |
|-----------|---------|
| **Kotlin** | `1.9.21` |
| **Gradle** | `8.2` |
| **Android Gradle Plugin (AGP)** | `8.2.1` |
| **Compile SDK** | `34` |
| **Min SDK** | `26` |
| **Target SDK** | `34` |
| **Jetpack Compose BOM** | `2023.03.00` |
| **WorkManager** | `2.8.1` |
| **DataStore** | `1.0.0` |
| **Retrofit** | `2.9.0` |
| **Jsoup** | `1.16.1` (used for fallback scraping) |

---

## 🔒 Logic, Privacy & Security

- **Username-based**: Currently fetches data from public GitHub profiles.
- **Data Privacy**: No personal data other than the contribution counts is stored.
- **Security Goal**: Future iterations should focus on secure token management if private repository tokens are implemented.
- **Rendering Logic**: Uses vertical offsets (`yPos`) as percentages of screen height to ensure responsiveness across different screen sizes.

---

## 🛠 Future Development (AI Roadmap)

If you are an AI assistant helping with this project:
1. **Robustness**: Improve the error handling in `GitHubRepository` when the API limit is reached.
2. **Themes**: Add support for custom color palettes (e.g., Solarized, Dracula).
3. **Security**: Implement EncryptedSharedPreferences or similar for any future sensitive data.
4. **Logic**: Enhance the `HeatmapRenderer` to support different year views or a "Current Streak" focus.

---
*Developed by Rahul - Optimized for 2026.*