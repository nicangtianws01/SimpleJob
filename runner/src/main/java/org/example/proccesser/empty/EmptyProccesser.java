package org.example.proccesser.empty;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Proccesser;
import org.example.common.ProccesserDef;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmptyProccesser implements Proccesser<EmptyDef> {

    @Override
    public boolean canProccess(ProccesserDef def){
        return EmptyDef.PROCCESSER_NAME.equalsIgnoreCase(def.getName());
    }

    @Override
    public void run(EmptyDef def) {
        log.info("Empty proccess");
    }
}