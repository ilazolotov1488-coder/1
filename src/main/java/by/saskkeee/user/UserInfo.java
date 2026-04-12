package by.saskkeee.user;

import net.minecraft.client.MinecraftClient;

public class UserInfo {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static String getUsername() {
        return mc.getSession().getUsername();
    }

    public static String getUID() {
        return "1";
    }

    public static String getRole() {
        return "Разработчик";
    }

}
