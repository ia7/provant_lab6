
package wallOfTweets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;


public class WoTUpdater extends HttpServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 5545093656717300953L;
	private Connection dbConnection = null;

	@Override
	public void doPost (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {

		boolean isOK = false;
		Vector<String> errorList = new Vector<String>();
		try {
			dbConnection = (Connection) this.getServletContext().getAttribute("dbconnection");
			String action = req.getParameter("action");
			if (action != null)
			{
				if (action.equals("Tweet!"))
					isOK = insert(req, errorList);
				else if (action.equals("login"))
					isOK = login(req, errorList);
				else if (action.equals("Sign Up")) {
					isOK = register(req, errorList);
					req.setAttribute("goBack", "user_registration.html");
				}
				else if (action.equals("logout"))
					isOK = logout(req);
				else if (action.equals("delete"))
					errorList.addElement("Tweet deletion not yet implemented");
			}
			else errorList.addElement("Unknown action");
		}
		catch (SQLException ex ) {
			while (ex != null) {
				errorList.addElement("SQL Error:   " + ex.getMessage ());
				errorList.addElement("SQL Error Code:   " + ex.getErrorCode());
				ex = ex.getNextException();
			}
		}
		if (isOK)
			res.sendRedirect("wall");
		else {

			req.setAttribute("theList", errorList);
			RequestDispatcher dispatch = req.getRequestDispatcher("error.vm");
			dispatch.forward(req, res);
		}

	}


	private boolean insert(HttpServletRequest req, Vector<String>  out)
	throws SQLException
	{

		String content = req.getParameter("message_body");
		Integer userID = (Integer) req.getSession().getAttribute("loggedUserID");
		boolean insertionOK = false;

		Boolean paramsOK = true;

		if (userID == null) { out.addElement("You must login first"); paramsOK = false; }
		if (content.equals(""))  { out.addElement("Content cannot be empty"); paramsOK = false;}

		if (paramsOK)
		{

			PreparedStatement stmt = dbConnection.prepareStatement("insert into tweets (usID, twTEXT) values (?, ?)");
			stmt.setInt(1, userID.intValue());
			stmt.setString(2, content);
			stmt.executeUpdate();
			stmt.close();
			insertionOK = true;
		}
		return insertionOK;
	}



	private boolean login(HttpServletRequest req, Vector<String>  out)
	throws SQLException
	{
		String user = req.getParameter("login_username");
		String pwd = req.getParameter("login_password");
		boolean logged = false;

		String  queryString = "select usID from users where usNAME = ? and usPASSW = ?";
		PreparedStatement stmt = dbConnection.prepareStatement(queryString);
		stmt.setString(1, user);
		stmt.setString(2, MD5Encoder.encode(pwd));
		ResultSet rs = stmt.executeQuery();
		if (rs.first())
		{
			req.getSession().setAttribute("loggedUserNAME",user);
			req.getSession().setAttribute("loggedUserID",new Integer(rs.getInt(1)));
			logged = true;
		}
		else
			out.addElement("Incorrect user and/or password");

		rs.close();
		stmt.close();

		return logged;
	}

	private boolean register(HttpServletRequest req, Vector<String>  out)
	throws SQLException
	{
		String user = req.getParameter("reg_username");
		String pwd = req.getParameter("reg_password");
		boolean registered = false;

		Boolean paramsOK = true;

		if (user.equals("")) { out.addElement("Username cannot be empty"); paramsOK = false;}
		if (pwd.equals(""))  { out.addElement("Password cannot be empty"); paramsOK = false;}

		ReCaptcha captcha = ReCaptchaFactory.newReCaptcha(Parameters.getString("recaptcha.publicKey"), Parameters.getString("recaptcha.privateKey"), false);
		ReCaptchaResponse response = captcha.checkAnswer(req.getRemoteAddr(), req.getParameter("recaptcha_challenge_field"), req.getParameter("recaptcha_response_field"));

		if (!response.isValid())  { out.addElement("Incorrect captcha values"); paramsOK = false;}

		if (paramsOK) {
			String insert = "insert into users(usNAME, usPASSW) values (?, ?)";
			PreparedStatement pst = dbConnection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
			pst.setString(1, user);
			pst.setString(2, MD5Encoder.encode(pwd));
			try {
				pst.executeUpdate();
				ResultSet keys = pst.getGeneratedKeys();
				keys.first();
				Integer userID = new Integer(keys.getInt(1));
				req.getSession().setAttribute("loggedUserNAME",user);
				req.getSession().setAttribute("loggedUserID",userID);
				registered = true;
				keys.close();}
			catch (SQLException ex ) {
				if (ex.getErrorCode() == 23001)
					out.addElement("Username "+user+" is already taken by another user");
				else throw ex;
			}
			pst.close();
		}

		return registered;
	}

	private boolean logout(HttpServletRequest req)
	{
		req.getSession().setAttribute("loggedUserNAME",null);
		req.getSession().setAttribute("loggedUserID",null);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doGet (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {

		// Repeated likes are still allowed

		String tweetID = req.getParameter("twid");
		if (tweetID != null) {

			Vector<String> errorList = new Vector<String>();

			Boolean allowVoting = true;
			HashSet<String> table = (HashSet<String>) req.getSession().getAttribute("likedTable");
			if (table == null){ 
				table = new HashSet<String>();
				req.getSession().setAttribute("likedTable",table);
			}
			else allowVoting = !table.contains(tweetID);

			PrintWriter  out = res.getWriter ( );
			String output = "<a href=error.vm>ERROR</a>";
			String update = "update tweets set twLIKES = twLIKES + 1 where twID = "+tweetID;
			String query = "select twLIKES from tweets where twID = "+tweetID;
			try {
				Connection con = (Connection) this.getServletContext().getAttribute("dbconnection");
				con.setAutoCommit(false);
				try {
					Statement stmt = con.createStatement();
					if (allowVoting) stmt.executeUpdate(update);
					ResultSet rs = stmt.executeQuery(query);
					rs.first();
					output = rs.getString(1);
					rs.close();
					stmt.close();
					con.commit();
					if (allowVoting) table.add(tweetID);
				}
				catch (SQLException ex ) {
					con.rollback();
					while (ex != null) {
						errorList.addElement("SQL Error:   " + ex.getMessage ());
						errorList.addElement("SQL Error Code:   " + ex.getErrorCode());
						ex = ex.getNextException();
					}
				}
				con.setAutoCommit(true);

			}
			catch (SQLException ex ) {
				while (ex != null) {
					errorList.addElement("SQL Error:   " + ex.getMessage ());
					errorList.addElement("SQL Error Code:   " + ex.getErrorCode());
					ex = ex.getNextException();
				}
			}

			//finally
			if (!errorList.isEmpty()) req.getSession().setAttribute("theList",errorList);
			out.print(output);
			return;
		}
		else res.sendRedirect("wall");
	}

}
