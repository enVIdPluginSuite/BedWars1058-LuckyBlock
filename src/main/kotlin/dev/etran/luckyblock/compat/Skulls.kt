package dev.etran.luckyblock.compat

import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/** Puts a custom skin on a skull, on every version (PlayerProfile API on 1.18.1+, GameProfile before). */
object Skulls {

    /** Applies the base64 "textures" value to the skull. Returns false if it could not be applied. */
    @JvmStatic
    fun applyTexture(meta: SkullMeta, base64Value: String?): Boolean {
        if (base64Value == null || base64Value.trim().isEmpty()) return false
        val value = base64Value.trim()
        val id = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8))
        return try {
            if (hasProfileApi()) viaProfileApi(meta, value, id) else viaGameProfile(meta, value, id)
        } catch (e: ReflectiveOperationException) {
            false
        } catch (e: RuntimeException) {
            false
        }
    }

    private fun hasProfileApi(): Boolean = try {
        Class.forName("org.bukkit.inventory.meta.SkullMeta")
            .getMethod("setOwnerProfile", Class.forName("org.bukkit.profile.PlayerProfile"))
        true
    } catch (e: ReflectiveOperationException) {
        false
    } catch (e: LinkageError) {
        false
    }

    private fun viaProfileApi(meta: SkullMeta, value: String, id: UUID): Boolean {
        val skin = decodeSkinUrl(value) ?: return false
        val profileClass = Class.forName("org.bukkit.profile.PlayerProfile")
        val texturesClass = Class.forName("org.bukkit.profile.PlayerTextures")
        val profile = Bukkit::class.java.getMethod("createPlayerProfile", UUID::class.java, String::class.java)
            .invoke(null, id, "LuckyBlock")
        val textures = profileClass.getMethod("getTextures").invoke(profile)
        texturesClass.getMethod("setSkin", URL::class.java).invoke(textures, skin)
        profileClass.getMethod("setTextures", texturesClass).invoke(profile, textures)
        // look the method up on the public SkullMeta interface: the implementing class is not public, so
        // calling its Method object from here would throw IllegalAccessException
        SkullMeta::class.java.getMethod("setOwnerProfile", profileClass).invoke(meta, profile)
        return true
    }

    private fun viaGameProfile(meta: SkullMeta, value: String, id: UUID): Boolean {
        val gameProfile = Class.forName("com.mojang.authlib.GameProfile")
        val property = Class.forName("com.mojang.authlib.properties.Property")
        val profile = gameProfile.getConstructor(UUID::class.java, String::class.java).newInstance(id, "LuckyBlock")
        val properties = gameProfile.getMethod("getProperties").invoke(profile)
        val textures = property.getConstructor(String::class.java, String::class.java).newInstance("textures", value)
        val put = properties.javaClass.getMethod("put", Any::class.java, Any::class.java)
        put.invoke(properties, "textures", textures)
        val field = meta.javaClass.getDeclaredField("profile")
        field.isAccessible = true
        field.set(meta, profile)
        return true
    }

    @Suppress("DEPRECATION")
    private fun decodeSkinUrl(base64Value: String): URL? = try {
        val json = String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8)
        val root = JsonParser().parse(json).asJsonObject
        URL(root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").asString)
    } catch (e: Exception) {
        null
    }
}
