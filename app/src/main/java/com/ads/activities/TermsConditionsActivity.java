package com.ads.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.ads.includes.MyToolbar;
import com.project.ads.R;

public class TermsConditionsActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String termsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Configurar el toolbar
        MyToolbar.show(this, "Términos y Condiciones", true);

        // Inicializar vistas
        webView = findViewById(R.id.webViewTerms);
        progressBar = findViewById(R.id.progressBarTerms);

        // Obtener la URL de los términos y condiciones del intent
        termsUrl = getIntent().getStringExtra("terms_url");
        if (termsUrl == null || termsUrl.isEmpty()) {
            // URL por defecto si no se proporciona una
            termsUrl = "https://sudominio.com/terminos-condiciones.html";
        }

        setupWebView();
    }

    private void setupWebView() {
        // Configurar el WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Ocultar la barra de progreso cuando la página termine de cargar
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        // Mostrar la barra de progreso mientras carga
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);

        // Cargar la URL
        webView.loadUrl(termsUrl);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}