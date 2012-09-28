#include "jni.h"
//#include <utils/Log.h>

//#define LOG_TAG "debug"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args) 

#include<android/log.h>

#define LOG_TAG "webcam"

#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <netdb.h> #include <cc.h> #include <arpa/inet.h> #include <netdb.h>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <udt.h>

using namespace std;

extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_utils_UdtTools_initialize(JNIEnv *env, jobject thiz) {
	LOGI("--->webcam jni init success");
//	UDT::startup();
	 
	struct addrinfo hints, *local, *peer;

   	memset(&hints, 0, sizeof(struct addrinfo));

  	hints.ai_flags = AI_PASSIVE;

   	hints.ai_family = AF_INET;

   	hints.ai_socktype = SOCK_STREAM;

 	if (0 != getaddrinfo(NULL, "9000", &hints, &local))
   	{
		LOGI("--->incorrect netwok address.");
      		return 0;
  	}

//   UDTSOCKET client = UDT::socket(local->ai_family, local->ai_socktype, local->ai_protocol);

	freeaddrinfo(local);

    return 0;
}

