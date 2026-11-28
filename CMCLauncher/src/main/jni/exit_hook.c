/**
 * @file exit_hook.c
 * @brief 游戏进程退出钩子
 *
 * 通过 bytehook 拦截 exit() 调用，将游戏 JVM 的退出转换为受控的 nominal_exit，
 * 避免 Dalvik/ART 在进程退出时产生 SIGSEGV 或 SIGABRT。
 * 同时在此安装 SDL 相关钩子（SDL 初始化拦截、dlopen 拦截）。
 */

#include <jni.h>
#include <unistd.h>
#include <stdbool.h>
#include <bytehook.h>
#include <dlfcn.h>
#include <android/log.h>
#include <stdlib.h>
#include "stdio_is.h"

// 原子标志：标记是否已经触发过退出（防止递归退出）
static _Atomic bool exit_tripped = false;

// SDL3 launcher 集成（native_hooks/sdl_hook.c）
// bytehook_hook_all 函数指针类型：用于钩住指定符号
typedef bytehook_stub_t (*bytehook_hook_all_fn)(const char *callee_path_name, const char *sym_name, void *new_func,
                                                bytehook_hooked_t hooked, void *hooked_arg);
void create_sdl_hooks(bytehook_hook_all_fn bytehook_hook_all_p);
void create_sdl_dlopen_hooks(bytehook_hook_all_fn bytehook_hook_all_p);

// exit 函数指针类型
typedef void (*exit_func)(int);

// 自定义 exit 处理：拦截 exit() 调用，转换为 nominal_exit
static void custom_exit(int code) {
    // 如果退出已经触发（递归调用或来自其他线程），则透传原始 exit 调用
    if(exit_tripped) {
        BYTEHOOK_CALL_PREV(custom_exit, exit_func, code);
        BYTEHOOK_POP_STACK();
        return;
    }
    exit_tripped = true;
    // 执行受控退出
    nominal_exit(code, false);
    BYTEHOOK_POP_STACK();
}

// 自定义 atexit 处理：与 custom_exit 类似，但不带退出码，也不透传
static void custom_atexit() {
    if(exit_tripped) {
        return;
    }
    exit_tripped = true;
    nominal_exit(0, false);
}

// 初始化 exit 钩子：加载 libbytehook.so，钩住 exit 函数，并安装 SDL 钩子
static bool init_exit_hook() {
    void* bytehook_handle = dlopen("libbytehook.so", RTLD_NOW);
    if(bytehook_handle == NULL) {
        goto dlerror;
    }

    bytehook_stub_t (*bytehook_hook_all_p)(const char *callee_path_name, const char *sym_name, void *new_func,
                                           bytehook_hooked_t hooked, void *hooked_arg);
    int (*bytehook_init_p)(int mode, bool debug);

    bytehook_hook_all_p = dlsym(bytehook_handle, "bytehook_hook_all");
    bytehook_init_p = dlsym(bytehook_handle, "bytehook_init");

    if(bytehook_hook_all_p == NULL || bytehook_init_p == NULL) {
        goto dlerror;
    }
    int bhook_status = bytehook_init_p(BYTEHOOK_MODE_AUTOMATIC, false);
    if(bhook_status == BYTEHOOK_STATUS_CODE_OK) {
        bytehook_stub_t stub = bytehook_hook_all_p(NULL, "exit", &custom_exit, NULL, NULL);
        __android_log_print(ANDROID_LOG_INFO, "exit_hook", "Successfully initialized exit hook, stub=%p", stub);
        // SDL hook 与 exit hook 在同一进程内安装；游戏 JVM 内嵌于本进程，
        // 因此游戏调用 SDL_InitSubSystem 时同样会被拦截
        create_sdl_hooks(bytehook_hook_all_p);
        create_sdl_dlopen_hooks(bytehook_hook_all_p);
        return true;
    } else {
        __android_log_print(ANDROID_LOG_INFO, "exit_hook", "bytehook_init failed (%i)", bhook_status);
        dlclose(bytehook_handle);
        return false;
    }

    dlerror:
    if(bytehook_handle != NULL) dlclose(bytehook_handle);
    __android_log_print(ANDROID_LOG_ERROR, "exit_hook", "Failed to load hook library: %s", dlerror());
    return false;
}

// JNI 入口：Java 层调用此方法初始化游戏退出钩子
// 如果 bytehook 钩子安装失败，则退而使用 atexit() 注册，
// 虽然无法报告正确的退出码，但可以防止 Dalvik 在 exit() 时产生 SIGSEGV/SIGABRT
JNIEXPORT void JNICALL
Java_com_cmc_launcher_bridge_ZLBridge_initializeGameExitHook(JNIEnv *env, jclass clazz) {
    bool hookReady = init_exit_hook();
    if(!hookReady){
        atexit(custom_atexit);
    }
}
