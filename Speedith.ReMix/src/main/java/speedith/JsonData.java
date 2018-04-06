package speedith.remix;

import java.util.*;

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
 * Unwrap the JsonData string into a proper object.
 */
public class JsonData {
    public String goal;
    public String extraInfo;

    public static JsonData fromJson(String jsonData)
    throws java.io.IOException {
        return new ObjectMapper().readValue(jsonData, JsonData.class);
    }

    public void setGoal(String goal) { this.goal = goal; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public String getGoal() { return this.goal; }
    public String getExtraInfo() { return this.extraInfo; }
}
