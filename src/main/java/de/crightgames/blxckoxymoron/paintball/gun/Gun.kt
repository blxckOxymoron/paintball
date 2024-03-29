package de.crightgames.blxckoxymoron.paintball.gun

import de.crightgames.blxckoxymoron.paintball.projectile.ProjectileType
import de.crightgames.blxckoxymoron.paintball.util.ThemeBuilder
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

data class Gun(
    val projectile: ProjectileType,
    val magazine: Magazine,
    val rateOfFire: Long = 1000, // millis
    val spray: Double = 0.1,
    val bullets: Int = 1, // number of bullets shot each shot 1 for simple weapons, 5 for shotguns
    val sound: Sound,
    val pitch: Float
) {
    fun createItem(material: Material = Material.NETHERITE_HOE): ItemStack {
        val item = ItemStack(material, 1)
        val meta = item.itemMeta!! // !! 'cause it's a crossbow
        meta.persistentDataContainer.set(GunDataContainer.KEY, GunDataContainer, this)
        meta.addEnchant(Enchantment.LUCK, 1, true)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.setDisplayName("${BOLD}${YELLOW}⭐ BLASTER")
        meta.lore = createItemLore()
        item.itemMeta = meta
        return item
    }

    fun getInfoText(p: Player): String {
        if (ReloadGun.currentlyReloading.containsKey(p.uniqueId)) return ThemeBuilder.themed("Ammo: :GOLD:⌛/${magazine.size}::")
        return if (magazine.content > 0) ThemeBuilder.themed(
            "Ammo: *${magazine.content}/${magazine.size}*"
        ) else ThemeBuilder.themed(
            "Ammo: :RED:0/${magazine.size}::"
        )
    }

    private fun createItemLore(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("$GREEN>>$BOLD STATS")
        // yikes! mixing of template and String.format()
        lines.add("$WHITE• rate of fire: ${"%.2f".format(1000.0 / rateOfFire)}/s")
        lines.add("$WHITE• bullets: $bullets")
        lines.add("$WHITE• spray: ${"%.2f".format(spray)}")
        lines.add("$GREEN>>$BOLD MAGAZINE")
        lines.add("$WHITE• size: ${magazine.size}")
        lines.add("$WHITE• reload: ${"%.2f".format(magazine.reloadSpeed / 1000.0)}s")
        lines.add("$GREEN>>$BOLD PROJECTILE")
        lines.add("$WHITE• particle: ${projectile.particle.name}")
        lines.add("$WHITE• speed: ${"%.5f".format(projectile.speed)}")
        lines.add("$WHITE• gravity: ${"%.5f".format(projectile.gravity)}")
        // lines.add("$WHITE• effects: ${projectile.effects.joinToString(" ") { "${it.first.name}[${it.second}]" }}")
        // lines.add("$WHITE• entity: ${projectile.entity?.name ?: "null"}")

        return lines
    }
}