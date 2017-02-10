package com.zhonghong.scanner.client;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class MediaScannerClient {
	private static final String TAG = "MediaScannerClient";
	
	private static final String AUTHORITY = "com.zhonghong.scanner.provider";
	
	private static final Uri MEDIA_URI = Uri.parse("content://" + AUTHORITY + "/media");	
	private static final Uri SCAN_STATE_URI = Uri.parse("content://" + AUTHORITY + "/scanstate");
	
	private static final int MEDIA_TYPE_AUDIO = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	private static final int MEDIA_TYPE_IMAGE = 3;
	
	public static final int MEDIA_TYPE_AUDIO_FLAG = 0x01;
	public static final int MEDIA_TYPE_VIDEO_FLAG = 0x02;
	public static final int MEDIA_TYPE_IMAGE_FLAG = 0x04;
	public static final int MEDIA_TYPE_ALL_FLAG = MEDIA_TYPE_AUDIO_FLAG | MEDIA_TYPE_VIDEO_FLAG | MEDIA_TYPE_IMAGE_FLAG;
	
	private static MediaScannerClient sMediaScannerClient;
	
	private Context mContext;
	private ScanStateObserver mScanStateObserver;
	private List<IScanListener> mScanListeners = new ArrayList<IScanListener>();	
	private int mMediaTypeFlag = MEDIA_TYPE_AUDIO_FLAG;		
	
	private MediaScannerClient(Context context) {
		mContext = context;
		
		mScanStateObserver = new ScanStateObserver(mContext, null);
		mScanStateObserver.startObserve();
	}
	
	public static MediaScannerClient getInstance(Context context) {
		synchronized (MediaScannerClient.class) {
			if (sMediaScannerClient == null) {
				sMediaScannerClient = new MediaScannerClient(context);
			}
			
			return sMediaScannerClient;
		}
	}
	
	public void destroy() {
		mScanStateObserver.stopObserve();
		sMediaScannerClient = null;
	}
	
	public void setMediaTypeFlag(int flag) {
		mMediaTypeFlag = flag;
	}
	
	/**
	 * 是否扫描中
	 */
	public boolean isScanning() {
		return mScanStateObserver.isScanning();
	}
	
	/**
	 * 获取媒体信息
	 */
	public ArrayList<String> getMediaInfo() {
		Cursor cursor = null;
		
		if ((mMediaTypeFlag & MEDIA_TYPE_ALL_FLAG) == MEDIA_TYPE_ALL_FLAG) {	// 查询所有
			cursor = mContext.getContentResolver().query(MEDIA_URI, null, null, null, null);
		} else {
			String selection = null;
			String args = "";
			
			if ((mMediaTypeFlag & MEDIA_TYPE_AUDIO_FLAG) == MEDIA_TYPE_AUDIO_FLAG) {
				selection = "type = ?";
				args += MEDIA_TYPE_AUDIO + "#";
			}
			if ((mMediaTypeFlag & MEDIA_TYPE_VIDEO_FLAG) == MEDIA_TYPE_VIDEO_FLAG) {
				selection = (selection == null) ? "type = ?" : selection + " or type = ?";
				args += MEDIA_TYPE_VIDEO + "#";
			}
			if ((mMediaTypeFlag & MEDIA_TYPE_IMAGE_FLAG) == MEDIA_TYPE_IMAGE_FLAG) {
				selection = (selection == null) ? "type = ?" : selection + " or type = ?";
				args += MEDIA_TYPE_IMAGE;
			}
			
			String[] selectionArgs = args.isEmpty() ? null : args.split("#");
			cursor = mContext.getContentResolver().query(MEDIA_URI, null, selection, selectionArgs, null);
		}
		
		if (cursor != null) {
			ArrayList<String> infos = new ArrayList<String>();
			final int index = cursor.getColumnIndex("path");
			
			while (cursor.moveToNext()) {
				infos.add(cursor.getString(index));
			}
			
			cursor.close();
			return infos;
		}
		
		return null;
	}
	
	/**
	 * 全盘扫描
	 */
	public void scanAll() {
		mContext.sendBroadcast(new Intent("zhonghong.intent.action.MEDIA_SCANNER_SCAN_ALL"));
	}
	
	/**
	 * 监听扫描状态
	 */
	private final class ScanStateObserver extends ContentObserver {
		private Context mContext;
		
		public ScanStateObserver(Context context, Handler handler) {
			super(handler);			
			mContext = context;
		}
		
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			Log.i(TAG, "onChange");
			if (SCAN_STATE_URI.equals(uri)) {
				if (isScanning()) {
					notifyScanBegin();
				} else {
					notifyScanEnd();
				}
			}
		}
		
		public boolean isScanning() {
			int state = 0;
			Cursor cursor = mContext.getContentResolver().query(SCAN_STATE_URI, null, null, null, null);
			
			if (cursor != null) {
				if (cursor.moveToNext()) {
					state = cursor.getInt(cursor.getColumnIndex("state"));
				}
				cursor.close();
			}
			
			return state == 1;
		}
		
		public void startObserve() {
			Log.i(TAG, "startObserve");
			mContext.getContentResolver().registerContentObserver(SCAN_STATE_URI, false, this);
		}
		
		public void stopObserve() {
			Log.i(TAG, "stopObserve");
			mContext.getContentResolver().unregisterContentObserver(this);
		}
	}
	
	public void registerScanListener(IScanListener listener) {
		Log.i(TAG, "registerScanListener listener: " + listener);
		synchronized (mScanListeners) {
			if ((listener != null) && !mScanListeners.contains(listener)) {
				mScanListeners.add(listener);
			}
		}
	}

	public void unregisterScanListener(IScanListener listener) {
		Log.i(TAG, "unregisterScanListener listener: " + listener);
		synchronized (mScanListeners) {
			if (mScanListeners.contains(listener)) {
				mScanListeners.remove(listener);
			}
		}
	}
	
	private void notifyScanBegin() {
		Log.i(TAG, "notifyScanBegin");
		synchronized (mScanListeners) {
			for (IScanListener sl : mScanListeners) {
				sl.onScanBegin();
			}
		}
	}
	
	private void notifyScanEnd() {
		Log.i(TAG, "notifyScanEnd");
		synchronized (mScanListeners) {
			for (IScanListener sl : mScanListeners) {
				sl.onScanEnd();
			}
		}
	}

	public interface IScanListener {
		void onScanBegin();
		void onScanEnd();
	}
}
