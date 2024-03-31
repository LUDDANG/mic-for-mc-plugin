package kr.enak.minecraft.plugins.micformc

import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

private fun ItemStack.isMic(): Boolean = this.itemMeta?.displayName.equals("§b§l마이크")

class BroadcastMicPlugin : VoicechatPlugin {
    override fun getPluginId(): String = "micformc"

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(MicrophonePacketEvent::class.java, this::onMicrophone)
    }

    private fun onMicrophone(e: MicrophonePacketEvent) {
        if (!MicForMC.isEnabled) return

        val conn = e.senderConnection?: return
        val micPlayer = conn.player?.player as? Player?: return

        if (
            !conn.group?.name.equals("broadcast", true)
            && !micPlayer.inventory.itemInMainHand.isMic()
            && !micPlayer.inventory.itemInOffHand.isMic()
            && !MicForMC.isNearStandingMic(micPlayer)
        ) return

        val api = e.voicechat
        Bukkit.getOnlinePlayers().forEachTry { player ->
            if (player.uniqueId == micPlayer.uniqueId) return@forEachTry

            val voiceConn = api.getConnectionOf(player.uniqueId)?: return@forEachTry
            api.sendStaticSoundPacketTo(voiceConn, e.packet.toStaticSoundPacket())
        }
    }
}