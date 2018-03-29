package td.framework.boot.autoconfigure.base.listener;

import java.io.File;

import org.springframework.boot.system.EmbeddedServerPortFileWriter;

public class ApplicationServerPortListener extends EmbeddedServerPortFileWriter {

	public ApplicationServerPortListener() {
		this(new File(System.getProperty("application.name") + ".port"));
	}

	public ApplicationServerPortListener(File file) {
		super(file);
	}

	public ApplicationServerPortListener(String filename) {
		super(filename);
	}

}
