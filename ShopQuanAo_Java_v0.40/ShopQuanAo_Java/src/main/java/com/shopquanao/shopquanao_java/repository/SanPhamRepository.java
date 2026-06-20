package com.shopquanao.shopquanao_java.repository;

import com.shopquanao.shopquanao_java.model.SanPham;
import com.shopquanao.shopquanao_java.model.ProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

// =========================================================================================================================
// KHO LƯU TRỮ TRUY XUẤT DỮ LIỆU NATIVE QUERY CHO TOÀN BỘ CHỨC NĂNG HỆ THỐNG SHOP QUẦN ÁO

public interface SanPhamRepository extends JpaRepository<SanPham, String> {

    // =====================================================================================================================
    // KHU VỰC KHÁCH HÀNG: TRUY XUẤT VÀ HIỂN THỊ THÔNG TIN SẢN PHẨM TRÊN TRANG CHỦ VÀ CHI TIẾT

    @Query(value = "SELECT sp.MaSP, sp.TenSanPham, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) as anhDaiDien, " +
            "(SELECT MIN(ct.GiaBan) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP) as giaBan, " +
            "ISNULL((SELECT AVG(CAST(d.SoSao AS FLOAT)) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1), 0) as diemDanhGia, " +
            "(SELECT COUNT(*) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1) as soLuongDanhGia " +
            "FROM SAN_PHAM sp WHERE sp.TrangThai = 1", nativeQuery = true)
    List<ProductProjection> findAllActiveProducts();

    @Query(value = "SELECT sp.MaSP, sp.TenSanPham, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) as anhDaiDien, " +
            "(SELECT MIN(ct.GiaBan) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP) as giaBan, " +
            "ISNULL((SELECT AVG(CAST(d.SoSao AS FLOAT)) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1), 0) as diemDanhGia, " +
            "(SELECT COUNT(*) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1) as soLuongDanhGia " +
            "FROM SAN_PHAM sp WHERE sp.TrangThai = 1 AND sp.MaSP IN (:fixedIds)", nativeQuery = true)
    List<ProductProjection> findTrendingProducts(@Param("fixedIds") List<String> fixedIds);

    @Query(value = "SELECT sp.MaSP, sp.TenSanPham, sp.MoTa, " +
            "(SELECT MIN(ct.GiaBan) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP) as giaBan, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) as anhDaiDien " +
            "FROM SAN_PHAM sp WHERE sp.MaSP = :id AND sp.TrangThai = 1", nativeQuery = true)
    ProductProjection findProductDetailById(@Param("id") String id);

    @Query(value = "SELECT DuongDan FROM HINH_ANH_SP WHERE MaSP = :id AND TrangThai = 1 ORDER BY LaAnhChinh DESC", nativeQuery = true)
    List<String> findImagesByProductId(@Param("id") String id);

    @Query(value = "SELECT ct.MaKichThuoc, k.TenKichThuoc FROM CHI_TIET_SP ct " +
            "JOIN KICH_THUOC k ON ct.MaKichThuoc = k.MaKichThuoc " +
            "WHERE ct.MaSP = :id AND ct.SoLuong > 0 AND ct.TrangThai = 1 " +
            "ORDER BY CASE " +
            "  WHEN k.TenKichThuoc = 'S' THEN 1 " +
            "  WHEN k.TenKichThuoc = 'M' THEN 2 " +
            "  WHEN k.TenKichThuoc = 'L' THEN 3 " +
            "  WHEN k.TenKichThuoc = 'XL' THEN 4 " +
            "  WHEN ISNUMERIC(k.TenKichThuoc) = 1 THEN CAST(k.TenKichThuoc AS INT) + 10 " +
            "  ELSE 99 END", nativeQuery = true)
    List<Map<String, Object>> findSizesByProductId(@Param("id") String id);

    @Query(value = "SELECT TenKhachHang, SoSao, NoiDung FROM DANH_GIA WHERE MaSP = :id AND TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findReviewsByProductId(@Param("id") String id);

    @Query(value = "SELECT TOP 4 sp.MaSP, sp.TenSanPham, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) as anhDaiDien, " +
            "(SELECT MIN(ct.GiaBan) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP) as giaBan, " +
            "ISNULL((SELECT AVG(CAST(d.SoSao AS FLOAT)) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1), 0) as diemDanhGia, " +
            "(SELECT COUNT(*) FROM DANH_GIA d WHERE d.MaSP = sp.MaSP AND d.TrangThai = 1) as soLuongDanhGia " +
            "FROM SAN_PHAM sp WHERE sp.MaDanhMuc = :categoryAndId AND sp.MaSP <> :id AND sp.TrangThai = 1 ORDER BY NEWID()", nativeQuery = true)
    List<ProductProjection> findRelatedProducts(@Param("categoryAndId") String category, @Param("id") String id);

    // =====================================================================================================================
    // KHU VỰC GIỎ HÀNG VÀ VẬN CHUYỂN KHUYẾN MÃI: QUẢN LÝ BIẾN THỂ, PHÍ SHIP VÀ COUPON GIẢM GIÁ

    @Query(value = "SELECT ct.MaCTSP, ct.GiaBan, ct.SoLuong, k.TenKichThuoc, k.MaKichThuoc, sp.TenSanPham, " +
            "(SELECT TOP 1 DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) AS AnhDaiDien " +
            "FROM CHI_TIET_SP ct " +
            "JOIN KICH_THUOC k ON ct.MaKichThuoc = k.MaKichThuoc " +
            "JOIN SAN_PHAM sp ON ct.MaSP = sp.MaSP " +
            "WHERE ct.MaSP = :maSP AND ct.TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findActiveVariantsByProductId(@Param("maSP") String maSP);

    @Query(value = "SELECT MaPTVC, TenPTVC, GiaVanChuyen FROM PHUONG_THUC_VAN_CHUYEN WHERE TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findAllActiveShippingMethods();

    @Query(value = "SELECT GiaVanChuyen FROM PHUONG_THUC_VAN_CHUYEN WHERE MaPTVC = :maPTVC AND TrangThai = 1", nativeQuery = true)
    BigDecimal findShippingFeeByMethodId(@Param("maPTVC") String maPTVC);

    @Query(value = "SELECT MaCode, GiaTri, LoaiGiamGia, SoLuong FROM MA_GIAM_GIA WHERE MaCode = :couponCode AND TrangThai = 1", nativeQuery = true)
    Map<String, Object> findActiveCouponByCode(@Param("couponCode") String couponCode);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM GIO_HANG WHERE MaKH = :maKH AND MaCTSP = :maCTSP", nativeQuery = true)
    void deleteCartItemFromDb(@Param("maKH") String maKH, @Param("maCTSP") String maCTSP);

    @Query(value = "SELECT sp.MaSP AS maSP, sp.TenSanPham AS tenSanPham, img.DuongDan AS anhDaiDien, " +
            "gh.MaCTSP AS maCTSP, k.TenKichThuoc AS tenKichThuoc, ct.GiaBan AS donGia, gh.SoLuong AS soLuong " +
            "FROM GIO_HANG gh " +
            "JOIN CHI_TIET_SP ct ON gh.MaCTSP = ct.MaCTSP " +
            "JOIN SAN_PHAM sp ON ct.MaSP = sp.MaSP " +
            "JOIN KICH_THUOC k ON ct.MaKichThuoc = k.MaKichThuoc " +
            "JOIN HINH_ANH_SP img ON sp.MaSP = img.MaSP " +
            "WHERE gh.MaKH = :maKH AND img.LaAnhChinh = 1", nativeQuery = true)
    List<Map<String, Object>> findCartItemsByCustomerIdNative(@Param("maKH") String maKH);

    // =====================================================================================================================
    // KHU VỰC ĐƠN HÀNG VÀ LỊCH SỬ MUA SẮM: ĐẶT HÀNG, THEO DÕI TRẠNG THÁI VÀ HOÀN KHO TỰ ĐỘNG

    @Query(value = "SELECT TOP 1 MaDonHang FROM DON_HANG WHERE MaDonHang LIKE :prefix% ORDER BY MaDonHang DESC", nativeQuery = true)
    String findLastOrderIdByPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT SoLuong FROM CHI_TIET_SP WHERE MaCTSP = :maCTSP", nativeQuery = true)
    Integer findStockByVariantId(@Param("maCTSP") String maCTSP);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE CHI_TIET_SP SET SoLuong = SoLuong - :qty WHERE MaCTSP = :maCTSP", nativeQuery = true)
    void deductStock(@Param("maCTSP") String maCTSP, @Param("qty") int qty);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO DON_HANG (MaDonHang, MaKH, TenNguoiNhan, SoDienThoai, Email, MaCode, MaPTVC, NgayDat, NgayGiaoDuKien, DiaChiGiao, GhiChu, TongSoLuong, TongTien, GiamGia, PhiVanChuyen, PhuongThucThanhToan, TrangThaiDonHang, TrangThai) " +
            "VALUES (:maDonHang, CAST(:maKH AS VARCHAR(5)), :tenNguoiNhan, :soDienThoai, :email, CAST(:maCode AS VARCHAR(20)), :maPTVC, SYSDATETIME(), DATEADD(day, 3, SYSDATETIME()), :diaChiGiao, :ghiChu, :tongSoLuong, :tongTien, :giamGia, :phiVanChuyen, :phuongThucThanhToan, N'Chờ xác nhận', 1)", nativeQuery = true)
    void insertNativeOrder(@Param("maDonHang") String maDonHang,
                           @Param("maKH") String maKH,
                           @Param("tenNguoiNhan") String tenNguoiNhan,
                           @Param("soDienThoai") String soDienThoai,
                           @Param("email") String email,
                           @Param("maCode") String maCode,
                           @Param("maPTVC") String maPTVC,
                           @Param("diaChiGiao") String diaChiGiao,
                           @Param("ghiChu") String ghiChu,
                           @Param("tongSoLuong") int tongSoLuong,
                           @Param("tongTien") BigDecimal tongTien,
                           @Param("giamGia") BigDecimal giamGia,
                           @Param("phiVanChuyen") BigDecimal phiVanChuyen,
                           @Param("phuongThucThanhToan") String phuongThucThanhToan);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO CHI_TIET_DON_HANG (MaDonHang, MaCTSP, SoLuong, DonGia, TrangThai) VALUES (:maDonHang, :maCTSP, :soLuong, :donGia, 1)", nativeQuery = true)
    void insertNativeOrderDetail(@Param("maDonHang") String maDonHang, @Param("maCTSP") String maCTSP, @Param("soLuong") int soLuong, @Param("donGia") BigDecimal donGia);

    @Query(value = "SELECT MaDonHang AS maDonHang, NgayDat AS ngayDat, TrangThaiDonHang AS trangThai, " +
            "TenNguoiNhan AS tenNguoiNhan, SoDienThoai AS soDienThoai, DiaChiGiao AS diaChiGiao, GhiChu AS ghiChu, " +
            "CAST(TongTien AS INT) AS tamTinh, CAST(PhiVanChuyen AS INT) AS phiVanChuyen, " +
            "CAST(GiamGia AS INT) AS giamGia, CAST((TongTien + PhiVanChuyen - GiamGia) AS INT) AS tongTien " +
            "FROM DON_HANG WHERE MaKH = :maKH AND TrangThai = 1 ORDER BY NgayDat DESC", nativeQuery = true)
    List<Map<String, Object>> findOrdersByCustomerIdNative(@Param("maKH") String maKH);

    @Query(value = "SELECT MaDonHang AS maDonHang, NgayDat AS ngayDat, TrangThaiDonHang AS trangThai, " +
            "TenNguoiNhan AS tenNguoiNhan, SoDienThoai AS soDienThoai, DiaChiGiao AS diaChiGiao, GhiChu AS ghiChu, " +
            "CAST(TongTien AS INT) AS tamTinh, CAST(PhiVanChuyen AS INT) AS phiVanChuyen, " +
            "CAST(GiamGia AS INT) AS giamGia, CAST((TongTien + PhiVanChuyen - GiamGia) AS INT) AS tongTien " +
            "FROM DON_HANG WHERE MaKH IS NULL AND MaDonHang LIKE 'GUEST%' AND TrangThai = 1 ORDER BY NgayDat DESC", nativeQuery = true)
    List<Map<String, Object>> findGuestOrdersNative();

    @Query(value = "SELECT ct.MaSP AS maSP, sp.TenSanPham AS tenSanPham, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) AS anhDaiDien, " +
            "k.TenKichThuoc AS tenKichThuoc, ctdh.SoLuong AS soLuong, " +
            "CAST(ctdh.DonGia AS INT) AS donGia, CAST(ctdh.ThanhTien AS INT) AS thanhTien " +
            "FROM CHI_TIET_DON_HANG ctdh " +
            "JOIN CHI_TIET_SP ct ON ctdh.MaCTSP = ct.MaCTSP " +
            "JOIN SAN_PHAM sp ON ct.MaSP = sp.MaSP " +
            "JOIN KICH_THUOC k ON ct.MaKichThuoc = k.MaKichThuoc " +
            "WHERE ctdh.MaDonHang = :maDonHang AND ctdh.TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findOrderDetailsByOrderIdNative(@Param("maDonHang") String maDonHang);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE DON_HANG SET TrangThaiDonHang = N'Hoàn tất', DaThanhToan = 1 WHERE MaDonHang = :maDonHang AND TrangThaiDonHang = N'Đang giao'", nativeQuery = true)
    int confirmReceivedOrderNative(@Param("maDonHang") String maDonHang);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE DON_HANG SET TrangThaiDonHang = N'Hủy' WHERE MaDonHang = :maDonHang AND TrangThaiDonHang = N'Chờ xác nhận'", nativeQuery = true)
    int cancelOrderNative(@Param("maDonHang") String maDonHang);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE CHI_TIET_SP SET SoLuong = SoLuong + :qty WHERE MaCTSP = :maCTSP", nativeQuery = true)
    void restoreStock(@Param("maCTSP") String maCTSP, @Param("qty") int qty);

    @Query(value = "SELECT COUNT(*) FROM DON_HANG WHERE MaKH = :maKH AND TrangThaiDonHang NOT IN (N'Hoàn tất', N'Hủy') AND TrangThai = 1", nativeQuery = true)
    int countActiveOrdersByCustomerIdNative(@Param("maKH") String maKH);

    @Query(value = "SELECT COUNT(*) FROM DON_HANG WHERE MaKH IS NULL AND MaDonHang LIKE 'GUEST%' AND TrangThaiDonHang NOT IN (N'Hoàn tất', N'Hủy') AND TrangThai = 1", nativeQuery = true)
    int countActiveGuestOrdersNative();

    // =====================================================================================================================
    // KHU VỰC TÀI KHOẢN VÀ PHẢN HỒI: ĐĂNG KÝ, ĐĂNG NHẬP, ĐÁNH GIÁ VÀ BẢO MẬT KHÓA TÀI KHOẢN NGƯỜI DÙNG

    @Query(value = "SELECT TOP 1 MaDanhGia FROM DANH_GIA WHERE MaSP = :maSP ORDER BY MaDanhGia DESC", nativeQuery = true)
    String findLastReviewIdByProductId(@Param("maSP") String maSP);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO DANH_GIA (MaDanhGia, MaSP, MaKH, TenKhachHang, SoSao, NoiDung, TrangThai) " +
            "VALUES (:maDanhGia, :maSP, CAST(:maKH AS VARCHAR(5)), :tenKhachHang, :soSao, :noiDung, 1)", nativeQuery = true)
    void insertNativeReview(@Param("maDanhGia") String maDanhGia,
                            @Param("maSP") String maSP,
                            @Param("maKH") String maKH,
                            @Param("tenKhachHang") String tenKhachHang,
                            @Param("soSao") int soSao,
                            @Param("noiDung") String noiDung);

    @Query(value = "SELECT MaKH, TenDangNhap, MatKhau, HoTen, SoDienThoai, Email, DiaChi, TrangThai FROM KHACH_HANG WHERE TenDangNhap = :email", nativeQuery = true)
    Map<String, Object> findCustomerByEmailNative(@Param("email") String email);

    @Query(value = "SELECT MaNV, TenDangNhap, MatKhau, HoTen, ChucVu, TrangThai FROM NHAN_VIEN WHERE TenDangNhap = :username", nativeQuery = true)
    Map<String, Object> findEmployeeByUsernameNative(@Param("username") String username);

    @Query(value = "SELECT TOP 1 MaKH FROM KHACH_HANG ORDER BY MaKH DESC", nativeQuery = true)
    String findLastCustomerIdNative();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO KHACH_HANG (MaKH, TenDangNhap, MatKhau, HoTen, Email, TrangThai) VALUES (:maKH, :username, :password, :fullName, :email, 1)", nativeQuery = true)
    void insertNativeCustomer(@Param("maKH") String maKH, @Param("username") String username, @Param("password") String password, @Param("fullName") String fullName, @Param("email") String email);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE KHACH_HANG SET MatKhau = :newPassword WHERE TenDangNhap = :email AND TrangThai = 1", nativeQuery = true)
    int updatePasswordByEmailNative(@Param("email") String email, @Param("newPassword") String newPassword);

    @Query(value = "SELECT MaKH, TenDangNhap, HoTen, SoDienThoai, DiaChi, TrangThai FROM KHACH_HANG ORDER BY MaKH DESC", nativeQuery = true)
    List<Map<String, Object>> findAllCustomersNative();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE KHACH_HANG SET TrangThai = :status WHERE MaKH = :maKH", nativeQuery = true)
    int updateCustomerStatusNative(@Param("maKH") String maKH, @Param("status") int status);

    // =====================================================================================================================
    // KHU VỰC QUẢN TRỊ ADMIN: ĐIỀU PHỐI ĐƠN HÀNG, ĐỐI SOÁT DOANH THU VÀ THỐNG KÊ BÁO CÁO TÀI CHÍNH

    @Query(value = "SELECT MaDonHang AS maDonHang, TenNguoiNhan AS tenKhachHang, NgayDat AS ngayDat, " +
            "CAST(TongTien AS INT) AS tamTinh, CAST(PhiVanChuyen AS INT) AS phiVanChuyen, CAST(GiamGia AS INT) AS giamGia, " +
            "CAST((TongTien + PhiVanChuyen - GiamGia) AS INT) AS tongTien, " +
            "TrangThaiDonHang AS trangThai, DaThanhToan AS daThanhToan " +
            "FROM DON_HANG WHERE TrangThai = 1 ORDER BY NgayDat DESC", nativeQuery = true)
    List<Map<String, Object>> findAllAdminOrdersNative();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE DON_HANG SET TrangThaiDonHang = :status, DaThanhToan = :daThanhToan WHERE MaDonHang = :maDonHang", nativeQuery = true)
    int updateOrderStatusAndPaymentNative(@Param("maDonHang") String maDonHang,
                                          @Param("status") String status,
                                          @Param("daThanhToan") int daThanhToan);

    @Query(value = "SELECT ISNULL(SUM(CAST((TongTien + PhiVanChuyen - GiamGia) AS INT)), 0) " +
            "FROM DON_HANG WHERE TrangThaiDonHang = N'Hoàn tất' AND TrangThai = 1", nativeQuery = true)
    int calculateTotalRevenueNative();

    @Query(value = "SELECT COUNT(*) FROM DON_HANG WHERE TrangThaiDonHang = :status AND TrangThai = 1", nativeQuery = true)
    int countOrdersByStatusNative(@Param("status") String status);

    @Query(value = "SELECT COUNT(*) FROM DON_HANG WHERE TrangThaiDonHang IN (N'Chờ xác nhận', N'Đang giao') AND TrangThai = 1", nativeQuery = true)
    int countProcessingOrdersNative();

    @Query(value = "SELECT COUNT(*) FROM DON_HANG WHERE TrangThai = 1", nativeQuery = true)
    int countTotalOrdersNative();

    @Query(value = "SELECT MaDonHang AS maDonHang, TenNguoiNhan AS tenKhachHang, NgayDat AS ngayDat, " +
            "CAST((TongTien + PhiVanChuyen - GiamGia) AS INT) AS tongTien " +
            "FROM DON_HANG WHERE TrangThaiDonHang = N'Hoàn tất' AND TrangThai = 1 ORDER BY NgayDat DESC", nativeQuery = true)
    List<Map<String, Object>> findCompletedOrdersNative();

    // =====================================================================================================================
    // KHU VỰC QUẢN TRỊ ADMIN: QUẢN LÝ KHO HÀNG, KHỞI TẠO VÀ CẬP NHẬT BIẾN THỂ SẢN PHẨM ĐỒNG BỘ ĐA BẢNG

    @Query(value = "SELECT sp.MaSP AS maSP, sp.TenSanPham AS tenSanPham, dm.TenDanhMuc AS tenDanhMuc, " +
            "(SELECT TOP 1 h.DuongDan FROM HINH_ANH_SP h WHERE h.MaSP = sp.MaSP AND h.LaAnhChinh = 1) AS anhDaiDien, " +
            "CAST((SELECT MIN(ct.GiaBan) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP) AS INT) AS giaBan, " +
            "ISNULL((SELECT SUM(ct.SoLuong) FROM CHI_TIET_SP ct WHERE ct.MaSP = sp.MaSP), 0) AS tongTonKho " +
            "FROM SAN_PHAM sp " +
            "JOIN DANH_MUC dm ON sp.MaDanhMuc = dm.MaDanhMuc " +
            "WHERE sp.TrangThai = 1 ORDER BY sp.MaSP DESC", nativeQuery = true)
    List<Map<String, Object>> findAllAdminProductsNative();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE SAN_PHAM SET TrangThai = 0 WHERE MaSP = :maSP", nativeQuery = true)
    void softDeleteProductNative(@Param("maSP") String maSP);

    @Query(value = "SELECT MaDanhMuc, TenDanhMuc FROM DANH_MUC WHERE TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findAllActiveCategories();

    @Query(value = "SELECT MaKichThuoc, TenKichThuoc FROM KICH_THUOC WHERE TrangThai = 1", nativeQuery = true)
    List<Map<String, Object>> findAllActiveSizes();

    @Query(value = "SELECT TOP 1 MaSP FROM SAN_PHAM " +
            "WHERE MaSP LIKE CONCAT(:prefix, '%') " +
            "ORDER BY CAST(SUBSTRING(MaSP, 2, LEN(MaSP)) AS INT) DESC", nativeQuery = true)
    String findLastProductIdByPrefix(@Param("prefix") String prefix);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO KICH_THUOC (MaKichThuoc, TenKichThuoc, TrangThai) " +
            "SELECT :maKichThuoc, :tenKichThuoc, 1 " +
            "WHERE NOT EXISTS (SELECT 1 FROM KICH_THUOC WHERE MaKichThuoc = :maKichThuoc)", nativeQuery = true)
    void insertNativeSize(@Param("maKichThuoc") String maKichThuoc, @Param("tenKichThuoc") String tenKichThuoc);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO SAN_PHAM (MaSP, TenSanPham, MaDanhMuc, MoTa, TrangThai) VALUES (:maSP, :tenSP, :maDanhMuc, :moTa, 1)", nativeQuery = true)
    void insertNativeProduct(@Param("maSP") String maSP, @Param("tenSP") String tenSP, @Param("maDanhMuc") String maDanhMuc, @Param("moTa") String moTa);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO CHI_TIET_SP (MaCTSP, MaSP, MaKichThuoc, SoLuong, GiaBan, TrangThai) VALUES (:maCTSP, :maSP, :maKichThuoc, :soLuong, :giaBan, 1)", nativeQuery = true)
    void insertNativeProductDetail(@Param("maCTSP") String maCTSP, @Param("maSP") String maSP, @Param("maKichThuoc") String maKichThuoc, @Param("soLuong") int soLuong, @Param("giaBan") BigDecimal giaBan);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "INSERT INTO HINH_ANH_SP (MaHinhAnh, MaSP, DuongDan, LaAnhChinh, TrangThai) VALUES (:maHA, :maSP, :duongDan, 1, 1)", nativeQuery = true)
    void insertNativeProductImage(@Param("maHA") String maHA, @Param("maSP") String maSP, @Param("duongDan") String duongDan);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE SAN_PHAM SET TenSanPham = :tenSP, MaDanhMuc = :maDM, MoTa = :moTa WHERE MaSP = :maSP", nativeQuery = true)
    void updateNativeProduct(@Param("maSP") String maSP, @Param("tenSP") String tenSP, @Param("maDM") String maDM, @Param("moTa") String moTa);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE HINH_ANH_SP SET DuongDan = :path WHERE MaSP = :maSP AND LaAnhChinh = 1", nativeQuery = true)
    void updateProductImageNative(@Param("maSP") String maSP, @Param("path") String path);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE CHI_TIET_SP SET TrangThai = 0 WHERE MaSP = :maSP", nativeQuery = true)
    void deactivateAllProductDetailsNative(@Param("maSP") String maSP);

    @Query(value = "SELECT COUNT(*) FROM CHI_TIET_SP WHERE MaCTSP = :maCTSP", nativeQuery = true)
    int countDetailByIdNative(@Param("maCTSP") String maCTSP);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE CHI_TIET_SP SET GiaBan = :price, SoLuong = :qty, TrangThai = 1 WHERE MaCTSP = :maCTSP", nativeQuery = true)
    void updateProductDetailNative(@Param("maCTSP") String maCTSP, @Param("qty") int qty, @Param("price") BigDecimal price);
}
// =========================================================================================================================
