package com.jjimi.carrotscreen;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
  private final Handler handler = new Handler();
  private WebView web;
  private SharedPreferences prefs;
  private boolean loaded;

  private final Runnable reconnect = new Runnable() {
    @Override public void run() {
      if (!loaded && web != null && web.getUrl() != null) web.reload();
      handler.postDelayed(this, 5000);
    }
  };

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    immersive();
    prefs = getSharedPreferences("carrot", MODE_PRIVATE);

    FrameLayout root = new FrameLayout(this);
    web = new WebView(this);
    root.addView(web, new FrameLayout.LayoutParams(-1, -1));

    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    web.setKeepScreenOn(true);
    web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient() {
      @Override public void onPageStarted(WebView v, String u, android.graphics.Bitmap i) { loaded = false; }
      @Override public void onPageFinished(WebView v, String u) { loaded = true; }
      @Override public void onReceivedError(WebView v, int c, String d, String u) { loaded = false; }
    });

    TextView gear = new TextView(this);
    gear.setText("⚙");
    gear.setTextSize(22);
    gear.setTextColor(Color.WHITE);
    gear.setGravity(Gravity.CENTER);
    gear.setBackgroundColor(0x66000000);
    gear.setOnClickListener(v -> addressDialog());
    FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(64, 64, Gravity.TOP | Gravity.END);
    gp.setMargins(0, 12, 12, 0);
    root.addView(gear, gp);
    setContentView(root);

    String saved = prefs.getString("url", "");
    if (saved.isEmpty()) addressDialog(); else web.loadUrl(saved);
    handler.postDelayed(reconnect, 5000);
  }

  private String normalize(String text) {
    String u = text.trim();
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
    return u;
  }

  private void addressDialog() {
    EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setHint("예: IP주소:7000");
    input.setText(prefs.getString("url", ""));
    input.setSelectAllOnFocus(true);
    new AlertDialog.Builder(this)
      .setTitle("당근맨 주소 입력")
      .setMessage("브라우저 주소창에 표시되는 IP주소와 :7000을 입력하세요.")
      .setView(input)
      .setPositiveButton("연결", (d, w) -> {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) { Toast.makeText(this, "주소를 입력하세요", Toast.LENGTH_LONG).show(); return; }
        String url = normalize(raw);
        prefs.edit().putString("url", url).apply();
        loaded = false;
        web.loadUrl(url);
      })
      .setNeutralButton("새로고침", (d, w) -> web.reload())
      .setNegativeButton("취소", null)
      .show();
  }

  private void immersive() {
    getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
      View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); }
  @Override public void onBackPressed() {
    if (web.canGoBack()) web.goBack();
    else Toast.makeText(this, "최근 앱 화면에서 닫을 수 있습니다", Toast.LENGTH_SHORT).show();
  }
  @Override protected void onResume() {
    super.onResume();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    web.onResume();
  }
  @Override protected void onDestroy() {
    handler.removeCallbacks(reconnect);
    if (web != null) web.destroy();
    super.onDestroy();
  }
}
