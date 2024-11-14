package com.ads.models;

public class Worker {
    String id;
    String name;
    String lastName;
    String email;
    String work;
    private String fcmToken;
    private boolean isAvailable;

    public Worker() {
    }

    public Worker(String id, String name, String lastName, String email, String work) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.work = work;
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

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }
}
