package com.rotempavel.bmi4;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String CHANNEL_ID = "health_tracker_channel";
    private static final String CHANNEL_NAME = "מעקב בריאות";
    private static final int NOTIF_PERM_CODE = 100;
    private static final int STORAGE_PERM_CODE = 101;
    
    // URLs
    private static final String APP_URL = "file:///android_asset/index.html";
    private static final String OAUTH_PREFIX = "https://accounts.google.com/o/oauth2/auth";
    private static final String DEEP_LINK_HOST = "rotempavel1-svg.github.io";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create notification channel
        createNotificationChannel();
        
        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERM_CODE);
            }
        }

        webView = new WebView(this);
        setContentView(webView);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Use Chrome user agent (avoid WebView detection by Google)
        String currentUA = settings.getUserAgentString();
        String chromeUA = currentUA.replace("; wv)", ")").replace("Version/4.0 ", "");
        settings.setUserAgentString(chromeUA);

        // Inject native bridge
        webView.addJavascriptInterface(new NativeBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                
                // Intercept OAuth -> Chrome Custom Tab
                if (url.startsWith(OAUTH_PREFIX)) {
                    launchChromeCustomTab(url);
                    return true;
                }
                
                // External links open in browser
                if (url.startsWith("http") && !url.contains(DEEP_LINK_HOST)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // Handle data: URLs for file downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            handleDownload(url, mimetype);
        });

        // Check if app was opened via OAuth callback
        if (!handleOAuthIntent(getIntent())) {
            webView.loadUrl(APP_URL);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOAuthIntent(intent);
    }

    private boolean handleOAuthIntent(Intent intent) {
        if (intent == null) return false;
        Uri data = intent.getData();
        if (data == null) return false;

        String fragment = data.getFragment();
        if (fragment != null && fragment.contains("access_token=")) {
            // Inject the token into the WebView via JS
            String js = "window.location.hash = '" + fragment + "'; " +
                       "if (typeof handleGFitCallback === 'function') handleGFitCallback();";
            webView.post(() -> webView.evaluateJavascript(js, null));
            return true;
        }
        return false;
    }

    private void launchChromeCustomTab(String url) {
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
        } catch (Exception e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("התראות אפליקציית מעקב בריאות");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{200, 100, 200});
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void handleDownload(String url, String mimetype) {
        try {
            if (url.startsWith("data:")) {
                // Handle data URLs (CSV/JSON exports)
                String[] parts = url.split(",", 2);
                if (parts.length < 2) return;
                
                String header = parts[0];
                String data = parts[1];
                
                boolean isBase64 = header.contains("base64");
                byte[] bytes;
                if (isBase64) {
                    bytes = Base64.decode(data, Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(data).getBytes();
                }
                
                // Determine extension from mimetype or data
                String ext = "txt";
                if (header.contains("json")) ext = "json";
                else if (header.contains("csv")) ext = "csv";
                else if (header.contains("html")) ext = "html";
                else if (mimetype != null) {
                    if (mimetype.contains("json")) ext = "json";
                    else if (mimetype.contains("csv")) ext = "csv";
                }
                
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String filename = "health_export_" + timestamp + "." + ext;
                
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File outFile = new File(downloadsDir, filename);
                
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(bytes);
                fos.close();
                
                runOnUiThread(() -> Toast.makeText(this, 
                    "✅ נשמר בהורדות: " + filename, Toast.LENGTH_LONG).show());
                
                // Offer to open the file
                try {
                    Uri fileUri = FileProvider.getUriForFile(this, 
                        getPackageName() + ".fileprovider", outFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(mimetype != null ? mimetype : "text/plain");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "שתף את הקובץ"));
                } catch (Exception ignored) {}
                
            } else {
                // Regular URL download via DownloadManager
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, 
                    "health_export_" + System.currentTimeMillis());
                
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    runOnUiThread(() -> Toast.makeText(this, "מוריד...", Toast.LENGTH_SHORT).show());
                }
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, 
                "שגיאה בהורדה: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Native bridge exposed to JavaScript.
     * JS can call these via window.AndroidBridge.xxx()
     */
    public class NativeBridge {
        
        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        @JavascriptInterface
        public String getAppVersion() {
            return "2.0";
        }

        @JavascriptInterface
        public void openOAuth(final String url) {
            runOnUiThread(() -> launchChromeCustomTab(url));
        }

        @JavascriptInterface
        public void openExternal(final String url) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "לא ניתן לפתוח את הקישור", Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Show a system notification.
         * JS: window.AndroidBridge.showNotification('כותרת', 'תוכן', 'tag')
         */
        @JavascriptInterface
        public void showNotification(final String title, final String body, final String tag) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        flags |= PendingIntent.FLAG_IMMUTABLE;
                    }
                    
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                        MainActivity.this, 0, intent, flags);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{200, 100, 200})
                        .setContentIntent(pendingIntent);

                    NotificationManagerCompat nm = NotificationManagerCompat.from(MainActivity.this);
                    
                    int notifId = tag != null ? tag.hashCode() : (int) System.currentTimeMillis();
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, 
                            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            nm.notify(notifId, builder.build());
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERM_CODE);
                        }
                    } else {
                        nm.notify(notifId, builder.build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @JavascriptInterface
        public boolean hasNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(MainActivity.this, 
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        @JavascriptInterface
        public void requestNotificationPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERM_CODE);
                }
            });
        }

        /**
         * Save text content to Downloads folder.
         * JS: window.AndroidBridge.saveFile('filename.csv', 'text content', 'text/csv')
         */
        @JavascriptInterface
        public boolean saveFile(final String filename, final String content, final String mimetype) {
            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                
                File outFile = new File(downloadsDir, filename);
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(content.getBytes("UTF-8"));
                fos.close();
                
                runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                    "✅ נשמר: " + filename, Toast.LENGTH_LONG).show());
                
                // Offer to share/open
                try {
                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this, 
                        getPackageName() + ".fileprovider", outFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(mimetype != null ? mimetype : "text/plain");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, "שתף או פתח")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                    "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
                return false;
            }
        }

        @JavascriptInterface
        public void vibrate(final int millis) {
            runOnUiThread(() -> {
                try {
                    android.os.Vibrator vib = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vib != null && vib.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib.vibrate(android.os.VibrationEffect.createOneShot(millis, 
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vib.vibrate(millis);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @JavascriptInterface
        public void toast(final String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void setSharedPref(String key, String value) {
            SharedPreferences prefs = getSharedPreferences("wtp_data", MODE_PRIVATE);
            prefs.edit().putString(key, value).apply();
        }

        @JavascriptInterface
        public String getSharedPref(String key) {
            SharedPreferences prefs = getSharedPreferences("wtp_data", MODE_PRIVATE);
            return prefs.getString(key, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIF_PERM_CODE && grantResults.length > 0 && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "✅ התראות הופעלו", Toast.LENGTH_SHORT).show();
        }
    }
}
