package com.civ.stringsTools;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.PsiPlainTextImpl;
import com.intellij.psi.impl.source.xml.XmlCommentImpl;
import com.intellij.psi.xml.XmlComment;
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

    @Override
    public void actionPerformed(AnActionEvent e) {
        XmlFile currentFile = (XmlFile) e.getData(DataKeys.PSI_FILE);
        XmlFile defaultFile = (XmlFile) Helpers.getDefaultStringsFile(currentFile);

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
        if (!Helpers.isStringsFile(file))
            e.getPresentation().setEnabled(false);
    }

    private static List<XmlFile> getLocalizedStringsFiles(PsiFile currentFile) {
        PsiDirectory currentDir = currentFile.getContainingDirectory();
        if (currentDir == null)
            return null;

        PsiDirectory resDir = currentDir.getParentDirectory();
        if (resDir == null)
            return null;

        List<XmlFile> result = new ArrayList<XmlFile>();
        PsiDirectory[] subdirs = resDir.getSubdirectories();
        for (PsiDirectory subdir : subdirs) {
            if (subdir.getName().startsWith(Helpers.LOCALIZED_VALUES_DIR_NAME)) {
                XmlFile localizedFile = (XmlFile) subdir.findFile(Helpers.STRINGS_FILE_NAME);
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
            mLocalizationName = dirName.equals(Helpers.DEFAULT_VALUES_DIR_NAME) ? "def" : dirName.substring(7);
        }

        @Override
        public void run() {
            Set<String> existingNames = getStringsNames(mDestination);
            XmlTag[] tags = mSource.getRootTag().getSubTags();

            for (XmlTag tag : tags) {
                String name = tag.getAttributeValue(Helpers.ATTRIBUTE_NAME);
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
                String name = tag.getAttributeValue(Helpers.ATTRIBUTE_NAME);
                if (name != null)
                    names.add(name);
            }

            return names;
        }

        private void localizeTag(XmlTag tag) {
            String name = tag.getName();
            if (name.equals(Helpers.TAG_STRING))
                localizeStringTag(tag);
            else if (name.equals(Helpers.TAG_STRING_ARRAY) || name.equals(Helpers.TAG_PLURALS))
                localizeStringArrayTag(tag);
            // Do nothing for other tags
        }

        private void localizeStringTag(XmlTag tag) {
            // Do nothing if string tag contains subtags
            if (tag.getSubTags().length > 0)
                return;

            String text = tag.getValue().getText();
            String trimmedText = tag.getValue().getTrimmedText();
            boolean isCdata = text.startsWith("<![CDATA[");
            String newText = String.format("%s:[%s]", mLocalizationName, isCdata ? trimmedText : text);
            if (isCdata)
                tag.getValue().setText(newText);
            else
                tag.getValue().setEscapedText(newText);
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
