package com.handicraft.server.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /** Upload a MultipartFile. resource_type "auto" lets Cloudinary accept both images and videos. */
    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    /** Upload a base64 data-URL string (e.g. "data:image/jpeg;base64,/9j/4AAQ...") directly to Cloudinary. */
    public String uploadBase64Image(String base64DataUrl) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    base64DataUrl,
                    ObjectUtils.asMap("resource_type", "image")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Base64 image upload failed", e);
        }
    }

    /**
     * Delete an image from Cloudinary by its secure URL.
     * Extracts the public_id from a URL like:
     *   https://res.cloudinary.com/<cloud>/image/upload/v1234567890/abcdef.jpg
     */
    public boolean deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;
        try {
            String publicId = extractPublicId(imageUrl);
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            System.err.println("Cloudinary delete failed for: " + imageUrl + " — " + e.getMessage());
            return false;
        }
    }

    private String extractPublicId(String url) {
        int queryIdx = url.indexOf('?');
        if (queryIdx != -1) url = url.substring(0, queryIdx);

        String marker = "/upload/";
        int uploadIdx = url.lastIndexOf(marker);
        if (uploadIdx == -1) {
            throw new IllegalArgumentException("Cannot extract public_id from URL: " + url);
        }

        String tail = url.substring(uploadIdx + marker.length());

        if (tail.matches("^v\\d+/.*")) {
            tail = tail.substring(tail.indexOf('/') + 1);
        }

        int dotIdx = tail.lastIndexOf('.');
        if (dotIdx != -1) {
            tail = tail.substring(0, dotIdx);
        }

        return tail;
    }
}
