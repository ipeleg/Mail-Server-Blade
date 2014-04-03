package com.mygreenbill.common;

import com.mygreenbill.JsonMessageHandler;
import com.mygreenbill.Exceptions.*;
import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jacob on 3/18/14.
 */
public class ConnectionManager
{
    private static ConnectionManager instance = null;
    private final Logger LOGGER = Logger.getLogger(ConnectionManager.class);

    private AtomicInteger jsonRequestID = new AtomicInteger(0);
    private ConcurrentHashMap<String, RequestJson> sentJsonRequestsMap;
    private int resendingSleepTime;
    private int maxNumberOfResendAttempts;
    private DatagramSocket socket;
    private Thread sender;
    private final int MAX_DATAGRAM_SIZE = 65508;
    private JsonMessageHandler jsonMessageHandler;

    private PoolProperties jdbcPoolProperties;
    private DataSource datasource;

    private int innerConnectionPort;
    private int databasePort;
    private String managmentBladeIp;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;
    private String databaseName;

    public static ConnectionManager getInstance() throws InitException
    {
        if (instance == null)
            instance = new ConnectionManager();
        return instance;
    }

    private ConnectionManager() throws InitException
    {
        sentJsonRequestsMap = new ConcurrentHashMap<String, RequestJson>();

        init();
        initConnectionPool();
    }

    private void initConnectionPool()
    {
        //todo yaki - move the hard coded configuration to file
        jdbcPoolProperties = new PoolProperties();

        jdbcPoolProperties.setUrl("jdbc:mysql://" + databaseHost + ":3306/" + databaseName);
        jdbcPoolProperties.setDriverClassName("com.mysql.jdbc.Driver");
        jdbcPoolProperties.setUsername(databaseUser);
        jdbcPoolProperties.setPassword(databasePassword);
        jdbcPoolProperties.setJmxEnabled(true);
        jdbcPoolProperties.setTestWhileIdle(false);
        jdbcPoolProperties.setTestOnBorrow(true);
        jdbcPoolProperties.setValidationQuery("SELECT 1");
        jdbcPoolProperties.setTestOnReturn(false);
        jdbcPoolProperties.setValidationInterval(30000);
        jdbcPoolProperties.setTimeBetweenEvictionRunsMillis(30000);
        jdbcPoolProperties.setMaxActive(100);
        jdbcPoolProperties.setInitialSize(10);
        jdbcPoolProperties.setMaxWait(10000);
        jdbcPoolProperties.setRemoveAbandonedTimeout(60);
        jdbcPoolProperties.setMinEvictableIdleTimeMillis(30000);
        jdbcPoolProperties.setMinIdle(10);
        jdbcPoolProperties.setLogAbandoned(true);
        jdbcPoolProperties.setRemoveAbandoned(true);
        jdbcPoolProperties.setJdbcInterceptors(
                "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
                        "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        datasource = new DataSource();
        datasource.setPoolProperties(jdbcPoolProperties);
    }

    private void init() throws InitException
    {
        try
        {
            ConfigurationManager configurationManager = ConfigurationManager.getInstance();

            // init the class members from the Configuration file
            this.innerConnectionPort = configurationManager.getValueWithDefault("blades_listen_port", 9980);
            this.managmentBladeIp = configurationManager.getProperty("management_blade_ip");
            this.databaseHost = configurationManager.getProperty("database_host");
            this.databaseUser = configurationManager.getProperty("database_user");
            this.databasePassword = configurationManager.getProperty("database_password");
            this.databaseName = configurationManager.getProperty("database_name");
            this.databasePort = Integer.parseInt(configurationManager.getProperty("database_port"));
            this.resendingSleepTime = Integer.parseInt(configurationManager.getProperty("sleep_time_before_resending_json_request"));
            this.maxNumberOfResendAttempts = configurationManager.getValueWithDefault("max_number_of_request_resend_attempts", 6);
            this.socket = new DatagramSocket();

            // start the sender thread
            sender = new Thread(new JsonRequestSender(resendingSleepTime));
            sender.start();
            LOGGER.info("Sender Thread has Started");

            Thread listeningThread = new Thread(new InnerCommunicationListening());
            listeningThread.start();
            LOGGER.info("Listening Thread has started");
            
            // Creating new JSON message handler
            jsonMessageHandler = new JsonMessageHandler();
        }
        catch (ConfigurationException e)
        {
            LOGGER.error("unable to init " + this.getClass().getSimpleName() + " error in configuration manager");
            throw new InitException("could not init the ConnectionManager Class", e);
        }
        catch (SocketException e)
        {
            LOGGER.error("unable to init " + this.getClass().getSimpleName() + " failed to init datagram", e);
            throw new InitException("could not init the ConnectionManager Class", e);
        }

        LOGGER.info("init process finished successfully");
    }


    public void sendToTrafficBlade(JSONObject jsonMessage)
    {
        if (jsonMessage == null)
        {
            LOGGER.info("Received null message to send to client... do nothing");
        }

        // create new JsonRequest Object and add it to the sentMap -> then interrupt the thread
        // RequestJson newRequestJson = new RequestJson(prepareJsonMessageToDeliver(jsonMessage), 0);

        RequestJson newRequestJson = new RequestJson(jsonMessage, 0);
        sentJsonRequestsMap.put(String.valueOf(this.jsonRequestID.get()), newRequestJson);

        sender.interrupt();
    }

    public Connection getDatabaseConnection() throws DatabaseException
    {
        Connection connection = null;
        try
        {
            connection = datasource.getConnection();
        }
        catch (SQLException e)
        {
            LOGGER.error("Unable to get new  connection from connection pool: " + e.getMessage(),e);
            throw new DatabaseException("Unable to get new connection from connection pool", e.getCause());
        }
        return connection;
    }

    public void closeDatabaseConnection(Connection toClose)
    {
        if (toClose != null)
        {
            try
            {
                toClose.close();
            }
            catch (SQLException ignore)
            {
            	
            }
        }
    }

    /**
     * Inner Class for sending messages
     */
    public class JsonRequestSender implements GreenBillClient
    {
        private int threadSleepTime;
        private final Logger LOGGER = Logger.getLogger(JsonRequestSender.class);

        public JsonRequestSender(int threadSleepTime)
        {
            this.threadSleepTime = threadSleepTime;
        }

        @Override
        public void run()
        {

            while (true)
            {
                if  (sentJsonRequestsMap.size() == 0)
                {
                    try
                    {
                        this.LOGGER.info("Nothing to Resend Sleeping for " + threadSleepTime + "...");
                        Thread.sleep(threadSleepTime);
                    }
                    catch (InterruptedException e)
                    {
                        this.LOGGER.info("JsonRequestSender sleep was interrupted: " + e.getMessage());
                    }
                    continue;
                }

                this.LOGGER.info(sentJsonRequestsMap.size() + " Requests to send, resending them..");
                Iterator<Map.Entry<String, RequestJson>> iterator = sentJsonRequestsMap.entrySet().iterator();
                
                while (iterator.hasNext())
                {
                    Map.Entry<String, RequestJson> pairs = (Map.Entry<String, RequestJson>)iterator.next();
                    RequestJson toResend = (RequestJson) pairs.getValue();
                    if (toResend.getNumberOfResendingAttempts() < maxNumberOfResendAttempts)
                    {
                        sendMessage(toResend);
                        toResend.incrementResendAttempt();
                        iterator.remove();
                    }
                    else
                    {
                        this.LOGGER.info("No more attempts to resend the Json request! already failed: " + toResend.getNumberOfResendingAttempts()
                                        + " Disposing the request: " + toResend.getRequest().toString());
                        iterator.remove();
                    }
                }

                //after all is sent... sleep for the sleep time
                this.LOGGER.info("All Requests sent.. sleeping for: " + threadSleepTime + " ms");
                try
                {
                    Thread.sleep(threadSleepTime);
                }
                catch (InterruptedException e)
                {
                    this.LOGGER.info("JsonRequestSender sleep was interrupted: " + e.getMessage());
                }
            }
        }

        @Override
        public void sendMessage(RequestJson message)
        {
            try
            {
                InetAddress address = InetAddress.getByName(managmentBladeIp);
                byte[] buffer = message.toString().getBytes();
                if (buffer.length < MAX_DATAGRAM_SIZE)
                {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, innerConnectionPort);
                    socket.send(packet);
                }
                else
                {
                    LOGGER.info("Message size " + buffer.length +" in bigger than the max datagram size: " + MAX_DATAGRAM_SIZE);
                }

            }
            catch (UnknownHostException e)
            {
                LOGGER.error("Error with host name: " + managmentBladeIp, e);
                return;
            }
            catch (IOException e)
            {
                LOGGER.info("Unable to send the request: " + message, e);
                return;
            }
        }
    }

    /**
     * Inner Class for receiving messages
     */
    public class InnerCommunicationListening implements GreenBillServer
    {
        private DatagramSocket datagramSocket;
        private final Logger LOGGER = Logger.getLogger(InnerCommunicationListening.class);

        public InnerCommunicationListening() throws InitException
        {
            try
            {
                datagramSocket = new DatagramSocket(innerConnectionPort);
            }
            catch (SocketException e)
            {
                this.LOGGER.error("Unable to start the InnerCommunicationListening thread ", e);
                throw new InitException("Unable to start the InnerCommunicationListening thread");
            }
        }

        @Override
        public void run()
        {
           listen();
        }

        @Override
        public void listen()
        {
            while (true)
            {
                byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, MAX_DATAGRAM_SIZE);
                try
                {
                    // read the datagram;
                    datagramSocket.receive(packet);
                    // the data has read into buffer
                    buffer = packet.getData();
                    String str = new String(buffer, "UTF-8");
                    JSONObject ob = new JSONObject(str);
                    this.LOGGER.info("New message received, composed it into JSON: " + ob.toString());
                    jsonMessageHandler.processJson(ob);
                }
                catch (IOException e)
                {
                    this.LOGGER.error(e);
                }
                catch (JSONException e)
                {
                    this.LOGGER.error(e);
                }
            }

        }
    }
}
