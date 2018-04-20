package arionum.net.cubedpixels.views;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;

public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideBackButton();
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorBlue)
                .buttonsColor(R.color.colorDark)
                .image(R.drawable.avd_anim_white)
                .title("The Arionum Wallet")
                .description("- Manage your Arionum on the go\n" +
                        "- Send and receive ARO's by using QR codes"
                        + "\n- Receive transaction notifications")
                .build());

        addSlide(new SlideFragmentBuilder()
                        .backgroundColor(R.color.colorBlue)
                        .buttonsColor(R.color.colorDark)
                        .neededPermissions(new String[]{Manifest.permission.CAMERA})
                        .image(R.drawable.avd_anim_white)
                        .title("Setup")
                        .description("To import existing ARO wallets, the app requires permission to the camera to scan QR codes.")
                        .build(),
                new MessageButtonBehaviour(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }, "Permission Granted"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }


    }

    @Override
    public void onFinish() {
        super.onFinish();
        Intent i = new Intent(this, IntroViewActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        return;
    }

    public static class PreIntroAcitivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.pre_intro_view);
            ImageView i = findViewById(R.id.preintroimage);
            final Animation a = new TranslateAnimation(0, 0, 0, 20);
            a.setStartOffset(0);
            a.setRepeatCount(Animation.INFINITE);
            a.setDuration(1200);
            a.setInterpolator(new LinearInterpolator());
            a.setRepeatMode(Animation.REVERSE);

            final Animation b = new TranslateAnimation(0, 0, 0, 20);
            b.setStartOffset(400);
            b.setRepeatCount(Animation.INFINITE);
            b.setDuration(1200);
            b.setInterpolator(new LinearInterpolator());
            b.setRepeatMode(Animation.REVERSE);

            i.setAnimation(b);
            findViewById(R.id.btn_startup).setAnimation(a);


            findViewById(R.id.btn_startup).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(PreIntroAcitivity.this, IntroActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            });
        }

        @Override
        public void onBackPressed() {
            return;
        }
    }

    public static class IntroViewActivity extends AppCompatActivity {
        public void switchView() {
            boolean visible = findViewById(R.id.button_layout).getVisibility() == View.VISIBLE;
            if (visible) {
                findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.button_layout).setVisibility(View.GONE);
            } else {
                findViewById(R.id.login_layout).setVisibility(View.GONE);
                findViewById(R.id.button_layout).setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.intro_view);

            findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchView();
                }
            });

            findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchView();
                }
            });

            findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(IntroViewActivity.this, QRview.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            });

            findViewById(R.id.btn_create).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.introtop), "This function is not available!", Snackbar.LENGTH_SHORT);
                    mySnackbar.show();
                }
            });

            findViewById(R.id.btn_createNOW).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText address = findViewById(R.id.address_text);
                    EditText publickey = findViewById(R.id.public_text);
                    EditText privatekey = findViewById(R.id.private_text);
                    saveString("address", address.getText().toString());
                    saveString("publickey", publickey.getText().toString());
                    saveString("privatekey", privatekey.getText().toString());
                    Intent i = new Intent(IntroViewActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
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
    }
}
