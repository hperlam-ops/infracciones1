package servicio;

public class LoginResponse {
    private boolean autenticado;
    private int idUsuario;
    private String nombreUsuario;
    private String nombreCompleto;
    private String rol;

    public LoginResponse() {
    }

    public LoginResponse(boolean autenticado, int idUsuario, String nombreUsuario,
            String nombreCompleto, String rol) {
        this.autenticado = autenticado;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public boolean isAutenticado() {
        return autenticado;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }
}
