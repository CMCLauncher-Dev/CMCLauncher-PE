/**
 * @file lwjgl_dlopen_hook.c
 * @brief LWJGL dlopen 钩子：拦截 LWJGL 的动态库加载
 *
 * 通过 RegisterNatives 替换 LWJGL 的 org.lwjgl.system.linux.DynamicLinkLoader.ndlopen
 * native 方法，实现以下功能：
 *
 * 1. Vulkan 加载重定向：
 *    拦截 libvulkan.so 的加载请求，改用启动器自定义的 Vulkan 驱动加载逻辑
 *    （maybe_load_vulkan），将 Vulkan 句柄重定向到私有命名空间中的加载器副本。
 *
 * 2. Android 10 及更早版本的 linker 命名空间 bug 缓解：
 *    Android 10 及更早版本的 linker 不支持命名空间嵌套。
 *    大多数库位于 launcher 路径下，加载没有问题。但当 VulkanMod 等模组
 *    尝试通过此方法加载默认 jni libs 目录之外的库（如 shaderc）时，
 *    由于路径不在匿名命名空间的允许路径中，加载会失败。
 *    此钩子位于 libpojavexec 中，属于 classloader 命名空间，
 *    因此可以成功加载这些库。
 *
 * Reference:
 * - ndlopen 原始实现: https://github.com/PojavLauncherTeam/lwjgl3/blob/3.3.1/modules/lwjgl/core/src/generated/c/linux/org_lwjgl_system_linux_DynamicLinkLoader.c#L11
 */

#include <android/api-level.h>
#include <android/log.h>
#include <jni.h>

#include <environ/environ.h>

#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>

// 自定义 Vulkan 加载函数（定义于 vulkan_checker.c）
extern void* maybe_load_vulkan();

/**
 * ndlopen 的替代实现：
 * - 拦截 libvulkan.so 的加载，改用自定义 Vulkan 驱动
 * - 通过 libpojavexec 的 classloader 命名空间加载库，缓解 linker 命名空间 bug
 */
static jlong ndlopen_bugfix(__attribute__((unused)) JNIEnv *env,
                            __attribute__((unused)) jclass class,
                            jlong filename_ptr,
                            jint jmode) {
    const char* filename = (const char*) filename_ptr;

    // 拦截 libvulkan.so 的加载，改用自定义 Vulkan 驱动
    if(strstr(filename, "libvulkan.so") == filename) {
        printf("LWJGL linkerhook: replacing load for libvulkan.so with custom driver\n");
        return (jlong) maybe_load_vulkan();
    }

    // 通过 libpojavexec 的 classloader 命名空间加载库，
    // 缓解 Android 10 及更早版本 linker 不支持命名空间嵌套的 bug
    int mode = (int)jmode;
    return (jlong) dlopen(filename, mode);
}

/**
 * 安装 LWJGL dlopen 钩子：
 * 通过 RegisterNatives 替换 LWJGL 的 ndlopen native 方法，
 * 实现 linker bug 缓解和自定义库加载重定向。
 */
void installLwjglDlopenHook() {
    __android_log_print(ANDROID_LOG_INFO, "LwjglLinkerHook", "Installing LWJGL dlopen() hook");
    JNIEnv* env = pojav_environ->runtimeJNIEnvPtr_JRE;
    jclass dynamicLinkLoader = (*env)->FindClass(env, "org/lwjgl/system/linux/DynamicLinkLoader");
    if(dynamicLinkLoader == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "LwjglLinkerHook", "Failed to find the target class");
        (*env)->ExceptionClear(env);
        return;
    }
    JNINativeMethod ndlopenMethod[] = {
            {"ndlopen", "(JI)J", &ndlopen_bugfix}
    };
    if((*env)->RegisterNatives(env, dynamicLinkLoader, ndlopenMethod, 1) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "LwjglLinkerHook", "Failed to register the hooked method");
        (*env)->ExceptionClear(env);
    }
}
