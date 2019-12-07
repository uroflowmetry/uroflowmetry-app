package com.uroflowmetry.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.uroflowmetry.App;
import com.uroflowmetry.R;
import com.uroflowmetry.api.ApiClient;
import com.uroflowmetry.api.ApiInterface;
import com.uroflowmetry.models.AppStorage;
import com.uroflowmetry.models.BaseResult;
import com.uroflowmetry.models.DataModel;
import com.uroflowmetry.models.ResultLogin;
import retrofit2.Call;
import retrofit2.Callback;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    String m_strId, m_strPassword;
    int m_nIdx = 0;

    ApiInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        initView();
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
    }

    private void initView(){
        EditText editEmailView =  findViewById(R.id.editEmail);
        EditText editPasswordView =  findViewById(R.id.editPassword);
        editEmailView.setText(App.getUserId());
        editPasswordView.setText(App.getPassword());

        findViewById(R.id.btnLogin).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkValidation())
                    return;
                procLogin();
            }
        });


        findViewById(R.id.ripTxtGotoSignup).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
                finish();

            }
        });

        TextView textView = (TextView)findViewById(R.id.txtHavenotAccount);
        textView.setPaintFlags(textView.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);

    }



    private boolean checkValidation() {
        EditText editEmailView =  findViewById(R.id.editEmail);
        EditText editPasswordView =  findViewById(R.id.editPassword);

        editEmailView.setError(null);
        editPasswordView.setError(null);

        m_strId = editEmailView.getText().toString();
        m_strPassword = editPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(m_strId)) {
            editEmailView.setError(getString(R.string.error_field_required));
            focusView = editEmailView;
            cancel = true;
        }
        if (TextUtils.isEmpty(m_strPassword)) {
            editPasswordView.setError(getString(R.string.error_field_required));
            focusView = editPasswordView;
            cancel = true;
        }
        if (cancel) {
            focusView.requestFocus();
            return false;
        }
        return true;
    }



    private void procLogin(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userId", m_strId);
        jsonObject.addProperty("password", m_strPassword);

        final ProgressDialog m_dlgWait = ProgressDialog.show(LoginActivity.this, null, null);
        m_dlgWait.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        m_dlgWait.setContentView(R.layout.dlg_loader);

        Call<ResultLogin> call = apiInterface.doLogin(jsonObject);
        call.enqueue(new Callback<ResultLogin>() {
            @Override
            public void onResponse(Call<ResultLogin> call, retrofit2.Response<ResultLogin> response) {
                m_dlgWait.dismiss();
                ResultLogin resultLogin = response.body();
                if (resultLogin.getStatus() > 0) {
                    App.setUserId(m_strId);
                    App.setPassword(m_strPassword);
                    m_nIdx = resultLogin.getUseridx();

                    procSendData();

                } else {
                    Toast.makeText(LoginActivity.this, R.string.msg_err_login_failed, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<ResultLogin> call, Throwable t) {
                m_dlgWait.dismiss();
                Toast.makeText(LoginActivity.this, R.string.msg_err_request, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void procSendData(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("userIdx", m_nIdx);
        jsonObject.addProperty("email", AppStorage.Companion.build(this).getProfile().getMyEmail());
        jsonObject.addProperty("doctoremail", AppStorage.Companion.build(this).getProfile().getDoctorEmail());
        jsonObject.addProperty("volume", 500);

        JsonArray jsonArrayData = new JsonArray();
        List<DataModel> data = AppStorage.Companion.build(this).getSavedDataModel();
        if (data != null){
            for (DataModel dataModel : data){
                JsonObject jsonData = new JsonObject();
                jsonData.addProperty("checktime", dataModel.getStartedTimeString());
                jsonData.addProperty("measuredvolume", dataModel.getVoidedVolume());
                jsonData.addProperty("measuredduration", dataModel.getMeasuredTimeSeconds());
                jsonData.addProperty("flowrate", dataModel.getFlowRateF());
                String strTimeValue =  dataModel.getValues().toString();
                strTimeValue = strTimeValue.replace("[", "0.0, ");
                strTimeValue = strTimeValue.replace("]", " ,0.0");
                jsonData.addProperty("timedistance",  strTimeValue);
                jsonArrayData.add(jsonData);
            }
        }
        jsonObject.add("data", jsonArrayData);

        final ProgressDialog m_dlgWait = ProgressDialog.show(LoginActivity.this, null, null);
        m_dlgWait.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        m_dlgWait.setContentView(R.layout.dlg_loader);

        Call<BaseResult> call = apiInterface.doSendData(jsonObject);
        call.enqueue(new Callback<BaseResult>() {
            @Override
            public void onResponse(Call<BaseResult> call, retrofit2.Response<BaseResult> response) {
                m_dlgWait.dismiss();
                BaseResult baseResult = response.body();
                if (baseResult.getStatus() > 0) {
                    Toast.makeText(LoginActivity.this, R.string.msg_success_register, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.msg_err_login_failed, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<BaseResult> call, Throwable t) {
                m_dlgWait.dismiss();
                Toast.makeText(LoginActivity.this, R.string.msg_err_request, Toast.LENGTH_SHORT).show();
            }
        });
    }

}

