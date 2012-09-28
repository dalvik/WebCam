#************encode amr*************

TOP_LOCAL_PATH := $(call my-dir)

include $(call all-subdir-makefiles)

LOCAL_PATH := $(TOP_LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= pcm_to_amr.c

LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -lm -llog

LOCAL_MODULE_TAGS := pcm

# This is the target being built.
LOCAL_MODULE:= libpcm

# Also need the JNI headers.
LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE)

LOCAL_STATIC_LIBRARIES := AMR_NB_ENC

include $(BUILD_SHARED_LIBRARY)
