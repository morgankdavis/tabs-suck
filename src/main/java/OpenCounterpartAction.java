
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;


public class OpenCounterpartAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.mkd.TabsSuck.OpenCounterpartAction");
    private int targetIndex;


    public OpenCounterpartAction(int targetIndex) {
        super("Editor " + (targetIndex + 1));

        this.targetIndex = targetIndex;
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);

        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                Presentation presentation = event.getPresentation();

                int sourceIndex = projectComponent.indexOfEditor(editor);

                if (targetIndex == -1 || targetIndex == sourceIndex ) {
                    presentation.setEnabled(false);
                }
                else {
                    presentation.setEnabled(true);
                }
            }
            else {
                log.warn("Editor in OpenCounterpartAction.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in OpenCounterpartAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("OpenCounterpartAction.actionPerformed(), project: " + project);

        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                int sourceIndex = projectComponent.indexOfEditor(editor);
                projectComponent.openCounterpartAction(sourceIndex, targetIndex);
            }
            else {
                log.warn("Editor in OpenCounterpartAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in OpenCounterpartAction.actionPerformed() is null.");
        }
    }
}
