# A simple test for the minimal standard C++ library
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_LDLIBS := libudt.so
#LOCAL_SHARED_LIBRARIES := libudt.so

LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog

LOCAL_CPPFLAGS += -fexceptions

LOCAL_MODULE := RecvFile 

LOCAL_MODULE_TAGS := libRecvFile
  
# Also need the JNI headers.
LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE)

LOCAL_STATIC_LIBRARIES := libm

LOCAL_SHARED_LIBRARIES := \
	libutils udt

LOCAL_SRC_FILES :=  \
                   udttools.cpp udt4/app/stun.cpp udt4/app/stun-monitor.cpp  

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
