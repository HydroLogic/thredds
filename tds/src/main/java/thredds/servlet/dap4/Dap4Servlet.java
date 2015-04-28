/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.servlet.dap4;

import dap4.servlet.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.ThreddsConfig;
import thredds.server.dap4.ThreddsDSP;
import ucar.nc2.constants.CDM;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
@RequestMapping("/dap4/{dataset}")
public class Dap4Servlet extends DapServlet
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    // NetcdfDataset enhancement to use: need only coord systems
    //static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    //////////////////////////////////////////////////
    // Type Decls

    static class Dap4Factory extends DSPFactory
    {

        public Dap4Factory()
        {
            // Register known DSP classes: order is important.
            registerDSP(ThreddsDSP.class, true);
        }

    }

    static {
        DapCache.setFactory(new Dap4Factory());
    }

    //////////////////////////////////////////////////
    // Constructor(s)

    public Dap4Servlet()
    {
        super("/dap4");
    } // pass up the @RequestMapping value

    @PostConstruct
    public void init() throws ServletException {
        super.init();
    }

    //////////////////////////////////////////////////////////

    @Override
    protected void
    doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException
    {
        super.doGet(req, resp);
    }

    //////////////////////////////////////////////////////////

    @Override
    protected void
    doFavicon(DapRequest drq)
            throws IOException
    {
        throw new UnsupportedOperationException("Favicon");
    }

    @Override
    protected void
    doCapabilities(DapRequest drq)
            throws IOException
    {
        addCommonHeaders(drq);
        OutputStream out = drq.getOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
                CDM.utf8Charset));
        pw.println("Capabilities page not yet supported");
        pw.flush();
    }

    @Override
    protected String
    getResourcePath(DapRequest drq)
            throws IOException
    {
        // Using context information, we need to
        // construct a file path to the specified dataset
        String datasetpath = drq.getDataset();
        if(datasetpath.startsWith("/"))
            datasetpath = datasetpath.substring(1);
        return TdsRequestedDataset.getLocationFromRequestPath(datasetpath);
    }

    @Override
    protected long
    getBinaryWriteLimit()
    {
        // Thredds config is hosed; it needs to be constructed statically.
        // Work around: just use the default
        int dfalt = (int) (DapServlet.DEFAULTBINARYWRITELIMIT / 1000000); // default
        int mblimit = dfalt;
        /*
        try {
            mblimit = ThreddsConfig.getInt("Dap4.binaryLimit",dfalt);
        } catch (Throwable e) {//probably being used in testcase
            mblimit = dfalt;
        } */
        return mblimit * 1000000;
    }

}


