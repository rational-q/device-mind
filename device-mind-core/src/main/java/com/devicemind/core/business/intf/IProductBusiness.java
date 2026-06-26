package com.devicemind.core.business.intf;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.devicemind.core.model.dto.ProductCreateDTO;
import com.devicemind.core.model.dto.ProductPageQueryDTO;
import com.devicemind.core.model.dto.ProductUpdateDTO;
import com.devicemind.core.model.vo.ProductVO;

public interface IProductBusiness {

    Page<ProductVO> listPage(ProductPageQueryDTO query);

    ProductVO getById(Long id);

    void create(ProductCreateDTO dto);

    void update(Long id, ProductUpdateDTO dto);

    void delete(Long id);
}
