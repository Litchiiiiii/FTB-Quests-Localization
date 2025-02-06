package me.litchi.ftbqlocal.utils;

import dev.architectury.platform.Platform;

public class Constants {
    public static class PackMCMeta {
        public static final String PACKNAME = "FTB-Quest-Localization-Resourcepack.zip";
        public static final String DESCRIPTION = "Localization Resourcepack";
        // https://minecraft.wiki/w/Pack_format
        public static final int PACKFORMAT = 8;
        public static final String FILEPATH = "pack.mcmeta";// Output file path
        public static final String OUTPUTFOLDER = "FTBLang";
        public static final String KUBEJSFOLDER = "kubejs\\assets\\ftbquestlocalizer\\lang\\";
        public static final String QUESTFOLDER = "config\\ftbquests\\quests\\";
        public static final String BACKUPFOLDER = "backup\\ftbquests\\quests";
        public static final String KUBEJSBACKUPFOLDER = "backup\\"+KUBEJSFOLDER;
        public static final String GAMEDIR = Platform.getGameFolder().toString();

    }
}
