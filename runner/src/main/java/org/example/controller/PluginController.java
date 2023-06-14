package org.example.controller;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.example.cache.ProccesserCache;
import org.example.cache.ProccesserDefCache;
import org.example.common.Proccesser;
import org.example.common.ProccesserDef;
import org.example.common.TaskDef;
import org.example.common.anno.JsonTypeDef;
import org.example.service.PluginService;
import org.example.util.ClassLoaderUtil;
import org.example.util.SpringBeanRegister;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/plugin")
public class PluginController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpringBeanRegister register;

    private final PluginService pluginService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PluginController(SpringBeanRegister register, PluginService pluginService) {
        this.register = register;
        this.pluginService = pluginService;
    }

    /**
     * 上传并载入插件
     *
     * @return
     * @throws ClassNotFoundException
     * @throws MalformedURLException
     */
    @RequestMapping(value = "/push", method = RequestMethod.GET)
    public String pushPlugin() throws ClassNotFoundException, MalformedURLException {
        File file = new File("C:/Users/nicangtianws/data/test/inside-1.0-SNAPSHOT.jar");
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(file.toURI().toURL());
        assert classLoader != null;

        logger.info(classLoader.getClass().getName());

        classLoader.loadClass("org.example.proccesser.inside.InsideDef");
        Class<?> clazz = classLoader.loadClass("org.example.proccesser.inside.InsideProccesser");

        register.registerBean(clazz.getSimpleName(), clazz);

        Proccesser proccesser = (Proccesser) register.getBean(clazz.getSimpleName());
        ProccesserCache.addProccesser(clazz.getSimpleName(), proccesser);
        return "ok";
    }

    @RequestMapping(value = "/runTask", method = RequestMethod.GET)
    public String runTask() throws IOException {
        // 输出插件个数
        Map<String, Proccesser> proccessers = ProccesserCache.getProccessers();
        logger.info("Plugin number: {}", proccessers.size());

        // 读取配置文件，执行任务
        StringBuilder configBuilder = new StringBuilder();
        File file = ResourceUtils.getFile("classpath:task-config/task01.json");
        new StringBuilder();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                configBuilder.append(line);
            }
        }
        String config = configBuilder.toString();

        // 从项目中加载def子类
        Reflections reflections = new Reflections("org.example.proccesser");
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(JsonTypeInfo.class);
        // 遍历基类
        for (Class<?> type : types) {
            // 扫描子类
            Set<?> clazzs = reflections.getSubTypesOf(type);
            if(CollectionUtils.isEmpty(clazzs)){
                continue;
            }
            // 注册子类
            for (Object clazz : clazzs) {
                omSubTypeSet((Class<?>) clazz);
            }
        }

        // 从插件load的类中查找def子类
        List<Class<?>> classes = ProccesserDefCache.getProccesserDefs();
        classes.stream().filter(clazz -> {
            JsonTypeDef annotation = clazz.getAnnotation(JsonTypeDef.class);
            return annotation != null && clazz.getSimpleName().endsWith("Def");
        }).forEach(this::omSubTypeSet);

        // 解析配置文件
        TaskDef taskDef = objectMapper.readValue(config, TaskDef.class);

        // 执行步骤
        List<ProccesserDef> steps = taskDef.getSteps();
        for (ProccesserDef def : steps) {
            Proccesser proccesser = proccessers.get(def.getName());
            if (proccesser != null) {
                proccesser.run(def);
            } else {
                logger.info("Proccesser not found!");
            }
        }

        return config;
    }

    public void omSubTypeSet(Class<?> clazz){
        // 跳过接口和抽象类
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }
        // 提取 JsonTypeDef 注解
        JsonTypeDef extendClassDefine = clazz.getAnnotation(JsonTypeDef.class);
        if (extendClassDefine == null) {
            return;
        }
        // 注册子类型，使用名称建立关联
        objectMapper.registerSubtypes(new NamedType(clazz, extendClassDefine.value()));
    }

    @RequestMapping(value = "/loadAll", method = RequestMethod.GET)
    public String loadAll() {
        pluginService.loadAllPlugins();
        return "ok";
    }

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {
        return "ok";
    }
}