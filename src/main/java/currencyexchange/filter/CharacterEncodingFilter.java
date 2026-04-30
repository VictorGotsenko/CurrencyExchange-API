package currencyexchange.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Применяю фильтр ко всем запросам
@WebFilter(filterName = "EncodingFilter", urlPatterns = "/*")
public class CharacterEncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // для запроса
        request.setCharacterEncoding("UTF-8");

        // для ответа
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Определение политики: скрипты, стили и шрифты только с того же сайта
        String cspValue = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; " +
                "font-src 'self'; upgrade-insecure-requests;";

        // Установка заголовка
        httpResponse.setHeader("Content-Security-Policy", cspValue);
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");

        // Передача запроса дальше по цепочке
        chain.doFilter(request, response);
    }
}
