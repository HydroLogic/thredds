// $Id$
package thredds.crawlabledataset;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 17, 2005 3:31:33 PM
 */
public class RegExpAndReplaceOnNameLabeler implements CrawlableDatasetLabeler
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( RegExpAndReplaceOnNameLabeler.class );

  private String regExp;
  private java.util.regex.Pattern pattern;
  private String replaceString;

  public RegExpAndReplaceOnNameLabeler( String regExp, String replaceString )
  {
    this.regExp = regExp;
    this.pattern = java.util.regex.Pattern.compile( regExp );
    this.replaceString = replaceString;
  }

  public Object getConfigObject() { return null; }
  public String getRegExp() { return regExp; }
  public String getReplaceString() { return replaceString; }

  public String getLabel( CrawlableDataset dataset )
  {
    java.util.regex.Matcher matcher = this.pattern.matcher( dataset.getName() );
    if ( ! matcher.find() ) return null;

    StringBuffer startTime = new StringBuffer();
    matcher.appendReplacement( startTime, this.replaceString );
    startTime.delete( 0, matcher.start() );

    if ( startTime.length() == 0 ) return null;

    return startTime.toString();
  }
}
/*
 * $Log: RegExpAndReplaceOnNameLabeler.java,v $
 * Revision 1.2  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/18 23:51:05  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 *
 */