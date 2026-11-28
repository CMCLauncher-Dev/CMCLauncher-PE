/**
 * @file utils.h
 * @brief 通用工具函数与宏定义
 *
 * 提供 JNI 层常用的工具函数，包括：
 * - Java 字符串数组与 C 字符串数组的相互转换
 * - 跨 JVM 环境（Dalvik/ART 与 OpenJDK）的对象转换
 * - dlsym 动态符号查找宏
 * - 剪贴板操作
 * - LWJGL dlopen 钩子安装
 */
#pragma once

#include <stdbool.h>
#include "environ/environ.h"
#include "logger/logger.h"

// 剪贴板操作类型常量（与 Java 层 CallbackBridge 对应）
#define CLIPBOARD_COPY 2000
#define CLIPBOARD_PASTE 2001
#define CLIPBOARD_OPEN 2002

// SDL launcher 集成相关常量
#define NOTIF_TYPE_SDL 0
#define ACTION_INIT_LAUNCHER_INTEGRATION 0
#define ACTION_SEND_TEXTBOX_RECT 1

// 声明与函数 fn 同类型的函数指针类型 fn_t
#define DECL_DLSYM(fn) typedef typeof(&fn) fn##_t;

// 通过 dlsym 查找符号并赋值给函数指针，失败时记录错误日志
#define SET_DLSYM_PTR(handle, fn)                     \
    fn##_t fn##_p;                                   \
    do {                                             \
        dlerror();                                   \
        void *_p = dlsym((handle), #fn);             \
        const char *_e = dlerror();                  \
        if (_e || !_p) {                             \
            LOG_TO_E("<%s> %s", "Native", "dlsym(" #fn ") failed: %s", _e ? _e : "unknown error"); \
        }                                            \
        fn##_p = (fn##_t)_p;                         \
    } while (0)

// 获取当前线程附加的 JNIEnv，失败时执行 then 语句
#define TRY_ATTACH_ENV(env_name, vm, error_message, then) JNIEnv* env_name;\
do {                                                                       \
    env_name = get_attached_env(vm);                                       \
    if(env_name == NULL) {                                                 \
        printf(error_message);                                             \
        then                                                               \
    }                                                                      \
} while(0)

// 将 Java String 数组转换为 C 字符串数组（调用方负责释放）
char** convert_to_char_array(JNIEnv *env, jobjectArray jstringArray);
// 将 C 字符串数组转换为 Java String 数组
jobjectArray convert_from_char_array(JNIEnv *env, char **charArray, int num_rows);
// 释放由 convert_to_char_array 创建的 C 字符串数组
void free_char_array(JNIEnv *env, jobjectArray jstringArray, const char **charArray);
// 跨 JVM 环境转换 jstring（Dalvik/ART <-> OpenJDK）
jstring convertStringJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jstring srcStr);
// 跨 JVM 环境转换 jintArray（Dalvik/ART <-> OpenJDK）
jintArray convertIntArrayJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jintArray srcIntArray);

// 获取当前线程附加到指定 JVM 的 JNIEnv（若未附加则自动附加）
JNIEnv* get_attached_env(JavaVM* jvm);

/** 通过 launcher 侧 CallbackBridge.notifyLauncher 通知 Java（如 SDL 集成初始化） */
bool notifyLauncher(JNIEnv *dvm_env, int type, int actions[], int len);

// 钩住 exec 函数，拦截进程执行
void hookExec();
// 安装 LWJGL dlopen 钩子，拦截 LWJGL 的动态库加载
void installLwjglDlopenHook();
// 安装 EMUI 迭代器缓解措施（修复华为 EMUI 上的兼容性问题）
void installEMUIIteratorMititgation();
// JNI 入口：原生剪贴板操作（复制/粘贴/打开）
JNIEXPORT jstring JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass clazz, jint action, jbyteArray copySrc);
