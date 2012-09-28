#include "jni.h"
//#include <utils/Log.h>

//#define LOG_TAG "debug"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args) 

#include<android/log.h>

#define LOG_TAG "amr_to_pcm"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "typedef.h"
#include "n_proc.h"
#include "cnst.h"
#include "mode.h"
#include "frame.h"
#include "strfunc.h"
#include "sp_dec.h"
#include "d_homing.h"
#include <pthread.h>


#define AMR_MAGIC_NUMBER "#!AMR\n"
#define MAX_PACKED_SIZE (MAX_SERIAL_SIZE / 8 + 2)

//const char decoder_id[] = "@(#)$Id $";

/* frame size in serial bitstream file (frame type + serial stream + flags) */
#define SERIAL_FRAMESIZE (1+MAX_SERIAL_SIZE+5)

static Speech_Decode_FrameState *speech_decoder_state = 0;
static pthread_mutex_t amr_decode_lock;


JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_UdtTools_initAmrDecoder(JNIEnv *env, jobject thiz) 
{
	pthread_mutex_init(&amr_decode_lock,NULL);
	if (Speech_Decode_Frame_init(&speech_decoder_state, "Decoder"))
      		return -1;
	LOGI("### UdtTools initAmrDecoder success");
	return 0;
}

JNIEXPORT jint JNICALL Java_com_iped_ipcam_gui_UdtTools_amrDecoder(JNIEnv *env, jobject thiz,jbyteArray src,jint src_size,jbyteArray dst,jint dst_size,jint channels) 
{
 	 // LOGI("### 11111");
	  Word16 serial[SERIAL_FRAMESIZE];   /* coded bits                    */
	  Word16 synth[L_FRAME];             /* Synthesis                     */
	  Word32 frame;
	  enum Mode mode = (enum Mode)0;
	  enum RXFrameType rx_type = (enum RXFrameType)0;
	  Word16 reset_flag = 0;
	  Word16 reset_flag_old = 1;
	  Word16 i;
	  int x;
 	  //LOGI("### 22222");	  
	  UWord8 toc, q, ft;
	  UWord8* psrc;
	  Word16* pdst;
	  UWord8 packed_bits[MAX_PACKED_SIZE];
	  Word16 packed_size[16] = {12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0, 0, 0};
	  dst_size=0;
	  frame = 0;
	  jboolean isCopy = 1; 
	  char* srcData = (char*)(*env)->GetByteArrayElements(env,src, &isCopy); 
	  isCopy = 1;
	  char* desData = (char*)(*env)->GetByteArrayElements(env,dst, &isCopy); 
	  psrc=(UWord8*)srcData;
	  pdst=(Word16*)desData;
 	 // LOGI("### 66666");
//LOGI("des data length %d",(*env)->GetArrayLength(env,dst));
	  //pthread_mutex_lock(&amr_decode_lock);
	  while (src_size>0/*fread (&toc, sizeof(UWord8), 1, file_serial) == 1*/)
	  {
		 /* read rest of the frame based on ToC byte */
		  toc=*psrc;
		  psrc++;
		  src_size--;
//LOGI("toc = %02x",toc);
		  q  = (toc >> 2) & 0x01;
		  ft = (toc >> 3) & 0x0F;
		  if(src_size<sizeof(UWord8)*packed_size[ft]) {
		  	break;
		  }
		  memcpy(packed_bits,psrc,sizeof(UWord8)*packed_size[ft]);
		  psrc+=packed_size[ft];
		  src_size-=sizeof(UWord8)*packed_size[ft];
		  /*fread (packed_bits, sizeof(UWord8), packed_size[ft], file_serial);*/
		  rx_type = UnpackBits(q, ft, packed_bits, &mode, &serial[1]);
/*		    
		  ++frame;
		  if ( (frame%50) == 0) {
			fprintf (stderr, "\rframe=%d  ", frame);
		  }
*/
	          if (rx_type == RX_NO_DATA) {
	            mode = speech_decoder_state->prev_mode;
	          } else {
	            speech_decoder_state->prev_mode = mode;
	          }
		     /* if homed: check if this frame is another homing frame */
		     if (reset_flag_old == 1)
		     {
			 /* only check until end of first subframe */
			 reset_flag = decoder_homing_frame_test_first(&serial[1], mode);
		     }
		     /* produce encoder homing frame if homed & input=decoder homing frame */
		     if ((reset_flag != 0) && (reset_flag_old != 0))
		     {
			 for (i = 0; i < L_FRAME; i++)
			 {
			     synth[i] = EHF_MASK;
			 }
		     }
		     else
		     {     
			 /* decode frame */
			 Speech_Decode_Frame(speech_decoder_state, mode, &serial[1],
				             rx_type, synth);
		     }
		 	/* write synthesized speech to file */
			 for(x=0;x<L_FRAME;x++){
			 	for(i=0;i<channels;i++){
  			 		pdst[i]=synth[x];
			 	}
				pdst+=channels;
			 }
			 dst_size+=L_FRAME; 
		     /*
		     if (fwrite (synth, sizeof (Word16), L_FRAME, file_syn) != L_FRAME) {
			 fprintf(stderr, "\nerror writing output file: %s\n",
				 strerror(errno));
		     };
		     fflush(file_syn);
		     */
		     /* if not homed: check whether current frame is a homing frame */

		     if (reset_flag_old == 0)
		    {
			 /* check whole frame */
			 reset_flag = decoder_homing_frame_test(&serial[1], mode);
		     }
		     /* reset decoder if current frame is a homing frame */
		     if (reset_flag != 0)
		     {
			 Speech_Decode_Frame_reset(speech_decoder_state);
		     }
		     reset_flag_old = reset_flag;
	 }
	 //LOGI("### 77777");
 	(*env)->ReleaseByteArrayElements(env, src, srcData, 0); 
	 //LOGI("### 88888");
	(*env)->ReleaseByteArrayElements(env, dst, desData, 0); 
	// LOGI("### 99999");
	return dst_size;
         
}

JNIEXPORT void JNICALL Java_com_iped_ipcam_gui_UdtTools_exitAmrDecoder(JNIEnv *env, jobject thiz) 
{
	Speech_Decode_Frame_exit(&speech_decoder_state);
}
