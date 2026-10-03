# Épica 1 — Sistema de Registro y Consulta de Avistamientos de Fauna Silvestre

Plataforma REST para que investigadores y guardabosques **registren** avistamientos de animales en reservas naturales y **consulten** el historial filtrado por especie, zona o fecha.

| | |
|---|---|
| **Stack** | Java 21, Spring Boot 3.3.4, Spring MVC, Spring Data JPA (Hibernate 6), Bean Validation, H2 en memoria |
| **Build** | Maven 3.9+ |
| **Historias cubiertas** | HU1: registrar un avistamiento (especie, ubicación geográfica, fecha, observaciones). HU2: consultar todos los avistamientos registrados |
| **Estado** | 9/9 pruebas en verde, verificado end-to-end contra la app en ejecución |

---

## 1. Cómo ejecutar

**Todos los comandos se ejecutan desde la carpeta `avistamientos-fauna/`**, la que contiene el `pom.xml`:

```bash
cd avistamientos-fauna

# Desarrollo (recarga manual)
mvn spring-boot:run

# Pruebas
mvn test

# JAR ejecutable
mvn package
java -jar target/avistamientos-fauna-1.0.0.jar
```

> Lanzarlos desde la carpeta padre falla con `No plugin found for prefix 'spring-boot' in the current project`. No es un problema del proyecto: sin un `pom.xml` en el directorio actual, Maven no puede resolver el prefijo `spring-boot`, porque ese prefijo lo aporta el plugin declarado en el POM.

La aplicación queda en `http://localhost:8080`. La consola web de H2 está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:avistamientosdb`, usuario `sa`, sin contraseña).

Al arrancar se insertan 5 avistamientos de ejemplo desde [data.sql](src/main/resources/data.sql), así que los endpoints de consulta devuelven datos desde el primer momento.

Si el arranque falla con `Port 8080 was already in use`, suele quedar una instancia previa viva. En Windows, para identificarla y detenerla:

```bash
netstat -ano | findstr ":8080.*LISTENING"
taskkill /PID <pid> /F
```

Alternativamente, arrancar en otro puerto sin tocar la configuración: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`.

---

## 2. Arquitectura: por qué cuatro capas

```
Cliente HTTP
    │  JSON
    ▼
┌─────────────────────────────┐
│ AvistamientoController      │  Capa web: rutas, códigos HTTP, @Valid
└──────────────┬──────────────┘
               │  DTO (AvistamientoRequest / AvistamientoResponse)
               ▼
┌─────────────────────────────┐
│ AvistamientoService         │  Negocio: normaliza, mapea, decide qué consulta usar
└──────────────┬──────────────┘
               │  Entidad (Avistamiento)
               ▼
┌─────────────────────────────┐
│ AvistamientoRepository      │  Acceso a datos: interfaz, Spring Data genera la impl.
└──────────────┬──────────────┘
               │  JPQL → SQL
               ▼
         ┌───────────┐
         │  H2 (mem) │
         └───────────┘

   ManejadorGlobalErrores  ──► intercepta excepciones de cualquier capa y las
                               traduce a un JSON de error uniforme
```

**La regla que sostiene el diseño:** cada capa solo conoce a la de abajo, y los tipos que cruzan fronteras son distintos a propósito. El controlador nunca ve una entidad JPA; el servicio nunca ve un objeto HTTP. Eso permite cambiar el esquema de la base de datos sin romper el contrato público de la API, y viceversa.

### Estructura de archivos

```
avistamientos-fauna/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/reservas/avistamientos/
    │   │   ├── AvistamientosFaunaApplication.java   ← arranque
    │   │   ├── controller/AvistamientoController.java
    │   │   ├── service/AvistamientoService.java
    │   │   ├── repository/AvistamientoRepository.java
    │   │   ├── model/Avistamiento.java              ← entidad JPA
    │   │   ├── dto/AvistamientoRequest.java         ← entrada
    │   │   ├── dto/AvistamientoResponse.java        ← salida
    │   │   └── exception/
    │   │       ├── RecursoNoEncontradoException.java
    │   │       ├── RespuestaError.java
    │   │       └── ManejadorGlobalErrores.java
    │   └── resources/
    │       ├── application.properties
    │       └── data.sql
    └── test/java/com/reservas/avistamientos/
        └── AvistamientoApiTest.java
```

---

## 3. Documentación del código, archivo por archivo

### 3.1 `pom.xml`

Descriptor Maven del proyecto.

| Elemento | Qué hace |
|---|---|
| `<parent>` → `spring-boot-starter-parent:3.3.4` | Aporta un `dependencyManagement` con versiones compatibles de cientos de librerías. **Por eso ninguna dependencia de abajo lleva `<version>`.** También preconfigura compilador, Surefire y el plugin de repackage. |
| `<relativePath/>` vacío | Indica a Maven que busque el parent en el repositorio remoto, no en disco. |
| `java.version=21` | Fija el nivel de lenguaje. Habilita `record`, bloques de texto `"""` y `List.toList()`. |
| `project.build.sourceEncoding=UTF-8` | Evita que los acentos se corrompan al compilar. |
| `spring-boot-starter-web` | Spring MVC + Tomcat embebido + Jackson. Es lo que permite exponer controladores REST y serializar objetos a JSON. |
| `spring-boot-starter-data-jpa` | Spring Data JPA + Hibernate (implementación de JPA) + HikariCP (pool de conexiones). |
| `spring-boot-starter-validation` | Jakarta Bean Validation + Hibernate Validator. Habilita `@NotBlank`, `@Size`, `@PastOrPresent` sobre los DTO. |
| `h2` con `<scope>runtime</scope>` | Base de datos en memoria. El scope es `runtime` porque el código fuente nunca la importa: solo se necesita el driver JDBC al ejecutar. |
| `spring-boot-starter-test` con `<scope>test</scope>` | JUnit 5, AssertJ, Mockito, MockMvc. No se empaqueta en el JAR final. |
| `spring-boot-maven-plugin` | Da el goal `spring-boot:run` y el goal `repackage`, que genera el *fat jar* ejecutable con todas las dependencias dentro. |

---

### 3.2 `AvistamientosFaunaApplication.java` — punto de entrada

```java
@SpringBootApplication
public class AvistamientosFaunaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvistamientosFaunaApplication.class, args);
    }
}
```

`@SpringBootApplication` es un atajo que agrupa tres anotaciones:

1. **`@Configuration`** — la clase puede declarar beans.
2. **`@EnableAutoConfiguration`** — Spring Boot inspecciona el classpath y configura automáticamente Tomcat, Hibernate, el `DataSource` de H2 y Jackson, sin que escribamos un solo `@Bean`.
3. **`@ComponentScan`** — escanea este paquete y **todos sus subpaquetes** buscando componentes (`@RestController`, `@Service`, `@Repository`). Por eso esta clase debe vivir en el paquete raíz: si estuviera en `controller/`, el servicio y el repositorio no se descubrirían.

`SpringApplication.run()` crea el `ApplicationContext`, instancia los beans, levanta Tomcat y deja el proceso escuchando. Los `args` se interpretan como propiedades: `java -jar app.jar --server.port=9090` cambia el puerto sin recompilar.

---

### 3.3 `model/Avistamiento.java` — la entidad JPA

Cada instancia equivale a **una fila** de la tabla `avistamientos`. Hibernate traduce entre objetos Java y filas SQL.

#### Anotaciones de clase

| Anotación | Efecto |
|---|---|
| `@Entity` | Marca la clase como entidad gestionada, mapeada a una tabla. |
| `@Table(name = "avistamientos")` | Nombre explícito de la tabla. Sin esto Hibernate usaría `avistamiento` (singular, derivado del nombre de la clase). |

#### Campos

| Campo | Tipo | Mapeo y justificación |
|---|---|---|
| `id` | `Long` | `@Id` + `@GeneratedValue(strategy = IDENTITY)`. Clave primaria técnica (*surrogate key*). `IDENTITY` delega la generación a la base de datos vía columna autoincremental: es la estrategia más simple y funciona igual en H2, MySQL y PostgreSQL. |
| `especie` | `String` | `@Column(nullable = false, length = 120)`. Nombre común (`"Jaguar"`). Es el filtro más usado. |
| `nombreCientifico` | `String` | `@Column(name = "nombre_cientifico", length = 150)`. **Opcional**: un guardabosques puede no conocer el binomio en campo. Se nombra la columna explícitamente para usar `snake_case` en SQL. |
| `zona` | `String` | `@Column(nullable = false, length = 120)`. Sector de la reserva (`"Sector Norte"`). Permite agrupar por área. |
| `latitud` | `Double` | `@Column(nullable = false)`. **Se usa el envoltorio `Double` y no el primitivo `double`** para poder distinguir "sin dato" (`null`) de `0.0`, que es una coordenada válida. |
| `longitud` | `Double` | Mismo criterio que `latitud`. |
| `fechaAvistamiento` | `LocalDateTime` | Momento en que el animal fue observado **en campo**. |
| `observaciones` | `String` | `@Column(columnDefinition = "TEXT")`. Texto libre (comportamiento, número de individuos, clima); se mapea a `TEXT`/`CLOB` porque puede ser extenso y no cabría en un `VARCHAR` corto. |
| `registradoPor` | `String` | `@Column(nullable = false, length = 120)`. Autor del registro. |
| `fechaRegistro` | `LocalDateTime` | `@Column(nullable = false, updatable = false)`. Sello de auditoría: cuándo **entró el dato al sistema**. `updatable = false` impide que se modifique en futuros `UPDATE`. |

> **Las dos fechas no son redundantes.** `fechaAvistamiento` es el dato científico (cuándo se vio el animal); `fechaRegistro` es el dato de trazabilidad (cuándo se capturó en el sistema). Un avistamiento nocturno puede registrarse a la mañana siguiente, y la diferencia importa para auditar.

#### Constructores

- **Constructor vacío** — *requerido por JPA*. Hibernate necesita instanciar la entidad por reflexión al leer de la base de datos. No debe eliminarse.
- **Constructor de 8 argumentos** — de conveniencia, lo usa el servicio al convertir un DTO en entidad. Deliberadamente **no** recibe `id` ni `fechaRegistro`: esos valores los asignan la base de datos y el callback de abajo.

#### Método `alPersistir()`

```java
@PrePersist
void alPersistir() {
    if (this.fechaRegistro == null) {
        this.fechaRegistro = LocalDateTime.now();
    }
}
```

`@PrePersist` es un *callback del ciclo de vida* de JPA: Hibernate lo invoca automáticamente justo **antes** del `INSERT`. Garantiza que `fechaRegistro` siempre tenga valor sin depender de que el cliente lo envíe —y sin que el cliente pueda falsearlo—. La guarda `if (null)` permite que un proceso de importación histórica fije la fecha explícitamente.

> **El IDE marcará `alPersistir` como "never used": es un falso positivo.** El método no se invoca desde el código fuente, sino que Hibernate lo llama por reflexión al detectar `@PrePersist`. Los analizadores estáticos solo rastrean referencias explícitas y no modelan los callbacks del ciclo de vida de JPA. **No eliminar el método.** La visibilidad *package-private* (sin `public`) es deliberada: basta para que Hibernate lo invoque y evita exponerlo como parte de la API de la clase.

#### Getters, setters y `toString()`

JPA y Jackson acceden al estado a través de los getters/setters. El `toString()` solo incluye los cuatro campos identificatorios: es para logs y depuración, no se expone al cliente.

---

### 3.4 `dto/AvistamientoRequest.java` — DTO de entrada (HU1)

Un `record` de Java: inmutable, con constructor canónico y `equals`/`hashCode`/`toString` generados por el compilador.

**¿Por qué un DTO y no recibir la entidad directamente?** Tres razones concretas:

1. **Seguridad.** Si el endpoint aceptara `Avistamiento`, un cliente podría enviar `{"id": 1, "fechaRegistro": "1990-01-01T00:00:00"}` y sobrescribir estado interno o falsear la auditoría. El DTO simplemente no tiene esos campos.
2. **Validación propia de la API.** Las reglas HTTP (`@PastOrPresent`, rangos geográficos) viven aquí, sin contaminar el modelo de persistencia.
3. **Desacople.** Si mañana cambia una columna, el JSON público puede permanecer igual.

#### Validaciones campo por campo

| Campo | Restricciones | Razón |
|---|---|---|
| `especie` | `@NotBlank`, `@Size(max=120)` | Obligatorio. `@NotBlank` rechaza `null`, `""` y `"   "` (a diferencia de `@NotNull`, que aceptaría cadena vacía). El tamaño coincide con la columna. |
| `nombreCientifico` | `@Size(max=150)` | **Sin `@NotBlank`**: es opcional. Si llega, se limita la longitud. |
| `zona` | `@NotBlank`, `@Size(max=120)` | Obligatorio. |
| `latitud` | `@NotNull`, `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` | `@NotNull` valida presencia; los decimales validan el **rango geográfico real**, no solo que sea un número. |
| `longitud` | `@NotNull`, `@DecimalMin("-180.0")`, `@DecimalMax("180.0")` | Ídem. |
| `fechaAvistamiento` | `@NotNull`, `@PastOrPresent`, `@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")` | `@PastOrPresent` impide registrar avistamientos con fecha futura, que serían datos científicamente inválidos. `@JsonFormat` fija el formato aceptado para que el cliente no tenga ambigüedad. |
| `observaciones` | `@Size(max=2000)` | Opcional, con techo para evitar abuso. |
| `registradoPor` | `@NotBlank`, `@Size(max=120)` | Obligatorio: todo registro necesita responsable trazable. |

Cada anotación lleva su `message` en español; esos mensajes son exactamente los que aparecen en el campo `detalles` de la respuesta de error.

---

### 3.5 `dto/AvistamientoResponse.java` — DTO de salida (HU2)

Vista pública de un avistamiento. Expone los 10 campos (incluyendo `id` y `fechaRegistro`, que el request no acepta pero el cliente sí debe poder leer).

No devolver la entidad evita dos problemas clásicos:

- **`LazyInitializationException`** por serialización accidental de relaciones perezosas fuera de la sesión de Hibernate.
- **Fuga de campos internos** al contrato de la API.

#### Método `desde(Avistamiento)`

```java
public static AvistamientoResponse desde(Avistamiento entidad) { ... }
```

Fábrica estática que convierte entidad → DTO. Se coloca **aquí y no en el servicio** para mantener el mapeo junto al contrato que produce; el servicio solo invoca la referencia a método `AvistamientoResponse::desde` dentro de un `.map(...)`.

---

### 3.6 `repository/AvistamientoRepository.java` — acceso a datos

Es una **interfaz sin implementación**. Spring Data JPA genera la clase concreta al arrancar (proxy dinámico) y la registra como bean. Por eso **no hay una sola línea de JDBC en todo el proyecto**.

```java
public interface AvistamientoRepository extends JpaRepository<Avistamiento, Long>
```

Los dos parámetros genéricos son `<tipo de la entidad, tipo de su clave primaria>`. Al extender `JpaRepository` se heredan gratis `save`, `findById`, `findAll`, `count`, `existsById`, `deleteById` y varios más.

`@Repository` es opcional al extender `JpaRepository`, pero se deja para explicitar el rol de la capa.

#### `buscarConFiltros(...)` — la consulta central

```java
@Query("""
        select a from Avistamiento a
        where (:especie is null or lower(a.especie) like lower(concat('%', :especie, '%')))
          and (:zona    is null or lower(a.zona)    like lower(concat('%', :zona,    '%')))
          and (:desde   is null or a.fechaAvistamiento >= :desde)
          and (:hasta   is null or a.fechaAvistamiento <= :hasta)
        order by a.fechaAvistamiento desc
        """)
List<Avistamiento> buscarConFiltros(@Param("especie") String especie, ...);
```

Resuelve los **tres filtros de la épica** (especie, zona, rango de fechas) en una sola sentencia.

**El truco está en el patrón `(:parámetro is null or <condición>)`:** cuando el parámetro llega nulo, la condición se neutraliza y ese filtro se ignora. Así un único método cubre las 16 combinaciones posibles de filtros, en lugar de escribir ocho métodos derivados distintos.

Decisiones de implementación:

- Se usa **JPQL y no SQL nativo** para que la consulta siga siendo portable entre H2 y PostgreSQL (la épica admite ambos).
- `@Param` vincula cada argumento Java con su marcador nombrado dentro de la consulta.
- El bloque de texto `"""` (Java 15+) mantiene la consulta legible y alineada.
- `order by a.fechaAvistamiento desc` garantiza que lo más reciente llegue primero, sin que el cliente lo tenga que pedir.

#### `contarPorEspecie()` — agregación

```java
@Query("select a.especie, count(a) from Avistamiento a group by a.especie order by count(a) desc")
List<Object[]> contarPorEspecie();
```

Devuelve `List<Object[]>` porque una proyección de varias columnas **sin clase destino** se materializa como arreglo: posición `0` = especie (`String`), posición `1` = conteo (`Long`). El servicio lo convierte en un mapa legible.

---

### 3.7 `service/AvistamientoService.java` — lógica de negocio

Marcado con `@Service`, el estereotipo de Spring para la capa de negocio. Tres responsabilidades:

1. Traducir DTO → entidad y entidad → DTO.
2. Normalizar datos de entrada.
3. Decidir qué consulta del repositorio usar según los filtros recibidos.

#### Inyección de dependencias

```java
private static final Logger log = LoggerFactory.getLogger(AvistamientoService.class);
private final AvistamientoRepository repositorio;

public AvistamientoService(AvistamientoRepository repositorio) {
    this.repositorio = repositorio;
}
```

- El `Logger` es `static final`: patrón estándar de SLF4J, una sola instancia por clase.
- El repositorio se declara `final` y se inyecta **por constructor**, no con `@Autowired` sobre el campo. Así la dependencia queda obligatoria, el objeto es inmutable, y la clase se puede instanciar en pruebas unitarias pasando un mock. Desde Spring 4.3 no hace falta `@Autowired` cuando existe un único constructor.

#### Sobre transacciones

El alcance de la épica es **"consultas e inserciones sin transacciones"**, por lo que esta clase **no declara `@Transactional`**.

Conviene ser preciso al respecto: JPA siempre necesita una transacción para escribir. Lo que ocurre aquí es que cada llamada al repositorio se ejecuta en su propia transacción implícita, gestionada internamente por `SimpleJpaRepository` (la implementación que genera Spring Data, cuyos métodos de escritura ya están anotados). Al no haber operaciones compuestas que deban confirmarse o revertirse en conjunto, **no hace falta declarar una frontera transaccional propia**. Si más adelante se añadiera, por ejemplo, "registrar un avistamiento y actualizar un contador de la zona", ahí sí haría falta un `@Transactional` en el servicio.

#### Métodos

| Método | Historia | Qué hace |
|---|---|---|
| `registrar(AvistamientoRequest)` | **HU1** | Normaliza los textos, construye la entidad y la persiste con `save()`, que ejecuta el `INSERT` y retorna la instancia gestionada con el `id` generado. Devuelve el DTO ya con `id` y con `fechaRegistro` fijada por `@PrePersist`. Deja traza en el log a nivel `INFO`. |
| `buscar(especie, zona, desde, hasta)` | **HU2** | Consulta combinada. Los cuatro parámetros son opcionales; los nulos se descartan dentro del JPQL. Sin filtros devuelve el historial completo, en el mismo orden que una búsqueda filtrada. Traza a nivel `DEBUG` con el número de resultados. |
| `obtenerPorId(Long)` | soporte | `findById` devuelve `Optional`; aquí se transforma en `RecursoNoEncontradoException` cuando no hay dato, para que el manejador global lo traduzca a un HTTP 404 coherente. |
| `resumenPorEspecie()` | soporte | Convierte el `List<Object[]>` crudo del repositorio en un mapa (especie → total). Usa **`LinkedHashMap` y no `HashMap`** para preservar el orden por frecuencia que impone el `order by` de la consulta; un `HashMap` lo perdería. |

El pipeline de mapeo se repite en los métodos de consulta:

```java
.stream()                          // flujo de entidades
.map(AvistamientoResponse::desde)  // entidad → DTO
.toList();                         // lista inmutable (Java 16+)
```

#### `normalizar(String)` (privado)

```java
private String normalizar(String valor) {
    if (valor == null) return null;
    String limpio = valor.trim();
    return limpio.isEmpty() ? null : limpio;
}
```

Recorta espacios al inicio y al final, y convierte cadenas vacías en `null`. Dos efectos deseados:

- Un campo **opcional** enviado como `""` no se guarda como basura.
- Un **filtro** enviado como `""` no se activa por error. Sin esta normalización, `?especie=` haría que el JPQL evaluara `especie is null` como falso y aplicara un `like '%%'` innecesario.

---

### 3.8 `controller/AvistamientoController.java` — capa web

```java
@RestController
@RequestMapping("/api/avistamientos")
```

- **`@RestController`** = `@Controller` + `@ResponseBody`: todo valor retornado se serializa a JSON en lugar de resolverse como nombre de vista.
- **`@RequestMapping`** fija el prefijo común de rutas, de modo que cada método solo declara su parte.

El servicio se inyecta por constructor, igual que en la capa anterior.

#### `registrar(...)` — `POST /api/avistamientos`

```java
@PostMapping
public ResponseEntity<AvistamientoResponse> registrar(@Valid @RequestBody AvistamientoRequest peticion,
                                                      UriComponentsBuilder uriBuilder)
```

| Elemento | Función |
|---|---|
| `@RequestBody` | Jackson deserializa el JSON del cuerpo al `record` de entrada. |
| `@Valid` | **Dispara Bean Validation.** Si alguna restricción del DTO falla, Spring lanza `MethodArgumentNotValidException` **antes** de entrar al método, y el manejador global la convierte en 400. Sin esta anotación las validaciones del DTO serían decorativas. |
| `UriComponentsBuilder` | Spring lo inyecta ya apuntando al host y contexto actuales, sin hardcodear `localhost:8080`. |
| `ResponseEntity.created(ubicacion)` | Devuelve **201 Created** con cabecera `Location: /api/avistamientos/{id}`, como pide la semántica REST para una creación. |

#### `listar(...)` — `GET /api/avistamientos`

```java
@GetMapping
public ResponseEntity<List<AvistamientoResponse>> listar(
        @RequestParam(required = false) String especie,
        @RequestParam(required = false) String zona,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta)
```

Este método **cubre HU2 y los filtros de la descripción en un solo endpoint**:

- `@RequestParam(required = false)` hace opcional cada filtro; sin valor llega `null`.
- `@DateTimeFormat(iso = ISO.DATE_TIME)` instruye a Spring para convertir `2026-09-01T00:00:00` en `LocalDateTime`. **Sin esta anotación la conversión falla** y la petición responde 400.
- `listar` siempre delega en `buscar`. Si los cuatro parámetros llegan `null`, el JPQL no aplica filtro y devuelve el historial completo.
- Si no hay resultados devuelve **204 No Content** en lugar de `200 []`: indica al cliente "la consulta fue válida y no hay nada", distinto de un error.

#### `obtenerPorId(...)` — `GET /api/avistamientos/{id}`

`@PathVariable Long id` extrae el segmento de la URL y lo convierte a `Long`. Si no existe, el servicio lanza la excepción de dominio y el manejador responde 404. Si el segmento no es numérico (`/api/avistamientos/abc`), Spring lanza `MethodArgumentTypeMismatchException` y el manejador responde 400.

#### `resumenPorEspecie()` — `GET /api/avistamientos/resumen/especies`

Expone el conteo agregado. Jackson serializa el `Map<String, Long>` directamente como objeto JSON.

---

### 3.9 Paquete `exception/` — manejo de errores

#### `RecursoNoEncontradoException`

Extiende **`RuntimeException`** (no *checked*) para no obligar a declarar `throws` en toda la cadena de llamadas. El manejador global la intercepta y la traduce a HTTP 404. Su mensaje viaja al cliente.

#### `RespuestaError`

`record` con la estructura uniforme de **todos** los errores de la API:

| Campo | Contenido |
|---|---|
| `momento` | Instante en que se generó el error. |
| `estado` | Código HTTP numérico (400, 404…). |
| `error` | Etiqueta corta del tipo de error. |
| `mensaje` | Descripción general legible. |
| `detalles` | Mapa `campo → motivo`. **Solo se usa en errores de validación**; en el resto viaja `null` y Jackson lo omite gracias a `spring.jackson.default-property-inclusion=non_null`. |

Tener un único formato de error facilita el consumo: el cliente siempre sabe dónde leer el código, el mensaje y el detalle. La fábrica `RespuestaError.de(estado, error, mensaje)` cubre los errores simples sin desglose por campo.

#### `ManejadorGlobalErrores`

`@RestControllerAdvice` combina `@ControllerAdvice` (interceptor transversal a todos los controladores) con `@ResponseBody`. **Gracias a esta clase los controladores quedan libres de bloques `try/catch` repetidos.**

| `@ExceptionHandler` | Respuesta | Cuándo se dispara |
|---|---|---|
| `MethodArgumentNotValidException` | **400** con el mapa `detalles` campo por campo | Falla alguna restricción del DTO anotado con `@Valid`. Recorre `getFieldErrors()` y usa un `LinkedHashMap` para que los campos aparezcan en orden estable en el JSON. |
| `RecursoNoEncontradoException` | **404** con el mensaje original | El servicio no encontró el `id` solicitado. |
| `MethodArgumentTypeMismatchException` | **400** indicando el parámetro problemático | Parámetro de URL mal formado: `/avistamientos/abc` donde se espera un número, o una fecha con formato incorrecto. |

---

### 3.10 `resources/application.properties`

| Bloque | Propiedades | Efecto |
|---|---|---|
| **App** | `spring.application.name`, `server.port=8080` | Nombre en los logs y puerto de escucha. |
| **Datasource** | `spring.datasource.url=jdbc:h2:mem:avistamientosdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | Base H2 **en memoria**. `DB_CLOSE_DELAY=-1` mantiene la base viva mientras la JVM exista; sin ese flag H2 la destruiría al cerrarse la última conexión y se perderían los datos entre peticiones. |
| **Consola H2** | `spring.h2.console.enabled=true`, `path=/h2-console` | Habilita la consola web para inspeccionar las tablas durante el desarrollo. **Debe deshabilitarse en producción.** |
| **JPA** | `ddl-auto=update` | Hibernate crea/ajusta el esquema a partir de las entidades. Cómodo en desarrollo; en producción se sustituye por Flyway o Liquibase. |
| | `show-sql=true` + `hibernate.format_sql=true` | Imprime el SQL generado, formateado. Es lo que permite verificar que `buscarConFiltros` produce la consulta esperada. |
| | `open-in-view=false` | **Desactiva el antipatrón *Open Session In View***. Por defecto Spring Boot mantiene la sesión de Hibernate abierta durante todo el renderizado de la vista, lo que puede disparar consultas inesperadas. Como aquí se devuelven DTO ya materializados, no se necesita. |
| **Datos semilla** | `spring.sql.init.mode=always` | Ejecuta `data.sql` en cada arranque. |
| | `spring.jpa.defer-datasource-initialization=true` | **Crítico junto a `ddl-auto`**: retrasa la ejecución de `data.sql` hasta que Hibernate haya creado las tablas. Sin esta propiedad el `INSERT` correría primero y fallaría porque la tabla aún no existe. |
| **Jackson** | `default-property-inclusion=non_null` | Omite del JSON los campos nulos; es lo que hace que `detalles` no aparezca en los errores que no son de validación. |
| | `serialization.fail-on-empty-beans=false` | Evita que la serialización reviente ante un objeto sin propiedades accesibles. |
| **Logging** | `logging.level.com.reservas.avistamientos=DEBUG` | Habilita las trazas `log.debug(...)` del servicio solo para el código propio, sin inundar la salida con `DEBUG` de todo Spring. |

### 3.11 `resources/data.sql`

Cinco avistamientos de ejemplo con datos realistas (jaguar, oso de anteojos, cóndor andino, danta de montaña), repartidos en varias zonas y fechas. Incluye **dos registros de la misma especie** a propósito, para que el endpoint de resumen y los filtros tengan algo significativo que agrupar desde el primer arranque. Las columnas se escriben en `snake_case`, igual que los `@Column` de la entidad.

---

### 3.12 `test/AvistamientoApiTest.java` — 9 pruebas

```java
@SpringBootTest
@AutoConfigureMockMvc
```

- `@SpringBootTest` levanta el contexto completo de Spring (incluido Hibernate y la carga de `data.sql`).
- `@AutoConfigureMockMvc` inyecta `MockMvc`, que ejecuta peticiones HTTP simuladas **sin abrir un puerto real**: más rápido y sin conflictos de puertos en CI.

| Prueba | Verifica |
|---|---|
| `registraUnAvistamientoYDevuelve201` | HU1 completa: 201, `id` asignado, `especie` correcta y `fechaRegistro` generada por `@PrePersist`. |
| `rechazaUnAvistamientoSinEspecieCon400` | Bean Validation + manejador global: 400 con `detalles.especie` presente. |
| `rechazaFechaFuturaCon400` | La regla `@PastOrPresent` se aplica de verdad. |
| `listaTodosLosAvistamientos` | HU2: al menos los 5 registros semilla. |
| `filtraPorEspecieIgnorandoMayusculas` | Busca `"jaguar"` y encuentra `"Jaguar"`. |
| `filtraPorZonaYRangoDeFechas` | Dos filtros combinados en la misma consulta. |
| `devuelve404CuandoElIdNoExiste` | `RecursoNoEncontradoException` → 404 con el formato de error uniforme. |
| `devuelve204CuandoElFiltroNoEncuentraNada` | Distinción entre "sin resultados" y error. |
| `entregaElResumenPorEspecie` | La agregación cuenta correctamente las 2 entradas de jaguar. |

Resultado: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

---

## 4. Referencia de la API

| Método | Ruta | Códigos | Descripción |
|---|---|---|---|
| `POST` | `/api/avistamientos` | 201, 400 | Registra un avistamiento (**HU1**). Devuelve cabecera `Location`. |
| `GET` | `/api/avistamientos` | 200, 204 | Lista todos (**HU2**) o filtra con `especie`, `zona`, `desde`, `hasta`. |
| `GET` | `/api/avistamientos/{id}` | 200, 400, 404 | Un avistamiento puntual. |
| `GET` | `/api/avistamientos/resumen/especies` | 200 | Conteo de avistamientos por especie. |

### Ejemplos verificados contra la app en ejecución

**Registrar (HU1)**

```bash
curl -X POST http://localhost:8080/api/avistamientos \
  -H "Content-Type: application/json" \
  -d '{
    "especie": "Nutria de rio",
    "nombreCientifico": "Lontra longicaudis",
    "zona": "Rio Claro",
    "latitud": 4.55,
    "longitud": -74.21,
    "fechaAvistamiento": "2026-10-01T07:30:00",
    "observaciones": "Pareja nadando aguas arriba.",
    "registradoPor": "Ana Torres"
  }'
```

```
HTTP 201 — Location: http://localhost:8080/api/avistamientos/6
```
```json
{"id":6,"especie":"Nutria de rio","nombreCientifico":"Lontra longicaudis","zona":"Rio Claro",
 "latitud":4.55,"longitud":-74.21,"fechaAvistamiento":"2026-10-01T07:30:00",
 "observaciones":"Pareja nadando aguas arriba.","registradoPor":"Ana Torres",
 "fechaRegistro":"2026-10-02T17:22:51"}
```

**Consultar (HU2) y filtrar**

```bash
curl http://localhost:8080/api/avistamientos
curl "http://localhost:8080/api/avistamientos?especie=jaguar"
curl "http://localhost:8080/api/avistamientos?zona=Sector%20Norte"
curl "http://localhost:8080/api/avistamientos?desde=2026-09-01T00:00:00&hasta=2026-10-01T00:00:00"
curl "http://localhost:8080/api/avistamientos?especie=jaguar&zona=Sector%20Norte"
```

**Resumen por especie**

```json
{"Jaguar":2,"Condor andino":1,"Danta de montana":1,"Oso de anteojos":1}
```

**Error de validación (400)** — petición sin `especie` y con fecha futura:

```json
{
  "momento": "2026-10-02T17:22:51.4505636",
  "estado": 400,
  "error": "Datos invalidos",
  "mensaje": "La peticion contiene campos que no cumplen las reglas de validacion",
  "detalles": {
    "especie": "La especie es obligatoria",
    "fechaAvistamiento": "La fecha del avistamiento no puede ser futura"
  }
}
```

**Recurso inexistente (404)**

```json
{
  "momento": "2026-10-02T17:22:51.5055953",
  "estado": 404,
  "error": "No encontrado",
  "mensaje": "No existe un avistamiento con id 9999"
}
```

### Evidencias visuales

Las capturas están en la carpeta `imges/`.

**Figura 1.** Historial completo. `GET /api/avistamientos` responde **200** con el arreglo de avistamientos, del más reciente al más antiguo.

![GET 200 con la lista de avistamientos](imges/imagen%201.png)

**Figura 2.** Consulta por id. `GET /api/avistamientos/5` responde **200** con la danta de montaña.

![GET 200 del avistamiento 5](imges/imagen%202.png)

**Figura 3.** Filtro por especie. `GET /api/avistamientos?especie=jaguar` responde **200** con los dos jaguares, aunque la búsqueda vaya en minúsculas.

![GET 200 filtrado por jaguar](imges/imagen%203.png)

**Figura 4.** Resumen por especie. `GET /api/avistamientos/resumen/especies` responde **200**. Jaguar aparece dos veces; el resto, una.

![GET 200 del resumen por especie](imges/imagen%204.png)

**Figura 5.** Id inexistente. `GET /api/avistamientos/9` responde **404** con el mensaje `No existe un avistamiento con id 9`.

![GET 404 de un avistamiento inexistente](imges/imagen%205.png)

**Figura 6.** Otra toma del historial completo, el mismo `GET /api/avistamientos` con **200**.

![Segunda captura del GET de la lista](imges/imgen%206.png)

**Figura 7.** `POST /api/avistamientos` rechazado. La respuesta es **400 Bad Request**.

![POST 400 al registrar](imges/imagen%207.png)

**Figura 8.** Petición a la consola H2 (`POST /h2-console`). Responde **200** con el HTML de la página de acceso.

![Respuesta HTML de la consola H2](imges/imagen%208.png)

**Figura 9.** Ramas en GitHub: `main`, `dev` y las tres `feat/os/avistamiento_*`.

![Selector de ramas del repositorio](imges/imagen%209.png)

---

## 5. Cambiar H2 por PostgreSQL

La épica admite ambos motores. Como todas las consultas son JPQL (no SQL nativo), el cambio es solo de configuración:

1. Sustituir la dependencia `h2` por `org.postgresql:postgresql` en el `pom.xml`.
2. Ajustar `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/avistamientos
spring.datasource.username=postgres
spring.datasource.password=***
spring.jpa.hibernate.ddl-auto=validate
spring.h2.console.enabled=false
spring.sql.init.mode=never
```

No hay que tocar ni una línea de código Java.

---

## 6. Límites conocidos del alcance actual

Son decisiones conscientes, no omisiones:

- **Sin paginación.** `GET /api/avistamientos` devuelve todo. Con volúmenes reales conviene cambiar la firma del repositorio a `Page<Avistamiento> buscarConFiltros(..., Pageable pageable)`.
- **Sin autenticación.** `registradoPor` es un texto libre que el cliente envía; no hay verificación de identidad. Con Spring Security se tomaría del usuario autenticado.
- **Sin índices explícitos** sobre `especie`, `zona` y `fecha_avistamiento`. Con `ddl-auto=update` se pueden añadir vía `@Table(indexes = {...})`; con migraciones versionadas, en el script.
- **Sin actualización ni borrado.** La épica solo pide inserción y consulta, y el alcance se respetó tal cual.
- **Datos en memoria.** Al detener la aplicación se pierde todo y `data.sql` repuebla desde cero. Es lo adecuado para H2 en desarrollo; PostgreSQL resuelve la persistencia real.
