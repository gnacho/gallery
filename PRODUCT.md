# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin y Jetpack Compose, elegidos explicitamente por el usuario. Android nativo con MediaStore, sin sustituir la aplicacion del sistema.

## Users

El usuario propietario del Honor Magic V5 y de la coleccion local de fotos y videos. La tarea principal es explorar, encontrar, revisar, organizar y editar medios sin depender de una cuenta.

## Product Purpose

Reimplementar la Galeria Honor extraida con el comportamiento y la presentacion observables mas fieles posibles. El exito exige una app util con datos reales, pruebas y diferencias conocidas, no una maqueta.

## Positioning

Clon local y auditable de una galeria de sistema, construido desde evidencia estatica y dinamica. Las funciones propias se separan de las dependencias privadas de Honor.

## Operating Context

Telefono Android, pantalla compacta y pantalla interna desplegada. La referencia visual principal es la pantalla interna de 2172 x 2352 px a 480 dpi. No se instala ni se modifica nada en un dispositivo sin autorizacion especifica.

## Capabilities and Constraints

El inventario original contiene 63 funciones. La base verificada es `152.0.10.794`. La version `152.0.10.882` solo tiene referencias comunitarias y no se pudo descargar. Las novedades publicadas se clasifican en `analysis/latest/feature-diff.json`. Los servicios de IA, nube, cuenta o hardware privado no se presentaran como equivalentes locales sin evidencia.

## Brand Commitments

La interfaz observable de Galeria Honor es la referencia vinculante. El paquete sera independiente para no suplantar ni reemplazar la aplicacion del sistema. No se copiaran recursos protegidos cuando no sean necesarios para explicar o verificar una diferencia.

## Evidence on Hand

APK original, salidas REA, recursos decodificados, inventario de clases, capturas y jerarquias UI en `analysis/original/`. Fuentes de version y novedades en `analysis/latest/`. Las capturas contienen contenido personal y se conservan localmente.

## Product Principles

Primero la evidencia. Despues la implementacion vertical con pruebas. Ninguna funcion se declara verificada por existir en el manifiesto. Las dependencias externas se muestran con honestidad. La interfaz debe seguir siendo una galeria Android operativa y accesible.

## Accessibility & Inclusion

Objetivos tactiles de al menos 48 dp, navegacion Back del sistema, insets, tipografia escalable y contraste suficiente. Se comprobaran tema oscuro y escala de fuente en las clases de pantalla disponibles.
