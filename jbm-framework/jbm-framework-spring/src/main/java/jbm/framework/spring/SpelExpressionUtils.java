package jbm.framework.spring;

import cn.hutool.core.util.StrUtil;
import jbm.framework.spring.config.SpringContextHolder;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * el表达式的工具类
 * @author wesley
 */
public class SpelExpressionUtils {

    /**
     * 通过表达式转译
     *
     * @param expr
     * @param desiredResultType
     * @param <T>
     * @return
     */
    public static <T> T parseExpression(String expr, Class<T> desiredResultType) {
        if (StrUtil.isNotBlank(expr)) {
            try {
                ExpressionParser expressionParser = new SpelExpressionParser();
                StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext(SpringContextHolder.getApplicationContext());
                expr = parseEnvironment(expr);
                Expression expression = expressionParser.parseExpression(expr);
                return expression.getValue(standardEvaluationContext, desiredResultType);
            } catch (Exception e) {
                throw new RuntimeException("表达式识别错误", e);
            }
        }
        return null;
    }

    /**
     * 通过表达式转译
     *
     * @param expr
     * @return
     */
    public static String parseExpression(String expr) {
        if (StrUtil.isNotBlank(expr)) {
            try {
                ExpressionParser expressionParser = new SpelExpressionParser();
                StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext(SpringContextHolder.getApplicationContext());
                expr = parseEnvironment(expr);
                //如果存在#号代表的是表示
                if (StrUtil.contains(expr, "#")) {
                    Expression expression = expressionParser.parseExpression(expr);
                    return expression.getValue(standardEvaluationContext, String.class);
                } else {
                    return expr;
                }
            } catch (Exception e) {
                throw new RuntimeException("表达式识别错误", e);
            }
        }
        return null;
    }

    /**
     * 通过表达式转译环境变量
     *
     * @param expr
     * @return
     */
    public static String parseEnvironment(String expr) {
        if (StrUtil.isNotBlank(expr)) {
            try {
                return SpringContextHolder.getApplicationContext().getEnvironment().resolvePlaceholders(expr);
            } catch (Exception e) {
                throw new RuntimeException("环境变量设置错误", e);
            }
        }
        return expr;
    }


}
