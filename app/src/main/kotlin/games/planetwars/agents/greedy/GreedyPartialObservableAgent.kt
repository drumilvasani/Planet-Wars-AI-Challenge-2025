package games.planetwars.agents.greedy

import games.planetwars.agents.Action
import games.planetwars.agents.PartialObservationPlayer
import games.planetwars.core.Observation
import games.planetwars.core.Player
import games.planetwars.core.PlanetObservation
import kotlin.math.hypot

class GreedyPartialObservableAgent : PartialObservationPlayer() {

    private val WEAK_PLANET_THRESHOLD = 10.0
    private val distanceWeight = 0.5
    private val growthWeight = 2.0

    private fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return hypot(x1 - x2, y1 - y2)
    }

    private fun scorePlanet(target: PlanetObservation, source: PlanetObservation): Double {
        val dist = distance(source.position.x, source.position.y, target.position.x, target.position.y)
        val growth = target.growthRate ?: 0.0
        return (dist * distanceWeight) - (growth * growthWeight)
    }

    override fun getAction(observation: Observation): Action {
        val myPlanets = observation.observedPlanets.filter {
            it.owner == player && it.transporter == null && (it.nShips ?: 0.0) >= 10.0
        }
        if (myPlanets.isEmpty()) return Action.doNothing()

        val source = myPlanets.maxByOrNull { it.nShips ?: 0.0 } ?: return Action.doNothing()
        val sourceShips = source.nShips ?: return Action.doNothing()

        val neutralPlanets = observation.observedPlanets.filter { it.owner == Player.Neutral }
        val enemyPlanets = observation.observedPlanets.filter { it.owner == player.opponent() }
        val targetCandidates = (neutralPlanets + enemyPlanets)
        if (targetCandidates.isEmpty()) return Action.doNothing()

        val target = targetCandidates.minByOrNull { scorePlanet(it, source) } ?: return Action.doNothing()

        val numToSend = (sourceShips * 0.4).coerceAtLeast(1.0)

        return Action(player, source.id, target.id, numToSend)
    }

    override fun getAgentType(): String = "GreedyPartialObservableAgent"
}
