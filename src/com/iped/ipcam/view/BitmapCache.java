package com.iped.ipcam.view;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Hashtable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
/**
 * ��ֹ���
 *
 */
public class BitmapCache {
	static private BitmapCache cache;
	/** ����Chche���ݵĴ洢 */
	private Hashtable<String, BtimapRef> bitmapRefs;
	/** ����Reference�Ķ��У������õĶ����Ѿ������գ��򽫸����ô�������У� */
	private ReferenceQueue<Bitmap> q;

	/**
	 * �̳�SoftReference��ʹ��ÿһ��ʵ�������п�ʶ��ı�ʶ��
	 */
	private class BtimapRef extends SoftReference<Bitmap> {
		private String _key = "";

		public BtimapRef(Bitmap bmp, ReferenceQueue<Bitmap> q, String key) {
			super(bmp, q);
			_key = key;
		}
	}

	private BitmapCache() {
		bitmapRefs = new Hashtable<String, BtimapRef>();
		q = new ReferenceQueue<Bitmap>();

	}

	/**
	 * ȡ�û�����ʵ��
	 */
	public static BitmapCache getInstance() {
		if (cache == null) {
			cache = new BitmapCache();
		}
		return cache;

	}

	/**
	 * �������õķ�ʽ��һ��Bitmap�����ʵ���������ò����������
	 */
	private void addCacheBitmap(Bitmap bmp, String key) {
		cleanCache();// �����������
		BtimapRef ref = new BtimapRef(bmp, q, key);
		bitmapRefs.put(key, ref);
	}

	/**
	 * ������ָ�����ļ�����ȡͼƬ
	 */
	public Bitmap getBitmap(String filename) {

		Bitmap bitmapImage = null;
		// �������Ƿ��и�Bitmapʵ���������ã�����У�����������ȡ�á�
		if (bitmapRefs.containsKey(filename)) {
			BtimapRef ref = (BtimapRef) bitmapRefs.get(filename);
			bitmapImage = (Bitmap) ref.get();
		}
		// ���û�������ã����ߴ��������еõ���ʵ����null�����¹���һ��ʵ����
		// �����������½�ʵ����������
		if (bitmapImage == null) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inTempStorage = new byte[16 * 1024];

			// bitmapImage = BitmapFactory.decodeFile(filename, options);
			BufferedInputStream buf = null;
			InputStream is = null;
			try {
				is = new FileInputStream(filename);
				buf = new BufferedInputStream(is);
				bitmapImage = BitmapFactory.decodeStream(buf);
				this.addCacheBitmap(bitmapImage, filename);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(is != null) {
					try {
						is.close();
						is = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(buf != null) {
					try {
						buf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

		return bitmapImage;
	}

	private void cleanCache() {
		BtimapRef ref = null;
		while ((ref = (BtimapRef) q.poll()) != null) {
			bitmapRefs.remove(ref._key);
		}
	}

	// ���Cache�ڵ�ȫ�����ݮ�
	public void clearCache() {
		cleanCache();
		bitmapRefs.clear();
		System.gc();
		System.runFinalization();
	}

}
