package com.shopquanao.shopquanao_java.controller;

import com.shopquanao.shopquanao_java.repository.SanPhamRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

// =========================================================================================================================
// LỚP ĐIỀU HƯỚNG VÀ ĐỊNH TUYẾN QUẢN TRỊ ADMIN

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    // =====================================================================================================================
    // TRANG CHỦ BẢNG ĐIỀU KHIỂN QUẢN LÝ DANH SÁCH SẢN PHẨM

    @GetMapping("/dashboard")
    public String index(Model model, HttpSession session) {

        // Kiểm tra bảo mật phiên đăng nhập quản trị viên
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        // Đọc dữ liệu từ SQL Server, nạp lên view và làm sáng mục menu sidebar tương ứng
        List<Map<String, Object>> products = sanPhamRepository.findAllAdminProductsNative();
        model.addAttribute("products", products);
        model.addAttribute("title", "Quản lý sản phẩm - Admin");
        model.addAttribute("activePage", "products");

        return "admin/dashboard";
    }

    // =====================================================================================================================
    // XỬ LÝ XÓA MỀM SẢN PHẨM

    @GetMapping("/dashboard/soft-delete/{id}")
    public String deleteProduct(@PathVariable("id") String id, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        sanPhamRepository.softDeleteProductNative(id);
        return "redirect:/admin/dashboard";
    }

    // =====================================================================================================================
    // TRANG HIỂN THỊ BIỂU MẪU THÊM MỚI SẢN PHẨM

    @GetMapping("/dashboard/create")
    public String createProductForm(Model model, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) return "redirect:/login";

        model.addAttribute("title", "Thêm sản phẩm mới");
        model.addAttribute("categories", sanPhamRepository.findAllActiveCategories());

        // Phân loại dải kích thước động
        List<Map<String, Object>> sizes = sanPhamRepository.findAllActiveSizes();
        List<Map<String, Object>> textSizes = new ArrayList<>();
        List<Map<String, Object>> numSizes = new ArrayList<>();

        for (Map<String, Object> size : sizes) {
            String tenKichThuoc = size.get("TenKichThuoc").toString();
            if (tenKichThuoc.matches("\\d+")) {
                numSizes.add(size);
            } else {
                textSizes.add(size);
            }
        }
        model.addAttribute("textSizes", textSizes);
        model.addAttribute("numSizes", numSizes);
        model.addAttribute("activePage", "products");

        return "admin/create";
    }

    // =====================================================================================================================
    // LƯU SẢN PHẨM MỚI VÀ ĐỒNG BỘ ĐA BẢNG KHO HÀNG BIẾN THỂ

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/dashboard/create")
    public String handleCreateProduct(@RequestParam("tenSanPham") String tenSanPham,
                                      @RequestParam("maDanhMuc") String maDanhMuc,
                                      @RequestParam("moTa") String moTa,
                                      @RequestParam("imageFile") org.springframework.web.multipart.MultipartFile imageFile,
                                      @RequestParam(value = "selectedSizes", required = false) List<String> selectedSizes,
                                      @RequestParam Map<String, String> allRequestParams,
                                      HttpSession session,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        try {
            // -------------------------------------------------------------------------------------------------------------
            // Tự động tính toán mã sản phẩm tăng dần theo tiền tố danh mục (Ví dụ: A04, G17)
            String lastMaSP = sanPhamRepository.findLastProductIdByPrefix(maDanhMuc);
            int nextProductNum = 1;
            if (lastMaSP != null && lastMaSP.length() > 1) {
                nextProductNum = Integer.parseInt(lastMaSP.substring(1)) + 1;
            }
            String newMaSP = maDanhMuc + String.format("%02d", nextProductNum);

            // -------------------------------------------------------------------------------------------------------------
            // Xử lý ghi file truyền dữ liệu hình ảnh vật lý vào thư mục lưu trữ máy chủ
            String dbImgPath = "assets/img/products/default.jpg";
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/assets/img/products/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                java.io.File serverFile = new java.io.File(uploadDir + fileName);
                imageFile.transferTo(serverFile);
                dbImgPath = "assets/img/products/" + fileName;
            }

            // -------------------------------------------------------------------------------------------------------------
            // Kích hoạt Transaction ghi thông tin chung vào bảng SAN_PHAM và bảng HINH_ANH_SP
            sanPhamRepository.insertNativeProduct(newMaSP, tenSanPham, maDanhMuc, moTa);

            String newMaHA = newMaSP + "H01";
            sanPhamRepository.insertNativeProductImage(newMaHA, newMaSP, dbImgPath);

            // -------------------------------------------------------------------------------------------------------------
            // Duyệt mảng kích thước được tích chọn để chèn dải kho số lượng và giá bán vào CHI_TIET_SP
            if (selectedSizes != null) {
                for (String sizeName : selectedSizes) {
                    String priceStr = allRequestParams.get("price_" + sizeName);
                    String qtyStr = allRequestParams.get("qty_" + sizeName);

                    if (priceStr != null && !priceStr.isEmpty() && qtyStr != null && !qtyStr.isEmpty()) {
                        java.math.BigDecimal giaBan = new java.math.BigDecimal(priceStr);
                        int soLuong = Integer.parseInt(qtyStr);

                        String maKichThuoc = newMaSP + "Sz" + sizeName;
                        String maCTSP = "CT" + maKichThuoc;

                        sanPhamRepository.insertNativeSize(maKichThuoc, sizeName);
                        sanPhamRepository.insertNativeProductDetail(maCTSP, newMaSP, maKichThuoc, soLuong, giaBan);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới sản phẩm \"" + tenSanPham + "\" thành công!");
            return "redirect:/admin/dashboard";

        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm sản phẩm: " + ex.getMessage());
            return "redirect:/admin/dashboard/create";
        }
    }

    // =====================================================================================================================
    // TRANG QUẢN LÝ VÀ ĐIỀU PHỐI DANH SÁCH ĐƠN HÀNG TOÀN HỆ THỐNG

    @GetMapping("/orders")
    public String adminOrders(Model model, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> orders = sanPhamRepository.findAllAdminOrdersNative();
        model.addAttribute("orders", orders);
        model.addAttribute("title", "Quản lý đơn hàng");
        model.addAttribute("activePage", "orders");

        return "admin/orders";
    }

    // =====================================================================================================================
    // TIẾP NHẬN YÊU CẦU CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG & THANH TOÁN

    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("maDonHang") String maDonHang,
                                    @RequestParam("status") String status,
                                    @RequestParam("daThanhToan") int daThanhToan,
                                    HttpSession session,
                                    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        int rowsUpdated = sanPhamRepository.updateOrderStatusAndPaymentNative(maDonHang, status, daThanhToan);
        if (rowsUpdated > 0) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái và thanh toán đơn hàng " + maDonHang + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật dữ liệu thất bại.");
        }

        return "redirect:/admin/orders";
    }

    // =====================================================================================================================
    // TRANG ĐỐI SOÁT TÀI CHÍNH VÀ THỐNG KÊ BÁO CÁO DOANH THU

    @GetMapping("/revenue")
    public String adminRevenue(Model model, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        // -----------------------------------------------------------------------------------------------------------------
        // Tổng hợp số liệu tài chính thực thu và đếm số lượng đơn hàng theo trạng thái chuyên biệt
        int totalRevenue = sanPhamRepository.calculateTotalRevenueNative();
        int doneCount = sanPhamRepository.countOrdersByStatusNative("Hoàn tất");
        int processingCount = sanPhamRepository.countProcessingOrdersNative();
        int cancelCount = sanPhamRepository.countOrdersByStatusNative("Hủy");
        int totalOrders = sanPhamRepository.countTotalOrdersNative();

        // -----------------------------------------------------------------------------------------------------------------
        // Tính tỷ lệ % hoàn thành đơn hàng và làm tròn 1 chữ số thập phân
        double tyLeHoanThanh = 0.0;
        if (totalOrders > 0) {
            tyLeHoanThanh = ((double) doneCount / totalOrders) * 100;
            tyLeHoanThanh = Math.round(tyLeHoanThanh * 10.0) / 10.0;
        }

        List<Map<String, Object>> dsDonHoanThanh = sanPhamRepository.findCompletedOrdersNative();

        model.addAttribute("tongDoanhThu", totalRevenue);
        model.addAttribute("donHoanThanh", doneCount);
        model.addAttribute("donDangXuLy", processingCount);
        model.addAttribute("donHuy", cancelCount);
        model.addAttribute("tyLeHoanThanh", tyLeHoanThanh);
        model.addAttribute("dsDonHoanThanh", dsDonHoanThanh);
        model.addAttribute("title", "Thống kê doanh thu");
        model.addAttribute("activePage", "revenue");

        return "admin/revenue";
    }

    // =====================================================================================================================
    // TRANG NẠP THÔNG TIN ĐỐI SOÁT CŨ PHỤC VỤ SỬA ĐỔI SẢN PHẨM

    @GetMapping("/dashboard/edit/{id}")
    public String editProductForm(@PathVariable("id") String id, Model model, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) return "redirect:/login";

        com.shopquanao.shopquanao_java.model.ProductProjection product = sanPhamRepository.findProductDetailById(id);
        if (product == null) return "redirect:/admin/dashboard";

        // Chuyển cấu trúc danh sách size cũ thành bản đồ Map để tick sẵn checked lên giao diện biểu mẫu
        List<Map<String, Object>> currentVariants = sanPhamRepository.findActiveVariantsByProductId(id);
        Map<String, Map<String, Object>> variantMap = new java.util.HashMap<>();
        for (Map<String, Object> v : currentVariants) {
            variantMap.put(v.get("TenKichThuoc").toString(), v);
        }

        model.addAttribute("product", product);
        model.addAttribute("variantMap", variantMap);
        model.addAttribute("categories", sanPhamRepository.findAllActiveCategories());

        List<Map<String, Object>> allSizes = sanPhamRepository.findAllActiveSizes();
        List<Map<String, Object>> textSizes = new ArrayList<>();
        List<Map<String, Object>> numSizes = new ArrayList<>();
        for (Map<String, Object> s : allSizes) {
            if (s.get("TenKichThuoc").toString().matches("\\d+")) numSizes.add(s);
            else textSizes.add(s);
        }
        model.addAttribute("textSizes", textSizes);
        model.addAttribute("numSizes", numSizes);
        model.addAttribute("activePage", "products");

        return "admin/edit";
    }

    // =====================================================================================================================
    // CẬP NHẬT GHI ĐÈ AN TOÀN THÔNG TIN SẢN PHẨM VÀ ĐIỀU CHỈNH BIẾN THỂ KHO

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/dashboard/edit")
    public String handleUpdateProduct(@RequestParam("maSP") String maSP,
                                      @RequestParam("tenSanPham") String tenSanPham,
                                      @RequestParam("maDanhMuc") String maDanhMuc,
                                      @RequestParam("moTa") String moTa,
                                      @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                      @RequestParam(value = "selectedSizes", required = false) List<String> selectedSizes,
                                      @RequestParam Map<String, String> allRequestParams,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // -------------------------------------------------------------------------------------------------------------
            // Sửa đổi tên, danh mục, mô tả mới và tự động ghi đè tệp tin hình ảnh vật lý nếu có tải lên file mới
            sanPhamRepository.updateNativeProduct(maSP, tenSanPham, maDanhMuc, moTa);

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/assets/img/products/";
                imageFile.transferTo(new java.io.File(uploadDir + fileName));
                sanPhamRepository.updateProductImageNative(maSP, "assets/img/products/" + fileName);
            }

            // -------------------------------------------------------------------------------------------------------------
            // Tắt trạng thái hoạt động toàn bộ size cũ về bằng 0 trước khi duyệt kiểm tra danh sách size cấu hình mới
            sanPhamRepository.deactivateAllProductDetailsNative(maSP);

            if (selectedSizes != null) {
                for (String sizeName : selectedSizes) {
                    String priceStr = allRequestParams.get("price_" + sizeName);
                    String qtyStr = allRequestParams.get("qty_" + sizeName);

                    if (priceStr != null && !priceStr.isEmpty() && qtyStr != null && !qtyStr.isEmpty()) {
                        java.math.BigDecimal giaBan = new java.math.BigDecimal(priceStr);
                        int soLuong = Integer.parseInt(qtyStr);

                        String maKichThuoc = maSP + "Sz" + sizeName;
                        String maCTSP = "CT" + maKichThuoc;

                        try {
                            sanPhamRepository.insertNativeSize(maKichThuoc, sizeName);
                        } catch (Exception e) {
                        }

                        if (sanPhamRepository.countDetailByIdNative(maCTSP) > 0) {
                            sanPhamRepository.updateProductDetailNative(maCTSP, soLuong, giaBan);
                        } else {
                            sanPhamRepository.insertNativeProductDetail(maCTSP, maSP, maKichThuoc, soLuong, giaBan);
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // =====================================================================================================================
    // TRANG QUẢN LÝ VÀ THEO DÕI DANH SÁCH TÀI KHOẢN NGƯỜI DÙNG KHÁCH HÀNG

    @GetMapping("/customers")
    public String manageCustomers(Model model, HttpSession session) {
        if (session.getAttribute("ChucVu") == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> customers = sanPhamRepository.findAllCustomersNative();
        model.addAttribute("customers", customers);
        model.addAttribute("title", "Quản lý khách hàng");
        model.addAttribute("activePage", "customers");

        return "admin/customers";
    }

    // =====================================================================================================================
    // THỰC THI KHÓA HOẶC KÍCH HOẠT MỞ KHÓA TÀI KHOẢN KHÁCH HÀNG BẢO MẬT

    @PostMapping("/customers/toggle-status")
    public String toggleCustomerStatus(@RequestParam("maKH") String maKH,
                                       @RequestParam("currentStatus") int currentStatus,
                                       org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        int newStatus = (currentStatus == 1) ? 0 : 1;
        String action = (newStatus == 1) ? "Mở khóa" : "Khóa";

        int rowsUpdated = sanPhamRepository.updateCustomerStatusNative(maKH, newStatus);

        if (rowsUpdated > 0) {
            redirectAttributes.addFlashAttribute("successMessage", action + " tài khoản " + maKH + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Thao tác thất bại!");
        }

        return "redirect:/admin/customers";
    }
}

// =========================================================================================================================
