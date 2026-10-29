package servicio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminDataResource {

    @Context private HttpServletRequest request;

    @GET @Path("conductores")
    public Response conductores() {
        return consulta("SELECT id_conductor,nombre,apellido,dpi,licencia,telefono,direccion "
                + "FROM conductor ORDER BY apellido,nombre", "conductores");
    }

    @GET @Path("agentes-transito")
    public Response agentesTransito() {
        return consulta("SELECT id_agente,nombre,apellido FROM agente_transito "
                + "ORDER BY apellido,nombre", "agentes");
    }

    @POST @Path("agentes-transito") @Consumes(MediaType.APPLICATION_JSON)
    public Response crearAgenteTransito(String body) {
        return guardarAgenteTransito(body, 0);
    }

    @PUT @Path("agentes-transito/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response editarAgenteTransito(@PathParam("id") int id, String body) {
        if (id <= 0) return badRequest("El agente indicado no es válido.");
        return guardarAgenteTransito(body, id);
    }

    @DELETE @Path("agentes-transito/{id}")
    public Response eliminarAgenteTransito(@PathParam("id") int id) {
        if (!admin()) return forbidden();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "DELETE FROM agente_transito WHERE id_agente=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) {
                return Response.status(Response.Status.NOT_FOUND).entity(
                        Json.createObjectBuilder().add("mensaje",
                                "No se encontró el agente de tránsito.").build()).build();
            }
            return Response.noContent().build();
        } catch (Exception ex) {
            return error("No se pudo eliminar el agente. Puede tener infracciones registradas.");
        }
    }

    @GET @Path("vehiculos")
    public Response vehiculos() {
        return consulta("SELECT v.id_vehiculo,v.placa,v.marca,v.modelo,v.color,"
                + "v.id_tipo_vehiculo,v.id_conductor,t.nombre AS tipo FROM vehiculo v "
                + "LEFT JOIN tipo_vehiculo t ON t.id_tipo_vehiculo=v.id_tipo_vehiculo "
                + "ORDER BY v.placa", "vehiculos");
    }

    @GET @Path("tipos-vehiculo")
    public Response tiposVehiculo() {
        return consulta("SELECT id_tipo_vehiculo,nombre FROM tipo_vehiculo ORDER BY nombre", "tipos");
    }

    @POST @Path("tipos-vehiculo") @Consumes(MediaType.APPLICATION_JSON)
    public Response crearTipoVehiculo(String body) {
        if (!admin()) return forbidden();
        String nombre;
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body))) {
            nombre = reader.readObject().getString("nombre", "").trim();
        } catch (Exception ex) {
            return badRequest("Ingresa un nombre válido para el tipo de vehículo.");
        }
        if (nombre.isEmpty()) {
            return badRequest("El nombre del tipo de vehículo es obligatorio.");
        }
        try (Connection c = connection()) {
            if (existeTipoVehiculo(c, nombre, 0)) {
                return badRequest("Ya existe un tipo de vehículo con ese nombre.");
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO tipo_vehiculo (nombre) VALUES (?)")) {
                p.setString(1, nombre);
                p.executeUpdate();
            }
            return Response.ok().build();
        } catch (Exception ex) {
            return error("No se pudo crear el tipo de vehículo.");
        }
    }

    @PUT @Path("tipos-vehiculo/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response editarTipoVehiculo(@PathParam("id") int id, String body) {
        if (!admin()) return forbidden();
        String nombre;
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body))) {
            nombre = reader.readObject().getString("nombre", "").trim();
        } catch (Exception ex) {
            return badRequest("Ingresa un nombre válido para el tipo de vehículo.");
        }
        if (nombre.isEmpty()) {
            return badRequest("El nombre del tipo de vehículo es obligatorio.");
        }
        try (Connection c = connection()) {
            if (existeTipoVehiculo(c, nombre, id)) {
                return badRequest("Ya existe un tipo de vehículo con ese nombre.");
            }
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE tipo_vehiculo SET nombre=? WHERE id_tipo_vehiculo=?")) {
                p.setString(1, nombre);
                p.setInt(2, id);
                if (p.executeUpdate() == 0) {
                    return Response.status(Response.Status.NOT_FOUND).entity(
                            Json.createObjectBuilder().add("mensaje",
                                    "No se encontró el tipo de vehículo.").build()).build();
                }
            }
            return Response.ok().build();
        } catch (Exception ex) {
            return error("No se pudo actualizar el tipo de vehículo.");
        }
    }

    @DELETE @Path("tipos-vehiculo/{id}")
    public Response eliminarTipoVehiculo(@PathParam("id") int id) {
        if (!admin()) return forbidden();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "DELETE FROM tipo_vehiculo WHERE id_tipo_vehiculo=?")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) {
                return Response.status(Response.Status.NOT_FOUND).entity(
                        Json.createObjectBuilder().add("mensaje",
                                "No se encontró el tipo de vehículo.").build()).build();
            }
            return Response.noContent().build();
        } catch (Exception ex) {
            return error("No se pudo eliminar el tipo de vehículo. Puede estar asignado a vehículos registrados.");
        }
    }

    @GET @Path("pagos")
    public Response pagos() {
        String sql = "SELECT p.id_pago AS idPago, p.id_infraccion AS idInfraccion, "
                + "p.fecha_pago AS fechaPago, p.monto_total AS monto, "
                + "v.placa AS placa, "
                + "CONCAT(c.nombre, ' ', c.apellido) AS conductor, "
                + "s.descripcion AS sancion, u.nombre_completo AS registradoPor "
                + "FROM pago p "
                + "LEFT JOIN infraccion i ON i.id_infraccion=p.id_infraccion "
                + "LEFT JOIN vehiculo v ON v.id_vehiculo=i.id_vehiculo "
                + "LEFT JOIN conductor c ON c.id_conductor=i.id_conductor "
                + "LEFT JOIN sancion s ON s.id_sancion=i.id_sancion "
                + "LEFT JOIN usuario u ON u.id_usuario=p.id_usuario "
                + "ORDER BY p.fecha_pago DESC, p.id_pago DESC";
        return consulta(sql, "pagos");
    }

    @POST @Path("conductores") @Consumes(MediaType.APPLICATION_JSON)
    public Response crearConductor(String body) {
        return ejecutar(body, "INSERT INTO conductor (nombre,apellido,dpi,licencia,telefono,direccion) "
                + "VALUES (?,?,?,?,?,?)", "conductor");
    }

    @PUT @Path("conductores/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response editarConductor(@PathParam("id") int id, String body) {
        return ejecutar(body, "UPDATE conductor SET nombre=?,apellido=?,dpi=?,licencia=?,telefono=?,direccion=? "
                + "WHERE id_conductor=?", "conductor", id);
    }

    @DELETE @Path("conductores/{id}")
    public Response eliminarConductor(@PathParam("id") int id) {
        return eliminar("DELETE FROM conductor WHERE id_conductor=?", id, "conductor");
    }

    @POST @Path("vehiculos") @Consumes(MediaType.APPLICATION_JSON)
    public Response crearVehiculo(String body) {
        if (!admin()) return forbidden();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body));
                Connection c = connection()) {
            JsonObject d = reader.readObject();
            String sql = "INSERT INTO vehiculo (placa,marca,modelo,color,id_tipo_vehiculo,id_conductor) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, d.getString("placa"));
                p.setString(2, d.getString("marca"));
                p.setString(3, d.getString("modelo", ""));
                p.setString(4, d.getString("color", ""));
                p.setInt(5, d.getInt("idTipoVehiculo"));
                if (d.containsKey("idConductor") && !d.isNull("idConductor") && d.getInt("idConductor") > 0) {
                    p.setInt(6, d.getInt("idConductor"));
                } else {
                    p.setNull(6, java.sql.Types.INTEGER);
                }
                p.executeUpdate();
                return Response.ok().build();
            }
        } catch (Exception ex) {
            return error("No se pudo crear el vehículo.");
        }
    }

    @PUT @Path("vehiculos/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response editarVehiculo(@PathParam("id") int id, String body) {
        if (!admin()) return forbidden();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body));
                Connection c = connection()) {
            JsonObject d = reader.readObject();
            String sql = "UPDATE vehiculo SET placa=?,marca=?,modelo=?,color=?,id_tipo_vehiculo=?,id_conductor=? WHERE id_vehiculo=?";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, d.getString("placa"));
                p.setString(2, d.getString("marca"));
                p.setString(3, d.getString("modelo", ""));
                p.setString(4, d.getString("color", ""));
                p.setInt(5, d.getInt("idTipoVehiculo"));
                if (d.containsKey("idConductor") && !d.isNull("idConductor") && d.getInt("idConductor") > 0) {
                    p.setInt(6, d.getInt("idConductor"));
                } else {
                    p.setNull(6, java.sql.Types.INTEGER);
                }
                p.setInt(7, id);
                p.executeUpdate();
                return Response.ok().build();
            }
        } catch (Exception ex) {
            return error("No se pudo actualizar el vehículo.");
        }
    }

    @DELETE @Path("vehiculos/{id}")
    public Response eliminarVehiculo(@PathParam("id") int id) {
        return eliminar("DELETE FROM vehiculo WHERE id_vehiculo=?", id, "vehiculo");
    }

    /* ── Vehículos asignados a un conductor (vía columna id_conductor) ── */

    @GET @Path("conductores/{id}/vehiculos")
    public Response vehiculosDeConductor(@PathParam("id") int idConductor) {
        if (!admin()) return forbidden();
        String sql = "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, v.color, "
                + "v.id_tipo_vehiculo, t.nombre AS tipo "
                + "FROM vehiculo v "
                + "LEFT JOIN tipo_vehiculo t ON t.id_tipo_vehiculo = v.id_tipo_vehiculo "
                + "WHERE v.id_conductor = ? ORDER BY v.placa";
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idConductor);
            try (ResultSet r = p.executeQuery()) {
                List<JsonObject> rows = new ArrayList<>();
                while (r.next()) {
                    rows.add(Json.createObjectBuilder()
                            .add("id_vehiculo", r.getInt("id_vehiculo"))
                            .add("placa", r.getString("placa") != null ? r.getString("placa") : "")
                            .add("marca", r.getString("marca") != null ? r.getString("marca") : "")
                            .add("modelo", r.getString("modelo") != null ? r.getString("modelo") : "")
                            .add("color", r.getString("color") != null ? r.getString("color") : "")
                            .add("id_tipo_vehiculo", r.getInt("id_tipo_vehiculo"))
                            .add("tipo", r.getString("tipo") != null ? r.getString("tipo") : "")
                            .build());
                }
                javax.json.JsonArrayBuilder array = Json.createArrayBuilder();
                rows.forEach(array::add);
                return Response.ok(Json.createObjectBuilder()
                        .add("vehiculos", array).build()).build();
            }
        } catch (Exception ex) {
            return error("No se pudieron cargar los vehículos del conductor.");
        }
    }

    /** Asignar un vehículo existente a un conductor */
    @PUT @Path("conductores/{idC}/vehiculos/{idV}")
    public Response asignarVehiculo(@PathParam("idC") int idConductor,
                                     @PathParam("idV") int idVehiculo) {
        if (!admin()) return forbidden();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "UPDATE vehiculo SET id_conductor=? WHERE id_vehiculo=?")) {
            p.setInt(1, idConductor);
            p.setInt(2, idVehiculo);
            int filas = p.executeUpdate();
            if (filas == 0) return error("No se encontró el vehículo.");
            return Response.ok().build();
        } catch (Exception ex) {
            return error("No se pudo asignar el vehículo.");
        }
    }

    /** Desasignar un vehículo de su conductor (poner id_conductor = NULL) */
    @DELETE @Path("conductores/{idC}/vehiculos/{idV}")
    public Response desasignarVehiculo(@PathParam("idC") int idConductor,
                                       @PathParam("idV") int idVehiculo) {
        if (!admin()) return forbidden();
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(
                        "UPDATE vehiculo SET id_conductor=NULL WHERE id_vehiculo=? AND id_conductor=?")) {
            p.setInt(1, idVehiculo);
            p.setInt(2, idConductor);
            p.executeUpdate();
            return Response.noContent().build();
        } catch (Exception ex) {
            return error("No se pudo desasignar el vehículo.");
        }
    }

    /** Vehículos sin conductor asignado (para el selector) */
    @GET @Path("vehiculos-disponibles")
    public Response vehiculosDisponibles() {
        if (!admin()) return forbidden();
        String sql = "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, v.color, "
                + "v.id_tipo_vehiculo, t.nombre AS tipo "
                + "FROM vehiculo v "
                + "LEFT JOIN tipo_vehiculo t ON t.id_tipo_vehiculo = v.id_tipo_vehiculo "
                + "WHERE v.id_conductor IS NULL ORDER BY v.placa";
        try (Connection c = connection();
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            List<JsonObject> rows = new ArrayList<>();
            while (r.next()) {
                rows.add(Json.createObjectBuilder()
                        .add("id_vehiculo", r.getInt("id_vehiculo"))
                        .add("placa", r.getString("placa") != null ? r.getString("placa") : "")
                        .add("marca", r.getString("marca") != null ? r.getString("marca") : "")
                        .add("modelo", r.getString("modelo") != null ? r.getString("modelo") : "")
                        .add("color", r.getString("color") != null ? r.getString("color") : "")
                        .add("id_tipo_vehiculo", r.getInt("id_tipo_vehiculo"))
                        .add("tipo", r.getString("tipo") != null ? r.getString("tipo") : "")
                        .build());
            }
            javax.json.JsonArrayBuilder array = Json.createArrayBuilder();
            rows.forEach(array::add);
            return Response.ok(Json.createObjectBuilder()
                    .add("vehiculos", array).build()).build();
        } catch (Exception ex) {
            return error("No se pudieron cargar los vehículos disponibles.");
        }
    }

    @PUT @Path("usuarios/{id}") @Consumes(MediaType.APPLICATION_JSON)
    public Response editarUsuario(@PathParam("id") int id, String body) {
        if (!admin()) return forbidden();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body));
                Connection c = connection()) {
            JsonObject data = reader.readObject();
            String nombreUsuario = data.getString("nombreUsuario", "").trim();
            String nombreCompleto = data.getString("nombreCompleto", "").trim();
            String rol = data.getString("rol", "").trim().toUpperCase();
            String password = data.getString("contrasena", "");
            if (nombreUsuario.isEmpty() || nombreCompleto.isEmpty()) {
                return badRequest("La cuenta y el nombre completo son obligatorios.");
            }
            if (!"USUARIO".equals(rol) && !"ADMIN".equals(rol)) {
                return badRequest("El rol seleccionado no es válido.");
            }
            if (!password.isEmpty() && password.length() < 8) {
                return badRequest("La contraseña debe tener al menos 8 caracteres.");
            }
            try (PreparedStatement duplicate = c.prepareStatement(
                    "SELECT id_usuario FROM usuario WHERE LOWER(nombre_usuario)=LOWER(?) "
                    + "AND id_usuario<>? LIMIT 1")) {
                duplicate.setString(1, nombreUsuario);
                duplicate.setInt(2, id);
                try (ResultSet result = duplicate.executeQuery()) {
                    if (result.next()) {
                        return badRequest("Ya existe otra cuenta con ese nombre de usuario.");
                    }
                }
            }
            LoginResponse actual = usuarioActual();
            if (actual != null && actual.getIdUsuario() == id
                    && "USUARIO".equals(rol) && !hayOtroAdministrador(c, id)) {
                return badRequest("No puedes quitar el último administrador del sistema.");
            }
            String sql = password.isEmpty()
                    ? "UPDATE usuario SET nombre_usuario=?,nombre_completo=?,rol=? WHERE id_usuario=?"
                    : "UPDATE usuario SET nombre_usuario=?,contrasena=?,nombre_completo=?,rol=? WHERE id_usuario=?";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, nombreUsuario);
                int offset = 2;
                if (!password.isEmpty()) p.setString(offset++, password);
                p.setString(offset++, nombreCompleto);
                p.setString(offset++, rol);
                p.setInt(offset, id);
                if (p.executeUpdate() != 1) {
                    return Response.status(Response.Status.NOT_FOUND).entity(
                            Json.createObjectBuilder().add("mensaje",
                                    "No se encontró el usuario.").build()).build();
                }
                return Response.ok().build();
            }
        } catch (Exception ex) {
            return error("No se pudo actualizar el usuario.");
        }
    }

    private boolean hayOtroAdministrador(Connection connection, int id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM usuario WHERE id_usuario<>? "
                + "AND UPPER(TRIM(rol)) IN ('ADMIN','ADMINISTRADOR','ADMINISTRADOR GENERAL')")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    private LoginResponse usuarioActual() {
        HttpSession session = request.getSession(false);
        return session == null ? null
                : (LoginResponse) session.getAttribute("googleAuthenticatedUser");
    }

    private Response consulta(String sql, String key) {
        if (!admin()) return forbidden();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            List<JsonObject> rows = new ArrayList<>();
            while (r.next()) {
                javax.json.JsonObjectBuilder b = Json.createObjectBuilder();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
                    Object value = r.getObject(i);
                    if (value == null) b.add(r.getMetaData().getColumnLabel(i), "");
                    else if (value instanceof Number) b.add(r.getMetaData().getColumnLabel(i), ((Number) value).doubleValue());
                    else b.add(r.getMetaData().getColumnLabel(i), String.valueOf(value));
                }
                rows.add(b.build());
            }
            javax.json.JsonArrayBuilder array = Json.createArrayBuilder();
            rows.forEach(array::add);
            return Response.ok(Json.createObjectBuilder().add(key, array).build()).build();
        } catch (Exception ex) {
            return error("No se pudieron cargar los datos.");
        }
    }

    private boolean existeTipoVehiculo(Connection c, String nombre, int excluirId)
            throws Exception {
        String sql = "SELECT 1 FROM tipo_vehiculo "
                + "WHERE LOWER(TRIM(nombre))=LOWER(TRIM(?)) "
                + "AND id_tipo_vehiculo<>? LIMIT 1";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nombre);
            p.setInt(2, excluirId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    private Response guardarAgenteTransito(String body, int id) {
        if (!admin()) return forbidden();
        String nombre;
        String apellido;
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body))) {
            JsonObject data = reader.readObject();
            nombre = data.getString("nombre", "").trim();
            apellido = data.getString("apellido", "").trim();
        } catch (Exception ex) {
            return badRequest("Ingresa el nombre y apellido del agente.");
        }
        if (nombre.isEmpty() || apellido.isEmpty()) {
            return badRequest("El nombre y apellido del agente son obligatorios.");
        }
        String sql = id == 0
                ? "INSERT INTO agente_transito (nombre,apellido) VALUES (?,?)"
                : "UPDATE agente_transito SET nombre=?,apellido=? WHERE id_agente=?";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nombre);
            p.setString(2, apellido);
            if (id != 0) {
                p.setInt(3, id);
                if (p.executeUpdate() == 0) {
                    return Response.status(Response.Status.NOT_FOUND).entity(
                            Json.createObjectBuilder().add("mensaje",
                                    "No se encontró el agente de tránsito.").build()).build();
                }
            } else {
                p.executeUpdate();
            }
            return Response.ok().build();
        } catch (Exception ex) {
            return error(id == 0
                    ? "No se pudo crear el agente de tránsito."
                    : "No se pudo actualizar el agente de tránsito.");
        }
    }

    private Response ejecutar(String body, String sql, String tipo, int... id) {
        if (!admin()) return forbidden();
        try (JsonReader reader = Json.createReader(new java.io.StringReader(body));
                Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            JsonObject d = reader.readObject();
            if ("conductor".equals(tipo)) {
                p.setString(1,d.getString("nombre")); p.setString(2,d.getString("apellido"));
                p.setString(3,d.getString("dpi")); p.setString(4,d.getString("licencia"));
                p.setString(5,d.getString("telefono")); p.setString(6,d.getString("direccion"));
            }
            if (id.length > 0) p.setInt("conductor".equals(tipo) ? 7 : 6, id[0]);
            p.executeUpdate(); return Response.ok().build();
        } catch (Exception ex) { return error("No se pudo guardar el " + tipo + "."); }
    }

    private Response eliminar(String sql, int id, String tipo) {
        if (!admin()) return forbidden();
        try (Connection c=connection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,id); p.executeUpdate(); return Response.noContent().build();
        } catch (Exception ex) { return error("No se pudo eliminar el " + tipo + ". Puede tener infracciones relacionadas."); }
    }

    private Connection connection() throws Exception {
        Connection c = new Conexion().getConnection();
        if (c == null) throw new IllegalStateException("Sin conexión.");
        return c;
    }
    private boolean admin() {
        HttpSession s=request.getSession(false);
        if (s==null) return false; LoginResponse u=(LoginResponse)s.getAttribute("googleAuthenticatedUser");
        if (u==null || u.getRol()==null) return false; String r=u.getRol().trim().toUpperCase();
        return "ADMIN".equals(r)||"ADMINISTRADOR".equals(r)||"ADMINISTRADOR GENERAL".equals(r);
    }
    private Response forbidden(){return Response.status(Response.Status.FORBIDDEN).build();}
    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(
                Json.createObjectBuilder().add("mensaje", message).build()).build();
    }
    private Response error(String message){return Response.serverError().entity(Json.createObjectBuilder().add("mensaje",message).build()).build();}
}
