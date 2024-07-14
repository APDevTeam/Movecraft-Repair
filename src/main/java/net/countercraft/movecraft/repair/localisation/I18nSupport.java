package net.countercraft.movecraft.repair.localisation;


import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.config.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

public class I18nSupport {
    private static Properties languageFile;

    public static void init() {
        languageFile = new Properties();

        File langDirectory = new File(MovecraftRepair.getInstance().getDataFolder().getAbsolutePath() + "/localisation");
        if (!langDirectory.exists())
            langDirectory.mkdirs();

        InputStream stream = null;
        try {
            stream = new FileInputStream(langDirectory.getAbsolutePath()+"/mc-repairlang_" + Config.Locale + ".properties");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (stream == null) {
            MovecraftRepair.getInstance().getLogger().log(Level.SEVERE, "Critical Error in localisation system!");
            MovecraftRepair.getInstance().getServer().shutdown();
        }

        try {
            languageFile.load(stream);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String get(String key) {
        String ret = languageFile.getProperty(key);
        return ret != null ? ret : key;
    }

    @Contract("_ -> new")
    public static @NotNull TextComponent getInternationalisedComponent(String key) {
        return Component.text(get(key));
    }
}
