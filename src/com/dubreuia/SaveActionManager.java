package com.dubreuia;

import com.dubreuia.processors.ProcessorFactory;
import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dubreuia.utils.Documents.isDocumentActive;
import static com.dubreuia.utils.PsiFiles.isPsiFileExcluded;

public class SaveActionManager extends FileDocumentManagerAdapter {

    private static final Logger LOGGER = Logger.getInstance(SaveActionManager.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }

    private final Settings settings = ServiceManager.getService(Settings.class);

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        if (!SaveAllAction.TRIGGERED && isDocumentActive(document)) {
            LOGGER.debug("Save Actions - Document " + document + " is still active, do not execute");
            return;
        }
        final Project project = getProjectFromFocus();
        if (project != null) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (isPsiFileEligible(project, psiFile)) {
                processPsiFile(project, psiFile);
            }
        }
    }

    private Project getProjectFromFocus() {
        final DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
        if (null != dataContext) {
            return DataKeys.PROJECT.getData(dataContext);
        }
        return null;
    }

    private boolean isPsiFileEligible(Project project, PsiFile psiFile) {
        return psiFile != null && !isPsiFileExcluded(project, psiFile, settings.getExclusions());
    }

    private void processPsiFile(final Project project, final PsiFile psiFile) {
        final List<AbstractLayoutCodeProcessor> processors =
                ProcessorFactory.INSTANCE.getSaveActionsProcessors(project, psiFile, settings);
        LOGGER.debug("Save Actions - Running processors " + processors + ", file " + psiFile + ", project " + project);
        for (AbstractLayoutCodeProcessor processor : processors) {
            if (processor != null) {
                processor.run();
            }
        }
    }

}