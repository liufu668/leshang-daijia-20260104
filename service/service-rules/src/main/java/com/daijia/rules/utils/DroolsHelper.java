package com.daijia.rules.utils;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

/**
 * 接收规则字符串（规则文件的文本内容），返回KieSession
 */
public class DroolsHelper {

    //private static final String RULES_CUSTOMER_RULES_DRL = "rules/FeeRule.drl";

    public static KieSession loadForRule(String drlStr) {
        KieServices kieServices = KieServices.Factory.get();

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        //kieFileSystem.write(ResourceFactory.newClassPathResource(drlStr));

        // 正确方式：直接写入规则内容，指定一个虚拟路径
        String path = "src/main/resources/rules/temp_" + System.currentTimeMillis() + ".drl";
        kieFileSystem.write(path, drlStr);  // 直接写入字符串内容

        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();

        KieModule kieModule = kb.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        return kieContainer.newKieSession();
    }
}
