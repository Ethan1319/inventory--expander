package io.dithub.Ethan1319.plugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.util.*

class InventoryExpandPlugin : JavaPlugin(), Listener, org.bukkit.command.TabExecutor {

    private val playerInventories: MutableMap<UUID, Inventory> = mutableMapOf()
    private lateinit var dataFile: File
    private lateinit var dataConfig: YamlConfiguration

    override fun onEnable() {
        // 파일 준비
        dataFile = File(dataFolder, "data.yml")
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)

        // 저장된 인벤토리 불러오기
        loadInventories()

        // 리스너, 명령어 등록
        server.pluginManager.registerEvents(this, this)
        getCommand("expandinv")?.setExecutor(this)
    }

    override fun onDisable() {
        // 서버 종료 시 저장
        saveInventories()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.")
            return true
        }

        val player: Player = sender
        val uuid = player.uniqueId

        val inventory = playerInventories.getOrPut(uuid) {
            Bukkit.createInventory(null, 54, "${player.name}의 추가 인벤토리")
        }

        player.openInventory(inventory)
        player.sendMessage("추가 인벤토리를 열었습니다!")

        return true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player
        if (player is Player) {
            val uuid = player.uniqueId
            val inventory = event.inventory

            if (playerInventories[uuid] == inventory) {
                playerInventories[uuid] = inventory
                saveInventory(uuid, inventory)
                player.sendMessage("추가 인벤토리가 저장됨.")
            }
        }
    }

    // 개별 플레이어 인벤토리 저장
    private fun saveInventory(uuid: UUID, inventory: Inventory) {
        val itemsList = inventory.contents.map { it ?: ItemStack(Material.AIR) }
        dataConfig.set(uuid.toString(), itemsList)
        try {
            dataConfig.save(dataFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 전체 저장
    private fun saveInventories() {
        for ((uuid, inventory) in playerInventories) {
            saveInventory(uuid, inventory)
        }
    }

    // 저장된 인벤토리 불러오기
    private fun loadInventories() {
        for (key in dataConfig.getKeys(false)) {
            val uuid = UUID.fromString(key)
            val itemList = dataConfig.getList(key) ?: continue
            val inventory = Bukkit.createInventory(null, 54, "저장된 추가 인벤토리")
            for ((index, item) in itemList.withIndex()) {
                if (item is ItemStack) {
                    inventory.setItem(index, item)
                }
            }
            playerInventories[uuid] = inventory
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        return emptyList()
    }
}