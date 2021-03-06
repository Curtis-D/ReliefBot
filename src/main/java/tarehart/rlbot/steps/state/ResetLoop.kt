package tarehart.rlbot.steps.state

import rlbot.cppinterop.RLBotDll
import rlbot.gamestate.GameState
import tarehart.rlbot.AgentInput
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime

class ResetLoop(private val gameState: () -> GameState, private val duration: Duration) {

    private var nextReset = GameTime(0)

    /**
     * @param input Current agent input, used for game time
     * @return true if the game has been reset
     */
    fun check(input: AgentInput) : Boolean {
        if (input.time > nextReset) {
            reset(input)
            return true
        }
        return false
    }

    fun reset(input: AgentInput) {
        RLBotDll.setGameState(gameState.invoke().buildPacket())
        nextReset = input.time + duration
    }

}
