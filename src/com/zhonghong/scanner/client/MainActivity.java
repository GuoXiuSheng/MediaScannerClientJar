package com.zhonghong.scanner.client;

import java.util.ArrayList;

import com.zhonghong.scanner.client.MediaScannerClient.IScanListener;
import com.zhonghong.scanner.client.R;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
	private static final String TAG = "MediaScannerClient";
	
	private MediaScannerClient mMediaScannerClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMediaScannerClient = MediaScannerClient.getInstance(this);
		mMediaScannerClient.setMediaTypeFlag(MediaScannerClient.MEDIA_TYPE_AUDIO_FLAG);
		mMediaScannerClient.registerScanListener(mScanListener);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		mMediaScannerClient.unregisterScanListener(mScanListener);
		mMediaScannerClient.destroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.scan_all:			// 全盘扫描
			mMediaScannerClient.scanAll();
			break;
		case R.id.get_mediainfo:	// 获取媒体信息
			printMediaInfo();
			break;
		}
	}
	
	private IScanListener mScanListener = new IScanListener() {
		@Override
		public void onScanBegin() {
			Log.i(TAG, "onScanBegin");
		}
		
		@Override
		public void onScanEnd() {
			Log.i(TAG, "onScanEnd");
			printMediaInfo();
		}
	};
	
	private void printMediaInfo() {
		ArrayList<String> infos = mMediaScannerClient.getMediaInfo();
		if (infos != null) {
			int i = 0;
			for (String s : infos) {
				Log.i(TAG, "" + (i++) + ": " + s);
			}
		}
	}
}
