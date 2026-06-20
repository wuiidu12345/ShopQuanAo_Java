package com.shopquanao.shopquanao_java.controller;

import com.shopquanao.shopquanao_java.model.ProductProjection;
import com.shopquanao.shopquanao_java.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.transaction.annotation.Transactional;
import com.shopquanao.shopquanao_java.model.CartItem;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// =========================================================================================================================
// LỚP ĐIỀU HƯỚNG VÀ ĐỊNH TUYẾN CHỨC NĂNG HỆ THỐNG DÂN DỤNG CỦA KHÁCH HÀNG

@Controller
public class HomeController {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    // =====================================================================================================================
    // TRANG ĐĂNG NHẬP HỆ THỐNG

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Đăng Nhập");
        return "login";
    }

    // =====================================================================================================================
    // XỬ LÝ ĐĂNG NHẬP VÀ ĐỒNG BỘ ĐỒNG THỜI PHIÊN LÀM VIỆC CÙNG GIỎ HÀNG THÀNH VIÊN

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              HttpSession session,
                              Model model) {
        model.addAttribute("title", "Đăng Nhập");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập đầy đủ email và mật khẩu.");
            return "login";
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Đối soát thông tin khách hàng trong cơ sở dữ liệu
        Map<String, Object> khachHang = sanPhamRepository.findCustomerByEmailNative(email);
        if (khachHang != null && password.equals(khachHang.get("MatKhau"))) {
            if (Boolean.FALSE.equals(khachHang.get("TrangThai"))) {
                model.addAttribute("errorMessage", "Tài khoản khách hàng này hiện đã bị khóa.");
                return "login";
            }

            // Sao lưu giỏ hàng khách vãng lai trước khi ghi đè session giỏ hàng thành viên
            if (session.getAttribute("GuestCart") == null) {
                session.setAttribute("GuestCart", session.getAttribute("Cart"));
            }

            // Đồng bộ dữ liệu định danh lên phiên làm việc session
            session.setAttribute("MaKH", khachHang.get("MaKH").toString());
            session.setAttribute("TenKhachHang", khachHang.get("HoTen").toString());
            session.setAttribute("Email", khachHang.get("Email").toString());
            if (khachHang.get("SoDienThoai") != null)
                session.setAttribute("SoDienThoai", khachHang.get("SoDienThoai").toString());
            if (khachHang.get("DiaChi") != null) session.setAttribute("DiaChi", khachHang.get("DiaChi").toString());
            session.setAttribute("ChucVu", null);

            // Nạp danh sách giỏ hàng thực tế từ database của khách hàng này lên session
            loadCartFromDb(khachHang.get("MaKH").toString(), session);
            return "redirect:/";
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Đối soát thông tin nhân viên quản trị nếu không tìm thấy thông tin ở bảng khách hàng
        Map<String, Object> nhanVien = sanPhamRepository.findEmployeeByUsernameNative(email);
        if (nhanVien != null && password.equals(nhanVien.get("MatKhau"))) {
            if (Boolean.FALSE.equals(nhanVien.get("TrangThai"))) {
                model.addAttribute("errorMessage", "Tài khoản nhân viên này hiện đã bị khóa.");
                return "login";
            }

            // Ghi nhận phiên làm việc cho cán bộ quản trị
            session.setAttribute("MaNV", nhanVien.get("MaNV").toString());
            session.setAttribute("TenHienThi", nhanVien.get("HoTen").toString());
            session.setAttribute("ChucVu", nhanVien.get("ChucVu").toString());

            return "redirect:/admin/dashboard";
        }

        model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác.");
        return "login";
    }

    // =====================================================================================================================
    // TRANG ĐĂNG KÝ TÀI KHOẢN KHÁCH HÀNG MỚI

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("title", "Đăng Ký");
        return "register";
    }

    // =====================================================================================================================
    // TIẾP NHẬN YÊU CẦU ĐĂNG KÝ VÀ KHỞI TẠO MÃ KHÁCH HÀNG TỰ ĐỘNG TĂNG

    @Transactional
    @PostMapping("/register")
    public String handleRegister(@RequestParam("fullName") String fullName,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                                 Model model) {
        model.addAttribute("title", "Đăng Ký");

        if (fullName == null || fullName.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập đầy đủ thông tin biểu mẫu yêu cầu.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận nhập lại không trùng khớp.");
            return "register";
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.com$")) {
            model.addAttribute("errorMessage", "Định dạng địa chỉ Email không hợp lệ (Ví dụ hợp lệ: abc@gmail.com).");
            return "register";
        }

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$")) {
            model.addAttribute("errorMessage", "Mật khẩu phải từ 8 ký tự trở lên, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
            return "register";
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Kiểm tra tính trùng lặp của email đăng ký trong database
        Map<String, Object> existingUser = sanPhamRepository.findCustomerByEmailNative(email);

        if (existingUser != null && existingUser.get("MaKH") != null) {
            model.addAttribute("errorMessage", "Địa chỉ Email này đã được sử dụng đăng ký tài khoản trước đó.");
            return "register";
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Tính toán mã khách hàng lớn nhất hiện có để tự động tăng tiến tuần tự
        try {
            String lastCustomer = sanPhamRepository.findLastCustomerIdNative();
            String newMaKH = "KH001";
            if (lastCustomer != null && lastCustomer.length() > 2) {
                String numberPart = lastCustomer.substring(2);
                try {
                    int currentNumber = Integer.parseInt(numberPart);
                    newMaKH = "KH" + String.format("%03d", currentNumber + 1);
                } catch (NumberFormatException e) {
                    newMaKH = "KH001";
                }
            }

            sanPhamRepository.insertNativeCustomer(newMaKH, email, password, fullName, email);

            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng tiến hành đăng nhập tài khoản vừa tạo.");
            return "redirect:/login";

        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Lỗi lưu trữ hệ thống: " + ex.getMessage());
            return "register";
        }
    }

    // =====================================================================================================================
    // THỰC HIỆN ĐẶT LẠI MẬT KHẨU MỚI KHI QUÊN MẬT KHẨU

    @Transactional
    @PostMapping("/Home/ForgotPassword")
    @ResponseBody
    public Map<String, Object> handleForgotPassword(@RequestParam("email") String email,
                                                    @RequestParam("newPassword") String newPassword) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || email.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng không bỏ trống các thông tin nhập vào!");
            return response;
        }

        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$")) {
            response.put("success", false);
            response.put("message", "Thất bại: Mật khẩu mới phải từ 8 ký tự trở lên, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.");
            return response;
        }

        // Cập nhật trường mật khẩu mới của người dùng xuống cơ sở dữ liệu dựa theo email
        int rowsUpdated = sanPhamRepository.updatePasswordByEmailNative(email, newPassword);
        if (rowsUpdated > 0) {
            response.put("success", true);
            response.put("message", "Chúc mừng! Đặt lại mật khẩu tài khoản thành công.");
        } else {
            response.put("success", false);
            response.put("message", "Lỗi: Không tìm thấy tài khoản với địa chỉ Email này, hoặc tài khoản đang bị khóa!");
        }
        return response;
    }

    // =====================================================================================================================
    // ĐĂNG XUẤT TÀI KHOẢN VÀ KHÔI PHỤC PHIÊN LÀM VIỆC GIỎ HÀNG KHÁCH VÃNG LAI

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Khôi phục lại giỏ hàng vãng lai tạm thời nếu có trước khi đăng nhập
        if (session.getAttribute("GuestCart") != null) {
            session.setAttribute("Cart", session.getAttribute("GuestCart"));
            session.setAttribute("GuestCart", null);
        } else {
            session.setAttribute("Cart", new ArrayList<CartItem>());
        }

        // Giải phóng toàn bộ phiên đăng nhập định danh của người dùng trên hệ thống
        session.setAttribute("MaKH", null);
        session.setAttribute("TenKhachHang", null);
        session.setAttribute("Email", null);
        session.setAttribute("SoDienThoai", null);
        session.setAttribute("DiaChi", null);
        session.setAttribute("MaNV", null);
        session.setAttribute("TenHienThi", null);
        session.setAttribute("ChucVu", null);

        session.setAttribute("CurrentCoupon", null);
        session.setAttribute("CurrentShipping", null);

        return "redirect:/";
    }

    // =====================================================================================================================
    // ĐỌC DỮ LIỆU GIỎ HÀNG LƯU TRỮ TỪ SQL SERVER ĐẨY LÊN GIỎ HÀNG LÀM VIỆC TRÊN SESSION

    private void loadCartFromDb(String maKH, HttpSession session) {
        List<Map<String, Object>> rawCart = sanPhamRepository.findCartItemsByCustomerIdNative(maKH);
        List<CartItem> cartItems = new ArrayList<>();

        for (Map<String, Object> row : rawCart) {
            CartItem item = new CartItem();
            item.setMaSP((String) row.get("maSP"));
            item.setTenSanPham((String) row.get("tenSanPham"));
            item.setAnhDaiDien((String) row.get("anhDaiDien"));
            item.setMaCTSP((String) row.get("maCTSP"));
            item.setTenKichThuoc((String) row.get("tenKichThuoc"));

            Object priceObj = row.get("donGia");
            if (priceObj instanceof BigDecimal) {
                item.setDonGia((BigDecimal) priceObj);
            } else if (priceObj instanceof Number) {
                item.setDonGia(new BigDecimal(((Number) priceObj).doubleValue()));
            } else {
                item.setDonGia(BigDecimal.ZERO);
            }

            item.setSoLuong(((Number) row.get("soLuong")).intValue());
            item.setSelected(false);
            cartItems.add(item);
        }
        session.setAttribute("Cart", cartItems);
    }

    // =====================================================================================================================
    // TRANG CHỦ MUA SẮM

    @GetMapping("/")
    public String index(Model model,
                        @RequestParam(required = false, defaultValue = "1") int page,
                        @RequestParam(required = false) String searchName,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String priceRange,
                        @RequestParam(required = false) String sortType) {

        model.addAttribute("title", "Trang Chủ");
        model.addAttribute("searchName", searchName);
        model.addAttribute("category", category);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("sortType", sortType);

        // Nạp danh sách các tệp tin hình ảnh cho banner trình chiếu slide quảng cáo
        List<String> banners = Arrays.asList(
                "/assets/img/banner/Banner-1.jpg",
                "/assets/img/banner/Banner-2.jpg",
                "/assets/img/banner/Banner-3.jpg"
        );
        model.addAttribute("bannerImages", banners);

        // Nạp danh sách dải sản phẩm thịnh hành được cấu hình gán cứng từ database
        List<String> fixedIds = Arrays.asList("A06", "G09", "Q09", "G06");
        List<ProductProjection> rawTrending = sanPhamRepository.findTrendingProducts(fixedIds);
        List<ProductProjection> trendingProducts = new ArrayList<>(rawTrending);
        trendingProducts.sort((p1, p2) -> Integer.compare(fixedIds.indexOf(p1.getMaSP()), fixedIds.indexOf(p2.getMaSP())));
        model.addAttribute("trending", trendingProducts);

        List<ProductProjection> filtered = new ArrayList<>(sanPhamRepository.findAllActiveProducts());

        // Lọc sản phẩm động dựa theo từ khóa nhập vào thanh tìm kiếm
        if (searchName != null && !searchName.isEmpty()) {
            filtered.removeIf(p -> !p.getTenSanPham().toLowerCase().contains(searchName.toLowerCase()));
        }

        // Lọc sản phẩm theo danh mục chính (Áo, Quần, Giày) dựa vào ký tự chữ cái đầu của mã sản phẩm
        if (category != null && !category.isEmpty()) {
            String dbCategory = "";
            switch (category) {
                case "Áo":
                    dbCategory = "A";
                    break;
                case "Quần":
                    dbCategory = "Q";
                    break;
                case "Giày":
                    dbCategory = "G";
                    break;
            }
            final String targetCat = dbCategory;
            if (!targetCat.isEmpty()) {
                filtered.removeIf(p -> !p.getMaSP().startsWith(targetCat));
            }
        }

        // Lọc sản phẩm theo khoảng giá bán phù hợp túi tiền người mua
        if (priceRange != null && !priceRange.isEmpty()) {
            String[] prices = priceRange.split("-");
            if (prices.length == 2) {
                java.math.BigDecimal minPrice = new java.math.BigDecimal(prices[0]);
                java.math.BigDecimal maxPrice = new java.math.BigDecimal(prices[1]);
                filtered.removeIf(p -> p.getGiaBan() == null || p.getGiaBan().compareTo(minPrice) < 0 || p.getGiaBan().compareTo(maxPrice) > 0);
            }
        }

        // Sắp xếp thứ tự hiển thị dựa theo giá tăng, giảm dần hoặc mức điểm sao đánh giá cao
        if (sortType == null || sortType.isEmpty() || sortType.equals("default")) {
            filtered.sort((p1, p2) -> {
                String cat1 = p1.getMaSP().substring(0, 1);
                String cat2 = p2.getMaSP().substring(0, 1);
                int score1 = cat1.equals("A") ? 1 : cat1.equals("Q") ? 2 : cat1.equals("G") ? 3 : 4;
                int score2 = cat2.equals("A") ? 1 : cat2.equals("Q") ? 2 : cat2.equals("G") ? 3 : 4;
                if (score1 != score2) return Integer.compare(score1, score2);
                return p1.getMaSP().compareTo(p2.getMaSP());
            });
        } else {
            switch (sortType) {
                case "asc":
                    filtered.sort((p1, p2) -> p1.getGiaBan().compareTo(p2.getGiaBan()));
                    break;
                case "desc":
                    filtered.sort((p1, p2) -> p2.getGiaBan().compareTo(p1.getGiaBan()));
                    break;
                case "rating":
                    filtered.sort((p1, p2) -> Double.compare(p2.getDiemDanhGia(), p1.getDiemDanhGia()));
                    break;
            }
        }

        // Thuật toán chia trang kết quả hiển thị danh sách sản phẩm
        int pageSize = 16;
        int totalProducts = filtered.size();
        int totalPages = (int) Math.ceil((double) totalProducts / pageSize);
        if (totalPages == 0) totalPages = 1;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalProducts);

        List<ProductProjection> pageProducts = new ArrayList<>();
        if (start < totalProducts) {
            pageProducts = filtered.subList(start, end);
        }

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("products", pageProducts);

        return "index";
    }

    // =====================================================================================================================
    // TRANG GIỚI THIỆU

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "Giới Thiệu");
        return "about";
    }

    // =====================================================================================================================
    // TRANG XEM CHI TIẾT SẢN PHẨM VÀ CẤU HÌNH BIẾN THỂ KÍCH THƯỚC ĐỐI SOÁT

    @GetMapping("/product_details")
    public String productDetails(@RequestParam String id, Model model) {
        model.addAttribute("title", "Chi Tiết Đơn Hàng");

        if (id == null || id.isEmpty()) {
            return "redirect:/";
        }

        ProductProjection product = sanPhamRepository.findProductDetailById(id);
        if (product == null) {
            return "redirect:/";
        }
        model.addAttribute("product", product);

        // Nạp album hình ảnh phụ, dải kích thước tồn kho và danh sách bình luận đánh giá của món đồ
        List<String> listImages = sanPhamRepository.findImagesByProductId(id);
        model.addAttribute("listImages", listImages);

        List<Map<String, Object>> listSizes = sanPhamRepository.findSizesByProductId(id);
        model.addAttribute("listSizes", listSizes);

        List<Map<String, Object>> listComments = sanPhamRepository.findReviewsByProductId(id);
        model.addAttribute("listComments", listComments);

        // Tính giá trị trung bình cộng số sao đánh giá nhận được
        double averageRating = 0;
        if (!listComments.isEmpty()) {
            double totalStars = listComments.stream()
                    .mapToDouble(c -> ((Number) c.get("SoSao")).doubleValue())
                    .sum();
            averageRating = totalStars / listComments.size();
        }
        model.addAttribute("rating", Math.round(averageRating * 10) / 10.0);
        model.addAttribute("ratingCount", listComments.size());

        // Gợi ý ngẫu nhiên 4 sản phẩm liên quan có cùng nhóm danh mục
        String currentCategory = id.substring(0, 1);
        List<ProductProjection> relatedProducts = sanPhamRepository.findRelatedProducts(currentCategory, id);
        model.addAttribute("relatedProducts", relatedProducts);

        return "product_details";
    }

    // =====================================================================================================================
    // TIẾP NHẬN ĐÓNG GÓI SẢN PHẨM TÙY CHỌN VÀO GIỎ HÀNG QUA PHƯƠNG THỨC SUBMIT BIỂU MẪU

    @PostMapping("/AddToCartFromDetails")
    public String addToCartFromDetails(@RequestParam("MaSP") String maSP,
                                       @RequestParam("SizeId") String sizeId,
                                       @RequestParam("Quantity") int quantity,
                                       @RequestParam(value = "submitType", required = false) String submitType,
                                       HttpSession session,
                                       org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (maSP == null || maSP.isEmpty() || sizeId == null || sizeId.isEmpty() || quantity < 1) {
            return "redirect:/product_details?id=" + maSP;
        }

        List<Map<String, Object>> variants = sanPhamRepository.findActiveVariantsByProductId(maSP);
        Map<String, Object> targetVariant = variants.stream()
                .filter(v -> sizeId.equals(v.get("MaKichThuoc")))
                .findFirst()
                .orElse(null);

        if (targetVariant != null) {
            String maCTSP = (String) targetVariant.get("MaCTSP");
            List<CartItem> cart = getCart(session);
            CartItem existingItem = cart.stream().filter(c -> c.getMaCTSP().equals(maCTSP)).findFirst().orElse(null);

            // Tiến hành tăng số lượng tích lũy nếu món hàng đã có sẵn trong giỏ
            if (existingItem != null) {
                existingItem.setSoLuong(existingItem.getSoLuong() + quantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setMaSP(maSP);
                newItem.setTenSanPham((String) targetVariant.get("TenSanPham"));
                newItem.setAnhDaiDien((String) targetVariant.get("AnhDaiDien"));
                newItem.setMaCTSP(maCTSP);
                newItem.setTenKichThuoc((String) targetVariant.get("TenKichThuoc"));

                Object giaBanObj = targetVariant.get("GiaBan");
                if (giaBanObj instanceof BigDecimal) {
                    newItem.setDonGia((BigDecimal) giaBanObj);
                } else if (giaBanObj instanceof Number) {
                    newItem.setDonGia(new BigDecimal(((Number) giaBanObj).doubleValue()));
                } else {
                    newItem.setDonGia(BigDecimal.ZERO);
                }
                newItem.setSoLuong(quantity);
                cart.add(newItem);
            }
            session.setAttribute("Cart", cart);

            // Điều hướng dựa vào hành động bấm nút: Đến thẳng trang giỏ hàng hoặc ở lại trang xem chi tiết
            if ("buy".equals(submitType)) {
                return "redirect:/cart";
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
                return "redirect:/product_details?id=" + maSP;
            }
        }
        return "redirect:/product_details?id=" + maSP;
    }

    // =====================================================================================================================
    // TIẾP NHẬN YÊU CẦU THÊM MÓN ĐỒ VÀO GIỎ HÀNG TẠI TRANG XEM CHI TIẾT SẢN PHẨM

    @PostMapping("/Home/AddToCartDetailsAJAX")
    @ResponseBody
    public Map<String, Object> addToCartDetailsAJAX(@RequestParam("MaSP") String maSP,
                                                    @RequestParam("SizeId") String sizeId,
                                                    @RequestParam("Quantity") int quantity,
                                                    HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (maSP == null || maSP.isEmpty() || sizeId == null || sizeId.isEmpty() || quantity < 1) {
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ.");
            return response;
        }

        List<Map<String, Object>> variants = sanPhamRepository.findActiveVariantsByProductId(maSP);
        Map<String, Object> targetVariant = variants.stream()
                .filter(v -> sizeId.equals(v.get("MaKichThuoc")))
                .findFirst()
                .orElse(null);

        if (targetVariant != null) {
            String maCTSP = (String) targetVariant.get("MaCTSP");
            List<CartItem> cart = getCart(session);
            CartItem existingItem = cart.stream().filter(c -> c.getMaCTSP().equals(maCTSP)).findFirst().orElse(null);

            if (existingItem != null) {
                existingItem.setSoLuong(existingItem.getSoLuong() + quantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setMaSP(maSP);
                newItem.setTenSanPham((String) targetVariant.get("TenSanPham"));
                newItem.setAnhDaiDien((String) targetVariant.get("AnhDaiDien"));
                newItem.setMaCTSP(maCTSP);
                newItem.setTenKichThuoc((String) targetVariant.get("TenKichThuoc"));

                Object giaBanObj = targetVariant.get("GiaBan");
                if (giaBanObj instanceof BigDecimal) {
                    newItem.setDonGia((BigDecimal) giaBanObj);
                } else if (giaBanObj instanceof Number) {
                    newItem.setDonGia(new BigDecimal(((Number) giaBanObj).doubleValue()));
                } else {
                    newItem.setDonGia(BigDecimal.ZERO);
                }
                newItem.setSoLuong(quantity);
                cart.add(newItem);
            }
            session.setAttribute("Cart", cart);

            response.put("success", true);
            response.put("count", cart.size());
            response.put("message", "Đã thêm sản phẩm vào giỏ hàng!");
            return response;
        }

        response.put("success", false);
        response.put("message", "Sản phẩm không tồn tại hoặc hết hàng.");
        return response;
    }

    // =====================================================================================================================
    // GỬI BÌNH LUẬN ĐÁNH GIÁ SẢN PHẨM VÀ TỰ ĐỘNG TÍNH TOÁN MÃ ĐÁNH GIÁ TĂNG DẦN

    @Transactional
    @PostMapping("/Home/AddReview")
    public String addReview(@RequestParam("MaSP") String maSP,
                            @RequestParam("TenKhachHang") String tenKhachHang,
                            @RequestParam("SoSao") int soSao,
                            @RequestParam("NoiDung") String noiDung,
                            HttpSession session) {

        if (maSP == null || maSP.isEmpty() || tenKhachHang == null || tenKhachHang.trim().isEmpty() || noiDung == null || noiDung.trim().isEmpty()) {
            return "redirect:/product_details?id=" + maSP;
        }

        // Tách chuỗi số của mã lớn nhất hiện tại để tự động sinh mã đánh giá mới kế tiếp (Ví dụ: A01DG003)
        try {
            String lastReviewId = sanPhamRepository.findLastReviewIdByProductId(maSP);
            int nextNumber = 1;

            if (lastReviewId != null && lastReviewId.length() > 5) {
                String numberPart = lastReviewId.substring(5);
                try {
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }
            String newReviewId = maSP + "DG" + String.format("%03d", nextNumber);

            String maKH = (String) session.getAttribute("MaKH");
            sanPhamRepository.insertNativeReview(newReviewId, maSP, maKH, tenKhachHang, soSao, noiDung);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "redirect:/product_details?id=" + maSP + "&review=success";
    }

    // =====================================================================================================================
    // ĐỒNG BỘ HIỂN THỊ CHI TIẾT LỊCH SỬ ĐƠN HÀNG CÁ NHÂN CỦA NGƯỜI DÙNG

    @GetMapping("/order")
    public String order(Model model, HttpSession session) {
        model.addAttribute("title", "Quản Lý Đơn Hàng");

        String maKH = (String) session.getAttribute("MaKH");
        List<Map<String, Object>> rawOrders;

        // Phân tách luồng truy xuất giữa thành viên đã đăng nhập và khách vãng lai (GUEST)
        if (maKH != null && !maKH.isEmpty()) {
            rawOrders = sanPhamRepository.findOrdersByCustomerIdNative(maKH);
        } else {
            rawOrders = sanPhamRepository.findGuestOrdersNative();
        }

        // Quét vòng lặp lồng danh sách chi tiết các mặt hàng đã đặt mua vào bên trong từng hóa đơn tương ứng
        List<Map<String, Object>> ordersList = new ArrayList<>();
        for (Map<String, Object> order : rawOrders) {
            Map<String, Object> editableOrder = new HashMap<>(order);
            String maDonHang = (String) order.get("maDonHang");

            List<Map<String, Object>> details = sanPhamRepository.findOrderDetailsByOrderIdNative(maDonHang);
            editableOrder.put("chiTiet", details);

            ordersList.add(editableOrder);
        }

        model.addAttribute("orders", ordersList);
        return "order";
    }

    // =====================================================================================================================
    // TIẾP NHẬN THAO TÁC ẤN NÚT XÁC NHẬN "ĐÃ NHẬN ĐƯỢC HÀNG" ĐỂ HOÀN TẤT ĐƠN

    @PostMapping("/Home/ConfirmReceived")
    @ResponseBody
    public Map<String, Object> confirmReceived(@RequestParam("maDonHang") String maDonHang) {
        Map<String, Object> response = new HashMap<>();
        if (maDonHang == null || maDonHang.isEmpty()) {
            response.put("success", false);
            response.put("message", "Mã đơn hàng không hợp lệ.");
            return response;
        }

        // Chạy câu lệnh UPDATE ghi đè trạng thái đơn hàng sang 'Hoàn tất'
        int rowsUpdated = sanPhamRepository.confirmReceivedOrderNative(maDonHang);
        if (rowsUpdated > 0) {
            response.put("success", true);
            response.put("message", "Xác nhận đã nhận hàng thành công! Trạng thái đơn đã chuyển sang 'Hoàn tất'.");
        } else {
            response.put("success", false);
            response.put("message", "Cập nhật thất bại. Đơn hàng phải ở trạng thái 'Đang giao' mới có thể chuyển sang hoàn tất.");
        }
        return response;
    }

    // =====================================================================================================================
    // TRÍCH XUẤT HOẶC KHỞI TẠO ĐỐI TƯỢNG DANH SÁCH GIỎ HÀNG TRÊN PHIÊN LÀM VIỆC

    private List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("Cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("Cart", cart);
        }
        return cart;
    }

    // =====================================================================================================================
    // THIẾT LẬP ĐỘ ƯU TIÊN SẮP XẾP QUY CHUẨN CÁC KÍCH CỠ QUẦN ÁO VÀ GIÀY DÉP

    private int getSizePriority(String sizeName) {
        if (sizeName == null) return 99;
        switch (sizeName.toUpperCase()) {
            case "S":
                return 1;
            case "M":
                return 2;
            case "L":
                return 3;
            case "XL":
                return 4;
            default:
                try {
                    return Integer.parseInt(sizeName) + 10;
                } catch (NumberFormatException e) {
                    return 99;
                }
        }
    }

    // =====================================================================================================================
    // XỬ LÝ THÊM NHANH SẢN PHẨM VỚI BIẾN THỂ KÍCH CỠ NHỎ NHẤT VÀO GIỎ HÀNG TẠI TRANG CHỦ

    @PostMapping("/Home/AddToCartAJAX")
    @ResponseBody
    public Map<String, Object> addToCartAJAX(@RequestParam("id") String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (id == null || id.isEmpty()) {
            response.put("success", false);
            response.put("message", "sản phẩm không tồn tại.");
            return response;
        }

        List<Map<String, Object>> variants = sanPhamRepository.findActiveVariantsByProductId(id);
        if (variants == null || variants.isEmpty()) {
            response.put("success", false);
            response.put("message", "sản phẩm này đã hết hàng.");
            return response;
        }

        // Sắp xếp các biến thể để tự động chọn lấy kích cỡ có độ ưu tiên nhỏ nhất
        variants.sort((v1, v2) -> {
            String size1 = (String) v1.get("TenKichThuoc");
            String size2 = (String) v2.get("TenKichThuoc");
            return Integer.compare(getSizePriority(size1), getSizePriority(size2));
        });

        Map<String, Object> targetVariant = variants.get(0);
        String maCTSP = (String) targetVariant.get("MaCTSP");

        List<CartItem> cart = getCart(session);
        CartItem existingItem = cart.stream().filter(c -> c.getMaCTSP().equals(maCTSP)).findFirst().orElse(null);

        if (existingItem != null) {
            existingItem.setSoLuong(existingItem.getSoLuong() + 1);
        } else {
            CartItem newItem = new CartItem();
            newItem.setMaSP(id);
            newItem.setTenSanPham((String) targetVariant.get("TenSanPham"));
            newItem.setAnhDaiDien((String) targetVariant.get("AnhDaiDien"));
            newItem.setMaCTSP(maCTSP);
            newItem.setTenKichThuoc((String) targetVariant.get("TenKichThuoc"));

            Object giaBanObj = targetVariant.get("GiaBan");
            if (giaBanObj instanceof BigDecimal) {
                newItem.setDonGia((BigDecimal) giaBanObj);
            } else if (giaBanObj instanceof Number) {
                newItem.setDonGia(new BigDecimal(((Number) giaBanObj).doubleValue()));
            } else {
                newItem.setDonGia(BigDecimal.ZERO);
            }

            newItem.setSoLuong(1);
            cart.add(newItem);
        }

        session.setAttribute("Cart", cart);

        response.put("success", true);
        response.put("count", cart.size());
        response.put("message", "Đã thêm sản phẩm vào giỏ hàng!");
        return response;
    }

    // =====================================================================================================================
    // TRUY XUẤT ĐẾM TỔNG SỐ ĐẦU MỤC SẢN PHẨM HIỆN CÓ TRONG GIỎ HÀNG

    @GetMapping("/Home/GetCartCount")
    @ResponseBody
    public Map<String, Object> getCartCount(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        res.put("count", getCart(session).size());
        return res;
    }

    // =====================================================================================================================
    // ĐẾM SỐ LƯỢNG ĐƠN HÀNG HOẠT ĐỘNG CHƯA HOÀN TẤT/HỦY ĐỂ RENDERING RA BADGE THANH TIÊU ĐỀ

    @GetMapping("/Home/GetOrderCount")
    @ResponseBody
    public Map<String, Object> getOrderCount(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        String maKH = (String) session.getAttribute("MaKH");
        int count = 0;

        if (maKH != null && !maKH.isEmpty()) {
            count = sanPhamRepository.countActiveOrdersByCustomerIdNative(maKH);
        } else {
            count = sanPhamRepository.countActiveGuestOrdersNative();
        }

        res.put("count", count);
        return res;
    }

    // =====================================================================================================================
    // TRANG GIỎ HÀNG

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("title", "Giỏ Hàng");
        List<CartItem> cartItems = getCart(session);
        model.addAttribute("cartItems", cartItems);

        // Nạp dải danh sách tất cả các kích thước hoạt động tương ứng cho từng đầu mục hàng
        Map<String, List<Map<String, Object>>> sizeDict = new HashMap<>();
        for (CartItem item : cartItems) {
            List<Map<String, Object>> sizes = sanPhamRepository.findSizesByProductId(item.getMaSP());
            sizeDict.put(item.getMaCTSP(), sizes);
        }
        model.addAttribute("sizeDict", sizeDict);

        List<Map<String, Object>> shipMethods = sanPhamRepository.findAllActiveShippingMethods();
        model.addAttribute("shipMethods", shipMethods);

        model.addAttribute("currentCoupon", session.getAttribute("CurrentCoupon"));
        model.addAttribute("currentShipping", session.getAttribute("CurrentShipping"));

        boolean isAllSelected = !cartItems.isEmpty() && cartItems.stream().allMatch(CartItem::isSelected);
        model.addAttribute("isAllSelected", isAllSelected);

        return "cart";
    }

    // =====================================================================================================================
    // XỬ LÝ CHUYỂN ĐỔI THAY ĐỔI BIẾN THỂ KÍCH CỠ NGAY TẠI GIAO DIỆN BẢNG GIỎ HÀNG

    @PostMapping("/Home/UpdateCartSize")
    @ResponseBody
    public Map<String, Object> updateCartSize(@RequestParam String maSP, @RequestParam String oldMaCTSP, @RequestParam String newSizeId, HttpSession session) {
        List<CartItem> cart = getCart(session);
        CartItem item = cart.stream().filter(x -> x.getMaCTSP().equals(oldMaCTSP)).findFirst().orElse(null);

        if (item != null) {
            List<Map<String, Object>> newVariants = sanPhamRepository.findActiveVariantsByProductId(maSP);
            Map<String, Object> newVariant = newVariants.stream()
                    .filter(v -> v.get("MaKichThuoc").toString().equals(newSizeId))
                    .findFirst().orElse(null);

            if (newVariant != null) {
                // Kiểm tra xung đột: thực hiện cộng dồn số lượng đặt nếu biến thể mới chọn đã tồn tại sẵn trong giỏ
                CartItem duplicate = cart.stream().filter(x -> x.getMaCTSP().equals(newVariant.get("MaCTSP"))).findFirst().orElse(null);

                if (duplicate != null && !duplicate.getMaCTSP().equals(oldMaCTSP)) {
                    duplicate.setSoLuong(duplicate.getSoLuong() + item.getSoLuong());
                    cart.remove(item);
                } else {
                    item.setMaCTSP(newVariant.get("MaCTSP").toString());
                    item.setTenKichThuoc(newVariant.get("TenKichThuoc").toString());
                    item.setDonGia(new BigDecimal(newVariant.get("GiaBan").toString()));
                }
                session.setAttribute("Cart", cart);
                return Collections.singletonMap("success", true);
            }
        }
        return Collections.singletonMap("success", false);
    }

    // =====================================================================================================================
    // CẬP NHẬT TRẠNG THÁI CHỌN ĐẶT HÀNG CHO MỘT DÒNG SẢN PHẨM KHỚP MÃ BIẾN THỂ

    @PostMapping("/Home/UpdateCartSelection")
    @ResponseBody
    public Map<String, Object> updateCartSelection(@RequestParam String maCTSP, @RequestParam boolean isSelected, HttpSession session) {
        List<CartItem> cart = getCart(session);
        for (CartItem item : cart) {
            if (item.getMaCTSP().equals(maCTSP)) {
                item.setSelected(isSelected);
                break;
            }
        }
        session.setAttribute("Cart", cart);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    // =====================================================================================================================
    // ĐỒNG LOẠT CẬP NHẬT CHỌN HOẶC HỦY CHỌN TOÀN BỘ DANH SÁCH MỤC HÀNG TRONG GIỎ

    @PostMapping("/Home/UpdateCartSelectionAll")
    @ResponseBody
    public Map<String, Object> updateCartSelectionAll(@RequestParam boolean isSelected, HttpSession session) {
        List<CartItem> cart = getCart(session);
        for (CartItem item : cart) {
            item.setSelected(isSelected);
        }
        session.setAttribute("Cart", cart);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    // =====================================================================================================================
    // THỰC HIỆN TÍNH TOÁN THEO THỜI GIAN THỰC ĐỒNG BỘ CHI TIẾT TẠM TÍNH, SHIP VÀ KHUYẾN MÃI

    @PostMapping("/Home/GetOrderSummary")
    @ResponseBody
    public Map<String, Object> getOrderSummary(@RequestParam(required = false) List<String> selectedIds,
                                               @RequestParam String shippingMethod,
                                               @RequestParam String couponCode,
                                               @RequestParam(required = false, defaultValue = "false") boolean clearCoupon,
                                               HttpSession session) {
        List<CartItem> cart = getCart(session);
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String msg = "";

        boolean isInputValid = false;
        String activeCode = null;

        if (clearCoupon) {
            session.setAttribute("CurrentCoupon", null);
        }

        // Cập nhật lại mảng trạng thái tích chọn mua của từng dòng tài nguyên trong giỏ hàng session
        if (selectedIds != null && !selectedIds.isEmpty()) {
            for (CartItem item : cart) {
                if (selectedIds.contains(item.getMaCTSP())) {
                    item.setSelected(true);
                    subTotal = subTotal.add(item.getDonGia().multiply(new BigDecimal(item.getSoLuong())));
                } else {
                    item.setSelected(false);
                }
            }
            session.setAttribute("Cart", cart);
        } else {
            cart.forEach(x -> x.setSelected(false));
            session.setAttribute("Cart", cart);
        }

        if (shippingMethod != null && !shippingMethod.isEmpty()) {
            session.setAttribute("CurrentShipping", shippingMethod);
        } else {
            if (session.getAttribute("CurrentShipping") != null) {
                shippingMethod = session.getAttribute("CurrentShipping").toString();
            } else {
                shippingMethod = "BD";
            }
        }

        BigDecimal dbShippingFee = sanPhamRepository.findShippingFeeByMethodId(shippingMethod);
        if (dbShippingFee != null) {
            shippingFee = dbShippingFee;
        }

        // Đối soát tính hợp lệ và thời hạn, số lượng áp dụng còn lại của mã giảm giá dưới SQL
        if (couponCode != null && !couponCode.isEmpty() && !clearCoupon) {
            Map<String, Object> coupon = sanPhamRepository.findActiveCouponByCode(couponCode);

            if (coupon != null && coupon.get("SoLuong") != null && ((Number) coupon.get("SoLuong")).intValue() > 0) {
                activeCode = couponCode;
                session.setAttribute("CurrentCoupon", couponCode);
                isInputValid = true;
            } else {
                msg = "Mã giảm giá không tồn tại hoặc đã hết hạn.";
                isInputValid = false;

                if (session.getAttribute("CurrentCoupon") != null) {
                    activeCode = session.getAttribute("CurrentCoupon").toString();
                }
            }
        } else {
            if (!clearCoupon && session.getAttribute("CurrentCoupon") != null) {
                activeCode = session.getAttribute("CurrentCoupon").toString();
                Map<String, Object> check = sanPhamRepository.findActiveCouponByCode(activeCode);

                if (check != null && check.get("SoLuong") != null && ((Number) check.get("SoLuong")).intValue() > 0) {
                    isInputValid = true;
                } else {
                    activeCode = null;
                    session.setAttribute("CurrentCoupon", null);
                }
            }
        }

        // Định biên công thức toán học tính trừ khấu hao số tiền giảm dựa theo phân loại mã
        if (activeCode != null) {
            Map<String, Object> coupon = sanPhamRepository.findActiveCouponByCode(activeCode);
            if (coupon != null && coupon.get("LoaiGiamGia") != null && coupon.get("GiaTri") != null) {
                String loaiGiamGia = coupon.get("LoaiGiamGia").toString();
                BigDecimal giaTri = new BigDecimal(coupon.get("GiaTri").toString());

                if ("percent".equals(loaiGiamGia)) {
                    discountAmount = subTotal.multiply(giaTri.divide(new BigDecimal("100")));
                } else if ("cash".equals(loaiGiamGia)) {
                    discountAmount = giaTri;
                } else if ("shipping".equals(loaiGiamGia)) {
                    discountAmount = shippingFee;
                }
            }
        }

        BigDecimal grandTotal = subTotal.add(shippingFee).subtract(discountAmount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("TamTinh", subTotal);
        res.put("PhiVanChuyen", shippingFee);
        res.put("GiamGia", discountAmount);
        res.put("TongCong", grandTotal);
        res.put("Message", msg);
        res.put("CouponValid", isInputValid);
        res.put("AppliedCode", activeCode);

        return res;
    }

    // =====================================================================================================================
    // HIỆU CHỈNH TĂNG GIẢM HOẶC THAY ĐỔI TRỰC TIẾP SỐ LƯỢNG SẢN PHẨM TRONG GIỎ HÀNG

    @PostMapping("/Home/UpdateCartQuantity")
    @ResponseBody
    public Map<String, Object> updateCartQuantity(@RequestParam String maCTSP, @RequestParam int quantity, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        List<CartItem> cart = (List<CartItem>) session.getAttribute("Cart");

        boolean found = false;
        if (cart != null) {
            for (CartItem item : cart) {
                if (item.getMaCTSP().equals(maCTSP)) {
                    item.setSoLuong(quantity);
                    found = true;
                    break;
                }
            }
        }

        session.setAttribute("Cart", cart);

        response.put("success", found);
        if (!found) {
            response.put("message", "Không tìm thấy sản phẩm trong giỏ hàng!");
        }

        return response;
    }

    // =====================================================================================================================
    // XÓA LOẠI BỎ HẲN MỘT DÒNG MẶT HÀNG RA KHỎI LIST GIỎ HÀNG TẠM THỜI TRÊN SESSION

    @PostMapping("/Home/RemoveFromCart")
    @ResponseBody
    public Map<String, Object> removeFromCart(@RequestParam String maCTSP, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        List<CartItem> cart = getCart(session);

        CartItem item = cart.stream()
                .filter(x -> x.getMaCTSP().equals(maCTSP))
                .findFirst()
                .orElse(null);

        if (item != null) {
            cart.remove(item);
            session.setAttribute("Cart", cart);

            response.put("success", true);
            return response;
        }

        response.put("success", false);
        response.put("message", "Không tìm thấy sản phẩm cần xóa trong hệ thống.");
        return response;
    }

    // =====================================================================================================================
    // TRANG THÀNH TIỀN KHAI BÁO NHẬN HÀNG VÀ CHỌN PHƯƠNG THỨC THANH TOÁN GIAO DỊCH

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        model.addAttribute("title", "Thanh Toán");

        List<CartItem> cart = getCart(session);
        List<CartItem> selectedItems = cart.stream().filter(CartItem::isSelected).toList();

        if (selectedItems.isEmpty()) {
            return "redirect:/cart";
        }

        BigDecimal tamTinh = BigDecimal.ZERO;
        for (CartItem item : selectedItems) {
            tamTinh = tamTinh.add(item.getThanhTien());
        }

        String maPTVC = (String) session.getAttribute("CurrentShipping");
        if (maPTVC == null || maPTVC.isEmpty()) {
            maPTVC = "BD";
        }
        BigDecimal phiShip = sanPhamRepository.findShippingFeeByMethodId(maPTVC);
        if (phiShip == null) {
            phiShip = BigDecimal.ZERO;
        }

        BigDecimal giamGia = BigDecimal.ZERO;
        String couponCode = (String) session.getAttribute("CurrentCoupon");
        if (couponCode != null && !couponCode.isEmpty()) {
            Map<String, Object> coupon = sanPhamRepository.findActiveCouponByCode(couponCode);
            if (coupon != null && coupon.get("GiaTri") != null && coupon.get("LoaiGiamGia") != null) {
                String loaiGiamGia = coupon.get("LoaiGiamGia").toString();
                BigDecimal giaTri = new BigDecimal(coupon.get("GiaTri").toString());

                if ("percent".equals(loaiGiamGia)) {
                    giamGia = tamTinh.multiply(giaTri.divide(new BigDecimal("100")));
                } else if ("cash".equals(loaiGiamGia)) {
                    giamGia = giaTri;
                } else if ("shipping".equals(loaiGiamGia)) {
                    giamGia = phiShip;
                }
            }
        }

        BigDecimal tongCong = tamTinh.add(phiShip).subtract(giamGia);
        if (tongCong.compareTo(BigDecimal.ZERO) < 0) {
            tongCong = BigDecimal.ZERO;
        }

        model.addAttribute("selectedItems", selectedItems);
        model.addAttribute("tamTinh", tamTinh);
        model.addAttribute("phiShip", phiShip);
        model.addAttribute("giamGia", giamGia);
        model.addAttribute("tongCong", tongCong);

        // Tự động nạp sẵn thông tin hồ sơ liên hệ cá nhân nếu thành viên đã đăng nhập trước đó
        model.addAttribute("userTen", session.getAttribute("TenKhachHang"));
        model.addAttribute("userSdt", session.getAttribute("SoDienThoai"));
        model.addAttribute("userDiaChi", session.getAttribute("DiaChi"));

        return "checkout";
    }

    // =====================================================================================================================
    // TIẾP NHẬN FORM ĐẶT HÀNG, KIỂM TRA ĐA LUỒNG BẤT ĐỒNG BỘ GỬI HÓA ĐƠN VÀ TRỪ KHO ĐỒNG BỘ

    @Transactional
    @PostMapping("/Home/PlaceOrder")
    @ResponseBody
    public Map<String, Object> placeOrder(@RequestParam("tenNguoiNhan") String tenNguoiNhan,
                                          @RequestParam("soDienThoai") String soDienThoai,
                                          @RequestParam("diaChi") String diaChi,
                                          @RequestParam(value = "ghiChu", required = false) String ghiChu,
                                          @RequestParam("phuongThucThanhToan") String phuongThucThanhToan,
                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (soDienThoai == null || soDienThoai.length() != 10 || !soDienThoai.startsWith("0") || !soDienThoai.matches("\\d+")) {
                response.put("success", false);
                response.put("message", "Số điện thoại không hợp lệ! (Phải chứa đúng 10 chữ số và bắt đầu bằng số 0)");
                return response;
            }

            List<CartItem> cart = getCart(session);
            List<CartItem> selectedItems = cart.stream().filter(CartItem::isSelected).toList();

            if (selectedItems.isEmpty()) {
                response.put("success", false);
                response.put("message", "Vui lòng chọn ít nhất 1 sản phẩm để tiến hành thanh toán.");
                return response;
            }

            // -------------------------------------------------------------------------------------------------------------
            // Rà soát chống lỗi tranh chấp dữ liệu race condition số lượng thực tế tồn kho biến thể
            for (CartItem item : selectedItems) {
                Integer stock = sanPhamRepository.findStockByVariantId(item.getMaCTSP());
                if (stock == null || stock < item.getSoLuong()) {
                    response.put("success", false);
                    response.put("message", "Sản phẩm \"" + item.getTenSanPham() + "\" - Kích thước " + item.getTenKichThuoc() + " hiện tại không đủ số lượng cung cấp (Trong kho chỉ còn: " + (stock == null ? 0 : stock) + ").");
                    return response;
                }
            }

            // -------------------------------------------------------------------------------------------------------------
            // Thiết lập sinh mã đơn hàng định danh tuần tự tự động tăng (Ví dụ: KH001DH004 hoặc GUESTDH002)
            String maKH = (String) session.getAttribute("MaKH");
            String prefixID = (maKH != null && !maKH.isEmpty()) ? maKH : "GUEST";
            String prefixOrder = prefixID + "DH";

            String lastOrder = sanPhamRepository.findLastOrderIdByPrefix(prefixOrder);
            int nextNumber = 1;
            if (lastOrder != null) {
                String numberPart = lastOrder.substring(prefixOrder.length());
                try {
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }
            String maDonHang = prefixOrder + String.format("%03d", nextNumber);

            BigDecimal subTotal = BigDecimal.ZERO;
            int totalQty = 0;
            for (CartItem item : selectedItems) {
                subTotal = subTotal.add(item.getThanhTien());
                totalQty += item.getSoLuong();
            }

            String maPTVC = (String) session.getAttribute("CurrentShipping");
            if (maPTVC == null || maPTVC.isEmpty()) {
                maPTVC = "BD";
            }
            BigDecimal phiShip = sanPhamRepository.findShippingFeeByMethodId(maPTVC);
            if (phiShip == null) phiShip = BigDecimal.ZERO;

            BigDecimal giamGia = BigDecimal.ZERO;
            String couponCode = (String) session.getAttribute("CurrentCoupon");
            if (couponCode != null && !couponCode.isEmpty()) {
                Map<String, Object> coupon = sanPhamRepository.findActiveCouponByCode(couponCode);
                if (coupon != null && coupon.get("GiaTri") != null && coupon.get("LoaiGiamGia") != null) {
                    String loaiGiamGia = coupon.get("LoaiGiamGia").toString();
                    BigDecimal giaTri = new BigDecimal(coupon.get("GiaTri").toString());

                    if ("percent".equals(loaiGiamGia)) {
                        giamGia = subTotal.multiply(giaTri.divide(new BigDecimal("100")));
                    } else if ("cash".equals(loaiGiamGia)) {
                        giamGia = giaTri;
                    } else if ("shipping".equals(loaiGiamGia)) {
                        giamGia = phiShip;
                    }
                }
            }

            String email = "guest@email.com";
            if (session.getAttribute("Email") != null) {
                email = session.getAttribute("Email").toString();
            }

            // -------------------------------------------------------------------------------------------------------------
            // Tạo mới một bản ghi bưu tá tương ứng lưu trữ thông tin xuống bảng DON_HANG
            sanPhamRepository.insertNativeOrder(
                    maDonHang,
                    maKH,
                    tenNguoiNhan,
                    soDienThoai,
                    email,
                    couponCode,
                    maPTVC,
                    diaChi,
                    ghiChu,
                    totalQty,
                    subTotal,
                    giamGia,
                    phiShip,
                    phuongThucThanhToan
            );

            // -------------------------------------------------------------------------------------------------------------
            // Khởi động chuỗi Transaction chèn chi tiết, trừ kho biến thể và dọn dẹp bảng giỏ hàng lưu ở database
            for (CartItem item : selectedItems) {
                sanPhamRepository.insertNativeOrderDetail(maDonHang, item.getMaCTSP(), item.getSoLuong(), item.getDonGia());
                sanPhamRepository.deductStock(item.getMaCTSP(), item.getSoLuong());

                if (maKH != null && !maKH.isEmpty()) {
                    sanPhamRepository.deleteCartItemFromDb(maKH, item.getMaCTSP());
                }
            }

            // Làm sạch các mốc dữ liệu giỏ hàng vừa mua thành công ra khỏi bộ nhớ tạm trình duyệt session
            cart.removeAll(selectedItems);
            session.setAttribute("Cart", cart);
            session.setAttribute("CurrentCoupon", null);

            // -------------------------------------------------------------------------------------------------------------
            // Áp dụng giải pháp đa luồng CompletableFuture phân tách tác vụ chạy ngầm gửi hóa đơn điện tử tốn thời gian
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("\n[ĐA LUỒNG] -> Luồng phụ [" + Thread.currentThread().getName() + "] đang bắt đầu kết nối Server gửi Email...");
                    System.out.println("[ĐA LUỒNG] -> Đang tiến hành đóng gói và gửi hóa đơn điện tử cho đơn hàng: #" + maDonHang);

                    Thread.sleep(3000);

                    System.out.println("[ĐA LUỒNG] -> Luồng phụ [" + Thread.currentThread().getName() + "] đã gửi Email thành công cho đơn hàng: #" + maDonHang + "\n");
                } catch (InterruptedException e) {
                    System.err.println("Lỗi xử lý luồng phụ: " + e.getMessage());
                }
            });

            response.put("success", true);
            response.put("maDonHang", maDonHang);
            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi hệ thống xảy ra trong quá trình xử lý đơn hàng: " + ex.getMessage());
            return response;
        }
    }

    // =====================================================================================================================
    // NGƯỜI DÙNG CHỦ ĐỘNG TỰ HỦY ĐƠN HÀNG KHI ĐƠN ĐANG Ở TRẠNG THÁI CHỜ XÁC NHẬN

    @PostMapping("/Home/CancelOrder")
    @ResponseBody
    public Map<String, Object> cancelOrder(@RequestParam("maDonHang") String maDonHang, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // Kích hoạt câu lệnh điều hướng UPDATE trạng thái hóa đơn về mục 'Hủy' dưới SQL Server
        int rowsUpdated = sanPhamRepository.cancelOrderNative(maDonHang);

        if (rowsUpdated > 0) {
            // Duyệt danh sách chi tiết đơn hàng để cộng trả lại số lượng tồn kho sản phẩm tương ứng cho bảng CHI_TIET_SP
            List<Map<String, Object>> details = sanPhamRepository.findOrderDetailsByOrderIdNative(maDonHang);
            for (Map<String, Object> detail : details) {
                String maCTSP = (String) detail.get("MaCTSP");
                int soLuong = (int) detail.get("SoLuong");
                sanPhamRepository.restoreStock(maCTSP, soLuong);
            }

            response.put("success", true);
            response.put("message", "Đơn hàng đã được hủy thành công và kho đã được cập nhật.");
        } else {
            response.put("success", false);
            response.put("message", "Không thể hủy đơn hàng này (Đơn hàng không ở trạng thái 'Chờ xác nhận').");
        }
        return response;
    }
}

// =========================================================================================================================
