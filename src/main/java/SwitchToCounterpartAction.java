
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;


public class SwitchToCounterpartAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.mkd.TabsSuck.SwitchToCounterpartAction");


    public SwitchToCounterpartAction() {
        super("Switch to Counterpart");
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);

        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                Presentation presentation = event.getPresentation();

                if (projectComponent.isEditorAttachedToProjectWindow(editor)) {
                    presentation.setVisible(true);
                }
                else {
                    presentation.setVisible(false);
                }
            }
            else {
                log.warn("Editor in SwitchToCounterpartAction.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in SwitchToCounterpartAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("SwitchToCounterpartAction.actionPerformed(), project: " + project);

        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                int sourceIndex = projectComponent.indexOfEditor(editor);
                projectComponent.switchToCounterpartAction(sourceIndex);
            }
            else {
                log.warn("Editor in SwitchToCounterpartAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in SwitchToCounterpartAction.actionPerformed() is null.");
        }
    }
}
