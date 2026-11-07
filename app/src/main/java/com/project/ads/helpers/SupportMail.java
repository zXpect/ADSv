package com.project.ads.helpers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.project.ads.R;

/** Abre la app de correo con un mensaje para el soporte de ADS. */
public final class SupportMail {

    private SupportMail() {
    }

    public static void open(Context context) {
        String email = context.getString(R.string.support_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_mail_subject));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.support_no_mail_app, email), Toast.LENGTH_LONG).show();
        }
    }
}
