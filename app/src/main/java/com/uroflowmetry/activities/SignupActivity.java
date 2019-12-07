package com.uroflowmetry.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import com.uroflowmetry.App;
import com.uroflowmetry.R;
import com.uroflowmetry.api.ApiClient;
import com.uroflowmetry.api.ApiInterface;
import com.uroflowmetry.models.BaseResult;
import retrofit2.Call;
import retrofit2.Callback;

public class SignupActivity extends AppCompatActivity {
    public final String TAG = SignupActivity.class.getName();
    ApiInterface apiInterface;

    String m_strId, m_strPassword, m_strUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        initView();
        apiInterface = ApiClient.getClient().create(ApiInterface.class);
    }

    private void initView(){

        TextView textView = (TextView)findViewById(R.id.txtHaveAccount);
        textView.setPaintFlags(textView.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
        findViewById(R.id.txtHaveAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoLogin();
            }
        });

        findViewById(R.id.btnSignUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkValidation()){
                    procSignup();
                }
            }
        });

    }



    private void gotoLogin(){
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean checkValidation() {
        EditText editUseridView =  findViewById(R.id.editUserId);
        EditText editPasswordView =  findViewById(R.id.editPassword);
        EditText editUsernameView = findViewById(R.id.editUserName);
        EditText editRePasswordView = findViewById(R.id.editRePassword);


        editUseridView.setError(null);
        editPasswordView.setError(null);
        editUsernameView.setError(null);
        editRePasswordView.setError(null);

        m_strUserName = editUsernameView.getText().toString();
        m_strId = editUseridView.getText().toString();
        m_strPassword = editPasswordView.getText().toString();
        String strRePassword = editRePasswordView.getText().toString();

        if (TextUtils.isEmpty(m_strUserName)) {
            editUsernameView.setError(getString(R.string.error_field_required));
            editUsernameView.requestFocus();
            return  false;
        }
        if (TextUtils.isEmpty(m_strId)) {
            editUseridView.setError(getString(R.string.error_field_required));
            editUseridView.requestFocus();
            return  false;
        }
        if (TextUtils.isEmpty(m_strPassword)) {
            editPasswordView.setError(getString(R.string.error_field_required));
            editPasswordView.requestFocus();
            return  false;
        }
        if (m_strPassword.equals(strRePassword) == false) {
            editPasswordView.setError(getString(R.string.error_retype_password));
            editPasswordView.requestFocus();
            return  false;
        }
        return true;
    }

    private void procSignup (){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("fullname", m_strUserName);
        jsonObject.addProperty("userId", m_strId);
        jsonObject.addProperty("password", m_strPassword);

        final ProgressDialog m_dlgWait = ProgressDialog.show(SignupActivity.this, null, null);
        m_dlgWait.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        m_dlgWait.setContentView(R.layout.dlg_loader);

        Call<BaseResult> call = apiInterface.doSignup(jsonObject);
        call.enqueue(new Callback<BaseResult>() {
            @Override
            public void onResponse(Call<BaseResult> call, retrofit2.Response<BaseResult> response) {
                m_dlgWait.dismiss();
                BaseResult resultSignup = response.body();
                if (resultSignup.getStatus() > 0) {
                    Toast.makeText(SignupActivity.this, R.string.msg_success_register, Toast.LENGTH_SHORT).show();
                    App.setUserId(m_strId);
                    App.setPassword(m_strPassword);

                    gotoLogin();
                } else {
                    Toast.makeText(SignupActivity.this, resultSignup.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<BaseResult> call, Throwable t) {
                m_dlgWait.dismiss();
                Toast.makeText(SignupActivity.this, R.string.msg_err_request, Toast.LENGTH_SHORT).show();
            }
        });
    }



}
