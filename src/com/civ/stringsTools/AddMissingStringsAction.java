package com.civ.stringsTools;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kiva on 5/31/14.
 */
public class AddMissingStringsAction extends AnAction {

    private static final String STRINGS_FILE_NAME = "strings.xml";
    private static final String DEFAULT_VALUES_DIR_NAME = "values";
    private static final String LOCALIZED_VALUES_DIR_NAME = "values-";
    private static final String TAG_STRING = "string";
    private static final String TAG_STRING_ARRAY = "string-array";
    private static final String ATTRIBUTE_NAME = "name";

    @Override
    public void actionPerformed(AnActionEvent e) {
        XmlFile currentFile = (XmlFile) e.getData(DataKeys.PSI_FILE);
        XmlFile defaultFile = (XmlFile) getDefaultStringsFile(currentFile);

        if (currentFile == defaultFile) {
            List<XmlFile> localizedFiles = getLocalizedStringsFiles(currentFile);
            WriteCommandAction.runWriteCommandAction(e.getProject(),
                    new AddMissingStringsFromMultipleSourcesCommand(localizedFiles, defaultFile));
        } else {
            WriteCommandAction.runWriteCommandAction(e.getProject(), new AddMissingStringsCommand(defaultFile,
                    currentFile));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        PsiFile file = e.getData(DataKeys.PSI_FILE);

        if (!(file instanceof XmlFile))
            e.getPresentation().setEnabled(false);

        if (!file.getName().equals(STRINGS_FILE_NAME))
            e.getPresentation().setEnabled(false);
    }

    private PsiFile getDefaultStringsFile(PsiFile currentFile) {
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

    private List<XmlFile> getLocalizedStringsFiles(PsiFile currentFile) {
        PsiDirectory currentDir = currentFile.getContainingDirectory();
        if (currentDir == null)
            return null;

        PsiDirectory resDir = currentDir.getParentDirectory();
        if (resDir == null)
            return null;

        List<XmlFile> result = new ArrayList<XmlFile>();
        PsiDirectory[] subdirs = resDir.getSubdirectories();
        for (PsiDirectory subdir : subdirs) {
            if (subdir.getName().startsWith(LOCALIZED_VALUES_DIR_NAME)) {
                XmlFile localizedFile = (XmlFile) subdir.findFile(STRINGS_FILE_NAME);
                if (localizedFile != null)
                    result.add(localizedFile);
            }
        }

        return result;
    }


    private static class AddMissingStringsCommand implements Runnable {

        private XmlFile mSource;

        private XmlFile mDestination;
        private String mLocalizationName;

        private AddMissingStringsCommand(XmlFile source, XmlFile destination) {
            this.mSource = source;
            this.mDestination = destination;

            String dirName = destination.getContainingDirectory().getName();
            mLocalizationName = dirName.equals(DEFAULT_VALUES_DIR_NAME) ? "def" : dirName.substring(7);
        }

        @Override
        public void run() {
            Set<String> existingNames = getStringsNames(mDestination);
            XmlTag[] tags = mSource.getRootTag().getSubTags();

            for (XmlTag tag : tags) {
                String name = tag.getAttributeValue(ATTRIBUTE_NAME);
                if (name != null && !existingNames.contains(name)) {
                    XmlTag resultTag = mDestination.getRootTag().addSubTag(tag, false);
                    localizeTag(resultTag);
                }
            }
        }

        private Set<String> getStringsNames(XmlFile file) {
            Set<String> names = new HashSet<String>();
            XmlTag[] tags = file.getRootTag().getSubTags();

            for (XmlTag tag : tags) {
                String name = tag.getAttributeValue(ATTRIBUTE_NAME);
                if (name != null)
                    names.add(name);
            }

            return names;
        }

        private void localizeTag(XmlTag tag) {
            String name = tag.getName();
            if (name.equals(TAG_STRING))
                localizeStringTag(tag);
            else if (name.equals(TAG_STRING_ARRAY))
                localizeStringArrayTag(tag);
            // Do nothing for other tags
        }

        private void localizeStringTag(XmlTag tag) {
            String text = tag.getValue().getText();
            tag.getValue().setText(String.format("%s:[%s]", mLocalizationName, text));
        }

        private void localizeStringArrayTag(XmlTag tag) {
            XmlTag[] subtags = tag.getSubTags();
            for (XmlTag subtag : subtags)
                localizeStringTag(subtag);
        }

    }

    private static class AddMissingStringsFromMultipleSourcesCommand implements Runnable {

        private List<XmlFile> mSources;
        private XmlFile mDestination;

        private AddMissingStringsFromMultipleSourcesCommand(List<XmlFile> sources, XmlFile destination) {
            mSources = sources;
            mDestination = destination;
        }

        @Override
        public void run() {
            if (mSources == null || mSources.isEmpty())
                return;

            for (XmlFile source : mSources)
                new AddMissingStringsCommand(source, mDestination).run();
        }
    }
}
