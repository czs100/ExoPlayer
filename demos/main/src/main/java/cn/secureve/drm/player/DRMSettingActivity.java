package cn.secureve.drm.player;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.demo.R;
import com.google.android.material.chip.Chip;

import cn.secureve.easyplay.JWTConfig;
import cn.secureve.easyplay.ServiceManager;

public class DRMSettingActivity extends AppCompatActivity {
    private final int MESSAGE_JWT_RESULT = 0;

    private SharedPreferences drmPreferences;

    @SuppressLint("HandlerLeak")
    private Handler messageHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_JWT_RESULT:{
                    if(0 == msg.arg1){
                        Toast.makeText(getBaseContext(), "成功获取JWT", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getBaseContext(), "获取JWT失败", Toast.LENGTH_SHORT).show();

                    }
                    break;
                }
                default:{
                    break;
                }

            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drm_setting_activity);

        //Load preference.
        drmPreferences = getSharedPreferences("drm_setting", MODE_PRIVATE);
        if(drmPreferences.getBoolean("jwt_unset", true)){
                JWTConfig jwtcfg = ServiceManager.ReadJWTConfig(getBaseContext());
                SharedPreferences.Editor editor = drmPreferences.edit();
                editor.putString("jwt_drm_server_url", jwtcfg.getJwt_drm_server_url());
                editor.putString("jwt_user_name", jwtcfg.getJwt_user_name());
                editor.putString("jwt_user_password", jwtcfg.getJwt_user_password());
                editor.putBoolean("jwt_unset", false);
                editor.commit();
        }

        if(null != drmPreferences ){

            EditText editTextJwtServerUrl = (EditText)findViewById(R.id.editTextJwtServerUrl);
            EditText editTextJwtUserName = (EditText)findViewById(R.id.editTextJwtUserName);
            EditText editTextJwtUserPassword = (EditText)findViewById(R.id.editTextJwtUserPassword);

            //读取配置中的参数。
            String jwtServerUrl = drmPreferences.getString("jwt_drm_server_url", null);
            String jwtUserName = drmPreferences.getString("jwt_user_name", null);
            String jwtUserPassword = drmPreferences.getString("jwt_user_password", null);

            //参数不为空则把值填充到控件里面。
            if(null != jwtServerUrl){
                editTextJwtServerUrl.setText(drmPreferences.getString("jwt_drm_server_url", null));
            }

            if(null != jwtUserName){
                editTextJwtUserName.setText(drmPreferences.getString("jwt_user_name", null));
            }

            if(null != jwtUserPassword){
                editTextJwtUserPassword.setText(drmPreferences.getString("jwt_user_password", null));
            }


            CheckBox chipVisibleWatermark = (CheckBox) findViewById(R.id.chkVisibleWatermark);
            chipVisibleWatermark.setChecked(drmPreferences.getBoolean("watermark_visible", false));
            chipVisibleWatermark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences.Editor editor = drmPreferences.edit();
                    editor.putBoolean("watermark_visible", isChecked);
                    editor.commit();
                }
            });
            ServiceManager.setGlobalPreference(drmPreferences);
        }

        Button buttonCreateJWT = (Button)findViewById(R.id.buttonCreateToken);
        buttonCreateJWT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get the input parameters.
                EditText editTextJwtServerUrl = (EditText)findViewById(R.id.editTextJwtServerUrl);
                EditText editTextJwtUserName = (EditText)findViewById(R.id.editTextJwtUserName);
                EditText editTextJwtUserPassword = (EditText)findViewById(R.id.editTextJwtUserPassword);


                String jwtServerUrl = editTextJwtServerUrl.getText().toString();
                String jwtUserName = editTextJwtUserName.getText().toString();
                String jwtUserPassword = editTextJwtUserPassword.getText().toString();


                //保存这些信息
                SharedPreferences.Editor editor = drmPreferences.edit();
                editor.putString("jwt_drm_server_url", jwtServerUrl);
                editor.putString("jwt_user_name", jwtUserName);
                editor.putString("jwt_user_password", jwtUserPassword);
                editor.commit();
                ServiceManager.setGlobalPreference(drmPreferences);

                //避免在UI线程访问网络。
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //发请求到服务器端获取JWT并把JWT缓存到内存中。
                        int result = ServiceManager.createJWT(jwtServerUrl, jwtUserName, jwtUserPassword);

                        Message message =  new Message();
                        message.what = MESSAGE_JWT_RESULT;
                        message.arg1 = result;
                        messageHandler.sendMessage(message);
                    }
                }).start();



            }
        });
    }
}
