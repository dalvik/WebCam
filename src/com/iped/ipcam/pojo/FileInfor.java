package com.iped.ipcam.pojo;

public class FileInfor {

	private String fileName;
	
	private String filePath;
	
	private String absolutePath;

	public FileInfor() {
		super();
	}

	public FileInfor(String fileName, String filePath, String absolutePath) {
		super();
		this.fileName = fileName;
		this.filePath = filePath;
		this.absolutePath = absolutePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}
	
}
