# TP Android — Gestion des contacts (V4)

Kotlin • XML • Android Studio • SQLite natif (sans Room) • AndroidX Preference • Fragments • Carte OpenStreetMap (osmdroid)

## Ouvrir et lancer le projet
1. Android Studio : **File > Open** et choisir le dossier `ContactsV4`, puis attendre la synchronisation Gradle
   (Android Studio télécharge lui-même Gradle 8.7 d'après `gradle/wrapper/gradle-wrapper.properties`).
2. Installer les SDK demandés (compileSdk 34). `minSdk` = 26.
3. **Carte** : elle utilise OpenStreetMap (osmdroid) : aucune clé API ni compte à créer. Il suffit que l'émulateur / l'appareil ait accès à internet.
4. Lancer sur un émulateur (avec internet) ou un appareil.

## Fonctionnalités
- **SQLite** (`contacts.db`) : `ContactContract`, `ContactDbHelper` (singleton, 7 contacts de démo dont 3 sans GPS), `ContactDao` (seul SQL, requêtes paramétrées), accès hors thread principal.
- **CRUD** : bouton « + » (création), liste et détail lus en base, modification, suppression avec confirmation.
- **Préférences** : taille de police, couleur du texte, bouton Appeler dans la liste (`ParametresFragment`, `PreferencesManager`), appliquées au retour sur la liste.
- **V3 conservé** : une seule activité + fragments (`replace` / `addToBackStack`), filtre, détail, Appeler (`ACTION_DIAL`), E-mail, Galerie, Caméra, Carte.
- Robustesse : validation, rotation sans doublon ni perte de saisie, photos copiées dans `filesDir`, thème clair/sombre.

## Structure
```
app/src/main/java/com/example/contacts/
  data/   Contact, ContactContract, ContactDbHelper, ContactDao, ContactValidator
  prefs/  PreferencesManager (DisplayPrefs)
  ui/     MainActivity, Navigator, ContactViewModel, ListeContactsFragment, ContactAdapter,
          DetailContactFragment, CarteContactFragment, ParametresFragment, ConfirmDeleteDialogFragment
  util/   PhotoStorage, ImageLoader, IntentActions
app/src/main/res/  layouts, menu, xml/preferences.xml, values/arrays.xml, drawables
app/src/test/         ContactValidatorTest   (JVM)
app/src/androidTest/  ContactDaoTest         (appareil/émulateur)
docs/                 Compte rendu (DOCX modifiable + PDF)
```

## Tests
- Unitaires : clic droit sur `app/src/test` > *Run Tests* (ou `./gradlew test`).
- Instrumentés (DAO) : clic droit sur `app/src/androidTest` > *Run Tests* (émulateur démarré).
- Manuels : voir le tableau du compte rendu (section 9) — à exécuter puis compléter.

## Rappel
Si vous modifiez le schéma pendant le développement : désinstaller l'application, ou incrémenter `DATABASE_VERSION` et écrire `onUpgrade`.
