# Sistema de Gestión de Órdenes y Pagos

Sistema backend empresarial desarrollado con **Java 17** y **Spring Boot 3.2.0**, diseñado como software de producción real para la gestión completa del ciclo de vida de órdenes y pagos.

## 📋 Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Dominio del Negocio](#dominio-del-negocio)
- [Reglas de Negocio](#reglas-de-negocio)
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Configuración](#configuración)
- [Endpoints API](#endpoints-api)
- [Decisiones Técnicas](#decisiones-técnicas)
- [Ejecución](#ejecución)

---

## Descripción General

Este sistema gestiona el ciclo completo de órdenes de compra y sus pagos asociados, implementando un modelo de dominio rico con reglas de negocio explícitas, validaciones estrictas y transiciones de estado controladas.

**Características principales:**
- Gestión completa del ciclo de vida de órdenes
- Sistema de pagos con múltiples transacciones
- Validaciones de negocio y transiciones de estado
- Arquitectura en capas clara y mantenible
- Manejo global de errores
- Consistencia transaccional

---

## Arquitectura

El proyecto sigue una **arquitectura en capas** clara y profesional:

```
┌─────────────────────────────────────┐
│         API Layer (REST)            │
│      Controllers + DTOs             │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│      Application Layer              │
│    Services (Lógica de Negocio)     │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│        Domain Layer                 │
│   Entities + Enums + Rules          │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│      Infrastructure Layer           │
│    Repositories (JPA)               │
└─────────────────────────────────────┘
```

### Capas

1. **API Layer** (`com.ordersystem.api`)
   - Controllers REST
   - DTOs de Request/Response
   - Manejo de excepciones global

2. **Application Layer** (`com.ordersystem.application`)
   - Services con lógica de negocio
   - Orquestación de operaciones
   - Validaciones de negocio adicionales

3. **Domain Layer** (`com.ordersystem.domain`)
   - Entidades del dominio
   - Enums de estados
   - Reglas de negocio encapsuladas

4. **Infrastructure Layer** (`com.ordersystem.domain.repository`)
   - Repositorios Spring Data JPA
   - Persistencia de datos

---

## Dominio del Negocio

### Entidades Principales

#### 1. Customer (Cliente)
Representa un cliente del sistema.

**Atributos:**
- `id`: Identificador único
- `name`: Nombre del cliente
- `email`: Email único del cliente
- `createdAt`: Fecha de creación
- `orders`: Lista de órdenes del cliente

#### 2. Order (Orden)
Representa una orden de compra.

**Atributos:**
- `id`: Identificador único
- `customer`: Cliente que realiza la orden
- `items`: Lista de ítems de la orden
- `totalAmount`: Monto total calculado
- `status`: Estado actual de la orden (enum)
- `createdAt`: Fecha de creación
- `payments`: Lista de pagos asociados

**Estados posibles:**
- `CREATED`: Orden creada, pendiente de confirmación
- `CONFIRMED`: Orden confirmada, lista para pago
- `PAID`: Orden pagada completamente
- `SHIPPED`: Orden enviada al cliente
- `CANCELLED`: Orden cancelada

#### 3. OrderItem (Ítem de Orden)
Representa un producto dentro de una orden.

**Atributos:**
- `id`: Identificador único
- `productName`: Nombre del producto
- `quantity`: Cantidad solicitada
- `unitPrice`: Precio unitario
- `order`: Orden a la que pertenece
- `subtotal`: Cantidad × Precio unitario (calculado)

#### 4. Payment (Pago)
Representa un pago asociado a una orden.

**Atributos:**
- `id`: Identificador único
- `order`: Orden asociada
- `amount`: Monto del pago
- `status`: Estado actual del pago (enum)
- `createdAt`: Fecha de creación
- `transactions`: Historial de transacciones

**Estados posibles:**
- `PENDING`: Pago pendiente de procesamiento
- `APPROVED`: Pago aprobado
- `REJECTED`: Pago rechazado
- `FAILED`: Pago fallido

#### 5. PaymentTransaction (Transacción de Pago)
Registra cada cambio de estado en un pago para auditoría.

**Atributos:**
- `id`: Identificador único
- `payment`: Pago asociado
- `status`: Estado registrado
- `timestamp`: Fecha y hora del cambio

---

## Reglas de Negocio

### Reglas de Orden

#### 1. Creación de Orden
- Una orden se crea siempre en estado `CREATED`
- Debe tener al menos un ítem
- El `totalAmount` se calcula automáticamente sumando los subtotales de los ítems
- El cliente debe existir en el sistema

#### 2. Confirmación de Orden
- **Solo** se puede confirmar una orden en estado `CREATED`
- La confirmación cambia el estado a `CONFIRMED`
- Una vez confirmada, la orden está lista para recibir pagos

#### 3. Pago de Orden
- **Solo** se puede crear un pago para una orden en estado `CONFIRMED`
- Cuando un pago es `APPROVED` y el total pagado ≥ total de la orden, la orden pasa automáticamente a `PAID`
- Un pago `FAILED` **NO** cambia el estado de la orden

#### 4. Cancelación de Orden
- Se puede cancelar una orden en estado `CREATED` o `CONFIRMED`
- **NO** se puede cancelar una orden en estado `PAID` o `SHIPPED`
- Una orden cancelada no puede realizar más operaciones

#### 5. Envío de Orden
- **Solo** se puede marcar como enviada una orden en estado `PAID`
- Una orden enviada es un estado final

### Reglas de Pago

#### 1. Creación de Pago
- Un pago siempre inicia en estado `PENDING`
- El monto del pago no puede exceder el monto pendiente de la orden
- Se registra automáticamente una transacción inicial en estado `PENDING`

#### 2. Aprobación de Pago
- **Solo** se puede aprobar un pago en estado `PENDING`
- Al aprobarse, se registra una transacción con estado `APPROVED`
- Si el total pagado alcanza o supera el total de la orden, la orden pasa a `PAID`

#### 3. Rechazo de Pago
- **Solo** se puede rechazar un pago en estado `PENDING`
- Al rechazarse, se registra una transacción con estado `REJECTED`
- Un pago rechazado no afecta el estado de la orden

#### 4. Fallo de Pago
- **Solo** se puede marcar como fallido un pago en estado `PENDING`
- Al fallar, se registra una transacción con estado `FAILED`
- **Un pago fallido NO cambia el estado de la orden** (regla de negocio explícita)

### Diagrama de Transiciones de Estado

#### Estados de Orden
```
CREATED → CONFIRMED → PAID → SHIPPED
   ↓         ↓
CANCELLED  CANCELLED
```

#### Estados de Pago
```
PENDING → APPROVED
    ↓
REJECTED
    ↓
FAILED
```

---

## Tecnologías

- **Java 17**: Lenguaje de programación
- **Spring Boot 3.2.0**: Framework principal
- **Spring Data JPA**: Persistencia de datos
- **H2 Database**: Base de datos en memoria para desarrollo
- **PostgreSQL**: Preparado para producción
- **Bean Validation**: Validaciones de datos
- **Lombok**: Reducción de boilerplate
- **Maven**: Gestión de dependencias

---

## Estructura del Proyecto

```
src/main/java/com/ordersystem/
├── api/
│   ├── controller/          # Controllers REST
│   │   ├── CustomerController.java
│   │   ├── OrderController.java
│   │   └── PaymentController.java
│   ├── dto/
│   │   ├── request/         # DTOs de entrada
│   │   └── response/        # DTOs de salida
│   └── exception/           # Manejo global de errores
│       ├── ErrorResponse.java
│       └── GlobalExceptionHandler.java
├── application/
│   └── service/             # Servicios de aplicación
│       ├── CustomerService.java
│       ├── OrderService.java
│       └── PaymentService.java
├── domain/
│   ├── enums/               # Enumeraciones de estados
│   │   ├── OrderStatus.java
│   │   └── PaymentStatus.java
│   ├── model/               # Entidades del dominio
│   │   ├── Customer.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Payment.java
│   │   └── PaymentTransaction.java
│   └── repository/          # Repositorios JPA
│       ├── CustomerRepository.java
│       ├── OrderRepository.java
│       ├── PaymentRepository.java
│       └── PaymentTransactionRepository.java
└── OrderManagementSystemApplication.java
```

---

## Configuración

### Base de Datos

El sistema está configurado para usar **H2** en desarrollo y **PostgreSQL** en producción.

#### Desarrollo (H2)
```properties
spring.datasource.url=jdbc:h2:mem:orderdb
spring.datasource.username=sa
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

#### Producción (PostgreSQL)
Descomentar y configurar en `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/orderdb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### Puerto del Servidor
Por defecto el servidor se ejecuta en el puerto **8080**.

---

## Endpoints API

### Clientes

#### Crear Cliente
```http
POST /api/customers
Content-Type: application/json

{
  "name": "Juan Pérez",
  "email": "juan@example.com"
}
```

#### Obtener Cliente por ID
```http
GET /api/customers/{id}
```

#### Obtener Cliente por Email
```http
GET /api/customers/email/{email}
```

### Órdenes

#### Crear Orden
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": 1,
  "items": [
    {
      "productName": "Producto 1",
      "quantity": 2,
      "unitPrice": 100.50
    },
    {
      "productName": "Producto 2",
      "quantity": 1,
      "unitPrice": 250.00
    }
  ]
}
```

#### Obtener Orden por ID
```http
GET /api/orders/{id}
```

#### Confirmar Orden
```http
POST /api/orders/{id}/confirm
```

#### Cancelar Orden
```http
POST /api/orders/{id}/cancel
```

#### Marcar Orden como Enviada
```http
POST /api/orders/{id}/ship
```

#### Obtener Órdenes por Cliente
```http
GET /api/orders/customer/{customerId}
```

#### Obtener Órdenes por Estado
```http
GET /api/orders/status/{status}
```

### Pagos

#### Crear Pago
```http
POST /api/payments
Content-Type: application/json

{
  "orderId": 1,
  "amount": 451.00
}
```

#### Obtener Pago por ID
```http
GET /api/payments/{id}
```

#### Aprobar Pago
```http
POST /api/payments/{id}/approve
```

#### Rechazar Pago
```http
POST /api/payments/{id}/reject
```

#### Marcar Pago como Fallido
```http
POST /api/payments/{id}/fail
```

#### Obtener Pagos por Orden
```http
GET /api/payments/order/{orderId}
```

#### Obtener Resumen de Orden con Pagos
```http
GET /api/payments/order/{orderId}/summary
```

---

## Decisiones Técnicas

### 1. Arquitectura en Capas
**Decisión**: Separación clara entre API, Application, Domain e Infrastructure.

**Justificación**: Facilita el mantenimiento, testing y evolución del sistema. Cada capa tiene responsabilidades bien definidas.

### 2. Reglas de Negocio en el Dominio
**Decisión**: Las reglas de transición de estado están encapsuladas en métodos de las entidades (`Order.confirm()`, `Payment.approve()`, etc.).

**Justificación**: Mantiene la coherencia del modelo de dominio y evita la anémica de entidades. Las reglas están donde deben estar.

### 3. Uso de @Transactional
**Decisión**: Todos los métodos de servicio que modifican datos están anotados con `@Transactional`.

**Justificación**: Garantiza consistencia transaccional. Los métodos de solo lectura usan `@Transactional(readOnly = true)` para optimización.

### 4. DTOs Separados
**Decisión**: Uso de DTOs específicos para Request y Response, separados de las entidades del dominio.

**Justificación**: 
- Controla la exposición de datos a través de la API
- Evita problemas de serialización JSON (referencias circulares)
- Permite versionado de API independiente del dominio

### 5. Manejo Global de Errores
**Decisión**: `@ControllerAdvice` centraliza el manejo de excepciones.

**Justificación**: 
- Respuestas HTTP consistentes
- Código más limpio en los controllers
- Facilita el logging y monitoreo

### 6. Validaciones con Bean Validation
**Decisión**: Validaciones tanto en DTOs como en entidades usando `@Valid` y anotaciones JSR-303.

**Justificación**: 
- Validaciones declarativas y reutilizables
- Mensajes de error claros y consistentes
- Validación automática en el nivel de controller

### 7. Repositorios con Queries Optimizadas
**Decisión**: Uso de `@Query` con `JOIN FETCH` para evitar el problema N+1.

**Justificación**: Optimiza el rendimiento al cargar entidades relacionadas en una sola consulta.

### 8. H2 para Desarrollo, PostgreSQL para Producción
**Decisión**: H2 en memoria para desarrollo rápido, PostgreSQL para producción.

**Justificación**: 
- Desarrollo sin configuración de base de datos
- Cambio fácil entre ambientes
- PostgreSQL ofrece mejor rendimiento y características en producción

### 9. Lombok para Reducir Boilerplate
**Decisión**: Uso de Lombok (`@Data`, `@Builder`, etc.).

**Justificación**: Reduce código repetitivo manteniendo la legibilidad. Los métodos generados son estándar y bien conocidos.

### 10. Estados como Enums
**Decisión**: Estados de orden y pago como enumeraciones Java.

**Justificación**: 
- Type-safety
- Fácil de extender
- Documentación clara de estados válidos

---

## Ejecución

### Requisitos Previos
- Java 17 o superior
- Maven 3.6 o superior

### Ejecución Local

1. **Clonar o descargar el proyecto**

2. **Compilar el proyecto**
```bash
mvn clean install
```

3. **Ejecutar la aplicación**
```bash
mvn spring-boot:run
```

O usando el JAR compilado:
```bash
java -jar target/order-management-system-1.0.0.jar
```

4. **Acceder a la consola H2** (solo desarrollo)
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:orderdb
Usuario: sa
Password: (vacío)
```

### Testing con Postman/curl

**Ejemplo: Flujo completo**

1. Crear un cliente:
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Juan Pérez","email":"juan@example.com"}'
```

2. Crear una orden:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      {"productName":"Producto A","quantity":2,"unitPrice":100.50},
      {"productName":"Producto B","quantity":1,"unitPrice":250.00}
    ]
  }'
```

3. Confirmar la orden:
```bash
curl -X POST http://localhost:8080/api/orders/1/confirm
```

4. Crear un pago:
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"amount":451.00}'
```

5. Aprobar el pago:
```bash
curl -X POST http://localhost:8080/api/payments/1/approve
```

6. Consultar el resumen:
```bash
curl http://localhost:8080/api/payments/order/1/summary
```

---

## Consideraciones de Producción

Para un despliegue en producción, considerar:

1. **Configuración de Base de Datos**: Usar PostgreSQL con pool de conexiones configurado
2. **Seguridad**: Implementar autenticación/autorización (Spring Security)
3. **Logging**: Configurar logging estructurado (Logback, Log4j2)
4. **Monitoreo**: Integrar Actuator y métricas (Prometheus, Micrometer)
5. **Documentación API**: Integrar Swagger/OpenAPI
6. **Testing**: Agregar tests unitarios e integración
7. **Validaciones adicionales**: Implementar validaciones de negocio más complejas
8. **Optimización**: Revisar índices de base de datos según queries frecuentes

---

## Licencia

Este proyecto está diseñado como software de ejemplo para fines educativos y profesionales.

---

**Desarrollado con Spring Boot y mejores prácticas de desarrollo backend empresarial.**


