package speedith;

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


/**
 * A simple formula wrapper for ReMix communication.
 */
public class ReMixFormula {
    private String label;
    private Object data;
    private String language;
    private List<String> variables;
    private List<String> placeholders;

    public ReMixFormula(String label,
                        Object data,
                        String language,
                        List<String> variables,
                        List<String> placeholders) {
        this.label = label;
        this.data = data;
        this.language = language;
        this.variables = variables;
        this.placeholders = placeholders;
    }
}
