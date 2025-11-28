package com.jbm.cluster.job.execute;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.otter.canal.common.utils.JsonUtils;
import com.jbm.cluster.api.model.job.rule.NodeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author scolin
 * @description 代码节点执行器（执行用户编写的Java/JavaScript代码）
 * @date 2025/11/26
 */
@Component
@Slf4j
public class CodeNodeExecutor implements NodeExecutor {
    private static final ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();

    @Override
    public NodeExecutionResult execute(NodeData node, Map<String, Object> inputData) {
        try {
            Map<String, Object> nodeData = node.getData();

            // 获取代码
            String code = (String) nodeData.get("code");
            if (code == null || code.trim().isEmpty()) {
                return NodeExecutionResult.error("代码节点的代码不能为空");
            }

            // 获取输入变量和输出变量的定义
            List<Map<String, Object>> inputVariables = (List<Map<String, Object>>) nodeData.get("inputVariables");
            List<Map<String, Object>> outputVariables = (List<Map<String, Object>>) nodeData.get("outputVariables");

            // 执行代码
            Map<String, Object> executionContext = executeCode(code, inputData, inputVariables, outputVariables);

            // 检查是否设置了__WAIT_TRIGGER__标识
            Object waitTrigger = executionContext.get("__WAIT_TRIGGER__");
            log.info("代码节点{}的__WAIT_TRIGGER__值: {}, 类型: {}", 
                node.getId(), waitTrigger, waitTrigger != null ? waitTrigger.getClass().getName() : "null");
            
            if (waitTrigger != null && Boolean.parseBoolean(waitTrigger.toString())) {
                // 代码节点设置了等待标识，进入等待触发状态
                log.info("代码节点{}检测到__WAIT_TRIGGER__=true，进入等待触发状态", node.getId());

                // 获取触发类型和触发键
                String triggerType = nodeData.getOrDefault("triggerType", "code_waiting").toString();
                String triggerKey = nodeData.getOrDefault("triggerKey", node.getId()).toString();

                // 过滤掉__WAIT_TRIGGER__，回增其他输出变量
                executionContext.remove("__WAIT_TRIGGER__");

                return NodeExecutionResult.waiting(triggerType, triggerKey);
            } else {
                log.info("代码节点{}未设置等待标识，仃需执行成功", node.getId());
            }

            return NodeExecutionResult.success(executionContext);

        } catch (Exception e) {
            log.error("代码节点执行失败", e);
            return NodeExecutionResult.error("代码节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行用户代码
     */
    private Map<String, Object> executeCode(String code,
            Map<String, Object> inputData,
            List<Map<String, Object>> inputVariables,
            List<Map<String, Object>> outputVariables) throws Exception {
        // 使用JavaScript引擎执行代码（可以执行类似Java的语法）
        ScriptEngine engine = ENGINE_MANAGER.getEngineByName("JavaScript");

        // 将输入参数注入到执行上下文
        if (inputData != null) {
            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
        }

        // 执行用户代码
        engine.eval(code);

        // 收集输出变量
        Map<String, Object> outputData = new HashMap<>(inputData != null ? inputData : new HashMap<>());

        // 首先，从engine中提取所有全局变量（包括__WAIT_TRIGGER__）
        try {
            Object waitTrigger = engine.get("__WAIT_TRIGGER__");
            if (waitTrigger != null) {
                outputData.put("__WAIT_TRIGGER__", waitTrigger);
            }
        } catch (Exception e) {
            // 如果__WAIT_TRIGGER__不存在，忽略
            log.debug("未找到__WAIT_TRIGGER__变量", e);
        }

        // 然后，收集outputVariables中定义的其他输出变量
        if (outputVariables != null && !outputVariables.isEmpty()) {
            for (Map<String, Object> outputVar : outputVariables) {
                String varName = (String) outputVar.get("name");
                if (varName != null) {
                    Object value = engine.get(varName);
                    outputData.put(varName, value);
                }
            }
        }

        return outputData;
    }

    @Override
    public String getSupportedType() {
        return "code";
    }
}
