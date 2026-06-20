package com.shopquanao.shopquanao_java.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SAN_PHAM")
public class SanPham {
    @Id
    @Column(name = "MaSP")
    private String maSP;

    @Column(name = "TenSanPham")
    private String tenSanPham;

    @Column(name = "MoTa")
    private String moTa;

    @Column(name = "TrangThai")
    private Boolean trangThai;

    @Column(name = "MaDanhMuc")
    private String maDanhMuc;

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

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Boolean getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(Boolean trangThai) {
        this.trangThai = trangThai;
    }

    public String getMaDanhMuc() {
        return maDanhMuc;
    }

    public void setMaDanhMuc(String maDanhMuc) {
        this.maDanhMuc = maDanhMuc;
    }
}
