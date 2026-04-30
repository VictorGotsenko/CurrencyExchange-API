package currencyexchange;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;


@WebServlet(name = "WelcomeServlet", urlPatterns = "/")
public class WelcomeServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Welcome Page</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>CurrencyExchange</h1>");
        out.println("<p>Приветственная страница проекта “Обмен валют”</p>");
        out.println("<p>This is the welcome page of the Currency Exchange project</p>");
        out.println("</body>");
        out.println("</html>");
    }

}
