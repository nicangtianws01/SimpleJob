package org.example.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.cache.ProccessorCache;
import org.example.common.Proccessor;
import org.example.common.ProccessorDef;
import org.example.common.TaskDef;
import org.example.util.ObjectMapperUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基础执行类
 * 能够执行任务流
 */
@Slf4j
@Component
public class BaseRunner implements Runner {

    @Value("${org.example.package}")
    private String basePackage;

    @Override
    public void run(String config) {
        try {
            // 输出插件个数
            Map<String, Proccessor> proccessors = ProccessorCache.getProccessors();
            log.info("Plugin number: {}", proccessors.size());

            ObjectMapper objectMapper = ObjectMapperUtil.getObjectMapper();

            // 解析配置文件
            TaskDef taskDef = objectMapper.readValue(config, TaskDef.class);

            // 通过spel表达式进行变量替换
            Map<String, String> vars = taskDef.getVars();
            EvaluationContext context = new StandardEvaluationContext();
            vars.forEach(context::setVariable);
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(objectMapper.writeValueAsString(taskDef),
                    new TemplateParserContext("${", "}"));
            String newDef = (String) expression.getValue(context);
            taskDef = objectMapper.readValue(newDef, TaskDef.class);

            // 执行任务
            List<ProccessorDef> steps = taskDef.getSteps();
            for (ProccessorDef def : steps) {
                Proccessor proccessor = proccessors.get(def.getName());
                if (proccessor != null && proccessor.canProccess(def)) {
                    proccessor.run(def);
                } else {
                    throw new RuntimeException("匹配不到对应的执行器!");
                }
            }
        } catch (JsonProcessingException e) {
            log.error("解析配置文件失败：{}", e.getMessage());
        }
    }
}
