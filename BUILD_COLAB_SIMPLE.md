
# Build Android APK đơn giản trên Google Colab

## Bước 1: Setup môi trường
```python
# Cell 1: Install Java và Android SDK
!apt update
!apt install -y openjdk-17-jdk wget unzip

# Set JAVA_HOME
import os
os.environ['JAVA_HOME'] = '/usr/lib/jvm/java-17-openjdk-amd64'
os.environ['PATH'] = f"/usr/lib/jvm/java-17-openjdk-amd64/bin:{os.environ['PATH']}"

print("Java version:")
!java -version
```

## Bước 2: Download Android SDK (tối giản)
```bash
# Cell 2: Download minimal Android SDK
!wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
!unzip -q commandlinetools-linux-9477386_latest.zip
!mkdir -p /content/android-sdk/cmdline-tools
!mv cmdline-tools /content/android-sdk/cmdline-tools/latest

# Set environment
import os
os.environ['ANDROID_HOME'] = '/content/android-sdk'
os.environ['ANDROID_SDK_ROOT'] = '/content/android-sdk'
os.environ['PATH'] = f"/content/android-sdk/cmdline-tools/latest/bin:{os.environ['PATH']}"
```

## Bước 3: Install minimal packages
```bash
# Cell 3: Install only essential packages
!yes | /content/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools"
!yes | /content/android-sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-34"  
!yes | /content/android-sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"

# Skip NDK for now
echo "SDK setup complete - lightweight version"
```

## Bước 4: Clone và setup project
```bash
# Cell 4: Clone project
!git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
%cd YOUR_REPO/Android/src

# Make gradlew executable
!chmod +x gradlew

# Create local.properties
!echo "sdk.dir=/content/android-sdk" > local.properties

# Check structure
!ls -la
```

## Bước 5: Build APK (lightweight)
```bash
# Cell 5: Build without native code
!./gradlew assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx1g"

# Check result
!ls -la app/build/outputs/apk/debug/
```

## Bước 6: Download APK
```python
# Cell 6: Download result
import shutil
from datetime import datetime

apk_path = "/content/YOUR_REPO/Android/src/app/build/outputs/apk/debug/app-debug.apk"
timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
dest_path = f"/content/chat-app-lite_{timestamp}.apk"

if os.path.exists(apk_path):
    shutil.copy2(apk_path, dest_path)
    size_mb = os.path.getsize(dest_path) / (1024*1024)
    print(f"✅ APK created: {dest_path}")
    print(f"📦 Size: {size_mb:.1f} MB")
    
    # Download link
    from google.colab import files
    files.download(dest_path)
else:
    print("❌ APK not found")
```

## Lỗi thường gặp và cách fix:

### 1. Out of Memory
```bash
# Add to gradle.properties
echo "org.gradle.jvmargs=-Xmx1g -XX:MaxPermSize=256m" >> gradle.properties
echo "org.gradle.parallel=false" >> gradle.properties
echo "org.gradle.daemon=false" >> gradle.properties
```

### 2. Build timeout
```bash
# Build với timeout longer
timeout 1800 ./gradlew assembleDebug --no-daemon
```

### 3. Dependencies conflict
```bash
# Clean và rebuild
./gradlew clean
./gradlew assembleDebug --refresh-dependencies
```

## Kích thước APK:
- Không có llama.cpp: ~5-15 MB
- Có thể add llama.cpp sau khi test thành công
