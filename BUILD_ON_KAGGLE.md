
# Hướng dẫn Build Android APK với Llama.cpp trên Kaggle

## Tại sao dùng Kaggle?
- **FREE GPU**: Kaggle cung cấp 30h/tuần GPU miễn phí
- **Môi trường Linux**: Tương thích hoàn hảo với Android NDK
- **RAM cao**: 16GB RAM giúp build nhanh hơn
- **Storage**: 20GB free storage

## Bước 1: Setup Kaggle Notebook

### 1.1 Tạo New Notebook
```
- Vào https://kaggle.com
- Tạo New Notebook 
- Chọn "GPU P100" (free)
- Language: Python
- Bật Internet: Settings > Internet > On
```

### 1.2 Clone Repository
```bash
# Cell 1: Clone repo
!git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
%cd YOUR_REPO
!ls -la
```

## Bước 2: Cài đặt Android SDK & NDK

### 2.1 Download Android SDK
```bash
# Cell 2: Download Android SDK
!wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
!unzip commandlinetools-linux-9477386_latest.zip
!mkdir -p /kaggle/working/android-sdk/cmdline-tools
!mv cmdline-tools /kaggle/working/android-sdk/cmdline-tools/latest
```

### 2.2 Install NDK & Build Tools
```bash
# Cell 3: Install required packages
!yes | /kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools"
!yes | /kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-34"
!yes | /kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
!yes | /kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager "ndk;25.2.9519653"
!yes | /kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager "cmake;3.22.1"

# Verify installation
!/kaggle/working/android-sdk/ndk/25.2.9519653/ndk-build --version
```

## Bước 3: Setup Environment Variables (ĐÃ CHỈNH SỬA)

```python
# Cell 4: Setup environment variables
import os

# Set Android environment
os.environ['ANDROID_HOME'] = '/kaggle/working/android-sdk'
os.environ['ANDROID_SDK_ROOT'] = '/kaggle/working/android-sdk'
os.environ['ANDROID_NDK_HOME'] = '/kaggle/working/android-sdk/ndk/25.2.9519653'
os.environ['ANDROID_NDK_ROOT'] = '/kaggle/working/android-sdk/ndk/25.2.9519653'

# Add to PATH
current_path = os.environ.get('PATH', '')
new_paths = [
    '/kaggle/working/android-sdk/platform-tools',
    '/kaggle/working/android-sdk/build-tools/34.0.0',
    '/kaggle/working/android-sdk/cmdline-tools/latest/bin',
    '/kaggle/working/android-sdk/ndk/25.2.9519653'
]

for path in new_paths:
    if path not in current_path:
        os.environ['PATH'] = f"{path}:{current_path}"
        current_path = os.environ['PATH']

# Java environment (Kaggle có sẵn Java 17)
os.environ['JAVA_HOME'] = '/usr/lib/jvm/java-17-openjdk-amd64'

# Gradle settings
os.environ['GRADLE_OPTS'] = '-Xmx4g -XX:MaxPermSize=512m'
os.environ['GRADLE_USER_HOME'] = '/kaggle/working/.gradle'

print("✅ Environment setup complete!")
print(f"ANDROID_HOME: {os.environ.get('ANDROID_HOME')}")
print(f"ANDROID_NDK_HOME: {os.environ.get('ANDROID_NDK_HOME')}")
print(f"JAVA_HOME: {os.environ.get('JAVA_HOME')}")

# Test commands
print("\n🔍 Testing installations:")
!which java && java -version
!ls -la /usr/lib/jvm/ | grep java
!which adb && adb version
!/kaggle/working/android-sdk/ndk/25.2.9519653/ndk-build --version
```

## Bước 4: Prepare Project

### 4.1 Navigate và setup permissions
```bash
# Cell 5: Setup project
%cd /kaggle/working/YOUR_REPO/Android/src

# Make gradlew executable
!chmod +x gradlew

# Create gradle directory if not exists
!mkdir -p /kaggle/working/.gradle

# Check project structure
!ls -la
!cat gradle/wrapper/gradle-wrapper.properties
```

### 4.2 Fix common issues
```bash
# Cell 6: Fix potential issues
# Create local.properties file
!echo "sdk.dir=/kaggle/working/android-sdk" > local.properties
!echo "ndk.dir=/kaggle/working/android-sdk/ndk/25.2.9519653" >> local.properties

# Check Gradle version
!./gradlew --version

# Download dependencies first
!./gradlew dependencies --configuration implementation
```

## Bước 5: Build APK

### 5.1 Clean Build
```bash
# Cell 7: Clean build
!./gradlew clean

# Check if clean successful
!ls -la app/build/ || echo "Clean completed - build folder removed"
```

### 5.2 Build Debug APK
```bash
# Cell 8: Build APK with verbose output
!./gradlew assembleDebug --info --stacktrace

# Alternative với less memory usage
# !./gradlew assembleDebug --max-workers=2 --parallel=false
```

### 5.3 Check Build Result
```bash
# Cell 9: Check build output
!ls -la app/build/outputs/apk/debug/

# Get APK info
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ APK built successfully!"
    echo "📦 APK size: $(du -h $APK_PATH | cut -f1)"
    echo "📍 APK location: $APK_PATH"
else
    echo "❌ APK build failed!"
    echo "📋 Check logs above for errors"
fi
```

## Bước 6: Download APK

### 6.1 Copy APK to workspace
```python
# Cell 10: Prepare APK for download
import shutil
import os
from datetime import datetime

# APK paths
apk_source = "/kaggle/working/YOUR_REPO/Android/src/app/build/outputs/apk/debug/app-debug.apk"
timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
apk_dest = f"/kaggle/working/llama-chat-android_{timestamp}.apk"

if os.path.exists(apk_source):
    shutil.copy2(apk_source, apk_dest)
    
    # Get file info
    size_mb = os.path.getsize(apk_dest) / (1024*1024)
    
    print(f"✅ APK copied successfully!")
    print(f"📦 Size: {size_mb:.1f} MB")
    print(f"📍 Location: {apk_dest}")
    print(f"⬇️ Download using Files panel on left sidebar")
    
    # Create download link (in some Kaggle environments)
    from IPython.display import FileLink, display
    display(FileLink(apk_dest))
    
else:
    print("❌ APK not found! Check build errors above.")
    print("🔍 Checking if build folder exists:")
    !ls -la /kaggle/working/YOUR_REPO/Android/src/app/build/outputs/apk/debug/ || echo "No APK output folder"
```

### 6.2 Upload to Google Drive (Optional)
```python
# Cell 11: Upload to Google Drive (optional)
# Uncomment nếu muốn upload lên Drive
"""
from google.colab import drive
import shutil

# Mount Google Drive
drive.mount('/content/drive')

# Copy to Drive
drive_path = f"/content/drive/MyDrive/llama-chat-android_{timestamp}.apk"
if os.path.exists(apk_dest):
    shutil.copy2(apk_dest, drive_path)
    print(f"✅ APK uploaded to Google Drive: {drive_path}")
else:
    print("❌ No APK to upload")
"""
print("📝 Uncomment above code if you want to upload to Google Drive")
```

## Bước 7: Troubleshooting

### 7.1 Common Build Errors
```bash
# Cell 12: Debug commands
echo "🔍 Debugging information:"
echo "========================"

echo "📁 Project structure:"
!ls -la /kaggle/working/YOUR_REPO/Android/src/

echo -e "\n📋 Environment variables:"
!printenv | grep -E "(ANDROID|JAVA|GRADLE)" | sort

echo -e "\n💾 Disk space:"
!df -h /kaggle/working

echo -e "\n🧠 Memory usage:"
!free -h

echo -e "\n📱 Android SDK packages:"
!/kaggle/working/android-sdk/cmdline-tools/latest/bin/sdkmanager --list_installed
```

### 7.2 Fix specific issues
```bash
# Cell 13: Common fixes

# If "ANDROID_HOME not set" error:
!echo "export ANDROID_HOME=/kaggle/working/android-sdk" >> ~/.bashrc
!echo "export ANDROID_NDK_HOME=/kaggle/working/android-sdk/ndk/25.2.9519653" >> ~/.bashrc

# If Gradle daemon issues:
!./gradlew --stop
!rm -rf /kaggle/working/.gradle/daemon/

# If out of memory:
!echo "org.gradle.jvmargs=-Xmx2g -XX:MaxPermSize=512m" >> gradle.properties
!echo "org.gradle.parallel=false" >> gradle.properties

# If NDK not found:
!ls -la /kaggle/working/android-sdk/ndk/
```

### 7.3 Alternative build methods
```bash
# Cell 14: Alternative builds

# Method 1: Build without llama.cpp (faster)
!./gradlew assembleDebug -x :app:externalNativeBuild

# Method 2: Build with different ABI
!./gradlew assembleDebug -PANDROID_ABI=arm64-v8a

# Method 3: Release build (smaller size)
!./gradlew assembleRelease --info
```

## Bước 8: Build với Llama.cpp (Advanced)

### 8.1 Setup CMake cho llama.cpp
```bash
# Cell 15: Setup for llama.cpp build
echo "🔧 Setting up CMake for llama.cpp..."

# Check CMake version
!/kaggle/working/android-sdk/cmake/3.22.1/bin/cmake --version

# Set CMake path
export CMAKE_PATH="/kaggle/working/android-sdk/cmake/3.22.1/bin"
export PATH="$CMAKE_PATH:$PATH"

echo "✅ CMake setup complete"
```

### 8.2 Build với native libraries
```bash
# Cell 16: Build with native code
echo "🚀 Building with llama.cpp native libraries..."

# This will take longer (10-20 minutes)
!./gradlew assembleDebug \
  -Pandroid.useAndroidX=true \
  -Pandroid.enableJetifier=true \
  -DCMAKE_TOOLCHAIN_FILE=/kaggle/working/android-sdk/ndk/25.2.9519653/build/cmake/android.toolchain.cmake \
  --info

echo "🎉 Native build completed!"
```

## Expected Results:
- **Build time**: 5-20 phút (tùy có llama.cpp không)
- **APK size**: 
  - Không llama.cpp: 5-15 MB
  - Có llama.cpp: 50-150 MB
- **Success rate**: ~80% nếu làm đúng steps

## Tips để Build thành công:

### 1. Quản lý Memory
```bash
# Giảm memory usage
export GRADLE_OPTS="-Xmx2g -XX:MaxPermSize=512m"
!./gradlew assembleDebug --max-workers=1 --parallel=false
```

### 2. Network Issues
```bash
# Nếu download chậm
!./gradlew assembleDebug --refresh-dependencies --rerun-tasks
```

### 3. Clean everything nếu lỗi
```bash
!./gradlew clean
!rm -rf .gradle/
!rm -rf app/build/
!./gradlew assembleDebug
```

## Lưu ý quan trọng:
- ✅ Kaggle có giới hạn 30h GPU/tuần
- ✅ Session timeout sau 12h inactive  
- ✅ Nên backup code trên GitHub
- ✅ APK sẽ mất khi session end
- ✅ Download APK ngay sau khi build xong
- ✅ Có thể build nhiều lần trong 1 session

## Các lệnh hữu ích:
```bash
# Xem log chi tiết
!./gradlew assembleDebug --info --stacktrace

# Build chỉ một architecture
!./gradlew assembleDebug -PANDROID_ABI=arm64-v8a

# Skip tests
!./gradlew assembleDebug -x test

# Build release
!./gradlew assembleRelease
```
