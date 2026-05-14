package com.rotempavel.bmi4;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    
    // URL of the hosted PWA
    private static final String APP_URL = "https://rotempavel1-svg.github.io/bmi4/index.html";
    private static final String OAUTH_PREFIX = "https://accounts.google.com/o/oauth2/auth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // Hide status bar visibility for fullscreen feel
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // KEY FIX 1: Use Chrome user agent (remove "wv" WebView marker)
        // Google blocks OAuth when it sees "wv" in the user agent
        String currentUA = settings.getUserAgentString();
        String chromeUA = currentUA.replace("; wv)", ")").replace("Version/4.0 ", "");
        settings.setUserAgentString(chromeUA);

        // JavaScript bridge for native communication
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        // KEY FIX 2: Intercept OAuth URLs and launch Chrome Custom Tabs
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith(OAUTH_PREFIX)) {
                    launchChromeCustomTab(url);
                    return true;
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

        // Check if app was opened via OAuth callback intent
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

    /**
     * Handle the OAuth callback URL.
     * When Chrome Custom Tabs redirects to our app URL after Google auth,
     * Android calls this with the URL containing the token in the fragment.
     */
    private boolean handleOAuthIntent(Intent intent) {
        if (intent == null) return false;

        Uri data = intent.getData();
        if (data == null) return false;

        String fragment = data.getFragment();
        if (fragment != null && fragment.contains("access_token=")) {
            String urlWithToken = APP_URL + "#" + fragment;
            webView.loadUrl(urlWithToken);
            return true;
        }

        String url = data.toString();
        if (url.startsWith("https://rotempavel1-svg.github.io/bmi4")) {
            webView.loadUrl(url);
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

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * JavaScript-accessible bridge.
     * The web app can call these methods via window.AndroidBridge.xxx()
     */
    class AndroidBridge {
        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        @JavascriptInterface
        public String getAppVersion() {
            return "1.1";
        }

        @JavascriptInterface
        public void openOAuth(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    launchChromeCustomTab(url);
                }
            });
        }
    }
}
