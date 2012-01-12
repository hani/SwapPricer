package net.formicary.pricer.tools;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;
import org.fpml.spec503wd3.DataDocument;

import javax.xml.bind.*;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;

/**
 * @author hsuleiman
 *         Date: 1/4/12
 *         Time: 4:51 PM
 */
public class FastInfosetTransformer {
  private File from;
  private File to;
  private JAXBContext context;

  public FastInfosetTransformer(String fromDir, String toDir) throws JAXBException {
    this.from = new File(fromDir);
    this.to = new File(toDir);
    if(!to.exists()) to.mkdirs();
    this.context = JAXBContext.newInstance(DataDocument.class);
  }

  private void convert() throws FileNotFoundException, JAXBException {
    Unmarshaller um = context.createUnmarshaller();
    Marshaller m = context.createMarshaller();
    for (String file : from.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith((".xml"));
      }
    })) {
      JAXBElement<DataDocument> dd = (JAXBElement<DataDocument>)um.unmarshal(new File(from, file));
      String target = file.substring(0, file.lastIndexOf('.')) + ".fi";
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(to, target)));
      StAXDocumentSerializer serializer = new StAXDocumentSerializer(os);
      m.marshal(dd, (XMLStreamWriter)serializer);
    }
  }

  public static void main(String[] args) throws JAXBException, FileNotFoundException {
    FastInfosetTransformer transformer = new FastInfosetTransformer(args[0], args[1]);
    transformer.convert();
  }
}
