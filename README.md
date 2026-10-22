# 📸 Travel Journal

> **"Don't just take photos. Tell a story."**

Travel Journal is a smart Android application that turns your camera roll into a living, breathing travel log. By leveraging **Geolocation**, **Google Maps**, and **Generative AI**, it organizes your memories, maps your journey, and even writes the captions for you.

---

## 🚀 Key Features

### 📍 Travel Mapping
Automatically pins your uploaded photos to an interactive map based on their geospatial metadata. See exactly where every memory happened.

### 🎬 Movie Mode (Time-Warp Storytelling)
Select a "trio" of your favorite moments, and Travel Journal stitches them into a cinematic highlight reel with background music.

### 📒 Digital Scrapbook
A nostalgic, auto-generated collage mode that arranges your photos into beautiful layouts with customizable backgrounds.

### 🧠 AI-Powered Context
* **Auto-Captioning:** The app sends your images to our backend, where the **Llama 3** model (via Groq) analyzes the scene and generates witty, descriptive captions.
* **Smart Caching:** To save costs and speed up load times, captions are cached in **MongoDB**. If you view a memory twice, it loads instantly from the cache.

---

## 🛠 Tech Stack & Architecture

We built Travel Journal as a **Full-Stack Kotlin Monolith**, ensuring type safety and consistency from the UI all the way to the database.

### 📱 Android Client (The Face)
* **Language:** Kotlin (100%)
* **UI Framework:** **Jetpack Compose** (Modern, declarative UI toolkit)
* **Maps:** **Google Maps SDK for Android** (Composable Maps library)
* **Networking:** **Retrofit** & **OkHttp** (For communicating with the backend)
* **Async Processing:** **Kotlin Coroutines** (For non-blocking background tasks like image uploading)
* **Image Loading:** **Coil** (Coroutines Image Loader)

### ☁️ Backend Server (The Brain)
* **Framework:** **Spring Boot 3** (Kotlin)
* **Database:** **MongoDB** (NoSQL storage for flexibility with image metadata)
* **AI Engine:** **Groq API** (Running Llama 3 for ultra-fast inference)
* **Tunneling:** **ngrok** (Exposes localhost to the Android device securely)

---

## ⚡ Getting Started

### Prerequisites
* **Android Studio** (Koala or newer)
* **IntelliJ IDEA** (Recommended for Backend)
* **MongoDB** (Local or Atlas)
* **ngrok** account

### 1. Backend Setup
1.  Open the `backend/` folder in IntelliJ or any desired IDE.
2.  Create a `.env` file in `src/main/resources` (or set Environment Variables in Run Configuration):
    ```env
    MONGO_URI=mongodb+srv://<user>:<pass>@cluster.mongodb.net/journaldb
    GROQ_API_KEY=gsk_your_groq_api_key_here
    PORT=8080
    ```
3.  Run `JournAIApplication.kt`.
4.  Expose port 8080: `ngrok http 8080`. Copy the forwarded URL (e.g., `https://a1b2.ngrok-free.app`).

### 2. Client Setup
1.  Open the `client/` folder in Android Studio.
2.  Navigate to `utils/Constants.kt` (or wherever `BASE_URL` is defined).
3.  Paste your ngrok URL:
    ```kotlin
    const val BASE_URL = "[https://a1b2.ngrok-free.app/api/](https://a1b2.ngrok-free.app/api/)"
    ```
4.  Sync Gradle and run on an Emulator or Physical Device.

---

## 👥 Contributors

This project was built with ❤️ by:

* **Kushi**
* **Joshua**
* **Louise**
* **Siddhesh**
* **Rachit**

---
