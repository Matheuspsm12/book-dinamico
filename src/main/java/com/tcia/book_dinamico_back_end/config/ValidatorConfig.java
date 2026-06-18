package com.tcia.book_dinamico_back_end.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidatorConfig {

    @Bean
    MessageSource messageSource() {
        ReloadableResourceBundleMessageSource msgSource =
                new ReloadableResourceBundleMessageSource();

        msgSource.setBasenames("classpath:messages", "classpath:ValidationMessages", "classpath:errors");
        msgSource.setDefaultEncoding("UTF-8");
        msgSource.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        return msgSource;
    }

    @Bean
    LocalValidatorFactoryBean validatorFactoryBean() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

}
