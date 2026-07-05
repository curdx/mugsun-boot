package com.mugsun.boot.gen.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.mugsun.boot.gen.entity.Product;
import com.mugsun.boot.gen.service.ProductService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 代码生成演示表-商品 控制层。
 *
 * @author wdx
 * @since 2026-07-04
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 保存代码生成演示表-商品。
     *
     * @param product 代码生成演示表-商品
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody Product product) {
        return productService.save(product);
    }

    /**
     * 根据主键删除代码生成演示表-商品。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return productService.removeById(id);
    }

    /**
     * 根据主键更新代码生成演示表-商品。
     *
     * @param product 代码生成演示表-商品
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody Product product) {
        return productService.updateById(product);
    }

    /**
     * 查询所有代码生成演示表-商品。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<Product> list() {
        return productService.list();
    }

    /**
     * 根据主键获取代码生成演示表-商品。
     *
     * @param id 代码生成演示表-商品主键
     * @return 代码生成演示表-商品详情
     */
    @GetMapping("getInfo/{id}")
    public Product getInfo(@PathVariable Long id) {
        return productService.getById(id);
    }

    /**
     * 分页查询代码生成演示表-商品。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<Product> page(Page<Product> page) {
        return productService.page(page);
    }

}
