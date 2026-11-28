/**
 * @file sdl_dlopen_hook.c
 * @brief SDL dlopen/dlsym 钩子：隔离 SDL 的 JNI_OnLoad 并注入 dlsym 层代理
 *
 * 主要功能：
 * 1. SDL JNI_OnLoad 隔离：
 *    游戏 JVM 内嵌于 Dalvik/ART 进程中，SDL 库的 JNI_OnLoad 会被游戏 JVM 调用。
 *    但 SDL 的 JNI_OnLoad 内部会尝试获取 Dalvik 的 JNIEnv 并注册 native 方法，
 *    这在游戏 JVM 上下文中会失败或产生冲突。
 *    此钩子拦截 dlsym 对 libSDL3.so 的 JNI_OnLoad 解析，替换为隔离版本：
 *    - 仅当调用方是 Dalvik JVM 时才执行原始 JNI_OnLoad
 *    - 游戏 JVM 调用时直接返回 JNI_VERSION_1_4（跳过 SDL 的 JNI 初始化）
 *
 * 2. dlsym 层代理注入：
 *    LWJGL 的 org.lwjgl.sdl 绑定等消费方通过 dlopen+dlsym 解析 SDL 函数指针后
 *    直接调用，bytehook 的 GOT 导入补丁（hook_all）无法拦截这类调用。
 *    此钩子在 dlsym 出口统一将 SDL 符号的解析结果替换为 sdl_hook.c 中的
 *    dlsym 层代理函数，确保主窗口复用、EGL 兼容、Vulkan 加载器一致性等功能
 *    对所有调用方生效。
 *
 * Reference: https://github.com/AngelAuraMC/Amethyst-Android/commit/e4c79084d
 */

#include <dlfcn.h>
#include <jni.h>
#include <bytehook.h>
#include <stdlib.h>
#include <string.h>

#include "environ/environ.h"
#include "logger/logger.h"
#include "sdl_hook.h"
#include "utils.h"

// dlsym 函数指针类型
typedef void *(*dlsym_func_t)(void *handle, const char *symbol);
// JNI_OnLoad 函数指针类型
typedef jint (*jni_on_load_func_t)(JavaVM *vm, void *reserved);

// bytehook_hook_all 函数指针类型
typedef bytehook_stub_t (*bytehook_hook_all_fn)(const char *callee_path_name, const char *symbol,
                                                  void *new_func, bytehook_hooked_t hooked, void *hooked_arg);

// libSDL3.so 的 dlopen 句柄（延迟初始化）
static void *sdlHandle;
// 原始 SDL JNI_OnLoad 函数指针
static jni_on_load_func_t originalSdlJniOnLoad;

// 隔离版 SDL JNI_OnLoad：
// 仅当调用方是 Dalvik JVM 时才执行原始 JNI_OnLoad，
// 游戏 JVM 调用时直接返回 JNI_VERSION_1_4（跳过 SDL 的 JNI 初始化）
static jint isolatedSdlJniOnLoad(JavaVM *vm, void *reserved) {
    if (originalSdlJniOnLoad != NULL && pojav_environ->dalvikJavaVMPtr == vm) {
        return originalSdlJniOnLoad(vm, reserved);
    }
    return JNI_VERSION_1_4;
}

// 自定义 dlsym：拦截 dlsym 调用，实现 JNI_OnLoad 隔离和 dlsym 层代理注入
static void *customDlsym(void *handle, const char *symbol) {
    void *result = BYTEHOOK_CALL_PREV(customDlsym, dlsym_func_t, handle, symbol);
    BYTEHOOK_POP_STACK();
    // 延迟初始化 libSDL3.so 句柄
    if (sdlHandle == NULL) {
        sdlHandle = dlopen("libSDL3.so", RTLD_LOCAL | RTLD_NOW);
    }
    // 拦截 libSDL3.so 的 JNI_OnLoad 解析，替换为隔离版本
    if (sdlHandle != NULL && handle == sdlHandle && symbol != NULL && strcmp(symbol, "JNI_OnLoad") == 0) {
        originalSdlJniOnLoad = (jni_on_load_func_t) result;
        result = (void *) isolatedSdlJniOnLoad;
    }
    // LWJGL 的 org.lwjgl.sdl 绑定等消费方经 dlsym 解析 SDL 函数指针后直接调用，
    // hook_all 的 GOT 导入补丁拦截不到；在解析出口统一换成 sdl_hook 的 dlsym 层代理
    if (result != NULL && symbol != NULL) {
        void *proxy = sdlDlsymProxy(symbol, result);
        if (proxy != NULL) {
            LOG_TO_I("SDL_Hook: dlsym proxy for %s", symbol);
            result = proxy;
        }
    }
    return result;
}

// 安装 SDL dlopen 钩子：钩住 dlsym 函数
void create_sdl_dlopen_hooks(bytehook_hook_all_fn hookAll) {
    if (hookAll == NULL) return;
    hookAll(NULL, "dlsym", (void *) customDlsym, NULL, NULL);
}
