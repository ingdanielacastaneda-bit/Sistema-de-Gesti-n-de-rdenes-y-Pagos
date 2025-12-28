# Sistema de Gestión de Órdenes y Pagos

Backend empresarial desarrollado con **Java 17** y **Spring Boot 3.2**, enfocado en **modelado de dominio**, **reglas de negocio explícitas** y **consistencia transaccional**.  
Diseñado como **software de producción real**, no como ejercicio académico.

---

## Qué demuestra este proyecto

- Modelado de dominio orientado a negocio (DDD ligero)
- Control explícito de estados y transiciones
- Reglas de negocio encapsuladas en el dominio
- Consistencia transaccional en operaciones críticas
- Separación clara de responsabilidades
- Código preparado para evolución y escalabilidad funcional

---

## Dominio del negocio

### Entidades principales

- **Customer**: Cliente del sistema
- **Order**: Orden de compra con múltiples ítems
- **OrderItem**: Ítems asociados a una orden
- **Payment**: Pagos asociados a una orden
- **PaymentTransaction**: Registro histórico de cambios de estado de los pagos

---

## Estados y transiciones

### Estados de Orden

CREATED → CONFIRMED → PAID → SHIPPED  
CREATED → CANCELLED  
CONFIRMED → CANCELLED  

Reglas:
- Una orden inicia siempre en `CREATED`
- Solo puede confirmarse desde `CREATED`
- Solo puede pagarse desde `CONFIRMED`
- Una orden `PAID` o `SHIPPED` no puede cancelarse
- `SHIPPED` es un estado final

---

### Estados de Pago

PENDING → APPROVED  
PENDING → REJECTED  
PENDING → FAILED  

Reglas:
- Un pago inicia siempre en `PENDING`
- Solo un pago `APPROVED` puede marcar una orden como `PAID`
- Un pago `REJECTED` o `FAILED` **no cambia** el estado de la orden
- Cada cambio de estado se registra como una transacción

> Todas las transiciones están protegidas por reglas de dominio y no pueden ejecutarse arbitrariamente desde la API.

---

## Arquitectura

Arquitectura en capas clara y mantenible:

- **API Layer**  
  Controllers REST, DTOs de entrada y salida, manejo global de errores

- **Application Layer**  
  Servicios de aplicación que orquestan los casos de uso

- **Domain Layer**  
  Entidades, enumeraciones y reglas de negocio encapsuladas

- **Infrastructure Layer**  
  Persistencia de datos con Spring Data JPA

No existe lógica de negocio en los controladores.

---

## Tecnologías

- Java 17
- Spring Boot 3.2
- Spring Data JPA
- H2 (desarrollo)
- PostgreSQL (preparado para producción)
- Bean Validation
- Lombok
- Maven

---

## Ejecución local

### Requisitos
- Java 17 o superior
- Maven 3.6+

### Ejecutar la aplicación

mvn clean install  
mvn spring-boot:run  

### Consola H2 (desarrollo)

- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:orderdb
- Usuario: sa
- Contraseña: (vacía)

---

## Enfoque del proyecto

Este proyecto está orientado a demostrar:

- Diseño backend profesional
- Manejo de flujos de negocio reales
- Reglas de dominio explícitas y protegidas
- Uso correcto de transacciones
- Buenas prácticas en Java y Spring Boot

---

## Licencia

Proyecto desarrollado con fines profesionales y educativos para demostrar arquitectura backend, diseño de dominio y buenas prácticas en Java y Spring Boot.



