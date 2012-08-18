package bobina.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Admin
 * Date: 16.08.12
 * Time: 1:10
 */
public class Command
{
    public static final int CODE_EXIT = -1;
    public static final int CODE_GET_OPERATION_INFO = -2;
    public static final int CODE_GET_AVAILABLE_OPERATIONS = -3;

    public static final String PARAMETER_PREFIX = "<@";
    public static final String PARAMETER_POSTFIX = "@>";

    private int code;
    private HashMap<String, String> parametersMap = new HashMap<String, String>();

    public Command( String str ) throws SyntaxErrorException
    {
        try
        {
            String[] tokens = str.split( " -" );
            code = Integer.parseInt( tokens[0] );

            for( int i = 1; i < tokens.length; i++ )
            {
                String parameterName = tokens[i].substring( 0, tokens[i].indexOf( " " ) ).trim();
                String parameterValue = tokens[i].substring( tokens[i].indexOf( " " ), tokens[i].length() ).trim();
                parametersMap.put( parameterName, parameterValue );
            }
        }
        catch( Exception e )
        {
            throw new SyntaxErrorException( "request has wrong format." );
        }
    }

    public int getCode()
    {
        return code;
    }

    public String getParameter( String parameterName )
    {
        return parametersMap.get( parameterName );
    }

    public boolean isSystemCommand()
    {
        return ( code < 0 );
    }

    /**
     * Метод формирует sql-запрос по заданному шаблону, подставляя все необходимые параметры
     *
     * @param templateQuery шаблон запроса, имена подставляемых параметров помещаются в "<@ @>". Пример "SELECT * FROM users WHERE username = <@username@>"
     * @return готовый к применению sql-запрос
     * @throws NoParameterException если не задан параметр, требуемый в запросе
     */
    public String getQuery( String templateQuery ) throws NoParameterException
    {
        Set<String> queryParameters = new HashSet<String>();
        int cur = templateQuery.indexOf( PARAMETER_PREFIX );
        while( cur != -1 )
        {
            int end = templateQuery.indexOf( PARAMETER_POSTFIX, cur );
            String parameterName = templateQuery.substring( cur + PARAMETER_PREFIX.length(), end );
            queryParameters.add( parameterName );
            cur = templateQuery.indexOf( PARAMETER_PREFIX, end );
        }
        if( parametersMap.keySet().containsAll( queryParameters ) )
        {
            return getUnSafeQuery( templateQuery );
        }
        else
        {
            throw new NoParameterException( "it is necessary to set all parameters: " + queryParameters );
        }
    }

    /**
     * Метод формирует sql-запрос по заданному шаблону, подставляя имеющиеся в текущем объекте Command параметры.
     * Не заданные параметры игнорируются
     *
     * @param templateQuery
     * @return готовый к применению sql-запрос. Не найденные параметры так и останутся в виде "<@parameterName@>" в запросе
     */
    public String getUnSafeQuery( String templateQuery )
    {
        String query = templateQuery;
        Set<String> keySet = parametersMap.keySet();
        for( String key : keySet )
        {
            query = query.replace( PARAMETER_PREFIX + key + PARAMETER_POSTFIX, parametersMap.get( key ) );
        }
        return query.trim();
    }
}
