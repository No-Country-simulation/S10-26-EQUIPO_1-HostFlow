package com.hostflow.dto.response;

public record CloudinaryUploadResult(
        String publicId,
        String secureUrl,
        String format,
        long bytes,
        String folder
) {
}

