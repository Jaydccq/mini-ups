package com.miniups.shortlink.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ShortLinkSentinelConfig {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkSentinelConfig.class);

    private final ShortLinkProperties properties;

    public ShortLinkSentinelConfig(ShortLinkProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initSentinelRules() {
        List<ParamFlowRule> rules = new ArrayList<>();
        ShortLinkProperties.Sentinel sentinel = properties.getSentinel();

        ParamFlowRule createRule = new ParamFlowRule("shortlink-create")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(sentinel.getCreateThresholdPerSecond());
        rules.add(createRule);

        ParamFlowRule redirectRule = new ParamFlowRule("shortlink-redirect")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(sentinel.getRedirectThresholdPerSecond());
        rules.add(redirectRule);

        ParamFlowRuleManager.loadRules(rules);
        log.info("Sentinel param flow rules for short link initialized: {}", rules.size());
    }
}
