package com.salk.migration.shell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * 自定义shell模式
 * 
 * @author salkli
 * @since 2022/4/9
 **/
@Component
public class PaasPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        return new AttributedString("salk-mshell:>", AttributedStyle.BOLD.foreground(AttributedStyle.RED));
    }
}
