package it.francescogabbrielli.apps.stargazers.controller;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.TypedValue;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

/**
 * Created by Francesco Gabbrielli on 5/03/18.
 */
public class WebViewClient extends android.webkit.WebViewClient {

    private Context context;
    private WebView view;

    public WebViewClient(Context context, WebView view) {
        this.context = context;
        this.view = view;
        view.getSettings().setJavaScriptEnabled(true);
        view.setWebViewClient(this);
    }

    public void load(String url) {
        view.post(() -> view.loadUrl(url));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(i);
        return true;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        //TODO: display error?
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Resources r = context.getResources();
        int px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics()));
        view.scrollBy(0,-px);
    }
}
