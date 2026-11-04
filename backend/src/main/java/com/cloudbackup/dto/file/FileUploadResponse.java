package com.cloudbackup.dto.file;

import java.util.List;

public class FileUploadResponse {

    private int uploadedCount;
    private long totalSizeBytes;
    private List<FileMetadataResponse> files;

    public FileUploadResponse() {
    }

    public FileUploadResponse(int uploadedCount, long totalSizeBytes, List<FileMetadataResponse> files) {
        this.uploadedCount = uploadedCount;
        this.totalSizeBytes = totalSizeBytes;
        this.files = files;
    }

    public int getUploadedCount() {
        return uploadedCount;
    }

    public void setUploadedCount(int uploadedCount) {
        this.uploadedCount = uploadedCount;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public List<FileMetadataResponse> getFiles() {
        return files;
    }

    public void setFiles(List<FileMetadataResponse> files) {
        this.files = files;
    }
}
