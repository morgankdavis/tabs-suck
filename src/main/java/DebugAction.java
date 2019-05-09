
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;


public class DebugAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.FollowFirstAction");


    @Override
    public void actionPerformed(AnActionEvent event) {

        log.debug("DebugAction.actionPerformed()");

        Project project = event.getProject();
        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);
        if (projectComponent != null) {
            projectComponent.debugAction();
        }
    }
}
