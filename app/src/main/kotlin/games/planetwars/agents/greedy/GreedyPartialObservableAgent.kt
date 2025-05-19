package games.planetwars.agents.greedy

import games.planetwars.agents.Action
import games.planetwars.agents.PartialObservationPlayer
import games.planetwars.core.Observation

class GreedyPartialObservableAgent : PartialObservationPlayer() {

    override fun getAction(observation: Observation): Action {
        // Find my planets that have no transporter
        val myPlanets = observation.observedPlanets.filter { it.owner == player && it.transporter == null }
        if (myPlanets.isEmpty()) return Action.doNothing()

        // Find enemy or neutral planets
        val targets = observation.observedPlanets.filter { it.owner != player }
        if (targets.isEmpty()) return Action.doNothing()

        // Select strongest source planet
        val source = myPlanets.maxByOrNull { it.nShips ?: 0.0 }!!
        val target = targets.minByOrNull { it.nShips ?: Double.MAX_VALUE }!!

        val sourceShips = source.nShips ?: return Action.doNothing()
        val numToSend = sourceShips / 2

        return Action(player, source.id, target.id, numToSend)
    }

    override fun getAgentType(): String {
        return "GreedyPartialObservableAgent"
    }
}