#ifndef _CODEC_H_
#define _CODEC_H_

#include "chp_comdef.h"

typedef enum
{
	CHP_DRI_CODEC_MP3 = 0x100,
	CHP_DRI_CODEC_AAC_ADTS,
	CHP_DRI_CODEC_AAC_RAW,
	CHP_DRI_CODEC_ADPCM,
	CHP_DRI_CODEC_AMRNB,
	CHP_DRI_CODEC_AMRWB,
	CHP_DRI_CODEC_WMA,
	CHP_DRI_CODEC_MIDI0,
	CHP_DRI_CODEC_OGG,
	CHP_DRI_CODEC_SBC
}CHP_AUD_CODEC_E;

enum
{
	//decode
	CHP_RTN_AUD_DEC_INIT_FAIL = 0x200,
	CHP_RTN_AUD_GET_INFO_FAIL,
	CHP_RTN_AUD_DEC_FAIL,
	CHP_RTN_AUD_DEC_CLOSE_FAIL,
	CHP_RTN_AUD_OP_TIMEOUT,
	//encode
	CHP_RTN_AUD_ENC_INIT_FAIL = 0x2000,
	CHP_RTN_AUD_ENC_FAIL,
	CHP_RTN_AUD_ENC_TIMEOUT,
	CHP_RTN_AUD_ENC_CLOSE_FAIL,
	CHP_RTN_AUD_ENC_NEED_MORE_DATA
};

typedef struct
{
	CHP_U32 bit_rate;			//������
	CHP_U32 sample_rate;		//������
	CHP_U32 channel_mode;		//mono, 0 ; stereo, 1
	CHP_U32 codec_info_handle; 
}CHP_AUD_DEC_INFO_T;

typedef struct
{
	void *p_in_buf;				//����ԭʼ���ݵĻ�������ַ
	void *p_out_buf;			//���PCM���ݵĻ�������ַ
	CHP_U32 in_buf_len;			//��Ч����ԭʼ���ݵĴ�С
	CHP_U32 out_buf_len;		//���PCM���ݻ������Ĵ�С
	CHP_U32 frame_cnt;			//��Ҫ�����֡��
	CHP_U32 used_size;			//����ʵ���õ���ԭʼ���ݴ�С
	CHP_U32 dec_frame_cnt;		//ʵ�ʽ����֡��
	CHP_U32 dec_pcm_size;		//ʵ�ʽ����PCM���ݴ�С
}CHP_AUD_DEC_DATA_T;

typedef struct
{
	CHP_U32 audio_type;			//AAC,mp3...
	CHP_U32 bit_rate;			//12800��
	CHP_U32 sample_rate;		//8K, 32K, 44.1K��
	CHP_U32 sample_size;		//16bit, 18bit, 24bit��
	CHP_U32 channel_mode;		//mono, 0 ; stereo, 1
}CHP_AUD_ENC_INFO_T;

typedef struct
{
	void *p_in_buf;				//RAW PCM���ݴ洢�ĵ�ַ
	void *p_out_buf;			//��������Ƶ���ݴ�ŵ�ַ
	CHP_U32 in_buf_len;			//RAW PCM����Ĵ�С
	CHP_U32 out_buf_len;		//ѹ���������Ĵ�С
	CHP_U32 frame_cnt;			//��Ҫ�����֡��
	CHP_U32 used_size;			//ʵ��ʹ�õ�RAW PCM�洢��������С
	CHP_U32 enc_frame_cnt;		//ʵ�ʱ����֡��
	CHP_U32 enc_data_len; 		//ѹ������Ƶ���ݵĳ���
}CHP_AUD_ENC_DATA_T;

#endif
