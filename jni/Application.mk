#APP_STL := stlport_static
#APP_STL := stlport_shared
APP_CPPFLAGS += -frtti  
APP_STL := gnustl_static 
APP_CPPFLAGS += -fexceptions
APP_ABI := armeabi# armeabi-v7a
APP_MODULES = udt RecvFile speex amr pcm
