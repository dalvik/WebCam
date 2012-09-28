#ifndef _AMRNB_ENCODE_H_
#define _AMRNB_ENCODE_H_

#include "codec.h"

CHP_RTN_T amrnb_encoder_init(CHP_MEM_FUNC_T *p_mem_func,CHP_AUD_ENC_INFO_T *p_enc_info,CHP_U32 *bl_handle);
CHP_RTN_T amrnb_encode(CHP_U32 bl_handle,CHP_AUD_ENC_DATA_T *p_enc_data);
CHP_RTN_T amrnb_encoder_close(CHP_U32 bl_handle);

#endif
