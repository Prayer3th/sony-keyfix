package local.sony.keyfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** TV-first controls; all state changes are explicit remote-control actions. */
public final class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView status;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            status.setText(RemapService.status);
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(15, 25, 35));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(24), dp(48), dp(24));
        scroll.addView(root);
        TextView title = text(getString(R.string.app_name), 28, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text(getString(R.string.subtitle), 16, Color.rgb(171, 190, 205)));
        status = text(getString(RemapService.status), 18, Color.rgb(125, 222, 213));
        status.setPadding(0, dp(16), 0, dp(12));
        status.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status);
        TextView mapping = text(getString(R.string.mappings), 18, Color.WHITE);
        mapping.setLineSpacing(dp(5), 1);
        root.addView(mapping);
        root.addView(text(getString(R.string.menu_hint), 14, Color.rgb(171, 190, 205)));
        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(0, dp(14), 0, 0);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(controls);
        Button start = button(controls, R.string.start);
        start.setOnClickListener(v -> {
            prefs.edit().putBoolean("enabled", true).apply();
            startForegroundService(new Intent(this, RemapService.class));
        });
        button(controls, R.string.stop).setOnClickListener(v -> {
            prefs.edit().putBoolean("enabled", false).apply();
            stopService(new Intent(this, RemapService.class));
            RemapService.status = R.string.status_stopped;
            status.setText(R.string.status_stopped);
        });
        button(controls, R.string.help).setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle(R.string.help).setMessage(R.string.help_body)
            .setPositiveButton(R.string.close, null).show());
        button(controls, R.string.about).setOnClickListener(v -> showAbout());
        CheckBox boot = new CheckBox(this);
        boot.setText(R.string.boot);
        boot.setTextSize(16);
        boot.setMinHeight(dp(48));
        boot.setChecked(prefs.getBoolean("boot", false));
        boot.setOnCheckedChangeListener((button, checked) -> prefs.edit().putBoolean("boot", checked).apply());
        root.addView(boot);
        root.addView(text(getString(R.string.footer), 13, Color.rgb(171, 190, 205)));
        setContentView(scroll);
        start.requestFocus();
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        return view;
    }
    private Button button(LinearLayout row, int label) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(16); button.setAllCaps(false); button.setMinHeight(dp(48));
        row.addView(button, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return button;
    }
    private void showAbout() {
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { version = ""; }
        new AlertDialog.Builder(this).setTitle(getString(R.string.app_name) + " " + version)
            .setMessage(getString(R.string.author) + "\n\n" + getString(R.string.about_body))
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.licenses, (dialog, which) -> showLicenses()).show();
    }
    private void showLicenses() {
        StringBuilder contents = new StringBuilder();
        for (String name : new String[]{"LICENSE.txt", "AdbLib-LICENSE.txt", "NOTICE.txt"}) {
            try (InputStream in = getAssets().open(name)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[2048]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                contents.append(new String(out.toByteArray(), "UTF-8")).append("\n\n");
            } catch (Exception e) { contents.append(getString(R.string.license_error)); }
        }
        ScrollView scroll = new ScrollView(this);
        TextView body = text(contents.toString(), 14, Color.WHITE);
        body.setPadding(dp(24), dp(12), dp(24), dp(12));
        scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(R.string.licenses).setView(scroll)
            .setPositiveButton(R.string.close, null).show();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public void onResume() { super.onResume(); handler.post(refresh); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
