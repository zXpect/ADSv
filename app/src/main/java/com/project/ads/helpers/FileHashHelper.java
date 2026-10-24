package com.project.ads.helpers;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.InputStream;
import java.security.MessageDigest;

/** SHA-256 de un archivo elegido, para no subir dos veces el mismo PDF (DEF-23). */
public final class FileHashHelper {

    private FileHashHelper() {
    }

    /** Hash en hexadecimal, o null si no se pudo leer el archivo. */
    public static String sha256(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return null;
        }
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
