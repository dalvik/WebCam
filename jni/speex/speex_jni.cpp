#include <jni.h>   
  
#include <string.h>   
#include <unistd.h>   
  
//#define LOG_TAG "debug"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args) 

#include<android/log.h>

#define LOG_TAG "speex"

#include <speex/speex.h>   
#include "speex/speex_echo.h"
#include "speex/speex_preprocess.h"

//#define NN 128
//#define TAIL 1024

int sampleRate = 8000;

static int codec_open = 0;  
  
static int dec_frame_size;  
static int enc_frame_size;  
  
//static SpeexBits ebits, dbits;  
//void *enc_state;  
//void *dec_state;  
  
//static JavaVM *gJavaVM;  

SpeexEchoState *st;

extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_initEcho(JNIEnv *env, jobject obj, int frameLen, int tailLen) {
 	st = speex_echo_state_init(frameLen, tailLen);
	LOGD("### Speex init success");
	return 0;
}


extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_cancellation(JNIEnv *env, jobject obj,jshortArray mic,jshortArray ref,jshortArray out) {
	jboolean isCopy = 1; 
	short* ref_buf = (short*)env->GetShortArrayElements(mic, &isCopy); 
	short* echo_buf = (short*)env->GetShortArrayElements(ref, &isCopy); 
	short* e_buf = (short*)env->GetShortArrayElements(out, &isCopy); 
	speex_echo_cancellation(st, ref_buf, echo_buf, e_buf);
	env->ReleaseShortArrayElements(mic, (jshort*)ref_buf, 0); 
	env->ReleaseShortArrayElements(ref, (jshort*)echo_buf, 0);
	env->ReleaseShortArrayElements(out, (jshort*)e_buf, 0);  
}

extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_stopEcho(JNIEnv *env, jobject obj) {
 	speex_echo_state_destroy(st);
	LOGD("### Speex destroy success");
	return 0;
}

/*
extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_open(JNIEnv *env, jobject obj, jint compression) {  
    int tmp;  
  
    if (codec_open++ != 0)  
        return (jint)0;  
  
    speex_bits_init(&ebits);  
    speex_bits_init(&dbits);  
  
    enc_state = speex_encoder_init(&speex_nb_mode);  
    dec_state = speex_decoder_init(&speex_nb_mode);  
    tmp = compression;  
    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);  
    speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);  
    speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);  
  
    return (jint)0;  
}  
  
extern "C" JNIEXPORT jint Java_com_iped_ipcam_gui_Speex_encode  
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {  
  
        jshort buffer[enc_frame_size];  
        jbyte output_buffer[enc_frame_size];  
    int nsamples = (size-1)/enc_frame_size + 1;  
    int i, tot_bytes = 0;  
  
    if (!codec_open)  
        return 0;  
  
    speex_bits_reset(&ebits);  
  
    for (i = 0; i < nsamples; i++) {  
        env->GetShortArrayRegion(lin, offset + i*enc_frame_size, enc_frame_size, buffer);  
        speex_encode_int(enc_state, buffer, &ebits);  
    }  
    //env->GetShortArrayRegion(lin, offset, enc_frame_size, buffer);   
    //speex_encode_int(enc_state, buffer, &ebits);   
  
    tot_bytes = speex_bits_write(&ebits, (char *)output_buffer,  
                     enc_frame_size);  
    env->SetByteArrayRegion(encoded, 0, tot_bytes,  
                output_buffer);  
  
        return (jint)tot_bytes;  
}  
  
extern "C" JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_decode  
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {  
  
        jbyte buffer[dec_frame_size];  
        jshort output_buffer[dec_frame_size];  
        jsize encoded_length = size;  
  
    if (!codec_open)  
        return 0;  
  
    env->GetByteArrayRegion(encoded, 0, encoded_length, buffer);  
    speex_bits_read_from(&dbits, (char *)buffer, encoded_length);  
    speex_decode_int(dec_state, &dbits, output_buffer);  
    env->SetShortArrayRegion(lin, 0, dec_frame_size,  
                 output_buffer);  
  
    return (jint)dec_frame_size;  
}  
  
extern "C"  JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_Speex_getFrameSize  
    (JNIEnv *env, jobject obj) {  
  
    if (!codec_open)  
        return 0;  
    return (jint)enc_frame_size;  
  
}  
  
extern "C"  JNIEXPORT void JNICALL Java_com_iped_ipcam_gui_Speex_close  
    (JNIEnv *env, jobject obj) {  
  
    if (--codec_open != 0)  
        return;  
  
    speex_bits_destroy(&ebits);  
    speex_bits_destroy(&dbits);  
    speex_decoder_destroy(dec_state);  
    speex_encoder_destroy(enc_state);  
}  
*/
