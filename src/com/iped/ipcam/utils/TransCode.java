package com.iped.ipcam.utils;

import java.io.UnsupportedEncodingException;


/**
 * 
 * Copyright:Copyright Love999(c) 2010
 * Company:Love999
 * @author sky
 * @version 1.0
 */
/**
 *<p>进制转换工具类</p>
 *<p>Description:各种数制间的转换</p>
 *<p>Copyright: Copyright Love999(c) 2010</p>
 *<p>Company:HangZhou Love999 Technology Development Co.Ltd</p>
 *@author 桑开洋
 *@version 1.0
 */
public class TransCode {

	/**
	 * char型转化为十六进制
	 * 开始符
	 * @param c
	 * @return
	 */
	public static String toHexChar(char c) {
		String str = Integer.toHexString(c);
		return str + " ";
	}
	
	/**
	 * int型转化为四字节十六进制
	 * 中心编码
	 * @param i
	 * @return
	 */
	public static String toHexInt4(Long n){
		StringBuffer sb = new StringBuffer();
		String str = Long.toHexString(n);
		if(str.length()%2 != 0)
			str = "0" + str;
		if(str.length()< 4){
			str = "00" + str;
		}
		if(str.length() < 6){
			str = "00" + str;
		}
		if(str.length() < 8){
			str = "00" + str;
		}
		for(int i = 0;i <=str.length()-1; i++){
			char tempStr = str.charAt(i);
			if(i%2 !=0){
				sb  = sb.append(tempStr+" ");
			}else{
				sb  = sb.append(tempStr);
			}
		}
		return sb.toString();
	}
	
	/**
     * byte类型转换为十六进制
     * 序号
     * @param b
     * @return
     */
    public static String byte2HexString(byte b){
    	String str = Integer.toHexString(b);
    	if(str.length() !=2){
    		str = "0" + str;
    	}
    	return str + " ";
    }

    /**
     * byte类型转换为四字节的十六进制
     * 序号
     * @param b
     * @return
     */
    public static String byteTo4HexString(byte b){
    	String str = Integer.toHexString(b);
    	StringBuffer sb = new StringBuffer();
    	if(str.length() !=2){
    		str = "0" + str;
    	}
    	sb.append(str+" 00 00 00 ");
    	return sb.toString();
    }
	/**
	 * bytes数组转换成十六进制字符串
	 * 指令
	 * @param common
	 * @return
	 */
    public static String byte2HexStr(char[] common) {
        String hs="";
        String stmp="";
        for (int n=0;n<common.length;n++) {
            stmp=(Integer.toHexString(common[n] & 0XFF));
            if (stmp.length()==1) hs=hs+"0"+stmp +" ";
            else hs=hs+stmp+" ";
            //if (n<b.length-1)  hs=hs+":";
        }
        return hs;
    }
    
    /**
	 * INT型转化为一字节十六进制
	 * 数据长度
	 * @param n
	 * @return
	 */
	public static String toHexInt1(float n){
		StringBuffer sb = new StringBuffer();
		String str = Integer.toHexString((int)n);
		if(str.length()%2 != 0)
			str = "0" + str;
		for(int i = 0;i <=str.length()-1; i++){
			char tempStr = str.charAt(i);
			if(i%2 !=0){
				sb  = sb.append(tempStr+" ");
			}else{
				sb  = sb.append(tempStr);
			}
		}
		return sb.toString();
	}
	
    /**
	 * int型转化为两字节十六进制
	 * 数据长度
	 * @param n
	 * @return
	 */
	public static String toHexInt2(int n){
		StringBuffer sb = new StringBuffer();
		String str = Integer.toHexString(n);
		if(str.length()%2 != 0)
			str = "0" + str;
		if(str.length()< 4){
			str = "00" + str;
		}
		for(int i = 0;i <=str.length()-1; i++){
			char tempStr = str.charAt(i);
			if(i%2 !=0){
				sb  = sb.append(tempStr+" ");
			}else{
				sb  = sb.append(tempStr);
			}
		}
		return sb.toString();
	}
	
	
	/**
	 * int型转化为三字节十六进制
	 * 数据长度
	 * @param n
	 * @return
	 */
	public static String toHexInt3(int n){
		StringBuffer sb = new StringBuffer();
		String str = Integer.toHexString(n);
		if(str.length()%2 != 0)
			str = "0" + str;
		if(str.length()< 4){
			str = "00" + str;
		}
		if(str.length()< 6){
			str = "00" + str;
		}
		for(int i = 0;i <=str.length()-1; i++){
			char tempStr = str.charAt(i);
			if(i%2 !=0){
				sb  = sb.append(tempStr + " ");
			}else{
				sb  = sb.append(tempStr);
			}
		}
		return sb.toString();
	}
	
	
	/**
	 * 字符串转换为十六进制
	 * 数据的内容
	 * @param str
	 * @return
	 */
	public static String toHexString(String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			int ch = (int) str.charAt(i);
			String ss = Integer.toHexString(ch);
			sb.append(ss + " ");
		}
		return sb.toString();
	}
	
	
	//将中文转成HexString
	public static String chineseToHexString(String str){
		if(str==null || str.length()<=0){
			return "";
		}
	    byte[] a = str.getBytes();
	    StringBuffer sb = new StringBuffer();
	    for(int i=0; i<a.length; i++){
	    	sb.append(Integer.toHexString((256+a[i])%256) + " ");
	    }   
		return sb.toString();
	}
	
	//将中文转成HexString
	public static String chineseToHexString2(String str){
		if(str==null || str.length()<=0){
			return "";
		}
	    byte[] a = new byte[0];
		try {
			a = str.getBytes("GBK");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  
	    StringBuffer sb = new StringBuffer();
	    for(int i=0; i<a.length; i++){
	    	sb.append(Integer.toHexString((256+a[i])%256) + " ");
	    }   
		return sb.toString();
	}
	 
	public static String toUnicode(String str){
	        char[]arChar=str.toCharArray();
	        int iValue=0;
	        String uStr="";
	        for(int i=0;i<arChar.length;i++){
	            iValue=(int)str.charAt(i);           
	            if(iValue<=256){
	                uStr+="\\u00"+Integer.toHexString(iValue);
	            }else{
	                uStr+="\\u"+Integer.toHexString(iValue);
	            }
	        }
	        return uStr;
	    }

		public static String decodeUnicode(final String dataStr) {
			int start = 0;
			int end = 0;
			StringBuffer buffer = new StringBuffer();
			while (start > -1) {
				end = dataStr.indexOf("\\u", start + 2);
				String charStr = "";
				if (end == -1) {
					charStr = dataStr.substring(start + 2, dataStr.length());
				} else {
					charStr = dataStr.substring(start + 2, end);
				}
				char letter = (char) Integer.parseInt(charStr, 16); // 16进制parse整形字符串。
				buffer.append(new Character(letter).toString());
				start = end;
			}
			return buffer.toString();
		}
		
		private static String hexString = "0123456789ABCDEF";

		// 中文转十六进制
		public static String encode(String str) {
			byte[] bytes = str.getBytes();
			StringBuilder sb = new StringBuilder(bytes.length * 2);
			// 将字节数组中每个字节拆解成2位16进制整数
			for (int i = 0; i < bytes.length; i++){
				sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
				sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0) + " ");
			}
			return sb.toString();
		}
		
		
}
