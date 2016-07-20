package org.openas2;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 ;


public class OpenAS2Exception extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String SOURCE_MESSAGE = "message";
    public static final String SOURCE_FILE = "file";
    private Map<String,Object> sources;
	private static final Logger logger = LoggerFactory.getLogger(OpenAS2Exception.class);

    public OpenAS2Exception() {
        log(false);
    }

    public OpenAS2Exception(String msg) {
        super(msg);
    }

    public OpenAS2Exception(String msg, Throwable cause) {
        super(msg, cause);
    }

    public OpenAS2Exception(Throwable cause) {
        super(cause);
    }

    public Object getSource(String id) {
        Map<String,Object> sources = getSources();

        return sources.get(id);
    }

    public void setSources(Map<String,Object> sources) {
        this.sources = sources;
    }

    public Map<String,Object> getSources() {
        if (sources == null) {
            sources = new HashMap<String,Object>();
        }

        return sources;
    }

    public void addSource(String id, Object source) {
        Map<String,Object> sources = getSources();
        sources.put(id, source);
        
    }

    public void terminate() {
        log(true);
    }

    protected void log(boolean terminated) {
        StringBuilder builder = new StringBuilder("Error occurred:: \n");
        if (getSources().containsKey(SOURCE_MESSAGE)) {
            builder.append("[[MESSAGE]=").append(getSource(SOURCE_MESSAGE)).append("]\n");
        }
        if (getSources().containsKey(SOURCE_FILE)) {
            builder.append("[[FILE]=").append(getSource(SOURCE_FILE)).append("]\n");
        }
    	logger.error(builder.toString(), this);
    }
}
