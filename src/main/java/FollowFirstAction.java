
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;


public class FollowFirstAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.FollowFirstAction");

    static final String FOLLOW_TITLE = "Follow First Editor";
    static final String UNFOLLOW_TITLE = "Unfollow First Editor";


    public FollowFirstAction() {
        super(FOLLOW_TITLE);
    }


    /*
        https://intellij-support.jetbrains.com/hc/en-us/community/posts/206763405-How-to-dynamically-change-icons-in-the-tool-bar-

        Override AnAction.update(AnActionEvent) and update the Presentation
        object obtained from com.intellij.openapi.actionSystem.AnActionEvent.getPresentation().
        If your action is shown in other places than the toolbar you can check
        AnActionEvent.getPlace() to see what place is being updated.
     */

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);

        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                // three possibilities:
                // 1. this is the first editor
                //      set its title to FOLLOW_TITLE and disable it
                // 2. this is not first, and it's not following
                //      set its title to FOLLOW_TITLE, enable it
                // 3. this is not first, and it's following
                //      set its title to UNFOLLOW_TITLE, and enable it

                int sourceIndex = projectComponent.indexOfEditor(editor);
                Presentation presentation = event.getPresentation();

                if (projectComponent.isEditorAttachedToProjectWindow(editor)) {

                    if (sourceIndex == 0) {
                        // 1.
                        presentation.setText(FOLLOW_TITLE);
                        presentation.setEnabled(false);
                    } else {

                        boolean following = projectComponent.isFollowingFirst(sourceIndex);
                        if (following) {
                            // 3.
                            presentation.setText(UNFOLLOW_TITLE);
                            presentation.setEnabled(true);
                        } else {
                            // 2.
                            presentation.setText(FOLLOW_TITLE);
                            presentation.setEnabled(true);
                        }
                    }

                    presentation.setVisible(true);
                }
                else {
                    presentation.setVisible(false);
                }
            }
            else {
                log.warn("Editor in FollowFirstAction.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in FollowFirstAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("FollowFirstAction.actionPerformed(), project: " + project);

        TabsSuckProjectComponent projectComponent = (TabsSuckProjectComponent)project.getComponent(TabsSuckProjectComponent.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                int sourceIndex = projectComponent.indexOfEditor(editor);
                projectComponent.setIsFollowingFirst(sourceIndex, !projectComponent.isFollowingFirst(sourceIndex));
            }
            else {
                log.warn("Editor in FollowFirstAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in FollowFirstAction.actionPerformed() is null.");
        }
    }
}
