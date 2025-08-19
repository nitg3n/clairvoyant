package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.storage.PlayerActions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

/**
 * Manages database connections and queries.
 */
class DatabaseManager(private val plugin: Clairvoyant) {

    private lateinit var db: Database

    init {
        connect()
        initializeSchema()
    }

    /**
     * Establishes a connection to the SQLite database.
     */
    private fun connect() {
        val dbFile = File(plugin.dataFolder, "clairvoyant.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
        }
        db = Database.connect("jdbc:sqlite:${dbFile.path}", "org.sqlite.JDBC")
    }

    /**
     * Creates the database tables if they do not already exist.
     */
    private fun initializeSchema() {
        transaction(db) {
            SchemaUtils.create(PlayerActions)
        }
    }

    /**
     * Logs a player action to the database.
     * @param actionData The action data to log.
     */
    fun logAction(actionData: ActionData) {
        transaction(db) {
            PlayerActions.insert {
                it[playerUUID] = actionData.playerUUID.toString()
                it[playerName] = actionData.playerName
                it[actionType] = actionData.actionType.name
                it[material] = actionData.material
                it[world] = actionData.world
                it[x] = actionData.x
                it[y] = actionData.y
                it[z] = actionData.z
                it[timestamp] = actionData.timestamp
            }
        }
    }

    /**
     * Retrieves the total number of actions for a specific player.
     * @param playerUUID The UUID of the player.
     * @return The total action count.
     */
    fun getActionCount(playerUUID: UUID): Long {
        return transaction(db) {
            PlayerActions
                .select(PlayerActions.id.count())
                .where { PlayerActions.playerUUID eq playerUUID.toString() }
                .first()[PlayerActions.id.count()]
        }
    }


    /**
     * Retrieves all action logs for a specific player.
     * @param playerUUID The UUID of the player.
     * @return A list of ActionData objects.
     */
    fun getPlayerActions(playerUUID: UUID): List<ActionData> {
        return transaction(db) {
            PlayerActions.selectAll().where { PlayerActions.playerUUID eq playerUUID.toString() }
                .orderBy(PlayerActions.timestamp)
                .map { row ->
                    ActionData(
                        playerUUID = UUID.fromString(row[PlayerActions.playerUUID]),
                        playerName = row[PlayerActions.playerName],
                        actionType = ActionType.valueOf(row[PlayerActions.actionType]),
                        material = row[PlayerActions.material],
                        world = row[PlayerActions.world],
                        x = row[PlayerActions.x],
                        y = row[PlayerActions.y],
                        z = row[PlayerActions.z],
                        timestamp = row[PlayerActions.timestamp]
                    )
                }
        }
    }

    /**
     * Retrieves mining statistics for a specific player.
     * @param playerUUID The UUID of the player.
     * @return A map of material names to the count of blocks broken.
     */
    fun getPlayerMiningStats(playerUUID: UUID): Map<String, Int> {
        return transaction(db) {
            val materialCol = PlayerActions.material
            val countCol = materialCol.count()

            PlayerActions
                .select(materialCol, countCol)
                .where {
                    (PlayerActions.playerUUID eq playerUUID.toString()) and
                            (PlayerActions.actionType eq ActionType.BLOCK_BREAK.name)
                }
                .groupBy(materialCol)
                .associate {
                    it[materialCol] to it[countCol].toInt()
                }
        }
    }
}