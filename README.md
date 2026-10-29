# Sistema de Gestión de Infracciones de Tránsito

Aplicación web beta para administrar conductores, vehículos, sanciones e infracciones
y permitir que usuarios normales consulten sus infracciones por placa o DPI.

## Descripción

Este proyecto implementa una solución web para la gestión de infracciones de tránsito. Incluye un panel de administración con autenticación de usuarios, consulta de información en tiempo real y operaciones CRUD sobre el catálogo de sanciones.

La interfaz está diseñada con Bootstrap 5 y JavaScript, mientras que el backend está desarrollado en Java utilizando una aplicación web Java EE desplegable como archivo WAR.

## Funcionalidades actuales

### Gestión de sanciones

- Listar sanciones registradas.
- Crear nuevas sanciones.
- Editar descripciones y costos.
- Eliminar sanciones con confirmación.
- Buscar sanciones por descripción o identificador.
- Ordenar por ID, descripción o costo.
- Mostrar el total de sanciones registradas.
- Actualizar el listado desde el servidor.

### Administración de usuarios

- Inicio de sesión.
- Visualización del usuario activo.
- Cierre de sesión.
- Consulta de usuarios registrados.
- Visualización de roles y permisos.

### Gestión administrativa

- CRUD de conductores.
- Creación y asignación de vehículos a conductores.
- Registro de infracciones buscando el vehículo por placa.
- Asociación automática del conductor asignado al vehículo.
- Consulta administrativa del historial de pagos confirmados y del total recaudado.
- CRUD del catálogo de tipos de vehículo.
- CRUD de agentes de tránsito que registran infracciones.

### Portal de usuario y pagos beta

- Consulta de infracciones por placa o DPI.
- Visualización de datos del conductor aunque no tenga infracciones.
- Generación de vouchers de pago simulados.
- Confirmación simulada del pago y conservación del historial de la infracción.

### Autenticación

- Inicio de sesión local.
- Inicio de sesión con Google OAuth.
- Separación entre panel administrativo y portal de usuario.
- Esta versión beta no incluye recuperación de contraseñas.

### Interfaz de usuario

- Panel administrativo.
- Navegación entre módulos.
- Diseño responsive.
- Componentes de Bootstrap 5.
- Iconos de Bootstrap Icons.
- Mensajes de éxito y error.
- Modales para confirmar eliminaciones.
- Estados de carga y manejo de errores.
- Formatos de moneda para quetzales guatemaltecos.
- Consideraciones de accesibilidad con etiquetas ARIA y navegación por teclado.

## Tecnologías

- Java 8
- Java EE / aplicación web Java
- JSP
- HTML5
- CSS3
- JavaScript
- Bootstrap 5.3
- Bootstrap Icons
- MySQL
- JDBC
- Apache Ant
- NetBeans
- GlassFish 5.1

## Arquitectura

El proyecto está organizado en capas para separar las responsabilidades de la aplicación:

- `src/java/controlador`: controladores de la aplicación.
- `src/java/servicio`: servicios, conexión a base de datos y lógica de negocio.
- `web/vistas`: páginas y vistas de la interfaz.
- `web/js`: autenticación y comunicación con la API.
- `web/css`: estilos y tema visual.
- `web/WEB-INF`: configuración de despliegue de la aplicación.

## Módulos principales

- Panel de administración
- Catálogo de sanciones
- Consulta de usuarios
- Autenticación y sesiones
- Comunicación con servicios backend
- Conexión con base de datos

La vista de pagos permite consultar y buscar los pagos confirmados; la
generación de vouchers y la confirmación simulada se realizan desde el portal
de usuario.

## Requisitos

- JDK 8 o compatible con la configuración del proyecto.
- NetBeans u otro IDE compatible con proyectos Ant.
- GlassFish 5.1 o un servidor compatible con Java EE 8.
- MySQL.
- Driver JDBC de MySQL.

> El proyecto fue configurado originalmente para Java 8 y GlassFish. Puede requerir ajustes de dependencias o del servidor si se ejecuta con versiones diferentes.

## Ejecución

1. Clona el repositorio:

   ```bash
   git clone https://github.com/hperlam-ops/infracciones1.git
   ```

2. Abre el proyecto en NetBeans.

3. Configura el servidor GlassFish y selecciónalo como servidor de ejecución.

4. Configura MySQL y verifica que exista la base de datos `infraccion_db`.
   La conexión usa estos valores por defecto:

   - URL: `jdbc:mysql://localhost:3306/infraccion_db`
   - Usuario: `root`
   - Contraseña: vacía

   También acepta las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`.

5. Verifica que el driver JDBC de MySQL esté disponible en las librerías del proyecto.

6. Ejecuta el proyecto desde NetBeans o genera el archivo `.war` con Apache Ant.

7. Abre la aplicación desde la URL proporcionada por el servidor de aplicaciones.

## Construcción con Apache Ant

Para generar la aplicación, utiliza el archivo `build.xml` incluido en el repositorio:

```bash
ant clean
ant
```

El archivo WAR generado puede encontrarse en la carpeta de distribución configurada por el proyecto.

## Estado del proyecto

Proyecto académico de desarrollo web con Java. La aplicación se encuentra en
fase beta; los módulos principales de usuarios, conductores, vehículos,
sanciones, infracciones y portal de consulta están implementados.

## Mejoras futuras

- Incorporar una base de datos de ejemplo o scripts SQL de instalación.
- Separar completamente las credenciales de base de datos de la configuración del código.
- Agregar validaciones centralizadas en el backend.
- Implementar paginación para listados grandes.
- Incorporar filtros adicionales para usuarios y sanciones.
- Añadir pruebas automatizadas.
- Documentar los endpoints disponibles.
- Mejorar la gestión de permisos por módulo.

## Autoría

Proyecto desarrollado como práctica académica de desarrollo de aplicaciones web.
