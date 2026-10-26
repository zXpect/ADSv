package com.project.ads.providers;

/**
 * Resultado de una llamada al backend. Retrofit invoca estos métodos en el hilo
 * principal, así que se puede actualizar la UI directamente.
 */
public interface ApiCallback<T> {
    void onSuccess(T data);

    /**
     * @param message    mensaje para mostrar al usuario
     * @param httpStatus código HTTP, o 0 si no hubo respuesta (sin conexión)
     */
    void onError(String message, int httpStatus);
}
