package arionum.net.cubedpixels.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import arionum.net.cubedpixels.R;
import es.dmoral.toasty.Toasty;
import mehdi.sakout.fancybuttons.FancyButton;

public class PasswordView extends AppCompatActivity {

    private static PasswordCallback callback;

    public static void makePasswordPromt(Context context, PasswordCallback callback) {
        PasswordView.callback = callback;
        Intent i = new Intent(context, PasswordView.class);
        context.startActivity(i);
    }

    public static boolean hasPassword() {
        return !getString("identification_password").equals("");
    }

    private static String getString(String key) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(HomeView.instance);
        String value = sharedPref.getString(key, "");
        return value;
    }

    public static boolean isValidPassword(final String password) {

        return !TextUtils.isEmpty(password);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //TODO -> THEME PROCESSOR
        setTheme(((boolean) SettingsView.getSettingFromName("blacktheme").getValue()) ? R.style.DarkAppTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_layout);
        FancyButton goBackButton = findViewById(R.id.btn_go_back);
        FancyButton passwordButton = findViewById(R.id.btn_pw);

        final EditText pw_field = findViewById(R.id.password_text);
        final EditText second_pw_field = findViewById(R.id.password_text_second);


        if (hasPassword()) {
            pw_field.setHint("Password");
            passwordButton.setText("Enter Password");
        } else {
            second_pw_field.setVisibility(View.VISIBLE);
            pw_field.setHint("Repeat Password");
            passwordButton.setText("Save Password");
        }

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.verification_done(false);
                finish();
            }
        });

        passwordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasPassword())
                    if (pw_field.getText().toString().length() > 3 && isValidPassword(pw_field.getText().toString())) {

                        if (!second_pw_field.getText().toString().equals(pw_field.getText().toString())) {
                            Toasty.error(PasswordView.this, "Passwords doesn't match!", Toast.LENGTH_SHORT, true).show();
                            return;
                        }
                        saveString("identification_password", pw_field.getText().toString());
                        Toasty.info(PasswordView.this, "Password has been saved.", Toast.LENGTH_SHORT, true).show();
                        callback.verification_done(true);
                        finish();
                    } else {
                        System.out.println(isValidPassword(pw_field.getText().toString()));
                        pw_field.setText("");
                        Toasty.error(PasswordView.this, "Password invalid!VALID: > 3 & a-z0-9@#$%^&+=!", Toast.LENGTH_SHORT, true).show();
                        pw_field.setHint("PW INVALID! try again");
                    }
                else {
                    //TODO -> LOGIN WITH PW
                    if (pw_field.getText().toString().equalsIgnoreCase(getString("identification_password"))) {
                        Toasty.success(PasswordView.this, "Verification successful!", Toast.LENGTH_SHORT, true).show();
                        callback.verification_done(true);
                        finish();
                    } else {
                        pw_field.setText("");
                        Toasty.error(PasswordView.this, "Password didn't match!", Toast.LENGTH_SHORT, true).show();
                        pw_field.setHint("PW INVALID! try again");
                    }
                }
            }
        });
    }

    public void saveString(String key, String string) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, string);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        return;
    }


    public static abstract class PasswordCallback {
        public abstract void verification_done(boolean accepted);
    }
}
