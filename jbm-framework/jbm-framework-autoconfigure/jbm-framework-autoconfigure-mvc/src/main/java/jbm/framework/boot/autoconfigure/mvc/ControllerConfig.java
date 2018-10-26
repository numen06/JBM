package jbm.framework.boot.autoconfigure.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

/**
 * @author wesley.zhang
 * @date 2017年10月18日
 * @version 1.0
 *
 */
@ControllerAdvice
public class ControllerConfig {

	@Autowired
	ResourceUrlProvider resourceUrlProvider;

	@ModelAttribute("urls")
	public ResourceUrlProvider urls() {
		return this.resourceUrlProvider;
	}

}
