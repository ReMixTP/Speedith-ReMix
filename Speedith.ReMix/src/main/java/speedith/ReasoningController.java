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
import static speedith.i18n.Translations.*;
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
 * This controller handles two key tasks:
 *   - Provide reasoning rules to ReMix.
 *   - Apply reasoning rules to Spider diagram states.
 */
@RestController
@RequestMapping("/api/v0.0.1/reasoning")
public class ReasoningController {

    @RequestMapping("")
    public Success<List<RRule>> availableRules() {
        List<RRule> ruleList = new ArrayList<RRule>();
        // Rules(language, automated/manual, identifier, description)
        ruleList.add(new RRule("lang.speedith", "manual", "SpeedithManual",
                              "Apply the given Speedith rule."));
        return new Success("reasoning", ruleList);
    }

    @RequestMapping(value="/apply", method=RequestMethod.POST)
    public Success<List<ReMixFormula>> applyRule(@RequestParam("id") String ruleID, @RequestBody String jsonData) {
        Success<List<ReMixFormula>> result;
        switch (ruleID) {
        case "SpeedithManual":
            try { result = this.applySpecificRule(JsonData.fromJson(jsonData)); }
            catch (java.io.IOException ex) { result = new Success(false, "Bad POST data."); }
            break;
        default:
            result = new Success(false, String.format("There is no rule with ID '%s'.", ruleID));
            break;
        }
        return result;
    }

    /**
     * applySpecificRule
     * Given some information from the POST data (now inside the jsonData),
     * apply a rule to a spider diagram, and produce some new, consequential spider diagrams as a result.
     * This function is massive and awful, but that is because it is mostly ripped from the CLI stuff
     * and shoe-horned into this JSON setting.
     */
    private Success<List<ReMixFormula>> applySpecificRule(JsonData jsonData) {
        // Parse the jsonData to the native format.
        try {
            String speedithFormula = jsonData.getGoal();
            ExtraInfo extraInfo = ExtraInfo.fromJson(jsonData.getExtraInfo());
            String ruleName = extraInfo.getRuleName();
            String spider = extraInfo.getSpider();
            int subDiagramIndex = extraInfo.getSubDiagramIndex();
            String regionString = extraInfo.getRegion();
            Region region = (regionString == null) ? null : SpiderDiagramsReader.readRegion(regionString);
            SpiderDiagram readSpiderDiagram = (speedithFormula == null) ? null : SpiderDiagramsReader.readSpiderDiagram(speedithFormula);
            if (readSpiderDiagram != null) {
                InferenceRule<? extends RuleArg> inferenceRule = InferenceRules.getInferenceRule(ruleName);
                if (inferenceRule == null) {
                    return new Success(false, i18n("APP_UNKNOWN_INFERENCE_RULE"));
                }
                RuleArg ruleArg;
                if (subDiagramIndex >= 0) {
                    if (!Strings.isNullOrEmpty(spider)) {
                        if (region != null) {
                            ruleArg = new SpiderRegionArg(0, subDiagramIndex, spider, region);
                        } else {
                            ruleArg = new SpiderArg(0, subDiagramIndex, spider);
                        }
                    } else {
                        ruleArg = new SubDiagramIndexArg(0, subDiagramIndex);
                    }
                } else {
                    return new Success(false, i18n("MAIN_SUBDIAGRAM_INDEX_NEGATIVE"));
                }
                RuleApplicationResult subGoals = inferenceRule.apply(ruleArg, new Goals(Arrays.asList(readSpiderDiagram)));
                List<ReMixFormula> newGoals = new ArrayList();
                if (subGoals != null && subGoals.getGoals() != null && subGoals.getGoals().getGoalsCount() > 0) {
                    SDExporter exporter = SDExporting.getExporter("DefaultOutputFormat", new HashMap<String, String>());
                    for (int i = 0; i < subGoals.getGoals().getGoalsCount(); i++) {
                        readSpiderDiagram = subGoals.getGoals().getGoalAt(i);
                        if (exporter != null) {
                            newGoals.add(ReMixFormula.fromJson(exporter.export(readSpiderDiagram)));
                        } else {
                            return new Success(false, i18n("APP_UNKNOWN_EXPORTER"));
                        }
                    }
                }
                return new Success("newGoals", newGoals);
            } else {
                return new Success(false, i18n("ERR_CLI_PARSE_FAILED"));
            }
        } catch (Exception err) {
            return new Success(false, "An unknown error occurred.");
        }
    }
}

class RRule extends ArrayList<String> {
    public RRule(String lang, String automated, String ruleID, String description) {
        super();
        this.add(lang);
        this.add(automated);
        this.add(ruleID);
        this.add(description);
    }
}

class ExtraInfo {
    public String ruleName;
    public String spider;
    public int subDiagramIndex;
    public String region;

    public static ExtraInfo fromJson(String jsonData)
    throws java.io.IOException {
        return new ObjectMapper().readValue(jsonData, ExtraInfo.class);
    }

    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setSpider(String spider) { this.spider = spider; }
    public void setSubDiagramIndex(int subDiagramIndex) { this.subDiagramIndex = subDiagramIndex; }
    public void setRegion(String region) { this.region = region; }
    public String getRuleName() { return this.ruleName; }
    public String getSpider() { return this.spider; }
    public int    getSubDiagramIndex() { return this.subDiagramIndex; }
    public String getRegion() { return this.region; }
}
