package jkind.api.xml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Kind2WebInputStream extends InputStream {
	public static void main(String[] args) throws Exception {
		URI uri = new URI("http://kind.cs.uiowa.edu:8181/");
		try (LineInputStream lines = new LineInputStream(new Kind2WebInputStream(uri, getLustre()))) {
			String line;
			while ((line = lines.readLine()) != null) {
				System.out.print(line);
			}
		}
	}

	private static String getLustre() throws IOException {
		try (FileReader reader = new FileReader("c:/desktop/test.lus")) {
			StringBuilder result = new StringBuilder();
			int i;
			while ((i = reader.read()) != -1) {
				result.append((char) i);
			}
			return result.toString();
		}
	}

	private static final int POLL_INTERVAL = 1000;
	private final URI baseUri;
	private final String lustre;
	private String jobId;
	private String buffer;
	private int index;
	private boolean done;

	public Kind2WebInputStream(URI baseUri, String lustre) {
		this.baseUri = baseUri;
		this.lustre = lustre;
	}

	@Override
	public int read() throws IOException {
		if (done) {
			return -1;
		}

		if (jobId == null) {
			submitJob();
			buffer = "";
			index = 0;
		}

		while (index >= buffer.length()) {
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
				return -1;
			}
			buffer = retrieveJob();
			if (buffer == null) {
				done = true;
				return -1;
			}
			index = 0;
		}

		return buffer.charAt(index++);
	}

	private void submitJob() throws IOException {
		URL url = baseUri.resolve("submitjob").toURL();
		URLConnection conn = createRequest(lustre, url);
		conn.connect();
		jobId = getJobId(conn.getInputStream());
		conn.getInputStream().close();
	}

	private URLConnection createRequest(String lustre, URL url) throws IOException {
		// http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests/2793153#2793153
		URLConnection conn = url.openConnection();
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		// Just generate some unique random value.
		String boundary = Long.toHexString(System.currentTimeMillis());
		String CRLF = "\r\n"; // Line separator required by multipart/form-data.
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(),
				"UTF-8"), true)) {
			// kind param
			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"kind\"").append(CRLF);
			writer.append(CRLF).append("kind2").append(CRLF).flush();

			// arg param
			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"arg\"").append(CRLF);
			writer.append(CRLF).append("-xml").append(CRLF).flush();

			// file param
			writer.append("--" + boundary).append(CRLF);
			writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"upload.lus\"")
					.append(CRLF);
			writer.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
			writer.append(CRLF).flush();
			writer.append(lustre);
			writer.append(CRLF).flush();
			// CRLF is important! It indicates end of boundary.

			// End of multipart/form-data.
			writer.append("--" + boundary + "--").append(CRLF);
		}
		return conn;
	}

	private String retrieveJob() throws IOException {
		StringBuilder content = new StringBuilder();
		URL url = baseUri.resolve("retrievejob/").resolve(jobId).toURL();
		URLConnection conn = url.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			content.append(line).append("\n");
		}
		conn.getInputStream().close();

		Pattern pattern = Pattern.compile(".*<para>(.*)</para>.*", Pattern.DOTALL);
		Matcher match = pattern.matcher(content.toString());
		if (!match.matches()) {
			throw new IllegalArgumentException(content.toString());
		}
		String body = match.group(1);

		if (body.startsWith("<Jobstatus msg=\"completed\">")) {
			return null;
		} else {
			return body;
		}
	}

	private String getJobId(InputStream is) throws IOException {
		Pattern pattern = Pattern.compile(".*jobid=\"(.*?)\".*");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = reader.readLine()) != null) {
			Matcher match = pattern.matcher(line);
			if (match.matches()) {
				return match.group(1);
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		if (jobId != null) {
			cancelJob();
		}
	}

	private void cancelJob() throws IOException {
		URLConnection conn = baseUri.resolve("canceljob/").resolve(jobId).toURL().openConnection();
		conn.getInputStream().close();
	}
}
