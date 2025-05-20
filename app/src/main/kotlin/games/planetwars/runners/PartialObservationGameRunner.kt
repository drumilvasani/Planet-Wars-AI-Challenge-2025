package games.planetwars.runners

import games.planetwars.agents.PartialObservationAgent
import games.planetwars.agents.greedy.GreedyPartialObservableAgent
import games.planetwars.agents.random.PartialObservationBetterRandomAgent
import games.planetwars.core.*

data class PartialObservationGameRunner(
    val agent1: PartialObservationAgent,
    val agent2: PartialObservationAgent,
    val gameParams: GameParams,
) {
    var gameState: GameState = GameStateFactory(gameParams).createGame()
    var forwardModel: ForwardModel = ForwardModel(gameState.deepCopy(), gameParams)

    init {
        newGame()
    }

    fun newGame() {
        if (gameParams.newMapEachRun) {
            gameState = GameStateFactory(gameParams).createGame()
        }
        forwardModel = ForwardModel(gameState.deepCopy(), gameParams)
        agent1.prepareToPlayAs(Player.Player1, gameParams)
        agent2.prepareToPlayAs(Player.Player2, gameParams)
    }

    fun runGame(): ForwardModel {
        newGame()
        while (!forwardModel.isTerminal()) {
            val state = forwardModel.state
            val p1Observation = ObservationFactory.create(state, setOf(Player.Player1))
            val p2Observation = ObservationFactory.create(state, setOf(Player.Player2))

            val actions = mapOf(
                Player.Player1 to agent1.getAction(p1Observation),
                Player.Player2 to agent2.getAction(p2Observation),
            )
            forwardModel.step(actions)
        }
        return forwardModel
    }

    fun stepGame(): ForwardModel {
        if (forwardModel.isTerminal()) return forwardModel

        val state = forwardModel.state
        val p1Observation = ObservationFactory.create(state, setOf(Player.Player1))
        val p2Observation = ObservationFactory.create(state, setOf(Player.Player2))

        val actions = mapOf(
            Player.Player1 to agent1.getAction(p1Observation),
            Player.Player2 to agent2.getAction(p2Observation),
        )
        forwardModel.step(actions)
        return forwardModel
    }

    fun runGames(nGames: Int): Map<Player, Int> {
        val scores = mutableMapOf(Player.Player1 to 0, Player.Player2 to 0, Player.Neutral to 0)
        for (i in 0 until nGames) {
            val finalModel = runGame()
            val winner = finalModel.getLeader()
            scores[winner] = scores[winner]!! + 1
            //println(forwardModel.statusString())
        }
        //println(forwardModel.statusString())

        return scores
    }
}

fun main() {
    val gameParams = GameParams(numPlanets = 20)

    val agent1 = GreedyPartialObservableAgent()
    val agent2 = PartialObservationBetterRandomAgent()

    val runner = PartialObservationGameRunner(agent1, agent2, gameParams)

    val finalModel = runner.runGame()
    println("Game over!")
    println(finalModel.statusString())

    val nGames = 1000
    val tStart = System.currentTimeMillis()
    val results = runner.runGames(nGames)
    val dt = System.currentTimeMillis() - tStart

    println(results)
    println("Time per game: ${dt.toDouble() / nGames} ms")

    val nSteps = ForwardModel.nUpdates
    println("Time per step: ${dt.toDouble() / nSteps} ms")
    println("Successful actions: ${ForwardModel.nActions}")
    println("Failed actions: ${ForwardModel.nFailedActions}")
}