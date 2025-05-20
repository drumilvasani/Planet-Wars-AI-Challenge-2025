package games.planetwars.runners

import games.planetwars.agents.PartialObservationAgent
import games.planetwars.agents.greedy.GreedyPartialObservableAgent
import games.planetwars.agents.random.PartialObservationBetterRandomAgent
import games.planetwars.agents.random.PartialObservationPureRandomAgent
import games.planetwars.core.GameParams
import games.planetwars.core.Player

fun main() {
    val agents = SamplePartialPlayerList().getPartialList()
    val league = PartialRoundRobinLeague(agents, gamesPerPair = 50)
    val results = league.runRoundRobin()

    //println(results)
    val writer = LeagueWriter()
    val leagueResult = LeagueResult(results.values.toList())
    val markdownContent = writer.generateMarkdownTable(leagueResult)
    val savePath = writer.saveMarkdownToFile(markdownContent)

    val sortedResults = results.toList().sortedByDescending { it.second.points }.toMap()
    for (entry in sortedResults.values) {
        println("${entry.agentName} : ${entry.points} : ${entry.nGames}")
    }
}

class SamplePartialPlayerList {
    fun getPartialList(): MutableList<PartialObservationAgent> {
        return mutableListOf(
            GreedyPartialObservableAgent(),
            PartialObservationBetterRandomAgent(),
            PartialObservationPureRandomAgent()
        )
    }
}

data class PartialRoundRobinLeague(
    val agents: List<PartialObservationAgent>,
    val gamesPerPair: Int = 10,
    val gameParams: GameParams = GameParams(numPlanets = 20),
) {
    fun runPair(agent1: PartialObservationAgent, agent2: PartialObservationAgent): Map<Player, Int> {
        val gameRunner = PartialObservationGameRunner(agent1, agent2, gameParams)
        return gameRunner.runGames(gamesPerPair)
    }

    fun runRoundRobin(): Map<String, LeagueEntry> {
        val t = System.currentTimeMillis()
        val scores = mutableMapOf<String, LeagueEntry>()
        for (agent in agents) {
            scores[agent.getAgentType()] = LeagueEntry(agent.getAgentType())
        }

        for (i in 0 until agents.size) {
            for (j in 0 until agents.size) {
                if (i == j) continue
                val agent1 = agents[i]
                val agent2 = agents[j]
                val result = runPair(agent1, agent2)

                val entry1 = scores[agent1.getAgentType()]!!
                val entry2 = scores[agent2.getAgentType()]!!
                entry1.points += result[Player.Player1]!!
                entry2.points += result[Player.Player2]!!
                entry1.nGames += gamesPerPair
                entry2.nGames += gamesPerPair
            }
        }

        println("Partial Round Robin took ${(System.currentTimeMillis() - t).toDouble()/1000} seconds")
        return scores
    }
}
