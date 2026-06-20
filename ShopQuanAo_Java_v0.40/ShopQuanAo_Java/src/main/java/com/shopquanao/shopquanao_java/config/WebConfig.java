package com.shopquanao.shopquanao_java.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// =========================================================================================================================
// CẤU HÌNH ĐỊNH TUYẾN TÀI NGUYÊN TĨNH

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // ---------------------------------------------------------------------------------------------------------------------
    // Bộ xử lý nạp tài nguyên Real-time
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Lấy đường dẫn vật lý tuyệt đối đến thư mục chứa ảnh sản phẩm trong dự án
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/assets/img/products/";

        // Khi trình duyệt gọi link /assets/img/products/**, hệ thống sẽ ánh xạ nạp thẳng file từ ổ đĩa máy chủ
        registry.addResourceHandler("/assets/img/products/**")
                .addResourceLocations("file:" + uploadDir);
    }
}

// =========================================================================================================================
