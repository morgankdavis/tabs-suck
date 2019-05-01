import com.intellij.ide.actions.EditSourceInNewWindowAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class NewWindowAction extends EditSourceInNewWindowAction {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.NewWindowAction");


    public NewWindowAction() {
        super();
    }

    @Override
    public void update(AnActionEvent event) {

        super.update(event);

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                Presentation presentation = event.getPresentation();

                if (projectComponent.isEditorAttachedToProjectWindow(editor)) {

                    presentation.setText("New Window");
                    presentation.setVisible(true);
                }
                else {
                    presentation.setVisible(false);
                }
            }
            else {
                log.warn("Editor in NewWindowAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in NewWindowAction.actionPerformed() is null.");
        }
    }
}
