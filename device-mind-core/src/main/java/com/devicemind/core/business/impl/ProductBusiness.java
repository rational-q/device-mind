package com.devicemind.core.business.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.common.exception.ServiceException;
import com.devicemind.core.business.intf.IProductBusiness;
import com.devicemind.core.model.dto.ProductCreateDTO;
import com.devicemind.core.model.dto.ProductPageQueryDTO;
import com.devicemind.core.model.dto.ProductUpdateDTO;
import com.devicemind.core.model.entity.DmDevice;
import com.devicemind.core.model.entity.DmProductDO;
import com.devicemind.core.model.vo.ProductVO;
import com.devicemind.core.stdsvc.intf.IDmDeviceService;
import com.devicemind.core.stdsvc.intf.IDmProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class ProductBusiness implements IProductBusiness {

    @Autowired
    private IDmProductService productService;
    @Autowired
    private IDmDeviceService deviceService;

    @Override
    public Page<ProductVO> listPage(ProductPageQueryDTO query) {
        LambdaQueryWrapper<DmProductDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getProductKey())) {
            wrapper.eq(DmProductDO::getProductKey, query.getProductKey());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(DmProductDO::getName, query.getName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(DmProductDO::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(DmProductDO::getCreatedDate);

        Page<DmProductDO> page = Page.of(query.getPageNum(), query.getPageSize());
        productService.page(page, wrapper);

        List<ProductVO> vos = page.getRecords().stream().map(this::toVO).toList();
        Page<ProductVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public ProductVO getById(Long id) {
        DmProductDO product = productService.getById(id);
        if (product == null) {
            throw new ServiceException(404, "产品不存在");
        }
        return toVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ProductCreateDTO dto) {
        long count = productService.lambdaQuery()
                .eq(DmProductDO::getProductKey, dto.getProductKey())
                .count();
        if (count > 0) {
            throw new ServiceException("产品标识已存在: " + dto.getProductKey());
        }
        DmProductDO entity = new DmProductDO();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("ACTIVE");
        productService.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductUpdateDTO dto) {
        DmProductDO existing = productService.getById(id);
        if (existing == null) {
            throw new ServiceException(404, "产品不存在");
        }
        BeanUtils.copyProperties(dto, existing, "id");
        productService.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (productService.getById(id) == null) {
            throw new ServiceException(404, "产品不存在");
        }
        long deviceCount = deviceService.lambdaQuery()
                .eq(DmDevice::getProductId, id)
                .count();
        if (deviceCount > 0) {
            throw new ServiceException("产品下存在设备，无法删除");
        }
        productService.removeById(id);
    }

    private ProductVO toVO(DmProductDO entity) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
