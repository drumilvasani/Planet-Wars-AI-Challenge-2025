package games.planetwars.agents.greedy

import games.planetwars.agents.Action
import games.planetwars.agents.PlanetWarsPlayer
import games.planetwars.core.GameState
import games.planetwars.core.Player
import games.planetwars.core.Planet
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

class GreedyLookaheadAgent : PlanetWarsPlayer() {

    override fun getAgentType(): String = "Greedy Lookahead Agent"

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter { it.owner == player && it.transporter == null && it.nShips > 10 }
        val targets = gameState.planets.filter { it.owner != player }

        if (myPlanets.isEmpty() || targets.isEmpty()) {
            return Action.doNothing()
        }

        val candidateActions = mutableListOf<Triple<Planet, Planet, Double>>()

        for (source in myPlanets) {
            for (target in targets) {
                if (source.nShips > target.nShips * 1.2) { // Safety buffer
                    val score = evaluateTarget(source, target, gameState) + Random.nextDouble(0.0, 0.1) // Tiebreaker
                    candidateActions.add(Triple(source, target, score))
                }
            }
        }

        val best = candidateActions.maxByOrNull { it.third }
        if (best != null) {
            val (source, target, _) = best
            val numToSend = source.nShips / 2
            return Action(player, source.id, target.id, numToSend)
        }

        // Reinforce weakest owned planet
        val weakPlanets = myPlanets.filter { it.nShips < 10 }
        if (weakPlanets.isNotEmpty()) {
            val weakest = weakPlanets.minByOrNull { it.nShips } ?: return Action.doNothing()
            val strongest = myPlanets.maxByOrNull { it.nShips } ?: return Action.doNothing()
            val numToSend = strongest.nShips / 4
            return Action(player, strongest.id, weakest.id, numToSend)
        }

        return Action.doNothing()
    }

    private fun evaluateTarget(source: Planet, target: Planet, gameState: GameState): Double {
        val distance = source.position.distance(target.position)
        val growthPotential = target.growthRate
        val shipCost = target.nShips

        // Simplified scoring
        var score = growthPotential * 2.0 - shipCost * 1.0 - distance * 0.5

        // Early expansion prioritization
        if (gameState.gameTick < 20 && target.owner == Player.Neutral) {
            score += 15.0 // Increase priority for neutral planets early in the game
        }

        // Predict opponent moves and calculate resource management bonus
        val opponentMovePrediction = predictOpponentMoves(target, gameState)
        val resourceManagementBonus = calculateResourceManagementBonus(source, target, gameState)

        return score + opponentMovePrediction + resourceManagementBonus
    }

    private fun predictOpponentMoves(target: Planet, gameState: GameState): Double {
        val opponentPlanets = gameState.planets.filter { it.owner == player.opponent() }
        var predictedThreatLevel = 0.0

        // Predict opponent focus on expanding to neutral planets or attacking weak planets
        if (target.owner == Player.Neutral) {
            predictedThreatLevel += 10.0 // Higher prediction weight for neutral planets
        }

        for (opponentPlanet in opponentPlanets) {
            val distanceToOpponent = target.position.distance(opponentPlanet.position)
            if (distanceToOpponent < 10.0) { // Threshold distance
                predictedThreatLevel += 15.0 // Increase threat level for proximity
            }
        }

        return predictedThreatLevel
    }

    private fun calculateResourceManagementBonus(source: Planet, target: Planet, gameState: GameState): Double {
        val myPlanets = gameState.planets.filter { it.owner == player }
        var resourceManagementBonus = 0.0

        // Reinforcement strategy: prioritize sending reinforcements to planets with high growth rates
        for (myPlanet in myPlanets) {
            if (myPlanet.growthRate > 5.0 && myPlanet.nShips < 20) { // Example thresholds
                resourceManagementBonus += 5.0
            }
        }

        // Strategic value: prioritize planets that increase control over the map
        if (target.growthRate > 3.0) {
            resourceManagementBonus += 5.0
        }

        return resourceManagementBonus
    }
}