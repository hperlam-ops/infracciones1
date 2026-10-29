/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servicio;
import java.sql.*;


/**
 *
 * @author henry
 */
public class Conexion {
    Connection con;
    public Conexion(){
        try{
            cargarDriver();
            String url = valor("DB_URL", "jdbc:mysql://localhost:3306/infraccion_db");
            String usuario = valor("DB_USER", "root");
            String contrasena = valor("DB_PASSWORD", "");
            con = DriverManager.getConnection(url, usuario, contrasena);
            System.out.println("Conexion con exito");
        }
        catch(Exception e){
            System.err.println("Error de conexión a MySQL: " + e.getMessage());
        }
    }
    public Connection getConnection() {
        return con;
}
    private void cargarDriver() throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Class.forName("com.mysql.jdbc.Driver");
        }
    }

    private String valor(String nombre, String defecto) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.trim().isEmpty()) {
            valor = System.getProperty(nombre);
        }
        return valor == null || valor.trim().isEmpty() ? defecto : valor.trim();
    }
 }
    
