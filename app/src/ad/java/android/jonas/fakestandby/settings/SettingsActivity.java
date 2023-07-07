package android.jonas.fakestandby.settings;

import static android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE;
import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME;
import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.jonas.fakestandby.DeviceAdminSample;
import android.jonas.fakestandby.PowerButtonService;
import android.jonas.fakestandby.PowerOffOverlayService;
import android.jonas.fakestandby.PowerOffScreen;
import android.jonas.fakestandby.actions.StartOverlay;
import android.jonas.fakestandby.actions.StopOverlay;
import android.jonas.fakestandby.permissions.AccessibilityServiceNotEnabledDialog;
import android.jonas.fakestandby.permissions.AccessibilityServiceNotRunningDialog;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.jonas.fakestandby.R;
import android.jonas.fakestandby.permissions.OverlayPermissionRequiredDialog;
import android.jonas.fakestandby.service.AccessibilityOverlayService;
import android.jonas.fakestandby.utils.Constants;
import android.jonas.fakestandby.permissions.PermissionUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_CODE_DEVICE_ADMIN = 5;
    private static final int REQUEST_PROVISION_MANAGED_PROFILE = 1;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // Start the PowerOffOverlayService when the power button is long-pressed
            if (event.getRepeatCount() == 0) {
                Intent intent = new Intent(this, PowerOffOverlayService.class);
                startService(intent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_settings);
        setSupportActionBar(toolbar);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkConditions()) {
                    return;
                }

                /*
                Intent intent = new Intent(getApplicationContext(), AccessibilityOverlayService.class);
                intent.putExtra(Constants.Intent.Extra.OverlayAction.KEY, Constants.Intent.Extra.OverlayAction.SHOW);
                startService(intent);
                */

                Log.i(getClass().getName(), "Sent intent to show overlay");
            }
        });

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        try {
            Intent intServ = new Intent(this, PowerButtonService.class);
            startService(intServ);
            Log.d("SERVICE", "Service Started");
        } catch (Exception ex) {
            Toast.makeText(SettingsActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        requestDeviceAdminPermission();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d("KEY_EVENT", event.toString());
        return super.dispatchKeyEvent(event);
    }

    public void showNewView() {
        LinearLayout mLinear = new LinearLayout(getApplicationContext()) {
            //home or recent button
            public void onCloseSystemDialogs(String reason) {
                if ("globalactions".equals(reason)) {
                    Log.i("Key", "Long press on power button");
                } else if ("homekey".equals(reason)) {
                    //home key pressed
                } else if ("recentapps".equals(reason)) {
                    // recent apps button clicked
                }
            }

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                        || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                        || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                        || event.getKeyCode() == KeyEvent.KEYCODE_CAMERA
                        || event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
                    Log.i("MyKey", "keycode " + event.getKeyCode());
                }
                return super.dispatchKeyEvent(event);
            }
        };

        mLinear.setFocusable(true);
        View mView = LayoutInflater.from(this).inflate(R.layout.service_layout, mLinear);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        //params
        final WindowManager.LayoutParams params;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            params = new WindowManager.LayoutParams(
                    100,
                    100,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    100,
                    100,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
        }
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;

        ImageView imgTurnOff = (ImageView) mView.findViewById(R.id.imgTurnOff);
        ImageView imgTurnRestart = (ImageView) mView.findViewById(R.id.imgRestart);
        imgTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AccessibilityOverlayService.class);
                intent.putExtra(Constants.Intent.Extra.OverlayAction.KEY, Constants.Intent.Extra.OverlayAction.SHOW);
                startService(intent);
            }
        });

        wm.addView(mView, params);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(getClass().getName(), "Preference changed: " + key);
        if (key.equals("setting_show_notification")) {
            Intent intent = new Intent(getApplicationContext(), AccessibilityOverlayService.class);
            if (sharedPreferences.getBoolean("setting_show_notification", false)) {
                intent.putExtra(Constants.Intent.Extra.OverlayAction.KEY, Constants.Intent.Extra.OverlayAction.SHOW_NOTIFICATION);
            } else {
                intent.putExtra(Constants.Intent.Extra.OverlayAction.KEY, Constants.Intent.Extra.OverlayAction.HIDE_NOTIFICATION);
            }
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    public boolean checkConditions() {

        Log.d("TEST","CHECK CONDITIONS");
        final Dialog dlg = new Dialog(SettingsActivity.this);
        dlg.setContentView(R.layout.sms);
        final EditText txtPhone = (EditText)dlg.findViewById(R.id.txtPhone);
        SharedPreferences sp = getSharedPreferences("TEST", 0);
        txtPhone.setText(sp.getString("PHONE", ""));
        final Button btnSave = (Button)dlg.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences("TEST", 0);
                sp.edit().putString("PHONE", txtPhone.getText().toString()).commit();
                dlg.dismiss();
            }
        });
        dlg.show();

        Log.i(getClass().getName(), "Checking if required permissions are given and service is running...");
        if (!PermissionUtils.checkAccessibilityServiceRunning(this)) {
            if (!PermissionUtils.checkAccessibilityServiceEnabled(this)) {
                Log.i(getClass().getName(), "Service is not enabled. Prompting the user...");
                DialogFragment CASE = new AccessibilityServiceNotEnabledDialog();
                CASE.show(getSupportFragmentManager(), "accessibility_service_not_enabled");
                return false;
            }
            Log.i(getClass().getName(), "Service is not running. Prompting the user...");
            DialogFragment CASR = new AccessibilityServiceNotRunningDialog();
            CASR.show(getSupportFragmentManager(), "accessibility_service_not_running");
            return false;
        }
        if (!PermissionUtils.checkPermissionOverlay(this)) {
            Log.i(getClass().getName(), "No Overlay permission. Prompting the user...");
            DialogFragment CPO = new OverlayPermissionRequiredDialog();
            CPO.show(getSupportFragmentManager(), "overlay_permission_required");
            return false;
        }
        Log.i(getClass().getName(), "Everything is fine. Overlay can be launched.");
        return true;
    }

    public void requestDeviceAdminPermission() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(EXTRA_DEVICE_ADMIN, new ComponentName(this, DeviceAdminSample.class));
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Explanation for why the app needs device admin permission.");
        startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
        Log.d("ADMIN_PERMISSION", "Asking for permissions");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DEVICE_ADMIN && resultCode == RESULT_OK) {
            // Device admin permission granted
            activateDeviceAdmin();
        } else {
            // Device admin permission not granted
        }
        Log.d("ADMIN_PERMISSION", "Result Code:" + resultCode);
    }

    private void provisionManagedProfile() {
        Activity activity = SettingsActivity.this;
        if (null == activity) {
            return;
        }
        Intent intent = new Intent(ACTION_PROVISION_MANAGED_PROFILE);
        if (Build.VERSION.SDK_INT >= 24) {
            intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                    DeviceAdminSample.getComponentName(activity));
        } else {
            //noinspection deprecation
            intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                    activity.getApplicationContext().getPackageName());
            intent.putExtra(EXTRA_DEVICE_ADMIN, DeviceAdminSample.getComponentName(activity));
        }
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_PROVISION_MANAGED_PROFILE);
        } else {
            Toast.makeText(activity, "Device provisioning is not enabled. Stopping.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    AsyncTask<String, String, Void> asyncTask;

    public void activateDeviceAdmin() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        //ComponentName componentName = new ComponentName(this, DeviceAdminSample.class);

        //provisionManagedProfile();
        //devicePolicyManager.setProfileEnabled(componentName);
        //devicePolicyManager.setKeyguardDisabled(componentName, true);
        SettingsActivity context = this;
        ComponentName componentName = new ComponentName(context, DeviceAdminSample.class);
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

// Register the LockReceiver with the IntentFilter
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        context.registerReceiver(new DeviceAdminSample(), filter);

        if (!devicePolicyManager.isAdminActive(componentName)) {
            // The app is not a device administrator yet
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
        } else {
            // The app is already a device administrator
            Log.d("ADMIN_PERMISSION", "Already an Admin");
            if (!devicePolicyManager.isProfileOwnerApp(getPackageName())) {
                // Start the provisioning flow to set the profile owner.
                Intent intent = new Intent(ACTION_PROVISION_MANAGED_PROFILE);
                intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, componentName);
                startActivityForResult(intent, REQUEST_PROVISION_MANAGED_PROFILE);
            }

            asyncTask = new AsyncTask<String, String, Void>() {
                int currentLevel = 0;
                int prevLevel  = 0;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                    prevLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                }

                @Override
                protected Void doInBackground(String... strings) {
                    while(asyncTask.isCancelled() == false) {
                        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
                        currentLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                        Log.d("VOLUME", "Level:" + currentLevel);
                        if(currentLevel > prevLevel) {
                            //Start the overlay
                            Intent intOverlay = new Intent(SettingsActivity.this, PowerOffScreen.class);
                            startActivity(intOverlay);
                        } else if(currentLevel < prevLevel) {
                            Intent intOverlay = new Intent(SettingsActivity.this, StopOverlay.class);
                            startActivity(intOverlay);
                        }
                        prevLevel = currentLevel;
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ex) {

                        }
                    }
                    return null;
                }
            };
            asyncTask.execute();
            if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                // Block power off and safe boot actions
                devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT);
                Log.d("DEVICE", "Reached 2");
            } else {
                Log.d("DEVICE", "Reached 3");
                // Check if the current user is the profile owner
                if (devicePolicyManager.isProfileOwnerApp(getPackageName())) {
                    // Block power off and safe boot actions for the managed profile
                    devicePolicyManager.addUserRestriction(componentName, UserManager.DISALLOW_SAFE_BOOT);
                    Log.d("DEVICE", "Reached 4");
                }
            }
        }
    }
}
