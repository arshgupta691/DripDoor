package com.dripdoor.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dripdoor.config.AppConfig;

import java.util.Map;

/**
 * Provides Cloudinary image URL generation for product assets.
 * Upload is handled outside the app; this service only builds delivery URLs.
 */
public class CloudinaryService {

    private static final Cloudinary CLD = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", AppConfig.CLOUDINARY_CLOUD,
            "api_key",    "placeholder",   // read-only URL generation — no key needed
            "api_secret", "placeholder",
            "secure",     true
    ));

    /**
     * Generates a transformation URL for the given public ID.
     *
     * @param publicId   e.g. "products/sapphire_ring"
     * @param width      desired image width in px
     * @param height     desired image height in px
     * @return the full HTTPS CDN URL
     */
    @SuppressWarnings("unchecked")
    public static String getImageUrl(String publicId, int width, int height) {
        try {
            return CLD.url().transformation(
                    new com.cloudinary.Transformation()
                            .width(width).height(height)
                            .crop("fill").gravity("auto")
                            .quality("auto").fetchFormat("auto")
            ).generate(publicId);
        } catch (Exception e) {
            // Fallback: build a direct URL without transformation
            return AppConfig.CLOUDINARY_BASE_URL + "w_" + width + ",h_" + height
                    + ",c_fill/" + publicId;
        }
    }

    /**
     * Convenience overload — returns a 400×400 square thumbnail.
     */
    public static String getThumbnailUrl(String publicId) {
        return getImageUrl(publicId, 400, 400);
    }
}
