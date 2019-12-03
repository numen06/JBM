package jbm.framework.boot.autoconfigure.modbus.process;

import com.github.zengfr.easymodbus4j.common.util.BitSetUtil;
import com.github.zengfr.easymodbus4j.common.util.RegistersUtil;
import com.github.zengfr.easymodbus4j.func.request.*;
import com.github.zengfr.easymodbus4j.func.response.*;
import com.github.zengfr.easymodbus4j.processor.AbstractModbusProcessor;
import com.github.zengfr.easymodbus4j.processor.ModbusSlaveRequestProcessor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.BitSet;
import java.util.Random;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:30
 **/
public class ExampleModbusSlaveRequestProcessor extends AbstractModbusProcessor implements ModbusSlaveRequestProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ExampleModbusSlaveRequestProcessor.class);
    protected static Random random = new Random();

    public ExampleModbusSlaveRequestProcessor(short transactionIdentifierOffset) {
        super(transactionIdentifierOffset, true);
    }

    @Override
    public WriteSingleCoilResponse writeSingleCoil(short unitIdentifier, WriteSingleCoilRequest request) {

        return new WriteSingleCoilResponse(request.getOutputAddress(), request.isState());
    }

    @Override
    public WriteSingleRegisterResponse writeSingleRegister(short unitIdentifier, WriteSingleRegisterRequest request) {
        return new WriteSingleRegisterResponse(request.getRegisterAddress(), request.getRegisterValue());
    }

    @Override
    public ReadCoilsResponse readCoils(short unitIdentifier, ReadCoilsRequest request) {
        BitSet coils = new BitSet(request.getQuantityOfCoils());
        coils = BitSetUtil.getRandomBits(request.getQuantityOfCoils(), random);
        if (coils.size() > 0 && random.nextInt(99) < 66)
            coils.set(0, false);
        return new ReadCoilsResponse(coils);
    }

    @Override
    public ReadDiscreteInputsResponse readDiscreteInputs(short unitIdentifier, ReadDiscreteInputsRequest request) {
        BitSet coils = new BitSet(request.getQuantityOfCoils());
        coils = BitSetUtil.getRandomBits(request.getQuantityOfCoils(), random);

        return new ReadDiscreteInputsResponse(coils);
    }

    @Override
    public ReadInputRegistersResponse readInputRegisters(short unitIdentifier, ReadInputRegistersRequest request) {
        int[] registers = new int[request.getQuantityOfInputRegisters()];
        registers = RegistersUtil.getRandomRegisters(registers.length, 1, 1024, random);

        return new ReadInputRegistersResponse(registers);
    }

    @Override
    public ReadHoldingRegistersResponse readHoldingRegisters(short unitIdentifier, ReadHoldingRegistersRequest request) {
        int[] registers = new int[request.getQuantityOfInputRegisters()];
        registers = RegistersUtil.getRandomRegisters(registers.length, 1, 1024, random);
        // RegistersFactory.getInstance().getOrInit(unitIdentifier).getHoldingRegister(request.getStartingAddress());
        return new ReadHoldingRegistersResponse(registers);

    }

    @Override
    public WriteMultipleCoilsResponse writeMultipleCoils(short unitIdentifier, WriteMultipleCoilsRequest request) {
        return new WriteMultipleCoilsResponse(request.getStartingAddress(), request.getQuantityOfOutputs());
    }

    @Override
    public WriteMultipleRegistersResponse writeMultipleRegisters(short unitIdentifier, WriteMultipleRegistersRequest request) {
        // RegistersFactory.getInstance().getOrInit(unitIdentifier).setHoldingRegister(request.getStartingAddress(),
        // request.getRegisters());
        return new WriteMultipleRegistersResponse(request.getStartingAddress(), request.getQuantityOfRegisters());
    }

}
