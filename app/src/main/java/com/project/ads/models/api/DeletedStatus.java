package com.project.ads.models.api;

/** Respuesta de POST /api/accounts/deleted-status/: si la cuenta del correo fue eliminada y por quién. */
public class DeletedStatus {
    public static final String BY_ADMIN = "admin";

    private boolean deleted;
    private String deleted_by;

    public boolean isDeleted() {
        return deleted;
    }

    /** true si la eliminó el equipo de ADS desde el panel; false si la eliminó el propio usuario. */
    public boolean isDeletedByAdmin() {
        return BY_ADMIN.equals(deleted_by);
    }
}
