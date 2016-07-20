package org.openas2.app;

import java.io.BufferedWriter;
import java.util.List;

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.XMLSession;
import org.openas2.cmd.CommandManager;
import org.openas2.cmd.CommandRegistry;
import org.openas2.cmd.processor.BaseCommandProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * original author unknown
 * 
 * in this release added ability to have multiple command processors
 * @author joseph mcverry
 *
 */
public class OpenAS2Server {
	protected BufferedWriter sysOut;
	private static final Logger logger = LoggerFactory.getLogger(OpenAS2Server.class);
	
	public static void main(String[] args) {
		OpenAS2Server server = new OpenAS2Server();
		server.start(args);
	}

	public void start(String[] args) {
		BaseCommandProcessor cmd = null;
		XMLSession session = null;
		int exitStatus = 0;

		try {
		    
			logger.trace("=== TRACE LEVEL ===");
			logger.debug("=== DEBUG LEVEL ===");
			logger.info("=== INFO LEVEL ===");
			logger.warn("=== WARN LEVEL ===");
			logger.error("=== ERROR LEVEL ===");
			logger.info(Session.TITLE + "\r\nStarting Server...");

			// create the OpenAS2 Session object
			// this is used by all other objects to access global configs and functionality
			logger.info("Loading configuration...");

			if (args.length > 0) {
				session = new XMLSession(args[0]);
			} else {
				logger.error("Usage:\r\n");
				logger.error("java org.openas2.app.OpenAS2Server <configuration file>");
				throw new Exception("Missing configuration file");
			}
			// create a command processor

			// get a registry of Command objects, and add Commands for the Session
			logger.info("Registering Session to Command Processor...");

			CommandRegistry reg = session.getCommandRegistry();

			// start the active processor modules
			logger.info("Starting Active Modules...");
			session.getProcessor().startActiveModules();

			// enter the command processing loop
			logger.info("OpenAS2 V" + Session.VERSION + " Started");

			
			logger.info("- OpenAS2 Started - V" + Session.VERSION);
			
			CommandManager cmdMgr = session.getCommandManager();
			List<BaseCommandProcessor> processors = cmdMgr.getProcessors();
			for (int i = 0; i < processors.size(); i++) {
				logger.info("Loading Command Processor..." + processors.toString());
				cmd = (BaseCommandProcessor) processors.get(i);
				cmd.init();
				cmd.addCommands(reg);
				cmd.start();
			}
			breakOut : while (true) {
				for (int i = 0; i < processors.size(); i++) {
					cmd = (BaseCommandProcessor) processors.get(i);
					if (cmd.isTerminated())
						break breakOut;
					Thread.sleep(100); 
				}
			}
			logger.info("- OpenAS2 Stopped -");
		} catch (Exception e) {
			exitStatus = -1;
			logger.error("Exception happend ", e);
		} catch (Error err) {
			exitStatus = -1;
			logger.error("Fatal Error happend ", err);
		} finally {

			if (session != null) {
				try {
					session.getProcessor().stopActiveModules();
				} catch (OpenAS2Exception same) {
					same.terminate();
				}
			}

			if (cmd != null) {
				try {
					cmd.deInit();
				} catch (OpenAS2Exception cdie) {
					cdie.terminate();
				}
			}

			logger.info("OpenAS2 has shut down\r\n");

			System.exit(exitStatus);
		}
	}

//	public void write(String msg) {
//		if (sysOut == null) {
//			sysOut = new BufferedWriter(new OutputStreamWriter(System.out));
//		}
//
//		try {
//			sysOut.write(msg);
//			sysOut.flush();
//		} catch (java.io.IOException e) {
//			e.printStackTrace();
//		}
//	}
}
