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
 * This controller handles two key tasks:
 *   - Provide reasoning rules to ReMix.
 *   - Apply reasoning rules to Spider diagram states.
 */
@RestController
@RequestMapping("/api/v0.0.1/reasoning")
public class ReasoningController {

    @RequestMapping("")
    public Success<List<Rule>> availableRules() {
        List<Rule> ruleList = new ArrayList<Rule>();
        // Rules(language, automated/manual, identifier, description)
        ruleList.add(new Rule("lang.speedith", "automated", "SpeedithAuto",
                              "Let Speedith work out the best way to proceed with the proof."));
        return new Success("reasoning", ruleList);
    }

    @RequestMapping(value="/apply", method=RequestMethod.POST)
    public Success<List<ReMixFormula>> applyRule(@RequestParam("id") String ruleID) {
        Success<List<ReMixFormula>> result;
        switch (ruleID) {
        // case "SpeedithAuto":
        //     result = this.applyAuto();
        //     break;
        default:
            result = new Success(false, String.format("There is no rule with ID '%s'.", ruleID));
            break;
        }
        return result;
    }
}

class Rule extends ArrayList<String> {
    public Rule(String lang, String automated, String ruleID, String description) {
        super();
        this.add(lang);
        this.add(automated);
        this.add(ruleID);
        this.add(description);
    }
}
