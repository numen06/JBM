package jbm.framework.boot.autoconfigure.taskflow.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JbmWork {
}
