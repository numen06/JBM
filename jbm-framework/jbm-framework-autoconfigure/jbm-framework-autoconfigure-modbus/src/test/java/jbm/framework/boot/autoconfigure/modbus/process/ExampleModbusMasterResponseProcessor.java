package jbm.framework.boot.autoconfigure.modbus.process;
import com.github.zengfr.easymodbus4j.func.AbstractRequest;
import com.github.zengfr.easymodbus4j.func.request.ReadCoilsRequest;
import com.github.zengfr.easymodbus4j.func.request.ReadDiscreteInputsRequest;
import com.github.zengfr.easymodbus4j.func.response.ReadCoilsResponse;
import com.github.zengfr.easymodbus4j.func.response.ReadDiscreteInputsResponse;
import com.github.zengfr.easymodbus4j.handle.impl.ModbusMasterResponseHandler;
import com.github.zengfr.easymodbus4j.processor.AbstractModbusProcessor;
import com.github.zengfr.easymodbus4j.processor.ModbusMasterResponseProcessor;
import com.github.zengfr.easymodbus4j.protocol.ModbusFunction;
import com.github.zengfr.easymodbus4j.util.ModbusFunctionUtil;

import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:44
 **/
public class ExampleModbusMasterResponseProcessor extends AbstractModbusProcessor implements ModbusMasterResponseProcessor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ExampleModbusMasterResponseProcessor.class);
    public ExampleModbusMasterResponseProcessor(short transactionIdentifierOffset) {
        super(transactionIdentifierOffset, true);
    }

    public boolean processResponseFrame(Channel channel, int unitId, AbstractRequest reqFunc, ModbusFunction respFunc) {
        boolean success=this.isRequestResponseMatch(reqFunc, respFunc);
        if (respFunc instanceof ReadCoilsResponse) {
            ReadCoilsResponse resp = (ReadCoilsResponse) respFunc;
            if (reqFunc instanceof ReadCoilsRequest) {
                ReadCoilsRequest req = (ReadCoilsRequest) reqFunc;
                // process business logic
                success=true;
            }
        }
        if (respFunc instanceof ReadDiscreteInputsResponse) {
            ReadDiscreteInputsResponse resp = (ReadDiscreteInputsResponse) respFunc;
            byte[] resutArray = resp.getInputStatus().toByteArray();
            byte[] valuesArray = ModbusFunctionUtil.getFunctionValues(respFunc);
            if (reqFunc instanceof ReadDiscreteInputsRequest) {
                ReadDiscreteInputsRequest req = (ReadDiscreteInputsRequest) reqFunc;
                // process business logic
                success=true;
            }
        }
        logger.debug(String.format("success:%s", success));
        return success;
    };
}