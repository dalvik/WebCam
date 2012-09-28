
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)


LOCAL_SRC_FILES:=\
	count.c \
	pitch_fr.c \
	d_gain_p.c \
	g_adapt.c \
	dec_lag3.c \
	dtx_dec.c \
	d_plsf_3.c \
	q_plsf_5.c \
	g_code.c \
	qgain795.c \
	gc_pred.c \
	dtx_enc.c \
	dec_amr.c \
	c8_31pf.c \
	vad2.c \
	spstproc.c \
	sqrt_l.c \
	q_gain_p.c \
	q_plsf_3.c \
	dec_lag6.c \
	int_lpc.c \
	oper_32b.c \
	c4_17pf.c \
	c1035pf.c \
	d_plsf_5.c \
	inv_sqrt.c \
	ex_ctrl.c \
	copy.c \
	lsp.c \
	lsp_avg.c \
	spreproc.c \
	pstfilt.c \
	basicop2.c \
	d_homing.c \
	bits2prm.c \
	post_pro.c \
	inter_36.c \
	bgnscd.c \
	gmed_n.c \
	ec_gains.c \
	p_ol_wgh.c \
	gain_q.c \
	dec_gain.c \
	log2.c \
	calc_en.c \
	b_cn_cod.c \
	lsp_lsf.c \
	cl_ltp.c \
	cod_amr.c \
	convolve.c \
	enc_lag3.c \
	enc_lag6.c \
	reorder.c \
	qgain475.c \
	lsp_az.c \
	a_refl.c \
	ol_ltp.c \
	lpc.c \
	mac_32.c \
	qua_gain.c \
	d_gain_c.c \
	pow2.c \
	d_plsf.c \
	set_zero.c \
	c_g_aver.c \
	ton_stab.c \
	pre_big.c \
	syn_filt.c \
	residu.c \
	weight_a.c \
	levinson.c \
	pitch_ol.c \
	calc_cor.c \
	autocorr.c \
	lsfwt.c \
	ph_disp.c \
	int_lsf.c \
	pred_lt.c \
	cor_h.c \
	az_lsp.c \
	q_plsf.c \
	q_gain_c.c \
	vad1.c \
	agc.c \
	d3_14pf.c \
	pre_proc.c \
	lag_wind.c \
	preemph.c \
	d2_9pf.c \
	g_pitch.c \
	d8_31pf.c \
	set_sign.c \
	s10_8pf.c \
	d1035pf.c \
	d4_17pf.c \
	r_fft.c \
	hp_max.c \
	prm2bits.c \
	cbsearch.c \
	c2_9pf.c \
	c3_14pf.c \
	d2_11pf.c \
	c2_11pf.c \
	vadname.c \
	strfunc.c \
	sid_sync.c \
	e_homing.c \
	n_proc.c \
	sp_dec.c \
	decoder.c \
	amr_to_pcm.c


#LOCAL_LDFLAGS :=  spc.a fipop.a

LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -lm -llog
LOCAL_CFLAGS += -DMMS_IO -O3 -DWMOPS=0 -DVAD1


LOCAL_MODULE_TAGS := amr

# This is the target being built.
LOCAL_MODULE:= libamr

# All of the shared libraries we link against.
LOCAL_SHARED_LIBRARIES := \
	libutils


# Also need the JNI headers.
LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE)

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

