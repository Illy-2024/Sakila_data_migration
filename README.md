# Projet de Migration de Données Sakila (SQL vers NoSQL)

## Objectif du Projet
Ce projet a pour but de réaliser une migration de données depuis la base de données relationnelle Sakila (PostgreSQL) vers des bases de données NoSQL (Redis et MongoDB). L'application de migration est développée en Java.

## Schéma de Migration
Le tableau ci-dessous détaille la répartition des tables de la base de données Sakila vers leurs SGBD cibles NoSQL :

| Table        | SGBD Cible |
|--------------|------------|
| `Country`    | Redis      |
| `City`       | Redis      |
| `Film`       | MongoDB    |
| `Actor`      | MongoDB    |
| `Category`   | MongoDB    |
| `Language`   | MongoDB    |

## Choix des Formats de Données NoSQL

### Redis
Pour les tables `Country` et `City`, les données sont stockées sous forme de chaînes JSON.
* **Format de clé :** `nom_table:id` (ex: `city:1`, `country:50`)
* **Format de valeur (chaîne JSON) :** `{"id": <ID>, "nom": "<Nom>", ...}`
    * Exemple pour `City` : `{"id":1, "name":"A Coruna", "country_id":87}`
    * Exemple pour `Country` : `{"id":1, "name":"Afghanistan"}`
* **Justification :** Le stockage clé-valeur simple est efficace pour les recherches directes par ID. La chaîne JSON permet de stocker des données structurées et de les analyser facilement lors de la récupération.

### MongoDB
Pour les tables `Film`, `Actor`, `Category` et `Language`, les données sont stockées sous forme de documents BSON au sein de collections dédiées.
* **Noms des collections :** `films`, `actors`, `categories`, `languages`
* **Structure du document :** Chaque document utilise le champ `_id` pour la clé primaire de PostgreSQL, suivi des autres attributs de la table.
    * Exemple pour `Actor` : `{"_id": 1, "first_name": "PENELOPE", "last_name": "GUINESS"}`
    * Exemple pour `Film` : `{"_id": 1, "title": "ACADEMY DINOSAUR", "description": "A Epic Drama of a Feminist And a Squirrel...", "release_year": 2006, "language_id": 1}`
* **Justification :** Le modèle orienté document est adapté aux données riches et imbriquées (bien que peu utilisé ici pour la simplicité) et offre une flexibilité de schéma. L'utilisation des IDs PostgreSQL comme `_id` dans MongoDB assure l'unicité et un mappage direct.

## Prérequis
Avant d'exécuter la migration, assurez-vous que les éléments suivants sont installés et configurés :

1.  **Java Development Kit (JDK) 17+ :** Assurez-vous que le JDK est installé et que la variable d'environnement `JAVA_HOME` est configurée.
2.  **Maven (Optionnel, mais recommandé pour la gestion des dépendances) :** Assurez-vous que Maven est installé et accessible via votre PATH.
3.  **Serveur PostgreSQL :**
    * Doit être en cours d'exécution sur `localhost:5432`.
    * La base de données `sakila_project` doit être créée et remplie (à l'aide de `sakila-schema.sql` et `sakila-data.sql` fournis dans l'énoncé du projet).
    * Les identifiants de connexion configurés dans `App.java` doivent correspondre à votre configuration PostgreSQL (par défaut : utilisateur `postgres`, mot de passe `ilham@25`).
4.  **Serveur Redis :**
    * Doit être en cours d'exécution sur `127.0.0.1:6379`.
    * Si vous utilisez WSL/Ubuntu, assurez-vous qu'il est démarré : `sudo service redis-server start`
5.  **Serveur MongoDB :**
    * Doit être en cours d'exécution sur `localhost:27017`.
    * Si vous utilisez WSL/Ubuntu, assurez-vous qu'il est démarré : `sudo systemctl start mongod`
6.  **Git :** Pour cloner et gérer le dépôt.
7.  **Interfaces Graphiques Optionnelles pour Vérification :**
    * Redis Insight : Pour naviguer dans les données Redis.
    * MongoDB Compass : Pour naviguer dans les données MongoDB.

## Configuration et Exécution de la Migration

1.  **Cloner le Dépôt GitHub :**
    Ouvrez votre terminal ou invite de commande et exécutez :
    ```bash
    git clone [https://github.com/Illy-2024/Sakila_data_migration.git](https://github.com/Illy-2024/Sakila_data_migration.git)
    cd Sakila_data_migration
    ```
2.  **Naviguer vers la racine du projet :**
    Assurez-vous d'être dans le répertoire `SakilaMigration` (ou le nom de votre dossier de projet).
    ```bash
    # Exemple si votre projet est dans un sous-dossier (si Sakila_data_migration contient un sous-dossier SakilaMigration):
    cd SakilaMigration
    ```
    *(Note: Si `Sakila_data_migration` est le dossier principal de votre code Java, cette étape pourrait ne pas être nécessaire ou la commande `cd Sakila_data_migration` au-dessus suffira.)*

3.  **Compiler le Projet (avec Maven) :**
    Exécutez cette commande pour télécharger les dépendances et compiler le code :
    ```bash
    mvn clean install
    ```
    *(Si vous n'utilisez pas Maven, vous devrez inclure manuellement les JARs des pilotes JDBC, Jedis et MongoDB dans votre classpath lors de la compilation et de l'exécution.)*

4.  **Assurer le Démarrage des Bases de Données :**
    * **PostgreSQL :** Vérifiez que votre serveur PostgreSQL est actif.
    * **Redis (dans le terminal Ubuntu/WSL) :**
        ```bash
        sudo service redis-server start
        ```
    * **MongoDB (dans le terminal Ubuntu/WSL) :**
        ```bash
        sudo systemctl start mongod
        ```
    Laissez ces terminaux ouverts pendant l'exécution de la migration.

5.  **Exécuter l'Application de Migration :**
    ```bash
    java -cp target/SakilaMigration-1.0-SNAPSHOT.jar com.sakila.migration.App
    ```
    *(Ajustez `SakilaMigration-1.0-SNAPSHOT.jar` au nom réel de votre fichier JAR si différent.)*

    Vous devriez voir les messages de connexion et de progression de la migration s'afficher dans la console.

## Dépannage (Troubleshooting)
* **"Connection refused" / "Failed to connect to Redis/MongoDB" :** Assurez-vous que les serveurs de bases de données respectifs sont bien en cours d'exécution dans votre terminal Ubuntu/WSL (voir l'étape 4).
* **"No suitable driver found" :** Vérifiez que le pilote JDBC PostgreSQL est correctement inclus dans les dépendances de votre projet (`pom.xml`) et que Maven a correctement construit le JAR.
* **Données non affichées correctement :**
    * Vérifiez la logique de vos requêtes `SELECT` et la construction des documents/clés dans votre code `App.java`.
    * Envisagez de décommenter les lignes `drop()` (pour MongoDB) et `del()` (pour Redis) dans `App.java` pour effacer les anciennes données avant de relancer la migration, afin d'assurer un test propre.

## Auteur
Illy-2024