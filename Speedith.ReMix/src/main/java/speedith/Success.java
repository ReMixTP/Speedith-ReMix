package speedith.remix;

import java.util.*;
import java.io.*;

import speedith.core.lang.DiagramType;
import speedith.core.lang.Region;
import speedith.core.lang.SpiderDiagram;
import speedith.core.lang.export.SDExportProvider;
import speedith.core.lang.export.SDExporter;
import speedith.core.lang.export.SDExporting;
import speedith.core.lang.reader.ReadingException;
import speedith.core.lang.reader.SpiderDiagramsReader;
import speedith.core.reasoning.Goals;
import speedith.core.reasoning.InferenceRule;
import speedith.core.reasoning.InferenceRuleProvider;
import speedith.core.reasoning.InferenceRules;
import speedith.core.reasoning.RuleApplicationResult;
import speedith.core.reasoning.args.RuleArg;
import speedith.core.reasoning.args.SpiderArg;
import speedith.core.reasoning.args.SpiderRegionArg;
import speedith.core.reasoning.args.SubDiagramIndexArg;
import propity.util.Strings;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;


/**
 * A simple "success" wrapper.
 * For JSON purposes, basically.
 */
@JsonSerialize(using=CustomSuccessSerializer.class)
public class Success <T> {
    private boolean result;
    private String reason;
    private String key;
    private T content;

    public Success(String key, T content) {
        this.result = true;
        this.reason = "";
        this.key = key;
        this.content = content;
    }

    public Success(boolean succeeded, String reason) {
        if (succeeded) {  // Succeeded, but using wrong constructor?
            throw new RuntimeException("Succeeded and failed simultaneously?");
        }
        this.result = false;
        this.reason = reason;
        this.key = "";
        this.content = null;
    }

    public boolean getResult() { return result; }
    public String getReason() { return reason; }
    public String getKey() { return key; }
    public T getContent() { return content; }
}

class CustomSuccessSerializer extends JsonSerializer<Success> {
  public void serialize(Success value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    if (value.getResult()) {
        // Success!
        jgen.writeObjectField("result", "success");
        jgen.writeObjectField(value.getKey(), value.getContent());
    } else {
        // Failure...
        jgen.writeObjectField("result", "failure");
        jgen.writeObjectField("reason", value.getReason());
    }
    jgen.writeEndObject();
  }
}
