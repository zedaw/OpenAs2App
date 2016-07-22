package org.openas2.processor.receiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.message.InvalidMessageException;
import org.openas2.message.Message;
import org.openas2.params.CompositeParameters;
import org.openas2.params.DateParameters;
import org.openas2.params.InvalidParameterException;
import org.openas2.params.MessageParameters;
import org.openas2.util.IOUtilOld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class NetModule extends BaseReceiverModule {

    public static final String PARAM_ADDRESS = "address";

    public static final String PARAM_PORT = "port";

    public static final String PARAM_PROTOCOL = "protocol";

    public static final String PARAM_SSL_KEYSTORE = "ssl_keystore";

    public static final String PARAM_SSL_KEYSTORE_PASSWORD = "ssl_keystore_password";

    public static final String PARAM_SSL_PROTOCOL = "ssl_protocol";

    public static final String PARAM_ERROR_DIRECTORY = "errordir";

    public static final String PARAM_ERRORS = "errors";

    public static final String DEFAULT_ERRORS = "$date.yyyyMMddhhmmss$";

    private HTTPServerThread mainThread;

    private static final Logger logger = LoggerFactory.getLogger(NetModule.class);

    public void doStart() throws OpenAS2Exception {
        try {
            mainThread = new HTTPServerThread(this, getParameter(PARAM_ADDRESS, false), getParameterInt(PARAM_PORT, true));
            mainThread.start();
        } catch (IOException ioe) {
            String host = getParameter(PARAM_ADDRESS, false);
            if (host == null || host.length() < 1)
                host = "localhost";
            logger.error("Error in HTTP connection starting server thread on host::port: " + host + "::" + getParameterInt(PARAM_PORT, true), ioe);
            throw new WrappedException(ioe);
        }
    }

    public void doStop() throws OpenAS2Exception {
        if (mainThread != null) {
            mainThread.terminate();
            mainThread = null;
        }
    }

    public void init(Session session, Map<String, String> options) throws OpenAS2Exception {
        super.init(session, options);
        getParameter(PARAM_PORT, true);
        // Override the password if it was passed as a system property
        String pwd = System.getProperty("org.openas2.ssl.Password");
        if (pwd != null) {
            setParameter(PARAM_SSL_KEYSTORE_PASSWORD, pwd);;
        }

    }

    protected abstract NetModuleHandler getHandler();

    protected void handleError(Message msg, OpenAS2Exception oae) {
        oae.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
        oae.terminate();

        try {
            CompositeParameters params = new CompositeParameters(false).add("date", new DateParameters()).add("msg", new MessageParameters(msg));

            String name = params.format(getParameter(PARAM_ERRORS, DEFAULT_ERRORS));
            String directory = getParameter(PARAM_ERROR_DIRECTORY, true);

            File msgFile = IOUtilOld.getUnique(IOUtilOld.getDirectoryFile(directory), IOUtilOld.cleanFilename(name));
            String msgText = msg.toString();
            FileOutputStream fOut = new FileOutputStream(msgFile);

            fOut.write(msgText.getBytes());
            fOut.close();

            // make sure an error of this event is logged
            InvalidMessageException im = new InvalidMessageException("Stored invalid message to " + msgFile.getAbsolutePath());
            im.terminate();
        } catch (OpenAS2Exception oae2) {
            oae2.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            oae2.terminate();
        } catch (IOException ioe) {
            WrappedException we = new WrappedException(ioe);
            we.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            we.terminate();
        }
    }

    protected class ConnectionThread extends Thread {

        private NetModule owner;

        private Socket socket;

        public ConnectionThread(NetModule owner, Socket socket) {
            super();
            this.owner = owner;
            this.socket = socket;
            start();
        }

        public void setOwner(NetModule owner) {
            this.owner = owner;
        }

        public NetModule getOwner() {
            return owner;
        }

        public Socket getSocket() {
            return socket;
        }

        public void run() {
            Socket s = getSocket();

            getOwner().getHandler().handle(getOwner(), s);

            try {
                s.close();
            } catch (IOException sce) {
                new WrappedException(sce).terminate();
            }
        }
    }

    protected class HTTPServerThread extends Thread {

        private NetModule owner;

        private ServerSocket socket;

        private boolean terminated;

        public HTTPServerThread(NetModule owner, String address, int port) throws IOException {
            super();
            this.owner = owner;
            String protocol = "http";
            String sslProtocol = "TLS";
            try {
                protocol = owner.getParameter(PARAM_PROTOCOL, "http");
                sslProtocol = owner.getParameter(PARAM_SSL_PROTOCOL, "TLS");
            } catch (InvalidParameterException e) {
                // Do nothing
            }
            if ("https".equalsIgnoreCase(protocol)) {
                String ksName;
                char[] ksPass;
                try {
                    ksName = owner.getParameter(PARAM_SSL_KEYSTORE, true);
                    ksPass = owner.getParameter(PARAM_SSL_KEYSTORE_PASSWORD, true).toCharArray();
                } catch (InvalidParameterException e) {
                    logger.error("Required SSL parameter missing.", e);
                    throw new IOException("Failed to retrieve required SSL parameters. Check config XML");
                }
                KeyStore ks;
                try {
                    ks = KeyStore.getInstance("JKS");
                } catch (KeyStoreException e) {
                    logger.error("Failed to initialise SSL keystore.", e);
                    throw new IOException("Error initialising SSL keystore");
                }
                try {
                    ks.load(new FileInputStream(ksName), ksPass);
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Failed to load keystore: " + ksName, e);
                    throw new IOException("Error loading SSL keystore");
                } catch (CertificateException e) {
                    logger.error("Failed to load SSL certificate: " + ksName, e);
                    throw new IOException("Error loading SSL certificate");
                }
                KeyManagerFactory kmf;
                try {
                    kmf = KeyManagerFactory.getInstance("SunX509");
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Failed to create key manager instance", e);
                    throw new IOException("Error creating SSL key manager instance");
                }
                try {
                    kmf.init(ks, ksPass);
                } catch (Exception e) {
                    logger.error("Failed to initialise key manager instance", e);
                    throw new IOException("Error initialising SSL key manager instance");
                }
                // setup the trust manager factory
                TrustManagerFactory tmf;
                try {
                    tmf = TrustManagerFactory.getInstance("SunX509");
                    tmf.init(ks);
                } catch (Exception e1) {
                    logger.error("Failed to create trust manager instance", e1);
                    throw new IOException("Error creating SSL trust manager instance");
                }
                SSLContext sc;
                try {
                    sc = SSLContext.getInstance(sslProtocol);
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Failed to create SSL context instance", e);
                    throw new IOException("Error creating SSL context instance");
                }
                try {
                    sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                } catch (KeyManagementException e) {
                    logger.error("Failed to initialise SSL context instance", e);
                    throw new IOException("Error initialising SSL context instance");
                }
                SSLServerSocketFactory ssf = sc.getServerSocketFactory();
                if (address != null) {
                    socket = (SSLServerSocket) ssf.createServerSocket(port, 0, InetAddress.getByName(address));
                } else
                    socket = (SSLServerSocket) ssf.createServerSocket(port);
            } else {
                socket = new ServerSocket();
                if (address != null) {
                    socket.bind(new InetSocketAddress(address, port));
                } else {
                    socket.bind(new InetSocketAddress(port));
                }
            }
        }

        public void setOwner(NetModule owner) {
            this.owner = owner;
        }

        public NetModule getOwner() {
            return owner;
        }

        public ServerSocket getSocket() {
            return socket;
        }

        public void setTerminated(boolean terminated) {
            this.terminated = terminated;

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    owner.forceStop(e);
                }
            }
        }

        public boolean isTerminated() {
            return terminated;
        }

        public void run() {
            while (!isTerminated()) {
                try {
                    Socket conn = socket.accept();
                    conn.setSoLinger(true, 60);
                    new ConnectionThread(getOwner(), conn);
                } catch (IOException e) {
                    if (!isTerminated()) {
                        owner.forceStop(e);
                    }
                }
            }

            logger.info("exited");
        }


        public void terminate() {
            setTerminated(true);
        }
    }
}
