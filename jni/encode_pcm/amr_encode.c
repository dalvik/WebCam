#include "amrnb_encode.h"

static CHP_MEM_FUNC_T mem_func;
static CHP_AUD_ENC_INFO_T audio_info;
static CHP_AUD_ENC_DATA_T enc_data;
static CHP_U32 bl_handle;

void init_amr_codec()
{
	CHP_RTN_T error_flag;
	//int i;
	mem_func.chp_malloc = (CHP_MALLOC_FUNC)malloc;
	mem_func.chp_free = (CHP_FREE_FUNC)free;
	mem_func.chp_memset = (CHP_MEMSET)memset;
	mem_func.chp_memcpy = (CHP_MEMCPY)memcpy;
	audio_info.audio_type = CHP_DRI_CODEC_AMRNB;
	audio_info.bit_rate = 12200;
	//audio_info.sample_rate = 8000;
	//audio_info.sample_size = 16;
	//audio_info.channel_mode = 1;
	error_flag = amrnb_encoder_init( &mem_func, &audio_info, & bl_handle);
	if(error_flag!=CHP_RTN_SUCCESS){
		printf("error init new amr encoder\n");
		exit(0);
	}
	enc_data.p_in_buf = NULL;  //pcm 输入缓冲
	enc_data.p_out_buf = 0//amr输出缓冲
	enc_data.frame_cnt = 1; //期望输出的amr帧数每帧32字节
	enc_data.in_buf_len = 0//输入缓冲长度
	enc_data.out_buf_len =0 //输出缓冲
	enc_data.used_size = 0;
	enc_data.enc_data_len = 0;
}

int encode_pcm(char *pcm_buffer , int pcm_frames,char *amr_buffer , int amr_len)
{
	CHP_RTN_T error_flag;
	enc_data.p_in_buf = pcm_buffer;
	enc_data.p_out_buf = amr_buffer;
	enc_data.frame_cnt = amr_len/32;//pcm_frames/160
	enc_data.in_buf_len = pcm_frames*2;
	enc_data.out_buf_len = amr_len;
	error_flag = amrnb_encode(bl_handle,&enc_data);
	if(error_flag == CHP_RTN_AUD_ENC_FAIL || error_flag == CHP_RTN_AUD_ENC_NEED_MORE_DATA){
		printf("###########amr encode fail##################\n");
		return -1;
	}
	return 0;
}
