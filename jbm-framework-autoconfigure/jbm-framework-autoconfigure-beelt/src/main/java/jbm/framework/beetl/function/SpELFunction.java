package jbm.framework.beetl.function;

import org.beetl.core.Context;
import org.beetl.core.Function;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 执行一个SpEL表达式，返回结果得Function， 注意这个函数对象必须以Spring上下文Bean的形式提供给Beetl使用
 *
 * @author wesley.zhang
 * @version 0.1.0 暂时无法直接访问Beetl上下文中的临时变量，因为获取不到变量名
 */
public class SpELFunction implements Function, ApplicationContextAware {
	/* ----- ----- ----- ----- 属性 ----- ----- ----- ----- */
	/**
	 * Spel表达式解析器
	 */
	private final ExpressionParser expressionParser = new SpelExpressionParser();
	/**
	 * Spring 应用程序上下文
	 */
	private ApplicationContext applicationContext = null;

	/**
	 * Spring 应用程序上下文
	 *
	 * @param applicationContext
	 * @throws BeansException
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/* ----- ----- ----- ----- 其他方法 ----- ----- ----- ----- */
	/**
	 * 函数调用处理方法
	 *
	 * @param paras
	 * @param ctx
	 * @return
	 */
	@Override
	public Object call(Object[] paras, Context ctx) {
		// 参数异常
		if ((paras.length == 0) || (paras[0] == null) || (!(paras[0] instanceof String))) {
			throw new IllegalArgumentException("函数格式：Object spel(String expression)");
		}

		Expression expression = expressionParser.parseExpression((String) paras[0]);
		EvaluationContext evaluationContext = getEvaluationContext(ctx);

		return expression.getValue(evaluationContext);
	}

	/**
	 * 获取表达式执行上下文
	 *
	 * @param context
	 * @return
	 */
	private EvaluationContext getEvaluationContext(Context context) {
		// FUTURE 后续改进，将产生过一次的EvaluationContext放入Beetl当前处理的上下文中缓存，避免重复创建
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new BeetlContextPropertyAccessor(context));
		evaluationContext.addPropertyAccessor(new MapAccessor());
		evaluationContext.addPropertyAccessor(new EnvironmentAccessor());
		evaluationContext.setBeanResolver(new BeanFactoryResolver(applicationContext));
		return evaluationContext;
	}

	/**
	 * 允许SpEL访问Beetl上下文中变量的访问器
	 *
	 * @author wesley.zhang
	 */
	private static class BeetlContextPropertyAccessor implements PropertyAccessor {
		/* ----- ----- ----- ----- 属性 ----- ----- ----- ----- */
		/**
		 * Beetl上下文
		 */
		private Context context = null;

		/**
		 * 以Beetl上下文构造BeetlContextPropertyAccessor
		 *
		 * @param context
		 *            Beetl上下文
		 */
		public BeetlContextPropertyAccessor(Context context) {
			this.context = context;
		}

		/* ----- ----- ----- ----- 其他方法 ----- ----- ----- ----- */
		@Override
		public Class<Object>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return (target == null) && (findVar(name) != null);
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			return new TypedValue(findVar(name));
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			throw new UnsupportedOperationException();
		}

		/**
		 * 获取指定名字的Beetl变量，这里只能获取到全局变量，局部变量暂时无法获取
		 *
		 * @param name
		 * @return
		 */
		private Object findVar(String name) {
			return context.getGlobal(name);
		}
	}
}
