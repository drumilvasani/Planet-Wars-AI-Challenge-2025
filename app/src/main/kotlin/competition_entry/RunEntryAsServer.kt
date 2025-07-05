package competition_entry

import games.planetwars.agents.greedy.GreedyFullObservableAgent
import games.planetwars.agents.greedy.GreedyLookaheadAgent
import games.planetwars.agents.random.CarefulRandomAgent
import json_rmi.GameAgentServer

fun main() {
    val server = GameAgentServer(port = 8080, agentClass = GreedyLookaheadAgent::class)
    server.start(wait = true)
}
