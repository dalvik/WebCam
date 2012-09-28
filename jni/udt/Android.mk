# A simple test for the minimal standard C++ library
#

LOCAL_PATH := $(call my-dir)

LOCAL_CPP_EXTENSION := .cpp

LOCAL_LDLIBS :=    libm.so
 
include $(CLEAR_VARS)

LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -lm -llog

LOCAL_MODULE := udt 


LOCAL_SRC_FILES := md5.cpp common.cpp window.cpp list.cpp buffer.cpp packet.cpp channel.cpp queue.cpp ccc.cpp cache.cpp core.cpp epoll.cpp api.cpp  


include $(BUILD_SHARED_LIBRARY)
