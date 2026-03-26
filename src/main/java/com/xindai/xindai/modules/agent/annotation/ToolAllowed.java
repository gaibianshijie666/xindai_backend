package com.xindai.xindai.modules.agent.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolAllowed {
    String[] portals() default {};
}
