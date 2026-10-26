package com.project.ads.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;

import com.project.ads.R;
import com.project.ads.helpers.ConsentHelper;
import com.project.ads.helpers.LegalTextParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Términos y Condiciones y Aviso de privacidad dentro de la app (res/raw), con la
 * versión que guarda el registro (ConsentHelper). Reemplaza la página externa.
 * Se abre desde Mi cuenta y desde los dos registros, en la pestaña pedida.
 */
public class LegalActivity extends AppCompatActivity {

    private static final String TAG = "LegalActivity";

    public static final String EXTRA_DOCUMENT = "document";
    public static final String DOCUMENT_TERMS = "terms";
    public static final String DOCUMENT_PRIVACY = "privacy";

    private TextView mTabTerms;
    private TextView mTabPrivacy;
    private LinearLayout mContainer;
    private NestedScrollView mScroll;

    public static Intent intent(Context context, String document) {
        return new Intent(context, LegalActivity.class).putExtra(EXTRA_DOCUMENT, document);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        mTabTerms = findViewById(R.id.tab_legal_terms);
        mTabPrivacy = findViewById(R.id.tab_legal_privacy);
        mContainer = findViewById(R.id.container_legal);
        mScroll = findViewById(R.id.scroll_legal);

        findViewById(R.id.btn_legal_back).setOnClickListener(v -> finish());
        mTabTerms.setOnClickListener(v -> show(DOCUMENT_TERMS));
        mTabPrivacy.setOnClickListener(v -> show(DOCUMENT_PRIVACY));

        String document = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_DOCUMENT) : getIntent().getStringExtra(EXTRA_DOCUMENT);
        show(DOCUMENT_PRIVACY.equals(document) ? DOCUMENT_PRIVACY : DOCUMENT_TERMS);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_DOCUMENT, mTabPrivacy.isSelected() ? DOCUMENT_PRIVACY : DOCUMENT_TERMS);
    }

    private void show(String document) {
        boolean privacy = DOCUMENT_PRIVACY.equals(document);
        selectTab(mTabTerms, !privacy);
        selectTab(mTabPrivacy, privacy);

        ((TextView) findViewById(R.id.tv_legal_document_title)).setText(
                privacy ? R.string.legal_privacy_title : R.string.legal_terms_title);
        ((TextView) findViewById(R.id.tv_legal_version)).setText(privacy
                ? getString(R.string.legal_privacy_version, ConsentHelper.PRIVACY_VERSION)
                : getString(R.string.legal_terms_version, ConsentHelper.TERMS_VERSION,
                        getString(R.string.legal_terms_updated)));

        mContainer.removeAllViews();
        for (LegalTextParser.Block block : LegalTextParser.parse(
                readRaw(privacy ? R.raw.legal_privacy : R.raw.legal_terms))) {
            mContainer.addView(blockView(block));
        }
        mScroll.scrollTo(0, 0);
    }

    /** Pestaña elegida con fondo de tarjeta, como el control Lista / Mapa. */
    private void selectTab(TextView tab, boolean selected) {
        tab.setSelected(selected);
        tab.setBackgroundResource(selected ? R.drawable.bg_ads_segmented_selected : 0);
        tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.ads_text : R.color.ads_muted));
    }

    private View blockView(LegalTextParser.Block block) {
        switch (block.type) {
            case SECTION: {
                TextView title = text(block.text, R.style.TextAppearance_ADS_Subtitle, R.color.ads_text);
                ViewCompat.setAccessibilityHeading(title, true);
                margins(title, R.dimen.space_6);
                return title;
            }
            case CLAUSE:
                return row(block.number, block.text);
            case BULLET:
                return row("•", block.text);
            default: {
                TextView paragraph = text(block.text, R.style.TextAppearance_ADS_BodySmall, R.color.ads_charcoal);
                margins(paragraph, R.dimen.space_3);
                return paragraph;
            }
        }
    }

    /** Número o viñeta a la izquierda y el texto con sangría colgante (el lector de pantalla lee el texto). */
    private View row(String marker, String body) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        margins(row, R.dimen.space_3);

        TextView number = text(marker, R.style.TextAppearance_ADS_BodySmall_Strong, R.color.ads_text);
        number.setMinWidth(getResources().getDimensionPixelSize(R.dimen.legal_marker_width));
        number.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(number, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView text = text(body, R.style.TextAppearance_ADS_BodySmall, R.color.ads_charcoal);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private TextView text(String value, @StyleRes int appearance, int color) {
        TextView view = new TextView(this);
        TextViewCompat.setTextAppearance(view, appearance);
        view.setTextColor(ContextCompat.getColor(this, color));
        view.setText(value);
        view.setTextIsSelectable(true);
        return view;
    }

    private void margins(View view, int topDimen) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(topDimen);
        view.setLayoutParams(params);
    }

    private String readRaw(@RawRes int res) {
        try (InputStream in = getResources().openRawResource(res)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "No se pudo leer el documento legal", e);
            return "";
        }
    }
}
