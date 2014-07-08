package jkind.api.xml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jkind.api.results.JKindResultRealizability;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParseThreadReal extends Thread {
	private final JKindXmlFileInputStream xmlStream;
	private Throwable throwable;
	private SAXParser parser;
	private XmlHandlerReal handler;

	public XmlParseThreadReal(JKindXmlFileInputStream xmlStream, JKindResultRealizability result)
			throws ParserConfigurationException, SAXException {
		super("Xml Parse");
		this.xmlStream = xmlStream;
		parser = SAXParserFactory.newInstance().newSAXParser();
		handler = new XmlHandlerReal(result);
	}


	@Override
	public void run() {

		try {
			LineInputStream lines = new LineInputStream(xmlStream);
			StringBuilder buffer = null;
			String line;
			while ((line = lines.readLine()) != null) {
				if (line.contains("</Realizability>")) {
					buffer.append(line);
					parseRealizability(buffer.toString());
					buffer = null;
				} else if (line.contains("<Realizability")) {
					buffer = new StringBuilder();
					buffer.append(line);
				} else if (buffer != null) {
					buffer.append(line);
				}
			}
		} catch (Throwable t) {
			throwable = t;
		}
	}

	public void parseRealizability(String realizabilityXml) throws SAXException, IOException {
		parser.parse(new InputSource(new StringReader(realizabilityXml)), handler);
	}

	public Throwable getThrowable() {
		return throwable;
	}
}
