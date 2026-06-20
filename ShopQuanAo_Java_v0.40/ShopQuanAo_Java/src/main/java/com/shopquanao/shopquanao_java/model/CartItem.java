package com.shopquanao.shopquanao_java.model;

import java.math.BigDecimal;
import java.io.Serializable;

public class CartItem implements Serializable {
    private String maSP;
    private String tenSanPham;
    private String anhDaiDien;
    private String maCTSP;
    private String tenKichThuoc;
    private BigDecimal donGia;
    private int soLuong;
    private boolean isSelected = false;

    public String getMaSP() {
        return maSP;
    }

    public void setMaSP(String maSP) {
        this.maSP = maSP;
    }

    public String getTenSanPham() {
        return tenSanPham;
    }

    public void setTenSanPham(String tenSanPham) {
        this.tenSanPham = tenSanPham;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public String getMaCTSP() {
        return maCTSP;
    }

    public void setMaCTSP(String maCTSP) {
        this.maCTSP = maCTSP;
    }

    public String getTenKichThuoc() {
        return tenKichThuoc;
    }

    public void setTenKichThuoc(String tenKichThuoc) {
        this.tenKichThuoc = tenKichThuoc;
    }

    public BigDecimal getDonGia() {
        return donGia;
    }

    public void setDonGia(BigDecimal donGia) {
        this.donGia = donGia;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public BigDecimal getThanhTien() {
        if (donGia == null) return BigDecimal.ZERO;
        return donGia.multiply(new BigDecimal(soLuong));
    }
}
