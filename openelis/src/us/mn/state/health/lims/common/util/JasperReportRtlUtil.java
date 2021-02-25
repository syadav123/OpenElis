package us.mn.state.health.lims.common.util;

import java.util.Iterator;
import java.util.List;

import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * Report utilities
 * Please refer to: http://community.jaspersoft.com/questions/523041/right-left-arabic-reports
 * There is another solution at: http://jaspermirror.sourceforge.net/
 * which is not used here
 * @author AFattahi
 *
 */
public class JasperReportRtlUtil {

    public JasperReportRtlUtil(){

    }
    /**
     * mirror each page layout
     * @param print
     */
    public void mirrorLayout(JasperPrint print) {
        int pageWidth = print.getPageWidth();
        for (Object element : print.getPages()) {
            JRPrintPage page = (JRPrintPage) element;
            mirrorLayout(page.getElements(), pageWidth);
        }
    }

    /**
     * mirror a list of elements
     * @param print
     */
    protected void mirrorLayout(List<?> elements, int totalWidth) {
        for (Iterator<?> it = elements.iterator(); it.hasNext();) {
            JRPrintElement element = (JRPrintElement) it.next();
            int mirrorX = totalWidth - element.getX() - element.getWidth();
            element.setX(mirrorX);

            if (element instanceof JRPrintFrame) {
                JRPrintFrame frame = (JRPrintFrame) element;
                mirrorLayout(frame.getElements(), frame.getWidth());
            }
        }
    }
}
