**WeatherApp: Part 2**

This Android application use Open-Meteo API to retrieve weather information, either current or historical. It empowers users to input a location (latitude/longitude) and date to acquire the corresponding maximum and minimum temperatures for that specific time and place.

**Technologies**

- Kotlin: The modern, concise programming language used for Android development.
- Jetpack Compose: A declarative UI framework for building responsive and efficient user interfaces.
- Retrofit: A type-safe HTTP client for Android, simplifying interaction with RESTful APIs.
- Gson: A library used to convert JSON data into Kotlin data objects and vice versa.
- Room Persistence Library: Provides an abstraction layer for SQLite, making it easier to work with the local database.
- ViewModel (Android Architecture Component): Facilitates data management and separation of concerns between UI and data logic.
- Coroutines: Enable asynchronous operations (like networking and database access) without blocking the main thread.

**Code Logic**

1. **User Interface (UI) and Data Flow:**
    - **`MainActivity.kt`:** This is the starting point. It establishes the user interface (UI) using Jetpack Compose and creates a ViewModel instance to manage data.
    - **`WeatherApp.kt`:** This function constructs the UI's visual structure using composables. It retrieves data from the ViewModel to display weather information or error messages.
    - **ViewModel (`WeatherViewModel.kt`)** acts as the central communication hub:
        - Holds the weather data (current or historical).
        - Validates user input (date, latitude, longitude).
        - Handles interactions with the Open-Meteo API:
            - Fetches weather data based on user input (current or historical).
            - Parses the JSON response from the API into usable Kotlin data objects.
        - Manages interactions with the Room database:
            - Retrieves weather data for a specific location and date from the database (if available).
            - Stores fetched weather data in the database for offline access.
        - Provides methods for:
            - Getting weather data from the database (`getWeatherFromDatabase`).
            - Fetching weather data from the API (`fetchWeatherData`).
            - Fetching and storing historical data in bulk (`fetchAndStoreHistoricalData`).

2. **Data Modeling:**
    - **`WeatherData.kt`:**  This class represents the structure of weather data received from the API. It includes properties like latitude, longitude, daily temperature highs and lows, etc.
    - **`WeatherEntry.kt`:**  This class represents a weather record stored in the Room database. It has properties like date, location (latitude/longitude), and maximum/minimum temperatures.

3. **API Interaction:**
    - **`Retrofit`** is used to interact with the Open-Meteo API.
    - **`WeatherService.kt` and `ForecastService.kt`:** These interfaces define the API endpoints for fetching current forecasts and historical data, respectively.

4. **Database Access:**
    - **Room** simplifies working with the local SQLite database.
    - **`WeatherDao.kt`:** This interface defines methods for database operations like inserting and retrieving weather data.
    - **`WeatherDatabase.kt`:**  This class defines the Room database schema.

**Key Functionalities Explained:**

* **`getWeatherFromDatabase`:** This method checks the database for weather data matching the user-provided location and date. If found, it returns the data; otherwise, it returns null.
* **`fetchWeatherData`:** This method validates user input (date, latitude, longitude). It then fetches weather data from the API based on the type of request (current or historical).
    - For current forecasts (dates within the next 10 days), it uses the `ForecastService` endpoint.
    - For historical data (beyond 10 days ago), it uses the `WeatherService` endpoint for archive data.
    - Upon successful retrieval, it parses the JSON response, extracts relevant data (max/min temperatures), and updates the ViewModel's weather data. It also stores the data in the database for future use.
* **`fetchAndStoreHistoricalData`:** This method fetches historical weather data for a location within the past 10 years. It performs the following steps:
    - Validates user input (latitude, longitude).
    - Constructs a request to the `WeatherService` endpoint, specifying the desired date range (up to 10 years back from today).
    - Parses the JSON response and extracts daily temperature data.
    - Loops through each day's data, creates a `WeatherEntry` object, and stores it in the database.
      **Usage**
- Install the application on an Android device or emulator.
- Enter the following details:
- Date: In the format MM-DD
- Year: Valid year (e.g., 2023)
- Latitude: A value between -90 and 90.
- Longitude: A value between -180 and 180.
- Click "Get Weather" to fetch the weather data for the given location and date.
- Click "Fetch and Store Historical Data" to download historical data for up to the last 10 years and store it for offline access.
-
**Error Handling:**

The code implements basic error handling mechanisms:

* It validates user input (date, latitude, longitude) to prevent invalid requests.
* It checks for network issues during API calls.
* It handles potential parsing errors from the API response.
* The ViewModel exposes an error state (`_error`) that the UI can observe and display appropriate error messages to the user.
