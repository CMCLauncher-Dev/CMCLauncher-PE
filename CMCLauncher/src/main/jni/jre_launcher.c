/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
// Boardwalk: missing include
#include <string.h>

#include "logger/logger.h"
#include "utils.h"
#include "environ/environ.h"

// 取消注释可将信号处理重定向到 JVM
// #define TRY_SIG2JVM

// PojavLauncher: fixme: 这些版本号是否正确？
#define FULL_VERSION "1.8.0-internal"
#define DOT_VERSION "1.8"

// 传递给 JLI_Launch 的固定参数（程序名、启动器名等）
static const char* const_progname = "java";
static const char* const_launcher = "openjdk";
static const char** const_jargs = NULL;
static const char** const_appclasspath = NULL;
static const jboolean const_javaw = JNI_FALSE;
static const jboolean const_cpwildcard = JNI_TRUE;
static const jint const_ergo_class = 0; // DEFAULT_POLICY

// JLI_Launch 函数指针类型定义（JDK 启动器接口）
typedef jint JLI_Launch_func(int argc, char ** argv, /* main argc, argc */
        int jargc, const char** jargv,          /* java args */
        int appclassc, const char** appclassv,  /* app classpath */
        const char* fullversion,                /* full version defined */
        const char* dotversion,                 /* dot version defined */
        const char* pname,                      /* program name */
        const char* lname,                      /* launcher name */
        jboolean javaargs,                      /* JAVA_ARGS */
        jboolean cpwildcard,                    /* classpath wildcard*/
        jboolean javaw,                         /* windows-only javaw */
        jint ergo                               /* ergonomics class policy */
);

// SIGABRT 信号等待器数据：用于在 JVM 崩溃时捕获 abort 信号
struct {
    sigset_t tracked_sigset;  // 被跟踪的信号集
    int pipe[2];              // 信号处理线程与等待线程之间的管道
} abort_waiter_data;

_Noreturn extern void nominal_exit(int code, bool is_signal);

// 信号等待线程：阻塞等待管道中的信号，收到后调用 nominal_exit 退出
_Noreturn static void* abort_waiter_thread(void* extraArg) {
    pthread_sigmask(SIG_BLOCK, &abort_waiter_data.tracked_sigset, NULL);
    int signal;
    read(abort_waiter_data.pipe[0], &signal, sizeof(int));
    nominal_exit(signal, true);
}

// 信号处理函数：将信号写入管道，然后死循环等待等待线程处理
_Noreturn static void abort_waiter_handler(int signal) {
    write(abort_waiter_data.pipe[1], &signal, sizeof(int));
    while(1) {}
}

// 设置 SIGABRT 信号捕获：创建管道和等待线程，注册信号处理函数
static void abort_waiter_setup() {
    const static int tracked_signals[] = {SIGABRT};
    const static int ntracked = (sizeof(tracked_signals) / sizeof(tracked_signals[0]));
    struct sigaction sigactions[ntracked];
    sigemptyset(&abort_waiter_data.tracked_sigset);

    for (size_t i = 0; i < ntracked; i++)
    {
        sigaddset(&abort_waiter_data.tracked_sigset, tracked_signals[i]);
        sigactions[i].sa_handler = abort_waiter_handler;
    }

    if (pipe(abort_waiter_data.pipe) != 0)
    {
        printf("Failed to set up aborter pipe: %s\n", strerror(errno));
        return;
    }

    pthread_t waiter_thread; int result;

    if ((result = pthread_create(&waiter_thread, NULL, abort_waiter_thread, NULL)) != 0)
    {
        printf("Failed to start up waiter thread: %s", strerror(result));
        for(int i = 0; i < 2; i++) close(abort_waiter_data.pipe[i]);
        return;
    }

    for (size_t i = 0; i < ntracked; i++)
    {
        if (sigaction(tracked_signals[i], &sigactions[i], NULL) != 0)
        {
            printf("Failed to set signal hander for signal %i: %s", i, strerror(errno));
        }
    }
}

// 启动 JVM：加载 libjli.so，重置信号处理，调用 JLI_Launch
static jint launchJVM(int margc, char** margv) {
   void* libjli = dlopen("libjli.so", RTLD_LAZY | RTLD_GLOBAL);
   struct sigaction clean_sa;
   memset(&clean_sa, 0, sizeof (struct sigaction));

   // 重置所有信号处理为默认值（SIGSEGV 设为忽略，避免 JVM 崩溃时直接终止）
   for (int sigid = SIGHUP; sigid < NSIG; sigid++)
   {
       if (sigid == SIGSEGV)
           clean_sa.sa_handler = SIG_IGN;
       else clean_sa.sa_handler = SIG_DFL;

       sigaction(sigid, &clean_sa, NULL);
   }

   abort_waiter_setup();
   // Boardwalk: silence
   // LOGD("JLI lib = %x", (int)libjli);
   if (NULL == libjli)
   {
       LOG_TO_E("JLI lib = NULL: %s", dlerror());
       return -1;
   }
   LOG_TO_D("Found JLI lib");

   JLI_Launch_func *pJLI_Launch = (JLI_Launch_func *)dlsym(libjli, "JLI_Launch");
    // Boardwalk: silence
    // LOGD("JLI_Launch = 0x%x", *(int*)&pJLI_Launch);

   if (NULL == pJLI_Launch)
   {
       LOG_TO_E("JLI_Launch = NULL");
       return -1;
   }

   LOG_TO_D("Calling JLI_Launch");

   return pJLI_Launch(margc, margv,
                   0, NULL, // sizeof(const_jargs) / sizeof(char *), const_jargs,
                   0, NULL, // sizeof(const_appclasspath) / sizeof(char *), const_appclasspath,
                   FULL_VERSION,
                   DOT_VERSION,
                   *margv, // (const_progname != NULL) ? const_progname : *margv,
                   *margv, // (const_launcher != NULL) ? const_launcher : *margv,
                   (const_jargs != NULL) ? JNI_TRUE : JNI_FALSE,
                   const_cpwildcard, const_javaw, const_ergo_class);

}

// JNI 入口：Java 层调用此方法启动 JVM（用于在 Android 上运行 Minecraft）
JNIEXPORT jint JNICALL Java_com_oracle_dalvik_VMLauncher_launchJVM(JNIEnv *env, jclass clazz, jobjectArray argsArray) {
    jint res = 0;

    // 保存 Dalvik 的 JNIEnv 指针，供 JVM 启动线程使用
    pojav_environ->dalvikJNIEnvPtr_ANDROID = env;

    if (argsArray == NULL)
    {
        LOG_TO_E("Args array null, returning");
        return 0;
    }

    int argc = (*env)->GetArrayLength(env, argsArray);
    char **argv = convert_to_char_array(env, argsArray);
    LOG_TO_D("Done processing args");

    res = launchJVM(argc, argv);

    LOG_TO_D("Going to free args");
    free_char_array(env, argsArray, argv);

    LOG_TO_D("Free done");
    return res;
}
