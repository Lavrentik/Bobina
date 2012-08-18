package bobina.client;

import bobina.server.Server;
import bobina.server.UserLoginException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/** User: Admin
 * Date: 14.08.12
 * Time: 21:18
 */
public class Client
{
    private final String SERVER_NAME = "localhost";
    private final int PORT = 3333;

    private BufferedReader consoleInput = new BufferedReader( new InputStreamReader( System.in ) );
    private BufferedReader reader = null;
    private PrintWriter writer = null;

    public static void main( String[] args ) throws IOException
    {
        new Client();
    }

    Client() throws IOException
    {
        InetAddress address = InetAddress.getByName( SERVER_NAME );
        Socket socket = new Socket( address, PORT );

        reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        writer = new PrintWriter( new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) ), true );

        try
        {
            login();

            while( true )
            {
                process();
            }


        }
        catch( UserLoginException e )
        {
            //do nothing.
        }
        catch( ConnectionClosedException e )
        {
            //do nothing.
        }
        finally
        {
            socket.close();
        }
    }

    private void login() throws IOException, UserLoginException
    {
        System.out.print( "Login:  " );
        String user = consoleInput.readLine();
        writer.println( user );
        int resultCode = Integer.parseInt( reader.readLine() );
        printServerResponse();
        if( resultCode != Server.CODE_SUCCESS )
        {
            throw new UserLoginException();
        }
    }

    private void process() throws IOException, ConnectionClosedException
    {
        System.out.println( "Enter command:  " );
        String command = consoleInput.readLine();
        writer.println( command );
        int resultCode = Integer.parseInt( reader.readLine() );
        printServerResponse();
        if( resultCode == Server.CODE_CONNECTION_CLOSED )
        {
            throw new ConnectionClosedException();
        }
    }

    private void printServerResponse() throws IOException
    {
        String resultMessage = reader.readLine();
        while( !Server.END_MESSAGE.equals( resultMessage ) )
        {
            System.out.println( resultMessage );
            resultMessage = reader.readLine();
        }
    }
}
