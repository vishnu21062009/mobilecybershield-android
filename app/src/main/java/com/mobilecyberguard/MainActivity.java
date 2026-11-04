package com.mobilecyberguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private static final String START_URL = "https://mobilecybergaurd.netlify.app/";
    private static final int CAMERA_REQUEST = 1001;
    private static final int FILE_REQUEST = 1002;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingCameraRequest;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new ShieldWebViewClient());
        webView.setWebChromeClient(new ShieldWebChromeClient());
        webView.loadUrl(START_URL);
    }

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    private void finishCameraRequest(boolean granted) {
        if (pendingCameraRequest == null) return;
        if (granted) pendingCameraRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}); else pendingCameraRequest.deny();
        pendingCameraRequest = null;
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_REQUEST) finishCameraRequest(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_REQUEST || filePathCallback == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount(); result = new Uri[count];
                for (int i = 0; i < count; i++) result[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) result = new Uri[]{data.getData()};
        }
        filePathCallback.onReceiveValue(result); filePathCallback = null;
    }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (filePathCallback != null) filePathCallback.onReceiveValue(null); if (webView != null) { webView.stopLoading(); webView.destroy(); } super.onDestroy(); }

    private final class ShieldWebViewClient extends WebViewClient {
        private boolean isAppUrl(Uri uri) { return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && "mobilecybergaurd.netlify.app".equalsIgnoreCase(uri.getHost()); }
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl(); if (isAppUrl(uri)) return false;
            try { view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { } return true;
        }
        @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) { }
    }
    private final class ShieldWebChromeClient extends WebChromeClient {
        @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) filePathCallback.onReceiveValue(null); filePathCallback = callback;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*");
            try { startActivityForResult(intent, FILE_REQUEST); return true; } catch (Exception e) { filePathCallback = null; return false; }
        }
        @Override public void onPermissionRequest(PermissionRequest request) {
            runOnUiThread(() -> {
                boolean wantsCamera = false;
                for (String resource : request.getResources()) if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) wantsCamera = true;
                if (!wantsCamera) { request.deny(); return; }
                if (hasCameraPermission()) request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                else { pendingCameraRequest = request; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST); else finishCameraRequest(false); }
            });
        }
    }
}
