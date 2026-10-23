# Sistema de Gestión de Infracciones de Tránsito

Aplicación web para administrar un catálogo de sanciones de tránsito y consultar los usuarios registrados en el sistema.

## Descripción

Este proyecto implementa una solución web para la gestión de infracciones de tránsito. Incluye un panel de administración con autenticación de usuarios, consulta de información en tiempo real y operaciones CRUD sobre el catálogo de sanciones.

La interfaz está diseñada con Bootstrap 5 y JavaScript, mientras que el backend está desarrollado en Java utilizando una aplicación web Java EE desplegable como archivo WAR.

## Funcionalidades

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

4. Configura la conexión a MySQL en la clase o archivo correspondiente del proyecto.

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

Proyecto académico de desarrollo web con Java, orientado a la administración de sanciones de tránsito y usuarios.

## Mejoras futuras

- Incorporar una base de datos de ejemplo o scripts SQL de instalación.
- Separar las credenciales de base de datos de la configuración del código.
- Agregar validaciones centralizadas en el backend.
- Implementar paginación para listados grandes.
- Incorporar filtros adicionales para usuarios y sanciones.
- Añadir pruebas automatizadas.
- Documentar los endpoints disponibles.
- Mejorar la gestión de permisos por módulo.

## Autoría

Proyecto desarrollado como práctica académica de desarrollo de aplicaciones web.
