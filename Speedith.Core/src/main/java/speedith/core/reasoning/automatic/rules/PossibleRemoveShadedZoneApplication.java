package speedith.core.reasoning.automatic.rules;

import speedith.core.lang.SpiderDiagram;
import speedith.core.lang.Zone;
import speedith.core.reasoning.*;
import speedith.core.reasoning.args.MultipleRuleArgs;
import speedith.core.reasoning.args.RuleArg;
import speedith.core.reasoning.args.ZoneArg;
import speedith.core.reasoning.automatic.AppliedRules;
import speedith.core.reasoning.automatic.wrappers.SpiderDiagramOccurrence;
import speedith.core.reasoning.rules.IntroShadedZone;
import speedith.core.reasoning.rules.RemoveShadedZone;

/**
 * @author Sven Linker [s.linker@brighton.ac.uk]
 */
public class PossibleRemoveShadedZoneApplication extends PossibleRuleApplication {

    private final Zone zone;

    public PossibleRemoveShadedZoneApplication(SpiderDiagramOccurrence target, InferenceRule<? super RuleArg> rule, Zone zone) {
        super(target, rule);
        this.zone = zone;
    }

    @Override
    public RuleArg getArg(int subgoalindex)  {
        int subDiagramIndex = getTarget().getOccurrenceIndex();
        ZoneArg zoneArg = new ZoneArg(subgoalindex, subDiagramIndex, zone);
        return new MultipleRuleArgs(zoneArg);
    }

    public Zone getZone() {
        return zone;
    }



    @Override
    public boolean isSuperfluous(Proof p, int subGoalIndex) {
        for (int i = 0; i < p.getRuleApplicationCount(); i++) {
            RuleApplication application = p.getRuleApplicationAt(i);
            if (application.getInferenceRule() instanceof IntroShadedZone) {
                MultipleRuleArgs args = (MultipleRuleArgs) application.getRuleArguments();
                MultipleRuleArgs thisArgs = (MultipleRuleArgs) getArg(subGoalIndex);
                // application is superfluous if :
                // a) both rules work on the same subgoal
                // b) the result of the already applied rule is the premiss of the current rule
                // c) both refer to the same zones

                if (args.getRuleArgs().containsAll(thisArgs.getRuleArgs())) {
                    ZoneArg arg = (ZoneArg) args.get(0);
                    SpiderDiagram result = p.getGoalsAt(i + 1).getGoalAt(arg.getSubgoalIndex()).getSubDiagramAt(arg.getSubDiagramIndex());
                    return getTarget().getDiagram().equals(result);
                }
            } else if (application.getInferenceRule() instanceof RemoveShadedZone) {
                MultipleRuleArgs args = (MultipleRuleArgs) application.getRuleArguments();
                MultipleRuleArgs thisArgs = (MultipleRuleArgs) getArg(subGoalIndex);
                // application is superfluous if the other rule
                // a) works on the same subgoal
                // b) and on the same subdiagram and
                // c) both refer to the same zone
                if (args.getRuleArgs().containsAll(thisArgs.getRuleArgs())) {
                    return true;
                }
            }
        }
        return false;
    }
}
