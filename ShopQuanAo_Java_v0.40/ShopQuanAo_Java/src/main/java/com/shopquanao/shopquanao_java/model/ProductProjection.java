package com.shopquanao.shopquanao_java.model;

import java.math.BigDecimal;

public interface ProductProjection {
    String getMaSP();

    String getTenSanPham();

    String getAnhDaiDien();

    BigDecimal getGiaBan();

    Double getDiemDanhGia();

    Integer getSoLuongDanhGia();

    String getMoTa();
}