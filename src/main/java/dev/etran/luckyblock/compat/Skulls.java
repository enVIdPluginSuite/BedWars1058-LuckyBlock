package dev.etran.luckyblock.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;

/** Puts a custom skin on a skull, on every version (PlayerProfile API on 1.18.1+, GameProfile before). */
public final class Skulls {
  private Skulls() {}

  /** Applies the base64 "textures" value to the skull. Returns false if it could not be applied. */
  public static boolean applyTexture(SkullMeta meta, String base64Value) {
    if (base64Value == null || base64Value.trim().isEmpty())
      return false;
    String value = base64Value.trim();
    UUID id = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    try {
      if (hasProfileApi())
        return viaProfileApi(meta, value, id);
      return viaGameProfile(meta, value, id);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return false;
    }
  }

  private static boolean hasProfileApi() {
    try {
      Class.forName("org.bukkit.inventory.meta.SkullMeta").getMethod("setOwnerProfile", Class.forName("org.bukkit.profile.PlayerProfile"));
      return true;
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }

  private static boolean viaProfileApi(SkullMeta meta, String value, UUID id) throws ReflectiveOperationException {
    URL skin = decodeSkinUrl(value);
    if (skin == null)
      return false;
    Class<?> profileClass = Class.forName("org.bukkit.profile.PlayerProfile");
    Class<?> texturesClass = Class.forName("org.bukkit.profile.PlayerTextures");
    Object profile = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class).invoke(null, id, "LuckyBlock");
    Object textures = profileClass.getMethod("getTextures").invoke(profile);
    texturesClass.getMethod("setSkin", URL.class).invoke(textures, skin);
    profileClass.getMethod("setTextures", texturesClass).invoke(profile, textures);
    // look the method up on the public SkullMeta interface: the implementing class is not public, so
    // calling its Method object from here would throw IllegalAccessException
    SkullMeta.class.getMethod("setOwnerProfile", profileClass).invoke(meta, profile);
    return true;
  }

  private static boolean viaGameProfile(SkullMeta meta, String value, UUID id) throws ReflectiveOperationException {
    Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
    Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
    Object profile = gameProfile.getConstructor(UUID.class, String.class).newInstance(id, "LuckyBlock");
    Object properties = gameProfile.getMethod("getProperties").invoke(profile);
    Object textures = property.getConstructor(String.class, String.class).newInstance("textures", value);
    Method put = properties.getClass().getMethod("put", Object.class, Object.class);
    put.invoke(properties, "textures", textures);
    Field field = meta.getClass().getDeclaredField("profile");
    field.setAccessible(true);
    field.set(meta, profile);
    return true;
  }

  @SuppressWarnings("deprecation")
  private static URL decodeSkinUrl(String base64Value) {
    try {
      String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
      JsonObject root = new JsonParser().parse(json).getAsJsonObject();
      return new URL(root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
    } catch (Exception e) {
      return null;
    }
  }
}
