@echo off
<<<<<<< HEAD

REM Script untuk mendapatkan SHA-1 fingerprint - Windows

 

echo ========================================

echo  Getting SHA-1 Fingerprint for Debug

echo ========================================

echo.

 

REM Check if keytool is available

where keytool >nul 2>nul

if %errorlevel% neq 0 (

    echo ERROR: keytool not found!

    echo.

    echo Please install Java JDK 11+ and add to PATH:

    echo 1. Download from: https://adoptium.net/

    echo 2. Install Java

    echo 3. Add to PATH: C:\Program Files\Java\jdk-17\bin

    echo 4. Restart terminal and run this script again

    echo.

    pause

    exit /b 1

)

 

echo Java version:

java -version

echo.

 

echo Getting SHA-1 from Debug Keystore...

echo.

 

keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1"

 

echo.

echo ========================================

echo  INSTRUCTIONS:

echo ========================================

echo 1. Copy the SHA1 value above

echo 2. Go to Firebase Console: https://console.firebase.google.com/

echo 3. Select your project

echo 4. Go to Project Settings

echo 5. Select Android app

echo 6. Click "Add fingerprint"

echo 7. Paste SHA1 and Save

echo 8. Download new google-services.json

echo 9. Replace app/google-services.json

echo.

pause
=======
REM Script untuk mendapatkan SHA-1 fingerprint - Windows

echo ========================================
echo  Getting SHA-1 Fingerprint for Debug
echo ========================================
echo.

REM Check if keytool is available
where keytool >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: keytool not found!
    echo.
    echo Please install Java JDK 11+ and add to PATH:
    echo 1. Download from: https://adoptium.net/
    echo 2. Install Java
    echo 3. Add to PATH: C:\Program Files\Java\jdk-17\bin
    echo 4. Restart terminal and run this script again
    echo.
    pause
    exit /b 1
)

echo Java version:
java -version
echo.

echo Getting SHA-1 from Debug Keystore...
echo.

keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1"

echo.
echo ========================================
echo  INSTRUCTIONS:
echo ========================================
echo 1. Copy the SHA1 value above
echo 2. Go to Firebase Console: https://console.firebase.google.com/
echo 3. Select your project
echo 4. Go to Project Settings
echo 5. Select Android app
echo 6. Click "Add fingerprint"
echo 7. Paste SHA1 and Save
echo 8. Download new google-services.json
echo 9. Replace app/google-services.json
echo.
pause
>>>>>>> a41839f9e5927726999be00441dd2bfe4c5552c0
