package currencyexchange.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "ExceptionHandler", urlPatterns = "/exceptionHandler")
public class ExceptionHandler extends HttpServlet {
//    http://localhost:8080//exceptionHandler

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Получаю существующую сессию (false - не создавать новую, если нет)
        HttpSession session = request.getSession(false);
        String errorCode = (String) session.getAttribute("errorCode");

        String jsonError = (String) session.getAttribute("jsonError");
        switch (errorCode) {
            case "SC_INTERNAL_SERVER_ERROR" -> {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //500
            }
            case "SC_BAD_REQUEST" -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            }
            case "SC_NOT_FOUND" -> {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); //404
            }
            case "SC_CONFLICT" -> {
                response.setStatus(HttpServletResponse.SC_CONFLICT);  //409
            }
        }
        PrintWriter out = response.getWriter();
        out.print(jsonError);
        out.flush();
    }
}
