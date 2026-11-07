package com.project.ads.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.project.ads.R;

/**
 * Ventana de carga del registro del trabajador. Muestra los pasos (crear la
 * cuenta, subir los documentos con su avance y enviarlos a revisión) y no se
 * cierra con atrás ni al tocar afuera, para que quede claro que la pantalla de
 * atrás no se puede usar mientras tanto. Si no hay documentos, solo muestra el
 * primer paso.
 */
public class RegistrationProgressDialog {

    public static final int STEP_ACCOUNT = 1;
    public static final int STEP_DOCUMENTS = 2;
    public static final int STEP_REVIEW = 3;

    private final Context mContext;
    private final AlertDialog mDialog;
    private final TextView mTitle;
    private final TextView mDetail;
    private final ProgressBar[] mSpinners = new ProgressBar[3];
    private final ImageView[] mIcons = new ImageView[3];
    private final TextView[] mLabels = new TextView[3];
    private final View mStep2;
    private final View mStep3;
    private final ProgressBar mDocsBar;

    public RegistrationProgressDialog(Context context) {
        mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_registration_progress, null);
        mTitle = view.findViewById(R.id.tv_regprog_title);
        mDetail = view.findViewById(R.id.tv_regprog_detail);
        mSpinners[0] = view.findViewById(R.id.pb_step1);
        mSpinners[1] = view.findViewById(R.id.pb_step2);
        mSpinners[2] = view.findViewById(R.id.pb_step3);
        mIcons[0] = view.findViewById(R.id.iv_step1);
        mIcons[1] = view.findViewById(R.id.iv_step2);
        mIcons[2] = view.findViewById(R.id.iv_step3);
        mLabels[0] = view.findViewById(R.id.tv_step1);
        mLabels[1] = view.findViewById(R.id.tv_step2);
        mLabels[2] = view.findViewById(R.id.tv_step3);
        mStep2 = view.findViewById(R.id.layout_step2);
        mStep3 = view.findViewById(R.id.layout_step3);
        mDocsBar = view.findViewById(R.id.pb_documents);
        mDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
        mDialog.setCanceledOnTouchOutside(false);
    }

    /** Con documentos se ven los tres pasos; sin ellos, solo "Crear tu cuenta". */
    public void setWithDocuments(boolean withDocuments) {
        mStep2.setVisibility(withDocuments ? View.VISIBLE : View.GONE);
        mStep3.setVisibility(withDocuments ? View.VISIBLE : View.GONE);
        if (!withDocuments) mDocsBar.setVisibility(View.GONE);
    }

    /** Marca como hechos los pasos anteriores y en curso el indicado. */
    public void setStep(int step) {
        for (int i = 0; i < 3; i++) {
            int n = i + 1;
            if (n < step) {
                mSpinners[i].setVisibility(View.GONE);
                mIcons[i].setVisibility(View.VISIBLE);
                mIcons[i].setImageResource(R.drawable.ic_check_circle);
                ImageViewCompat.setImageTintList(mIcons[i],
                        ContextCompat.getColorStateList(mContext, R.color.ads_brand_text));
                mLabels[i].setTextAppearance(R.style.TextAppearance_ADS_BodySmall);
                mLabels[i].setTextColor(ContextCompat.getColor(mContext, R.color.ads_text));
            } else if (n == step) {
                mSpinners[i].setVisibility(View.VISIBLE);
                mIcons[i].setVisibility(View.GONE);
                mLabels[i].setTextAppearance(R.style.TextAppearance_ADS_BodySmall_Strong);
                mLabels[i].setTextColor(ContextCompat.getColor(mContext, R.color.ads_text));
            } else {
                mSpinners[i].setVisibility(View.GONE);
                mIcons[i].setVisibility(View.VISIBLE);
                mIcons[i].setImageResource(R.drawable.bg_ads_step_pending);
                ImageViewCompat.setImageTintList(mIcons[i], null);
                mLabels[i].setTextAppearance(R.style.TextAppearance_ADS_BodySmall);
                mLabels[i].setTextColor(ContextCompat.getColor(mContext, R.color.ads_muted));
            }
        }
        mTitle.setText(step >= STEP_DOCUMENTS ? R.string.regprog_title_docs : R.string.regprog_title);
        mDocsBar.setVisibility(step == STEP_DOCUMENTS ? View.VISIBLE : View.GONE);
        setMessage(null);
    }

    /** Avance de la subida: "Subir tus documentos (2 de 5)" y la barra. */
    public void setDocuments(int uploaded, int total) {
        mLabels[1].setText(mContext.getString(R.string.regprog_step_docs_count, uploaded, total));
        mDocsBar.setProgress(total > 0 ? Math.round(uploaded * 100f / total) : 0);
    }

    /** Detalle opcional bajo los pasos (p. ej. el porcentaje de la foto de perfil). */
    public void setMessage(CharSequence message) {
        boolean has = message != null && message.length() > 0;
        mDetail.setText(has ? message : "");
        mDetail.setVisibility(has ? View.VISIBLE : View.GONE);
    }

    public void show() {
        if (!mDialog.isShowing()) mDialog.show();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public void dismiss() {
        if (mDialog.isShowing()) mDialog.dismiss();
    }
}
