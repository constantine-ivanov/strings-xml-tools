package com.civ.stringsTools;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.source.xml.XmlElementImpl;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlComment;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlPsiManager;
import com.intellij.xml.util.XmlPsiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortStringsAction extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        PsiFile currentFile = e.getData(DataKeys.PSI_FILE);
        PsiFile defaultFile = Helpers.getDefaultStringsFile(currentFile);

        WriteCommandAction.runWriteCommandAction(e.getProject(), new SortStringsCommand((XmlFile) currentFile,
                (XmlFile) defaultFile));
    }

    @Override
    public void update(AnActionEvent e) {
        PsiFile file = e.getData(DataKeys.PSI_FILE);

        if (!Helpers.isLocalizedStringsFile(file))
            e.getPresentation().setEnabled(false);
    }


    private static class SortStringsCommand implements Runnable {

        private XmlFile mReferenceFile;
        private XmlFile mTargetFile;

        private SortStringsCommand(XmlFile where, XmlFile reference) {
            mTargetFile = where;
            mReferenceFile = reference;
        }

        @Override
        public void run() {
            List<PsiElement> referenceElements = getTagsAndComments(mReferenceFile);
            List<PsiElement> targetElements = getTagsAndComments(mTargetFile);

            // Create maps of pairs <Element name, Element> to ease element search by name
            Map<String, PsiElement> referenceMap = new HashMap<String, PsiElement>();
            Map<String, PsiElement> targetMap = new HashMap<String, PsiElement>();
            for(int i = 0; i < referenceElements.size(); i++) {
                PsiElement element = referenceElements.get(i);
                referenceMap.put(getElementName(element), element);
            }
            for(int i = 0; i < targetElements.size(); i++) {
                PsiElement element = targetElements.get(i);
                targetMap.put(getElementName(element), element);
            }

            int targetIndex = 0;
            for(int i = 0; i < Math.min(referenceElements.size(), targetElements.size()); i++) {
                String referenceName = getElementName(referenceElements.get(i));
                String targetName = getElementName(targetElements.get(targetIndex));

                if(referenceName.equals(targetName)) {
                    targetIndex++;
                    continue;
                }

                PsiElement replace = targetMap.get(referenceName);
                if(replace == null)
                    continue;

                int replaceIndex = targetElements.indexOf(replace);
                swapElements(targetElements, targetMap, targetIndex, replaceIndex);
                targetIndex++;
            }
        }

        private String getElementName(PsiElement element) {
            if (element instanceof XmlTag)
                return ((XmlTag) element).getAttributeValue(Helpers.ATTRIBUTE_NAME);
            else return element.getText();
        }

        private List<PsiElement> getTagsAndComments(XmlFile file) {
            List<PsiElement> result = new ArrayList<PsiElement>();
            PsiElement[] elements = file.getRootTag().getChildren();
            for(PsiElement element : elements) {
                if(element instanceof XmlTag || element instanceof XmlComment)
                    result.add(element);
            }

            return result;
        }

        private void swapElements(List<PsiElement> elements, Map<String, PsiElement> map, int index1, int index2) {
            PsiElement element1 = elements.get(index1);
            PsiElement element2 = elements.get(index2);
            PsiElement parent =  element1.getParent();

            // Swap elements in the PSI tree
            PsiElement newElement1 = parent.addAfter(element2, element1);
            PsiElement newElement2 = parent.addAfter(element1, element2);
            element1.delete();
            element2.delete();

            // Swap elements in the provided list
            elements.set(index1, newElement1);
            elements.set(index2, newElement2);

            // Add new elements to the map
            map.put(getElementName(newElement1), newElement1);
            map.put(getElementName(newElement2), newElement2);
        }
    }
}
