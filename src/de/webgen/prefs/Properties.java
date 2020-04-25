/* Copyright (C) 2020 Christoph Theis */
package de.webgen.prefs;

import java.io.File;

public class Properties {
    // Verzeichnisse in einer vorgegeben Reihenfolge suchen:
    // - aktuelles Verzeichnis
    // - user app data
    // - all app data
    public static File findPath(String what) {
        File file = null;

        file = new File(System.getProperty("user.dir") + File.separator + what);

        if (!file.exists())
            file = new File(System.getenv("ALLUSERSPROFILE") + File.separator + "TTM" + File.separator + what);

        if (!file.exists())
            file = new File(System.getenv("APPDATA") + File.separator + "TTM" + File.separator + what);

        if (!file.exists())
            file = new File(System.getenv("LOCALAPPDATA") + File.separator + "TTM" + File.separator + what);

        if (!file.exists())
            return null;

        return file;
    }

    
}
