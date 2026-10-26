package com.project.ads.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.project.ads.R;

/**
 * Diálogo de espera («Iniciando sesión...») con el tema ADS, en lugar del
 * ProgressDialog de Android, que está obsoleto y no toma el tema (DEF-29).
 * Tiene los mismos métodos que se usaban de ProgressDialog; no se cierra al
 * tocar afuera ni con atrás, salvo que se pida.
 */
public class LoadingDialog {

    private final AlertDialog mDialog;
    private final TextView mMessage;

    public LoadingDialog(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        mMessage = view.findViewById(R.id.tv_loading_message);
        mDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    /** Se puede cambiar con el diálogo abierto (p. ej. el porcentaje de una subida). */
    public void setMessage(CharSequence message) {
        mMessage.setText(message);
    }

    public void setCancelable(boolean cancelable) {
        mDialog.setCancelable(cancelable);
    }

    public void setCanceledOnTouchOutside(boolean cancel) {
        mDialog.setCanceledOnTouchOutside(cancel);
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
