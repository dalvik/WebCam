#include "jni.h"
//#include <utils/Log.h>

//#define LOG_TAG "debug"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args) 

#include<android/log.h>

#define LOG_TAG "pcm_to_amr"
#include "amrnb_encode.h"
#include <stdio.h>
#include <stdlib.h>

static CHP_MEM_FUNC_T mem_func;
static CHP_AUD_ENC_INFO_T audio_info;
static CHP_AUD_ENC_DATA_T enc_data;
static CHP_U32 bl_handle;

JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_UdtTools_initAmrEncoder(JNIEnv *env, jobject thiz) 
{
	CHP_RTN_T error_flag;
	mem_func.chp_malloc = (CHP_MALLOC_FUNC)malloc;
	mem_func.chp_free = (CHP_FREE_FUNC)free;
	mem_func.chp_memset = (CHP_MEMSET)memset;
	mem_func.chp_memcpy = (CHP_MEMCPY)memcpy;
	audio_info.audio_type = CHP_DRI_CODEC_AMRNB;
	audio_info.bit_rate = 12200;
	error_flag = amrnb_encoder_init( &mem_func, &audio_info, & bl_handle);
	if(error_flag!=CHP_RTN_SUCCESS){
		LOGD("### UdtTools initAmrEncoder error!,%x ",error_flag);
		exit(0);
		return -1;
	}
	enc_data.p_in_buf = NULL;  
	enc_data.p_out_buf = 0;
	enc_data.frame_cnt = 1;
	enc_data.in_buf_len = 0;
	enc_data.out_buf_len =0;
	enc_data.used_size = 0;
	enc_data.enc_data_len = 0;
	LOGD("### UdtTools initAmrEncoder success");
	return 0;
}

JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_UdtTools_EncoderPcm(JNIEnv *env, jobject thiz,jshortArray pcmBuffer, int pcm_frames,jshortArray amrBuffer, int amr_len) 
{
	CHP_RTN_T error_flag;
 	jboolean isCopy = 1; 
	short* pcm_buffer = (short*)(*env)->GetShortArrayElements(env,pcmBuffer, &isCopy); 
	short* amr_buffer = (short*)(*env)->GetShortArrayElements(env,amrBuffer, &isCopy);
	enc_data.p_in_buf = pcm_buffer;
	enc_data.p_out_buf = amr_buffer;
	enc_data.frame_cnt = pcm_frames/320;
	enc_data.in_buf_len = pcm_frames;
	enc_data.out_buf_len = amr_len;
	error_flag = amrnb_encode(bl_handle,&enc_data);
	if(error_flag == CHP_RTN_AUD_ENC_FAIL || error_flag == CHP_RTN_AUD_ENC_NEED_MORE_DATA){
		LOGD("### UdtTools amr encode fail! %x ",error_flag);
		(*env)->ReleaseShortArrayElements(env, pcmBuffer, pcm_buffer, 0); 
		(*env)->ReleaseShortArrayElements(env, amrBuffer, amr_buffer, 0); 
		return -1;
	}
	(*env)->ReleaseShortArrayElements(env, pcmBuffer, pcm_buffer, 0); 
	(*env)->ReleaseShortArrayElements(env, amrBuffer, amr_buffer, 0); 
	return 0;
}
/*
JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_UdtTools_EncoderPcm(JNIEnv *env, jobject thiz,jbyteArray pcmBuffer, int pcm_frames,jbyteArray amrBuffer, int amr_len) 
{
	CHP_RTN_T error_flag;
 	jboolean isCopy = 1; 
	char* pcm_buffer = (char*)(*env)->GetByteArrayElements(env,pcmBuffer, &isCopy); 
	char* amr_buffer = (char*)(*env)->GetByteArrayElements(env,amrBuffer, &isCopy);
	enc_data.p_in_buf = pcm_buffer;
	enc_data.p_out_buf = amr_buffer;
	enc_data.frame_cnt = pcm_frames/320;
	enc_data.in_buf_len = pcm_frames;
	enc_data.out_buf_len = amr_len;
	error_flag = amrnb_encode(bl_handle,&enc_data);
	if(error_flag == CHP_RTN_AUD_ENC_FAIL || error_flag == CHP_RTN_AUD_ENC_NEED_MORE_DATA){
		LOGD("### UdtTools amr encode fail! %x ",error_flag);
		(*env)->ReleaseByteArrayElements(env, pcmBuffer, pcm_buffer, 0); 
		(*env)->ReleaseByteArrayElements(env, amrBuffer, amr_buffer, 0); 
		return -1;
	}
	(*env)->ReleaseByteArrayElements(env, pcmBuffer, pcm_buffer, 0); 
	(*env)->ReleaseByteArrayElements(env, amrBuffer, amr_buffer, 0); 
	return 0;
}*/
