package net.countercraft.movecraft.repair.localisation;


import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.repair.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

public class I18nSupport {
    private static Properties langFile;

    public static void init() {
        langFile = new Properties();

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
            langFile.load(stream);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getInternationalisedString(String key) {
        String ret = langFile.getProperty(key);
        if (ret != null)
            return ret;

        return key;
    }
}
