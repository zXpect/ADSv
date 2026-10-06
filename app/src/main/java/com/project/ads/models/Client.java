package com.project.ads.models;

public class Client {
    private String id;
    private String name;
    private String lastName;
    private String email;
    private String image;
    private String phone;

    public Client() {
    }

    public Client(String id, String name, String lastName, String email) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
    }


    public Client(String id, String name, String lastName, String email, String phone) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }


    public Client(String id, String name, String lastName, String email, String phone, String image) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    public String getFormattedPhone() {
        if (!hasPhone()) {
            return "No disponible";
        }
        // Si tiene el formato +57XXXXXXXXXX
        if (phone.startsWith("+57")) {
            return phone;
        }
        // Si no tiene el prefijo, agregarlo
        return "+57 " + phone;
    }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();

        if (name != null && !name.trim().isEmpty()) {
            fullName.append(name.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }

        return fullName.length() > 0 ? fullName.toString() : "Cliente";
    }

    @Override
    public String toString() {
        return "Client{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", hasImage=" + (image != null && !image.isEmpty()) +
                '}';
    }
}