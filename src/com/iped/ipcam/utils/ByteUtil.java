package com.iped.ipcam.utils;

public class ByteUtil {

	// 以下 是整型数 和 网络字节序的 byte[] 数组之间的转换
	public static byte[] longToBytes(long n) {
		byte[] b = new byte[8];
		b[7] = (byte) (n & 0xff);
		b[6] = (byte) (n >> 8 & 0xff);
		b[5] = (byte) (n >> 16 & 0xff);
		b[4] = (byte) (n >> 24 & 0xff);
		b[3] = (byte) (n >> 32 & 0xff);
		b[2] = (byte) (n >> 40 & 0xff);
		b[1] = (byte) (n >> 48 & 0xff);
		b[0] = (byte) (n >> 56 & 0xff);
		return b;
	}

	public static void longToBytes(long n, byte[] array, int offset) {
		array[7 + offset] = (byte) (n & 0xff);
		array[6 + offset] = (byte) (n >> 8 & 0xff);
		array[5 + offset] = (byte) (n >> 16 & 0xff);
		array[4 + offset] = (byte) (n >> 24 & 0xff);
		array[3 + offset] = (byte) (n >> 32 & 0xff);
		array[2 + offset] = (byte) (n >> 40 & 0xff);
		array[1 + offset] = (byte) (n >> 48 & 0xff);
		array[0 + offset] = (byte) (n >> 56 & 0xff);
	}

	public static long bytesToLong(byte[] array) {
		return ((((long) array[0] & 0xff) << 56)
				| (((long) array[1] & 0xff) << 48)
				| (((long) array[2] & 0xff) << 40)
				| (((long) array[3] & 0xff) << 32)
				| (((long) array[4] & 0xff) << 24)
				| (((long) array[5] & 0xff) << 16)
				| (((long) array[6] & 0xff) << 8) | (((long) array[7] & 0xff) << 0));
	}

	public static long bytesToLong(byte[] array, int offset) {
		return ((((long) array[offset + 0] & 0xff) << 56)
				| (((long) array[offset + 1] & 0xff) << 48)
				| (((long) array[offset + 2] & 0xff) << 40)
				| (((long) array[offset + 3] & 0xff) << 32)
				| (((long) array[offset + 4] & 0xff) << 24)
				| (((long) array[offset + 5] & 0xff) << 16)
				| (((long) array[offset + 6] & 0xff) << 8) | (((long) array[offset + 7] & 0xff) << 0));
	}

	public static byte[] intToBytes(int n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);
		return b;
	}

	public static void intToBytes(int n, byte[] array, int offset) {
		array[3 + offset] = (byte) (n & 0xff);
		array[2 + offset] = (byte) (n >> 8 & 0xff);
		array[1 + offset] = (byte) (n >> 16 & 0xff);
		array[offset] = (byte) (n >> 24 & 0xff);
	}

	public static int bytesToInt(byte b[]) {
		return b[3] & 0xff | (b[2] & 0xff) << 8 | (b[1] & 0xff) << 16
				| (b[0] & 0xff) << 24;
	}

	public static int byteToInt2(byte[] b) {   
		return b[0] | (b[1]<<8) | (b[2]<<16) | (b[3]<<24);
	}
	
	public static int byteToInt4(byte[] b, int offSet) {
		int n;
		n = 0;
		n = (n<<8)|(b[3+offSet]&0xff);
		n = (n<<8)|(b[2+offSet]&0xff);
		n = (n<<8)|(b[1+offSet]&0xff);
		n = (n<<8)|(b[0+offSet]&0xff);
		return n;
	}
	
	public static int byteToInt3(byte[] b, int offset) {   
		return b[0+offset] | (b[offset+1]<<8) | (b[offset+2]<<16) | (b[offset+3]<<24);
	}
	
	public static int byte2int(byte[] res) { 
		// 一个byte数据左移24位变成0x??000000，再右移8位变成0x00??0000 
		int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) // | 表示安位或 
		| ((res[2] << 24) >>> 8) | (res[3] << 24); 
		return targets; 
	} 

	
	public static int bytesToInt(byte b[], int offset) {
		return b[offset + 3] & 0xff | (b[offset + 2] & 0xff) << 8
				| (b[offset + 1] & 0xff) << 16 | (b[offset] & 0xff) << 24;
	}

	public static byte[] uintToBytes(long n) {
		byte[] b = new byte[4];
		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);

		return b;
	}

	public static void uintToBytes(long n, byte[] array, int offset) {
		array[3 + offset] = (byte) (n);
		array[2 + offset] = (byte) (n >> 8 & 0xff);
		array[1 + offset] = (byte) (n >> 16 & 0xff);
		array[offset] = (byte) (n >> 24 & 0xff);
	}

	public static long bytesToUint(byte[] array) {
		return ((long) (array[3] & 0xff)) | ((long) (array[2] & 0xff)) << 8
				| ((long) (array[1] & 0xff)) << 16
				| ((long) (array[0] & 0xff)) << 24;
	}

	public static long bytesToUint(byte[] array, int offset) {
		return ((long) (array[offset + 3] & 0xff))
				| ((long) (array[offset + 2] & 0xff)) << 8
				| ((long) (array[offset + 1] & 0xff)) << 16
				| ((long) (array[offset] & 0xff)) << 24;
	}

	public static byte[] shortToBytes(short n) {
		byte[] b = new byte[2];
		b[1] = (byte) (n & 0xff);
		b[0] = (byte) ((n >> 8) & 0xff);
		return b;
	}

	public static void shortToBytes(short n, byte[] array, int offset) {
		array[offset + 1] = (byte) (n & 0xff);
		array[offset] = (byte) ((n >> 8) & 0xff);
	}

	public static short bytesToShort(byte[] b) {
		return (short) (b[1] & 0xff | (b[0] & 0xff) << 8);
	}

	public static short bytesToShort(byte[] b, int offset) {
		return (short) (b[offset + 1] & 0xff | (b[offset] & 0xff) << 8);
	}

	public static byte[] ushortToBytes(int n) {
		byte[] b = new byte[2];
		b[1] = (byte) (n & 0xff);
		b[0] = (byte) ((n >> 8) & 0xff);
		return b;
	}

	public static void ushortToBytes(int n, byte[] array, int offset) {
		array[offset + 1] = (byte) (n & 0xff);
		array[offset] = (byte) ((n >> 8) & 0xff);
	}

	public static int bytesToUshort(byte b[]) {
		return b[1] & 0xff | (b[0] & 0xff) << 8;
	}

	public static int bytesToUshort(byte b[], int offset) {
		return b[offset + 1] & 0xff | (b[offset] & 0xff) << 8;
	}

	public static byte[] ubyteToBytes(int n) {
		byte[] b = new byte[1];
		b[0] = (byte) (n & 0xff);
		return b;
	}

	public static void ubyteToBytes(int n, byte[] array, int offset) {
		array[0] = (byte) (n & 0xff);
	}

	public static int bytesToUbyte(byte[] array) {
		return array[0] & 0xff;
	}

	public static int bytesToUbyte(byte[] array, int offset) {
		return array[offset] & 0xff;
	}

	// char 类型、 float、double 类型和 byte[] 数组之间的转换关系还需继续研究实现。

	/*public static byte[] shortTobyte(short res) {
		byte[] targets = new byte[2];
		targets[1] = (byte) ((res >> 8) & 0xFF);
		targets[0] = (byte) (res & 0xFF);
		return targets;
	}

	public static byte[] intTobyte(int res) {
		byte[] targets = new byte[4];
		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
		return targets;
	}
*/
	/**************************/

	public static String byte2Hex(byte[] buf) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("{");
		for (byte b : buf) {
			if (b == 0) {
				strbuf.append("00");
			} else if (b == -1) {
				strbuf.append("FF");
			} else {
				String str = Integer.toHexString(b).toUpperCase();
				// sb.append(a);
				if (str.length() == 8) {
					str = str.substring(6, 8);
				} else if (str.length() < 2) {
					str = "0" + str;
				}
				strbuf.append(str);
			}
			strbuf.append(" ");
		}
		strbuf.append("}");
		return strbuf.toString();
	}

	public static void main(String[] args) {
		byte[] array = new byte[4];
		array[3] = (byte) 0xF4;
		array[2] = 0x13;
		array[1] = 0x12;
		array[0] = 0x11;

		System.out.println("=================== Integer bytes =============");

		System.out.println("the bytes is:" + byte2Hex(array));
		System.out.print("println bytesToInt :");
		System.out.println(bytesToInt(array));
		System.out.printf("printf bytesToInt :%X\n", bytesToInt(array));
	}
}
