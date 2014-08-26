package jkind.api.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jkind.JKindException;
import jkind.api.results.JKindResultRealizability;
import jkind.api.results.RealizabilityResult;
import jkind.interval.IntEndpoint;
import jkind.interval.Interval;
import jkind.interval.NumericEndpoint;
import jkind.interval.NumericInterval;
import jkind.interval.RealEndpoint;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidRealizability;
import jkind.results.Realizability;
import jkind.results.Signal;
import jkind.results.UnknownRealizability;
import jkind.results.ValidRealizability;
import jkind.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParseThreadReal extends Thread {
	private final InputStream xmlStream;
	private Throwable throwable;
	private final JKindResultRealizability result;
	private final DocumentBuilderFactory factory;
	
	public XmlParseThreadReal(InputStream xmlStream, JKindResultRealizability result) {
		super("Xml Parse");
		this.xmlStream = xmlStream;
		this.result = result;
		this.factory = DocumentBuilderFactory.newInstance();
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
					parseRealizabilityXml(buffer.toString());
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

	public void parseRealizabilityXml(String realizabilityXml) throws ParserConfigurationException {
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc;
		try {
			doc = builder.parse(new InputSource(new StringReader(realizabilityXml)));
		} catch (SAXException | IOException e) {
			throw new JKindException("Error parsing: " + realizabilityXml, e);
		}

		Realizability r = getRealizability(doc.getDocumentElement());

		RealizabilityResult real = result.getRealizabilityResult();
		if (real == null) {
			real = result.addRealizability(r.getName());
			if (real == null) {
				return;
			}
		}
		real.setRealizability(r);	
	}
	
	private Realizability getRealizability(Element realizabilityElement) {
		String name = realizabilityElement.getAttribute("name");
		double runtime = getRuntime(getElement(realizabilityElement, "Runtime"));
		int k = getK(getElement(realizabilityElement, "K"));
		String answer = getAnswer(getElement(realizabilityElement, "Answer"));
		List<String> invariants = getInvariants(getElements(realizabilityElement, "Invariant"));
		Counterexample cex = getCounterexample(getElement(realizabilityElement, "Counterexample"), k);

		switch (answer) {
		case "valid":
			return new ValidRealizability(name, k, runtime, invariants);

		case "falsifiable":
			return new InvalidRealizability(name, cex, runtime);

		case "unknown":
			return new UnknownRealizability(name, cex);

		default:
			throw new JKindException("Unknown property answer in XML file: " + answer);
		}
	}
	
	private double getRuntime(Node runtimeNode) {
		if (runtimeNode == null) {
			return 0;
		}
		return Double.parseDouble(runtimeNode.getTextContent());
	}
	
	private int getK(Node kNode) {
		if (kNode == null) {
			return 0;
		}
		return Integer.parseInt(kNode.getTextContent());
	}


	
	private String getAnswer(Node answerNode) {
		return answerNode.getTextContent();
	}

	private List<String> getInvariants(List<Element> invariantElements) {
		List<String> invariants = new ArrayList<>();
		for (Element invariantElement : invariantElements) {
			invariants.add(invariantElement.getTextContent());
		}
		return invariants;
	}
	
	private Counterexample getCounterexample(Element cexElement, int k) {
		if (cexElement == null) {
			return null;
		}

		Counterexample cex = new Counterexample(k);
		for (Element signalElement : getElements(cexElement, "Signal")) {
			cex.addSignal(getSignal(signalElement));
		}
		return cex;
	}

	private Signal<Value> getSignal(Element signalElement) {
		String name = signalElement.getAttribute("name");
		String type = signalElement.getAttribute("type");
		if (type.contains("subrange ")) {
			type = "int";
		}

		Signal<Value> signal = new Signal<>(name);
		for (Element valueElement : getElements(signalElement, "Value")) {
			int time = Integer.parseInt(valueElement.getAttribute("time"));
			signal.putValue(time, getValue(valueElement, type));
		}
		return signal;
	}

	private Value getValue(Element valueElement, String type) {
		Element intervalElement = getElement(valueElement, "Interval");
		if (intervalElement != null) {
			return getIntervalValue(intervalElement, type);
		}

		return Util.parseValue(type, valueElement.getTextContent());
	}

	private Interval getIntervalValue(Element intervalElement, String type) {
		String low = intervalElement.getAttribute("low");
		String high = intervalElement.getAttribute("high");
		NumericEndpoint lowEnd;
		NumericEndpoint highEnd;

		switch (type) {
		case "int":
			lowEnd = readIntEndpoint(low);
			highEnd = readIntEndpoint(high);
			break;

		case "real":
			lowEnd = readRealEndpoint(low);
			highEnd = readRealEndpoint(high);
			break;

		default:
			throw new JKindException("Unknown interval type in XML file: " + type);
		}

		return new NumericInterval(lowEnd, highEnd);
	}

	private IntEndpoint readIntEndpoint(String text) {
		switch (text) {
		case "inf":
			return IntEndpoint.POSITIVE_INFINITY;
		case "-inf":
			return IntEndpoint.NEGATIVE_INFINITY;
		default:
			IntegerValue iv = (IntegerValue) Util.parseValue("int", text);
			return new IntEndpoint(iv.value);
		}
	}

	private RealEndpoint readRealEndpoint(String text) {
		switch (text) {
		case "inf":
			return RealEndpoint.POSITIVE_INFINITY;
		case "-inf":
			return RealEndpoint.NEGATIVE_INFINITY;
		default:
			RealValue rv = (RealValue) Util.parseValue("real", text);
			return new RealEndpoint(rv.value);
		}
	}
	
	private Element getElement(Element element, String name) {
		return (Element) element.getElementsByTagName(name).item(0);
	}
	
	private List<Element> getElements(Element element, String name) {
		List<Element> elements = new ArrayList<>();
		NodeList nodeList = element.getElementsByTagName(name);
		for (int i = 0; i < nodeList.getLength(); i++) {
			elements.add((Element) nodeList.item(i));
		}
		return elements;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}
}
