package bobina.server;

import bobina.base.*;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;

/**
 * User: Admin
 * Date: 14.08.12
 * Time: 21:59
 */
public class ServerThread extends Thread
{
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Connection connectionDB;
    private String username;
    private int userID;

    public ServerThread( Socket socket ) throws IOException
    {
        this.socket = socket;
        reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        writer = new PrintWriter( new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) ), true );
        start();
    }

    public void run()
    {
        try
        {
            connectionDB = getConnection();

            userAuthentification();

            while( true )
            {
                serveQuery();
            }
        }
        catch( IOException e )
        {
            System.err.println( "IO Exception" );
            System.err.println( e.getMessage() );
        }
        catch( SQLException e )
        {
            System.err.println( "SQL Exception" );
            System.err.println( e.getMessage() );
        }
        catch( UserLoginException e )
        {
            //do nothing.
        }
        catch( UserExitException e )
        {
            //do nothing.
        }
        catch( LogicException e )
        {
            //do nothing.
        }
        finally
        {
            closeAll();
        }
    }

    private void serveQuery() throws IOException, SQLException, LogicException
    {
        try
        {
            Command command = new Command( reader.readLine().trim() );
            if( command.isSystemCommand() )
            {
                processSystemCommand( command );
            }
            else
            {
                processCommand( command );
            }
        }
        catch( SyntaxErrorException e )
        {
            sendResponseToClient( Server.CODE_ERROR_WRONG_SYNTAX, e.getMessage(), "correct syntax:",
                                  "operationCode [-parameterName1 parameterValue1 -parameterName2 parameterValue2 ...]" );
        }
    }

    private void processSystemCommand( Command command ) throws SQLException, LogicException
    {
        if( !command.isSystemCommand() )
        {
            return;
        }

        if( command.getCode() == Command.CODE_EXIT )
        {
            sendResponseToClient( Server.CODE_CONNECTION_CLOSED, "connection closed" );
            throw new UserExitException();
        }
        else if( command.getCode() == Command.CODE_GET_OPERATION_INFO )
        {
            sendOperationInfo( command );
        }
        else if( command.getCode() == Command.CODE_GET_AVAILABLE_OPERATIONS )
        {
            sendAvailableOperations();
        }
        else
        {
            sendResponseToClient( Server.CODE_ERROR_OPERATION_NOT_FOUND, "operation " + command.getCode() + " not found" );
        }
    }

    private void sendAvailableOperations() throws SQLException
    {
        Statement statement = connectionDB.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT id, title FROM operations WHERE id IN (SELECT operation_id FROM permissions WHERE user_id = " + userID + ");" );
        String[] messages = formArrayOfMessagesFromResultSet( resultSet );
        if( ( messages == null ) || ( messages.length == 0 ) )
        {
            messages = new String[]{ "you have not allowed operations" };
        }
        sendResponseToClient( Server.CODE_SUCCESS, messages );
        statement.close();
    }

    private String[] formArrayOfMessagesFromResultSet( ResultSet resultSet ) throws SQLException
    {
        LinkedList<String> messages = new LinkedList<String>();
        while( resultSet.next() )
        {
            String message = "";
            for( int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++ )
            {
                message += resultSet.getString( i ) + " ";
            }
            messages.add( message.trim() );
        }
        return Arrays.copyOf(messages.toArray(),messages.size(), String[].class);
    }

    private void sendOperationInfo( Command command ) throws SQLException
    {
        int commandID;
        try
        {
            commandID = Integer.parseInt( command.getParameter( "operationID" ) );
        }
        catch( NumberFormatException e )
        {
            sendResponseToClient( Server.CODE_ERROR_WRONG_SYNTAX, "wrong parameter operationID" );
            return;
        }

        Statement statement = connectionDB.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT id, title, description FROM operations WHERE id = " + commandID + ";" );
        String[] messages = formArrayOfMessagesFromResultSet( resultSet );
        if( ( messages == null ) || ( messages.length == 0 ) )
        {
            sendResponseToClient( Server.CODE_ERROR_OPERATION_NOT_FOUND,
                                  "operation " + commandID + " not found" );
        }
        else
        {
            sendResponseToClient( Server.CODE_SUCCESS, messages );
        }

        statement.close();
    }

    private void processCommand( Command command ) throws IOException, SQLException
    {
        Statement statement = connectionDB.createStatement();
        ResultSet resultSet = statement.executeQuery( "SELECT * FROM operations WHERE id = '" + command.getCode() + "';" );
        if( resultSet.next() )
        {
            if( hasUserPermissions( command.getCode() ) )
            {
                String templateQuery = resultSet.getString( "query" );
                try
                {
                    String query = command.getQuery(templateQuery);
                    executeQuery(query);
                }
                catch(NoParameterException e)
                {
                    sendResponseToClient( Server.CODE_ERROR_NO_PARAMETER, e.getMessage() );
                }
            }
            else
            {
                sendResponseToClient( Server.CODE_ERROR_NOT_PERMISSIONS,
                                      "you have not permissions to execute operation " + command.getCode() );
            }
        }
        else
        {
            sendResponseToClient( Server.CODE_ERROR_OPERATION_NOT_FOUND, "operation " + command.getCode() + " not found" );
        }
        statement.close();

    }

    private void executeQuery( String query ) throws SQLException
    {
        Statement statement = connectionDB.createStatement();

        if( query.startsWith( "SELECT" ) )
        {
            ResultSet resultSet = statement.executeQuery( query );
            String[] messages = formArrayOfMessagesFromResultSet( resultSet );
            if( ( messages == null ) || ( messages.length == 0 ) )
            {
                messages = new String[]{ "empty set" };
            }
            sendResponseToClient( Server.CODE_SUCCESS, messages );
        }
        else
        {
            int rowCount = statement.executeUpdate( query );
            sendResponseToClient( Server.CODE_SUCCESS, rowCount + " row(s) updated" );
        }

        statement.close();
    }

    private boolean hasUserPermissions( int operationID ) throws SQLException
    {
        Statement statement = connectionDB.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT * FROM permissions WHERE user_id = " + userID + " AND operation_id = " + operationID + ";" );
        return resultSet.next();
        //TODO: close statement
    }

    /**
     * Установка соединения с использованием свойств, заданных в файле database.properties
     *
     * @return Соединение с базой данных
     */
    private Connection getConnection() throws SQLException, IOException
    {
        Properties properties = new Properties();
        FileInputStream in = new FileInputStream( "database.properties" );
        properties.load( in );
        in.close();

        String drivers = properties.getProperty( "jdbc.drivers" );

        if( drivers != null ) System.setProperty( "jdbc.drivers", drivers );

        String url = properties.getProperty( "jdbc.url" );
        String username = properties.getProperty( "jdbc.username" );
        String password = properties.getProperty( "jdbc.password" );

        return DriverManager.getConnection( url, username, password );
    }

    private void userAuthentification() throws IOException, SQLException, LogicException
    {
        username = reader.readLine();
        Statement statement = connectionDB.createStatement();
        ResultSet resultSet = statement.executeQuery( "SELECT * FROM users WHERE username = '" + username + "';" );
        if( resultSet.next() )
        {
            userID = resultSet.getInt( "id" );
            sendResponseToClient( Server.CODE_SUCCESS );
        }
        else
        {
            sendResponseToClient( Server.CODE_ERROR_USER_NOT_FOUND, "user not found in a database" );
            statement.close();
            throw new UserLoginException();
        }
        statement.close();
    }

    private void closeAll()
    {
        closeReader();
        closeWriter();
        closeSocket();
        closeDBConnection();
    }

    private void closeReader()
    {
        try
        {
            reader.close();
        }
        catch( IOException e )
        {
            System.err.println( "Reader not closed" );
        }
    }

    private void closeWriter()
    {
        writer.close();
    }

    private void closeSocket()
    {
        try
        {
            socket.close();
        }
        catch( IOException e )
        {
            System.err.println( "Socket not closed" );
        }
    }

    private void closeDBConnection()
    {
        try
        {
            connectionDB.close();
        }
        catch( SQLException e )
        {
            System.err.println( "Connection to database not closed" );
        }
    }

    private void sendResponseToClient( int errorCode, String... messages )
    {
        writer.println( errorCode );
        for( String message : messages )
        {
            writer.println( message );
        }
        writer.println( Server.END_MESSAGE );
    }
}
