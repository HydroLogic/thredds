/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.d4ts;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.servlet.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/d4ts/{dataset}")
public class D4TSServlet extends DapServlet
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static final String TESTDATADIR = "/WEB-INF/resources/testfiles"; // relative to resource path

    //////////////////////////////////////////////////
    // Type Decls

    static class D4TSFactory extends DSPFactory
    {

        public D4TSFactory()
        {
            // Register known DSP classes: order is important.
            // Only used in server
            registerDSP(SynDSP.class, true);
            registerDSP(CDMDSP.class, true);
        }

    }

    //////////////////////////////////////////////////

    static {
        DapCache.setFactory(new D4TSFactory());
    }

    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSServlet()
    {
        super("/d4ts");
    }

    @PostConstruct
    public void init() throws ServletException
    {
        super.init();
    }

    //////////////////////////////////////////////////////////

    @Override
    @RequestMapping(value="{dataset}", method= RequestMethod.GET)
    protected void
    doGet(HttpServletRequest req, HttpServletResponse resp, @PathVariable("dataset") String dataset)
            throws IOException, ServletException
    {
        super.doGet(req,resp,dataset);
    }

    @Override
    protected void
    doFavicon(DapRequest drq)
            throws IOException
    {
        String favfile = getResourcePath(drq);
        if(favfile != null) {
            try (FileInputStream fav = new FileInputStream(favfile)) {
                byte[] content = DapUtil.readbinaryfile(fav);
                OutputStream out = drq.getOutputStream();
                out.write(content);
            }
        }
    }

    @Override
    protected void
    doCapabilities(DapRequest drq)
            throws IOException
    {
        addCommonHeaders(drq);

        // Figure out the directory containing
        // the files to display.
        String dir = getResourcePath(drq);
        if(dir == null)
            throw new DapException("Cannot locate resources directory");

        // Generate the front page
        FrontPage front = new FrontPage(dir, drq);
        String frontpage = front.buildPage();

        if(frontpage == null)
            throw new DapException("Cannot create front page")
                    .setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // // Convert to UTF-8 and then to byte[]
        byte[] frontpage8 = DapUtil.extract(DapUtil.UTF8.encode(frontpage));

        OutputStream out = drq.getOutputStream();
        out.write(frontpage8);

    }

    @Override
    protected String
    getResourcePath(DapRequest drq)
            throws IOException
    {
        // Using context information, we need to
        // construct a file path to the specified dataset
        String suffix = DapUtil.denullify(drq.getDataset());
        String datasetfilepath = drq.getRealPath(TESTDATADIR + DapUtil.canonicalpath(suffix));

        // See if it really exists and is readable and of proper type
        File dataset = new File(datasetfilepath);
        if(!dataset.exists())
            throw new DapException("Requested file does not exist: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);

        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        return datasetfilepath;
    }

    @Override
    protected long getBinaryWriteLimit()
    {
        return DEFAULTBINARYWRITELIMIT;
    }
}
