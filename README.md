# DripDoor 💎
> **Haute Jewellery, Curated for You.**

DripDoor is a premium, luxury e-commerce desktop application built with Java Swing and FlatLaf. Designed with a Dior-inspired minimalist aesthetic, it offers a highly personalized jewelry shopping experience complete with an AI Stylist, interactive try-on features, and real-time order tracking.

## ✨ Key Features
* **AI Stylist (Powered by Gemini AI):** Curates personalized jewelry bundles based on the user's outfit, occasion, and purchase history.
* **Interactive Outfit Builder:** A visual mannequin interface allowing users to "try on" and mix-and-match necklaces, earrings, rings, bracelets, and watches before purchasing.
* **Circular Drip Credit:** A unique buy-back program that rewards customers with 70% of their purchase value as redeemable credit for future orders.
* **Live Order Dispatch Simulation:** A real-time 10-minute dispatch progress tracker showing the curation and packaging journey.
* **Real-Time Sync:** Carts, wishlists, and orders are synced seamlessly across sessions using Firebase Realtime Database.
* **Secure Authentication:** User onboarding features Firebase Email/Password authentication with email verification routing.

## 🛠 Tech Stack
* **Language:** Java 17
* **UI Framework:** Swing with FlatLaf (custom themes, rounded corners, modern styling)
* **Backend & Database:** Firebase Admin SDK (Authentication, Realtime Database)
* **AI Integration:** Google Gemini (Gemini-2.5-flash-lite via Firebase REST APIs)
* **Asset Management:** Cloudinary (Asynchronous image loading and CDN delivery)
* **Build Tool:** Maven

## 🚀 Getting Started

### Prerequisites
Ensure you have **Java 17** and **Maven** installed on your system.

### Configuration
1. **Firebase Setup:** Download your Firebase `serviceAccountKey.json` from the Firebase Console (Project Settings > Service Accounts).
2. Place the file in the `src/main/resources/config/` directory.
3. **Environment Variables (Optional):** Ensure your Firebase Web API key is configured as an environment variable (`FIREBASE_WEB_API_KEY`) or update it directly in `src/main/java/com/dripdoor/config/AppConfig.java`.

### Build & Run
Since this is a Swing application managed via Maven, compile and run it directly using the `exec-maven-plugin`:

```bash
# Clean and compile the project
mvn clean package

# Run the application
mvn exec:java

📸 **Application Structure**
  com.dripdoor.view: Contains all FlatLaf-styled UI components (Login, Dashboard, Catalog, AI Stylist).
  
  com.dripdoor.controller: Handles business logic, cart management, and AI prompt orchestration.
  
  com.dripdoor.service: Manages external API integrations (Firebase Auth/DB, Cloudinary, Gemini).
  
  com.dripdoor.model: Core data structures (User, Product, Order, CartItem, SavedAddress).
