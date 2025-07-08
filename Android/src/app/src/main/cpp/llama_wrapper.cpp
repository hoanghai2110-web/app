#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "LlamaWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_sosgm_chat_LlamaWrapper_getVersion(JNIEnv *env, jobject /* this */) {
    std::string version = "Lite Version 1.0";
    LOGD("Returning version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_sosgm_chat_LlamaWrapper_processText(JNIEnv *env, jobject /* this */, jstring input) {
    const char *inputStr = env->GetStringUTFChars(input, 0);

    // Simple echo for testing
    std::string result = "Echo: ";
    result += inputStr;

    env->ReleaseStringUTFChars(input, inputStr);
    LOGD("Processed text successfully");

    return env->NewStringUTF(result.c_str());
}