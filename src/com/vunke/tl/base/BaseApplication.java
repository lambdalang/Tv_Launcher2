package com.vunke.tl.base;

import com.lzy.okhttputils.OkHttpUtils;

import android.app.Application;

public class BaseApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		OkHttpUtils.init(this);
	}
}
