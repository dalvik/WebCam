#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "typedef.h"
#include "cnst.h"
#include "n_proc.h"
#include "mode.h"
#include "frame.h"
#include "strfunc.h"
#include "sp_enc.h"
#include "pre_proc.h"
#include "sid_sync.h"
#include "vadname.h"
#include "e_homing.h"
#include <pthread.h>

#ifndef ssize_t
typedef int ssize_t;
#endif
#define AMR_MAGIC_NUMBER "#!AMR\n"
#define MAX_PACKED_SIZE (MAX_SERIAL_SIZE / 8 + 2)

const char coder_id[] = "@(#)$Id $";

/* frame size in serial bitstream file (frame type + serial stream + flags) */
#define SERIAL_FRAMESIZE (1+MAX_SERIAL_SIZE+5)


#define DTX_DEFAULT       1   
#define DEFAULT_MODE    MR122  

static Speech_Encode_FrameState *speech_encoder_state = NULL;
static sid_syncState *sid_state = NULL;
static pthread_mutex_t amr_encode_lock;

int init_amrcoder(short  dtx_t)
{
	Word16 dtx = 1;    /* enable encoder DTX                */
	if(dtx_t<0||dtx_t>1)
		dtx = DTX_DEFAULT;
	else
		dtx=(Word16)dtx_t;
	pthread_mutex_init(&amr_encode_lock,NULL);
	if (   Speech_Encode_Frame_init(&speech_encoder_state, dtx, "encoder")
     		 || sid_sync_init (&sid_state))
      		return -1;
	return 0;
}

void  exit_amrcoder()
{
	Speech_Encode_Frame_exit(&speech_encoder_state);
 	 sid_sync_exit (&sid_state);
}

int amrcoder(char *src,ssize_t src_size,char *dst,ssize_t *dst_size,int armmode,int channels){
	char *modeStr = NULL;
  	char *usedModeStr = NULL;
  
  	Word16 new_speech[L_FRAME];         /* Pointer to new speech data        */
  	Word16 serial[SERIAL_FRAMESIZE];    /* Output bitstream buffer           */
	Word16 *psrc;

  	UWord8 packed_bits[MAX_PACKED_SIZE];
 	 Word16 packed_size;

  	Word32 frame;                  
  
  /* changed eedodr */
  	Word16 reset_flag;

  	int i;
	int y;
  	enum Mode mode;
  	enum Mode used_mode;
  	enum TXFrameType tx_type;
	
	*dst_size=0;
	if(armmode<0||armmode>7)
		mode=DEFAULT_MODE;
	else
		mode=(enum Mode)armmode;
   /*-----------------------------------------------------------------------*
   * Initialisation of the coder.                                          *
   *-----------------------------------------------------------------------*/
   
  /*
  	if (   Speech_Encode_Frame_init(&speech_encoder_state, dtx, "encoder")
     		 || sid_sync_init (&sid_state))
      		return -1;
 */
  frame = 0;
  psrc=(Word16 *)src;
  /*16 bits 2channels each pcm frame occupy 4bytes*/
  pthread_mutex_lock(&amr_encode_lock);
  while (src_size>=2*channels*L_FRAME)
  {
     /*use left channel  */
      for(y=0;y<L_FRAME;y++){
	  	new_speech[y]=psrc[0];
		psrc+=channels;
      }
      src_size-=L_FRAME*2*channels;
      frame++;
     
     /* zero flags and parameter bits */
     for (i = 0; i < SERIAL_FRAMESIZE; i++)
         serial[i] = 0;

     /* check for homing frame */
     reset_flag = encoder_homing_frame_test(new_speech);
     
     /* encode speech */
     Speech_Encode_Frame(speech_encoder_state, mode,
                         new_speech, &serial[1], &used_mode); 

     /* print frame number and mode information */
     mode2str(mode, &modeStr);
     mode2str(used_mode, &usedModeStr);
     if ( (frame%50) == 0) {
        fprintf (stderr, "\rframe=%-8d mode=%-5s used_mode=%-5s",
                 frame, modeStr, usedModeStr);
     }

     /* include frame type and mode information in serial bitstream */
     sid_sync (sid_state, used_mode, &tx_type);

     packed_size = PackBits(used_mode, mode, tx_type, &serial[1], packed_bits);

     /* write file storage format bitstream to output file */
	 memcpy(dst,packed_bits,sizeof(UWord8)*packed_size);
	 dst+=sizeof(UWord8)*packed_size;
	 *dst_size+=sizeof(UWord8)*packed_size;
	 /*
     if (fwrite (packed_bits, sizeof (UWord8), packed_size, file_serial)
         != packed_size) {
         fprintf(stderr, "\nerror writing output file: %s\n",
                 strerror(errno));
         exit(-1);
     }
     fflush(file_serial);
	*/
     /* perform homing if homing frame was detected at encoder input */
     if (reset_flag != 0)
     {
         Speech_Encode_Frame_reset(speech_encoder_state);
         sid_sync_reset(sid_state);
     }
  }
  pthread_mutex_unlock(&amr_encode_lock);
  
  /*-----------------------------------------------------------------------*
   * Close down speech coder                                               *
   *-----------------------------------------------------------------------*/
/*
  	Speech_Encode_Frame_exit(&speech_encoder_state);
 	 sid_sync_exit (&sid_state);
 
  */
  return (0);
}

