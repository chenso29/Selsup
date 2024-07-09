package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    public static void main(String[] args) throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
        crptApi.createDocument(new Document(), "signature");
    }

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock;
    private final ScheduledExecutorService scheduler;
    private final int requestLimit;
    private final long interval;
    private int requestCount;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.lock = new ReentrantLock();
        this.requestLimit = requestLimit;
        this.interval = timeUnit.toMillis(1);
        this.requestCount = 0;

        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                requestCount = 0;
            } finally {
                lock.unlock();
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws Exception {
        lock.lock();
        try {
            while (requestCount >= requestLimit) {
                lock.unlock();
                Thread.sleep(interval);
                lock.lock();
            }
            requestCount++;
        } finally {
            lock.unlock();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(document)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static class Document {
        public String description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}

