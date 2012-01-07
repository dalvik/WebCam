package com.iped.ipcam.exception;

public class CamManagerException extends Exception {

	private static final long serialVersionUID = -91093442610560881L;

	public CamManagerException() {
	}

	public CamManagerException(String detailMessage) {
		super(detailMessage);
	}

	public CamManagerException(Throwable throwable) {
		super(throwable);
	}

	public CamManagerException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
