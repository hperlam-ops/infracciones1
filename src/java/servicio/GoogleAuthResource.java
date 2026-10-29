package servicio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("auth/google")
public class GoogleAuthResource {

    private static final Logger LOGGER =
            Logger.getLogger(GoogleAuthResource.class.getName());
    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT =
            "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SESSION_USER = "googleAuthenticatedUser";
    private static final String SESSION_STATE = "googleOAuthState";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Context
    private HttpServletRequest request;

    @GET
    @Path("/start")
    public Response iniciar() {
        String clientId = requiredSetting("GOOGLE_CLIENT_ID");
        String redirectUri = redirectUri();
        String state = createState();
        request.getSession(true).setAttribute(SESSION_STATE, state);

        String location = AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state);
        return Response.seeOther(URI.create(location)).build();
    }

    @GET
    @Path("/callback")
    public Response callback(
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String error) {
        if (error != null) {
            return redirectWithError("Google rechazó el acceso.");
        }
        HttpSession session = request.getSession(false);
        String expectedState = session == null
                ? null : (String) session.getAttribute(SESSION_STATE);
        if (expectedState == null || state == null
                || !MessageDigest.isEqual(
                        expectedState.getBytes(StandardCharsets.UTF_8),
                        state.getBytes(StandardCharsets.UTF_8))) {
            return redirectWithError("La solicitud de acceso no es válida. Inténtalo nuevamente.");
        }
        if (code == null || code.trim().isEmpty()) {
            return redirectWithError("Google no devolvió un código de acceso.");
        }

        try {
            JsonObject token = exchangeCode(code);
            String accessToken = token.getString("access_token", null);
            if (accessToken == null || accessToken.isEmpty()) {
                return redirectWithError("Google no devolvió un token de acceso.");
            }
            JsonObject profile = requestUserInfo(accessToken);
            Usuario usuario = findAuthorizedUser(profile);

            LoginResponse login = new LoginResponse(
                    true,
                    usuario.getIdUsuario(),
                    usuario.getNombreUsuario(),
                    usuario.getNombreCompleto(),
                    usuario.getRol());
            session.setAttribute(SESSION_USER, login);
            session.removeAttribute(SESSION_STATE);
            return Response.seeOther(URI.create(
                    request.getContextPath() + "/index.html?google=success")).build();
        } catch (IllegalStateException ex) {
            LOGGER.log(Level.WARNING, "Google OAuth rechazado: {0}", ex.getMessage());
            return redirectWithError(ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error inesperado durante Google OAuth.", ex);
            return redirectWithError(
                    "No fue posible validar tu cuenta de Google. Revisa el log de GlassFish.");
        }
    }

    @GET
    @Path("/session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response session() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        LoginResponse usuario = (LoginResponse) session.getAttribute(SESSION_USER);
        if (usuario == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(usuario).build();
    }

    @GET
    @Path("/logout")
    public Response logout() {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Response.noContent().build();
    }

    private JsonObject exchangeCode(String code) throws IOException {
        String body = "code=" + encode(code)
                + "&client_id=" + encode(requiredSetting("GOOGLE_CLIENT_ID"))
                + "&client_secret=" + encode(requiredSetting("GOOGLE_CLIENT_SECRET"))
                + "&redirect_uri=" + encode(redirectUri())
                + "&grant_type=authorization_code";
        return postJson(TOKEN_ENDPOINT, body);
    }

    private JsonObject requestUserInfo(String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(USERINFO_ENDPOINT).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        return readJson(connection);
    }

    private JsonObject postJson(String endpoint, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(content);
        }
        return readJson(connection);
    }

    private JsonObject readJson(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400
                ? connection.getErrorStream() : connection.getInputStream();
        String content = read(stream);
        if (status >= 400) {
            throw new IllegalStateException(
                    "Google rechazó la autenticación (HTTP " + status + ").");
        }
        try (JsonReader reader = Json.createReader(
                new java.io.StringReader(content))) {
            return reader.readObject();
        }
    }

    private Usuario findAuthorizedUser(JsonObject profile) throws Exception {
        String email = profile.getString("email", null);
        boolean verified = profile.getBoolean("email_verified", false);
        if (email == null || !verified) {
            throw new IllegalStateException("Google no pudo verificar tu correo.");
        }
        email = email.trim().toLowerCase();

        String sql = "SELECT id_usuario, nombre_usuario, nombre_completo, rol "
                + "FROM usuario WHERE LOWER(TRIM(nombre_usuario)) = ? LIMIT 1";
        Conexion conexion = new Conexion();
        Connection connection = conexion.getConnection();
        if (connection == null) {
            throw new IllegalStateException("No se pudo conectar con la base de datos.");
        }
        try (Connection connectionToClose = connection;
                PreparedStatement statement = connectionToClose.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Usuario(
                            result.getInt("id_usuario"),
                            result.getString("nombre_usuario"),
                            null,
                            result.getString("nombre_completo"),
                            result.getString("rol"));
                }

                String nombreCompleto = profile.getString("name", email);
                String insert = "INSERT INTO usuario "
                        + "(nombre_usuario, contrasena, nombre_completo, rol) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement create = connectionToClose.prepareStatement(
                        insert, Statement.RETURN_GENERATED_KEYS)) {
                    create.setString(1, email);
                    create.setString(2, UUID.randomUUID().toString());
                    create.setString(3, nombreCompleto);
                    create.setString(4, "Usuario");
                    create.executeUpdate();
                    try (ResultSet generated = create.getGeneratedKeys()) {
                        if (!generated.next()) {
                            throw new IllegalStateException(
                                    "No se pudo crear el usuario de Google.");
                        }
                        return new Usuario(
                                generated.getInt(1),
                                email,
                                null,
                                nombreCompleto,
                                "Usuario");
                    }
                }
            }
        }
    }

    private String redirectUri() {
        String configured = setting("GOOGLE_REDIRECT_URI");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured;
        }
        return request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath()
                + "/webresources/auth/google/callback";
    }

    private String requiredSetting(String name) {
        String value = setting(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Falta configurar " + name + " en el servidor.");
        }
        return value.trim();
    }

    private String setting(String name) {
        String value = System.getenv(name);
        return value == null ? System.getProperty(name) : value;
    }

    private String createState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar la autenticación.");
        }
    }

    private String read(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private Response redirectWithError(String message) {
        return Response.seeOther(URI.create(
                request.getContextPath() + "/index.html?google=error&message="
                + encode(message == null ? "No se pudo iniciar sesión con Google." : message))).build();
    }
}
