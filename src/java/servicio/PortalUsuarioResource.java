package servicio;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("portal")
public class PortalUsuarioResource {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger LOGGER =
            Logger.getLogger(PortalUsuarioResource.class.getName());

    @Context
    private HttpServletRequest request;

    @GET
    @Path("mis-infracciones")
    @Produces(MediaType.APPLICATION_JSON)
    public Response misInfracciones(@QueryParam("identificador") String identificador) {
        LoginResponse usuario = usuarioActual();
        if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();
        if (esAdministrador(usuario.getRol())) return Response.status(Response.Status.FORBIDDEN).build();
        if (identificador == null || identificador.trim().isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Ingresa un número de placa o DPI.");
        }

        List<PortalInfraccion> infracciones = new ArrayList<>();
        String valor = identificador.trim();
        String sql = "SELECT i.id_infraccion, i.fecha, i.observaciones, "
                + "s.descripcion AS sancion, s.costo, v.placa, v.marca, v.modelo, v.color, "
                + "c.id_conductor, c.nombre AS nombre_conductor, c.apellido AS apellido_conductor, "
                + "c.dpi, "
                + "p.id_pago, p.fecha_pago, p.monto_total "
                + "FROM infraccion i "
                + "LEFT JOIN sancion s ON s.id_sancion=i.id_sancion "
                + "LEFT JOIN vehiculo v ON v.id_vehiculo=i.id_vehiculo "
                + "LEFT JOIN conductor c ON c.id_conductor=i.id_conductor "
                + "LEFT JOIN pago p ON p.id_infraccion=i.id_infraccion "
                + "WHERE LOWER(TRIM(v.placa))=LOWER(TRIM(?)) "
                + "OR LOWER(TRIM(c.dpi))=LOWER(TRIM(?)) "
                + "ORDER BY i.fecha DESC,i.id_infraccion DESC";
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, valor);
            statement.setString(2, valor);
            Conductor conductor = null;
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (conductor == null && result.getObject("id_conductor") != null) {
                        conductor = new Conductor(result.getInt("id_conductor"),
                                result.getString("nombre_conductor"),
                                result.getString("apellido_conductor"),
                                result.getString("dpi"));
                    }
                    infracciones.add(new PortalInfraccion(result.getInt("id_infraccion"),
                            result.getTimestamp("fecha"), result.getString("observaciones"),
                            result.getString("sancion"), result.getDouble("costo"),
                            result.getString("placa"), result.getString("marca"),
                            result.getString("modelo"), result.getString("color"),
                            result.getString("nombre_conductor"),
                            result.getString("apellido_conductor"),
                            result.getObject("id_pago") != null,
                            result.getTimestamp("fecha_pago"),
                            result.getObject("monto_total") == null
                                    ? null : result.getDouble("monto_total")));
                }
            }
            if (conductor == null) {
                conductor = buscarConductorPorDpi(connection, valor);
            }
            if (conductor == null) {
                conductor = conductorGuardado(valor);
            } else {
                guardarConductor(valor, conductor);
            }
            return Response.ok(new PortalUsuario(usuario.getNombreCompleto(),
                    usuario.getNombreUsuario(), valor, conductor, infracciones)).build();
        } catch (Exception ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR,
                    "No se pudo cargar la información del usuario.");
        }
    }

    private Conductor buscarConductorPorDpi(Connection connection, String identificador)
            throws Exception {
        String sql = "SELECT id_conductor,nombre,apellido,dpi "
                + "FROM conductor WHERE LOWER(TRIM(dpi))=LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identificador);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new Conductor(result.getInt("id_conductor"),
                        result.getString("nombre"), result.getString("apellido"),
                        result.getString("dpi"));
            }
        }
    }

    private void guardarConductor(String identificador, Conductor conductor) {
        HttpSession session = request.getSession(true);
        session.setAttribute("portalIdentificador", identificador);
        session.setAttribute("portalConductor", conductor);
    }

    private Conductor conductorGuardado(String identificador) {
        HttpSession session = request.getSession(false);
        if (session == null || !identificador.equals(session.getAttribute("portalIdentificador"))) {
            return null;
        }
        Object valor = session.getAttribute("portalConductor");
        return valor instanceof Conductor ? (Conductor) valor : null;
    }

    @POST
    @Path("generar-voucher")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarVoucher(String contenido) {
        LoginResponse usuario = usuarioActual();
        if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();
        if (esAdministrador(usuario.getRol())) return Response.status(Response.Status.FORBIDDEN).build();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(contenido))) {
            JsonObject data = reader.readObject();
            int id = data.getInt("idInfraccion");
            String identificador = data.getString("identificador", "").trim();
            if (identificador.isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "Debes indicar la placa o DPI.");
            }
            if (!validarInfraccion(id, identificador)) {
                return error(Response.Status.NOT_FOUND,
                        "La infracción ya no existe o no corresponde a la consulta.");
            }
            String codigo = String.format("BAN-%06d-%06d", RANDOM.nextInt(1000000),
                    RANDOM.nextInt(1000000));
            HttpSession session = request.getSession(true);
            session.setAttribute("voucherCodigo", codigo);
            session.setAttribute("voucherInfraccion", id);
            session.setAttribute("voucherIdentificador", identificador);
            return Response.ok(Json.createObjectBuilder().add("codigoPago", codigo)
                    .add("idInfraccion", id).build()).build();
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, "No se pudo generar el voucher.");
        }
    }

    @POST
    @Path("confirmar-pago")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarPago(String contenido) {
        LoginResponse usuario = usuarioActual();
        if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();
        if (esAdministrador(usuario.getRol())) return Response.status(Response.Status.FORBIDDEN).build();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(contenido))) {
            String codigo = reader.readObject().getString("codigo", "").trim();
            HttpSession session = request.getSession(false);
            String guardado = session == null ? null : (String) session.getAttribute("voucherCodigo");
            Integer id = session == null ? null : (Integer) session.getAttribute("voucherInfraccion");
            String identificador = session == null ? null
                    : (String) session.getAttribute("voucherIdentificador");
            if (codigo.isEmpty() || guardado == null || id == null
                    || !guardado.equalsIgnoreCase(codigo) || identificador == null) {
                return error(Response.Status.BAD_REQUEST,
                        "El código de pago no es válido o ya fue utilizado.");
            }
            if (!registrarPago(id, identificador, usuario.getIdUsuario())) {
                return error(Response.Status.CONFLICT,
                        "La infracción ya no existe, ya está pagada o no corresponde al código.");
            }
            session.removeAttribute("voucherCodigo");
            session.removeAttribute("voucherInfraccion");
            session.removeAttribute("voucherIdentificador");
            return Response.ok(Json.createObjectBuilder()
                    .add("mensaje", "Pago confirmado. La infracción ahora aparece como pagada.")
                    .add("idInfraccion", id).build()).build();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al confirmar pago.", ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR,
                    "No se pudo registrar el pago en la base de datos: "
                    + (ex.getMessage() == null ? "error desconocido." : ex.getMessage()));
        }
    }

    private boolean validarInfraccion(int id, String identificador) throws Exception {
        String sql = "SELECT i.id_infraccion FROM infraccion i "
                + "LEFT JOIN vehiculo v ON v.id_vehiculo=i.id_vehiculo "
                + "LEFT JOIN conductor c ON c.id_conductor=i.id_conductor "
                + "LEFT JOIN pago p ON p.id_infraccion=i.id_infraccion "
                + "WHERE i.id_infraccion=? AND p.id_pago IS NULL "
                + "AND (LOWER(TRIM(v.placa))=LOWER(TRIM(?)) "
                + "OR LOWER(TRIM(c.dpi))=LOWER(TRIM(?)))";
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, identificador);
            statement.setString(3, identificador);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private boolean registrarPago(int id, String identificador, int idUsuario) throws Exception {
        String sql = "INSERT INTO pago (id_infraccion, id_usuario, fecha_pago, monto_total) "
                + "SELECT i.id_infraccion, ?, CURRENT_TIMESTAMP, s.costo "
                + "FROM infraccion i "
                + "LEFT JOIN vehiculo v ON v.id_vehiculo=i.id_vehiculo "
                + "LEFT JOIN conductor c ON c.id_conductor=i.id_conductor "
                + "LEFT JOIN pago p ON p.id_infraccion=i.id_infraccion "
                + "LEFT JOIN sancion s ON s.id_sancion=i.id_sancion "
                + "WHERE i.id_infraccion=? AND p.id_pago IS NULL "
                + "AND (LOWER(TRIM(v.placa))=LOWER(TRIM(?)) "
                + "OR LOWER(TRIM(c.dpi))=LOWER(TRIM(?)))";
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, id);
            statement.setString(3, identificador);
            statement.setString(4, identificador);
            return statement.executeUpdate() == 1;
        }
    }

    private Connection conexion() throws Exception {
        Connection connection = new Conexion().getConnection();
        if (connection == null) throw new IllegalStateException("Sin conexión.");
        return connection;
    }

    private LoginResponse usuarioActual() {
        HttpSession session = request.getSession(false);
        return session == null ? null
                : (LoginResponse) session.getAttribute("googleAuthenticatedUser");
    }

    private boolean esAdministrador(String rol) {
        if (rol == null) return false;
        String normalizado = rol.trim().toUpperCase();
        return "ADMIN".equals(normalizado) || "ADMINISTRADOR".equals(normalizado)
                || "ADMINISTRADOR GENERAL".equals(normalizado);
    }

    private Response error(Response.Status status, String mensaje) {
        return Response.status(status).entity(Json.createObjectBuilder()
                .add("mensaje", mensaje).build()).build();
    }

    public static class PortalUsuario {
        private final String nombreCompleto;
        private final String nombreUsuario;
        private final String identificador;
        private final Conductor conductor;
        private final List<PortalInfraccion> infracciones;

        public PortalUsuario(String nombreCompleto, String nombreUsuario,
                String identificador, Conductor conductor,
                List<PortalInfraccion> infracciones) {
            this.nombreCompleto = nombreCompleto;
            this.nombreUsuario = nombreUsuario;
            this.identificador = identificador;
            this.conductor = conductor;
            this.infracciones = infracciones;
        }

        public String getNombreCompleto() { return nombreCompleto; }
        public String getNombreUsuario() { return nombreUsuario; }
        public String getIdentificador() { return identificador; }
        public Conductor getConductor() { return conductor; }
        public List<PortalInfraccion> getInfracciones() { return infracciones; }
    }

    public static class Conductor implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private final String nombre;
        private final String apellido;
        private final String dpi;

        public Conductor(int id, String nombre, String apellido, String dpi) {
            this.id = id; this.nombre = nombre; this.apellido = apellido;
            this.dpi = dpi;
        }

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getDpi() { return dpi; }
    }

    public static class PortalInfraccion {
        private final int idInfraccion;
        private final java.sql.Timestamp fecha;
        private final String observaciones;
        private final String sancion;
        private final double costo;
        private final String placa;
        private final String marca;
        private final String modelo;
        private final String color;
        private final String nombreConductor;
        private final String apellidoConductor;
        private final boolean pagada;
        private final java.sql.Timestamp fechaPago;
        private final Double montoPagado;

        public PortalInfraccion(int idInfraccion, java.sql.Timestamp fecha,
                String observaciones, String sancion, double costo, String placa,
                String marca, String modelo, String color, String nombreConductor,
                String apellidoConductor, boolean pagada,
                java.sql.Timestamp fechaPago, Double montoPagado) {
            this.idInfraccion = idInfraccion; this.fecha = fecha;
            this.observaciones = observaciones; this.sancion = sancion; this.costo = costo;
            this.placa = placa; this.marca = marca; this.modelo = modelo; this.color = color;
            this.nombreConductor = nombreConductor; this.apellidoConductor = apellidoConductor;
            this.pagada = pagada; this.fechaPago = fechaPago; this.montoPagado = montoPagado;
        }

        public int getIdInfraccion() { return idInfraccion; }
        public java.sql.Timestamp getFecha() { return fecha; }
        public String getObservaciones() { return observaciones; }
        public String getSancion() { return sancion; }
        public double getCosto() { return costo; }
        public String getPlaca() { return placa; }
        public String getMarca() { return marca; }
        public String getModelo() { return modelo; }
        public String getColor() { return color; }
        public String getNombreConductor() { return nombreConductor; }
        public String getApellidoConductor() { return apellidoConductor; }
        public boolean isPagada() { return pagada; }
        public java.sql.Timestamp getFechaPago() { return fechaPago; }
        public Double getMontoPagado() { return montoPagado; }
    }
}
