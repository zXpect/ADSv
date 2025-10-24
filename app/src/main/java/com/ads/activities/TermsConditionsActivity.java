package com.ads.activities;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.ads.R;

public class TermsConditionsActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String termsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Configurar el Toolbar con colores adaptativos
        setupToolbar();

        // Inicializar vistas
        webView = findViewById(R.id.webViewTerms);
        progressBar = findViewById(R.id.progressBarTerms);

        // Obtener la URL de los términos y condiciones desde el intent
        termsUrl = getIntent().getStringExtra("terms_url");
        if (termsUrl == null || termsUrl.trim().isEmpty()) {
            termsUrl = "https://terminosycondicionesads.netlify.app";
        }

        setupWebView();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Términos y Condiciones");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Aplicar colores adaptativos al título
            int titleColor = ContextCompat.getColor(this, R.color.text_primary);
            toolbar.setTitleTextColor(titleColor);

            // Aplicar color adaptativo al icono de navegación
            if (toolbar.getNavigationIcon() != null) {
                int iconColor = ContextCompat.getColor(this, R.color.text_primary);
                toolbar.getNavigationIcon().setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    private void setupWebView() {
        // Configurar fondo adaptativo para el WebView
        int bgColor = ContextCompat.getColor(this, R.color.background_color);
        webView.setBackgroundColor(bgColor);

        webView.getSettings().setJavaScriptEnabled(true);

        // Soporte para modo oscuro en WebView (Android 10+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            int nightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                webView.getSettings().setForceDark(android.webkit.WebSettings.FORCE_DARK_ON);
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        // Aplicar color adaptativo al ProgressBar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int accentColor = ContextCompat.getColor(this, R.color.colorAccent);
            progressBar.getIndeterminateDrawable().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        }

        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
        webView.loadUrl(termsUrl);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}