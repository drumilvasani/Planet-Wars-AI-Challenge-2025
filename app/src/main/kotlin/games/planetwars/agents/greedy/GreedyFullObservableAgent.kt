package games.planetwars.agents.random

import games.planetwars.agents.Action
import games.planetwars.agents.PlanetWarsPlayer
import games.planetwars.core.GameState
import games.planetwars.core.Planet
import games.planetwars.core.Player
import kotlin.math.hypot

class GreedyFullObservableAgent : PlanetWarsPlayer() {

    private val ATTACK_SAFETY_BUFFER = 1.2
    private val FALLBACK_ATTACK_BUFFER = 1.5
    private val WEAK_PLANET_THRESHOLD = 10

    private val shipWeight = 1.0
    private val distanceWeight = 0.5
    private val growthWeight = 2.0

    private fun distance(p1: Planet, p2: Planet): Double {
        val dx = p1.position.x - p2.position.x
        val dy = p1.position.y - p2.position.y
        return hypot(dx, dy)
    }

    private fun scorePlanet(target: Planet, source: Planet): Double {
        val dist = distance(source, target)
        return (target.nShips * shipWeight) + (dist * distanceWeight) - (target.growthRate * growthWeight)
    }

    private fun enemyTransportersLaunchedFrom(planet: Planet, gameState: GameState): Boolean {
        return gameState.planets.any { other ->
            other.transporter?.let { t ->
                t.sourceIndex == planet.id && t.owner != player
            } ?: false
        }
    }

    override fun getAction(gameState: GameState): Action {
        val myPlanets = gameState.planets.filter { it.owner == player && it.transporter == null }
        if (myPlanets.isEmpty()) return Action.doNothing()

        val enemyOrNeutralPlanets = gameState.planets.filter { it.owner != player }
        if (enemyOrNeutralPlanets.isEmpty()) return Action.doNothing()

        val strongestSource = myPlanets.maxByOrNull { it.nShips } ?: return Action.doNothing()

        // Phase 1: Smart target selection based on scoring
        val scoredTargets = enemyOrNeutralPlanets
            .map { it to scorePlanet(it, strongestSource) }
            .sortedBy { it.second }

        val bestTarget = scoredTargets.firstOrNull()?.first
        if (bestTarget != null && strongestSource.nShips > bestTarget.nShips * ATTACK_SAFETY_BUFFER) {
            val numToSend = strongestSource.nShips / 2
            return Action(player, strongestSource.id, bestTarget.id, numToSend)
        }

        // Phase 2: Opportunistic Attacks — enemy planets just launched a transporter
        val vulnerableEnemies = enemyOrNeutralPlanets
            .filter { enemyTransportersLaunchedFrom(it, gameState) }

        val opportunisticTarget = vulnerableEnemies.minByOrNull { it.nShips }
        if (opportunisticTarget != null) {
            val numToSend = strongestSource.nShips / 2
            return Action(player, strongestSource.id, opportunisticTarget.id, numToSend)
        }

        // Phase 3: Reinforce weak owned planets
        val weakOwnedPlanets = myPlanets.filter { it.nShips < WEAK_PLANET_THRESHOLD }
        if (weakOwnedPlanets.isNotEmpty()) {
            val weakest = weakOwnedPlanets.minByOrNull { it.nShips } ?: return Action.doNothing()
            val strong = myPlanets.maxByOrNull { it.nShips } ?: return Action.doNothing()
            val numToSend = strong.nShips / 4
            return Action(player, strong.id, weakest.id, numToSend)
        }

        // Phase 4: Fallback attack with variable strength
        val fallbackTarget = enemyOrNeutralPlanets.minByOrNull { it.nShips } ?: return Action.doNothing()
        val fallbackSource = myPlanets.maxByOrNull { it.nShips } ?: return Action.doNothing()

        val fallbackNumToSend = if (fallbackSource.nShips > fallbackTarget.nShips * FALLBACK_ATTACK_BUFFER) {
            fallbackSource.nShips / 2
        } else {
            fallbackSource.nShips / 3
        }

        return Action(player, fallbackSource.id, fallbackTarget.id, fallbackNumToSend)
    }

    override fun getAgentType(): String = "GreedyFullObservableAgent"
}