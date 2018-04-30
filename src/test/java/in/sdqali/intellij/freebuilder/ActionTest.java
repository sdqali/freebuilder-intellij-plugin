package in.sdqali.intellij.freebuilder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ActionTest {
  private Project project;
  private Editor editor;
  private OpenApiShim openApiShim;
  private PsiDocumentManager documentManager;
  private Document document;
  private PsiFile psiFile;
  private Presentation presentation;

  @Before
  public void setup() {
    project = mock(Project.class);
    editor = mock(Editor.class);
    document = mock(Document.class);
    presentation = mock(Presentation.class);
    when(editor.getDocument()).thenReturn(document);

    psiFile = mock(PsiJavaFile.class);

    openApiShim = mock(OpenApiShim.class);
    documentManager = mock(PsiDocumentManager.class);
    when(openApiShim.getDocumentManager(project)).thenReturn(documentManager);
  }

  @Test
  public void shouldBeVisibleForJavaFiles() {
    Action action = new Action(openApiShim);
    AnActionEvent event = mock(AnActionEvent.class);
    when(event.getPresentation()).thenReturn(presentation);

    when(event.getProject()).thenReturn(project);
    when(event.getData(CommonDataKeys.EDITOR)).thenReturn(editor);

    when(documentManager.getPsiFile(document)).thenReturn(psiFile);

    action.update(event);
    verify(presentation, times(1)).setVisible(true);
  }
}