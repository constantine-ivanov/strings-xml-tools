package com.civ.stringsTools;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.Nullable;

/**
 * Created by kiva on 6/9/14.
 */
public class Helpers {

    public static final String STRINGS_FILE_NAME = "strings.xml";
    public static final String DEFAULT_VALUES_DIR_NAME = "values";
    public static final String LOCALIZED_VALUES_DIR_NAME = "values-";
    public static final String TAG_STRING = "string";
    public static final String TAG_STRING_ARRAY = "string-array";
    public static final String ATTRIBUTE_NAME = "name";

    /**
     * @return true if the given PsiFile is a strings.xml file.
     */
    public static boolean isStringsFile(PsiFile file) {
        return file instanceof XmlFile && file.getName().equals(STRINGS_FILE_NAME);
    }

    /**
     * Returns true if the given file is the default strings.xml file
     */
    public static boolean isLocalizedStringsFile(PsiFile file) {
        if(!isStringsFile(file))
            return false;

        PsiDirectory dir = file.getContainingDirectory();
        return dir != null && dir.getName().startsWith(LOCALIZED_VALUES_DIR_NAME);
    }

    /**
     * Returns default strings.xml file.
     * @param currentFile One of localized strings files.
     */
    public static PsiFile getDefaultStringsFile(PsiFile currentFile) {
        PsiDirectory currentDir = currentFile.getContainingDirectory();
        if (currentDir == null)
            return null;
        // If the specified file is from the "values" directory, it's already our default strings file
        if (currentDir.getName().equals(DEFAULT_VALUES_DIR_NAME))
            return currentFile;

        PsiDirectory defaultDir = currentDir.getParentDirectory().findSubdirectory(DEFAULT_VALUES_DIR_NAME);
        if (defaultDir == null)
            return null;

        return defaultDir.findFile(STRINGS_FILE_NAME);
    }


}
