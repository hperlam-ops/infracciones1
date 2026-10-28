package servicio;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("recuperacion")
@Produces(MediaType.APPLICATION_JSON)
public class RecuperacionResource {
    private static final Map<String, Solicitud> SOLICITUDES = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @POST
    @Path("solicitar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response solicitar(String contenido) {
        try (JsonReader reader = Json.createReader(new java.io.StringReader(contenido))) {
            String identificador = reader.readObject().getString("identificador", "").trim().toLowerCase();
            if (!CORREO.matcher(identificador).matches()) {
                return respuesta("Si la cuenta corresponde a un correo, recibirás instrucciones.");
            }
            if (!existeCorreo(identificador)) {
                return respuesta("Si la cuenta corresponde a un correo, recibirás instrucciones.");
            }
            String token = UUID.randomUUID().toString();
            SOLICITUDES.put(token, new Solicitud(identificador, Instant.now().plusSeconds(900)));
            enviarCorreo(identificador, token);
            return respuesta("Revisa tu correo. El enlace vence en 15 minutos.");
        } catch (Exception ex) {
            return Response.serverError().entity(Json.createObjectBuilder()
                    .add("mensaje", "No se pudo enviar el correo de recuperación. Configura SMTP en el servidor.")
                    .build()).build();
        }
    }

    @POST
    @Path("restablecer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restablecer(String contenido) {
        try (JsonReader reader = Json.createReader(new java.io.StringReader(contenido))) {
            JsonObject data = reader.readObject();
            String token = data.getString("token", "").trim();
            String password = data.getString("contrasena", "");
            Solicitud solicitud = SOLICITUDES.remove(token);
            if (solicitud == null || solicitud.expira.isBefore(Instant.now())
                    || password.length() < 8) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Json.createObjectBuilder()
                        .add("mensaje", "El enlace no es válido, expiró o la contraseña no cumple el mínimo de 8 caracteres.")
                        .build()).build();
            }
            try (Connection connection = conexion();
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE usuario SET contrasena=? WHERE LOWER(nombre_usuario)=?")) {
                statement.setString(1, password);
                statement.setString(2, solicitud.correo);
                if (statement.executeUpdate() != 1) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(Json.createObjectBuilder()
                            .add("mensaje", "La cuenta ya no existe.").build()).build();
                }
            }
            return respuesta("Contraseña actualizada correctamente.");
        } catch (Exception ex) {
            return Response.serverError().entity(Json.createObjectBuilder()
                    .add("mensaje", "No se pudo actualizar la contraseña.").build()).build();
        }
    }

    private boolean existeCorreo(String correo) throws Exception {
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id_usuario FROM usuario WHERE LOWER(nombre_usuario)=?")) {
            statement.setString(1, correo);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void enviarCorreo(String destino, String token) throws Exception {
        String remitente = requerido("SMTP_USER");
        String password = requerido("SMTP_PASSWORD");
        String host = valor("SMTP_HOST", "smtp.gmail.com");
        String port = valor("SMTP_PORT", "587");
        String base = valor("APP_BASE_URL", "http://localhost:8080/infracciones1/web");
        java.util.Properties properties = new java.util.Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remitente, password);
            }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(remitente));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        message.setSubject("Recuperación de contraseña");
        message.setText("Restablece tu contraseña con este token (válido 15 minutos):\n\n"
                + token + "\n\nIngresa el token en la pantalla de inicio de sesión.");
        Transport.send(message);
    }

    private Connection conexion() throws Exception {
        Connection connection = new Conexion().getConnection();
        if (connection == null) throw new IllegalStateException("Sin conexión.");
        return connection;
    }
    private String requerido(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.trim().isEmpty()) throw new IllegalStateException("Falta " + nombre);
        return valor.trim();
    }
    private String valor(String nombre, String defecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.trim().isEmpty() ? defecto : valor.trim();
    }
    private Response respuesta(String mensaje) {
        return Response.ok(Json.createObjectBuilder().add("mensaje", mensaje).build()).build();
    }
    private static class Solicitud {
        final String correo;
        final Instant expira;
        Solicitud(String correo, Instant expira) { this.correo = correo; this.expira = expira; }
    }
}
