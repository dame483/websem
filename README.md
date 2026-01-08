
# Projet de valorisation des données du Web – Linked Open Data (LOD)

Ce projet vise à valoriser des données issues du Web sémantique en s’appuyant sur une architecture **full‑stack** composée de :

* un backend **Python (FastAPI)**,
* un backend **Java**,
* un frontend **React**.

Deux modes de lancement sont possibles :

* **manuel**, via un environnement virtuel Python,
* **automatisé**, via **Docker & Docker Compose** (recommandé).

---

## Prérequis

* Python ≥ 3.10
* Node.js (pour le frontend React)
* Docker Desktop

---

## Lancement du backend Python sans Docker (mode manuel)

### 1. Création d’un environnement virtuel

Afin d’isoler les dépendances du projet, il est recommandé de créer un environnement virtuel.

```bash
python -m venv env
```

### 2. Activation de l’environnement virtuel

```bash
source backend-python/env/bin/activate
```

### 3. Installation des dépendances

```bash
pip install -r requirements.txt
```

### 4. Vérification des paquets installés

```bash
pip freeze
```

### 5. Lancement du backend FastAPI

```bash
fastapi dev main.py
```

Le backend sera accessible à l’adresse :

* [http://localhost:8000](http://localhost:8000)
* Documentation Swagger : [http://localhost:8000/docs](http://localhost:8000/docs)

---

## Lancement du projet complet avec Docker (recommandé)

Ce mode permet de lancer **l’ensemble du projet** (backend Java, backend Python et frontend React) en une seule commande.

### 1. Installation de Docker

Télécharger et installer Docker Desktop depuis le site officiel :

[https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)

### 2. Lancement des services

À la racine du projet, exécuter la commande suivante :

```bash
docker compose up --build
```

### 3. Accès aux services

* Frontend React : [http://localhost:3000](http://localhost:3000)
* Backend Python (FastAPI) : [http://localhost:8000](http://localhost:8000)
* Documentation FastAPI : [http://localhost:8000/docs](http://localhost:8000/docs)
* Backend Java : [http://localhost:8080](http://localhost:8080)

---


## Technologies utilisées

* FastAPI (Python)
* Java
* React
* Docker & Docker Compose
* Linked Open Data / Web sémantique
