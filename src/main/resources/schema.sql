-- Importación completa para MySQL/phpMyAdmin y arranque de Spring Boot.
-- Única definición del esquema: Hibernate solo valida estas seis tablas.
-- Para incluir las dos cuentas con claves privadas, ejecute primero
-- scripts/preparar-importacion.ps1 e importe .local/importar-inventario.sql.
-- Sin ese paso, el administrador se crea al iniciar Java desde la configuración local.
CREATE DATABASE IF NOT EXISTS inventario_bodega
 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE inventario_bodega;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuarios (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(60) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL,
 rol VARCHAR(20) NOT NULL, estado BIT NOT NULL,
 CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN','BODEGUERO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categorias (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS marcas (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS proveedores (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(120) NOT NULL,
 telefono VARCHAR(30), correo VARCHAR(160)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS productos (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 codigo VARCHAR(50) NOT NULL UNIQUE, nombre VARCHAR(120) NOT NULL, descripcion VARCHAR(500),
 stock_actual INT NOT NULL DEFAULT 0, stock_minimo INT NOT NULL DEFAULT 0,
 categoria_id BIGINT NOT NULL, marca_id BIGINT NOT NULL, activo BIT NOT NULL,
 CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
 CONSTRAINT fk_producto_marca FOREIGN KEY (marca_id) REFERENCES marcas(id),
 CONSTRAINT ck_producto_stock CHECK (stock_actual >= 0 AND stock_minimo >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS entradas_inventario (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, fecha DATETIME(6) NOT NULL,
 cantidad INT NOT NULL, observacion VARCHAR(500), producto_id BIGINT NOT NULL,
 proveedor_id BIGINT NOT NULL, usuario_responsable_id BIGINT NOT NULL,
 CONSTRAINT fk_entrada_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
 CONSTRAINT fk_entrada_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
 CONSTRAINT fk_entrada_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios(id),
 CONSTRAINT ck_entrada_cantidad CHECK (cantidad > 0), INDEX idx_entrada_fecha (fecha,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- DATOS INICIALES DE EJEMPLO, exclusivamente para practicar con el sistema.
-- El arranque automático solo carga ejemplos cuando los catálogos están vacíos.
-- La importación privada puede pedir completar los ejemplos faltantes.
-- Cada fila se busca por nombre/código y nunca se actualizan datos existentes.
SET @cargar_ejemplos = COALESCE(@completar_ejemplos_iniciales,
 NOT EXISTS (SELECT 1 FROM categorias) AND
 NOT EXISTS (SELECT 1 FROM marcas) AND
 NOT EXISTS (SELECT 1 FROM proveedores) AND
 NOT EXISTS (SELECT 1 FROM productos));

START TRANSACTION;

INSERT INTO categorias (nombre)
SELECT 'DEMO - Papelería' WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'DEMO - Papelería');
INSERT INTO categorias (nombre)
SELECT 'DEMO - Herramientas' WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'DEMO - Herramientas');
INSERT INTO categorias (nombre)
SELECT 'DEMO - Limpieza' WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'DEMO - Limpieza');

INSERT INTO marcas (nombre)
SELECT 'DEMO - Genérica' WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'DEMO - Genérica');
INSERT INTO marcas (nombre)
SELECT 'DEMO - Oficina' WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'DEMO - Oficina');

INSERT INTO proveedores (nombre, telefono, correo)
SELECT 'DEMO - Proveedor de oficina', NULL, 'oficina@example.invalid'
WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM proveedores WHERE nombre = 'DEMO - Proveedor de oficina');
INSERT INTO proveedores (nombre, telefono, correo)
SELECT 'DEMO - Proveedor de suministros', NULL, 'suministros@example.invalid'
WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM proveedores WHERE nombre = 'DEMO - Proveedor de suministros');

-- Un solo producto; su stock debe aumentar únicamente mediante entradas reales.
-- Se buscan los catálogos por nombre y no se presuponen IDs autoincrementales.
INSERT INTO productos
 (codigo, nombre, descripcion, stock_actual, stock_minimo, categoria_id, marca_id, activo)
SELECT 'DEMO-001', 'Producto de ejemplo',
 'Ejemplo para prácticas: registre una entrada para aumentar sus existencias.',
 0, 5, c.id, m.id, 1
FROM categorias c CROSS JOIN marcas m
WHERE @cargar_ejemplos
 AND NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'DEMO-001')
 AND c.nombre = 'DEMO - Papelería' AND m.nombre = 'DEMO - Genérica';

-- CUENTAS INICIALES OPCIONALES.
-- El generador privado antepone estas variables con hashes BCrypt. Nunca hay
-- contraseñas universales, texto plano ni hashes de acceso fijos en el repositorio.
-- Cada cuenta se crea solo si falta su username; no se cambian claves, roles ni estados.
SET @cargar_usuarios =
 COALESCE(@usuario_admin_inicial, '') REGEXP '^[a-zA-Z0-9._-]{3,60}$'
 AND COALESCE(@usuario_bodeguero_inicial, '') REGEXP '^[a-zA-Z0-9._-]{3,60}$'
 AND LOWER(@usuario_admin_inicial) <> LOWER(@usuario_bodeguero_inicial)
 AND CHAR_LENGTH(COALESCE(@hash_admin_inicial, '')) = 60
 AND LEFT(COALESCE(@hash_admin_inicial, ''), 4) IN ('$2a$', '$2b$', '$2y$')
 AND CHAR_LENGTH(COALESCE(@hash_bodeguero_inicial, '')) = 60
 AND LEFT(COALESCE(@hash_bodeguero_inicial, ''), 4) IN ('$2a$', '$2b$', '$2y$');

INSERT INTO usuarios (username, password, rol, estado)
SELECT @usuario_admin_inicial, @hash_admin_inicial, 'ADMIN', 1
WHERE @cargar_usuarios
 AND NOT EXISTS (SELECT 1 FROM usuarios
  WHERE username = CONVERT(@usuario_admin_inicial USING utf8mb4) COLLATE utf8mb4_unicode_ci);
INSERT INTO usuarios (username, password, rol, estado)
SELECT @usuario_bodeguero_inicial, @hash_bodeguero_inicial, 'BODEGUERO', 1
WHERE @cargar_usuarios
 AND NOT EXISTS (SELECT 1 FROM usuarios
  WHERE username = CONVERT(@usuario_bodeguero_inicial USING utf8mb4) COLLATE utf8mb4_unicode_ci);

-- El historial comienza vacío: cantidad > 0 y stock inicial 0 impiden inventar entradas.
COMMIT;

SET @cargar_ejemplos = NULL, @cargar_usuarios = NULL, @completar_ejemplos_iniciales = NULL;
SET @usuario_admin_inicial = NULL, @hash_admin_inicial = NULL,
 @usuario_bodeguero_inicial = NULL, @hash_bodeguero_inicial = NULL;
