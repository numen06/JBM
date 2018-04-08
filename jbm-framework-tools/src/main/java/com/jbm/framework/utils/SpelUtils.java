package com.jbm.framework.utils;

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 
 * SPEL表达式工具类
 * 
 * @author Wesley
 * 
 */
public class SpelUtils {

	private final static ParserContext parserContext = new ParserContext() {
		@Override
		public boolean isTemplate() {
			return true;
		}

		@Override
		public String getExpressionPrefix() {
			return "${";
		}

		@Override
		public String getExpressionSuffix() {
			return "}";
		}
	};

	public static StandardEvaluationContext createContext() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		return context;
	}

	public static StandardEvaluationContext createContext(Object rootObject) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(rootObject);
		return context;
	}

	public static StandardEvaluationContext appendContext(StandardEvaluationContext context, String name, Object value) {
		context.setVariable(name, value);
		return context;
	}

	public static StandardEvaluationContext appendContext(StandardEvaluationContext context, Map<String, Object> variables) {
		context.setVariables(variables);
		return context;
	}

	public static Expression doParseExpression(String expressionString) throws ParseException {
		ExpressionParser exp = new SpelExpressionParser();
		return exp.parseExpression(expressionString, parserContext);
	}

	public static Object parseExpression(String expressionString) {
		return doParseExpression(expressionString).getValue();
	}

	public static Object parseExpression(String expressionString, StandardEvaluationContext context) {
		return doParseExpression(expressionString).getValue(context);
	}

	public static Object parseExpression(String expressionString, Object value) {
		StandardEvaluationContext context = createContext(value);
		return doParseExpression(expressionString).getValue(context);
	}

	public static Object parseExpression(String expressionString, String name, Object value) {
		StandardEvaluationContext context = appendContext(createContext(), name, value);
		return doParseExpression(expressionString).getValue(context);
	}

	public static Object parseExpression(String expressionString, Map<String, Object> variables) {
		StandardEvaluationContext context = appendContext(createContext(), variables);
		return doParseExpression(expressionString).getValue(context);
	}
}
