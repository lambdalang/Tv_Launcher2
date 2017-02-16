package com.vunke.tl.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kxy.tl.download.DownLoadService;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.util.LogUtil;

public class ConnectionChangeReceiver extends BroadcastReceiver {
	private static final String TAG = ConnectionChangeReceiver.class.getSimpleName();
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!UIUtil.isNetworkAvailable(context)) {
			LogUtil.i("tv_launcher", "网络未连接 ----------");
		}

		if (!UIUtil.isNetworkConnected(context)) {
			LogUtil.i("tv_launcher", "网络还未连接----------");
		}
		if (UIUtil.isNetConnected(context)) {
			LogUtil.i("tv_launcher", "网络已连接----------");
			Intent it = new Intent(context,DownLoadService.class);
			context.startService(it);
		}
	}

}










