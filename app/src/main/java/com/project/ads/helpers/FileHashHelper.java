package com.project.ads.helpers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.InputStream;
import java.security.MessageDigest;

/** SHA-256 y nombre de un archivo elegido, para no subir dos veces el mismo PDF (DEF-23). */
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

    /** Nombre con que el archivo aparece en el teléfono, o null si no se conoce. */
    public static String displayName(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return null;
        }
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
