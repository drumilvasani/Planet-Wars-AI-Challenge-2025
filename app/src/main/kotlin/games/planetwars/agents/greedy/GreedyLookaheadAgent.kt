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

    private val random = Random.Default

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
                val score = evaluateTarget(source, target, gameState) + random.nextDouble(0.0, 0.1) // Tiebreaker
                if (score != Double.NEGATIVE_INFINITY) {
                    candidateMoves.add(Triple(source, target, score))
                }
            }
        }

        val bestMove = candidateMoves.maxByOrNull { it.third } ?: return Action.doNothing()
        val (source, target, _) = bestMove

        val optimalShipsToSend = calculateOptimalShipsToSend(source, target, gameState)
        if (optimalShipsToSend <= 0 || optimalShipsToSend >= source.nShips) return Action.doNothing()

        return Action(player, source.id, target.id, optimalShipsToSend.toDouble())
    }

    private fun calculateOptimalShipsToSend(source: Planet, target: Planet, gameState: GameState): Int {
        val distance = source.position.distance(target.position)
        val ticksToArrival = distance / params.transporterSpeed
        val growthDuringTravel = target.growthRate * ticksToArrival
        val estimatedDefense = target.nShips + growthDuringTravel

        // Calculate ships needed for attack
        var optimalShipsToSend = (estimatedDefense * 1.25).toInt()

        // Ensure source planet retains enough ships for defense
        val defenseBuffer = 10 // Example buffer
        optimalShipsToSend = min(optimalShipsToSend, source.nShips.toInt() - defenseBuffer)

        // Prioritize high-growth targets
        if (target.growthRate > 3.0) {
            optimalShipsToSend = max(optimalShipsToSend, source.nShips.toInt() / 2)
        }

        // Ensure calculated ships do not exceed available ships
        optimalShipsToSend = min(optimalShipsToSend, source.nShips.toInt())

        return max(optimalShipsToSend, 0) // Ensure non-negative ship count
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