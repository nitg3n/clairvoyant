package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.MinedBlockAction
import com.nitg3n.clairvoyant.models.PlayerMoveAction
import com.nitg3n.clairvoyant.storage.PlayerActions
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.batchInsert
import java.util.concurrent.ConcurrentLinkedQueue

object LogProcessor {
    private val actionQueue = ConcurrentLinkedQueue<ActionData>()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun enqueue(action: ActionData) {
        actionQueue.add(action)
    }

    fun start(plugin: Clairvoyant) {
        job = scope.launch {
            while (isActive) {
                delay(5000)
                processQueue()
            }
        }
    }

    fun stop() {
        job?.cancel()
        runBlocking { processQueue() }
    }

    private suspend fun processQueue() {
        if (actionQueue.isEmpty()) return
        val actionsToProcess = generateSequence { actionQueue.poll() }.toList()
        if (actionsToProcess.isEmpty()) return

        try {
            DatabaseManager.dbQuery {
                PlayerActions.batchInsert(actionsToProcess) { action ->
                    this[PlayerActions.playerUuid] = action.playerUuid.toString()
                    this[PlayerActions.timestamp] = action.timestamp
                    when (action) {
                        is MinedBlockAction -> {
                            this[PlayerActions.actionType] = "MINE"
                            this[PlayerActions.world] = action.world
                            this[PlayerActions.x] = action.x
                            this[PlayerActions.y] = action.y
                            this[PlayerActions.z] = action.z
                            this[PlayerActions.blockMaterial] = action.blockMaterial
                        }
                        is PlayerMoveAction -> {
                            this[PlayerActions.actionType] = "MOVE"
                            this[PlayerActions.world] = action.world
                            this[PlayerActions.x] = action.x
                            this[PlayerActions.y] = action.y
                            this[PlayerActions.z] = action.z
                            this[PlayerActions.blockMaterial] = null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Clairvoyant.instance.logger.severe("Failed to process action queue: ${e.message}")
        }
    }
}