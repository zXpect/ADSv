package com.project.ads.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Consentimiento del registro (D18, D20, HU-02): versión de los Términos y
 * Condiciones y del aviso de privacidad aceptados, y cuándo. Se guarda en el
 * nodo del usuario como consent = {termsVersion, privacyVersion, acceptedAt}.
 * Las reglas no dejan borrarlo ni moverlo hacia atrás: después del registro,
 * actualizar el nodo con updateChildren, nunca con setValue sin consent.
 */
public final class ConsentHelper {

    // Subirlas cuando cambie el texto de res/raw/legal_terms.txt o legal_privacy.txt
    // (la pantalla LegalActivity muestra estas versiones)
    public static final String TERMS_VERSION = "1.1";
    public static final String PRIVACY_VERSION = "1.0";

    /** Extra con el momento de la aceptación, entre los pasos del registro del trabajador. */
    public static final String EXTRA_ACCEPTED_AT = "consent_accepted_at";

    private ConsentHelper() {
    }

    public static Map<String, Object> consent(long acceptedAt) {
        Map<String, Object> consent = new HashMap<>();
        consent.put("termsVersion", TERMS_VERSION);
        consent.put("privacyVersion", PRIVACY_VERSION);
        consent.put("acceptedAt", acceptedAt);
        return consent;
    }
}
