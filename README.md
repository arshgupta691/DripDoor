# DripDoor — Resources Directory

Place the following files here before building:

## Firebase
- config/serviceAccountKey.json  (Download from Firebase Console → Project Settings → Service Accounts)

## Cloudinary
- No files needed. Image URLs are generated at runtime via CloudinaryService.

## Fonts (optional override)
- fonts/Georgia.ttf
- fonts/Arial.ttf

## Build & Run
```
mvn clean package -q
java -jar target/dripdoor-app-1.0.0.jar
```
