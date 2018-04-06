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

import isabelle.*;

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
 *   - Provide translation rules to ReMix.
 *   - Apply translation rules between Isabelle statements and Spider diagrams.
 */
@RestController
@RequestMapping("/api/v0.0.1/translation")
public class TranslationController {

    @RequestMapping("")
    public Success<List<TRule>> availableRules() {
        List<TRule> ruleList = new ArrayList<TRule>();
        ruleList.add(new TRule("lang.speedith", "lang.isabelle", "automated"));
        ruleList.add(new TRule("lang.isabelle", "lang.speedith", "automated"));
        return new Success("translations", ruleList);
    }

    @RequestMapping(value="/apply", method=RequestMethod.POST)
    public Success<List<ReMixFormula>> applyRule(@RequestParam("from") String fromLanguageId,
                                                 @RequestParam("to") String toLanguageId,
                                                 @RequestBody String jsonData) {
        Success<List<ReMixFormula>> result;
        try {
        if (fromLanguageId.equals("lang.isabelle") && toLanguageId.equals("lang.speedith")) {
            result = translateIsabelleToSpeedith(TranslationJsonData.fromJson(jsonData));
        } else if (fromLanguageId.equals("lang.speedith") && toLanguageId.equals("lang.isabelle")) {
            result = translateSpeedithToIsabelle(TranslationJsonData.fromJson(jsonData));
        } else {
            result = new Success(false, "Unknown translation combination.");
        }
        } catch (java.io.IOException ex) { result = new Success(false, "Unable to parse JSON data."); }
        return result;
    }

    /**
     * We use Speedith to import Isabelle formulae and export Speedith formulae.
     */
    private Success<List<ReMixFormula>> translateIsabelleToSpeedith(TranslationJsonData jsonData) {
        /*Term term = Term_XML.Decode.term(YXML.parse_body(jsonData.getFormula));
        SDExporter exporter = SDExporting.getExporter("DefaultOutputFormat", new HashMap<String, String>())
        try {
            SpiderDiagram spiderDiagram = termToSpiderDiagram(term);
            return Success("formula", RemixFormula.fromJson(exporter.export(spiderDiagram)));
        }
        catch (speedith.core.lang.reader.ReadingException ex) { return new Success(false, "Unable to convert Isabelle formula to spider diagram."); }
        catch (speedith.core.lang.export.ExportException ex) { return new Success(false, "Unable to export spider diagram."); }
        catch (java.io.IOException ex) { return new Success(false, "Unable to create JSON from spider diagram."); }*/
        return new Success(false, "Currently unavailable.");
    }

    /**
     * We use Speedith to import Speedith formulae and export Isabelle formulae.
     */
    private Success<List<ReMixFormula>> translateSpeedithToIsabelle(TranslationJsonData jsonData) {
        String speedithFormula = jsonData.getFormula();
        SpiderDiagram readSpiderDiagram;
        try {
            readSpiderDiagram = (speedithFormula == null) ? null : SpiderDiagramsReader.readSpiderDiagram(speedithFormula);
        } catch (speedith.core.lang.reader.ReadingException ex) { return new Success(false, "Malformed spider diagram format."); }
        SDExporter exporter = SDExporting.getExporter("Isabelle2011", new HashMap<String, String>());
        try {
            return new Success("formula", ReMixFormula.fromJson(exporter.export(readSpiderDiagram)));
        } catch (speedith.core.lang.export.ExportException ex) { return new Success(false, "Unable to export spider diagram to Isabelle formula."); }
        catch (java.io.IOException ex) { return new Success(false, "Unable to create JSON from Isabelle formula."); }
    }
}


class TRule extends ArrayList<String> {
    public TRule(String from, String to, String automated) {
        super();
        this.add(from);
        this.add(to);
        this.add(automated);
    }
}

class TranslationJsonData {
    public String formula;
    public String extraInfo;

    public static TranslationJsonData fromJson(String jsonData)
    throws java.io.IOException {
        return new ObjectMapper().readValue(jsonData, TranslationJsonData.class);
    }

    public void setFormula(String formula) { this.formula = formula; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public String getFormula() { return this.formula; }
    public String getExtraInfo() { return this.extraInfo; }
}
