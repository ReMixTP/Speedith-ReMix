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
 * Respond to top-level probing by ReMix.
 * When ReMix probes the top-level access-point,
 * we should return a summary of the plug-in
 * matching the ReMix specification.
 */
@RestController
public class TopLevelController {

    /**
     * Handle the top-level request for plug-in information.
     * We only provide a single plug-in, but the interface expects a list.
     */
    @RequestMapping("/")
    public List<PluginInformation> home() {
        List<PluginInformation> pluginList = new ArrayList<PluginInformation>();
        pluginList.add(new PluginInformation());
        return pluginList;
    }
}

/**
 * Holds the information about the plug-in. Used because
 * the Spring Boot framework expects a POJO to convert to JSON.
 * This is that object.
 */
class PluginInformation {
    static String id = "si.urbas.speedith";
    static String name = "Speedith";
    static String website = "https://github.com/speedith/speedith";
    static String version = "0.0.1";
    static String description = "Speedith: the spider diagram reasoner.";
    static String icon = "None";
    static String base = "http://aarons-macbook.local:5002/api/v0.0.1";
    static RouteMap provides = new RouteMap();

    // Getter stuff
    public String getId() { return id; }
    public String getName() { return name; }
    public String getWebsite() { return website; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getBase() { return base; }
    public RouteMap getProvides() { return provides; }
}

/**
 * The RouteMap is, much like the PluginInformation, used as a JSON
 * source. This relates each service to a sub-URL in the plug-in.
 */
class RouteMap {
    static String reason = "/reasoning";

    // Getter stuff
    public String getReason () { return reason; }
}
