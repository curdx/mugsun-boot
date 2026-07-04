package com.mugsun.boot.gen.entity;

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;



/**
 * 代码生成演示表-商品 实体类。
 *
 * @author wdx
 * @since 2026-07-04
 */
@Table("gen_product")
public class Product extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productName;

    private BigDecimal price;

    private Integer stock;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

}
