package arionum.net.cubedpixels.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.HashMap;

import arionum.net.cubedpixels.MainActivity;
import arionum.net.cubedpixels.R;
import mehdi.sakout.fancybuttons.FancyButton;

public class SettingsView extends AppCompatActivity {

    private static HashMap<String, Setting> settings = new HashMap<>();
    private static SettingsView instance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //TODO -> THEME PROCESSOR
        setTheme(((boolean) SettingsView.getSettingFromName("blacktheme").getValue()) ? R.style.DarkAppTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        instance = this;
        initInteracts();
    }

    public static void registerSettings(Context c) {
        //TODO -> BLACK THEME
        settings.put("blacktheme", new Setting("blacktheme", getSavedBoolean("setting_blacktheme", c), new SettingToogleEvent() {
            @Override
            public void toogleSetting(boolean val) {
                new MaterialDialog.Builder(instance).title("Please restart the app for any changes").positiveText("Okay").show();
            }
        }));

        //TODO -> DEBUG MODE
        settings.put("debug", new Setting("debug", getSavedBoolean("setting_debug", c), new SettingToogleEvent() {
            @Override
            public void toogleSetting(boolean val) {
                if (val)
                    new MaterialDialog.Builder(instance).title("Debug mode has been activated!").positiveText("Okay").show();
            }
        }));
    }


    public void initInteracts() {
        //TODO -> MANAGE BUTTON BACK CLICK
        FancyButton fb = findViewById(R.id.gobackbutton);
        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //TODO -> SETTING 1 (BLACK THEME)
        Switch switch1 = findViewById(R.id.setting_check_one);
        switch1.setChecked(getSettingFromName("blacktheme").getValueAsBoolean());
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveSettingBoolean("setting_blacktheme", b, SettingsView.this);
                getSettingFromName("blacktheme").setValue(b);
                getSettingFromName("blacktheme").getEvent().toogleSetting(b);
            }
        });

        //TODO -> SETTING 2 (DEBUG MODE)
        Switch switch2 = findViewById(R.id.setting_check_two);
        switch2.setChecked(getSettingFromName("debug").getValueAsBoolean());
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                saveSettingBoolean("setting_debug", b, SettingsView.this);
                getSettingFromName("debug").setValue(b);
                getSettingFromName("debug").getEvent().toogleSetting(b);
            }
        });
    }


    private static boolean getSavedBoolean(String under, Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        boolean value = sharedPref.getBoolean(under, false);
        return value;
    }

    private static String getSavedSetting(String under, Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        String value = sharedPref.getString(under, "");
        return value;
    }

    private static void saveSettingString(String under, String content, Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(under, content);
        editor.commit();
    }

    private static void saveSettingBoolean(String under, boolean content, Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(under, content);
        editor.commit();
    }

    public static Setting getSettingFromName(String name) {
        if (settings.isEmpty())
            registerSettings(MainActivity.getInstance());
        return settings.get(name);
    }


    public static class Setting {
        private String name;
        private Object value;
        private SettingToogleEvent event;


        public Setting(String name, Object value, SettingToogleEvent event) {
            this.name = name;
            this.value = value;
            this.event = event;
        }

        public SettingToogleEvent getEvent() {
            return event;
        }

        public String getName() {
            return name;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean getValueAsBoolean() {
            return (boolean) value;
        }

        public String getValueAsString() {
            return (String) value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static abstract class SettingToogleEvent {
        public abstract void toogleSetting(boolean val);
    }
}
