package com.jbm.examples.extendfield.business.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.examples.extendfield.business.mapper.MdExtendOrderMapper;
import com.jbm.examples.extendfield.business.mp.MdExtendOrder;
import org.springframework.stereotype.Service;

@Service
public class MdExtendOrderService extends ServiceImpl<MdExtendOrderMapper, MdExtendOrder> {
}
