/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package servicio;

/**
 *
 * @author henry
 */
public class Sancion {
    private int idSancion;
    private String descripcion;
    private double costo;

    // Constructores, Getters y Setters
    public Sancion() {}

    public int getIdSancion() { return idSancion; }
    public void setIdSancion(int idSancion) { this.idSancion = idSancion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }
}

