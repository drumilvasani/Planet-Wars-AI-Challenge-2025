package games.planetwars.agents.greedy

import games.planetwars.agents.Action
import games.planetwars.agents.PlanetWarsPlayer
import games.planetwars.core.GameState
import games.planetwars.core.Planet
import kotlin.math.hypot
import kotlin.random.Random

class GreedyFullObservableAgent : PlanetWarsPlayer() {

    private val distanceWeight = 0.5
    private val growthWeight = 2.0
    private val shipGainWeight = 1.0
    private val safetyBuffer = 1.2
    private val random = Random.Default

    private fun distance(p1: Planet, p2: Planet): Double {
        val dx = p1.position.x - p2.position.x
        val dy = p1.position.y - p2.position.y
        return hypot(dx, dy)
    }

    private fun scoreAction(source: Planet, target: Planet): Double {
        val dist = distance(source, target)
        val growthPotential = target.growthRate
        val shipCost = target.nShips
        return -shipCost * shipGainWeight + growthPotential * growthWeight - dist * distanceWeight
    }

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter { it.owner == player && it.transporter == null }
        val targets = gameState.planets.filter { it.owner != player }

        if (myPlanets.isEmpty() || targets.isEmpty()) {
            return Action.doNothing()
        }

        // Evaluate all source-target pairs
        val candidateActions = mutableListOf<Triple<Planet, Planet, Double>>()

        for (source in myPlanets) {
            for (target in targets) {
                if (source.nShips > target.nShips * safetyBuffer) {
                    val score = scoreAction(source, target) + random.nextDouble(0.0, 0.1) // tiebreaker
                    candidateActions.add(Triple(source, target, score))
                }
            }
        }

        // Pick best move
        val best = candidateActions.maxByOrNull { it.third }
        if (best != null) {
            val (source, target, _) = best
            val numToSend = source.nShips / 2
            return Action(player, source.id, target.id, numToSend)
        }

        // Optional: reinforce weakest owned planet
        val weakPlanets = myPlanets.filter { it.nShips < 10 }
        if (weakPlanets.size >= 2) {
            val weakest = weakPlanets.minByOrNull { it.nShips } ?: return Action.doNothing()
            val strongest = myPlanets.maxByOrNull { it.nShips } ?: return Action.doNothing()
            val numToSend = strongest.nShips / 4
            return Action(player, strongest.id, weakest.id, numToSend)
        }

        return Action.doNothing()
    }

    override fun getAgentType(): String = "GreedyFullObservableAgent"
}
