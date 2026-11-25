package com.example.pixelclock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class SetupActivity extends Activity {
    private Button accessButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(padding + insets.getSystemWindowInsetLeft(),
                    padding + insets.getSystemWindowInsetTop(),
                    padding + insets.getSystemWindowInsetRight(),
                    padding + insets.getSystemWindowInsetBottom());
            return insets;
        });

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(32);
        content.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText(R.string.media_access_explanation);
        explanation.setTextSize(16);
        explanation.setGravity(Gravity.CENTER);
        explanation.setPadding(0, padding, 0, padding);
        content.addView(explanation);

        accessButton = new Button(this);
        accessButton.setText(R.string.open_notification_access);
        accessButton.setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        content.addView(accessButton);

        Button screensaverButton = new Button(this);
        screensaverButton.setText(R.string.open_screensaver_settings);
        screensaverButton.setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)));
        content.addView(screensaverButton);

        setContentView(content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            ComponentName listener = new ComponentName(this, MediaAccessService.class);
            accessButton.setText(manager.isNotificationListenerAccessGranted(listener)
                    ? R.string.manage_notification_access : R.string.open_notification_access);
        }
    }
}
