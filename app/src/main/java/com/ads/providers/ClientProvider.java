package com.ads.providers;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ads.models.Client;

import java.util.HashMap;
import java.util.Map;

public class ClientProvider {

    public final DatabaseReference mDataBase;
    private final StorageReference mStorage;

    private static final String STORAGE_PATH = "client_images";

    public ClientProvider() {
        mDataBase = FirebaseDatabase.getInstance().getReference().child("User").child("Clientes");
        mStorage = FirebaseStorage.getInstance().getReference().child(STORAGE_PATH);
    }

    public Task<Void> create(Client client){
        Map<String, Object> map = new HashMap<>();
        map.put("name", client.getName());
        map.put("lastName", client.getLastName());
        map.put("email", client.getEmail());

        // Incluir imagen si existe
        if (client.getImage() != null && !client.getImage().isEmpty()) {
            map.put("image", client.getImage());
        }

        return mDataBase.child(client.getId()).setValue(map);
    }

    /**
     * Sube una imagen de perfil a Firebase Storage
     * @param clientId ID del cliente
     * @param imageUri URI de la imagen local
     * @return UploadTask para monitorear el progreso
     */
    public UploadTask uploadProfileImage(String clientId, Uri imageUri) {
        String fileName = "profile_" + clientId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = mStorage.child(clientId).child(fileName);
        return imageRef.putFile(imageUri);
    }

    /**
     * Crea un cliente con imagen de perfil
     * @param client Cliente a crear
     * @param imageUri URI de la imagen (puede ser null)
     * @return Task con el resultado
     */
    public Task<Void> createWithImage(Client client, Uri imageUri) {
        if (imageUri == null) {
            // Si no hay imagen, crear cliente directamente
            return create(client);
        }

        // Subir imagen primero
        return uploadProfileImage(client.getId(), imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Obtener URL de descarga
                    return task.getResult().getStorage().getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Guardar URL en el objeto cliente
                    client.setImage(task.getResult().toString());
                    // Crear el cliente en la base de datos
                    return create(client);
                });
    }

    // Método para obtener los datos de un cliente
    public Task<DataSnapshot> getClient(String clientId) {
        return mDataBase.child(clientId).get();
    }

    // Método para actualizar la imagen de perfil
    public Task<Void> updateImage(String clientId, String imageUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("image", imageUrl);
        return mDataBase.child(clientId).updateChildren(map);
    }

    /**
     * Actualiza la imagen de perfil (sube nueva imagen y actualiza DB)
     * @param clientId ID del cliente
     * @param imageUri URI de la nueva imagen
     * @return Task con el resultado
     */
    public Task<Void> updateProfileImage(String clientId, Uri imageUri) {
        return uploadProfileImage(clientId, imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().getStorage().getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return updateImage(clientId, task.getResult().toString());
                });
    }

    // Método para actualizar el nombre
    public Task<Void> updateName(String clientId, String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        return mDataBase.child(clientId).updateChildren(map);
    }

    // Método para actualizar el apellido
    public Task<Void> updateLastName(String clientId, String lastName) {
        Map<String, Object> map = new HashMap<>();
        map.put("lastName", lastName);
        return mDataBase.child(clientId).updateChildren(map);
    }

    // Método para actualizar todos los datos del cliente
    public Task<Void> updateClient(String clientId, Map<String, Object> data) {
        return mDataBase.child(clientId).updateChildren(data);
    }

    /**
     * Elimina la imagen de perfil del Storage
     * @param clientId ID del cliente
     * @param imageUrl URL de la imagen a eliminar
     * @return Task con el resultado
     */
    public Task<Void> deleteProfileImage(String clientId, String imageUrl) {
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        return imageRef.delete();
    }

    // Getters
    public StorageReference getStorage() {
        return mStorage;
    }
}