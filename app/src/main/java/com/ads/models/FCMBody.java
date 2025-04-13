package com.ads.models;

import java.util.Map;


public class FCMBody {
    private Message message;

    public FCMBody(String token, String priority, Map<String, String> data) {
        this.message = new Message(token, priority, data);
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public static class Message {
        private String token;
        private Android android;
        private Map<String, String> data;

        public Message(String token, String priority, Map<String, String> data) {
            this.token = token;
            this.android = new Android(priority);
            this.data = data;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Android getAndroid() {
            return android;
        }

        public void setAndroid(Android android) {
            this.android = android;
        }

        public Map<String, String> getData() {
            return data;
        }

        public void setData(Map<String, String> data) {
            this.data = data;
        }
    }

    public static class Android {
        private String priority;

        public Android(String priority) {
            this.priority = priority;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }
}
