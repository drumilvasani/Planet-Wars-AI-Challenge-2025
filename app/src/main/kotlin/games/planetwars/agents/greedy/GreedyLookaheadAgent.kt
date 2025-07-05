package games.planetwars.agents.greedy

import games.planetwars.agents.Action
import games.planetwars.agents.PlanetWarsPlayer
import games.planetwars.core.GameState
import games.planetwars.core.Player
import games.planetwars.core.Planet
import kotlin.math.min

class GreedyLookaheadAgent : PlanetWarsPlayer() {

    override fun getAgentType(): String = "Greedy Lookahead Agent"

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter {
            it.owner == player && it.transporter == null && it.nShips > 10
        }

        if (myPlanets.isEmpty()) return Action.doNothing()

        val enemyOrNeutral = gameState.planets.filter { it.owner != player }

        val candidateMoves = mutableListOf<Triple<Planet, Planet, Double>>()

        for (source in myPlanets) {
            for (target in enemyOrNeutral) {
                val score = evaluateTarget(source, target, gameState)
                if (score != Double.NEGATIVE_INFINITY) {
                    candidateMoves.add(Triple(source, target, score))
                }
            }
        }

        val bestMove = candidateMoves.maxByOrNull { it.third } ?: return Action.doNothing()
        val (source, target, _) = bestMove

        val distance = source.position.distance(target.position)
        val ticksToArrival = distance / params.transporterSpeed
        val growthDuringTravel = target.growthRate * ticksToArrival
        val estimatedDefense = target.nShips + growthDuringTravel

        val shipsToSend = min(source.nShips / 1.2, estimatedDefense * 1.25).toInt()
        if (shipsToSend <= 0 || shipsToSend >= source.nShips) return Action.doNothing()

        return Action(player, source.id, target.id, shipsToSend.toDouble())
    }

    private fun evaluateTarget(source: Planet, target: Planet, gameState: GameState): Double {
        val distance = source.position.distance(target.position)
        val ticksToArrival = distance / params.transporterSpeed
        val growthDuringTravel = target.growthRate * ticksToArrival
        val futureShips = target.nShips + growthDuringTravel

        if (source.nShips <= futureShips * 1.1) return Double.NEGATIVE_INFINITY

        val value = target.growthRate * 10.0 - futureShips
        val distPenalty = distance * 0.5
        val ownerPenalty = when (target.owner) {
            Player.Neutral -> 0.0
            else -> 12.0
        }

        return value - distPenalty - ownerPenalty
    }
}
