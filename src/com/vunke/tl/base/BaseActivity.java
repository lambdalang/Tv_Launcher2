package com.vunke.tl.base;

import android.app.Activity;
import android.os.Bundle;

import com.vunke.tl.util.AppManager;

public class BaseActivity extends Activity {
    protected BaseActivity mcontext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mcontext = this;
        AppManager.getAppManager().addActivity(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 结束Activity&从堆栈中移除
        AppManager.getAppManager().finishActivity(this);
    }
}
