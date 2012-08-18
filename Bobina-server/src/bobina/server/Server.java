package bobina.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * User: Admin
 * Date: 14.08.12
 * Time: 21:16
 */
public class Server
{

    public static final int PORT = 3333;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_ERROR_USER_NOT_FOUND = 1;
    public static final int CODE_ERROR_OPERATION_NOT_FOUND = 2;
    public static final int CODE_ERROR_NOT_PERMISSIONS = 3;
    public static final int CODE_CONNECTION_CLOSED = 4;
    public static final int CODE_ERROR_NO_PARAMETER = 5;
    public static final int CODE_ERROR_WRONG_SYNTAX = 6;
    public static final String END_MESSAGE = "END.";

    public static void main( String[] args ) throws IOException
    {
        ServerSocket serverSocket = new ServerSocket( PORT );
        System.out.println( "Server Started" );
        try
        {
            while( true )
            {
                Socket socket = serverSocket.accept();
                try
                {
                    new ServerThread( socket );
                }
                catch( IOException e )
                {
                    socket.close();
                }
            }
        }
        finally
        {
            serverSocket.close();
            System.out.println( "Server Shut Down" );
        }
    }
}
