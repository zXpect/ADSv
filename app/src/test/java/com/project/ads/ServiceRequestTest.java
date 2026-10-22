package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.ServiceRequestHelper;
import com.project.ads.models.ServiceRequest;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Estados de la solicitud, formatos de dinero y duración, y lectura de lo que devuelve Firebase. */
public class ServiceRequestTest {

    private static ServiceRequest request(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put((String) keyValues[i], keyValues[i + 1]);
        return ServiceRequest.fromMap(map);
    }

    @Test
    public void formatCop() {
        assertEquals("$85.000", ServiceRequest.formatCop(85000));
        assertEquals("$25.500", ServiceRequest.formatCop(25500));
        assertEquals("$1.250.000", ServiceRequest.formatCop(1250000));
        assertEquals("$0", ServiceRequest.formatCop(0));
        assertEquals("$85.000", request("quoted_price", 85000L).getFormattedQuotedPrice());
    }

    @Test
    public void formatDuration() {
        assertEquals("15 min", ServiceRequest.formatDuration(15));
        assertEquals("1 h", ServiceRequest.formatDuration(60));
        assertEquals("1 h 30 min", ServiceRequest.formatDuration(90));
        assertEquals("12 h", ServiceRequest.formatDuration(720));
    }

    @Test
    public void statusDisplayText() {
        String[][] cases = {{"pending", "Pendiente"}, {"accepted", "Aceptado"}, {"on_the_way", "En Camino"},
                {"in_progress", "En Progreso"}, {"completed", "Completado"}, {"cancelled", "Cancelado"},
                {"rejected", "Rechazado"}, {"COMPLETED", "Completado"}, {"estado_nuevo", "estado_nuevo"}};
        for (String[] c : cases) assertEquals(c[0], c[1], request("status", c[0]).getStatusDisplayText());
        assertEquals("Desconocido", request().getStatusDisplayText());
    }

    @Test
    public void cancelOnlyBeforeTheTripStarts() {
        // D8: cliente y trabajador cancelan hasta antes de «Iniciar viaje»
        assertTrue(request("status", "pending").canBeCancelled());
        assertTrue(request("status", "accepted").canBeCancelled());
        for (String status : new String[]{"on_the_way", "in_progress", "completed", "cancelled", "rejected"}) {
            assertFalse(status, request("status", status).canBeCancelled());
        }
    }

    @Test
    public void rescheduleOnlyWhenAccepted() {
        assertTrue(request("status", "accepted").canBeRescheduled());
        assertFalse(request("status", "pending").canBeRescheduled());
        assertFalse(request("status", "on_the_way").canBeRescheduled());
    }

    @Test
    public void rateOnlyWhenCompletedPaidAndNotRated() {
        assertTrue(request("status", "completed", "payment_status", "approved").canBeRated());
        assertFalse(request("status", "completed", "payment_status", "pending").canBeRated());
        assertFalse(request("status", "in_progress", "payment_status", "approved").canBeRated());
        assertFalse(request("status", "completed", "payment_status", "approved", "is_rated", true).canBeRated());
    }

    @Test
    public void activeStates() {
        assertTrue(request("status", "accepted").isActive());
        assertTrue(request("status", "on_the_way").isActive());
        assertTrue(request("status", "in_progress").isActive());
        assertFalse(request("status", "pending").isActive());
        assertFalse(request("status", "completed").isActive());
    }

    @Test
    public void quoteState() {
        ServiceRequest quoted = request("quoted_price", 85000L, "quote_status", "quoted");
        assertTrue(quoted.hasQuote());
        assertTrue(quoted.isQuotePendingAnswer());
        assertFalse(request("quoted_price", 0L, "quote_status", "quoted").hasQuote());
        assertFalse(request("quoted_price", 85000L).hasQuote());
        assertTrue(request("quote_status", "accepted").isQuoteAccepted());
        assertTrue(request("quote_status", "rejected").isQuoteRejected());
        assertTrue(request("payment_status", "pending").isPaymentPending());
        assertTrue(request("payment_status", "approved").isPaid());
    }

    @Test
    public void fromMapAcceptsTheNumberTypesFirebaseReturns() {
        ServiceRequest r = request("quoted_price", 85000, "scheduled_at", 1.7e12, "reschedule_count", "1",
                "estimated_duration_min", 90L, "estimated_cost", 50000L, "is_rated", "true");
        assertEquals("$85.000", r.getFormattedQuotedPrice());
        assertEquals(1700000000000L, r.getScheduledAt());
        assertTrue(r.hasSchedule());
        assertEquals(1, r.getRescheduleCount());
        assertEquals(90, r.getEstimatedDurationMin());
        // Solo un booleano real cuenta: un texto «true» no marca la solicitud como calificada
        assertTrue(request("status", "completed", "payment_status", "approved", "is_rated", "true").canBeRated());
        assertFalse(request().hasSchedule());
        assertEquals(0, request("reschedule_count", "x").getRescheduleCount());
        assertNull(ServiceRequest.fromMap(null));
    }

    @Test
    public void taskItemsWithGapsArriveAsAMap() {
        Map<String, Object> item = new HashMap<>();
        item.put("custom_description", "Revisar humedad");
        item.put("quantity", 1L);
        Map<String, Object> withGaps = new HashMap<>();
        withGaps.put("1", item);
        assertEquals("• Otro: Revisar humedad · 1", request("task_items", withGaps).getTaskItemsDisplay(null));
        assertEquals("• Otro: Revisar humedad · 1",
                request("task_items", Arrays.asList(null, item)).getTaskItemsDisplay(null));
        assertNull(request().getTaskItemsDisplay(null));
    }

    @Test
    public void serviceRequestHelperReadsNumbersSafely() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", 3);
        map.put("double", 4.7);
        map.put("long", 5L);
        map.put("text", "6");
        map.put("bool", true);
        assertEquals(3L, ServiceRequestHelper.getLongValue(map, "int"));
        assertEquals(4L, ServiceRequestHelper.getLongValue(map, "double"));
        assertEquals(0L, ServiceRequestHelper.getLongValue(map, "text"));
        assertEquals(0L, ServiceRequestHelper.getLongValue(map, "missing"));
        assertEquals(5.0, ServiceRequestHelper.getDoubleValue(map, "long"), 0);
        assertEquals(3.0f, ServiceRequestHelper.getFloatValue(map, "int"), 0);
        assertEquals(4.7f, ServiceRequestHelper.getFloatValue(map, "double"), 0.0001f);
        assertTrue(ServiceRequestHelper.getBooleanValue(map, "bool"));
        assertFalse(ServiceRequestHelper.getBooleanValue(map, "text"));
        assertFalse(ServiceRequestHelper.getBooleanValue(map, "missing"));
    }

    @Test
    public void paymentMethod() {
        // Sin método elegido (app anterior) se paga en línea
        assertFalse(request("quote_status", "accepted").isCashPayment());
        assertFalse(request("payment_method", "digital").isCashPayment());
        assertTrue(request("payment_method", "cash").isCashPayment());

        // Se cambia con la cotización aceptada y mientras el servicio no termine (D10)
        assertTrue(request("status", "accepted", "quote_status", "accepted").canChangePaymentMethod());
        assertTrue(request("status", "in_progress", "quote_status", "accepted").canChangePaymentMethod());
        assertFalse(request("status", "accepted", "quote_status", "quoted").canChangePaymentMethod());
        assertFalse(request("status", "completed", "quote_status", "accepted").canChangePaymentMethod());
        assertFalse(request("status", "cancelled", "quote_status", "accepted").canChangePaymentMethod());
    }

    @Test
    public void requoteOnlyOnceWhileOnTheWayOrInProgress() {
        // D7: una sola re-cotización, en on_the_way o in_progress
        assertTrue(request("status", "on_the_way").canRequote());
        assertTrue(request("status", "in_progress").canRequote());
        assertFalse(request("status", "accepted").canRequote());
        assertFalse(request("status", "completed").canRequote());

        Map<String, Object> rejected = new HashMap<>();
        rejected.put("price", 130000L);
        rejected.put("reason_code", "dificultad");
        rejected.put("status", "rejected");
        ServiceRequest afterReject = request("status", "in_progress", "requote", rejected);
        assertFalse(afterReject.canRequote());
        assertFalse(afterReject.hasPendingRequote());
        assertTrue(afterReject.getRequote().isRejected());
    }

    @Test
    public void pendingRequoteIsReadFromFirebase() {
        Map<String, Object> pending = new HashMap<>();
        pending.put("price", 130000L);
        pending.put("reason_code", "otro");
        pending.put("reason_text", "Había que cambiar el tubo");
        pending.put("status", "pending");
        pending.put("requested_at", 1792000000000L);
        ServiceRequest request = request("status", "in_progress", "quoted_price", 95000L, "requote", pending);

        assertTrue(request.hasPendingRequote());
        assertEquals(130000L, request.getRequote().getPrice());
        assertEquals("otro", request.getRequote().getReasonCode());
        assertEquals("Había que cambiar el tubo", request.getRequote().getReasonText());
        // El precio vigente no cambia hasta que el cliente acepta
        assertEquals(95000L, request.getQuoted_price());
        // Sin el nodo no hay re-cotización
        assertNull(request("status", "in_progress").getRequote());
    }
}
