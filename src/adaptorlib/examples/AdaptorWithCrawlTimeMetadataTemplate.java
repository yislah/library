package adaptorlib.examples;

import adaptorlib.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

/**
 * Demonstrates what code is necessary for putting restricted
 * content onto a GSA.  The key operations are:
 * <ol><li> providing document ids
 *   <li> providing document bytes and ACLs given a document id</ol>
 */
public class AdaptorWithCrawlTimeMetadataTemplate extends AbstractAdaptor {
  private static final Logger log
      = Logger.getLogger(AdaptorWithCrawlTimeMetadataTemplate.class.getName());
  private Charset encoding = Charset.forName("UTF-8");

  /** Gives list of document ids that you'd like on the GSA. */
  @Override
  public void getDocIds(DocIdPusher pusher) throws InterruptedException {
    ArrayList<DocId> mockDocIds = new ArrayList<DocId>();
    /* Replace this mock data with code that lists your repository. */
    mockDocIds.add(new DocId("1001"));
    mockDocIds.add(new DocId("1002"));
    pusher.pushDocIds(mockDocIds);
  }

  /** Gives the bytes of a document referenced with id. */
  @Override
  public void getDocContent(Request req, Response resp) throws IOException {
    DocId id = req.getDocId();
    String str;
    if ("1001".equals(id.getUniqueId())) {
      str = "Document 1001 says hello and apple orange";
      // Make set to accumulate meta items.
      Set<MetaItem> metaItems = new TreeSet<MetaItem>();
      // Add user ACL.
      List<String> users1001 = Arrays.asList("peter,bart,simon");
      metaItems.add(MetaItem.permittedUsers(users1001));
      // Add group ACL.
      List<String> groups1001 = Arrays.asList("support,sales");
      metaItems.add(MetaItem.permittedGroups(groups1001));
      // Add custom meta items.
      metaItems.add(MetaItem.raw("my-special-key", "my-custom-value"));
      metaItems.add(MetaItem.raw("date", "not soon enough"));
      // Make metadata object, which checks items for consistency.
      // Must set metadata before getting OutputStream
      resp.setMetadata(new Metadata(metaItems));
    } else if ("1002".equals(id.getUniqueId())) {
      str = "Document 1002 says hello and banana strawberry";
      // Another example.
      Set<MetaItem> metaItems = new TreeSet<MetaItem>();
      // A document that's not public and has no ACLs causes head requests.
      metaItems.add(MetaItem.isNotPublic());
      // Set display URL.
      metaItems.add(MetaItem.displayUrl("http://www.google.com"));
      // Add custom meta items.
      metaItems.add(MetaItem.raw("date", "better never than late"));
      // Make metadata object, which checks items for consistency.
      // Must set metadata before getting OutputStream
      resp.setMetadata(new Metadata(metaItems));
    } else {
      throw new FileNotFoundException(id.getUniqueId());
    }
    // Must get the OutputStream after any possibility of throwing a
    // FileNotFoundException.
    OutputStream os = resp.getOutputStream();
    os.write(str.getBytes(encoding));
  }

  /** An example main for an adaptor that:<br>
   * <ol><li> enables serving doc contents,
   *   <li> sends docs ids at program start
   *   <li> and sends doc ids on schedule.</ol>
   */
  public static void main(String a[]) throws InterruptedException {
    Config config = new Config();
    config.autoConfig(a);
    Adaptor adaptor = new AdaptorWithCrawlTimeMetadataTemplate();
    GsaCommunicationHandler gsa = new GsaCommunicationHandler(adaptor, config);

    // Setup providing content.
    try {
      gsa.beginListeningForContentRequests();
      log.info("doc content serving started");
    } catch (IOException e) {
      throw new RuntimeException("could not start serving", e);
    }

    // Push once at program start.
    gsa.pushDocIds();

    // Schedule pushing of doc ids once per day.
    gsa.beginPushingDocIds(
        new ScheduleOncePerDay(/*hour*/3, /*minute*/0, /*second*/0));
    log.info("doc id pushing has been put on schedule");
  }
}
