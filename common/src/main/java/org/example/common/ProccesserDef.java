package org.example.common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.proccesser.empty.EmptyDef;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
        defaultImpl = EmptyDef.class, visible = true)
public class ProccesserDef {
    protected String type;
    protected String name;
    protected String desc;
}
