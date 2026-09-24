# Hits To Kill (Fabric 1.21.11)
Client-side. Shows under the crosshair: hits to kill (and with crit) for the targeted living entity.
Accounts for weapon damage, Sharpness, Strength/Weakness, target armor, toughness, Resistance, Protection.
Assumes a fully charged hit. Requires Fabric API.

Build: push to GitHub -> Actions -> artifact `hitstokill-jar` (use the jar without `-sources`).
Local: install Gradle 8.14+, JDK 21, run `gradle build` -> build/libs/hitstokill-1.0.0.jar
