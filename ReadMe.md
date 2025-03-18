# Portfolio 3: ZFS File System

## Verwendung
**Hinweis:** Dieses Programm funktioniert nur auf macOS.  
**Voraussetzung:** OpenZFS muss installiert sein.

## File System Initialisieren

1. Wechsel in das Quellverzeichnis:
   ```sh
   cd /src/
   ```  
2. Kompiliere die Initialisierungsdatei:
   ```sh
   javac InitializeFS.java
   ```  
3. Starte das Initialisierungsprogramm:
   ```sh
   java InitializeFS
   ```  
4. Gib den Namen für den ZFS-Pool in der Konsole ein.
5. Während der Initialisierung muss das Administratorpasswort mehrfach eingegeben werden, da `zfs` und `zpool` Root-Rechte benötigen.

## Clients Starten

Die Clients müssen in **zwei separaten Konsolen** gestartet werden.

1. Wechsel in das Quellverzeichnis:
   ```sh
   cd /src/
   ```  
2. Kompiliere den Client-Runner:
   ```sh
   javac ClientRunner.java
   ```  
3. Starte die Clients:
   ```sh
   java ClientRunner Client1
   ```
   bzw.
   ```sh
   java ClientRunner Client2
   ```  

Alternativ können das Dateisystem und **ein** Client auch direkt über eine IDE gestartet werden. Um Konflikte auszulösen müssen Dateien dann mit dem Finder verändert werden. Während der Entwicklung wurde IntelliJ verwendet.

## Nutzung der Clients

Die Clients lassen sich über die Konsole mit fest definierten Befehlen steuern. Eine Liste aller verfügbaren Befehle kann mit folgendem Befehl abgerufen werden:
```sh
help
```  

## Manueller Cleanup

Falls das Dateisystem manuell entfernt werden muss, können die folgenden Schritte ausgeführt werden:

### ZFS-Pool löschen
1. Liste aller ZFS-Pools anzeigen:
   ```sh
   zpool list
   ```  
2. Den entsprechenden Pool zerstören:
   ```sh
   sudo zpool destroy poolName
   ```  

### Virtuelle Festplatte auswerfen
1. Liste der angeschlossenen Laufwerke anzeigen:
   ```sh
   diskutil list
   ```  
2. **Identifiziere die mit ZFS verknüpften Disks.**
3. Die virtuelle Festplatte auswerfen:
   ```sh
   hdiutil eject diskIdentifier
   ```  
