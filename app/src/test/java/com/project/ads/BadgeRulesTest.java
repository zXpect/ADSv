package com.project.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.project.ads.helpers.UnreadNotificationsWatcher;
import com.project.ads.helpers.WorkerRequestsBadge;
import com.project.ads.models.ServiceRequest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Insignia de solicitudes nuevas del trabajador y texto de los conteos. */
public class BadgeRulesTest {

    private static ServiceRequest request(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put((String) keyValues[i], keyValues[i + 1]);
        return ServiceRequest.fromMap(map);
    }

    @Test
    public void pendingAndAcceptedWithoutQuoteAreNew() {
        assertTrue(WorkerRequestsBadge.isNew(request("status", "pending")));
        assertTrue(WorkerRequestsBadge.isNew(request("status", "accepted")));
    }

    @Test
    public void quotedOrStartedAreNotNew() {
        assertFalse(WorkerRequestsBadge.isNew(request("status", "accepted",
                "quoted_price", 90000L, "quote_status", "pending")));
        for (String status : new String[]{"on_the_way", "in_progress", "completed", "cancelled"}) {
            assertFalse(status, WorkerRequestsBadge.isNew(request("status", status)));
        }
        assertFalse(WorkerRequestsBadge.isNew(request()));
    }

    @Test
    public void badgeLabel() {
        assertEquals("", UnreadNotificationsWatcher.badgeLabel(0));
        assertEquals("1", UnreadNotificationsWatcher.badgeLabel(1));
        assertEquals("9", UnreadNotificationsWatcher.badgeLabel(9));
        assertEquals("9+", UnreadNotificationsWatcher.badgeLabel(10));
    }
}
