Portfolio 3: ZFS File System
Verwendung:
File System Initialisieren
1. cd /src/ 
2. javac InitializeFS.java 
3. java InitializeFS 
4. Pool Name über Konsole eingeben. 
5. Password muss mehrmals eingegeben werden, da zfs/zpool Adminrechte benötigt.

Clients Starten (in zwei separaten Konsolen!)
1. cd /src/
2. javac ClientRunner.java
3. java ClientRunner Client1
4. java ClientRunner Client2 

Die Clients lassen sich über die Konsole mit fest definierten Befehlen nutzen. 'help' gibt eine Liste der Befehle wieder.
Beispiel.