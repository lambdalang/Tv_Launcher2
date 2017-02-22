package com.kxy.tl.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kxy.tl.R;
import com.vunke.tl.auth.Auth;
import com.vunke.tl.base.BaseActivity;

public class AuthActivity extends BaseActivity implements View.OnClickListener{
    private Button auth_confirm;
    private TextView auth_errText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        init();
        initListener();
    }
    private void init(){
        auth_confirm = (Button)findViewById(R.id.auth_confirm);
        auth_errText = (TextView)findViewById(R.id.auth_errText);
        auth_confirm.requestFocus();
        String ErrorCode =  Auth.getAuthErrCode(getApplicationContext(),"1002008");
        if (ErrorCode.equals("1002008")){
            auth_errText.setText("数据请求超时，请检测网络后再试");
        }else{
            auth_errText.setText("用户认证失败 ["+ErrorCode+"]");
        }
    }
    private void initListener(){
        auth_confirm.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.auth_confirm){
            finish();
        }
    }
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	//Auth.RemoveAuthCode(getApplicationContext());
        Auth.RemoveAuthErrCode(getApplicationContext());
    }
}
