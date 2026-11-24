# Sistema Web de Gestión de Inventario y Entradas de Bodega

Proyecto académico de la **Universidad Técnica de Ambato**, **FISEI**, carrera de **Ingeniería en Software**, asignatura **Aplicaciones Orientadas a Servicios (SOA)**.

Aplicación MVC para administrar productos, registrar recepciones y mantener sus existencias e historial. El cliente se renderiza en el servidor con Thymeleaf. La instalación inicial incluye catálogos y proveedores de demostración, y **un solo producto de ejemplo con stock cero**, para practicar el registro de entradas. Los datos de ejemplo se identifican con `DEMO`.

Repositorio del equipo: **https://github.com/ManuelCusme/ProyectoSOA**.

Cada integrante ejecuta el proyecto contra su propia base local. El repositorio incluye el código y el SQL de creación con sus datos iniciales; cada compañero genera sus propias cuentas y contraseñas privadas para la importación. Los registros y las credenciales de otra instalación no se descargan al clonar.

## Inicio rápido para el equipo con IntelliJ IDEA

### 1. Requisitos de cada equipo

- **JDK 21** instalado; compruebe con `java -version`.
- **IntelliJ IDEA**, con soporte de Java y Maven. Se puede ejecutar como aplicación Java sin una configuración especial de Spring del IDE.
- **Git** para clonar y actualizar el repositorio.
- **XAMPP**, con el módulo **MySQL** activo, o un servidor MySQL accesible.
- Internet para descargar Maven y las dependencias la primera vez. No se necesita instalar Maven globalmente: se incluye el Wrapper.

### 2. Clonar y abrir el proyecto existente

En una terminal, dentro de la carpeta donde guardan sus proyectos:

```powershell
git clone https://github.com/ManuelCusme/ProyectoSOA.git
cd ProyectoSOA
```

En IntelliJ, seleccione **File → Open**, elija el **`pom.xml`** de la carpeta clonada y ábralo como proyecto. Espere a que Maven termine de sincronizar las dependencias. Si no se detecta Maven, haga clic derecho sobre `pom.xml` y seleccione **Add as Maven Project**.

En **File → Project Structure → Project**, seleccione **SDK 21** y el nivel de lenguaje 21. En **Settings → Build, Execution, Deployment → Build Tools → Maven**, utilice **Maven Wrapper**; compruebe también que el JDK del importador y el JRE del Runner sean Java 21. Si faltan clases o dependencias, use **Reload All Maven Projects** en la ventana Maven.

### 3. Encender MySQL y configurar el puerto de cada compañero

En XAMPP Control Panel, inicie **MySQL** y revise su puerto. En muchas instalaciones es **3306**; el equipo donde se desarrolló usa **3307**. Utilice el puerto real de su equipo. **Apache solo es necesario para phpMyAdmin; la aplicación Java funciona en su propio servidor, puerto 8080.**

En la terminal PowerShell de IntelliJ, ubicada en la raíz `ProyectoSOA`, genere la configuración privada:

```powershell
# Cambie 3306 por 3307 si ese es el puerto de su XAMPP.
.\scripts\configurar-local.ps1 -PuertoBaseDatos 3306
```

El script crea **`.local/application.properties`** con el usuario inicial `administrador`, una contraseña aleatoria y la conexión MySQL. **Abra ese archivo para consultar la contraseña inicial y ajustar `spring.datasource.username` / `spring.datasource.password` a su servidor.** Si usa el root local de XAMPP sin contraseña, puede conservar esos valores. Si el archivo ya existe, el script lo conserva; edítelo para cambiar el puerto o las credenciales.

Si Windows bloquea la ejecución de scripts, puede completar la configuración manualmente, sin modificar la política de PowerShell:

```powershell
New-Item -ItemType Directory -Path .local -Force
# Ejecute la copia solo si todavía no existe el archivo local.
Copy-Item .local.example.properties .local/application.properties
```

En el archivo copiado, complete `app.bootstrap.username`, `app.bootstrap.password` y la conexión JDBC con su puerto. Elija una contraseña de **al menos 10 caracteres y hasta 72 bytes UTF-8**. Puede usar este mismo procedimiento manual desde IntelliJ en otros sistemas operativos. No deje vacíos los campos del administrador al iniciar con una base sin usuarios.

**No suba `.local/application.properties` ni comparta sus contraseñas en el repositorio.** `.gitignore` excluye toda la carpeta `.local/`. Cada compañero tiene su propia contraseña; no hay una clave universal para el equipo.

### 4. Preparar e importar toda la base en phpMyAdmin

Con el archivo privado del paso 3 configurado, ejecute desde la raíz:

```powershell
.\scripts\preparar-importacion.ps1
```

Este comando utiliza **PHP incluido en XAMPP únicamente para generar hashes BCrypt**. La aplicación web sigue funcionando con Java. Si XAMPP está instalado en otra ubicación, indique por ejemplo `-RutaPhp 'D:\xampp\php\php.exe'`.

Se generan dos archivos dentro de `.local/`, ambos ignorados por Git:

- **`importar-inventario.sql`**: crea la base `inventario_bodega`, las seis tablas, los datos iniciales y las cuentas ADMIN y BODEGUERO con hashes BCrypt.
- **`credenciales-importacion.properties`**: guarda los usuarios y contraseñas para iniciar sesión. El administrador usa la clave local configurada; el bodeguero recibe una clave aleatoria propia de esa instalación.

Las claves de ese archivo son **contraseñas reales para escribir o pegar en el login**, aunque contengan letras aleatorias, `/` o `+`. En cambio, el campo `usuarios.password` de la base contiene un **hash BCrypt** que comienza por `$2a$`, `$2b$` o `$2y$`: ese hash no se introduce como contraseña en el formulario.

Encienda también **Apache** en XAMPP y abra **http://localhost/phpmyadmin** (ajuste el puerto de Apache si corresponde). Desde la página principal del servidor, entre en **Importar**, seleccione **`.local/importar-inventario.sql`**, mantenga el formato **SQL** y la codificación **utf-8**, y ejecute la importación. **No necesita crear ni seleccionar la base previamente**: el archivo incluye `CREATE DATABASE` y `USE inventario_bodega`.

Después, seleccione `inventario_bodega` en el panel izquierdo y compruebe las seis tablas. La sección **Base de datos e importación completa en phpMyAdmin** explica los registros creados, la importación directa de `schema.sql` y qué ocurre si la base ya contiene información.

**El SQL generado contiene hashes privados y el archivo de credenciales contiene contraseñas: no publique ninguno.** Si vuelve a ejecutar el generador, conserva las claves ya generadas y vuelve a preparar el SQL con el `schema.sql` actual. Importarlo sobre usuarios existentes no cambia sus contraseñas.

Si la política de su equipo impide ejecutar también este segundo script, puede importar directamente `src/main/resources/schema.sql`, iniciar Java con la configuración privada y crear al bodeguero desde **Usuarios**, como se explica en la guía de importación.

### 5. Ejecutar desde IntelliJ

El repositorio incluye la configuración compartida **InventarioBodega** en `.run/InventarioBodega.run.xml`. Selecciónela en el menú de ejecución de IntelliJ y pulse **Run**. Utiliza la clase principal y la raíz del proyecto como directorio de trabajo, sin incluir credenciales.

También puede abrir:

```text
src/main/java/ec/edu/uta/inventariobodega/InventariobodegaApplication.java
```

y pulsar el botón verde junto al método `main`. Si necesita configurar la ejecución manualmente en **Run → Edit Configurations → Application**, use:

| Opción                  | Valor                                                     |
| ----------------------- | --------------------------------------------------------- |
| Main class              | `ec.edu.uta.inventariobodega.InventariobodegaApplication` |
| JRE                     | Java 21 / Project SDK 21                                  |
| Use classpath of module | `inventariobodega` (módulo importado desde Maven)         |
| Working directory       | `$PROJECT_DIR$`, la carpeta donde está `pom.xml`          |

El directorio de trabajo es importante: Spring lee el archivo privado con la ruta relativa `./.local/application.properties`. No coloque las credenciales dentro de `src/main/resources/application.properties`.

Como alternativa, desde la terminal de IntelliJ:

```powershell
.\mvnw.cmd spring-boot:run
```

Ejecute **una sola instancia**: use el botón Run o el comando de terminal. Al iniciar, la aplicación ejecuta el mismo `schema.sql` y valida las tablas; una base ya importada conserva sus datos. Si omitió la importación, Spring también puede crear la base y cargar los ejemplos cuando los catálogos están vacíos, y crea el primer administrador desde la configuración local si `usuarios` está vacía. Para recibir también el bodeguero inicial antes del primer arranque, utilice la importación completa del paso 4.

### 6. Entrar y comprobar el funcionamiento

Abra **http://localhost:8080/login**. Si realizó la importación completa, consulte los usuarios y contraseñas en **`.local/credenciales-importacion.properties`**. Si usó solo el arranque automático, ingrese con el administrador de `.local/application.properties`.

1. Revise los catálogos iniciales y el producto **`DEMO-001`**, con stock actual `0` y stock mínimo `5`. También puede crear sus propios catálogos y productos.
2. Registre una entrada de `10` unidades para ese producto, seleccionando uno de los proveedores de ejemplo. El inventario debe mostrar `10`.
3. Registre otra de `3`. El stock debe quedar en `13` y el historial debe tener ambas entradas con fecha y responsable.
4. Cierre sesión e ingrese como **bodeguero**, con su clave privada generada. Podrá consultar y registrar entradas; al visitar `/admin/usuarios` se le denegará el acceso. Si utilizó el arranque sin importación de cuentas, cree primero ese usuario desde **Usuarios**.
5. Como administrador, intente eliminar el producto con historial: se desactivará conservando las entradas.

Las entradas de `10` y `3` son ejercicios para registrar manualmente; el SQL **no inventa entradas ni stock**. Cambiar la contraseña de inicialización en un archivo local **no cambia una cuenta ya creada**; para eso use **Usuarios → Editar acceso** desde una cuenta administradora.

### 7. Detener, volver a iniciar y actualizar

Detenga la aplicación con el botón rojo **Stop** de IntelliJ o **Ctrl+C** si la inició desde la terminal. La base conserva los registros; las sesiones del navegador se cierran al reiniciar.

Antes de trabajar con cambios nuevos del equipo:

```powershell
git status
# Guarde o confirme sus cambios locales antes de actualizar.
git pull --ff-only origin main
```

Después, recargue Maven si cambió `pom.xml` y vuelva a ejecutar. Su archivo privado permanece local. `target/`, `out/`, `.idea/`, logs y credenciales se excluyen del repositorio y se generan o configuran en cada equipo.

## Problemas comunes en IntelliJ y XAMPP

| Síntoma                                                           | Qué revisar                                                                                                                                       |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `release version 21 not supported` / `invalid target release: 21` | Project SDK, JDK del importador y Runner de Maven deben ser JDK 21. Confirme también `java -version` en la terminal usada.                        |
| Dependencias en rojo / `Cannot resolve symbol`                    | Conexión a Internet y **Reload All Maven Projects**. Abra el `pom.xml` existente como proyecto Maven.                                             |
| `Connection refused` / `Communications link failure`              | Inicie MySQL en XAMPP y compruebe host y puerto en `.local/application.properties` (3306 o 3307 según su instalación).                            |
| `Access denied for user`                                          | Ajuste el usuario y contraseña de la base; son distintos a las credenciales del login de la aplicación.                                           |
| `Base vacía: configure APP_ADMIN_USERNAME...`                     | Complete el administrador inicial y confirme que Working directory sea la raíz para encontrar `.local/application.properties`.                    |
| La contraseña inicial no permite entrar                           | Use la de su archivo local, no la de otro compañero. Si la cuenta ya existe, el inicializador no cambia su contraseña.                            |
| `Port 8080 was already in use`                                    | Detenga la otra instancia de la aplicación o cambie `PORT` a 8081 en la configuración de ejecución. Abra entonces `http://localhost:8081/login`.  |
| Error de creación de base o de tablas por permisos                | Cree la base e importe el esquema desde phpMyAdmin como se explica más abajo, o use un usuario con permisos sobre `inventario_bodega`.            |
| El script de PowerShell no se puede ejecutar                      | Utilice la copia manual de `.local.example.properties` indicada en el paso 3.                                                                     |
| Cambiar variables en la terminal no afecta al botón Run           | Configure **Environment variables** en **Run → Edit Configurations**; una variable puesta en una terminal no configura la ejecución Java del IDE. |

## Tecnologías

- Java 21 y Maven Wrapper incluido.
- Spring Boot **4.1.2**, conservado del proyecto original.
- Spring MVC, Spring Data JPA, Hibernate, Spring Security y Bean Validation.
- Thymeleaf, HTML5, CSS3 y Bootstrap **5.3.8**, servido localmente con su licencia MIT.
- Conexión MySQL a XAMPP mediante **MySQL Connector/J**, en **127.0.0.1:3307**.
- JUnit, Mockito, AssertJ y MockMvc, mediante las dependencias de pruebas de Spring Boot existentes.

La configuración utiliza `jdbc:mysql://...` y el controlador `com.mysql.cj.jdbc.Driver`. Los bloqueos se ejecutan mediante consultas `SELECT ... FOR UPDATE`, compatibles con MySQL y con el servidor de XAMPP, para evitar la cláusula `FOR UPDATE OF` que Hibernate generaba en este entorno. Se utiliza únicamente la dependencia JDBC `mysql-connector-j` del proyecto original.

## Configurar Java 21 en la terminal (opcional)

1. Instale un **JDK 21**. En PowerShell, ajuste las rutas a su instalación:

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   java -version
   .\mvnw.cmd -version
   ```

2. Abra **la carpeta clonada** en IntelliJ como se indica arriba. También se puede trabajar desde VS Code; Maven funciona sin extensiones del editor.
3. Abra XAMPP Control Panel e inicie su módulo **MySQL** (en esta instalación ejecuta MariaDB). Confirme el puerto en `C:\xampp\mysql\bin\my.ini`; aquí es **3307**, mientras otras instalaciones usan 3306.
4. Si desea usar phpMyAdmin, inicie también Apache y abra `http://localhost/phpmyadmin`. Su puerto de Apache puede ser diferente.

## Base de datos e importación completa en phpMyAdmin

La base se llama obligatoriamente **`inventario_bodega`**. El parámetro JDBC `createDatabaseIfNotExist=true` permite crearla al iniciar si el usuario tiene permisos. Si el servidor no responde, la aplicación falla con el error de conexión; no usa H2 ni otra base sustituta.

El archivo [`src/main/resources/schema.sql`](src/main/resources/schema.sql) es la definición única de **la base, las seis tablas y los datos iniciales**. Incluye `CREATE DATABASE IF NOT EXISTS inventario_bodega`, `USE inventario_bodega`, las tablas con sus relaciones y las inserciones de inicio. No contiene contraseñas fijas.

El generador [`scripts/preparar-importacion.ps1`](scripts/preparar-importacion.ps1) **antepone al mismo SQL** los nombres de las dos cuentas y sus hashes BCrypt privados. No mantiene una segunda definición de tablas. El SQL completo se escribe en `.local/importar-inventario.sql`.

Para una instalación completa desde cero:

1. Configure Java 21 y el archivo privado `.local/application.properties`, como indica el inicio rápido.
2. Ejecute `scripts/preparar-importacion.ps1`. Requiere el `php.exe` de XAMPP; no necesita instalar bibliotecas adicionales.
3. Inicie MySQL y Apache en XAMPP. Entre a phpMyAdmin con un usuario que pueda crear `inventario_bodega` y sus tablas.
4. Desde la pantalla principal del servidor, seleccione **Importar → Seleccionar archivo → `.local/importar-inventario.sql`**. Use formato **SQL** y codificación **utf-8**, y pulse **Importar / Continuar**, según su versión.
5. Verifique el mensaje de ejecución correcta y seleccione `inventario_bodega` en el panel lateral.
6. Abra **Examinar** en cada tabla y compruebe los datos de la siguiente tabla.
7. Ejecute `InventarioBodega` en IntelliJ. Entre al login con los datos de `.local/credenciales-importacion.properties`.

En una base sin información previa, la importación completa deja:

| Tabla                 | Registros iniciales                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------ |
| `usuarios`            | 2 cuentas activas: administrador (`ADMIN`) y bodeguero (`BODEGUERO`), con sus claves privadas.   |
| `categorias`          | 3: `DEMO - Papelería`, `DEMO - Herramientas`, `DEMO - Limpieza`.                                 |
| `marcas`              | 2: `DEMO - Genérica`, `DEMO - Oficina`.                                                          |
| `proveedores`         | 2 proveedores `DEMO`, con correos de ejemplo terminados en `.invalid` y sin teléfonos reales.    |
| `productos`           | **1**: `DEMO-001`, nombre `Producto de ejemplo`, stock actual **0**, stock mínimo **5**, activo. |
| `entradas_inventario` | **0**. Las entradas se registran desde la aplicación con fecha y responsable reales.             |

**Si importa directamente `src/main/resources/schema.sql`**, se crean igualmente la base, las tablas, los catálogos, los proveedores y el producto. Las cuentas se omiten porque el archivo público no contiene hashes de contraseñas universales: el administrador se creará al iniciar Java con la configuración local y podrá crear al bodeguero desde **Usuarios**. Para importar también ambas cuentas en un solo paso, use el SQL generado de `.local/`.

Si ya hay datos, el SQL no borra ni actualiza registros existentes. **Cada usuario se inserta por separado si falta su nombre de usuario** y el SQL recibe los hashes privados válidos. Si ya existe `administrador`, la importación puede crear `bodeguero` sin cambiar la contraseña, el rol ni el estado del administrador. Las cuentas existentes conservan sus claves: el archivo local no recupera una contraseña que se haya cambiado posteriormente desde la aplicación.

La importación privada generada solicita **completar los ejemplos faltantes**, buscando categorías, marcas y proveedores por nombre y el producto por código `DEMO-001`. Por eso también funciona sobre una base con datos propios: añade los ejemplos que falten y conserva los productos, stock y entradas existentes. Si ejecuta de nuevo esa importación manual, puede restaurar un ejemplo `DEMO` que hubiera eliminado o renombrado; no modifica registros ya encontrados. En cambio, el arranque automático con el `schema.sql` público solo carga ejemplos cuando **categorías, marcas, proveedores y productos están todos vacíos**, para no reponerlos en cada reinicio.

Si ya había arrancado Java y ve solamente al administrador en la tabla `usuarios`, ejecute otra vez `scripts/preparar-importacion.ps1` e importe el nuevo `.local/importar-inventario.sql`: ahora se completa la cuenta que falta aunque la tabla tenga usuarios.

Spring ejecuta este mismo `schema.sql` al iniciar. Hibernate usa **`ddl-auto=validate`**: comprueba las correspondencias, sin crear ni modificar tablas por su cuenta. No cambie esta propiedad a `create` o `create-drop`. [`database/crear-base.sql`](database/crear-base.sql) queda como opción para preparar únicamente la base y sus permisos; **no es un paso previo obligatorio** para importar el SQL completo.

Si ya existe un esquema incompatible, la validación informa el problema; el sistema no elimina ni migra datos existentes automáticamente.

Solo hay seis tablas de negocio, todas InnoDB:

| Tabla                 | Contenido y relaciones                                                                      |
| --------------------- | ------------------------------------------------------------------------------------------- |
| `usuarios`            | Usuario único, contraseña BCrypt, enum `ADMIN` o `BODEGUERO`, estado activo.                |
| `categorias`          | Nombre único. Una categoría puede tener varios productos.                                   |
| `marcas`              | Nombre único. Una marca puede tener varios productos.                                       |
| `proveedores`         | Nombre, teléfono y correo. Un proveedor puede aparecer en varias entradas.                  |
| `productos`           | Código único, nombre, descripción, stock actual y mínimo, categoría, marca y estado activo. |
| `entradas_inventario` | Fecha, cantidad, observación y referencias a producto, proveedor y usuario responsable.     |

Cada entrada pertenece a un producto, proveedor y usuario; cada uno puede tener muchas entradas. El proveedor se relaciona con la entrada y no con el producto. JPA usa relaciones `@ManyToOne` unidireccionales con carga diferida, y `@EntityGraph` en las consultas que muestran sus nombres. Esto representa las relaciones 1:N sin colecciones circulares. Las claves foráneas protegen la integridad.

No se crean tablas de roles, sesiones, caché ni auditoría. Las sesiones HTTP residen en memoria del servidor.

## Configuración privada y primer administrador

No hay una contraseña administrativa fija en el código. La importación privada puede crear las dos cuentas con BCrypt antes de arrancar Java. Si inicia sin esas cuentas y `usuarios` está vacía, el inicializador crea **una cuenta ADMIN activa** usando configuración privada y almacena únicamente su hash BCrypt. Si faltan las credenciales, el inicio se detiene con un mensaje de configuración. Cuando hay usuarios, el inicializador no reemplaza sus credenciales.

### Opción A: archivo local con contraseña aleatoria

Desde la raíz del proyecto:

```powershell
.\scripts\configurar-local.ps1
# Indique el puerto de su instalación de XAMPP:
# .\scripts\configurar-local.ps1 -PuertoBaseDatos 3306 -UsuarioAdministrador administrador
```

El script crea **`.local/application.properties`**, ignorado por Git, con un usuario `administrador`, una contraseña aleatoria y conexión MySQL a XAMPP. No sobrescribe un archivo existente. Abra ese archivo en VS Code para consultar su contraseña y ajustar las credenciales de la base. También puede copiar `.local.example.properties` a esa ubicación y completarlo manualmente.

Las credenciales guardadas en este archivo son privadas. No comparta el archivo ni lo incluya en la entrega pública. Al cambiar una contraseña de usuario después del primer arranque, utilice **Usuarios → Editar acceso**; editar las credenciales de inicialización no modifica una cuenta existente.

### Opción B: variables de entorno

Si no tiene el archivo local anterior, configure en la misma terminal donde ejecutará Maven:

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3307'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '' # Adapte este valor a su instalación local.
$env:APP_ADMIN_USERNAME = 'administrador'
# Solicita la contraseña sin escribirla en el historial de la terminal.
$credencialInicial = Get-Credential -UserName $env:APP_ADMIN_USERNAME -Message 'Defina la contraseña inicial (mínimo 10 caracteres)'
$env:APP_ADMIN_PASSWORD = $credencialInicial.GetNetworkCredential().Password
```

La conexión ya usa el protocolo y controlador de MySQL. Para conectar a otra instalación MySQL, configure su host, puerto y credenciales. Las verificaciones se realizan contra XAMPP; no se verificó otro servidor independiente.

El archivo local importado puede definir directamente las propiedades y prevalecer sobre los valores predeterminados basados en `DB_*` y `APP_ADMIN_*`. Si cambia de método de configuración, ajuste o retire las propiedades equivalentes de ese archivo privado. También se aceptan las variables estándar `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.

Al usar el botón Run de IntelliJ, introduzca estas variables en **Run → Edit Configurations → Environment variables**, dentro de la configuración de la aplicación. No copie contraseñas a la configuración compartida de `.run/` ni confirme configuraciones privadas del IDE.

## Compilar, probar y ejecutar

Ejecute desde la raíz:

```powershell
# Compilación sin ejecutar pruebas
.\mvnw.cmd -B -DskipTests compile

# Pruebas unitarias; la clase de integración se omite por defecto
.\mvnw.cmd -B test

# Pruebas completas contra XAMPP real; requiere conexión y permisos
$env:RUN_DB_TESTS = 'true'
.\mvnw.cmd -B test

# Empaquetar el JAR, incluyendo pruebas si RUN_DB_TESTS sigue activo
.\mvnw.cmd -B clean verify

# Ejecutar en desarrollo
.\mvnw.cmd spring-boot:run

# Alternativa: ejecutar el JAR compilado
java -jar .\target\inventariobodega-0.0.1-SNAPSHOT.jar
```

Abra **http://localhost:8080/login** e ingrese con la cuenta inicial configurada. Puede cambiar el puerto HTTP con `$env:PORT='8081'`. Detenga el proceso con **Ctrl+C**. Ejecute desde la raíz para que se encuentre `.local/application.properties`.

En Linux/macOS, los equivalentes son `./mvnw test`, `RUN_DB_TESTS=true ./mvnw test` y `./mvnw spring-boot:run`; configure Java 21 y las variables con la sintaxis de su terminal.

## Roles y funciones

| Función                                                 | ADMINISTRADOR (`ROLE_ADMIN`) | BODEGUERO (`ROLE_BODEGUERO`) |
| ------------------------------------------------------- | ---------------------------- | ---------------------------- |
| Iniciar y cerrar sesión                                 | Sí                           | Sí                           |
| Consultar productos, buscar por código/nombre/categoría | Sí                           | Sí                           |
| Consultar inventario e historial                        | Sí                           | Sí                           |
| Registrar entradas                                      | Sí                           | Sí                           |
| Crear/editar/eliminar o desactivar productos            | Sí                           | No                           |
| Gestionar categorías, marcas y proveedores              | Sí                           | No                           |
| Gestionar usuarios, roles y activación                  | Sí                           | No                           |

La pantalla inicial ofrece acceso a las tareas de bodega. Productos e inventario incluyen búsqueda y paginación de 15 elementos. El historial también se pagina. Los formularios y mensajes están en español, y Bootstrap y los estilos se sirven localmente.

Orden de uso sugerido:

1. Como administrador, cree categorías, marcas y proveedores.
2. Cree los productos indicando su stock mínimo. El stock actual comienza en cero.
3. Cree cuentas de bodeguero desde **Usuarios**.
4. Registre una entrada eligiendo un producto activo, proveedor y cantidad positiva.
5. Compruebe el inventario actualizado y la entrada en el historial.

## Cómo funciona una entrada

`EntradaController` recibe un DTO validado y obtiene el nombre del usuario desde la sesión autenticada. No acepta fecha ni responsable enviados por el navegador.

`EntradaService.registrar()` usa **`@Transactional`** y realiza lo siguiente:

1. Comprueba la cantidad positiva y los datos obligatorios.
2. Lee el producto con **`SELECT ... FOR UPDATE`**, un bloqueo pesimista que mantiene esa fila de InnoDB bloqueada hasta finalizar la transacción.
3. Comprueba que esté activo y que existan proveedor y usuario activo.
4. Calcula el nuevo stock, comprobando que la suma no desborde un entero.
5. Asigna la fecha del servidor en la zona **America/Guayaquil**, crea la entrada e incrementa las existencias.
6. Confirma ambas escrituras juntas. Si falla alguna, la transacción revierte todos los cambios.

Si dos personas registran entradas del mismo producto, una espera el bloqueo de la otra, lee después el stock confirmado y suma su cantidad. Así se evita que una actualización borre la otra. Las ediciones y eliminaciones del producto toman el mismo bloqueo.

Las entradas no tienen rutas de modificación ni eliminación. La edición de productos usa un DTO sin `stockActual`; enviar ese parámetro manualmente no altera las existencias. Un producto con historial se desactiva en lugar de borrarse. Las categorías, marcas y proveedores referenciados no pueden eliminarse y muestran mensajes comprensibles.

## Seguridad

- Formulario de autenticación de Spring Security y sesiones HTTP de 30 minutos de inactividad.
- BCrypt para todas las contraseñas. Se exigen al menos 10 caracteres y un máximo de 72 bytes UTF-8 para evitar truncamiento de BCrypt.
- Rutas `/admin/**` restringidas a `ROLE_ADMIN`, tanto para lectura como para escritura.
- CSRF activo. Thymeleaf incorpora el token en formularios POST; el cierre de sesión también es POST.
- El menú adapta las opciones al rol, y los permisos se comprueban también en el servidor.
- Usuarios inactivos rechazados al autenticar. Un filtro revoca sesiones existentes si cambian estado, rol, usuario o contraseña.
- Un administrador no puede desactivarse a sí mismo ni quitarse su propio rol. Los cambios de usuarios se serializan para conservar un administrador activo.
- Validación de formularios, DTO independientes de las entidades, restricciones SQL y mensajes de error sin exponer trazas al usuario.

La configuración de conexión predeterminada corresponde al desarrollo local en XAMPP. La contraseña de base debe ajustarse si su instalación no usa la cuenta local predeterminada.

## Estructura principal

```text
src/main/java/ec/edu/uta/inventariobodega/
  InventariobodegaApplication.java
  entity/       Las seis entidades y el enum Rol
  repository/   Seis repositorios JPA y consultas con bloqueos
  service/      Reglas de negocio y transacciones
  dto/          Formularios validados, sin stock actual ni responsable editable
  security/     Autenticación, autorización, revocación e inicializador
  controller/   Rutas MVC y manejo de errores
src/main/resources/
  application.properties
  schema.sql
  ValidationMessages.properties
  templates/    Login, inicio, productos, entradas, catálogos, usuarios y errores
  static/css/   Estilos de la interfaz
  static/vendor/ Bootstrap y licencia
src/test/java/ec/edu/uta/inventariobodega/
  EntradaServiceTests.java
  ValidacionTests.java
  BaseDatosIntegrationTests.java
database/crear-base.sql
scripts/configurar-local.ps1
scripts/preparar-importacion.ps1  Genera el SQL completo con cuentas privadas
scripts/generar-hashes.php      Herramienta local de XAMPP para BCrypt
.local.example.properties
.run/InventarioBodega.run.xml  Configuración IntelliJ compartida, sin secretos
```

## Pruebas y resultados

Las pruebas unitarias usan objetos simulados y no requieren base de datos. Las de integración cargan la aplicación, ejecutan MVC con Spring Security y Thymeleaf y usan **la base real de XAMPP a través de MySQL Connector/J**. Solo se habilitan con `RUN_DB_TESTS=true`.

Las pruebas de integración crean datos identificados con un prefijo aleatorio `test_` y eliminan únicamente sus propios registros al terminar. No vacían tablas ni eliminan bases; los ejemplos iniciales del esquema se conservan. Requieren permisos de lectura, escritura y eliminación sobre `inventario_bodega`; la cuenta inicial se prepara mediante el procedimiento descrito arriba.

Los informes detallados de la última ejecución están en `target/surefire-reports/`. El resultado final y las comprobaciones HTTP/navegador se registran también en [`docs/VALIDACION.md`](docs/VALIDACION.md).

Última ejecución, **24 de noviembre de 2026**, con `jdbc:mysql` y MySQL Connector/J sobre XAMPP: **36 pruebas aprobadas** (11 unitarias y 25 de integración), **0 fallos, 0 errores y 0 omitidas**. Incluyen la importación sobre un administrador ya existente, completando la cuenta faltante sin alterar sus datos. `clean verify` generó el JAR actualizado. Los detalles y la comprobación previa en Chrome están en `docs/VALIDACION.md`.

La nueva importación completa también se verificó desde cero en un servidor temporal independiente: seis tablas, los datos iniciales indicados, un único producto con stock cero, cuentas BCrypt que autentican correctamente y reimportaciones que conservan los datos existentes.

## Alcance y límites

La aplicación cubre el inventario y las entradas solicitadas. Los cambios en nombres de productos, proveedores o usuarios se reflejan al consultar entradas antiguas porque el historial conserva referencias, no copias de esos nombres.

Las sesiones se pierden al reiniciar el servidor. El script SQL es una definición inicial idempotente, no un sistema de migraciones para bases ajenas o versiones anteriores. La disponibilidad de la aplicación requiere XAMPP y Java en ejecución.

El módulo MySQL de este XAMPP ejecuta internamente MariaDB 10.4.32. MySQL Connector/J comunica su versión de compatibilidad como MySQL 5.5.5; Hibernate avisa que esa versión está por debajo de su mínimo soportado para MySQL. Las consultas de bloqueo usan SQL compatible explícito. Cambiar el controlador JDBC mantiene el servidor y los datos existentes; no instala ni sustituye el motor de XAMPP.

## Referencias técnicas

- [Documentación de Spring Boot](https://docs.spring.io/spring-boot/reference/)
- [Formulario de autenticación con Spring Security](https://spring.io/guides/gs/securing-web)
- [Documentación de MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/)
- [Abrir proyectos Maven en IntelliJ IDEA](https://www.jetbrains.com/help/idea/maven-support.html)
- [Configuración de ejecución Java en IntelliJ IDEA](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html)
- [Importar archivos SQL en phpMyAdmin](https://docs.phpmyadmin.net/en/latest/import_export.html)
