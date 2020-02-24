package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Map;

import org.slf4j.event.Level;

import jrds.ConnectedProbe;
import jrds.Probe;
import jrds.Util;
import jrds.factories.ProbeBean;
import jrds.starter.Resolver;
import jrds.starter.Starter;

/**
 * A generic probe to collect an HTTP service default generic : port to provide
 * a default port to collect file to provide a specific file to collect
 * 
 * Implementation should implement the parseStream method
 *
 * @author Fabrice Bacchella
 */
@ProbeBean({ "port", "file", "url", "urlhost", "scheme", "login", "password" })
public abstract class HttpProbe<KeyType> extends Probe<KeyType, Number> implements UrlProbe, ConnectedProbe {
    protected URL url = null;
    protected String urlhost = null;
    protected int port = -1;
    protected String file = "/";
    protected String scheme = null;
    protected String login = null;
    protected String password = null;
    private Starter resolver = null;
    protected String connectionName = null;

    public Boolean configure(URL url) {
        this.url = url;
        return finishConfigure(null);
    }

    public Boolean configure(Integer port, String file) {
        this.port = port;
        this.file = file;
        return finishConfigure(null);
    }

    public Boolean configure(Integer port) {
        this.port = port;
        return finishConfigure(null);
    }

    public Boolean configure(String file) {
        this.file = file;
        return finishConfigure(null);
    }

    public Boolean configure(List<Object> argslist) {
        return finishConfigure(argslist);
    }

    public Boolean configure(String file, List<Object> argslist) {
        this.file = file;
        return finishConfigure(argslist);
    }

    public Boolean configure(Integer port, List<Object> argslist) {
        this.port = port;
        return finishConfigure(argslist);
    }

    public Boolean configure(URL url, List<Object> argslist) {
        this.url = url;
        return finishConfigure(argslist);
    }

    public Boolean configure(Integer port, String file, List<Object> argslist) {
        this.port = port;
        this.file = file;
        return finishConfigure(argslist);
    }

    public Boolean configure() {
        return finishConfigure(null);
    }

    protected boolean finishConfigure(List<Object> argslist) {
        if(getConnectionName() == null) {
            if(url == null) {
                if(port <= 0 && (scheme == null || scheme.isEmpty())) {
                    scheme = "http";
                } else if(scheme == null || scheme.isEmpty()) {
                    if(port == 443) {
                        scheme = "https";
                    } else {
                        scheme = "http";
                    }
                }
                // Check if authentication elements were given, and construct the
                // authentication part if needed
                String userInfo = "";
                try {
                    if(login != null) {
                        userInfo = URLEncoder.encode(login, "UTF-8");
                    }
                    if(password != null) {
                        userInfo = userInfo + ":" + URLEncoder.encode(password, "UTF-8");
                    }
                    if(!userInfo.isEmpty()) {
                        userInfo += '@';
                    }
                } catch (UnsupportedEncodingException e1) {
                    // never reached catch
                }
                String portString = port < 0 ? "" : ":" + Integer.toString(port);
                if(urlhost == null) {
                    urlhost = getHost().getDnsName();
                }
                String urlString;
                if(argslist != null) {
                    try {
                        urlString = String.format(scheme + "://" + userInfo + urlhost + portString + file, argslist.toArray());
                        urlString = Util.parseTemplate(urlString, getHost(), argslist, this);
                    } catch (IllegalFormatConversionException e) {
                        log(Level.ERROR, "Illegal format string: %s://%s%s:%d%s, args %d", scheme, userInfo, urlhost, portString, file, argslist.size());
                        return false;
                    }
                } else {
                    urlString = Util.parseTemplate(scheme + "://" + userInfo + urlhost + portString + file, this, getHost());
                }
                try {
                    url = new URL(urlString);
                } catch (MalformedURLException e) {
                    log(Level.ERROR, e, "URL '%s:/%s/%s:%s%s' is invalid", scheme, userInfo, urlhost, portString, file);
                    return false;
                }
            }
            if("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                resolver = getParent().registerStarter(new Resolver(url.getHost()));
            }
            log(Level.DEBUG, "URL to collect is %s", getUrl());
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#isCollectRunning()
     */
    @Override
    public boolean isCollectRunning() {
        if(resolver == null || !resolver.isStarted())
            return false;
        return super.isCollectRunning();
    }

    /**
     * @param stream A stream collected from the http source
     * @return a map of collected value
     */
    protected abstract Map<KeyType, Number> parseStream(InputStream stream);

    /**
     * A utility method that transform the input stream to a List of lines
     * 
     * @param stream
     * @return
     */
    public List<String> parseStreamToLines(InputStream stream) {
        List<String> lines = java.util.Collections.emptyList();
        log(Level.DEBUG, "Getting %s", getUrl());
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            lines = new ArrayList<String>();
            String lastLine;
            while ((lastLine = in.readLine()) != null)
                lines.add(lastLine);
            in.close();
        } catch (IOException e) {
            log(Level.ERROR, e, "Unable to read url %s because: %s", getUrl(), e);
        }
        return lines;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.aol.jrds.Probe#getNewSampleValues()
     */
    public Map<KeyType, Number> getNewSampleValues() {
        log(Level.DEBUG, "Getting %s", getUrl());
        URLConnection cnx;
        try {
            cnx = getUrl().openConnection();
            cnx.setConnectTimeout(getTimeout() * 1000);
            cnx.setReadTimeout(getTimeout() * 1000);
            cnx.connect();
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection to %s failed: %s", getUrl(), e);
            return null;
        }
        try {
            InputStream is = cnx.getInputStream();
            Map<KeyType, Number> vars = parseStream(is);
            is.close();
            return vars;
        } catch (ConnectException e) {
            log(Level.ERROR, e, "Connection refused to %s", getUrl());
        } catch (IOException e) {
            // Clean http connection error management
            // see
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
            try {
                byte[] buffer = new byte[4096];
                int respCode = ((HttpURLConnection) cnx).getResponseCode();
                log(Level.ERROR, e, "Unable to read url %s because: %s, http error code: %d", getUrl(), e, respCode);
                InputStream es = ((HttpURLConnection) cnx).getErrorStream();
                // read the response body
                while (es.read(buffer) > 0) {
                }
                // close the error stream
                es.close();
            } catch (IOException ex) {
                log(Level.ERROR, ex, "Unable to recover from error in url %s because %s", getUrl(), ex);
            }
        }

        return null;
    }

    /**
     * @return Returns the url.
     */
    public String getUrlAsString() {
        return getUrl().toString();
    }

    public Integer getPort() {
        return port;
    }

    /**
     * @return Returns the url.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @param url The url to set.
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public String getSourceType() {
        return "HTTP";
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * @return the path
     */
    public String getFile() {
        return file;
    }

    /**
     * @param path the path to set
     */
    public void setFile(String path) {
        this.file = path;
    }

    /**
     * @return the urlhost
     */
    public String getUrlhost() {
        return urlhost;
    }

    /**
     * @param urlhost the urlhost to set
     */
    public void setUrlhost(String urlhost) {
        this.urlhost = urlhost;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getConnectionName() {
        return connectionName ;
    }

    @Override
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

}
