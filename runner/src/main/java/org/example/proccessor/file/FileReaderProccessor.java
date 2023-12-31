package org.example.proccessor.file;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Proccessor;
import org.example.common.ProccessorDef;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileReaderProccessor extends Proccessor {

    @Override
    public boolean canProccess(ProccessorDef def){
        return FileReaderDef.PROCCESSOR_NAME.equalsIgnoreCase(def.getName());
    }

    @Override
    public void run(ProccessorDef def) {
        FileReaderDef fileReaderDef = (FileReaderDef) def;
        log.info("File reader proccess");
        log.info("File name: {}", fileReaderDef.getFileName());
    }
}
