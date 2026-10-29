package servicio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("infracciones")
@Produces(MediaType.APPLICATION_JSON)
public class InfraccionesResource {

    private static final Logger LOGGER =
            Logger.getLogger(InfraccionesResource.class.getName());
    @Context
    private HttpServletRequest request;

    @GET
    public Response listar() {
        if (!esAdministrador()) return prohibido();
        String sql = "SELECT i.id_infraccion, i.fecha, i.observaciones, "
                + "i.id_vehiculo, i.id_conductor, i.id_sancion, i.id_agente, "
                + "v.placa, v.marca, v.modelo, "
                + "CONCAT(c.nombre, ' ', c.apellido) AS conductor, "
                + "s.descripcion AS sancion, s.costo, "
                + "CONCAT(a.nombre, ' ', a.apellido) AS agente "
                + "FROM infraccion i "
                + "LEFT JOIN vehiculo v ON v.id_vehiculo=i.id_vehiculo "
                + "LEFT JOIN conductor c ON c.id_conductor=i.id_conductor "
                + "LEFT JOIN sancion s ON s.id_sancion=i.id_sancion "
                + "LEFT JOIN agente_transito a ON a.id_agente=i.id_agente "
                + "ORDER BY i.fecha DESC, i.id_infraccion DESC";
        List<JsonObject> datos = new ArrayList<>();
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                datos.add(Json.createObjectBuilder()
                        .add("idInfraccion", result.getInt("id_infraccion"))
                        .add("fecha", texto(result.getTimestamp("fecha")))
                        .add("observaciones", texto(result.getString("observaciones")))
                        .add("idVehiculo", result.getInt("id_vehiculo"))
                        .add("idConductor", result.getInt("id_conductor"))
                        .add("idSancion", result.getInt("id_sancion"))
                        .add("idAgente", result.getInt("id_agente"))
                        .add("placa", texto(result.getString("placa")))
                        .add("vehiculo", texto(result.getString("marca")) + " "
                                + texto(result.getString("modelo")))
                        .add("conductor", texto(result.getString("conductor")))
                        .add("sancion", texto(result.getString("sancion")))
                        .add("costo", result.getDouble("costo"))
                        .add("agente", texto(result.getString("agente")))
                        .build());
            }
            return Response.ok(datos).build();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al listar infracciones.", ex);
            return error("No se pudieron cargar las infracciones.");
        }
    }

    @GET
    @Path("catalogos")
    public Response catalogos() {
        if (!esAdministrador()) return prohibido();
        try (Connection connection = conexion()) {
            JsonObjectBuilder result = Json.createObjectBuilder()
                    .add("sanciones", array(consultar(connection,
                            "SELECT id_sancion id, descripcion nombre FROM sancion ORDER BY descripcion")))
                    .add("vehiculos", array(consultar(connection,
                            "SELECT id_vehiculo id, CONCAT(placa, ' - ', marca, ' ', modelo) nombre "
                            + "FROM vehiculo ORDER BY placa")))
                    .add("conductores", array(consultar(connection,
                            "SELECT id_conductor id, CONCAT(nombre, ' ', apellido) nombre "
                            + "FROM conductor ORDER BY nombre, apellido")))
                    .add("agentes", array(consultar(connection,
                            "SELECT id_agente id, CONCAT(nombre, ' ', apellido) nombre "
                            + "FROM agente_transito ORDER BY nombre, apellido")));
            return Response.ok(result.build()).build();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al cargar catálogos de infracciones.", ex);
            return error("No se pudieron cargar los datos relacionados.");
        }
    }

        @GET
        @Path("vehiculo-por-placa")
        public Response vehiculoPorPlaca(@QueryParam("placa") String placa) {
            if (!esAdministrador()) return prohibido();
            if (placa == null || placa.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        Json.createObjectBuilder().add("mensaje", "Ingresa una placa.").build()).build();
            }
            String sql = "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, "
                    + "COALESCE(v.id_conductor, i.id_conductor) AS id_conductor, "
                    + "CONCAT(c.nombre, ' ', c.apellido) AS conductor "
                    + "FROM vehiculo v "
                    + "LEFT JOIN infraccion i ON i.id_vehiculo=v.id_vehiculo "
                    + "LEFT JOIN conductor c ON c.id_conductor=COALESCE(v.id_conductor, i.id_conductor) "
                    + "WHERE LOWER(TRIM(v.placa))=LOWER(TRIM(?)) "
                    + "ORDER BY i.fecha DESC LIMIT 1";
            try (Connection connection = conexion();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, placa.trim());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Response.status(Response.Status.NOT_FOUND).entity(
                                Json.createObjectBuilder().add("mensaje",
                                        "No existe un vehículo con esa placa.").build()).build();
                    }
                    JsonObjectBuilder response = Json.createObjectBuilder()
                            .add("idVehiculo", result.getInt("id_vehiculo"))
                            .add("placa", texto(result.getString("placa")))
                            .add("vehiculo", texto(result.getString("marca")) + " "
                                    + texto(result.getString("modelo")));
                    if (result.getObject("id_conductor") != null) {
                        response.add("idConductor", result.getInt("id_conductor"))
                                .add("conductor", texto(result.getString("conductor")));
                    }
                    return Response.ok(response.build()).build();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error al buscar vehículo por placa.", ex);
                return error("No se pudo buscar el vehículo. Revisa la conexión y el esquema de la base de datos.");
            }
        }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crear(String contenido) {
        return guardar(contenido, false);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editar(@PathParam("id") int id, String contenido) {
        return guardar(contenido, true, id);
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) {
        if (!esAdministrador()) return prohibido();
        try (Connection connection = conexion();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM infraccion WHERE id_infraccion=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
            return Response.noContent().build();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al eliminar infracción.", ex);
            return error("No se pudo eliminar la infracción. Puede tener pagos asociados.");
        }
    }

    private Response guardar(String contenido, boolean editar, int... id) {
        if (!esAdministrador()) return prohibido();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(contenido));
                Connection connection = conexion()) {
            JsonObject data = reader.readObject();
            if (data.getInt("idVehiculo", 0) <= 0
                    || data.getInt("idConductor", 0) <= 0
                    || data.getInt("idSancion", 0) <= 0
                    || data.getInt("idAgente", 0) <= 0
                    || data.getString("fecha", "").trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        Json.createObjectBuilder().add("mensaje",
                                "Vehículo, conductor, sanción, agente y fecha son obligatorios.").build()).build();
            }
            String sql = editar
                    ? "UPDATE infraccion SET id_vehiculo=?, id_conductor=?, id_sancion=?, "
                    + "id_agente=?, fecha=?, observaciones=? WHERE id_infraccion=?"
                    : "INSERT INTO infraccion "
                    + "(id_vehiculo, id_conductor, id_sancion, id_agente, fecha, observaciones, id_usuario) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
                llenar(statement, data);
                if (editar) {
                    statement.setInt(7, id[0]);
                } else {
                    LoginResponse usuario = usuarioActual();
                    if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();
                    statement.setInt(7, usuario.getIdUsuario());
                }
                statement.executeUpdate();
                return Response.status(editar ? Response.Status.OK : Response.Status.CREATED)
                        .build();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al guardar infracción.", ex);
            return error("No se pudo guardar la infracción: "
                    + (ex.getMessage() == null ? "error de base de datos." : ex.getMessage()));
        }
    }

    private void llenar(PreparedStatement statement, JsonObject data) throws Exception {
        statement.setInt(1, data.getInt("idVehiculo"));
        statement.setInt(2, data.getInt("idConductor"));
        statement.setInt(3, data.getInt("idSancion"));
        statement.setInt(4, data.getInt("idAgente"));
        statement.setString(5, data.getString("fecha"));
        statement.setString(6, data.getString("observaciones", ""));
    }

    private List<JsonObject> consultar(Connection connection, String sql) throws Exception {
        List<JsonObject> datos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                datos.add(Json.createObjectBuilder()
                        .add("id", result.getInt("id"))
                        .add("nombre", texto(result.getString("nombre")))
                        .build());
            }
        }
        return datos;
    }

    private javax.json.JsonArray array(List<JsonObject> values) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        values.forEach(array::add);
        return array.build();
    }

    private Connection conexion() throws Exception {
        Connection connection = new Conexion().getConnection();
        if (connection == null) throw new IllegalStateException("Sin conexión.");
        return connection;
    }

    private boolean esAdministrador() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        LoginResponse usuario = (LoginResponse) session.getAttribute("googleAuthenticatedUser");
        if (usuario == null || usuario.getRol() == null) return false;
        String rol = usuario.getRol().trim().toUpperCase();
        return "ADMIN".equals(rol) || "ADMINISTRADOR".equals(rol)
                || "ADMINISTRADOR GENERAL".equals(rol);
    }

    private LoginResponse usuarioActual() {
        HttpSession session = request.getSession(false);
        return session == null ? null
                : (LoginResponse) session.getAttribute("googleAuthenticatedUser");
    }

    private Response prohibido() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    private Response error(String mensaje) {
        return Response.serverError().entity(
                Json.createObjectBuilder().add("mensaje", mensaje).build()).build();
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
