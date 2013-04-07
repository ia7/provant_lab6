
package wallOfTweets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class WoTBrowser extends HttpServlet {

	private static final long serialVersionUID = -7773488651465054757L;
	Locale currentLocale = Locale.ENGLISH;

	@SuppressWarnings("unchecked")
	@Override
	public void doGet (HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		res.setContentType ( "text/html" );
		PrintWriter  out = res.getWriter ( );
		SimpleDateFormat mySQLTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.FULL, currentLocale);
		DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, currentLocale);

		out.println("<html>");
		out.println("<head><title>Wall of Tweets 6</title>");
		out.println("<link href=\"wallstyle.css\" rel=\"stylesheet\" type=\"text/css\" />");
		out.println("<script type=\"application/javascript\" language=\"JavaScript\">");
		out.println("function likeHandler(tweetID) {");
		out.println("	var target = 'tweet_likes_'+tweetID;");
		out.println("	var uri = 'update?twid='+tweetID");
		out.println("	var butid = 'button_'+tweetID;");
		out.println("	var req = new XMLHttpRequest();");
		out.println("	req.open('GET', uri, true);");
		out.println("	req.onreadystatechange = function() {");
		out.println("	    if (req.readyState == 4 && req.status==200) {");
		out.println("	        document.getElementById(target).innerHTML = req.responseText;");
		out.println("	        document.getElementById(butid).disabled = true;");
		out.println("	    }");
		out.println("	};");
		out.println("	req.send(null);");
		out.println("}");
		out.println("</script>");
		out.println("</head>");
		out.println("<body class=\"wallbody\">");

		out.println("<div class =\"login\">");
		out.println("<form action = \"update\" method=\"post\">");
		out.println("username: <input name=\"login_username\" size=20>");
		out.println("password: <input name=\"login_password\" type=\"password\" size=20>");
		out.println("<input type=\"submit\" name=\"action\" value=\"login\">");
		out.println("</form></div>");
		
		out.println("<div class =\"heading\">");
		out.println("<h1 class=\"walltitle\">Wall of Tweets 6</h1>");
		out.println("<p><strong><a href=\"user_registration.html\">Click Here To Register</a></strong></p>");
		out.println("</div>");

		String currentDate = dateFormatter.format(new java.util.Date());

		HashSet<String> likedtweets = (HashSet<String>) req.getSession().getAttribute("likedTable");
		if (likedtweets == null){ 
			likedtweets = new HashSet<String>();
			req.getSession().setAttribute("likedTable",likedtweets);
		}	

		try {
			Connection con = (Connection) this.getServletContext().getAttribute("dbconnection");
			Statement stmt = con.createStatement();
			String query = "select t.*, u.usNAME from tweets t natural join users u order by twTIME desc";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				java.util.Date thetime = mySQLTimeStamp.parse(rs.getString("twTIME"), new ParsePosition(0));
				String messDate = dateFormatter.format(thetime); // messDate is the tweet date without time eg: Monday, October 22, 2012
				if (!currentDate.equals(messDate)) {
					out.println("<br><h3>...... " + messDate + "</h3>");
					currentDate = messDate;
				}
				out.println("<div class=\"wallitem\">");
				out.println("<div class=\"likes\">");
				String tweetid = rs.getString("twID");
				String target = "tweet_likes_"+tweetid;
				String butid = "button_"+tweetid;
				String butAction = (likedtweets.contains(tweetid)) ? "disabled" : "onclick=\"likeHandler('"+tweetid+"')\""; 
				out.println("<span class=\"numlikes\" id='"+target+"'>"+rs.getString("twLIKES")+"</span><br/><span class=\"plt\">people like this</span><br/><br/>");
				out.println("<button id=\""+butid+"\" "+butAction+">like</button><br/>");
				out.println("</div>");
				out.println("<div class=\"item\">");
				out.println("<h4><em>" + rs.getString("usNAME") + "</em> @ "+ timeFormatter.format(thetime) +"</h4>"); // time without date
				out.println("<p>" + rs.getString("twTEXT") + "</p>");
				out.println("</div>");
				out.println("</div>");
			}
			rs.close();
			stmt.close();
		}

		catch (SQLException ex ) {
			throw new ServletException(ex);
		}
		out.println ( "</body></html>" );
		return ;
	}

}

