package com.iped.ipcam.utils;

public class ErrorCode {

	public final static int STUN_ERR_INTERNAL = -1;// internal error
	
	public final static int STUN_ERR_SERVER = -2; // server can't be reached
													// (not in WAN)
	public final static int STUN_ERR_TIMEOUT = -3;// timeout (camera
													// unreachable)
	public final static int STUN_ERR_INVALIDID = -4; // invalid camera id (not
														// registered)
	public final static int STUN_ERR_CONNECT = -5; // connect failure (a.k.a
													// STUN failure)
	public final static int STUN_ERR_BIND = -6; // bind failure (UDT on UDP)
}
