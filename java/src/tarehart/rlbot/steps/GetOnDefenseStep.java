package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;

public class GetOnDefenseStep implements Step {
    private boolean isComplete = false;
    private SplineHandle targetLocation = null;

    private static final float GOAL_DISTANCE = 97;
    private static final float HANDLE_LENGTH = 50;

    public static final SplineHandle BLUE_GOAL = new SplineHandle(new Vector3(0, -GOAL_DISTANCE, 0), new Vector3(-HANDLE_LENGTH, 0, 0), new Vector3(HANDLE_LENGTH, 0, 0));
    public static final SplineHandle ORANGE_GOAL = new SplineHandle(new Vector3(0, GOAL_DISTANCE, 0), new Vector3(-HANDLE_LENGTH, 0, 0), new Vector3(HANDLE_LENGTH, 0, 0));

    private Plan plan;

    public AgentOutput getOutput(AgentInput input) {

        if (targetLocation == null) {
            init(input);
        }

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (!needDefense(input) || SteerUtil.getDistanceFromMe(input, targetLocation.getLocation()) < 20) {
            isComplete = true;
            return new AgentOutput().withSlide(true);
        }

        Vector3 target;
        if (targetLocation.isWithinHandleRange(input.getMyPosition())) {
            target = targetLocation.getLocation();
        } else {
            target = targetLocation.getFarthestHandle(input.ballPosition);
        }


        if (input.getMyBoost() < 1 && target.distance(input.getMyPosition()) > 60 && Math.abs(SteerUtil.getCorrectionAngleRad(input, target)) < Math.PI / 20) {
            System.out.println("Front flipping toward goal!");
            plan = SetPieces.frontFlip();
            plan.begin();
            return plan.getOutput(input);
        } else {
            return SteerUtil.steerTowardPosition(input, target);
        }
    }

    private void init(AgentInput input) {
        targetLocation = input.team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    public static boolean needDefense(AgentInput input) {

        SplineHandle myGoal = input.team == Bot.Team.BLUE ? GetOnDefenseStep.BLUE_GOAL : GetOnDefenseStep.ORANGE_GOAL;

        double relativeBallY = input.ballPosition.y - input.getMyPosition().y;
        double relativeGoalY = myGoal.getLocation().y - input.getMyPosition().y;
        return relativeBallY * relativeGoalY > 0 && Math.abs(relativeGoalY) > 10;
    }
}