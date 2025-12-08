# Tests - Modo Multijugador Infierno Perfecto

## Ubicación Correcta de los Tests

Los tests deben estar en la estructura de Gradle del proyecto **Servidor**:

```
Servidor/
└── core/
    └── src/
        ├── main/java/          # Código fuente
        └── test/java/          # ← TESTS AQUÍ
            └── com/dojan/infiernoperfecto/
                └── batalla/
                    └── BatallaMultijugadorTest.java
```

## Ejecutar Tests

### Desde la carpeta Servidor

```bash
cd Servidor
./gradlew test
```

### Ejecutar solo los tests de Batalla

```bash
cd Servidor
./gradlew test --tests "com.dojan.infiernoperfecto.batalla.BatallaMultijugadorTest"
```

### Ver resultados detallados

```bash
cd Servidor
./gradlew test --info
```

## Estado Actual

- ✅ Fase 1.1: Constructor multijugador implementado
- ✅ Fase 1.2: Métodos de turno multijugador implementados
- ✅ Tests creados en ubicación correcta
- ⏳ Tests necesitan enemigos de prueba para funcionar completamente

## Notas Importantes

1. **Los tests están simplificados** porque aún no tenemos enemigos de prueba definidos
2. **Ejecutar desde carpeta Servidor** - no desde la raíz del proyecto
3. **Usar `./gradlew`** en Linux/Mac o `gradlew.bat` en Windows
4. Los tests verifican la estructura y métodos, pero algunos están marcados como TODO

## Próximos Pasos

Cuando implementemos las siguientes fases, agregaremos:

- Enemigos de prueba para tests más completos
- Tests de integración
- Tests de red (Fase 2)
