package ec.edu.uta.inventariobodega;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.uta.inventariobodega.dto.*;
import ec.edu.uta.inventariobodega.entity.*;
import ec.edu.uta.inventariobodega.repository.*;
import ec.edu.uta.inventariobodega.security.UsuarioDetailsService;
import ec.edu.uta.inventariobodega.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Pruebas contra MariaDB/MySQL real. Nunca utilizan H2 ni recrean el esquema. */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@SpringBootTest(properties = {"app.bootstrap.enabled=false"})
@AutoConfigureMockMvc
class BaseDatosIntegrationTests {
  @Autowired ProductoRepository productos;
  @Autowired CategoriaRepository categorias;
  @Autowired MarcaRepository marcas;
  @Autowired ProveedorRepository proveedores;
  @Autowired UsuarioRepository usuarios;
  @Autowired EntradaInventarioRepository entradas;
  @Autowired EntradaService servicio;
  @Autowired ProductoService productoService;
  @Autowired CatalogoService catalogoService;
  @Autowired UsuarioService usuarioService;
  @Autowired UsuarioDetailsService details;
  @Autowired PasswordEncoder encoder;
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;
  Long productoId, proveedorId, categoriaId, marcaId, adminId, bodegueroId, inactivoId;
  String admin, bodeguero, inactivo;
  static final String CLAVE = "prueba-temporal-segura";

  @BeforeEach
  void prepararDatosPropios() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              String prefijo = "test_" + UUID.randomUUID().toString().substring(0, 8);
              Categoria c = new Categoria();
              c.setNombre(prefijo);
              categoriaId = categorias.save(c).getId();
              Marca m = new Marca();
              m.setNombre(prefijo);
              marcaId = marcas.save(m).getId();
              Proveedor p = new Proveedor();
              p.setNombre(prefijo);
              proveedorId = proveedores.save(p).getId();
              Producto producto = new Producto();
              producto.setCodigo(prefijo);
              producto.setNombre("Producto de prueba " + prefijo);
              producto.setCategoria(c);
              producto.setMarca(m);
              producto.setActivo(true);
              producto.setStockMinimo(2);
              productoId = productos.save(producto).getId();
              admin = prefijo + "_admin";
              bodeguero = prefijo + "_bod";
              inactivo = prefijo + "_off";
              adminId = crearUsuario(admin, Rol.ADMIN, true);
              bodegueroId = crearUsuario(bodeguero, Rol.BODEGUERO, true);
              inactivoId = crearUsuario(inactivo, Rol.BODEGUERO, false);
            });
  }

  Long crearUsuario(String name, Rol rol, boolean activo) {
    Usuario u = new Usuario();
    u.setUsername(name);
    u.setRol(rol);
    u.setEstado(activo);
    u.setPassword(encoder.encode(CLAVE));
    return usuarios.save(u).getId();
  }

  @AfterEach
  void limpiarSoloDatosPropios() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              if (productoId != null) {
                jdbc.update("DELETE FROM entradas_inventario WHERE producto_id=?", productoId);
                jdbc.update("DELETE FROM productos WHERE id=?", productoId);
              }
              if (proveedorId != null)
                jdbc.update("DELETE FROM proveedores WHERE id=?", proveedorId);
              if (categoriaId != null)
                jdbc.update("DELETE FROM categorias WHERE id=?", categoriaId);
              if (marcaId != null) jdbc.update("DELETE FROM marcas WHERE id=?", marcaId);
              if (adminId != null) jdbc.update("DELETE FROM usuarios WHERE id=?", adminId);
              if (bodegueroId != null) jdbc.update("DELETE FROM usuarios WHERE id=?", bodegueroId);
              if (inactivoId != null) jdbc.update("DELETE FROM usuarios WHERE id=?", inactivoId);
            });
  }

  EntradaForm form(int cantidad) {
    EntradaForm f = new EntradaForm();
    f.setProductoId(productoId);
    f.setProveedorId(proveedorId);
    f.setCantidad(cantidad);
    return f;
  }

  int stock() {
    return productos.findById(productoId).orElseThrow().getStockActual();
  }

  long entradasPropias() {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM entradas_inventario WHERE producto_id=?", Long.class, productoId);
  }

  @Test
  void esquemaTieneSoloSeisTablasYRelacionesValidas() {
    assertThat(
            jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()",
                String.class))
        .containsExactlyInAnyOrder(
            "usuarios", "productos", "categorias", "marcas", "proveedores", "entradas_inventario");
    servicio.registrar(form(7), admin);
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              EntradaInventario e =
                  entradas
                      .findAllByOrderByFechaDescIdDesc(
                          org.springframework.data.domain.PageRequest.of(0, 100))
                      .stream()
                      .filter(x -> x.getProducto().getId().equals(productoId))
                      .findFirst()
                      .orElseThrow();
              assertThat(e.getProducto().getCategoria().getId()).isEqualTo(categoriaId);
              assertThat(e.getProducto().getMarca().getId()).isEqualTo(marcaId);
              assertThat(e.getProveedor().getId()).isEqualTo(proveedorId);
              assertThat(e.getUsuarioResponsable().getId()).isEqualTo(adminId);
            });
  }

  @Test
  void entradaActualizaStockYConservaHistorial() {
    assertThat(stock()).isZero();
    servicio.registrar(form(7), bodeguero);
    assertThat(stock()).isEqualTo(7);
    assertThat(entradasPropias()).isEqualTo(1);
  }

  @Test
  void ceroSeRechazaSinPersistir() {
    assertThatThrownBy(() -> servicio.registrar(form(0), admin))
        .isInstanceOf(ReglaNegocioException.class);
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void negativoSeRechazaSinPersistir() {
    assertThatThrownBy(() -> servicio.registrar(form(-3), admin))
        .isInstanceOf(ReglaNegocioException.class);
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void productoInexistenteSeRechaza() {
    EntradaForm f = form(4);
    f.setProductoId(Long.MAX_VALUE);
    assertThatThrownBy(() -> servicio.registrar(f, admin))
        .isInstanceOf(ReglaNegocioException.class);
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void proveedorInexistenteSeRechaza() {
    EntradaForm f = form(4);
    f.setProveedorId(Long.MAX_VALUE);
    assertThatThrownBy(() -> servicio.registrar(f, admin))
        .isInstanceOf(ReglaNegocioException.class);
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void rollbackDespuesDeEscribirNoDejaStockNiEntrada() {
    assertThatThrownBy(
            () ->
                new TransactionTemplate(manager)
                    .executeWithoutResult(
                        s -> {
                          servicio.registrar(form(9), admin);
                          assertThat(stock()).isEqualTo(9);
                          throw new IllegalStateException("Fallo después del flush");
                        }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void falloRealDeBaseDeDatosRevierteStock() {
    assertThatThrownBy(
            () ->
                new TransactionTemplate(manager)
                    .executeWithoutResult(
                        s -> {
                          servicio.registrar(form(9), admin);
                          jdbc.update(
                              "INSERT INTO"
                                  + " entradas_inventario(fecha,cantidad,producto_id,proveedor_id,usuario_responsable_id)"
                                  + " VALUES(NOW(),0,?,?,?)",
                              productoId,
                              proveedorId,
                              adminId);
                        }))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void dosEntradasSimultaneasConservanLaSuma() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch listos = new CountDownLatch(2), salida = new CountDownLatch(1);
    try {
      Future<?> a =
          pool.submit(
              () -> {
                listos.countDown();
                esperar(salida);
                servicio.registrar(form(11), admin);
              });
      Future<?> b =
          pool.submit(
              () -> {
                listos.countDown();
                esperar(salida);
                servicio.registrar(form(13), bodeguero);
              });
      assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
      salida.countDown();
      a.get(20, TimeUnit.SECONDS);
      b.get(20, TimeUnit.SECONDS);
      assertThat(stock()).isEqualTo(24);
      assertThat(entradasPropias()).isEqualTo(2);
    } finally {
      salida.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  static void esperar(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo agotado");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }

  @Test
  void adminAccedeARutasYFormulariosAdministrativos() throws Exception {
    for (String ruta :
        List.of(
            "/admin/usuarios",
            "/admin/usuarios/nuevo",
            "/admin/usuarios/" + bodegueroId + "/editar",
            "/admin/categorias",
            "/admin/categorias/nuevo",
            "/admin/categorias/" + categoriaId + "/editar",
            "/admin/marcas",
            "/admin/marcas/nuevo",
            "/admin/proveedores",
            "/admin/proveedores/nuevo",
            "/admin/proveedores/" + proveedorId + "/editar",
            "/admin/productos/nuevo",
            "/admin/productos/" + productoId + "/editar"))
      mvc.perform(get(ruta).with(user(details.loadUserByUsername(admin))))
          .andExpect(status().isOk());
  }

  @Test
  void bodegueroNoAccedeNiModificaAdministracion() throws Exception {
    for (String ruta :
        List.of(
            "/admin/usuarios",
            "/admin/productos/nuevo",
            "/admin/categorias",
            "/admin/marcas",
            "/admin/proveedores"))
      mvc.perform(get(ruta).with(user(details.loadUserByUsername(bodeguero))))
          .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/usuarios/" + bodegueroId)
                .with(user(details.loadUserByUsername(bodeguero)))
                .with(csrf())
                .param("username", bodeguero)
                .param("rol", "ADMIN")
                .param("estado", "true"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/productos/" + productoId + "/eliminar")
                .with(user(details.loadUserByUsername(bodeguero)))
                .with(csrf()))
        .andExpect(status().isForbidden());
    assertThat(usuarios.findById(bodegueroId).orElseThrow().getRol()).isEqualTo(Rol.BODEGUERO);
  }

  @Test
  void vistasPublicasYDeBodegaSeRenderizanConDatos() throws Exception {
    mvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Iniciar sesión")));
    servicio.registrar(form(2), admin);
    for (String ruta : List.of("/", "/productos", "/inventario", "/entradas", "/entradas/nueva"))
      mvc.perform(get(ruta).with(user(details.loadUserByUsername(bodeguero))))
          .andExpect(status().isOk());
    mvc.perform(
            get("/productos")
                .param("categoria", categoriaId.toString())
                .param("q", "Producto de prueba")
                .with(user(details.loadUserByUsername(bodeguero))))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto de prueba")));
  }

  @Test
  void usuarioInactivoNoPuedeAutenticarse() throws Exception {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(details);
    provider.setPasswordEncoder(encoder);
    AuthenticationManager auth = new ProviderManager(provider);
    assertThatThrownBy(
            () ->
                auth.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(inactivo, CLAVE)))
        .isInstanceOf(DisabledException.class);
    mvc.perform(post("/login").with(csrf()).param("username", inactivo).param("password", CLAVE))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void loginActivoLogoutYCsrfFuncionan() throws Exception {
    var resultado =
        mvc.perform(post("/login").with(csrf()).param("username", admin).param("password", CLAVE))
            .andExpect(redirectedUrl("/"))
            .andReturn();
    var sesion =
        (org.springframework.mock.web.MockHttpSession) resultado.getRequest().getSession(false);
    mvc.perform(get("/inventario").session(sesion)).andExpect(status().isOk());
    mvc.perform(post("/logout").with(user(details.loadUserByUsername(admin))).with(csrf()))
        .andExpect(redirectedUrl("/login?logout"));
    mvc.perform(post("/entradas").with(user(details.loadUserByUsername(admin))))
        .andExpect(status().isForbidden());
    mvc.perform(get("/inventario")).andExpect(status().is3xxRedirection());
  }

  @Test
  void desactivarCuentaRevocaSesionExistente() throws Exception {
    var principal = details.loadUserByUsername(bodeguero);
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              var u = usuarios.findById(bodegueroId).orElseThrow();
              u.setEstado(false);
            });
    mvc.perform(get("/inventario").with(user(principal)))
        .andExpect(redirectedUrl("/login?inactiva"));
  }

  @Test
  void cambiarPasswordRevocaSesionAutenticada() throws Exception {
    var resultado =
        mvc.perform(
                post("/login").with(csrf()).param("username", bodeguero).param("password", CLAVE))
            .andExpect(redirectedUrl("/"))
            .andReturn();
    var sesion =
        (org.springframework.mock.web.MockHttpSession) resultado.getRequest().getSession(false);
    UsuarioForm f = usuarioService.formulario(bodegueroId);
    f.setPassword("otra-clave-de-prueba");
    usuarioService.guardar(bodegueroId, f, admin);
    mvc.perform(get("/inventario").session(sesion)).andExpect(redirectedUrl("/login?inactiva"));
  }

  @Test
  void productoConHistorialSeDesactivaYNoSeBorra() {
    servicio.registrar(form(3), admin);
    assertThat(productoService.eliminar(productoId)).contains("desactivado");
    assertThat(productos.findById(productoId).orElseThrow().getActivo()).isFalse();
    assertThat(entradasPropias()).isEqualTo(1);
  }

  @Test
  void productoSinHistorialPuedeEliminarse() {
    productoService.eliminar(productoId);
    assertThat(productos.findById(productoId)).isEmpty();
  }

  @Test
  void referenciasImpidenEliminarCatalogos() {
    servicio.registrar(form(3), admin);
    for (var dato :
        List.of(
            Map.entry("categorias", categoriaId),
            Map.entry("marcas", marcaId),
            Map.entry("proveedores", proveedorId)))
      assertThatThrownBy(() -> catalogoService.eliminar(dato.getKey(), dato.getValue()))
          .isInstanceOf(ReglaNegocioException.class);
  }

  @Test
  void editarProductoNoPermiteManipularStock() throws Exception {
    var f = productoService.formulario(productoId);
    mvc.perform(
            post("/admin/productos/" + productoId)
                .with(user(details.loadUserByUsername(admin)))
                .with(csrf())
                .param("codigo", f.getCodigo())
                .param("nombre", f.getNombre())
                .param("stockMinimo", "2")
                .param("categoriaId", categoriaId.toString())
                .param("marcaId", marcaId.toString())
                .param("activo", "true")
                .param("stockActual", "999"))
        .andExpect(redirectedUrl("/productos"));
    assertThat(stock()).isZero();
  }

  @Test
  void formularioValidaEntradaSinGuardar() throws Exception {
    mvc.perform(
            post("/entradas")
                .with(user(details.loadUserByUsername(bodeguero)))
                .with(csrf())
                .param("productoId", productoId.toString())
                .param("proveedorId", proveedorId.toString())
                .param("cantidad", "0"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "cantidad"));
    mvc.perform(
            post("/entradas")
                .with(user(details.loadUserByUsername(bodeguero)))
                .with(csrf())
                .param("productoId", productoId.toString())
                .param("proveedorId", proveedorId.toString())
                .param("cantidad", "1.5"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "cantidad"))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Ingrese un número entero")));
    assertThat(stock()).isZero();
    assertThat(entradasPropias()).isZero();
  }

  @Test
  void flujoMvcCreaCatalogosProductoUsuarioYEntrada() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            transaccion -> {
              try {
                String nombre = "test_mvc_" + UUID.randomUUID().toString().substring(0, 8);
                var autenticado = details.loadUserByUsername(admin);
                for (String tipo : List.of("categorias", "marcas", "proveedores")) {
                  mvc.perform(
                          post("/admin/" + tipo)
                              .with(user(autenticado))
                              .with(csrf())
                              .param("nombre", nombre)
                              .param("telefono", "+593 999999999")
                              .param("correo", "prueba@example.test"))
                      .andExpect(redirectedUrl("/admin/" + tipo));
                }
                Long c =
                    jdbc.queryForObject(
                        "SELECT id FROM categorias WHERE nombre=?", Long.class, nombre);
                Long m =
                    jdbc.queryForObject("SELECT id FROM marcas WHERE nombre=?", Long.class, nombre);
                Long pr =
                    jdbc.queryForObject(
                        "SELECT id FROM proveedores WHERE nombre=?", Long.class, nombre);
                mvc.perform(
                        post("/admin/productos")
                            .with(user(autenticado))
                            .with(csrf())
                            .param("codigo", nombre)
                            .param("nombre", nombre)
                            .param("categoriaId", c.toString())
                            .param("marcaId", m.toString())
                            .param("stockMinimo", "2")
                            .param("activo", "true")
                            .param("stockActual", "999"))
                    .andExpect(redirectedUrl("/productos"));
                Long producto =
                    jdbc.queryForObject(
                        "SELECT id FROM productos WHERE codigo=?", Long.class, nombre);
                assertThat(
                        jdbc.queryForObject(
                            "SELECT stock_actual FROM productos WHERE id=?",
                            Integer.class,
                            producto))
                    .isZero();
                mvc.perform(
                        post("/admin/usuarios")
                            .with(user(autenticado))
                            .with(csrf())
                            .param("username", nombre)
                            .param("password", CLAVE)
                            .param("rol", "BODEGUERO")
                            .param("estado", "true"))
                    .andExpect(redirectedUrl("/admin/usuarios"));
                assertThat(usuarios.findByUsername(nombre).orElseThrow().getPassword())
                    .startsWith("$2");
                mvc.perform(
                        post("/entradas")
                            .with(user(autenticado))
                            .with(csrf())
                            .param("productoId", producto.toString())
                            .param("proveedorId", pr.toString())
                            .param("cantidad", "4")
                            .param("usuarioResponsableId", bodegueroId.toString())
                            .param("fecha", "2000-01-01"))
                    .andExpect(redirectedUrl("/entradas"));
                assertThat(
                        jdbc.queryForObject(
                            "SELECT stock_actual FROM productos WHERE id=?",
                            Integer.class,
                            producto))
                    .isEqualTo(4);
                assertThat(
                        jdbc.queryForObject(
                            "SELECT usuario_responsable_id FROM entradas_inventario WHERE"
                                + " producto_id=?",
                            Long.class,
                            producto))
                    .isEqualTo(adminId);
              } catch (Exception ex) {
                throw new IllegalStateException("Falló el flujo MVC completo.", ex);
              } finally {
                transaccion.setRollbackOnly();
              }
            });
  }

  @Test
  void contrasenasPersistidasSonBcryptYSePuedenActualizar() {
    assertThat(usuarios.findById(adminId).orElseThrow().getPassword()).startsWith("$2");
    UsuarioForm f = usuarioService.formulario(bodegueroId);
    f.setPassword("nueva-clave-de-prueba");
    usuarioService.guardar(bodegueroId, f, admin);
    assertThat(
            encoder.matches(
                "nueva-clave-de-prueba",
                usuarios.findById(bodegueroId).orElseThrow().getPassword()))
        .isTrue();
  }

  @Test
  void importacionCompletaUsuarioFaltanteSinCambiarAdministrador() {
    String hashAdministrador = usuarios.findById(adminId).orElseThrow().getPassword();
    String hashImportacion = encoder.encode(CLAVE);
    jdbc.update("DELETE FROM usuarios WHERE id=?", inactivoId);
    inactivoId = null;
    try {
      for (int intento = 0; intento < 2; intento++) {
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<Void>)
                conexion -> {
                  try (var variables =
                      conexion.prepareStatement(
                          "SET @usuario_admin_inicial=?, @hash_admin_inicial=?,"
                              + " @usuario_bodeguero_inicial=?, @hash_bodeguero_inicial=?,"
                              + " @completar_ejemplos_iniciales=NULL")) {
                    variables.setString(1, admin);
                    variables.setString(2, hashImportacion);
                    variables.setString(3, inactivo);
                    variables.setString(4, hashImportacion);
                    variables.execute();
                  }
                  org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                      conexion, new org.springframework.core.io.ClassPathResource("schema.sql"));
                  return null;
                });
        Usuario administrador = usuarios.findById(adminId).orElseThrow();
        assertThat(administrador.getPassword()).isEqualTo(hashAdministrador);
        assertThat(administrador.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(administrador.getEstado()).isTrue();
        Usuario nuevo = usuarios.findByUsername(inactivo).orElseThrow();
        assertThat(nuevo.getRol()).isEqualTo(Rol.BODEGUERO);
        assertThat(nuevo.getEstado()).isTrue();
        assertThat(encoder.matches(CLAVE, nuevo.getPassword())).isTrue();
        assertThat(
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM usuarios WHERE username=?", Long.class, inactivo))
            .isEqualTo(1L);
      }
    } finally {
      inactivoId = usuarios.findByUsername(inactivo).map(Usuario::getId).orElse(null);
    }
  }

  @Test
  void entradasNoSePuedenEditarNiEliminar() throws Exception {
    servicio.registrar(form(5), admin);
    mvc.perform(
            post("/entradas/1/eliminar").with(user(details.loadUserByUsername(admin))).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(post("/entradas/1").with(user(details.loadUserByUsername(admin))).with(csrf()))
        .andExpect(status().isNotFound());
    assertThat(stock()).isEqualTo(5);
    assertThat(entradasPropias()).isEqualTo(1);
  }
}
