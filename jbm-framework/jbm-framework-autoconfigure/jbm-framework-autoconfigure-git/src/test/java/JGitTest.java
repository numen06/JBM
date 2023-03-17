import jbm.framework.boot.autoconfigure.git.handler.JGitRepo;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-18 02:09
 **/
public class JGitTest {


    @Test
    public void testJGit() throws IOException, GitAPIException {
        JGitRepo jGitRepo = new JGitRepo("git@gitee.com:numen06/JBM.git");
        jGitRepo.init();
    }


}
