package net.formicary.pricer.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.*;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;
import org.fpml.spec503wd3.DataDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hsuleiman
 *         Date: 1/4/12
 *         Time: 4:51 PM
 */
public class FastInfosetTransformer {
  private File from;
  private File to;
  private JAXBContext context;
  private static final Logger log = LoggerFactory.getLogger(FastInfosetTransformer.class);

  public FastInfosetTransformer(String fromDir, String toDir) throws JAXBException {
    this.from = new File(fromDir);
    this.to = new File(toDir);
    if(!to.exists()) to.mkdirs();
    this.context = JAXBContext.newInstance(DataDocument.class);
  }

  private List<File> list(final File root) {
    final List<File> files = new ArrayList<File>();
    files.addAll(Arrays.asList(root.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        if(file.getName().endsWith((".xml"))) {
          return true;
        }
        if(file.isDirectory()) {
          files.addAll(list(file));
        }
        return false;
      }
    })));
    return files;
  }

  private void convert() throws FileNotFoundException, JAXBException {
    Unmarshaller um = context.createUnmarshaller();
    Marshaller m = context.createMarshaller();
    List<File> files = list(from);
    int count = 1;
    long now = System.currentTimeMillis();
    for (File file : files) {
      JAXBElement<DataDocument> dd = (JAXBElement<DataDocument>)um.unmarshal(file);
      String name = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".fi";
      File destDir = to;
      //if it's nested and we're sending it somewhere else, then create the same dir structure
      if(!file.getParentFile().equals(to) && !to.equals(from)) {
        String subPath = file.getParentFile().getAbsolutePath().substring(from.getAbsolutePath().length());
        destDir = new File(to, subPath);
        destDir.mkdirs();
      }
      File target = new File(destDir, name);
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(target));
      StAXDocumentSerializer serializer = new StAXDocumentSerializer(os);
      m.marshal(dd, (XMLStreamWriter)serializer);
      if(++count % 5000 == 0) {
        log.info("Transformed {} files", count);
      }
    }
    log.info("Transformed {} files in {}ms", count, System.currentTimeMillis() - now);
  }

  public static void main(String[] args) throws JAXBException, FileNotFoundException {
    FastInfosetTransformer transformer = new FastInfosetTransformer(args[0], args[1]);
    transformer.convert();
  }
}
