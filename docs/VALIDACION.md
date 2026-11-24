# Validación del proyecto

## Completar una base con administrador existente — 24 de noviembre de 2026

Se corrigió la importación privada para insertar **cada cuenta que falte por su username**, conservando contraseña, rol y estado de las cuentas existentes. La versión anterior omitía ambas cuentas si había cualquier usuario; por eso una instalación con administrador previo no recibía al bodeguero. La comparación de usernames normaliza explícitamente la intercalación UTF-8 para evitar conflictos entre variables de sesión y columnas de la base.

La importación privada también puede completar los catálogos de ejemplo y `DEMO-001` sobre una base que ya tenga datos. Cada ejemplo se comprueba por nombre o código antes de insertarlo. El arranque automático conserva la política de cargar ejemplos únicamente si los cuatro catálogos están vacíos.

Se creó un respaldo privado y se ejecutó el SQL sobre la base real `inventario_bodega` del puerto **3307**. Se verificó que todos los registros previos, incluida la contraseña del administrador, el producto existente con stock 5 y su entrada, permanecieran iguales. La carga añadió al bodeguero, los catálogos faltantes y un único `DEMO-001` con stock 0. Una segunda importación no alteró ni duplicó registros.

Estado comprobado después de la carga: **2 usuarios, 4 categorías, 3 marcas, 3 proveedores, 2 productos y 1 entrada**. Esos totales incluyen los datos propios que ya existían; el SQL no reinició el inventario ni eliminó movimientos.

Se agregó una prueba de regresión que importa dos veces sobre un administrador existente y comprueba que se cree el usuario faltante sin cambiar al administrador. `RUN_DB_TESTS=true` y `mvnw.cmd -B clean verify` finalizaron con **36 aprobadas** (11 unitarias y 25 de integración), **0 fallos, 0 errores y 0 omitidas**, y generaron el JAR actualizado.

Se inició el JAR temporalmente en el puerto 18080 contra la base real del puerto 3307. Las dos contraseñas privadas se comprobaron mediante el formulario de login de Spring Security: ambos usuarios accedieron al inventario, ADMIN accedió a usuarios y BODEGUERO recibió 403. La aplicación temporal se detuvo después; XAMPP conserva su proceso original.

El respaldo, el resultado de la carga y los logs se conservan en `.local/`, excluidos de Git. Las contraseñas iniciales son texto de acceso guardado en el archivo privado; los valores BCrypt de `usuarios.password` son hashes que no se escriben en el login.

## Importación completa y datos iniciales — 16 de noviembre de 2026

`schema.sql` ahora crea `inventario_bodega`, selecciona esa base, crea las seis tablas y carga los datos iniciales cuando los catálogos están vacíos. Incluye las instrucciones para insertar las dos cuentas cuando recibe sus hashes privados. El generador `scripts/preparar-importacion.ps1` combina las cuentas privadas con ese mismo archivo; no mantiene un esquema duplicado ni publica claves.

La prueba de importación se realizó con los ejecutables MySQL de XAMPP en un **servidor temporal independiente**, puerto **13307**, cuyo directorio de datos está en `.local/mysql-validacion-schema/data`. No se usó ni modificó el directorio de datos habitual de XAMPP. La base de prueba también se llama `inventario_bodega` y contiene solamente las seis tablas de negocio.

Verificaciones aprobadas:

- Importación del archivo público sin una base previamente creada: seis tablas, tres categorías, dos marcas, dos proveedores, un producto con stock cero y ningún movimiento.
- Importación del archivo privado generado: dos cuentas activas con roles ADMIN y BODEGUERO y contraseñas BCrypt válidas. Los hashes `$2y$` generados por PHP de XAMPP se verificaron también mediante autenticación real de Spring Security.
- Repetición de la importación pública y privada sin duplicar filas ni sobrescribir contraseñas.
- Conservación de nombres editados, un catálogo eliminado, entradas registradas y stock incrementado después de volver a importar.
- Regeneración del SQL privado conservando las contraseñas anteriores.
- `php -l scripts/generar-hashes.php`: sintaxis correcta en PHP 8.2.12. PHP solo se usa como herramienta local para generar BCrypt; el servidor de la aplicación permanece en Java.
- `RUN_DB_TESTS=true` y `mvnw.cmd -B clean verify` con la conexión MySQL apuntando al servidor temporal: **35 aprobadas, 0 fallos, 0 errores y 0 omitidas**. Se generó el JAR actualizado.
- Arranque del JAR en el puerto temporal **18080**: administrador y bodeguero importados iniciaron sesión y consultaron el inventario; el administrador accedió a usuarios y el bodeguero recibió 403.

Los registros de esta verificación están en `.local/mysql-validacion-schema/`. Se comprobó la ejecución de los archivos SQL con el cliente de MySQL; la guía de phpMyAdmin describe cómo importar esos mismos archivos, sin afirmar que se automatizó su interfaz gráfica. Los servidores temporales se detienen al finalizar y la aplicación del puerto 8080 permanece apagada.

En una instalación nueva, la carga completa deja **2 usuarios, 3 categorías, 2 marcas, 2 proveedores, 1 producto con stock 0 y 0 entradas**. Las cuentas y los datos propios de una instalación existente se conservan.

## Actualización: conexión MySQL — 16 de noviembre de 2026

La aplicación quedó **apagada** a petición del usuario. Se cambió la conexión de los archivos de configuración generales, locales y de ejemplo a `jdbc:mysql`, con el controlador `com.mysql.cj.jdbc.Driver`. Se eliminó la dependencia `mariadb-java-client` y se mantuvo `mysql-connector-j`.

Las consultas de bloqueo de productos y usuarios usan ahora `SELECT ... FOR UPDATE` explícito, compatible con el servidor de XAMPP y sin generar la cláusula `FOR UPDATE OF`. Los servicios conservan sus transacciones; la prueba de entradas simultáneas confirma que se conserva la suma del stock.

Se ejecutó nuevamente:

```powershell
$env:RUN_DB_TESTS = 'true'
.\mvnw.cmd -B clean verify
```

Resultado: **BUILD SUCCESS**, **35 aprobadas** (11 unitarias y 24 de integración), **0 fallos, 0 errores y 0 omitidas**. Se generó el JAR actualizado. El informe local está en `.local/mysql-verify.log` y los informes JUnit están en `target/surefire-reports/`.

Se conserva la base `inventario_bodega` y el puerto **3307**. El cambio corresponde al protocolo y controlador JDBC; el servidor de XAMPP sigue siendo el existente. MySQL Connector/J comunica la versión de compatibilidad 5.5.5 y Hibernate muestra una advertencia de versión mínima. Las operaciones del sistema, incluidos bloqueos y rollback, se verificaron contra ese servidor. No se instaló ni sustituyó el motor de base de datos y la aplicación permanece apagada tras las pruebas. Las comprobaciones de navegador descritas más abajo pertenecen a la ejecución anterior.

## Validación anterior — 15 de noviembre de 2026

Fecha: **15 de noviembre de 2026**, zona **America/Guayaquil**.

## Entorno real

- Windows, JDK **21.0.10**.
- Spring Boot **4.1.2**, versión original conservada.
- Maven Wrapper **3.10.0**.
- XAMPP: **MariaDB 10.4.32**, puerto **3307**.
- Base **inventario_bodega**, esquema InnoDB.
- Chrome instalado, controlado mediante CDP para las comprobaciones de navegador.

## Compilación y pruebas automatizadas

Comandos ejecutados desde la raíz en PowerShell:

```powershell
.\mvnw.cmd -B -DskipTests compile
.\mvnw.cmd -B test
$env:RUN_DB_TESTS = 'true'
.\mvnw.cmd -B test
.\mvnw.cmd -B clean verify
```

La última ejecución completa de `clean verify` terminó con **BUILD SUCCESS**:

| Tipo                                 | Clase                     | Aprobadas | Fallidas / errores | Omitidas |
| ------------------------------------ | ------------------------- | --------: | -----------------: | -------: |
| Unitarias                            | EntradaServiceTests       |         8 |                  0 |        0 |
| Unitarias                            | ValidacionTests           |         3 |                  0 |        0 |
| Integración con MariaDB y MVC reales | BaseDatosIntegrationTests |        24 |                  0 |        0 |
| **Total**                            |                           |    **35** |              **0** |    **0** |

La ejecución independiente sin `RUN_DB_TESTS` aprobó las 11 unitarias y omitió las pruebas de integración, como se diseñó. No se presentó esa ejecución como una validación de base de datos.

Se generó el JAR ejecutable `target/inventariobodega-0.0.1-SNAPSHOT.jar`. Los informes JUnit de la ejecución final están en `target/surefire-reports/`.

### Cobertura de las verificaciones

- Inicio del contexto completo y validación Hibernate de las seis entidades contra el esquema real.
- Relaciones producto-categoría, producto-marca y entrada-producto-proveedor-responsable.
- Stock inicial cero y aumento correcto al recibir productos.
- Rechazo de cantidades cero, negativas, nulas, decimales y de desbordamiento de enteros.
- Rechazo de productos/proveedores inexistentes y productos inactivos.
- Rollback después de escribir y después de una violación real de restricción SQL; se conservan stock e historial previos.
- Dos hilos concurrentes registran **11 + 13** unidades sobre el mismo producto: resultado confirmado **24**, con dos entradas.
- Acceso ADMIN a rutas y formularios administrativos; rechazo BODEGUERO a rutas de lectura y escritura administrativas, incluida la manipulación de rol.
- Autenticación real de usuarios activos, rechazo de inactivos y cierre de sesión POST.
- CSRF activo y rechazo de una escritura sin token.
- Sesión válida después de autenticar, y revocación por desactivación o cambio de contraseña.
- BCrypt en usuarios persistidos y actualización de contraseña.
- Desactivación de productos con historial, eliminación física sin historial y protección de catálogos referenciados.
- Intentos de cambiar stock desde formularios ignorados.
- Intentos de editar/eliminar entradas rechazados por inexistencia de esas rutas.
- Renderizado real de Thymeleaf con datos, búsquedas por categoría y texto.
- Flujo MVC completo de creación de categorías, marcas, proveedor, producto, usuario y entrada. El responsable enviado por el cliente se ignora y se conserva el autenticado.
- Mensajes de validación numérica en español.

Los registros de prueba se identifican con nombres aleatorios `test_`; se eliminan únicamente sus propios datos, o se revierte su transacción. No quedaron productos, catálogos ni entradas ficticias en la base.

## Comprobaciones con navegador

El JAR se inició con la configuración local privada. Se ejecutó `node scripts/verificar-navegador.cjs` usando Chrome en modo headless con depuración local en el puerto 9223.

Resultado: **APROBADO**.

- Login con las credenciales privadas del administrador inicial y mantenimiento de la sesión HTTP.
- Navegación por productos, inventario, historial, registro de entrada, usuarios, categorías, marcas y proveedores.
- Carga del CSS de Bootstrap desde la propia aplicación y presencia de tokens CSRF.
- Vista de escritorio de **1440 × 1000** y móvil de **390 × 844** sin desbordamiento horizontal de la página. Las tablas disponen de desplazamiento horizontal propio.
- Cierre de sesión mediante POST.
- Sin excepciones de ejecución detectadas por Chrome.

Las capturas y el resultado JSON se conservan en **`.local/qa/`**. Son artefactos locales ignorados por Git. Se inspeccionaron visualmente las capturas de la página inicial de escritorio y el inventario móvil. Esto se distingue de las pruebas JUnit; no se afirma una revisión manual exhaustiva de todos los dispositivos o navegadores.

## Estado real de la base

Se crearon y consultaron las siguientes seis tablas en `inventario_bodega`:

```text
categorias
entradas_inventario
marcas
productos
proveedores
usuarios
```

Se creó el usuario **administrador**, activo y con rol **ADMIN**. La contraseña se generó aleatoriamente en `.local/application.properties`, archivo privado ignorado por Git. En la base se almacena su hash BCrypt. No se publican credenciales en este documento.

Los productos, categorías, marcas, proveedores y entradas quedan vacíos, listos para registrar datos propios. No se alteraron otras bases de datos.

## Errores corregidos y límites observados

La primera ejecución de 33 pruebas tuvo 3 fallos y 10 errores debido a la detección de MariaDB como MySQL 5.5.5 y la cláusula de bloqueo incompatible `FOR UPDATE OF`. Se agregó el controlador JDBC de MariaDB, se verificó su detección correcta y se repitieron las pruebas. La ejecución final de 35 pruebas no tiene fallos ni errores.

Hibernate **7.4.13.Final** advierte que MariaDB **10.4.32** está por debajo de su mínimo oficialmente soportado, **10.6**. Las operaciones y transacciones de esta aplicación se comprobaron contra el XAMPP existente. No se cambió Spring Boot ni se actualizó el servidor de base de datos. El soporte oficial completo del ORM requiere MariaDB 10.6 o superior.

Java 21 también muestra el aviso de carga dinámica del agente de Mockito utilizado por las pruebas; no impide su ejecución ni afecta al JAR de la aplicación. El error SQL provocado deliberadamente por la prueba de rollback es esperado y queda registrado en el informe de ejecución.

No se verificó un servidor MySQL independiente ni otros navegadores. No quedan errores de compilación o de pruebas pendientes. Los procesos de aplicación y navegador abiertos para la verificación se detienen al finalizar; XAMPP se conserva en su estado original.
