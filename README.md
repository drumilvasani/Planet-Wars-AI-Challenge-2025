# Planet Wars RTS

<img width="406" alt="image" src="https://github.com/user-attachments/assets/d70c0d2a-bd57-4795-9ec4-fb35a401f8f3" alt="QR Code" width="150" align="right"/>


## note: currently in early access: 

The code is in early access, but the interfaces
are intended to be stable both for the fully observable and partially
observable versions of the game.

To run headless games use the examples in the `games.planetwars.runners` package.

To run games with a GUI, use the `games.planetwars.view.RunVisualGame` class.

The following features are ready for community testing:

* containerised version (PodMan or Docker)
-- see [Submission Instructions](submit_entry.md)
* [Python sample](app/src/main/python/client_server/game_agent_server.py) 



## Introduction

This repo contains the code and instructions for a series
of Planet Wars Real-Time Strategy (RTS) games.  So far it has been accepted to
run in conjunction with the following conferences:

- [GECCO 2025](https://gecco-2025.sigevo.org/Competition?itemId=5108)  
- [IEEE Conference on Games 2025](https://cog2025.inesc-id.pt/competitions/)
  

More details on the variants used for each conference will be published later.

Figures below show a fully observable and a partially observable game in play.

<img width="638" alt="image" src="https://github.com/user-attachments/assets/dc702b7c-745d-44e9-a7b9-d172ecd65478" />

<img width="640" alt="image" src="https://github.com/user-attachments/assets/e1de70d3-444d-49bf-b0ee-dc5982eebbfc" />



## The core idea

Planet Wars is an RTS game where players 
aim to gain control of planets and destroy enemy units.

We aim to provide a framework for developing and testing AI 
agents in a fast and flexible way.  The challenge is open-ended
as the game parameters can be varied to create a range of challenges.
Even the simpler versions still have the difficulty of
dealing with the simultaneous move nature of the game
and unpredictable opponent actions.

The software supports a family of games where key
details can be varied to affect the difficulty
of the game.  This includes:

* Observability of the game state: 
  - full
  - partial: full observability of the player's own assets, only ownership of neutral and opponent planets
* The number of planets
* The battle rules
* In-transit collisions
* Transit speed
* The time allowed per decision
* The winning conditions:
  - the number of planets controlled
  - the difference in the number of units
  - whether units in transit count towards the total
* The game duration

## Agent API

There are two types depending on the observability.
For the fully observable version, agents are given
a copy of the complete game state at each time step,
and follow this interface:


```kotlin
interface PlanetWarsAgent {
  fun getAction(gameState: GameState): Action
  fun getAgentType(): String
  fun prepareToPlayAs(player: Player, params: GameParams, opponent: Player? = null): PlanetWarsAgent

  // this is provided as a default implementation, but can be overridden if needed
  fun processGameOver(finalState: GameState) {}
}


```

For the partially observable game, agents use this interface:

```kotlin
interface PartialObservationAgent {
  fun getAction(observation: Observation): Action
  fun getAgentType(): String
  fun prepareToPlayAs(player: Player, params: GameParams, opponent: Player? = null): PartialObservationAgent
  // this is provided as a default implementation, but can be overridden if needed
  // note that the final state is fully observable, so the agent can use this to learn from the final state
  fun processGameOver(finalState: GameState) {}
}

```

There are simple helper classes provided to reconstruct a
game state from an observation, and an example of using this 
is given in this agent:

```kotlin

class PartialObservationPureRandomAgent() : PartialObservationPlayer() {

  private val sampler = DefaultHiddenInfoSampler(params)
  private val reconstructor = GameStateReconstructor(sampler)

  override fun getAction(observation: Observation): Action {
    // illustrate use of reconstructed game state from observation
    val gameState = reconstructor.reconstruct(observation)
    val source = gameState.planets.random()
    val target = gameState.planets.random()
    return Action(player, source.id, target.id, source.nShips / 2)
  }

  override fun getAgentType(): String {
    return "Pure Random Agent"
  }
}

```


However, part of the skill of an agent is in the assumptions it
makes about the hidden information, so we expect high performance
agents to improve on the provided reconstruction example.


A `GameState` object is required for planning algorithms in order to use the forward model provided.





## Game Runners

There are two ways to run games: synchronous and asynchronous.

### Synchronous

In synchronous mode, the game runner runs in
a single thread.  At each game tick, it calls the getAction method
of each agent, waits for the response, applies the actions of 
each agent to generate the next state using the
forward model, and the repeats until the game is over.

This mode is useful for debugging and testing:
it is often faster than asynchronous mode as it
avoids any overhead due to coroutines and timeouts
(though in theory asynchronous mode could be
faster on a multi-core machine as agents could run
in their own threads - the actual speed depends on
a number of factors).

###

In asynchronous mode, the game runner runs in multiple threads
or coroutines.  The key point is that every game tick it sends
the current state observation to each agent, and then waits for
for a specified number of milliseconds for a response.  
If the agent does not respond in time, the game runner assumes
a doNothing action, and proceeds to step the game forward.

Hence this mode is truly real-time.


## Agent Deployment

For debugging and development run your code locally by
extending the examples in the `games.planetwars.runners` package,
if developing in Kotlin, Java or any JVM language.

For other languages, follow the examples in the Python
directory (**coming soon**).

For competitions, you can deploy your agent to a PodMan
container, and provide the link via the competition
interface. (**coming soon**)

### Loading a learned model

(**coming soon**)

## The codebase and philosophy

The code aims to be well-structured, easy to read and efficient.
The agent interface is designed to be the same for all versions of the game, as is the game state representation and game state observations.
The key differences between versions of the game are captured in the
game parameters, observation details and forward model.

## The evaluation

For your own evaluations see the `games.planetwars.runners.RoundRobinLeague` example.
Running this with the sample agents will produce a league table similar to the following one:

| Rank | Agent Name | Win Rate | Played |
|------|------------|------|-------|
| 1 | EvoAgent-200-50-0.3-true | 79.5 | 200 |
| 2 | Better Random Agent | 69.0 | 200 |
| 3 | Careful Random Agent | 1.5 | 200 |

For compeitions we aim to run sufficient games to arrive at a stable rank order of the agents.


