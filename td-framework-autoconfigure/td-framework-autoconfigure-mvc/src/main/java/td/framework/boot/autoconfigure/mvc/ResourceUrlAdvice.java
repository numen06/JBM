package td.framework.boot.autoconfigure.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

@ControllerAdvice
public class ResourceUrlAdvice {

	@Autowired
	private ResourceUrlProvider resourceUrlProvider;

	@ModelAttribute
	public void urls(Model model) {
		if (model.containsAttribute("urls"))
			return;
		model.addAttribute("urls", this.resourceUrlProvider);
	}

}