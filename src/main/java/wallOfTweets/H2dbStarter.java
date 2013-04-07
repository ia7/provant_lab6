package wallOfTweets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class H2dbStarter  implements ServletContextListener {

	// This application uses an embedded relational database
	// through the "H2 Database Engine (www.h2database.com)
	// The database in question, "walldb", is stored in the directory WEB-INF/db
	// The application uses a single JDBC connection 
	// that is stored as an attribute of the "ServletContext", so that all the servlets
	// of the application will share it.
	// In the file WEB-INF/web.xml H2dbStarter is configured as a "listener" so
	// that its method contextInitalized is called when the application is launched

	private Connection conn;

	public void contextInitialized(ServletContextEvent servletContextEvent) {
		try {
			Class.forName(Parameters.getString("walldb.driver")); 
			ServletContext servContext = servletContextEvent.getServletContext();
			String dbPath = servContext.getRealPath("/")+Parameters.getString("walldb.path"); 
			conn = DriverManager.getConnection(Parameters.getString("walldb.uri")+dbPath, Parameters.getString("walldb.user"), Parameters.getString("walldb.password"));
			servContext.setAttribute("dbconnection", conn);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() {
        return conn;
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            Statement stat = conn.createStatement();
            stat.execute("SHUTDOWN");
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
       
    }

}
