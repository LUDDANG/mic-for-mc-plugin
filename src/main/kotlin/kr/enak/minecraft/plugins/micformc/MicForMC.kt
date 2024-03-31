package kr.enak.minecraft.plugins.micformc

import de.maxhenkel.voicechat.api.BukkitVoicechatService
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class MicForMC : JavaPlugin() {
    companion object {
        var standingMic: Entity? = null

        var isEnabled: Boolean = true

        fun isNearStandingMic(entity: LivingEntity): Boolean = standingMic != null
            && standingMic!!.location.distance(entity.location) <= 1.5
    }

    override fun onEnable() {
        super.onEnable()

        try {
            tryLoadVoiceChat()
        } catch (e: Throwable) {
            logger.log(Level.WARNING, "VoiceChat 로드 중 오류가 발생하여 플러그인을 비활성화합니다.", e)
            isEnabled = false
        }

        val cmd = CommandExecutor { sender, command, label, args ->
            if (args.isEmpty()) return@CommandExecutor run {
                sender.sendMessage("/$label (on/off/standing)")
                return@run true
            }

            when (args[0]) {
                "on" -> {
                    MicForMC.isEnabled = true
                    sender.sendMessage("마이크 방송을 활성화했습니다.")
                }

                "off" -> {
                    MicForMC.isEnabled = false
                    sender.sendMessage("마이크 방송을 비활성화했습니다.")
                }

                "standing" -> {
                    val player = sender as? org.bukkit.entity.Player ?: return@CommandExecutor run {
                        sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.")
                        return@run true
                    }

                    val standing = server.selectEntities(player, args[1]).firstOrNull()?: run {
                        sender.sendMessage("해당 엔티티를 찾을 수 없습니다.")
                        return@CommandExecutor true
                    }

                    standingMic = standing
                    sender.sendMessage("%s (을)를 스탠딩 마이크로 설정했습니다.".format(standing))
                }
            }

            return@CommandExecutor true
        }

        listOf("mfm", "micformc").mapNotNull {
            Bukkit.getPluginCommand(it)
        }.forEachTry { command ->
            command.setExecutor(cmd)
        }
    }

    private fun tryLoadVoiceChat() {
        val plugin = Bukkit.getPluginManager().getPlugin("voicechat")

        val service = server.servicesManager.load(BukkitVoicechatService::class.java)
            ?: throw RuntimeException("VoiceChat 을 찾을 수 없습니다.")
        service.registerPlugin(BroadcastMicPlugin())
    }
}