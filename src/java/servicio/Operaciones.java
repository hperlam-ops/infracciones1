/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package servicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author henry
 */
@Path("generic")
public class Operaciones {
    Conexion cn = new Conexion();
    Connection con;
    PreparedStatement ps;
    ResultSet rs;

    @Context
    private UriInfo context;

    public List<Usuario> Consultar() {
        List<Usuario> lista = new ArrayList<>();

        String sql = "select * from usuario";
        try{
            con= cn.getConnection();
            ps=con.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()){
            Usuario u = new Usuario();
            u.setIdUsuario(rs.getInt("id_usuario"));
            u.setNombreUsuario(rs.getString("nombre_usuario"));
            u.setContrasena(rs.getString("contrasena"));
            u.setNombreCompleto(rs.getString("nombre_completo"));
            u.setRol(rs.getString("rol"));
            
            lista.add(u);
        }
            return lista;
        }catch(Exception e){
            System.out.println("Error en Consultar: " + e.getMessage());
            return lista;
        }
    }
    @GET
    @Path("/lista")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Usuario> listar() {
        return(Consultar());
    }
    
    // --- MÉTODO INSERT (CREAR) ---
    @POST
    @Path("/agregar")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public int agregar(Usuario u) {
        String sql = "INSERT INTO usuario (nombre_usuario, contrasena, nombre_completo, rol) VALUES (?, ?, ?, ?)";
        try {
            con = cn.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, u.getNombreUsuario());
            ps.setString(2, u.getContrasena());
            ps.setString(3, u.getNombreCompleto());
            ps.setString(4, u.getRol());
            
            ps.executeUpdate();
            return 1; // 1 significa que se insertó con éxito
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return 0; // 0 significa que hubo un error
        }
    }
}