package com.project.ads.helpers;

import android.content.Context;

import com.project.ads.R;
import com.project.ads.models.ServiceRequest;
import com.project.ads.models.api.CatalogData;
import com.project.ads.models.api.QuoteReference;

/**
 * Texto de detalle de una solicitud para la tarjeta de cotización del cliente
 * y del trabajador: tareas pedidas, referencia congelada, motivo del ajuste,
 * agenda y, si se canceló, quién y por qué.
 */
public final class RequestDetailsHelper {

    private RequestDetailsHelper() {
    }

    /** null si la solicitud no tiene nada de esto (p. ej. una anterior al catálogo). */
    public static String details(Context context, ServiceRequest request, CatalogData catalog) {
        return details(context, request, catalog, true);
    }

    /**
     * Igual que {@link #details(Context, ServiceRequest, CatalogData)}; con
     * includeQuoteParts en false omite la referencia, el motivo del ajuste y la
     * agenda, porque la tarjeta de cotización rediseñada los muestra aparte.
     */
    public static String details(Context context, ServiceRequest request, CatalogData catalog,
                                 boolean includeQuoteParts) {
        StringBuilder text = new StringBuilder();

        String tasks = request.getTaskItemsDisplay(catalog);
        if (tasks != null) append(text, tasks);

        QuoteReference reference = request.getQuoteReference();
        if (includeQuoteParts && reference != null && reference.hasRange()) {
            append(text, context.getString(R.string.quote_detail_reference,
                    ServiceRequest.formatCop(reference.getMinCop()), ServiceRequest.formatCop(reference.getMaxCop())));
        }

        if (includeQuoteParts && request.hasQuote() && request.isQuoteAdjusted()
                && request.getQuoteAdjustmentReasonCode() != null) {
            append(text, context.getString(R.string.quote_detail_reason, ReasonHelper.label(
                    ReasonHelper.ADJUSTMENT, request.getQuoteAdjustmentReasonCode(),
                    request.getQuoteAdjustmentReasonText())));
        }

        if (includeQuoteParts && request.hasSchedule() && !request.isCancelled()) {
            append(text, context.getString(request.getRescheduleCount() > 0
                    ? R.string.schedule_detail_rescheduled : R.string.schedule_detail, request.getScheduleDisplay()));
        }

        if (request.isCancelled() && request.getCancellationReasonCode() != null) {
            String reason = ReasonHelper.cancellationLabel(
                    request.getCancellationReasonCode(), request.getCancellationReasonText());
            append(text, context.getString(ReasonHelper.ROLE_WORKER.equals(request.getCancelledBy())
                    ? R.string.cancelled_by_worker : R.string.cancelled_by_client, reason));
        }

        return text.length() > 0 ? text.toString() : null;
    }

    private static void append(StringBuilder text, String line) {
        if (text.length() > 0) text.append("\n\n");
        text.append(line);
    }
}
