
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;


public class OpenAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.OpenAction");
    private int targetIndex;


    public OpenAction(int targetIndex) {
        super("Editor " + (targetIndex + 1));

        this.targetIndex = targetIndex;
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);

        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                Presentation presentation = event.getPresentation();

                int sourceIndex = projectComponent.indexOfEditor(editor);

                if (targetIndex == sourceIndex) {
                    presentation.setEnabled(false);
                }
                else {
                    presentation.setEnabled(true);
                }
            }
            else {
                log.warn("Editor in OpenAction.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in OpenAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("OpenAction.actionPerformed(), project: " + project);

        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                int sourceIndex = projectComponent.indexOfEditor(editor);
                projectComponent.openAction(sourceIndex, targetIndex);
            }
            else {
                log.warn("Editor in OpenAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in OpenAction.actionPerformed() is null.");
        }
    }
}
