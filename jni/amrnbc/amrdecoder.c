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

const char decoder_id[] = "@(#)$Id $";

/* frame size in serial bitstream file (frame type + serial stream + flags) */
#define SERIAL_FRAMESIZE (1+MAX_SERIAL_SIZE+5)

 static Speech_Decode_FrameState *speech_decoder_state = NULL;
 static pthread_mutex_t amr_decode_lock;


 int init_amrdecoder()
 {
 	pthread_mutex_init(&amr_decode_lock,NULL);
 	if (Speech_Decode_Frame_init(&speech_decoder_state, "Decoder"))
      		return -1;
	return 0;
 }




void exit_amrdecoder()
{
 	Speech_Decode_Frame_exit(&speech_decoder_state);
 }
/*
********************************************************************************
*                             MAIN PROGRAM 
********************************************************************************
*/

/*
*src buffer of amr data 
*src_size  bytes of amr data
*dst buffer to store pcm data
*dst_size  frames of pcm sound data that have been converted 
*/
int amrdecoder(char *src,ssize_t src_size,char *dst,ssize_t*dst_size,int channels)
{
  
  Word16 serial[SERIAL_FRAMESIZE];   /* coded bits                    */
  Word16 synth[L_FRAME];             /* Synthesis                     */
  Word32 frame;

  enum Mode mode = (enum Mode)0;
  enum RXFrameType rx_type = (enum RXFrameType)0;
     
  Word16 reset_flag = 0;
  Word16 reset_flag_old = 1;
  Word16 i;
  int x;
  
  UWord8 toc, q, ft;
  UWord8*psrc;
  Word16*pdst;
  UWord8 packed_bits[MAX_PACKED_SIZE];
  Word16 packed_size[16] = {12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0, 0, 0};


  *dst_size=0;

   /* read and verify magic number */
/*
  fread(magic, sizeof(Word8), strlen(AMR_MAGIC_NUMBER), file_serial);
  if (strncmp((const char *)magic, AMR_MAGIC_NUMBER, strlen(AMR_MAGIC_NUMBER)))
  {
	   fprintf(stderr, "%s%s\n", "Invalid magic number: ", magic);
	   fclose(file_serial);
	   fclose(file_syn);
	   return 1;
   }
   */

  /*-----------------------------------------------------------------------*
   * Initialization of decoder                                             *
   *-----------------------------------------------------------------------*/
   /*
  	if (Speech_Decode_Frame_init(&speech_decoder_state, "Decoder"))
      		return -1;
    */
  /*-----------------------------------------------------------------------*
   * process serial bitstream frame by frame                               *
   *-----------------------------------------------------------------------*/
  frame = 0;
  psrc=(UWord8*)src;
  pdst=(Word16*)dst;
  pthread_mutex_lock(&amr_decode_lock);
  while (src_size>0/*fread (&toc, sizeof(UWord8), 1, file_serial) == 1*/)
  {
	  /* read rest of the frame based on ToC byte */
	  toc=*psrc;
	  psrc++;
	  src_size--;
	  q  = (toc >> 2) & 0x01;
	  ft = (toc >> 3) & 0x0F;
	  if(src_size<sizeof(UWord8)*packed_size[ft])
	  	break;
	  memcpy(packed_bits,psrc,sizeof(UWord8)*packed_size[ft]);
	  psrc+=packed_size[ft];
	  src_size-=sizeof(UWord8)*packed_size[ft];
	  /*fread (packed_bits, sizeof(UWord8), packed_size[ft], file_serial);*/

	  rx_type = UnpackBits(q, ft, packed_bits, &mode, &serial[1]);

     ++frame;
     if ( (frame%50) == 0) {
        fprintf (stderr, "\rframe=%d  ", frame);
     }

     if (rx_type == RX_NO_DATA) {
       mode = speech_decoder_state->prev_mode;
     }
     else {
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
	 *dst_size+=L_FRAME; 
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
  pthread_mutex_unlock(&amr_decode_lock);
  /*fprintf (stderr, "\n%d frame(s) processed\n", frame);*/
  
  /*-----------------------------------------------------------------------*
   * Close down speech decoder                                             *
   *-----------------------------------------------------------------------*/
   /*
	  Speech_Decode_Frame_exit(&speech_decoder_state);
	  */
  
  return 0;
}

