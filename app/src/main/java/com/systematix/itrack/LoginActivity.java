package com.systematix.itrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.android.volley.VolleyError;
import com.systematix.itrack.config.PreferencesList;
import com.systematix.itrack.config.UrlsConfig;
import com.systematix.itrack.items.User;
import com.systematix.itrack.utils.Api;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements Api.OnRespondListener {

    @BindView(R.id.login_btn_login) Button btnLogin;
    @BindView(R.id.login_progress_bar) ProgressBar progressBar;
    @BindView(R.id.login_txt_username) TextInputEditText txtUsername;
    @BindView(R.id.login_txt_password) TextInputEditText txtPassword;
    @BindView(R.id.login_txt_username_container) TextInputLayout txtUsernameContainer;
    @BindView(R.id.login_txt_password_container) TextInputLayout txtPasswordContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (this.checkForUser()) {
            // do not continue below
            return;
        }

        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        // hide loading first
        progressBar.setVisibility(View.GONE);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doLoading(true);

                final String username = txtUsername.getText().toString();
                final String password = txtPassword.getText().toString();
                final JSONObject params = new JSONObject();

                try {
                    params.put("username", username);
                    params.put("password", password);

                    Api.post(view.getContext())
                        .setTag("login")
                        .setUrl(UrlsConfig.LOGIN_URL)
                        .request(params);
                } catch (JSONException e) {
                    Snackbar.make(view, R.string.error, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        checkForLogOutMsg();
    }

    private boolean checkForUser() {
        final SharedPreferences sharedPreferences = getSharedPreferences(PreferencesList.PREF_LOGIN, MODE_PRIVATE);
        int uid = sharedPreferences.getInt(PreferencesList.PREF_USER_ID, -1);

        // if no id set, do nothing
        if (uid == -1) {
            return false;
        }

        // go to next activity here
        final Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        return true;
    }

    private void checkForLogOutMsg() {
        final SharedPreferences sharedPreferences = getSharedPreferences(PreferencesList.PREF_LOGIN, MODE_PRIVATE);
        boolean didLogOut = sharedPreferences.getBoolean(PreferencesList.PREF_DID_LOG_OUT, false);
        if (didLogOut) {
            Snackbar.make(btnLogin, R.string.msg_log_out, Snackbar.LENGTH_LONG).show();
        }
        // remove
        sharedPreferences.edit().remove(PreferencesList.PREF_DID_LOG_OUT).apply();
    }

    private void doLoading(boolean loading) {
        // disable input fields and button
        boolean enable = !loading;
        txtUsernameContainer.setEnabled(enable);
        txtPasswordContainer.setEnabled(enable);
        btnLogin.setEnabled(enable);
        progressBar.setVisibility(enable ? View.GONE : View.VISIBLE);

        // if loading, remove errors
        if (loading) {
            btnLogin.setText(R.string.loading_text);
            txtUsernameContainer.setError(null);
            txtPasswordContainer.setError(null);
        } else {
            btnLogin.setText(R.string.login_btn_login);
        }
    }


    // OnRespondListener
    @Override
    public void onResponse(String tag, JSONObject response) throws JSONException {
        // do not unload hehe

        // check if successful
        if (!Api.isSuccessful(response)) {
            final String msg = response.getString("msg");
            if (msg != null && msg.length() > 0) {
                txtUsernameContainer.setError("");
                txtPasswordContainer.setError(msg);
                Snackbar.make(btnLogin, msg, Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        // get user
        JSONObject jsonUser = response.getJSONObject("user");
        final User user = new User(jsonUser);

        // save user to sharedpref
        final SharedPreferences sharedPreferences = getSharedPreferences(PreferencesList.PREF_LOGIN, MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(PreferencesList.PREF_USER_ID, user.getId());
        editor.putString(PreferencesList.PREF_USER_JSON, jsonUser.toString());
        editor.putBoolean(PreferencesList.PREF_DID_LOG_IN, true);
        editor.apply();

        this.checkForUser();
    }

    @Override
    public void onErrorResponse(String tag, VolleyError error) {
        doLoading(false);
        Snackbar.make(btnLogin, R.string.error, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onException(JSONException e) {
        doLoading(false);
        Snackbar.make(btnLogin, R.string.error, Snackbar.LENGTH_LONG).show();
    }
}
