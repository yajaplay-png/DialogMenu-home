# MyHomes — Paper 1.21.11 / Java 21

Maven-ready source tree for the MyHomes Paper plugin.

## Build

Requirements:
- Java 21
- Maven 3.8+

Run from the repository root:

```text
mvn clean package
```

The plugin JAR is produced at:

```text
target/MyHomes.jar
```

## Important source-layout fix

There is exactly one `HomesCommand` class and it lives at:

```text
src/main/java/com/example/myhomes/commands/HomesCommand.java
```

There is intentionally no duplicate `src/main/java/com/example/myhomes/HomesCommand.java`.

## Dialog navigation

Internal dialog navigation uses Paper's callback-based `DialogAction.customClick(callback, options)` rather than the old `customClick(Key, null)` -> `PlayerCustomClickEvent` -> `showDialog` loop.

`HomesDialogListener` is retained only as an empty compatibility class and is not registered.
