package com.project.ads.models;

import com.project.ads.helpers.ServiceRequestHelper;

import java.util.Map;

/**
 * Re-cotización de un servicio (D7, HU-26): requests/<id>/requote. La escribe solo
 * el backend. Una por servicio, en on_the_way o in_progress; el precio vigente
 * (quoted_price) cambia solo si el cliente la acepta.
 */
public class Requote {

    public static final String PENDING = "pending";
    public static final String ACCEPTED = "accepted";
    public static final String REJECTED = "rejected";

    private long price;
    private String reasonCode;
    private String reasonText;
    private String status;
    private long requestedAt;

    @SuppressWarnings("unchecked")
    public static Requote fromMap(Object value) {
        if (!(value instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) value;
        Requote requote = new Requote();
        requote.price = ServiceRequestHelper.getLongValue(map, "price");
        requote.reasonCode = text(map, "reason_code");
        requote.reasonText = text(map, "reason_text");
        requote.status = text(map, "status");
        requote.requestedAt = ServiceRequestHelper.getLongValue(map, "requested_at");
        return requote;
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : null;
    }

    public long getPrice() { return price; }
    public String getReasonCode() { return reasonCode; }
    public String getReasonText() { return reasonText; }
    public String getStatus() { return status; }
    public long getRequestedAt() { return requestedAt; }

    public boolean isPending() { return PENDING.equals(status); }
    public boolean isAccepted() { return ACCEPTED.equals(status); }
    public boolean isRejected() { return REJECTED.equals(status); }
}
