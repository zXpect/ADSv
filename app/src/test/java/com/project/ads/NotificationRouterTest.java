package com.project.ads;

import static org.junit.Assert.assertEquals;

import com.project.ads.activities.NotificationsActivity;
import com.project.ads.activities.PqrDetailActivity;
import com.project.ads.activities.PqrListActivity;
import com.project.ads.activities.client.ServiceRequestDetailActivity;
import com.project.ads.activities.worker.RequestDetailActivity;
import com.project.ads.activities.worker.ServiceDetailWorkerActivity;
import com.project.ads.helpers.NotificationRouter;

import org.junit.Test;

/** Tabla de destinos de PENDIENTES_SIGUIENTES.md §1.2. */
public class NotificationRouterTest {

    private static final String REQ = "-OGq3VtR8mKx2PzL7aNc";

    private static Class<?> worker(String type) {
        return NotificationRouter.destination("trabajador", type, REQ);
    }

    private static Class<?> client(String type) {
        return NotificationRouter.destination("cliente", type, REQ);
    }

    @Test
    public void workerTypes() {
        assertEquals(RequestDetailActivity.class, worker("new_request"));
        for (String type : new String[]{"quote_accepted", "quote_rejected", "request_cancelled", "payment_approved",
                "requote_accepted", "requote_rejected"}) {
            assertEquals(type, ServiceDetailWorkerActivity.class, worker(type));
        }
    }

    @Test
    public void clientTypes() {
        for (String type : new String[]{"request_accepted", "request_rejected", "quote_received", "request_rescheduled",
                "status_change", "request_cancelled", "cash_received", "requote_received"}) {
            assertEquals(type, ServiceRequestDetailActivity.class, client(type));
        }
    }

    @Test
    public void withoutRequestOrUnknownTypeGoesToNotifications() {
        assertEquals(NotificationsActivity.class, NotificationRouter.destination("cliente", "quote_received", null));
        assertEquals(NotificationsActivity.class, NotificationRouter.destination("trabajador", "new_request", ""));
        assertEquals(NotificationsActivity.class, client("tipo_nuevo"));
        assertEquals(NotificationsActivity.class, worker(null));
        // Un tipo del otro rol no abre una pantalla que no le corresponde
        assertEquals(NotificationsActivity.class, client("new_request"));
        assertEquals(NotificationsActivity.class, worker("quote_received"));
        assertEquals(NotificationsActivity.class, NotificationRouter.destination("", "quote_received", REQ));
    }

    @Test
    public void pqrAnsweredOpensPqrList() {
        assertEquals(PqrListActivity.class, worker("pqr_answered"));
        assertEquals(PqrListActivity.class, client("pqr_answered"));
        assertEquals(PqrListActivity.class, NotificationRouter.destination("cliente", "pqr_answered", null));
    }

    @Test
    public void pqrAnsweredWithIdOpensTheDetail() {
        assertEquals(PqrDetailActivity.class, NotificationRouter.destination("cliente", "pqr_answered", REQ, "12"));
        assertEquals(PqrDetailActivity.class, NotificationRouter.destination("trabajador", "pqr_answered", REQ, "12"));
        assertEquals(PqrListActivity.class, NotificationRouter.destination("cliente", "pqr_answered", REQ, ""));
        // Los demás tipos no cambian
        assertEquals(ServiceRequestDetailActivity.class, NotificationRouter.destination("cliente", "quote_received", REQ, "12"));
    }

    @Test
    public void remindersOpenTheRequest() {
        // HU-17: service_reminder a los dos; quote_pending al cliente; payment_pending a los dos
        assertEquals(ServiceDetailWorkerActivity.class, worker("service_reminder"));
        assertEquals(ServiceDetailWorkerActivity.class, worker("payment_pending"));
        assertEquals(ServiceRequestDetailActivity.class, client("service_reminder"));
        assertEquals(ServiceRequestDetailActivity.class, client("quote_pending"));
        assertEquals(ServiceRequestDetailActivity.class, client("payment_pending"));
        // quote_pending no le llega al trabajador
        assertEquals(NotificationsActivity.class, worker("quote_pending"));
    }
}
