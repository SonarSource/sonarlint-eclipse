package taint;

import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

public class taint_issue {
    public void execute(HttpServletRequest request) throws IOException {
        String tainted = request.getParameter("tainted");

        String[][] localArray = new String[1][2];
        localArray[0][0] = tainted;
        localArray[0][1] = "safe";

        new FileInputStream(localArray[0][0]).read();
    }
}
