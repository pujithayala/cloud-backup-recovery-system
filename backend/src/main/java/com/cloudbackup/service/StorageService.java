package com.cloudbackup.service;

import java.io.InputStream;

public interface StorageService {

    void upload(String storageKey, InputStream inputStream, long size, String contentType);

    InputStream download(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);

    long getObjectSize(String storageKey);
}

