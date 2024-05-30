package com.jbm.framework.modbus;

import cn.hutool.core.util.ObjectUtil;
import com.serotonin.modbus4j.BasicProcessImage;
import com.serotonin.modbus4j.exception.IllegalDataAddressException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author fanscat
 * @createTime 2024/5/29 21:29
 */
@Slf4j
public class ValueChangeProcessImage extends BasicProcessImage {

    public ValueChangeProcessImage(int slaveId) {
        super(slaveId);
    }

    @Override
    public synchronized void writeCoil(int offset, boolean value) throws IllegalDataAddressException {
        if (!ObjectUtil.equals(getCoil(offset), value)) {
            super.writeCoil(offset, value);
        }
    }

    @Override
    public synchronized void writeHoldingRegister(int offset, short value) throws IllegalDataAddressException {
        if (!ObjectUtil.equals(getHoldingRegister(offset), value)) {
            super.writeHoldingRegister(offset, value);
        }
    }

    public synchronized void writeHoldingRegisters(int offset, short[] shorts) throws IllegalDataAddressException {
        for (int i = 0; i < shorts.length; i++) {
            this.writeHoldingRegister(offset + i, shorts[i]);
        }
    }
}
